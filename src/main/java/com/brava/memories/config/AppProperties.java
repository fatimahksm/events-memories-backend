package com.brava.memories.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        List<String> corsOrigins,
        Security security,
        Storage storage,
        Scanning scanning,
        Retention retention,
        Bootstrap bootstrap,
        Sentry sentry,
        Mail mail
) {
    public record Security(String jwtSecret, Duration jwtTtl, boolean cookieSecure, String visitorHashSecret) {}
    public record Storage(String provider, String localRoot, String publicBaseUrl, String r2AccountId, String r2AccessKeyId, String r2SecretAccessKey, String r2Bucket) {}
    public record Scanning(boolean enabled, String host, int port, Duration timeout) {}
    public record Retention(int defaultDaysAfterExpiry, int warningDaysBeforeDelete, int maxDeletionAttempts) {}
    public record Bootstrap(String adminEmail, String adminPassword) {}
    public record Sentry(String dsn, String environment) {}
    public record Mail(String host, int port, String username, String password, String fromAddress, Duration resetTokenTtl) {}
}
