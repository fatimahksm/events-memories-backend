package com.brava.memories.owner;
import com.brava.memories.event.*;
import com.brava.memories.media.*;
import com.brava.memories.storage.ObjectStorage;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;
import java.util.zip.*;
@Service
public class DownloadAllService {
 private final EventService events; private final MediaRepository media; private final ObjectStorage storage;
 public DownloadAllService(EventService events,MediaRepository media,ObjectStorage storage){this.events=events;this.media=media;this.storage=storage;}
 public void stream(UUID eventId,String principal,boolean admin,HttpServletResponse response) throws IOException {Event e=events.requireOwned(eventId,principal,admin);response.setContentType("application/zip");response.setHeader("Content-Disposition","attachment; filename=event-memories-"+e.getSlug()+".zip");try(ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()))){Set<String> used=new HashSet<>();for(Media m:media.findByEventIdAndStatus(eventId,MediaStatus.READY)){String name=unique(used,m.getSafeDisplayName());zip.putNextEntry(new ZipEntry((m.getVisibility()==MediaVisibility.PRIVATE?"private/":"public/")+name));try(InputStream in=storage.open(m.getStorageKey())){in.transferTo(zip);}zip.closeEntry();}}}
 private String unique(Set<String> used,String name){String n=name;int i=2;while(!used.add(n)){int dot=name.lastIndexOf('.');n=dot>0?name.substring(0,dot)+"-"+i+++name.substring(dot):name+"-"+i++;}return n;}
}
