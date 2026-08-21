package com.brava.memories.media;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface MediaRepository extends JpaRepository<Media, UUID> {
    Page<Media> findByEventIdAndStatusAndVisibilityOrderByCreatedAtDesc(UUID eventId, MediaStatus status, MediaVisibility visibility, Pageable pageable);
    Page<Media> findByEventIdOrderByCreatedAtDesc(UUID eventId, Pageable pageable);
    List<Media> findByEventIdAndStatus(UUID eventId, MediaStatus status);
    List<Media> findByEventId(UUID eventId);
    Optional<Media> findByEventIdAndClientUploadId(UUID eventId, UUID clientUploadId);
    List<Media> findByStatusOrderByCreatedAtAsc(MediaStatus status, Pageable pageable);
    long countByStatus(MediaStatus status);
    long countByEventId(UUID eventId);
    @Query("select coalesce(sum(m.fileSize),0) from Media m") long totalStoredBytes();

    @Query("select m from Media m where m.status = :status and m.processingStartedAt < :before order by m.processingStartedAt asc")
    List<Media> findStaleProcessing(@Param("status") MediaStatus status,@Param("before") Instant before,Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Media m where m.id = :id")
    Optional<Media> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
      select m from Media m
      where m.event.id = :eventId
        and m.status = :status
        and m.visibility = :visibility
      order by m.createdAt desc, m.id desc
      """)
    List<Media> findPublicFirstPage(@Param("eventId") UUID eventId,
                                    @Param("status") MediaStatus status,
                                    @Param("visibility") MediaVisibility visibility,
                                    Pageable pageable);

    @Query("""
      select m from Media m
      where m.event.id = :eventId
        and m.status = :status
        and m.visibility = :visibility
        and (m.createdAt < :cursorCreatedAt or (m.createdAt = :cursorCreatedAt and m.id < :cursorId))
      order by m.createdAt desc, m.id desc
      """)
    List<Media> findPublicAfterCursor(@Param("eventId") UUID eventId,
                                      @Param("status") MediaStatus status,
                                      @Param("visibility") MediaVisibility visibility,
                                      @Param("cursorCreatedAt") Instant cursorCreatedAt,
                                      @Param("cursorId") UUID cursorId,
                                      Pageable pageable);
}
