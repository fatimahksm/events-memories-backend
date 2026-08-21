package com.brava.memories.event;
import com.brava.memories.auth.*;
import com.brava.memories.common.exception.AppException;
import com.brava.memories.common.util.SlugUtil;
import com.brava.memories.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class EventService {
 private final EventRepository events; private final AppUserRepository users; private final AppProperties props;
 public EventService(EventRepository events,AppUserRepository users,AppProperties props){this.events=events;this.users=users;this.props=props;}
 @Transactional(readOnly=true) public Event requirePublic(String slug){Event e=events.findBySlug(slug).orElseThrow(()->new AppException("EVENT_NOT_FOUND","Event not found",HttpStatus.NOT_FOUND));if(e.isExpired()) throw new AppException("EVENT_EXPIRED","This event is no longer accepting guests",HttpStatus.GONE);return e;}
 @Transactional(readOnly=true) public Event requireById(UUID id){return events.findById(id).orElseThrow(()->new AppException("EVENT_NOT_FOUND","Event not found",HttpStatus.NOT_FOUND));}
 @Transactional(readOnly=true) public Event requireOwned(UUID eventId,String principal,boolean admin){Event e=requireById(eventId);if(!admin&&!e.getOwner().getId().toString().equals(principal))throw new AppException("FORBIDDEN","You cannot access this event",HttpStatus.FORBIDDEN);return e;}
 @Transactional public Event create(EventDtos.Create req){AppUser owner=requireEnabledOwner(req.ownerId());return create(owner,req.names(),req.quote(),req.namesAr(),req.quoteAr(),req.eventDate(),req.expiresAt(),req.mediaDeleteAt(),req.slug());}
 @Transactional public Event createWithTheme(EventDtos.Create req,EventDtos.UpdateTheme theme){Event event=create(req);event.updateTheme(theme.templateKey(),theme.backgroundImageUrl(),theme.primaryColor(),theme.accentColor(),theme.textColor(),theme.overlayOpacity(),theme.fontFamily(),theme.buttonRadiusPx(),theme.colorMode(),theme.backgroundPositionX(),theme.backgroundPositionY(),theme.backgroundFit());return event;}
 @Transactional public Event createForOwner(UUID ownerId,EventDtos.OwnerCreate req){AppUser owner=requireEnabledOwner(ownerId);return create(owner,req.names(),req.quote(),req.namesAr(),req.quoteAr(),req.eventDate(),req.expiresAt(),req.mediaDeleteAt(),req.slug());}
 private AppUser requireEnabledOwner(UUID ownerId){return users.findById(ownerId).filter(u->u.getRole()==UserRole.OWNER&&u.isEnabled()).orElseThrow(()->new AppException("OWNER_NOT_FOUND","Active owner not found",HttpStatus.NOT_FOUND));}
 private Event create(AppUser owner,String names,String quote,String namesAr,String quoteAr,java.time.LocalDate eventDate,Instant expiresAt,Instant requestedDeleteAt,String requestedSlug){if(!expiresAt.isAfter(Instant.now()))throw new AppException("INVALID_EXPIRY_DATE","Event expiry must be in the future",HttpStatus.BAD_REQUEST);String base=(requestedSlug==null||requestedSlug.isBlank())?SlugUtil.slugify(names):SlugUtil.slugify(requestedSlug);String slug=uniqueSlug(base);Instant deleteAt=requestedDeleteAt!=null?requestedDeleteAt:expiresAt.plusSeconds(props.retention().defaultDaysAfterExpiry()*86400L);validateRetention(expiresAt,deleteAt);return events.save(new Event(UUID.randomUUID(),owner,slug,names.trim(),clean(quote),clean(namesAr),clean(quoteAr),eventDate,expiresAt,deleteAt));}
 private String uniqueSlug(String base){String candidate=base;int i=2;while(events.existsBySlug(candidate))candidate=base+"-"+i++;return candidate;}
 @Transactional public Event update(UUID id,EventDtos.Update req){validateRetention(req.expiresAt(),req.mediaDeleteAt());Event e=requireById(id);e.updateBasics(req.names().trim(),clean(req.quote()),clean(req.namesAr()),clean(req.quoteAr()),req.eventDate(),req.expiresAt(),req.mediaDeleteAt(),req.active());return events.save(e);}
 @Transactional public Event updateTheme(Event e,EventDtos.UpdateTheme req){e.updateTheme(req.templateKey(),req.backgroundImageUrl(),req.primaryColor(),req.accentColor(),req.textColor(),req.overlayOpacity(),req.fontFamily(),req.buttonRadiusPx(),req.colorMode(),req.backgroundPositionX(),req.backgroundPositionY(),req.backgroundFit());return events.save(e);}

 @Transactional public Event extendRetention(UUID id,int days){
  if(days<1||days>365) throw new AppException("INVALID_RETENTION_EXTENSION","Retention extension must be between 1 and 365 days",HttpStatus.BAD_REQUEST);
  Event e=requireById(id);
  if(e.getRetentionStatus()==EventRetentionStatus.ARCHIVED) throw new AppException("EVENT_ALREADY_ARCHIVED","Archived event media cannot be restored",HttpStatus.CONFLICT);
  Instant base=e.getMediaDeleteAt().isAfter(Instant.now())?e.getMediaDeleteAt():Instant.now();
  e.extendRetention(base.plusSeconds(days*86400L));
  return e;
 }
 public EventDtos.PublicEvent publicDto(Event e){return new EventDtos.PublicEvent(e.getId(),e.getSlug(),e.getNames(),e.getQuote(),e.getNamesAr(),e.getQuoteAr(),e.getEventDate(),e.getExpiresAt(),EventDtos.theme(e));}
 private void validateRetention(Instant expiresAt,Instant deleteAt){if(!deleteAt.isAfter(expiresAt))throw new AppException("INVALID_RETENTION_DATE","Media delete date must be after expiry",HttpStatus.BAD_REQUEST);}
 private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
}
