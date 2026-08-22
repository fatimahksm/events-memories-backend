package com.brava.memories.upload;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Generates a smaller JPEG copy of an uploaded image for the album grid — the original stays
 *  untouched and full-resolution for the lightbox/download. Never throws: a thumbnail is an
 *  optimization, not a requirement, so any failure (unsupported format, corrupt file) just
 *  means no thumbnail is stored and the grid falls back to the original image. */
@Service
public class ThumbnailService {
    private static final Logger log = LoggerFactory.getLogger(ThumbnailService.class);
    private static final int MAX_DIMENSION = 640;

    public byte[] generate(InputStream imageBytes) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(imageBytes).size(MAX_DIMENSION, MAX_DIMENSION).outputFormat("jpg").outputQuality(0.82).toOutputStream(out);
            return out.toByteArray();
        } catch (Exception ex) {
            log.warn("Could not generate a thumbnail, the original will be used instead: {}", ex.getMessage());
            return null;
        }
    }
}
