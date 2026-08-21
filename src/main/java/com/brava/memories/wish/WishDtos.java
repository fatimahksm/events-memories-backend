package com.brava.memories.wish;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
public final class WishDtos {private WishDtos(){} public record Create(@Size(max=100) String guestName,@NotBlank @Size(max=1000) String message){} public record Item(UUID id,String guestName,String message,Instant createdAt){} public static Item item(Wish w){return new Item(w.getId(),w.getGuestName(),w.getMessage(),w.getCreatedAt());}}
