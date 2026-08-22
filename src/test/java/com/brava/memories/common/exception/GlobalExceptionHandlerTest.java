package com.brava.memories.common.exception;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GlobalExceptionHandlerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void aWrongHttpMethodOnAnExistingPathReturns405NotAGeneric500() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String adminEmail = "handler-admin-" + suffix + "@example.com";
        users.save(new AppUser(UUID.randomUUID(), adminEmail, encoder.encode("HandlerAdminPass123!"), "Handler Admin", UserRole.SUPER_ADMIN));
        var login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", adminEmail, "password", "HandlerAdminPass123!"))))
                .andExpect(status().isOk()).andReturn();
        Cookie auth = login.getResponse().getCookie("access_token");

        // /api/owner/events supports GET but not POST (owners no longer self-create events) —
        // this should be a clean 405, not an "unexpected error" 500 from the catch-all handler
        // swallowing the framework's own HttpRequestMethodNotSupportedException.
        mvc.perform(post("/api/owner/events").cookie(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("names", "x"))))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }
}
