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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventAccessLinkIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void eventAccessTokenIsStrictlyScopedToItsOwnEvent() throws Exception {
        String suffix = UUID.randomUUID().toString();
        var registration = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "displayName", "Scoped Owner",
                                "email", "scoped-" + suffix + "@example.com",
                                "password", "StrongPass123!"))))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie ownerAuth = registration.getResponse().getCookie("access_token");

        Instant expiry = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant deleteAt = expiry.plus(14, ChronoUnit.DAYS);

        var event1 = mvc.perform(post("/api/owner/events")
                        .cookie(ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "names", "Event One", "expiresAt", expiry.toString(),
                                "mediaDeleteAt", deleteAt.toString(), "slug", "scoped-one-" + suffix))))
                .andExpect(status().isCreated())
                .andReturn();
        var event1Json = json.readTree(event1.getResponse().getContentAsByteArray());
        String event1Id = event1Json.get("id").asString();
        String event1Token = event1Json.get("accessToken").asString();

        var event2 = mvc.perform(post("/api/owner/events")
                        .cookie(ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "names", "Event Two", "expiresAt", expiry.toString(),
                                "mediaDeleteAt", deleteAt.toString(), "slug", "scoped-two-" + suffix))))
                .andExpect(status().isCreated())
                .andReturn();
        String event2Id = json.readTree(event2.getResponse().getContentAsByteArray()).get("id").asString();

        // Exchange event1's token for a scoped session
        var access = mvc.perform(post("/api/auth/event-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("token", event1Token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.scopedEventId").value(event1Id))
                .andReturn();
        Cookie scopedAuth = access.getResponse().getCookie("access_token");

        // /owner/events returns ONLY event1, never event2
        mvc.perform(get("/api/owner/events").cookie(scopedAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(event1Id));

        // Can manage event1's content
        mvc.perform(put("/api/owner/events/{id}", event1Id)
                        .cookie(scopedAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("names", "Event One Renamed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").value("Event One Renamed"));

        // Cannot touch event2 at all - media list
        mvc.perform(get("/api/owner/events/{id}/media", event2Id).cookie(scopedAuth))
                .andExpect(status().isForbidden());

        // Cannot touch event2's content either
        mvc.perform(put("/api/owner/events/{id}", event2Id)
                        .cookie(scopedAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("names", "Hijacked"))))
                .andExpect(status().isForbidden());

        // Cannot create a new event with a scoped session
        mvc.perform(post("/api/owner/events")
                        .cookie(scopedAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "names", "Sneaky Third Event", "expiresAt", expiry.toString(),
                                "mediaDeleteAt", deleteAt.toString()))))
                .andExpect(status().isForbidden());

        // The full owner session still sees both events
        mvc.perform(get("/api/owner/events").cookie(ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // An invalid event token is rejected
        mvc.perform(post("/api/auth/event-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("token", "not-a-real-token"))))
                .andExpect(status().isUnauthorized());
    }
}
