package com.brava.memories.upload;
import com.brava.memories.media.*;
import com.brava.memories.scan.MalwareScanner;
import com.brava.memories.storage.ObjectStorage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.InputStream;
import java.time.*;
import java.util.UUID;

@Service
public class MediaProcessingService {
 private final MediaRepository mediaRepo; private final ObjectStorage storage; private final UploadValidationService validation; private final MalwareScanner scanner; private final TransactionTemplate tx;
 public MediaProcessingService(MediaRepository mediaRepo,ObjectStorage storage,UploadValidationService validation,MalwareScanner scanner,PlatformTransactionManager tm){this.mediaRepo=mediaRepo;this.storage=storage;this.validation=validation;this.scanner=scanner;this.tx=new TransactionTemplate(tm);}

 @Async("mediaProcessingExecutor") public void processAsync(UUID mediaId){process(mediaId,false);}
 @Async("mediaProcessingExecutor") public void recoverAsync(UUID mediaId){process(mediaId,true);}

 private void process(UUID id,boolean staleRecovery){
   MediaSnapshot m=claim(id,staleRecovery);
   if(m==null)return;
   String finalKey="events/"+m.eventId()+"/ready/"+m.id()+extensionFor(m.mimeType());
   try{
     // Crash-safe recovery: object may have moved successfully before DB state was committed.
     if(!storage.exists(m.storageKey())){if(storage.exists(finalKey)){ready(id,finalKey);return;}fail(id,"OBJECT_NOT_FOUND");return;}
     if(storage.contentLength(m.storageKey())!=m.fileSize()){rejectAndDelete(id,m.storageKey(),"SIZE_MISMATCH");return;}
     try(InputStream in=storage.open(m.storageKey())){validation.validateSignature(in,m.mimeType());}
     MalwareScanner.ScanResult result;
     try(InputStream in=storage.open(m.storageKey())){result=scanner.scan(in);}
     if(!result.clean()){rejectAndDelete(id,m.storageKey(),"MALWARE_DETECTED");return;}
     storage.move(m.storageKey(),finalKey);
     ready(id,finalKey);
   }catch(com.brava.memories.common.exception.AppException ex){rejectAndDelete(id,m.storageKey(),ex.code());}
   catch(Exception ex){fail(id,"PROCESSING_FAILED");}
 }

 private MediaSnapshot claim(UUID id,boolean staleRecovery){return tx.execute(status->{Media m=mediaRepo.findByIdForUpdate(id).orElse(null);if(m==null)return null;if(!staleRecovery&&m.getStatus()!=MediaStatus.UPLOADED)return null;if(staleRecovery&&(m.getStatus()!=MediaStatus.SCANNING||m.getProcessingStartedAt()==null||m.getProcessingStartedAt().isAfter(Instant.now().minus(Duration.ofMinutes(10)))))return null;m.markScanning();return new MediaSnapshot(m.getId(),m.getEvent().getId(),m.getStorageKey(),m.getMimeType(),m.getFileSize());});}
 private void ready(UUID id,String key){tx.executeWithoutResult(s->{Media m=mediaRepo.findByIdForUpdate(id).orElse(null);if(m!=null&&m.getStatus()==MediaStatus.SCANNING)m.markReady(key);});}
 private void fail(UUID id,String reason){tx.executeWithoutResult(s->{Media m=mediaRepo.findByIdForUpdate(id).orElse(null);if(m!=null&&m.getStatus()!=MediaStatus.READY&&m.getStatus()!=MediaStatus.REJECTED)m.fail(reason);});}
 private void rejectAndDelete(UUID id,String key,String reason){try{storage.delete(key);}catch(Exception ignored){}tx.executeWithoutResult(s->{Media m=mediaRepo.findByIdForUpdate(id).orElse(null);if(m!=null&&m.getStatus()!=MediaStatus.READY)m.reject(reason);});}
 private String extensionFor(String mime){return switch(mime){case "image/jpeg"->".jpg";case "image/png"->".png";case "image/webp"->".webp";case "video/mp4"->".mp4";case "video/quicktime"->".mov";default->"";};}
 private record MediaSnapshot(UUID id,UUID eventId,String storageKey,String mimeType,long fileSize){}
}
