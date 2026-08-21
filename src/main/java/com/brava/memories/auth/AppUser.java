package com.brava.memories.auth;

import jakarta.persistence.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {
    private static final SecureRandom RANDOM = new SecureRandom();
    @Id private UUID id;
    @Column(nullable=false, unique=true, length=190) private String email;
    @Column(name="password_hash", nullable=false) private String passwordHash;
    @Column(name="display_name", nullable=false, length=120) private String displayName;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private UserRole role;
    @Column(nullable=false) private boolean enabled;
    @Column(name="access_token", unique=true, length=64) private String accessToken;
    @Column(name="created_at", nullable=false) private Instant createdAt;

    protected AppUser() {}
    public AppUser(UUID id, String email, String passwordHash, String displayName, UserRole role) {
        this.id=id; this.email=email.trim().toLowerCase(Locale.ROOT); this.passwordHash=passwordHash; this.displayName=displayName.trim(); this.role=role; this.enabled=true; this.accessToken=generateToken(); this.createdAt=Instant.now();
    }
    public UUID getId(){return id;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;}
    public String getDisplayName(){return displayName;} public UserRole getRole(){return role;} public boolean isEnabled(){return enabled;} public String getAccessToken(){return accessToken;} public Instant getCreatedAt(){return createdAt;}
    public void setPasswordHash(String v){passwordHash=v;} public void setDisplayName(String v){displayName=v;} public void setEnabled(boolean v){enabled=v;}
    public void regenerateAccessToken(){accessToken=generateToken();}
    private static String generateToken(){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
}
