package com.brava.memories.admin;
import com.brava.memories.auth.*;
import com.brava.memories.common.exception.AppException;
import com.brava.memories.event.*;
import com.brava.memories.media.*;
import com.brava.memories.wish.WishRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class AdminService {
 private final AppUserRepository users; private final PasswordEncoder encoder; private final EventRepository events; private final MediaRepository media; private final WishRepository wishes;
 public AdminService(AppUserRepository users,PasswordEncoder encoder,EventRepository events,MediaRepository media,WishRepository wishes){this.users=users;this.encoder=encoder;this.events=events;this.media=media;this.wishes=wishes;}
 @Transactional public AdminDtos.Owner createOwner(AdminDtos.CreateOwner req){if(users.existsByEmailIgnoreCase(req.email()))throw new AppException("EMAIL_ALREADY_USED","Email is already in use",HttpStatus.CONFLICT);AppUser u=users.save(new AppUser(UUID.randomUUID(),req.email(),encoder.encode(req.password()),req.displayName().trim(),UserRole.OWNER));return dto(u);}
 @Transactional(readOnly=true) public List<AdminDtos.Owner> owners(){return users.findAll().stream().filter(u->u.getRole()==UserRole.OWNER).map(this::dto).toList();}
 @Transactional public AdminDtos.Owner setEnabled(UUID id,boolean enabled){AppUser u=users.findById(id).filter(x->x.getRole()==UserRole.OWNER).orElseThrow(()->new AppException("OWNER_NOT_FOUND","Owner not found",HttpStatus.NOT_FOUND));u.setEnabled(enabled);return dto(u);}
 @Transactional public AdminDtos.Owner regenerateAccessToken(UUID id){AppUser u=users.findById(id).filter(x->x.getRole()==UserRole.OWNER).orElseThrow(()->new AppException("OWNER_NOT_FOUND","Owner not found",HttpStatus.NOT_FOUND));u.regenerateAccessToken();return dto(u);}
 @Transactional(readOnly=true) public AdminDtos.Stats stats(){return new AdminDtos.Stats(users.countByRole(UserRole.OWNER),users.countByRoleAndEnabled(UserRole.OWNER,true),events.count(),events.countByActiveTrue(),media.count(),media.countByStatus(MediaStatus.READY),wishes.count(),media.totalStoredBytes());}
 @Transactional(readOnly=true) public AdminDtos.EventPage eventPage(int page,int size){var result=events.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0,page),Math.min(Math.max(1,size),50)));var items=result.getContent().stream().map(e->new AdminDtos.EventItem(EventDtos.summary(e),dto(e.getOwner()),media.countByEventId(e.getId()),wishes.countByEventId(e.getId()))).toList();return new AdminDtos.EventPage(items,result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages());}
 private AdminDtos.Owner dto(AppUser u){return new AdminDtos.Owner(u.getId(),u.getDisplayName(),u.getEmail(),u.isEnabled(),u.getAccessToken(),u.getCreatedAt());}
}
