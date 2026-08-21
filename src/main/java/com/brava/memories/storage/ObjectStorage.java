package com.brava.memories.storage;
import java.io.InputStream;
import java.time.Duration;
public interface ObjectStorage {
 String createUploadUrl(String key,String contentType,long contentLength,Duration ttl);
 String createDownloadUrl(String key,Duration ttl);
 boolean exists(String key);
 long contentLength(String key);
 String contentType(String key);
 InputStream open(String key);
 void write(String key,InputStream input,long contentLength,String contentType);
 void move(String sourceKey,String targetKey);
 void delete(String key);
}
