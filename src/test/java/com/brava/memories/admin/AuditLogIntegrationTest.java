package com.brava.memories.admin;

import com.brava.memories.auth.AppUser;
import com.brava.memories.auth.AppUserRepository;
import com.brava.memories.auth.UserRole;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.rate-limit.login-attempts-per-five-minutes=1000")
class AuditLogIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void creatingAnOwnerWritesAnAuditLogEntryVisibleToTheSuperAdmin() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String adminEmail = "audit-admin-" + suffix + "@example.com";
        users.save(new AppUser(UUID.randomUUID(), adminEmail, encoder.encode("AuditAdminPass123!"), "Audit Admin", UserRole.SUPER_ADMIN));

        var login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", adminEmail, "password", "AuditAdminPass123!"))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie auth = login.getResponse().getCookie("access_token");

        String ownerEmail = "audit-owner-" + suffix + "@example.com";
        mvc.perform(post("/api/admin/owners")
                        .cookie(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "displayName", "Audit Owner",
                                "email", ownerEmail,
                                "password", "OwnerPass1234"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/admin/audit-log").cookie(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("OWNER_CREATED"))
                .andExpect(jsonPath("$.items[0].actorEmail").value(adminEmail))
                .andExpect(jsonPath("$.items[0].details").value(ownerEmail));
    }
}
