package com.brava.memories.media;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="media_like", uniqueConstraints=@UniqueConstraint(name="uq_media_like", columnNames={"media_id","visitor_hash"}))
public class MediaLike {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="media_id") private Media media;
 @Column(name="visitor_hash", nullable=false, length=128) private String visitorHash;
 @Column(name="created_at", nullable=false) private Instant createdAt;
 protected MediaLike(){} public MediaLike(UUID id, Media media, String visitorHash){this.id=id;this.media=media;this.visitorHash=visitorHash;this.createdAt=Instant.now();}
 public UUID getId(){return id;}
}
