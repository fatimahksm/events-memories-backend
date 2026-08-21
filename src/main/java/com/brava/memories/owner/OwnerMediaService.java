package com.brava.memories.owner;
import com.brava.memories.common.exception.AppException;
import com.brava.memories.event.*;
import com.brava.memories.media.*;
import com.brava.memories.storage.ObjectStorage;
import com.brava.memories.wish.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.*;
@Service
public class OwnerMediaService {
 private final EventService events; private final MediaRepository media; private final MediaLikeRepository likes; private final ObjectStorage storage; private final WishService wishes;
 public OwnerMediaService(EventService events,MediaRepository media,MediaLikeRepository likes,ObjectStorage storage,WishService wishes){this.events=events;this.media=media;this.likes=likes;this.storage=storage;this.wishes=wishes;}
 @Transactional(readOnly=true) public MediaDtos.Page list(UUID eventId,String principal,boolean admin,int page,int size,MediaVisibility visibility,java.time.Instant from,java.time.Instant to){Event e=events.requireOwned(eventId,principal,admin);Page<Media> r=media.findAll(MediaRepository.filtered(e.getId(),visibility,from,to),PageRequest.of(Math.max(0,page),Math.min(Math.max(1,size),100)));return new MediaDtos.Page(r.getContent().stream().map(this::item).toList(),r.getNumber(),r.getSize(),r.getTotalElements(),r.getTotalPages());}
 @Transactional public MediaDtos.Item visibility(UUID eventId,UUID mediaId,String principal,boolean admin,MediaVisibility visibility){Event e=events.requireOwned(eventId,principal,admin);Media m=media.findById(mediaId).filter(x->x.getEvent().getId().equals(e.getId())).orElseThrow(()->new AppException("MEDIA_NOT_FOUND","Media not found",HttpStatus.NOT_FOUND));m.setVisibility(visibility);return item(m);}
 @Transactional public void delete(UUID eventId,UUID mediaId,String principal,boolean admin){Event e=events.requireOwned(eventId,principal,admin);Media m=media.findById(mediaId).filter(x->x.getEvent().getId().equals(e.getId())).orElseThrow(()->new AppException("MEDIA_NOT_FOUND","Media not found",HttpStatus.NOT_FOUND));storage.delete(m.getStorageKey());media.delete(m);}
 public List<WishDtos.Item> wishes(UUID eventId,String principal,boolean admin){events.requireOwned(eventId,principal,admin);return wishes.forEvent(eventId);}
 private MediaDtos.Item item(Media m){String url=(m.getStatus()==MediaStatus.READY)?storage.createDownloadUrl(m.getStorageKey(),Duration.ofMinutes(30)):null;return new MediaDtos.Item(m.getId(),m.getMediaType().name(),m.getMimeType(),m.getVisibility().name(),m.getGuestName(),m.getStatus().name(),url,likes.countByMediaId(m.getId()),m.getCreatedAt());}
}
