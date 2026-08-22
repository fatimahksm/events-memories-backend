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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full pipeline for a genuine HEVC video (the same fixture VideoTranscodingServiceTest exercises
 * directly): upload it as a guest would, and confirm the owner-facing media listing ends up with
 * both a poster (thumbnailUrl) and a web-compatible H.264 rendition (renditionUrl) — not just the
 * untouched original. Not @Transactional for the same reason as PublicUploadIntegrationTest: the
 * async pipeline's AFTER_COMMIT listener needs a real commit to fire.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.rate-limit.login-attempts-per-five-minutes=1000")
class VideoPipelineIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void guestVideoUploadEndsUpWithAPosterAndAWebCompatibleRendition() throws Exception {
        byte[] video = Files.readAllBytes(Path.of("src/test/resources/fixtures/sample-hevc.mp4"));
        String suffix = UUID.randomUUID().toString();

        var registration = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "displayName", "Video Pipeline Owner",
                                "email", "video-pipeline-" + suffix + "@example.com",
                                "password", "StrongPass123!"))))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie auth = registration.getResponse().getCookie("access_token");
        String ownerId = json.readTree(registration.getResponse().getContentAsByteArray()).get("id").asString();

        String adminEmail = "video-pipeline-admin-" + suffix + "@example.com";
        users.save(new AppUser(UUID.randomUUID(), adminEmail, encoder.encode("VideoPipelineAdminPass123!"), "Video Pipeline Admin", UserRole.SUPER_ADMIN));
        var adminLogin = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", adminEmail, "password", "VideoPipelineAdminPass123!"))))
                .andExpect(status().isOk()).andReturn();
        Cookie admin = adminLogin.getResponse().getCookie("access_token");

        String slug = "video-pipeline-" + suffix;
        var creation = mvc.perform(post("/api/admin/events")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "ownerId", ownerId,
                                "names", "Video Pipeline Event",
                                "expiresAt", Instant.now().plus(2, ChronoUnit.DAYS).toString(),
                                "mediaDeleteAt", Instant.now().plus(16, ChronoUnit.DAYS).toString(),
                                "slug", slug))))
                .andExpect(status().isCreated())
                .andReturn();
        String eventId = json.readTree(creation.getResponse().getContentAsByteArray()).get("id").asString();

        String visitorId = "video-visitor-" + suffix;
        var session = mvc.perform(post("/api/public/events/{slug}/uploads/session", slug)
                        .header("X-Visitor-Id", visitorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "clientUploadId", UUID.randomUUID().toString(),
                                "fileName", "wedding-clip.mp4",
                                "contentType", "video/mp4",
                                "size", video.length,
                                "visibility", "PUBLIC"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode sessionBody = json.readTree(session.getResponse().getContentAsByteArray());
        String mediaId = sessionBody.get("mediaId").asString();
        String uploadPath = sessionBody.get("uploadUrl").asString().replaceFirst("^https?://[^/]+", "");

        mvc.perform(put(uploadPath).content(video)).andExpect(status().isNoContent());
        mvc.perform(post("/api/public/events/{slug}/uploads/{mediaId}/finalize", slug, mediaId)
                        .header("X-Visitor-Id", visitorId))
                .andExpect(status().isOk());

        String finalStatus = null;
        for (int i = 0; i < 60; i++) {
            MvcResult r = mvc.perform(get("/api/public/events/{slug}/uploads/{mediaId}/status", slug, mediaId))
                    .andExpect(status().isOk()).andReturn();
            finalStatus = json.readTree(r.getResponse().getContentAsByteArray()).get("status").asString();
            if ("READY".equals(finalStatus) || "REJECTED".equals(finalStatus) || "FAILED".equals(finalStatus)) break;
            Thread.sleep(500);
        }
        assertThat(finalStatus).isEqualTo("READY");

        var ownerMedia = mvc.perform(get("/api/owner/events/{eventId}/media", eventId).cookie(auth))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = json.readTree(ownerMedia.getResponse().getContentAsByteArray()).get("items");
        JsonNode item = items.get(0);

        assertThat(item.get("url").asString()).isNotBlank();
        assertThat(item.get("thumbnailUrl").isNull()).as("poster frame should be generated for a video").isFalse();
        assertThat(item.get("renditionUrl").isNull()).as("H.264 web rendition should be generated for the HEVC original").isFalse();
    }
}
