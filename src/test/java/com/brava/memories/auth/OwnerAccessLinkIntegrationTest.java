package com.brava.memories.auth;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OwnerAccessLinkIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppUserRepository users;

    @Test
    void ownerCanAuthenticateWithAccessTokenAndInvalidTokenIsRejected() throws Exception {
        String suffix = UUID.randomUUID().toString();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "displayName", "Link Owner",
                                "email", "link-" + suffix + "@example.com",
                                "password", "StrongPass123!"))))
                .andExpect(status().isCreated());

        AppUser owner = users.findByEmailIgnoreCase("link-" + suffix + "@example.com").orElseThrow();
        String token = owner.getAccessToken();

        mvc.perform(post("/api/auth/owner-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("access_token", true))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.email").value("link-" + suffix + "@example.com"));

        mvc.perform(post("/api/auth/owner-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("token", "not-a-real-token"))))
                .andExpect(status().isUnauthorized());
    }
}
