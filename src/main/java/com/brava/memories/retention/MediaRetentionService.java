package com.brava.memories.retention;

import com.brava.memories.config.AppProperties;
import com.brava.memories.event.*;
import com.brava.memories.media.*;
import com.brava.memories.storage.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class MediaRetentionService {
    private static final Logger log=LoggerFactory.getLogger(MediaRetentionService.class);
    private final EventRepository events; private final MediaRepository media; private final ObjectStorage storage; private final AppProperties props;
    public MediaRetentionService(EventRepository events, MediaRepository media, ObjectStorage storage, AppProperties props){this.events=events;this.media=media;this.storage=storage;this.props=props;}

    @Transactional
    public List<UUID> markDueEvents(){
        var statuses=List.of(EventRetentionStatus.ACTIVE,EventRetentionStatus.PENDING_DELETION,EventRetentionStatus.DELETION_FAILED);
        var due=events.findByMediaDeleteAtBeforeAndRetentionStatusIn(Instant.now(),statuses);
        List<UUID> ids=new ArrayList<>();
        for(var e:due){
            if(e.getDeletionAttempts()>=props.retention().maxDeletionAttempts()){log.error("Retention deletion exhausted retries for event {}",e.getId());continue;}
            e.markPendingDeletion(); ids.add(e.getId());
        }
        return ids;
    }

    @Transactional
    public List<Media> beginDeletion(UUID eventId){
        Event e=events.findById(eventId).orElseThrow();
        if(e.getRetentionStatus()==EventRetentionStatus.ARCHIVED) return List.of();
        e.markDeleting();
        return media.findByEventId(eventId);
    }

    public void deleteStorageObjects(List<Media> items){
        for(var m:items){
            try{
                storage.delete(m.getStorageKey());
                if(m.getThumbnailKey()!=null)storage.delete(m.getThumbnailKey());
                if(m.getRenditionKey()!=null)storage.delete(m.getRenditionKey());
            }catch(Exception ex){ throw new IllegalStateException("Failed deleting media object "+m.getId(),ex); }
        }
    }

    @Transactional
    public void completeDeletion(UUID eventId){
        Event e=events.findById(eventId).orElseThrow();
        media.deleteAll(media.findByEventId(eventId));
        e.markArchived();
    }

    @Transactional
    public void failDeletion(UUID eventId,Exception error){
        events.findById(eventId).ifPresent(e->e.markDeletionFailed(error.getMessage()));
    }
}
