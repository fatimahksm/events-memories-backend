package com.brava.memories.wish;
import com.brava.memories.event.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class WishService {
 private final EventService events; private final WishRepository repo; public WishService(EventService events,WishRepository repo){this.events=events;this.repo=repo;}
 @Transactional public WishDtos.Item create(String slug,WishDtos.Create req){Event e=events.requirePublic(slug);Wish w=repo.save(new Wish(UUID.randomUUID(),e,clean(req.guestName()),req.message().trim()));return WishDtos.item(w);}
 @Transactional(readOnly=true) public List<WishDtos.Item> forEvent(UUID eventId){return repo.findByEventIdOrderByCreatedAtDesc(eventId).stream().map(WishDtos::item).toList();}
 private String clean(String s){return s==null||s.isBlank()?null:s.trim().replaceAll("[\\p{Cntrl}]","");}
}
