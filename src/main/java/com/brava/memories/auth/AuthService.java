package com.brava.memories.auth;

import com.brava.memories.common.exception.AppException;
import com.brava.memories.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
 private final AppUserRepository users; private final PasswordEncoder encoder; private final JwtEncoder jwtEncoder; private final AppProperties props;
 public AuthService(AppUserRepository users,PasswordEncoder encoder,JwtEncoder jwtEncoder,AppProperties props){this.users=users;this.encoder=encoder;this.jwtEncoder=jwtEncoder;this.props=props;}
 public record LoginResult(String token, AuthDtos.MeResponse user){}
 public LoginResult login(AuthDtos.LoginRequest req){
   AppUser u=users.findByEmailIgnoreCase(req.email().trim()).orElseThrow(()->new AppException("INVALID_CREDENTIALS","Invalid email or password",HttpStatus.UNAUTHORIZED));
   if(!u.isEnabled()||!encoder.matches(req.password(),u.getPasswordHash())) throw new AppException("INVALID_CREDENTIALS","Invalid email or password",HttpStatus.UNAUTHORIZED);
   return issueToken(u);
 }
 @Transactional
 public LoginResult register(AuthDtos.RegisterRequest req){
   String email=req.email().trim();
   if(users.existsByEmailIgnoreCase(email)) throw new AppException("EMAIL_ALREADY_USED","Email is already in use",HttpStatus.CONFLICT);
   AppUser user=users.save(new AppUser(UUID.randomUUID(),email,encoder.encode(req.password()),req.displayName().trim(),UserRole.OWNER));
   return issueToken(user);
 }
 private LoginResult issueToken(AppUser u){
   Instant now=Instant.now();
   JwtClaimsSet claims=JwtClaimsSet.builder().issuer("event-memories").issuedAt(now).expiresAt(now.plus(props.security().jwtTtl())).subject(u.getId().toString()).claim("email",u.getEmail()).claim("name",u.getDisplayName()).claim("roles", List.of(u.getRole().name())).build();
   String token=jwtEncoder.encode(JwtEncoderParameters.from(
           JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims)).getTokenValue();
   return new LoginResult(token,new AuthDtos.MeResponse(u.getId().toString(),u.getEmail(),u.getDisplayName(),u.getRole().name()));
 }
 public LoginResult ownerAccess(String token){
   AppUser u=users.findByAccessToken(token).filter(x->x.getRole()==UserRole.OWNER).orElseThrow(()->new AppException("INVALID_ACCESS_LINK","This access link is invalid or has expired",HttpStatus.UNAUTHORIZED));
   if(!u.isEnabled()) throw new AppException("ACCOUNT_DISABLED","This account has been disabled",HttpStatus.UNAUTHORIZED);
   return issueToken(u);
 }
 public AuthDtos.MeResponse me(String id){AppUser u=users.findById(java.util.UUID.fromString(id)).orElseThrow(()->new AppException("USER_NOT_FOUND","User not found",HttpStatus.NOT_FOUND));return new AuthDtos.MeResponse(u.getId().toString(),u.getEmail(),u.getDisplayName(),u.getRole().name());}
 @Transactional
 public void changePassword(String id,AuthDtos.ChangePasswordRequest req){
   AppUser u=users.findById(java.util.UUID.fromString(id)).orElseThrow(()->new AppException("USER_NOT_FOUND","User not found",HttpStatus.NOT_FOUND));
   if(!encoder.matches(req.currentPassword(),u.getPasswordHash())) throw new AppException("INVALID_CREDENTIALS","Current password is incorrect",HttpStatus.UNAUTHORIZED);
   u.setPasswordHash(encoder.encode(req.newPassword()));
 }
}
