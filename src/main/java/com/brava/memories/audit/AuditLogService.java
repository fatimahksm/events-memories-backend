package com.brava.memories.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository repo;
    public AuditLogService(AuditLogRepository repo) { this.repo = repo; }

    @Transactional
    public void record(UUID actorId, String actorEmail, String action, String targetType, UUID targetId, String details) {
        try {
            repo.save(new AuditLog(UUID.randomUUID(), actorId, actorEmail, action, targetType, targetId, details));
        } catch (Exception ex) {
            log.warn("Failed to write audit log entry for action {}: {}", action, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AuditLogDtos.Page page(int page, int size) {
        var result = repo.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100)));
        return new AuditLogDtos.Page(result.getContent().stream().map(AuditLogDtos::item).toList(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
