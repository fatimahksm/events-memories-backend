package com.brava.memories.auth;
import com.brava.memories.config.AppProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final AuthService service; private final AppProperties props;
 public AuthController(AuthService service,AppProperties props){this.service=service;this.props=props;}
 @PostMapping("/login") public AuthDtos.MeResponse login(@Valid @RequestBody AuthDtos.LoginRequest req,HttpServletResponse response){
   return authenticate(service.login(req),response);
 }
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public AuthDtos.MeResponse register(@Valid @RequestBody AuthDtos.RegisterRequest req,HttpServletResponse response){return authenticate(service.register(req),response);}
 @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) public void logout(HttpServletResponse response){response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("access_token","").httpOnly(true).secure(props.security().cookieSecure()).sameSite("Strict").path("/").maxAge(Duration.ZERO).build().toString());}
 @GetMapping("/me") public AuthDtos.MeResponse me(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt){return service.me(jwt.getSubject());}
 private AuthDtos.MeResponse authenticate(AuthService.LoginResult result,HttpServletResponse response){ResponseCookie cookie=ResponseCookie.from("access_token",result.token()).httpOnly(true).secure(props.security().cookieSecure()).sameSite("Strict").path("/").maxAge(props.security().jwtTtl()).build();response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());return result.user();}
}
