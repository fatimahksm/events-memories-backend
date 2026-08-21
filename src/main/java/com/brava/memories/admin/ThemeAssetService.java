package com.brava.memories.admin;

import com.brava.memories.common.exception.AppException;
import com.brava.memories.config.AppProperties;
import com.brava.memories.config.UploadProperties;
import com.brava.memories.storage.ObjectStorage;
import com.brava.memories.upload.UploadValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ThemeAssetService {
    private static final Map<String,String> EXTENSIONS=Map.of("image/jpeg","jpg","image/png","png","image/webp","webp");
    private final ObjectStorage storage; private final UploadValidationService validation; private final UploadProperties uploads; private final AppProperties properties; private final ThemeAssetRepository assets;
    public ThemeAssetService(ObjectStorage storage,UploadValidationService validation,UploadProperties uploads,AppProperties properties,ThemeAssetRepository assets){this.storage=storage;this.validation=validation;this.uploads=uploads;this.properties=properties;this.assets=assets;}
    public List<AdminDtos.Asset> list(){return assets.findAllByOrderByCreatedAtDesc().stream().map(a->new AdminDtos.Asset(a.getId(),a.getUrl(),a.getContentType(),a.getSizeBytes(),a.getCreatedAt())).toList();}
    public AdminDtos.AssetUpload upload(MultipartFile file) {
        String type = file.getContentType();
        if (file.isEmpty() || type == null || !EXTENSIONS.containsKey(type)) {
            throw new AppException("INVALID_BACKGROUND", "Choose a JPG, PNG, or WEBP image", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > uploads.maxImageBytes()) {
            throw new AppException("BACKGROUND_TOO_LARGE", "Background image exceeds the allowed size", HttpStatus.BAD_REQUEST);
        }
        try (InputStream validationStream = file.getInputStream()) {
            validation.validateSignature(validationStream, type);
        } catch (IOException ex) {
            throw new AppException("BACKGROUND_UPLOAD_FAILED", "Unable to validate the background image", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String key = "theme-assets/" + UUID.randomUUID() + "." + EXTENSIONS.get(type);
        try (InputStream uploadStream = file.getInputStream()) {
            storage.write(key, uploadStream, file.getSize(), type);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8));
            String url = properties.storage().publicBaseUrl() + "/api/public/theme-assets/" + encoded;
            assets.save(new ThemeAsset(UUID.randomUUID(), url, type, file.getSize()));
            return new AdminDtos.AssetUpload(url);
        } catch (IOException ex) {
            throw new AppException("BACKGROUND_UPLOAD_FAILED", "Unable to upload the background image", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
