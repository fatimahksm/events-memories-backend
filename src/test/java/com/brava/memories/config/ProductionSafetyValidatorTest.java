package com.brava.memories.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSafetyValidatorTest {

    @Test
    void nonProdProfileSkipsAllChecks() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("default");
        assertThatCode(env, properties("ChangeMe123!", "change-this-visitor-hash-secret", "change-this"));
    }

    @Test
    void prodRejectsDefaultAdminPassword() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        assertThatThrownBy(() -> new ProductionSafetyValidator(env, properties("ChangeMe123!", "real-secret-value", "real-jwt-secret")).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_ADMIN_PASSWORD");
    }

    @Test
    void prodRejectsDefaultVisitorHashSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        assertThatThrownBy(() -> new ProductionSafetyValidator(env, properties("real-password", "change-this-visitor-hash-secret", "real-jwt-secret")).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VISITOR_HASH_SECRET");
    }

    @Test
    void prodAcceptsFullyConfiguredSecrets() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        new ProductionSafetyValidator(env, properties("real-password", "real-visitor-secret", "real-jwt-secret")).validate();
    }

    private void assertThatCode(MockEnvironment env, AppProperties properties) {
        assertThat(env.getActiveProfiles()).doesNotContain("prod-only-marker");
        new ProductionSafetyValidator(env, properties).validate();
    }

    private AppProperties properties(String adminPassword, String visitorHashSecret, String jwtSecret) {
        return new AppProperties(
                "http://localhost:3000",
                List.of("http://localhost:3000"),
                new AppProperties.Security(jwtSecret, Duration.ofHours(8), true, visitorHashSecret),
                new AppProperties.Storage("r2", null, "http://localhost:8080", "account", "key", "secret", "bucket"),
                new AppProperties.Scanning(true, "localhost", 3310, Duration.ofMinutes(2)),
                new AppProperties.Retention(30, 3, 5),
                new AppProperties.Bootstrap("admin@brava.test", adminPassword));
    }
}
