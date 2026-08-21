package com.brava.memories.owner;
import com.brava.memories.admin.AdminDtos;
import com.brava.memories.admin.ThemeAssetService;
import com.brava.memories.common.exception.AppException;
import com.brava.memories.event.*;
import com.brava.memories.media.*;
import com.brava.memories.wish.WishDtos;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;
@RestController @RequestMapping("/api/owner")
public class OwnerController {
 private final EventRepository eventRepo; private final EventService events; private final OwnerMediaService media; private final DownloadAllService downloads; private final ThemeAssetService themeAssets;
 public OwnerController(EventRepository eventRepo,EventService events,OwnerMediaService media,DownloadAllService downloads,ThemeAssetService themeAssets){this.eventRepo=eventRepo;this.events=events;this.media=media;this.downloads=downloads;this.themeAssets=themeAssets;}
 private boolean admin(Jwt jwt){return jwt.getClaimAsStringList("roles").contains("SUPER_ADMIN");}
 private UUID scopedEventId(Jwt jwt){String claim=jwt.getClaimAsString("eventId");return claim!=null?UUID.fromString(claim):null;}
 @GetMapping("/events") public List<EventDtos.Summary> events(@AuthenticationPrincipal Jwt jwt){
   UUID scoped=scopedEventId(jwt);
   if(scoped!=null)return eventRepo.findById(scoped).map(EventDtos::summary).map(List::of).orElse(List.of());
   if(admin(jwt))return eventRepo.findAll().stream().map(EventDtos::summary).toList();
   return eventRepo.findByOwnerIdOrderByCreatedAtDesc(UUID.fromString(jwt.getSubject())).stream().map(EventDtos::summary).toList();
 }
 @PostMapping("/events") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public EventDtos.Summary createEvent(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody EventDtos.OwnerCreate req){
   if(scopedEventId(jwt)!=null)throw new AppException("FORBIDDEN","This access link can only manage its own event",HttpStatus.FORBIDDEN);
   return EventDtos.summary(events.createForOwner(UUID.fromString(jwt.getSubject()),req));
 }
 @PutMapping("/events/{eventId}") public EventDtos.Summary content(@PathVariable UUID eventId,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody EventDtos.OwnerUpdate req){Event e=events.requireOwned(eventId,jwt.getSubject(),admin(jwt),scopedEventId(jwt));return EventDtos.summary(events.updateContent(e,req));}
 @PutMapping("/events/{eventId}/theme") public EventDtos.Summary theme(@PathVariable UUID eventId,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody EventDtos.UpdateTheme req){Event e=events.requireOwned(eventId,jwt.getSubject(),admin(jwt),scopedEventId(jwt));return EventDtos.summary(events.updateTheme(e,req));}
 @PostMapping(value="/theme-assets",consumes=org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public AdminDtos.AssetUpload uploadThemeAsset(@RequestPart("file") org.springframework.web.multipart.MultipartFile file){return themeAssets.upload(file);}
 @GetMapping("/events/{eventId}/media") public MediaDtos.Page media(@PathVariable UUID eventId,@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size,@RequestParam(required=false) MediaVisibility visibility,@RequestParam(required=false) java.time.Instant from,@RequestParam(required=false) java.time.Instant to){return media.list(eventId,jwt.getSubject(),admin(jwt),scopedEventId(jwt),page,size,visibility,from,to);}
 @PatchMapping("/events/{eventId}/media/{mediaId}/visibility") public MediaDtos.Item visibility(@PathVariable UUID eventId,@PathVariable UUID mediaId,@AuthenticationPrincipal Jwt jwt,@RequestParam MediaVisibility value){return media.visibility(eventId,mediaId,jwt.getSubject(),admin(jwt),scopedEventId(jwt),value);}
 @DeleteMapping("/events/{eventId}/media/{mediaId}") public void delete(@PathVariable UUID eventId,@PathVariable UUID mediaId,@AuthenticationPrincipal Jwt jwt){media.delete(eventId,mediaId,jwt.getSubject(),admin(jwt),scopedEventId(jwt));}
 @GetMapping("/events/{eventId}/wishes") public List<WishDtos.Item> wishes(@PathVariable UUID eventId,@AuthenticationPrincipal Jwt jwt){return media.wishes(eventId,jwt.getSubject(),admin(jwt),scopedEventId(jwt));}
 @GetMapping("/events/{eventId}/download-all") public void download(@PathVariable UUID eventId,@AuthenticationPrincipal Jwt jwt,HttpServletResponse response) throws IOException{downloads.stream(eventId,jwt.getSubject(),admin(jwt),scopedEventId(jwt),response);}
}
