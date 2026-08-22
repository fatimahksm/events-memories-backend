package com.brava.memories.admin;
import com.brava.memories.auth.UserRole;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import com.brava.memories.event.EventDtos;
public final class AdminDtos {
 private AdminDtos(){}
 public record CreateOwner(@NotBlank @Size(max=120) String displayName,@NotBlank @Email @Size(max=190) String email,@NotBlank @Size(min=10,max=72) String password){}
 public record Owner(UUID id,String displayName,String email,boolean enabled,Instant createdAt){}
 public record Stats(long owners,long activeOwners,long events,long activeEvents,long media,long readyMedia,long wishes,long storedBytes){}
 public record EventItem(EventDtos.Summary event,Owner owner,long mediaCount,long wishCount){}
 public record EventPage(List<EventItem> items,int page,int size,long totalElements,int totalPages){}
 public record AssetUpload(String url){}
 public record Asset(UUID id,String url,String contentType,long sizeBytes,Instant createdAt){}
 public record PublishEvent(@NotNull @Valid EventDtos.Create event,@NotNull @Valid EventDtos.UpdateTheme theme){}
}
