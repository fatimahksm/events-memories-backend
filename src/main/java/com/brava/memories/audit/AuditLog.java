package com.brava.memories.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id private UUID id;
    @Column(name = "actor_id") private UUID actorId;
    @Column(name = "actor_email", nullable = false, length = 190) private String actorEmail;
    @Column(nullable = false, length = 60) private String action;
    @Column(name = "target_type", nullable = false, length = 40) private String targetType;
    @Column(name = "target_id") private UUID targetId;
    @Column(length = 500) private String details;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected AuditLog() {}
    public AuditLog(UUID id, UUID actorId, String actorEmail, String action, String targetType, UUID targetId, String details) {
        this.id = id; this.actorId = actorId; this.actorEmail = actorEmail; this.action = action;
        this.targetType = targetType; this.targetId = targetId; this.details = details; this.createdAt = Instant.now();
    }

    public UUID getId() { return id; } public UUID getActorId() { return actorId; } public String getActorEmail() { return actorEmail; }
    public String getAction() { return action; } public String getTargetType() { return targetType; } public UUID getTargetId() { return targetId; }
    public String getDetails() { return details; } public Instant getCreatedAt() { return createdAt; }
}
