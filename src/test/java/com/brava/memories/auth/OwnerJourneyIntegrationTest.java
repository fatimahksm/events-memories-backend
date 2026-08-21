package com.brava.memories.auth;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OwnerJourneyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void ownerCanRegisterAuthenticateCreateAndOpenEvent() throws Exception {
        String suffix = UUID.randomUUID().toString();
        var registration = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "displayName", "Journey Owner",
                                "email", "journey-" + suffix + "@example.com",
                                "password", "StrongPass123!"))))
                .andExpect(status().isCreated())
                .andExpect(cookie().httpOnly("access_token", true))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andReturn();

        Cookie auth = registration.getResponse().getCookie("access_token");
        mvc.perform(get("/api/auth/me").cookie(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Journey Owner"));

        String slug = "journey-" + suffix;
        Instant expiry = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant deleteAt = expiry.plus(14, ChronoUnit.DAYS);
        var creation = mvc.perform(post("/api/owner/events")
                        .cookie(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "names", "Test Celebration",
                                "quote", "A complete owner journey",
                                "namesAr", "احتفال تجريبي",
                                "quoteAr", "رحلة كاملة لصاحب المناسبة",
                                "expiresAt", expiry.toString(),
                                "mediaDeleteAt", deleteAt.toString(),
                                "slug", slug))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value(slug))
                .andReturn();

        String eventId = json.readTree(creation.getResponse().getContentAsByteArray()).get("id").asString();
        mvc.perform(put("/api/owner/events/{eventId}/theme", eventId)
                        .cookie(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.ofEntries(
                                Map.entry("templateKey", "minimal"),
                                Map.entry("backgroundImageUrl", "http://localhost:8080/api/public/theme-assets/test"),
                                Map.entry("primaryColor", "#102A56"),
                                Map.entry("accentColor", "#36A7FF"),
                                Map.entry("textColor", "#FFFFFF"),
                                Map.entry("overlayOpacity", 0.35),
                                Map.entry("fontFamily", "Inter, sans-serif"),
                                Map.entry("buttonRadiusPx", 24),
                                Map.entry("colorMode", "LIGHT"),
                                Map.entry("backgroundPositionX", 50),
                                Map.entry("backgroundPositionY", 30),
                                Map.entry("backgroundFit", "COVER")))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/public/events/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").value("Test Celebration"))
                .andExpect(jsonPath("$.theme.backgroundImageUrl").value("http://localhost:8080/api/public/theme-assets/test"))
                .andExpect(jsonPath("$.theme.primaryColor").value("#102A56"))
                .andExpect(jsonPath("$.theme.accentColor").value("#36A7FF"))
                .andExpect(jsonPath("$.theme.colorMode").value("LIGHT"))
                .andExpect(jsonPath("$.theme.backgroundPositionY").value(30))
                .andExpect(jsonPath("$.namesAr").value("احتفال تجريبي"))
                .andExpect(jsonPath("$.quoteAr").value("رحلة كاملة لصاحب المناسبة"));

        mvc.perform(get("/api/public/events/{slug}/album", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.hasMore").value(false));
    }
}
