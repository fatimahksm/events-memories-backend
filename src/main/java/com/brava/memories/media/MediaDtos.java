package com.brava.memories.media;
import java.time.Instant;
import java.util.*;
public final class MediaDtos {
 private MediaDtos(){}
 public record Item(UUID id,String mediaType,String mimeType,String visibility,String guestName,String status,String url,String thumbnailUrl,long likes,Instant createdAt){}
 public record Page(List<Item> items,int page,int size,long totalElements,int totalPages){}
 public record CursorPage(List<Item> items,String nextCursor,boolean hasMore){}
 public record LikeResponse(long likes,boolean liked){}
}
