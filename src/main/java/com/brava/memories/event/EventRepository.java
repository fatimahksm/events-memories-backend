package com.brava.memories.event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.*;
public interface EventRepository extends JpaRepository<Event, UUID> {
    Optional<Event> findBySlug(String slug);
    Optional<Event> findByAccessToken(String accessToken);
    List<Event> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Page<Event> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByActiveTrue();
    boolean existsBySlug(String slug);
    List<Event> findByMediaDeleteAtBefore(Instant now);
    List<Event> findByMediaDeleteAtBeforeAndRetentionStatusIn(Instant now, Collection<EventRetentionStatus> statuses);
}
