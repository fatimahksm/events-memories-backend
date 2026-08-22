package com.brava.memories.media;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface MediaRepository extends JpaRepository<Media, UUID>, JpaSpecificationExecutor<Media> {
    Page<Media> findByEventIdOrderByCreatedAtDesc(UUID eventId, Pageable pageable);

    @Query(
      value = """
        select m from Media m
        left join MediaLike l on l.media = m
        where m.event.id = :eventId and m.status = :status and m.visibility = :visibility
        group by m
        order by count(l.id) desc, m.createdAt desc
        """,
      countQuery = """
        select count(m) from Media m
        where m.event.id = :eventId and m.status = :status and m.visibility = :visibility
        """
    )
    Page<Media> findPublicOrderedByLikes(@Param("eventId") UUID eventId, @Param("status") MediaStatus status, @Param("visibility") MediaVisibility visibility, Pageable pageable);
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

    static Specification<Media> filtered(UUID eventId, MediaVisibility visibility, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("event").get("id"), eventId));
            if (visibility != null) predicates.add(cb.equal(root.get("visibility"), visibility));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            if (query != null) query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

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
