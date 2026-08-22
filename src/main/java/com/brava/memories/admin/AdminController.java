package com.brava.memories.admin;
import com.brava.memories.audit.AuditLogDtos;
import com.brava.memories.audit.AuditLogService;
import com.brava.memories.event.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/admin")
public class AdminController {
 private final AdminService admin; private final EventService events; private final EventRepository eventRepo; private final ThemeAssetService themeAssets; private final AuditLogService auditLog;
 public AdminController(AdminService admin,EventService events,EventRepository eventRepo,ThemeAssetService themeAssets,AuditLogService auditLog){this.admin=admin;this.events=events;this.eventRepo=eventRepo;this.themeAssets=themeAssets;this.auditLog=auditLog;}
 @GetMapping("/stats") public AdminDtos.Stats stats(){return admin.stats();}
 @GetMapping("/owners") public List<AdminDtos.Owner> owners(){return admin.owners();}
 @PostMapping("/owners") @ResponseStatus(HttpStatus.CREATED) public AdminDtos.Owner createOwner(@Valid @RequestBody AdminDtos.CreateOwner req,@AuthenticationPrincipal Jwt jwt){
   AdminDtos.Owner owner=admin.createOwner(req);
   audit(jwt,"OWNER_CREATED","OWNER",owner.id(),owner.email());
   return owner;
 }
 @PatchMapping("/owners/{id}/enabled") public AdminDtos.Owner enabled(@PathVariable UUID id,@RequestParam boolean value,@AuthenticationPrincipal Jwt jwt){
   AdminDtos.Owner owner=admin.setEnabled(id,value);
   audit(jwt,value?"OWNER_ENABLED":"OWNER_DISABLED","OWNER",owner.id(),owner.email());
   return owner;
 }
 @GetMapping("/events") public List<EventDtos.Summary> allEvents(){return eventRepo.findAll().stream().sorted(Comparator.comparing(Event::getCreatedAt).reversed()).map(EventDtos::summary).toList();}
 @GetMapping("/events/page") public AdminDtos.EventPage eventPage(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){return admin.eventPage(page,size);}
 @GetMapping("/theme-assets") public List<AdminDtos.Asset> themeAssets(){return themeAssets.list();}
 @PostMapping(value="/theme-assets",consumes=org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) public AdminDtos.AssetUpload uploadThemeAsset(@RequestPart("file") org.springframework.web.multipart.MultipartFile file){return themeAssets.upload(file);}
 @PostMapping("/events") @ResponseStatus(HttpStatus.CREATED) public EventDtos.Summary createEvent(@Valid @RequestBody EventDtos.Create req,@AuthenticationPrincipal Jwt jwt){
   EventDtos.Summary event=EventDtos.summary(events.create(req));
   audit(jwt,"EVENT_CREATED","EVENT",event.id(),event.names());
   return event;
 }
 @PostMapping("/events/publish") @ResponseStatus(HttpStatus.CREATED) public EventDtos.Summary publishEvent(@Valid @RequestBody AdminDtos.PublishEvent req,@AuthenticationPrincipal Jwt jwt){
   EventDtos.Summary event=EventDtos.summary(events.createWithTheme(req.event(),req.theme()));
   audit(jwt,"EVENT_PUBLISHED","EVENT",event.id(),event.names());
   return event;
 }
 @PutMapping("/events/{id}") public EventDtos.Summary updateEvent(@PathVariable UUID id,@Valid @RequestBody EventDtos.Update req,@AuthenticationPrincipal Jwt jwt){
   EventDtos.Summary event=EventDtos.summary(events.update(id,req));
   audit(jwt,"EVENT_UPDATED","EVENT",event.id(),event.names());
   return event;
 }
 @PutMapping("/events/{id}/theme") public EventDtos.Summary updateTheme(@PathVariable UUID id,@Valid @RequestBody EventDtos.UpdateTheme req,@AuthenticationPrincipal Jwt jwt){
   EventDtos.Summary event=EventDtos.summary(events.updateTheme(events.requireById(id),req));
   audit(jwt,"EVENT_THEME_UPDATED","EVENT",event.id(),event.names());
   return event;
 }
 @PostMapping("/events/{id}/retention/extend") public EventDtos.Summary extendRetention(@PathVariable UUID id,@RequestParam int days,@AuthenticationPrincipal Jwt jwt){
   EventDtos.Summary event=EventDtos.summary(events.extendRetention(id,days));
   audit(jwt,"EVENT_RETENTION_EXTENDED","EVENT",event.id(),event.names()+" (+"+days+"d)");
   return event;
 }
 @GetMapping("/audit-log") public AuditLogDtos.Page auditLog(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return auditLog.page(page,size);}
 private void audit(Jwt jwt,String action,String targetType,UUID targetId,String details){
   auditLog.record(UUID.fromString(jwt.getSubject()),jwt.getClaimAsString("email"),action,targetType,targetId,details);
 }
}
