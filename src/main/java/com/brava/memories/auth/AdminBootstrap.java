package com.brava.memories.auth;
import com.brava.memories.config.AppProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component
public class AdminBootstrap implements CommandLineRunner {
 private final AppUserRepository users; private final PasswordEncoder encoder; private final AppProperties props;
 public AdminBootstrap(AppUserRepository users,PasswordEncoder encoder,AppProperties props){this.users=users;this.encoder=encoder;this.props=props;}
 @Override public void run(String... args){String email=props.bootstrap().adminEmail(); if(email!=null&&!email.isBlank()&&!users.existsByEmailIgnoreCase(email)){users.save(new AppUser(UUID.randomUUID(),email,encoder.encode(props.bootstrap().adminPassword()),"Super Admin",UserRole.SUPER_ADMIN));}}
}
