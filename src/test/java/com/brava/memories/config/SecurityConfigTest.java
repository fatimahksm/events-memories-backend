package com.brava.memories.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {
    private static final String JWT_SECRET = "test-secret-that-is-at-least-32-bytes-long";

    @Test
    void configuredEncoderProducesTokenAcceptedByConfiguredDecoder() {
        AppProperties properties = properties();
        SecurityConfig config = new SecurityConfig();
        JwtEncoder encoder = config.jwtEncoder(properties);
        JwtDecoder decoder = config.jwtDecoder(properties);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("event-memories")
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(1)))
                .subject("user-id")
                .claim("roles", List.of("SUPER_ADMIN"))
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(() -> JwsAlgorithms.HS256).build(), claims)).getTokenValue();

        assertThat(decoder.decode(token).getSubject()).isEqualTo("user-id");
        assertThat(decoder.decode(token).getClaimAsStringList("roles"))
                .containsExactly("SUPER_ADMIN");
    }

    private AppProperties properties() {
        return new AppProperties(
                "http://localhost:3000",
                List.of("http://localhost:3000"),
                new AppProperties.Security(JWT_SECRET, Duration.ofHours(8), false, "visitor-secret"),
                null,
                null,
                null,
                null);
    }
}
