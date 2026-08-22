package com.brava.memories.auth;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.rate-limit.login-attempts-per-five-minutes=1000")
class PasswordResetIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppUserRepository users;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired PasswordEncoder encoder;

    private AppUser register(String suffix) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "displayName", "Reset Test",
                                "email", "reset-" + suffix + "@example.com",
                                "password", "OriginalPass123!"))))
                .andExpect(status().isCreated());
        return users.findByEmailIgnoreCase("reset-" + suffix + "@example.com").orElseThrow();
    }

    @Test
    void forgotPasswordAlwaysReturnsNoContentEvenForUnknownEmail() throws Exception {
        mvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", "no-such-user@example.com"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void forgotPasswordIssuesAUsableResetTokenThatChangesThePassword() throws Exception {
        String suffix = UUID.randomUUID().toString();
        AppUser user = register(suffix);

        mvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", user.getEmail()))))
                .andExpect(status().isNoContent());

        PasswordResetToken token = resetTokens.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .findFirst().orElseThrow();
        assertThat(token.isValid()).isTrue();

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("token", token.getToken(), "newPassword", "BrandNewPass456!"))))
                .andExpect(status().isNoContent());

        AppUser refreshed = users.findById(user.getId()).orElseThrow();
        assertThat(encoder.matches("BrandNewPass456!", refreshed.getPasswordHash())).isTrue();
        assertThat(encoder.matches("OriginalPass123!", refreshed.getPasswordHash())).isFalse();

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("token", token.getToken(), "newPassword", "AnotherPass789!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordRejectsAnUnknownToken() throws Exception {
        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("token", "not-a-real-token", "newPassword", "WhateverPass123!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expiredTokenIsRejected() {
        AppUser user = users.save(new AppUser(UUID.randomUUID(), "expired-" + UUID.randomUUID() + "@example.com", encoder.encode("Pass12345!"), "Expired Test", UserRole.OWNER));
        PasswordResetToken expired = resetTokens.save(new PasswordResetToken(UUID.randomUUID(), user, Duration.ofSeconds(-60)));
        assertThat(expired.isValid()).isFalse();
    }
}
