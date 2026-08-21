package com.brava.memories.auth;
import jakarta.validation.constraints.*;
public final class AuthDtos {
 private AuthDtos(){}
 public record LoginRequest(@NotBlank @Email @Size(max=190) String email,@NotBlank @Size(max=72) String password){}
 public record RegisterRequest(
         @NotBlank @Size(max=120) String displayName,
         @NotBlank @Email @Size(max=190) String email,
         @NotBlank @Size(min=10,max=72) String password){}
 public record OwnerAccessRequest(@NotBlank @Size(max=64) String token){}
 public record EventAccessRequest(@NotBlank @Size(max=64) String token){}
 public record ChangePasswordRequest(@NotBlank @Size(max=72) String currentPassword,@NotBlank @Size(min=10,max=72) String newPassword){}
 public record MeResponse(String id,String email,String displayName,String role,String scopedEventId){}
}
