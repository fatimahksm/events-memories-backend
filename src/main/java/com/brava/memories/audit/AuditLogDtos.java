package com.brava.memories.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuditLogDtos {
    private AuditLogDtos() {}
    public record Item(UUID id, String actorEmail, String action, String targetType, UUID targetId, String details, Instant createdAt) {}
    public record Page(List<Item> items, int page, int size, long totalElements, int totalPages) {}
    public static Item item(AuditLog a) { return new Item(a.getId(), a.getActorEmail(), a.getAction(), a.getTargetType(), a.getTargetId(), a.getDetails(), a.getCreatedAt()); }
}
