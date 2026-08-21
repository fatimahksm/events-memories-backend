package com.brava.memories.upload;
import org.slf4j.*;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
@Component
public class MediaProcessingListener {
 private static final Logger log=LoggerFactory.getLogger(MediaProcessingListener.class);
 private final MediaProcessingService service;
 public MediaProcessingListener(MediaProcessingService service){this.service=service;}
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
 public void onUploaded(MediaUploadedEvent event){try{service.processAsync(event.mediaId());}catch(TaskRejectedException ex){log.warn("Media processing queue full; recovery job will retry media {}",event.mediaId());}}
}
