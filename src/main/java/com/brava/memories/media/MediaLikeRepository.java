package com.brava.memories.media;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public interface MediaLikeRepository extends JpaRepository<MediaLike, UUID>{
 interface LikeCount { UUID getMediaId(); long getLikeCount(); }
 boolean existsByMediaIdAndVisitorHash(UUID mediaId,String visitorHash);
 Optional<MediaLike> findByMediaIdAndVisitorHash(UUID mediaId,String visitorHash);
 long countByMediaId(UUID mediaId);
 @Query("select l.media.id as mediaId, count(l.id) as likeCount from MediaLike l where l.media.id in :ids group by l.media.id")
 List<LikeCount> countForMediaIds(@Param("ids") Collection<UUID> ids);
 @Modifying @Transactional
 @Query(value="insert into media_like(id,media_id,visitor_hash,created_at) values (:id,:mediaId,:visitorHash,now()) on conflict (media_id,visitor_hash) do nothing",nativeQuery=true)
 int insertIfAbsent(@Param("id") UUID id,@Param("mediaId") UUID mediaId,@Param("visitorHash") String visitorHash);
}
