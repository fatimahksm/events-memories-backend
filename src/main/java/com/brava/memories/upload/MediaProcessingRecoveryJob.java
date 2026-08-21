package com.brava.memories.upload;
import com.brava.memories.config.ProcessingProperties;
import com.brava.memories.media.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

@Component
public class MediaProcessingRecoveryJob {
 private final MediaRepository media; private final MediaProcessingService processing; private final ProcessingProperties props;
 public MediaProcessingRecoveryJob(MediaRepository media,MediaProcessingService processing,ProcessingProperties props){this.media=media;this.processing=processing;this.props=props;}
 @Scheduled(fixedDelayString="${app.processing.recovery-delay-ms:60000}")
 public void recover(){
   int n=Math.max(1,props.recoveryBatchSize());
   List<Media> pending=media.findByStatusOrderByCreatedAtAsc(MediaStatus.UPLOADED,PageRequest.of(0,n));
   int remaining=Math.max(0,n-pending.size());
   List<Media> stale=remaining==0?List.of():media.findStaleProcessing(MediaStatus.SCANNING,Instant.now().minus(Duration.ofMinutes(10)),PageRequest.of(0,remaining));
   pending.forEach(m->processing.processAsync(m.getId()));
   stale.forEach(m->processing.recoverAsync(m.getId()));
 }
}
