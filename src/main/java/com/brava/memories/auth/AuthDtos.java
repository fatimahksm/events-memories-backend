package com.brava.memories.auth;
import jakarta.validation.constraints.*;
public final class AuthDtos {
 private AuthDtos(){}
 public record LoginRequest(@NotBlank @Email @Size(max=190) String email,@NotBlank @Size(max=72) String password){}
 public record RegisterRequest(
         @NotBlank @Size(max=120) String displayName,
         @NotBlank @Email @Size(max=190) String email,
         @NotBlank @Size(min=10,max=72) String password){}
 public record MeResponse(String id,String email,String displayName,String role){}
}
