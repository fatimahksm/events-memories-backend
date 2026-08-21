package com.brava.memories.storage;
import com.brava.memories.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.Base64;

@Service
@ConditionalOnProperty(name="app.storage.provider",havingValue="local",matchIfMissing=true)
public class LocalObjectStorage implements ObjectStorage {
 private final Path root; private final String baseUrl;
 public LocalObjectStorage(AppProperties props){this.root=Path.of(props.storage().localRoot()).toAbsolutePath().normalize();this.baseUrl=props.storage().publicBaseUrl();try{Files.createDirectories(root);}catch(IOException e){throw new IllegalStateException(e);}}
 private Path safe(String key){Path p=root.resolve(key).normalize();if(!p.startsWith(root))throw new IllegalArgumentException("Invalid key");return p;}
 public String createUploadUrl(String key,String type,long len,Duration ttl){String encoded=Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));return baseUrl+"/api/public/local-storage/"+encoded;}
 public String createDownloadUrl(String key,Duration ttl){String encoded=Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));return baseUrl+"/api/public/local-storage/"+encoded;}
 public boolean exists(String key){return Files.exists(safe(key));}
 public long contentLength(String key){try{return Files.size(safe(key));}catch(IOException e){throw new IllegalStateException(e);}}
 public String contentType(String key){try{return Files.probeContentType(safe(key));}catch(IOException e){return "application/octet-stream";}}
 public InputStream open(String key){try{return Files.newInputStream(safe(key));}catch(IOException e){throw new IllegalStateException(e);}}
 public void move(String source,String target){try{Path dest=safe(target);Files.createDirectories(dest.getParent());Files.move(safe(source),dest,StandardCopyOption.REPLACE_EXISTING);}catch(IOException e){throw new IllegalStateException(e);}}
 public void delete(String key){try{Files.deleteIfExists(safe(key));}catch(IOException ignored){}}
 public void write(String key,InputStream input){write(key,input,-1,null);}
 public void write(String key,InputStream input,long contentLength,String contentType){try{Path p=safe(key);Files.createDirectories(p.getParent());Files.copy(input,p,StandardCopyOption.REPLACE_EXISTING);}catch(IOException e){throw new IllegalStateException(e);}}
}
