package com.brava.memories.event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.*;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {
    Optional<Event> findBySlug(String slug);
    List<Event> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Page<Event> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByActiveTrue();
    boolean existsBySlug(String slug);
    List<Event> findByMediaDeleteAtBefore(Instant now);
    List<Event> findByMediaDeleteAtBeforeAndRetentionStatusIn(Instant now, Collection<EventRetentionStatus> statuses);

    static Specification<Event> filtered(String search, UUID ownerId, String dateField, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("names")), like),
                    cb.like(cb.lower(root.get("slug")), like),
                    cb.like(cb.lower(root.get("owner").get("displayName")), like),
                    cb.like(cb.lower(root.get("owner").get("email")), like)
                ));
            }
            if (ownerId != null) predicates.add(cb.equal(root.get("owner").get("id"), ownerId));
            if ("EVENT_DATE".equals(dateField)) {
                if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), from));
                if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), to));
            } else if ("EXPIRES_AT".equals(dateField)) {
                if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("expiresAt"), from.atStartOfDay(ZoneOffset.UTC).toInstant()));
                if (to != null) predicates.add(cb.lessThan(root.get("expiresAt"), to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
            } else if ("CREATED_AT".equals(dateField)) {
                if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay(ZoneOffset.UTC).toInstant()));
                if (to != null) predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
