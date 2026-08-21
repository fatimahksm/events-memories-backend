package com.brava.memories.event;

import com.brava.memories.auth.AppUser;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="event")
public class Event {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="owner_id") private AppUser owner;
    @Column(nullable=false, unique=true, length=140) private String slug;
    @Column(nullable=false, length=180) private String names;
    @Column(length=500) private String quote;
    @Column(name="names_ar", length=180) private String namesAr;
    @Column(name="quote_ar", length=500) private String quoteAr;
    @Column(name="event_date") private LocalDate eventDate;
    @Column(name="expires_at", nullable=false) private Instant expiresAt;
    @Column(name="media_delete_at", nullable=false) private Instant mediaDeleteAt;
    @Column(nullable=false) private boolean active;
    @Column(name="template_key", nullable=false, length=50) private String templateKey;
    @Column(name="background_image_url") private String backgroundImageUrl;
    @Column(name="primary_color", nullable=false, length=20) private String primaryColor;
    @Column(name="accent_color", nullable=false, length=20) private String accentColor;
    @Column(name="text_color", nullable=false, length=20) private String textColor;
    @Column(name="overlay_opacity", nullable=false) private double overlayOpacity;
    @Column(name="font_family", nullable=false, length=120) private String fontFamily;
    @Column(name="button_radius_px", nullable=false) private int buttonRadiusPx;
    @Enumerated(EnumType.STRING) @Column(name="color_mode", nullable=false, length=10) private EventColorMode colorMode;
    @Column(name="background_position_x", nullable=false) private double backgroundPositionX;
    @Column(name="background_position_y", nullable=false) private double backgroundPositionY;
    @Enumerated(EnumType.STRING) @Column(name="background_fit", nullable=false, length=10) private BackgroundFit backgroundFit;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @Enumerated(EnumType.STRING) @Column(name="retention_status", nullable=false, length=30) private EventRetentionStatus retentionStatus;
    @Column(name="deletion_attempts", nullable=false) private int deletionAttempts;
    @Column(name="deletion_started_at") private Instant deletionStartedAt;
    @Column(name="deleted_at") private Instant deletedAt;
    @Column(name="last_deletion_error", length=500) private String lastDeletionError;

    protected Event() {}
    public Event(UUID id, AppUser owner, String slug, String names, String quote, String namesAr, String quoteAr, LocalDate eventDate, Instant expiresAt, Instant mediaDeleteAt) {
        this.id=id; this.owner=owner; this.slug=slug; this.names=names; this.quote=quote; this.namesAr=namesAr; this.quoteAr=quoteAr; this.eventDate=eventDate; this.expiresAt=expiresAt; this.mediaDeleteAt=mediaDeleteAt;
        this.active=true; this.retentionStatus=EventRetentionStatus.ACTIVE; this.deletionAttempts=0; this.templateKey="elegant"; this.primaryColor="#FFFFFF"; this.accentColor="#C8A96B"; this.textColor="#FFFFFF"; this.overlayOpacity=.42; this.fontFamily="Georgia, serif"; this.buttonRadiusPx=999; this.colorMode=EventColorMode.DARK; this.backgroundPositionX=50; this.backgroundPositionY=50; this.backgroundFit=BackgroundFit.COVER; this.createdAt=Instant.now(); this.updatedAt=createdAt;
    }
    public UUID getId(){return id;} public AppUser getOwner(){return owner;} public String getSlug(){return slug;} public String getNames(){return names;} public String getQuote(){return quote;} public String getNamesAr(){return namesAr;} public String getQuoteAr(){return quoteAr;}
    public LocalDate getEventDate(){return eventDate;} public Instant getExpiresAt(){return expiresAt;} public Instant getMediaDeleteAt(){return mediaDeleteAt;} public boolean isActive(){return active;}
    public String getTemplateKey(){return templateKey;} public String getBackgroundImageUrl(){return backgroundImageUrl;} public String getPrimaryColor(){return primaryColor;} public String getAccentColor(){return accentColor;}
    public String getTextColor(){return textColor;} public double getOverlayOpacity(){return overlayOpacity;} public String getFontFamily(){return fontFamily;} public int getButtonRadiusPx(){return buttonRadiusPx;}
    public EventColorMode getColorMode(){return colorMode;} public double getBackgroundPositionX(){return backgroundPositionX;} public double getBackgroundPositionY(){return backgroundPositionY;} public BackgroundFit getBackgroundFit(){return backgroundFit;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public EventRetentionStatus getRetentionStatus(){return retentionStatus;} public int getDeletionAttempts(){return deletionAttempts;} public Instant getDeletionStartedAt(){return deletionStartedAt;} public Instant getDeletedAt(){return deletedAt;} public String getLastDeletionError(){return lastDeletionError;}
    public boolean isExpired(){ return !active || Instant.now().isAfter(expiresAt); }
    public void updateBasics(String names,String quote,String namesAr,String quoteAr,LocalDate eventDate,Instant expiresAt,Instant mediaDeleteAt,boolean active){this.names=names;this.quote=quote;this.namesAr=namesAr;this.quoteAr=quoteAr;this.eventDate=eventDate;this.expiresAt=expiresAt;this.mediaDeleteAt=mediaDeleteAt;this.active=active;touch();}
    public void updateContent(String names,String quote,String namesAr,String quoteAr,LocalDate eventDate){this.names=names;this.quote=quote;this.namesAr=namesAr;this.quoteAr=quoteAr;this.eventDate=eventDate;touch();}
    public void updateTheme(String templateKey,String backgroundImageUrl,String primaryColor,String accentColor,String textColor,double overlayOpacity,String fontFamily,int buttonRadiusPx,EventColorMode colorMode,double backgroundPositionX,double backgroundPositionY,BackgroundFit backgroundFit){this.templateKey=templateKey;this.backgroundImageUrl=backgroundImageUrl;this.primaryColor=primaryColor;this.accentColor=accentColor;this.textColor=textColor;this.overlayOpacity=overlayOpacity;this.fontFamily=fontFamily;this.buttonRadiusPx=buttonRadiusPx;this.colorMode=colorMode;this.backgroundPositionX=backgroundPositionX;this.backgroundPositionY=backgroundPositionY;this.backgroundFit=backgroundFit;touch();}
    public void setActive(boolean value){active=value;touch();}
    public void extendRetention(Instant newDeleteAt){this.mediaDeleteAt=newDeleteAt;if(retentionStatus!=EventRetentionStatus.ARCHIVED){this.retentionStatus=EventRetentionStatus.ACTIVE;this.lastDeletionError=null;}touch();}
    public void markPendingDeletion(){if(retentionStatus!=EventRetentionStatus.ARCHIVED){retentionStatus=EventRetentionStatus.PENDING_DELETION;active=false;touch();}}
    public void markDeleting(){retentionStatus=EventRetentionStatus.DELETING;deletionAttempts++;deletionStartedAt=Instant.now();lastDeletionError=null;active=false;touch();}
    public void markDeletionFailed(String message){retentionStatus=EventRetentionStatus.DELETION_FAILED;lastDeletionError=message==null?null:message.substring(0,Math.min(message.length(),500));touch();}
    public void markArchived(){retentionStatus=EventRetentionStatus.ARCHIVED;deletedAt=Instant.now();lastDeletionError=null;active=false;touch();}
    private void touch(){updatedAt=Instant.now();}
}
