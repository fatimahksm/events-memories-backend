package com.brava.memories.upload;

import com.brava.memories.auth.AppUser;
import com.brava.memories.auth.AppUserRepository;
import com.brava.memories.auth.UserRole;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Not @Transactional on purpose: the async media-processing pipeline only runs its
 * AFTER_COMMIT listener once the finalize() transaction actually commits, so these
 * tests need real commits rather than the usual rollback-per-test isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.rate-limit.login-attempts-per-five-minutes=1000")
class PublicUploadIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 't', 'e', 's', 't'};
    private static final byte[] NOT_A_JPEG_BYTES = "this is definitely not a jpeg file".getBytes();

    @Test
    void guestUploadOfAValidImageBecomesReadyAndVisibleViaStatusPolling() throws Exception {
        String slug = createEventAndGetSlug("upload-ready");
        String mediaId = createSessionAndUpload(slug, "memory.jpg", JPEG_BYTES);

        String finalStatus = pollUntilResolved(slug, mediaId);
        assertThat(finalStatus).isEqualTo("READY");
    }

    @Test
    void guestUploadWhoseBytesDoNotMatchTheDeclaredFormatIsRejectedNotSilentlyLost() throws Exception {
        String slug = createEventAndGetSlug("upload-rejected");
        String mediaId = createSessionAndUpload(slug, "memory.jpg", NOT_A_JPEG_BYTES);

        String finalStatus = pollUntilResolved(slug, mediaId);
        assertThat(finalStatus).isEqualTo("REJECTED");

        MvcResult statusResult = mvc.perform(get("/api/public/events/{slug}/uploads/{mediaId}/status", slug, mediaId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(statusResult.getResponse().getContentAsByteArray());
        assertThat(body.get("rejected").asBoolean()).isTrue();
    }

    private String createEventAndGetSlug(String label) throws Exception {
        String suffix = UUID.randomUUID().toString();
        Cookie admin = loginAsFreshAdmin(label, suffix);
        String ownerId = createOwner(admin, label, suffix);

        String slug = label + "-" + suffix;
        mvc.perform(post("/api/admin/events")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "ownerId", ownerId,
                                "names", "Upload Test Event",
                                "expiresAt", Instant.now().plus(2, ChronoUnit.DAYS).toString(),
                                "mediaDeleteAt", Instant.now().plus(16, ChronoUnit.DAYS).toString(),
                                "slug", slug))))
                .andExpect(status().isCreated());
        return slug;
    }

    private Cookie loginAsFreshAdmin(String label, String suffix) throws Exception {
        String adminEmail = label + "-admin-" + suffix + "@example.com";
        users.save(new AppUser(UUID.randomUUID(), adminEmail, encoder.encode("UploadTestAdminPass123!"), "Upload Test Admin", UserRole.SUPER_ADMIN));
        var login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", adminEmail, "password", "UploadTestAdminPass123!"))))
                .andExpect(status().isOk()).andReturn();
        return login.getResponse().getCookie("access_token");
    }

    private String createOwner(Cookie admin, String label, String suffix) throws Exception {
        var result = mvc.perform(post("/api/admin/owners")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "displayName", "Upload Test Owner",
                                "email", label + "-" + suffix + "@example.com",
                                "password", "OwnerPass1234"))))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsByteArray()).get("id").asString();
    }

    private String createSessionAndUpload(String slug, String fileName, byte[] bytes) throws Exception {
        String visitorId = "visitor-" + UUID.randomUUID();
        var session = mvc.perform(post("/api/public/events/{slug}/uploads/session", slug)
                        .header("X-Visitor-Id", visitorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "clientUploadId", UUID.randomUUID().toString(),
                                "fileName", fileName,
                                "contentType", "image/jpeg",
                                "size", bytes.length,
                                "visibility", "PUBLIC"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode sessionBody = json.readTree(session.getResponse().getContentAsByteArray());
        String mediaId = sessionBody.get("mediaId").asString();
        String uploadPath = sessionBody.get("uploadUrl").asString().replaceFirst("^https?://[^/]+", "");

        mvc.perform(put(uploadPath).content(bytes)).andExpect(status().isNoContent());
        mvc.perform(post("/api/public/events/{slug}/uploads/{mediaId}/finalize", slug, mediaId)
                        .header("X-Visitor-Id", visitorId))
                .andExpect(status().isOk());
        return mediaId;
    }

    private String pollUntilResolved(String slug, String mediaId) throws Exception {
        String finalStatus = null;
        for (int i = 0; i < 40; i++) {
            MvcResult result = mvc.perform(get("/api/public/events/{slug}/uploads/{mediaId}/status", slug, mediaId))
                    .andExpect(status().isOk())
                    .andReturn();
            finalStatus = json.readTree(result.getResponse().getContentAsByteArray()).get("status").asString();
            if ("READY".equals(finalStatus) || "REJECTED".equals(finalStatus) || "FAILED".equals(finalStatus)) break;
            Thread.sleep(250);
        }
        return finalStatus;
    }
}
