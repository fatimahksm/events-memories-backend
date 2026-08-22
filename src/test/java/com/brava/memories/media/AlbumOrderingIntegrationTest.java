package com.brava.memories.media;

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
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Not @Transactional: relies on the async media-processing pipeline's AFTER_COMMIT
 * listener (same reason as PublicUploadIntegrationTest), so needs real commits.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.rate-limit.login-attempts-per-five-minutes=1000")
class AlbumOrderingIntegrationTest {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 't', 'e', 's', 't'};

    @Test
    void mostLikedMediaSortsFirstEvenWhenUploadedEarliest() throws Exception {
        String slug = createEventAndGetSlug();

        // Upload in order: leastLiked, mostLiked, unliked. Recency order alone would keep this order;
        // like-count order must flip it so mostLiked (2 likes) leads, then leastLiked (1 like), then unliked (0).
        String leastLiked = uploadAndAwaitReady(slug, "least.jpg");
        String mostLiked = uploadAndAwaitReady(slug, "most.jpg");
        String unliked = uploadAndAwaitReady(slug, "none.jpg");

        like(slug, mostLiked, "visitor-aaaaaaaa");
        like(slug, mostLiked, "visitor-bbbbbbbb");
        like(slug, leastLiked, "visitor-cccccccc");

        MvcResult result = mvc.perform(get("/api/public/events/{slug}/album/paged", slug).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = json.readTree(result.getResponse().getContentAsByteArray()).get("items");

        assertThat(items.get(0).get("id").asString()).isEqualTo(mostLiked);
        assertThat(items.get(0).get("likes").asLong()).isEqualTo(2);
        assertThat(items.get(1).get("id").asString()).isEqualTo(leastLiked);
        assertThat(items.get(1).get("likes").asLong()).isEqualTo(1);
        assertThat(items.get(2).get("id").asString()).isEqualTo(unliked);
        assertThat(items.get(2).get("likes").asLong()).isEqualTo(0);
    }

    private void like(String slug, String mediaId, String visitorId) throws Exception {
        mvc.perform(post("/api/public/events/{slug}/media/{mediaId}/like", slug, mediaId)
                        .header("X-Visitor-Id", visitorId))
                .andExpect(status().isOk());
    }

    private String createEventAndGetSlug() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Cookie admin = loginAsFreshAdmin(suffix);
        String ownerId = createOwner(admin, suffix);

        String slug = "album-order-" + suffix;
        mvc.perform(post("/api/admin/events")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "ownerId", ownerId,
                                "names", "Album Order Event",
                                "expiresAt", Instant.now().plus(2, ChronoUnit.DAYS).toString(),
                                "mediaDeleteAt", Instant.now().plus(16, ChronoUnit.DAYS).toString(),
                                "slug", slug))))
                .andExpect(status().isCreated());
        return slug;
    }

    private Cookie loginAsFreshAdmin(String suffix) throws Exception {
        String adminEmail = "album-order-admin-" + suffix + "@example.com";
        users.save(new AppUser(UUID.randomUUID(), adminEmail, encoder.encode("AlbumOrderAdminPass123!"), "Album Order Admin", UserRole.SUPER_ADMIN));
        var login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", adminEmail, "password", "AlbumOrderAdminPass123!"))))
                .andExpect(status().isOk()).andReturn();
        return login.getResponse().getCookie("access_token");
    }

    private String createOwner(Cookie admin, String suffix) throws Exception {
        var result = mvc.perform(post("/api/admin/owners")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "displayName", "Album Order Owner",
                                "email", "album-order-" + suffix + "@example.com",
                                "password", "OwnerPass1234"))))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsByteArray()).get("id").asString();
    }

    private String uploadAndAwaitReady(String slug, String fileName) throws Exception {
        String visitorId = "visitor-" + UUID.randomUUID();
        var session = mvc.perform(post("/api/public/events/{slug}/uploads/session", slug)
                        .header("X-Visitor-Id", visitorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "clientUploadId", UUID.randomUUID().toString(),
                                "fileName", fileName,
                                "contentType", "image/jpeg",
                                "size", JPEG_BYTES.length,
                                "visibility", "PUBLIC"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode sessionBody = json.readTree(session.getResponse().getContentAsByteArray());
        String mediaId = sessionBody.get("mediaId").asString();
        String uploadPath = sessionBody.get("uploadUrl").asString().replaceFirst("^https?://[^/]+", "");

        mvc.perform(put(uploadPath).content(JPEG_BYTES)).andExpect(status().isNoContent());
        mvc.perform(post("/api/public/events/{slug}/uploads/{mediaId}/finalize", slug, mediaId)
                        .header("X-Visitor-Id", visitorId))
                .andExpect(status().isOk());

        for (int i = 0; i < 40; i++) {
            MvcResult statusResult = mvc.perform(get("/api/public/events/{slug}/uploads/{mediaId}/status", slug, mediaId))
                    .andExpect(status().isOk())
                    .andReturn();
            String value = json.readTree(statusResult.getResponse().getContentAsByteArray()).get("status").asString();
            if ("READY".equals(value)) return mediaId;
            Thread.sleep(250);
        }
        throw new IllegalStateException("Media never became READY: " + mediaId);
    }
}
