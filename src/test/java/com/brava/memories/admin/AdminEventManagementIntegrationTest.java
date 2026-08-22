package com.brava.memories.admin;

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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.rate-limit.login-attempts-per-five-minutes=1000")
class AdminEventManagementIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void searchOwnerAndDateFiltersNarrowTheEventsPage() throws Exception {
        Cookie admin = loginAsFreshAdmin();
        String suffix = UUID.randomUUID().toString();
        String ownerAId = createOwner(admin, "Filter Owner A " + suffix, "filter-owner-a-" + suffix + "@example.com");
        String ownerBId = createOwner(admin, "Filter Owner B " + suffix, "filter-owner-b-" + suffix + "@example.com");

        String uniqueName = "Zebra Wedding " + suffix;
        createEvent(admin, ownerAId, uniqueName, "2026-09-10", 5);
        createEvent(admin, ownerBId, "Other Event " + suffix, "2026-01-01", 5);

        // Search narrows to the one event whose name matches.
        JsonNode bySearch = pageResult(admin, "search", uniqueName);
        assertThat(bySearch.get("totalElements").asLong()).isEqualTo(1);
        assertThat(bySearch.get("items").get(0).get("event").get("names").asString()).isEqualTo(uniqueName);

        // Owner filter narrows to just that owner's events.
        JsonNode byOwner = pageResult(admin, "ownerId", ownerAId);
        assertThat(byOwner.get("totalElements").asLong()).isEqualTo(1);
        assertThat(byOwner.get("items").get(0).get("owner").get("id").asString()).isEqualTo(ownerAId);

        // Event-date filter narrows to events whose date falls in range.
        JsonNode byDate = pageResult(admin, "dateField", "EVENT_DATE", "from", "2026-09-01", "to", "2026-09-30");
        assertThat(byDate.get("totalElements").asLong()).isEqualTo(1);
        assertThat(byDate.get("items").get(0).get("event").get("names").asString()).isEqualTo(uniqueName);
    }

    @Test
    void extendingAccessPushesBothExpiryAndRetentionForward() throws Exception {
        Cookie admin = loginAsFreshAdmin();
        String suffix = UUID.randomUUID().toString();
        String ownerId = createOwner(admin, "Extend Owner " + suffix, "extend-owner-" + suffix + "@example.com");
        String eventId = createEvent(admin, ownerId, "Extend Event " + suffix, null, 3);

        MvcResult before = mvc.perform(get("/api/admin/events/page").cookie(admin).param("search", "Extend Event " + suffix))
                .andExpect(status().isOk()).andReturn();
        Instant expiresBefore = Instant.parse(json.readTree(before.getResponse().getContentAsByteArray()).get("items").get(0).get("event").get("expiresAt").asString());
        Instant deleteAtBefore = Instant.parse(json.readTree(before.getResponse().getContentAsByteArray()).get("items").get(0).get("event").get("mediaDeleteAt").asString());

        MvcResult extended = mvc.perform(post("/api/admin/events/{id}/extend", eventId).cookie(admin).param("days", "10"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(extended.getResponse().getContentAsByteArray());
        Instant expiresAfter = Instant.parse(body.get("expiresAt").asString());
        Instant deleteAtAfter = Instant.parse(body.get("mediaDeleteAt").asString());

        assertThat(expiresAfter).isAfter(expiresBefore);
        assertThat(deleteAtAfter).isAfterOrEqualTo(deleteAtBefore);
        assertThat(deleteAtAfter).isAfter(expiresAfter); // retention invariant never violated
    }

    @Test
    void disablingAnEventTakesItsPublicLinkOfflineAndReenablingRestoresIt() throws Exception {
        Cookie admin = loginAsFreshAdmin();
        String suffix = UUID.randomUUID().toString();
        String ownerId = createOwner(admin, "Toggle Owner " + suffix, "toggle-owner-" + suffix + "@example.com");
        String slug = "toggle-event-" + suffix;
        String eventId = createEvent(admin, ownerId, "Toggle Event " + suffix, null, 5, slug);

        mvc.perform(get("/api/public/events/{slug}", slug)).andExpect(status().isOk());

        mvc.perform(patch("/api/admin/events/{id}/active", eventId).cookie(admin).param("value", "false"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/public/events/{slug}", slug)).andExpect(status().isGone());

        mvc.perform(patch("/api/admin/events/{id}/active", eventId).cookie(admin).param("value", "true"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/public/events/{slug}", slug)).andExpect(status().isOk());
    }

    private JsonNode pageResult(Cookie admin, String... keyValuePairs) throws Exception {
        var request = get("/api/admin/events/page").cookie(admin);
        for (int i = 0; i < keyValuePairs.length; i += 2) request = request.param(keyValuePairs[i], keyValuePairs[i + 1]);
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsByteArray());
    }

    private Cookie loginAsFreshAdmin() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String adminEmail = "event-mgmt-admin-" + suffix + "@example.com";
        users.save(new AppUser(UUID.randomUUID(), adminEmail, encoder.encode("EventMgmtAdminPass123!"), "Event Mgmt Admin", UserRole.SUPER_ADMIN));
        var login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", adminEmail, "password", "EventMgmtAdminPass123!"))))
                .andExpect(status().isOk()).andReturn();
        return login.getResponse().getCookie("access_token");
    }

    private String createOwner(Cookie admin, String name, String email) throws Exception {
        var result = mvc.perform(post("/api/admin/owners")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("displayName", name, "email", email, "password", "OwnerPass1234"))))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsByteArray()).get("id").asString();
    }

    private String createEvent(Cookie admin, String ownerId, String names, String eventDate, int expiresInDays) throws Exception {
        return createEvent(admin, ownerId, names, eventDate, expiresInDays, null);
    }

    private String createEvent(Cookie admin, String ownerId, String names, String eventDate, int expiresInDays, String slug) throws Exception {
        java.util.Map<String, Object> body = new java.util.HashMap<>(Map.of(
                "ownerId", ownerId,
                "names", names,
                "expiresAt", Instant.now().plus(expiresInDays, ChronoUnit.DAYS).toString(),
                "mediaDeleteAt", Instant.now().plus(expiresInDays + 14L, ChronoUnit.DAYS).toString()));
        if (eventDate != null) body.put("eventDate", eventDate);
        if (slug != null) body.put("slug", slug);
        var result = mvc.perform(post("/api/admin/events")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(body)))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsByteArray()).get("id").asString();
    }
}
