package com.brava.memories.event;
import com.brava.memories.media.*;
import com.brava.memories.wish.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/public/events")
public class PublicEventController {
 private final EventService events; private final PublicMediaService media; private final WishService wishes;
 public PublicEventController(EventService events,PublicMediaService media,WishService wishes){this.events=events;this.media=media;this.wishes=wishes;}
 @GetMapping("/{slug}") public EventDtos.PublicEvent get(@PathVariable String slug){return events.publicDto(events.requirePublic(slug));}
 @GetMapping("/{slug}/album") public MediaDtos.CursorPage album(@PathVariable String slug,@RequestParam(required=false) String cursor,@RequestParam(defaultValue="24") int size){return media.album(slug,cursor,size);}
 @PostMapping("/{slug}/media/{mediaId}/like") public MediaDtos.LikeResponse like(@PathVariable String slug,@PathVariable UUID mediaId,@RequestHeader("X-Visitor-Id") String visitor){return media.toggleLike(slug,mediaId,visitor);}
 @PostMapping("/{slug}/wishes") public WishDtos.Item wish(@PathVariable String slug,@Valid @RequestBody WishDtos.Create req){return wishes.create(slug,req);}
}
