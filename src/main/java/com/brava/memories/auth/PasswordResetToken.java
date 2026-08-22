package com.brava.memories.auth;

import com.brava.memories.common.util.TokenUtil;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private AppUser user;
    @Column(nullable = false, unique = true, length = 64) private String token;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "used_at") private Instant usedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected PasswordResetToken() {}
    public PasswordResetToken(UUID id, AppUser user, Duration ttl) {
        this.id = id; this.user = user; this.token = TokenUtil.generate();
        this.createdAt = Instant.now(); this.expiresAt = createdAt.plus(ttl);
    }

    public UUID getId() { return id; } public AppUser getUser() { return user; } public String getToken() { return token; }
    public Instant getExpiresAt() { return expiresAt; } public Instant getUsedAt() { return usedAt; }
    public boolean isValid() { return usedAt == null && Instant.now().isBefore(expiresAt); }
    public void markUsed() { usedAt = Instant.now(); }
}
