package com.brava.memories.media;
import com.brava.memories.common.exception.AppException;
import com.brava.memories.config.AppProperties;
import com.brava.memories.event.*;
import com.brava.memories.storage.ObjectStorage;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
public class PublicMediaService {
 private final EventService events; private final MediaRepository media; private final MediaLikeRepository likes; private final ObjectStorage storage; private final AppProperties props;
 public PublicMediaService(EventService events,MediaRepository media,MediaLikeRepository likes,ObjectStorage storage,AppProperties props){this.events=events;this.media=media;this.likes=likes;this.storage=storage;this.props=props;}

 @Transactional(readOnly=true)
 public MediaDtos.CursorPage album(String slug,String cursor,int size){
   Event e=events.requirePublic(slug);int safeSize=Math.min(Math.max(size,1),50);Cursor c=decodeCursor(cursor);
   Pageable page=PageRequest.of(0,safeSize+1);
   List<Media> rows=c==null
           ?media.findPublicFirstPage(e.getId(),MediaStatus.READY,MediaVisibility.PUBLIC,page)
           :media.findPublicAfterCursor(e.getId(),MediaStatus.READY,MediaVisibility.PUBLIC,c.createdAt(),c.id(),page);
   boolean hasMore=rows.size()>safeSize;if(hasMore)rows=rows.subList(0,safeSize);
   Map<UUID,Long> counts=likeCounts(rows);
   List<MediaDtos.Item> items=rows.stream().map(m->item(m,counts.getOrDefault(m.getId(),0L))).toList();
   String next=hasMore&&!rows.isEmpty()?encodeCursor(rows.get(rows.size()-1)):null;
   return new MediaDtos.CursorPage(items,next,hasMore);
 }

 @Transactional(readOnly=true)
 public MediaDtos.Page albumPage(String slug,int page,int size){
   Event e=events.requirePublic(slug);int safeSize=Math.min(Math.max(size,1),60);
   Page<Media> result=media.findByEventIdAndStatusAndVisibilityOrderByCreatedAtDesc(e.getId(),MediaStatus.READY,MediaVisibility.PUBLIC,PageRequest.of(Math.max(0,page),safeSize));
   Map<UUID,Long> counts=likeCounts(result.getContent());
   List<MediaDtos.Item> items=result.getContent().stream().map(m->item(m,counts.getOrDefault(m.getId(),0L))).toList();
   return new MediaDtos.Page(items,result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages());
 }

 @Transactional
 public MediaDtos.LikeResponse toggleLike(String slug,UUID id,String visitorId){
   Event e=events.requirePublic(slug);if(visitorId==null||visitorId.length()<8||visitorId.length()>128)throw new AppException("INVALID_VISITOR","Invalid visitor identifier",HttpStatus.BAD_REQUEST);
   Media m=media.findById(id).filter(x->x.getEvent().getId().equals(e.getId())&&x.getStatus()==MediaStatus.READY&&x.getVisibility()==MediaVisibility.PUBLIC).orElseThrow(()->new AppException("MEDIA_NOT_FOUND","Media not found",HttpStatus.NOT_FOUND));
   String hash=hash(visitorId);likes.insertIfAbsent(UUID.randomUUID(),m.getId(),hash);return new MediaDtos.LikeResponse(likes.countByMediaId(id),true);
 }

 private Map<UUID,Long> likeCounts(List<Media> rows){if(rows.isEmpty())return Map.of();Map<UUID,Long> out=new HashMap<>();likes.countForMediaIds(rows.stream().map(Media::getId).toList()).forEach(x->out.put(x.getMediaId(),x.getLikeCount()));return out;}
 private MediaDtos.Item item(Media m,long count){return new MediaDtos.Item(m.getId(),m.getMediaType().name(),m.getMimeType(),m.getVisibility().name(),m.getGuestName(),m.getStatus().name(),storage.createDownloadUrl(m.getStorageKey(),Duration.ofMinutes(20)),count,m.getCreatedAt());}
 private String encodeCursor(Media m){String raw=m.getCreatedAt()+"|"+m.getId();return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));}
 private Cursor decodeCursor(String encoded){if(encoded==null||encoded.isBlank())return null;try{String raw=new String(Base64.getUrlDecoder().decode(encoded),StandardCharsets.UTF_8);String[] p=raw.split("\\|",2);return new Cursor(Instant.parse(p[0]),UUID.fromString(p[1]));}catch(Exception ex){throw new AppException("INVALID_CURSOR","Invalid album cursor",HttpStatus.BAD_REQUEST);}}
 private String hash(String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(props.security().visitorHashSecret().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private record Cursor(Instant createdAt,UUID id){}
}
