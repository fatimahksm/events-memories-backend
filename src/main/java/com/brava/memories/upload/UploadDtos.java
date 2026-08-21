package com.brava.memories.upload;
import com.brava.memories.media.MediaVisibility;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
public final class UploadDtos {
 private UploadDtos(){}
 public record CreateSession(@NotNull UUID clientUploadId,@NotBlank @Size(max=255) String fileName,@NotBlank @Size(max=100) String contentType,@Positive long size,@NotNull MediaVisibility visibility,@Size(max=100) String guestName){}
 public record Session(UUID mediaId,String uploadUrl,Instant expiresAt){}
 public record Finalize(UUID mediaId,String status){}
}
