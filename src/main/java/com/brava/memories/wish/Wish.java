package com.brava.memories.wish;
import com.brava.memories.event.Event;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="wish")
public class Wish {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="event_id") private Event event;
 @Column(name="guest_name", length=100) private String guestName;
 @Column(nullable=false, length=1000) private String message;
 @Column(name="created_at", nullable=false) private Instant createdAt;
 protected Wish(){} public Wish(UUID id, Event event, String guestName, String message){this.id=id;this.event=event;this.guestName=guestName;this.message=message;this.createdAt=Instant.now();}
 public UUID getId(){return id;} public String getGuestName(){return guestName;} public String getMessage(){return message;} public Instant getCreatedAt(){return createdAt;}
}
