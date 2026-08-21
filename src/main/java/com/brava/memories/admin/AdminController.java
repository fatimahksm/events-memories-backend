package com.brava.memories.admin;
import com.brava.memories.event.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/admin")
public class AdminController {
 private final AdminService admin; private final EventService events; private final EventRepository eventRepo; private final ThemeAssetService themeAssets;
 public AdminController(AdminService admin,EventService events,EventRepository eventRepo,ThemeAssetService themeAssets){this.admin=admin;this.events=events;this.eventRepo=eventRepo;this.themeAssets=themeAssets;}
 @GetMapping("/stats") public AdminDtos.Stats stats(){return admin.stats();}
 @GetMapping("/owners") public List<AdminDtos.Owner> owners(){return admin.owners();}
 @PostMapping("/owners") @ResponseStatus(HttpStatus.CREATED) public AdminDtos.Owner createOwner(@Valid @RequestBody AdminDtos.CreateOwner req){return admin.createOwner(req);}
 @PatchMapping("/owners/{id}/enabled") public AdminDtos.Owner enabled(@PathVariable UUID id,@RequestParam boolean value){return admin.setEnabled(id,value);}
 @GetMapping("/events") public List<EventDtos.Summary> allEvents(){return eventRepo.findAll().stream().sorted(Comparator.comparing(Event::getCreatedAt).reversed()).map(EventDtos::summary).toList();}
 @GetMapping("/events/page") public AdminDtos.EventPage eventPage(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){return admin.eventPage(page,size);}
 @PostMapping(value="/theme-assets",consumes=org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) public AdminDtos.AssetUpload uploadThemeAsset(@RequestPart("file") org.springframework.web.multipart.MultipartFile file){return themeAssets.upload(file);}
 @PostMapping("/events") @ResponseStatus(HttpStatus.CREATED) public EventDtos.Summary createEvent(@Valid @RequestBody EventDtos.Create req){return EventDtos.summary(events.create(req));}
 @PostMapping("/events/publish") @ResponseStatus(HttpStatus.CREATED) public EventDtos.Summary publishEvent(@Valid @RequestBody AdminDtos.PublishEvent req){return EventDtos.summary(events.createWithTheme(req.event(),req.theme()));}
 @PutMapping("/events/{id}") public EventDtos.Summary updateEvent(@PathVariable UUID id,@Valid @RequestBody EventDtos.Update req){return EventDtos.summary(events.update(id,req));}
 @PutMapping("/events/{id}/theme") public EventDtos.Summary updateTheme(@PathVariable UUID id,@Valid @RequestBody EventDtos.UpdateTheme req){return EventDtos.summary(events.updateTheme(events.requireById(id),req));}
 @PostMapping("/events/{id}/retention/extend") public EventDtos.Summary extendRetention(@PathVariable UUID id,@RequestParam int days){return EventDtos.summary(events.extendRetention(id,days));}
}
