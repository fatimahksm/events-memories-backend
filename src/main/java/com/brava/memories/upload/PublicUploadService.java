package com.brava.memories.upload;
import com.brava.memories.common.exception.AppException;
import com.brava.memories.config.UploadProperties;
import com.brava.memories.event.*;
import com.brava.memories.media.*;
import com.brava.memories.storage.ObjectStorage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class PublicUploadService {
 private final EventService events; private final MediaRepository media; private final UploadValidationService validation; private final ObjectStorage storage; private final UploadProperties props; private final ApplicationEventPublisher publisher;
 public PublicUploadService(EventService events,MediaRepository media,UploadValidationService validation,ObjectStorage storage,UploadProperties props,ApplicationEventPublisher publisher){this.events=events;this.media=media;this.validation=validation;this.storage=storage;this.props=props;this.publisher=publisher;}

 public UploadDtos.Session create(String slug,UploadDtos.CreateSession req){
   Event e=events.requirePublic(slug);
   MediaType type=validation.validateMetadata(req);
   Media existing=media.findByEventIdAndClientUploadId(e.getId(),req.clientUploadId()).orElse(null);
   if(existing!=null)return sessionFor(existing);

   UUID id=UUID.randomUUID();
   String key="events/"+e.getId()+"/quarantine/"+id;
   String safeName="memory-"+id+safeExtension(req.fileName());
   Media created=new Media(id,e,key,req.clientUploadId(),req.fileName(),safeName,type,req.contentType(),req.size(),req.visibility(),cleanName(req.guestName()));
   try{media.saveAndFlush(created);return sessionFor(created);}catch(DataIntegrityViolationException ex){
     Media raced=media.findByEventIdAndClientUploadId(e.getId(),req.clientUploadId()).orElseThrow(()->ex);
     return sessionFor(raced);
   }
 }

 @Transactional
 public UploadDtos.Finalize finalizeUpload(String slug,UUID mediaId){
   Event e=events.requirePublic(slug);
   Media m=media.findByIdForUpdate(mediaId).orElseThrow(()->new AppException("MEDIA_NOT_FOUND","Media not found",HttpStatus.NOT_FOUND));
   if(!m.getEvent().getId().equals(e.getId()))throw new AppException("MEDIA_NOT_FOUND","Media not found",HttpStatus.NOT_FOUND);
   if(m.getStatus()==MediaStatus.UPLOADED||m.getStatus()==MediaStatus.SCANNING||m.getStatus()==MediaStatus.READY)return new UploadDtos.Finalize(m.getId(),m.getStatus().name());
   if(m.getStatus()!=MediaStatus.PENDING_UPLOAD&&m.getStatus()!=MediaStatus.FAILED)throw new AppException("INVALID_MEDIA_STATE","Upload cannot be finalized in its current state",HttpStatus.CONFLICT);
   if(!storage.exists(m.getStorageKey()))throw new AppException("UPLOAD_NOT_FOUND","Uploaded file was not found",HttpStatus.BAD_REQUEST);
   m.markUploaded();
   publisher.publishEvent(new MediaUploadedEvent(m.getId()));
   return new UploadDtos.Finalize(m.getId(),m.getStatus().name());
 }

 public UploadDtos.Status status(String slug,UUID mediaId){
   Event e=events.requirePublic(slug);
   Media m=media.findById(mediaId).orElseThrow(()->new AppException("MEDIA_NOT_FOUND","Media not found",HttpStatus.NOT_FOUND));
   if(!m.getEvent().getId().equals(e.getId()))throw new AppException("MEDIA_NOT_FOUND","Media not found",HttpStatus.NOT_FOUND);
   boolean rejected=m.getStatus()==MediaStatus.REJECTED||m.getStatus()==MediaStatus.FAILED;
   return new UploadDtos.Status(m.getId(),m.getStatus().name(),rejected);
 }

 private UploadDtos.Session sessionFor(Media m){String url=storage.createUploadUrl(m.getStorageKey(),m.getMimeType(),m.getFileSize(),props.signedUrlTtl());return new UploadDtos.Session(m.getId(),url,Instant.now().plus(props.signedUrlTtl()));}
 private String cleanName(String v){if(v==null||v.isBlank())return null;return v.trim().replaceAll("[\\p{Cntrl}]","");}
 private String safeExtension(String name){int i=name.lastIndexOf('.');if(i<0)return "";String e=name.substring(i).toLowerCase();return e.matches("\\.(jpg|jpeg|png|webp|mp4|mov)")?e:"";}
}
