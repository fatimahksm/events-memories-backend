package com.brava.memories.event;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
public final class EventDtos {
 private EventDtos(){}
 public record Theme(String templateKey,String backgroundImageUrl,String primaryColor,String accentColor,String textColor,double overlayOpacity,String fontFamily,int buttonRadiusPx,EventColorMode colorMode,double backgroundPositionX,double backgroundPositionY,BackgroundFit backgroundFit){}
 public record PublicEvent(UUID id,String slug,String names,String quote,String namesAr,String quoteAr,LocalDate eventDate,Instant expiresAt,Theme theme){}
 public record Create(@NotNull UUID ownerId,@NotBlank @Size(max=180) String names,@Size(max=500) String quote,@Size(max=180) String namesAr,@Size(max=500) String quoteAr,LocalDate eventDate,@NotNull @Future Instant expiresAt,Instant mediaDeleteAt,@Size(max=140) String slug){}
 public record OwnerCreate(@NotBlank @Size(max=180) String names,@Size(max=500) String quote,@Size(max=180) String namesAr,@Size(max=500) String quoteAr,LocalDate eventDate,@NotNull @Future Instant expiresAt,Instant mediaDeleteAt,@Size(max=140) String slug){}
 public record Update(@NotBlank @Size(max=180) String names,@Size(max=500) String quote,@Size(max=180) String namesAr,@Size(max=500) String quoteAr,LocalDate eventDate,@NotNull Instant expiresAt,@NotNull Instant mediaDeleteAt,boolean active){}
 public record OwnerUpdate(@NotBlank @Size(max=180) String names,@Size(max=500) String quote,@Size(max=180) String namesAr,@Size(max=500) String quoteAr,LocalDate eventDate){}
 public record UpdateTheme(@NotBlank String templateKey,String backgroundImageUrl,@Pattern(regexp="^#[0-9A-Fa-f]{6}$") String primaryColor,@Pattern(regexp="^#[0-9A-Fa-f]{6}$") String accentColor,@Pattern(regexp="^#[0-9A-Fa-f]{6}$") String textColor,@DecimalMin("0.0") @DecimalMax("0.85") double overlayOpacity,@NotBlank @Size(max=120) String fontFamily,@Min(0) @Max(999) int buttonRadiusPx,@NotNull EventColorMode colorMode,@DecimalMin("0.0") @DecimalMax("100.0") double backgroundPositionX,@DecimalMin("0.0") @DecimalMax("100.0") double backgroundPositionY,@NotNull BackgroundFit backgroundFit){}
 public record Summary(UUID id,String slug,String names,String quote,String namesAr,String quoteAr,LocalDate eventDate,Instant expiresAt,Instant mediaDeleteAt,boolean active,EventRetentionStatus retentionStatus,int deletionAttempts,Theme theme,String accessToken){}
 public static Theme theme(Event e){return new Theme(e.getTemplateKey(),e.getBackgroundImageUrl(),e.getPrimaryColor(),e.getAccentColor(),e.getTextColor(),e.getOverlayOpacity(),e.getFontFamily(),e.getButtonRadiusPx(),e.getColorMode(),e.getBackgroundPositionX(),e.getBackgroundPositionY(),e.getBackgroundFit());}
 public static Summary summary(Event e){return new Summary(e.getId(),e.getSlug(),e.getNames(),e.getQuote(),e.getNamesAr(),e.getQuoteAr(),e.getEventDate(),e.getExpiresAt(),e.getMediaDeleteAt(),e.isActive(),e.getRetentionStatus(),e.getDeletionAttempts(),theme(e),e.getAccessToken());}
}
