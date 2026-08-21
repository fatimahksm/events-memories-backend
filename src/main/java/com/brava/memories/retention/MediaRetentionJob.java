package com.brava.memories.retention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class MediaRetentionJob {
 private static final Logger log=LoggerFactory.getLogger(MediaRetentionJob.class);
 private final MediaRetentionService service;
 public MediaRetentionJob(MediaRetentionService service){this.service=service;}
 @Scheduled(cron="${MEDIA_RETENTION_CRON:0 30 3 * * *}") public void cleanup(){
   for(var eventId:service.markDueEvents()){
     try{var items=service.beginDeletion(eventId);service.deleteStorageObjects(items);service.completeDeletion(eventId);log.info("Archived event {} after deleting {} media objects",eventId,items.size());}
     catch(Exception ex){log.error("Retention cleanup failed for event {}",eventId,ex);service.failDeletion(eventId,ex);}
   }
 }
}
