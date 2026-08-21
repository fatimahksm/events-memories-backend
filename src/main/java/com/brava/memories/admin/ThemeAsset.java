package com.brava.memories.admin;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "theme_asset")
public class ThemeAsset {
    @Id private UUID id;
    @Column(nullable=false, length=500) private String url;
    @Column(name="content_type", nullable=false, length=100) private String contentType;
    @Column(name="size_bytes", nullable=false) private long sizeBytes;
    @Column(name="created_at", nullable=false) private Instant createdAt;

    protected ThemeAsset() {}
    public ThemeAsset(UUID id, String url, String contentType, long sizeBytes) {
        this.id=id; this.url=url; this.contentType=contentType; this.sizeBytes=sizeBytes; this.createdAt=Instant.now();
    }
    public UUID getId(){return id;} public String getUrl(){return url;} public String getContentType(){return contentType;}
    public long getSizeBytes(){return sizeBytes;} public Instant getCreatedAt(){return createdAt;}
}
