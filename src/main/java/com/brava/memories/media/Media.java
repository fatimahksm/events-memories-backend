package com.brava.memories.media;
import com.brava.memories.event.Event;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="media")
public class Media {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="event_id") private Event event;
    @Column(name="storage_key", nullable=false, unique=true) private String storageKey;
    @Column(name="thumbnail_key", length=500) private String thumbnailKey;
    @Column(name="rendition_key", length=500) private String renditionKey;
    @Column(name="client_upload_id") private UUID clientUploadId;
    @Column(name="original_file_name", nullable=false, length=255) private String originalFileName;
    @Column(name="safe_display_name", nullable=false, length=255) private String safeDisplayName;
    @Enumerated(EnumType.STRING) @Column(name="media_type", nullable=false, length=20) private MediaType mediaType;
    @Column(name="mime_type", nullable=false, length=100) private String mimeType;
    @Column(name="file_size", nullable=false) private long fileSize;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private MediaVisibility visibility;
    @Column(name="guest_name", length=100) private String guestName;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private MediaStatus status;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="ready_at") private Instant readyAt;
    @Column(name="rejection_reason", length=80) private String rejectionReason;
    @Column(name="processing_started_at") private Instant processingStartedAt;
    @Version @Column(nullable=false) private long version;

    protected Media(){}
    public Media(UUID id, Event event, String storageKey, UUID clientUploadId, String originalFileName, String safeDisplayName, MediaType mediaType, String mimeType, long fileSize, MediaVisibility visibility, String guestName){this.id=id;this.event=event;this.storageKey=storageKey;this.clientUploadId=clientUploadId;this.originalFileName=originalFileName;this.safeDisplayName=safeDisplayName;this.mediaType=mediaType;this.mimeType=mimeType;this.fileSize=fileSize;this.visibility=visibility;this.guestName=guestName;this.status=MediaStatus.PENDING_UPLOAD;this.createdAt=Instant.now();}
    public UUID getId(){return id;} public Event getEvent(){return event;} public String getStorageKey(){return storageKey;} public String getThumbnailKey(){return thumbnailKey;} public String getRenditionKey(){return renditionKey;} public UUID getClientUploadId(){return clientUploadId;} public String getOriginalFileName(){return originalFileName;} public String getSafeDisplayName(){return safeDisplayName;} public MediaType getMediaType(){return mediaType;} public String getMimeType(){return mimeType;} public long getFileSize(){return fileSize;} public MediaVisibility getVisibility(){return visibility;} public String getGuestName(){return guestName;} public MediaStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public Instant getReadyAt(){return readyAt;} public String getRejectionReason(){return rejectionReason;} public Instant getProcessingStartedAt(){return processingStartedAt;} public long getVersion(){return version;}
    public void markUploaded(){status=MediaStatus.UPLOADED;rejectionReason=null;processingStartedAt=null;} public void markScanning(){status=MediaStatus.SCANNING;processingStartedAt=Instant.now();} public void markReady(String finalKey){storageKey=finalKey;status=MediaStatus.READY;readyAt=Instant.now();rejectionReason=null;processingStartedAt=null;} public void setThumbnailKey(String key){thumbnailKey=key;} public void setRenditionKey(String key){renditionKey=key;} public void reject(String reason){status=MediaStatus.REJECTED;rejectionReason=reason;processingStartedAt=null;} public void fail(String reason){status=MediaStatus.FAILED;rejectionReason=reason;processingStartedAt=null;} public void setVisibility(MediaVisibility v){visibility=v;}
}
