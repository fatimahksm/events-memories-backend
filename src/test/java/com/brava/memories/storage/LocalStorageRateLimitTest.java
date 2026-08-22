package com.brava.memories.storage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test: many guests uploading over the same venue Wi-Fi share one client IP.
 * The local-storage PUT (the on-disk stand-in for a direct-to-R2 presigned upload, which
 * never touches this server in a real deployment) must not be throttled by the generic
 * public-mutations-per-minute limiter — that limiter has no per-visitor key for this path
 * (a real presigned PUT can't carry our custom header either), so counting it here would
 * collapse every guest on one IP into a single shared bucket.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.rate-limit.public-mutations-per-minute=5")
class LocalStorageRateLimitTest {
    @Autowired MockMvc mvc;

    @Test
    void manyConcurrentGuestUploadsFromOneIpAreNotThrottledByThePublicMutationLimit() throws Exception {
        for (int i = 0; i < 20; i++) {
            String key = "test/rate-limit-check/" + i;
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8));
            mvc.perform(put("/api/public/local-storage/{encoded}", encoded).content("bytes".getBytes()))
                    .andExpect(status().isNoContent());
        }
    }
}
