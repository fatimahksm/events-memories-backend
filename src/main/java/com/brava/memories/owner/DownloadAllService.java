package com.brava.memories.owner;
import com.brava.memories.event.*;
import com.brava.memories.media.*;
import com.brava.memories.storage.ObjectStorage;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
@Service
public class DownloadAllService {
 private final EventService events; private final MediaRepository media; private final ObjectStorage storage;
 public DownloadAllService(EventService events,MediaRepository media,ObjectStorage storage){this.events=events;this.media=media;this.storage=storage;}
 public void stream(UUID eventId,String principal,boolean admin,HttpServletResponse response) throws IOException {
  Event e=events.requireOwned(eventId,principal,admin);
  response.setContentType("application/zip");
  response.setHeader("Content-Disposition","attachment; filename=event-memories-"+e.getSlug()+".zip");
  List<Media> items=media.findByEventIdAndStatus(eventId,MediaStatus.READY);
  if(items.isEmpty()){new ZipOutputStream(response.getOutputStream()).close();return;}
  // A sequential fetch-then-write loop pays every file's full R2 round-trip time back to
  // back, which is what made "download all" so slow with many files. Pre-fetch a few files
  // at a time into temp files on a small thread pool so those round-trips overlap, while
  // still streaming each one in small chunks (never a whole file in memory) — the pool size
  // also bounds how many temp files exist on disk at once.
  ExecutorService pool=Executors.newFixedThreadPool(Math.min(3,items.size()));
  List<Future<Path>> fetches=new ArrayList<>();
  for(Media m:items)fetches.add(pool.submit(()->fetchToTemp(m)));
  try{
   Set<String> used=new HashSet<>();
   try(ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()))){
    zip.setLevel(Deflater.BEST_SPEED);
    for(int i=0;i<items.size();i++){
     String name=unique(used,items.get(i).getSafeDisplayName());
     Path temp=fetches.get(i).get();
     zip.putNextEntry(new ZipEntry((items.get(i).getVisibility()==MediaVisibility.PRIVATE?"private/":"public/")+name));
     Files.copy(temp,zip);
     zip.closeEntry();
    }
   }
  }catch(InterruptedException ie){Thread.currentThread().interrupt();throw new IOException("Download interrupted",ie);}
  catch(ExecutionException ee){throw new IOException("Failed to fetch media for download",ee.getCause());}
  finally{
   pool.shutdownNow();
   for(Future<Path> f:fetches){try{if(f.isDone()&&!f.isCancelled())Files.deleteIfExists(f.get());}catch(Exception ignored){}}
  }
 }
 private Path fetchToTemp(Media m) throws IOException{
  Path temp=Files.createTempFile("dl-",".bin");
  try(InputStream in=storage.open(m.getStorageKey())){Files.copy(in,temp,StandardCopyOption.REPLACE_EXISTING);}
  return temp;
 }
 private String unique(Set<String> used,String name){String n=name;int i=2;while(!used.add(n)){int dot=name.lastIndexOf('.');n=dot>0?name.substring(0,dot)+"-"+i+++name.substring(dot):name+"-"+i++;}return n;}
}
