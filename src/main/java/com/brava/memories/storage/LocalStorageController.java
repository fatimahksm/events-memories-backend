package com.brava.memories.storage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/public/local-storage")
@ConditionalOnProperty(name="app.storage.provider",havingValue="local",matchIfMissing=true)
public class LocalStorageController {
 private final LocalObjectStorage storage;
 public LocalStorageController(LocalObjectStorage storage){this.storage=storage;}
 private String decode(String v){return new String(Base64.getUrlDecoder().decode(v),StandardCharsets.UTF_8);}
 @PutMapping("/{encoded}") @ResponseStatus(HttpStatus.NO_CONTENT) public void put(@PathVariable String encoded,@RequestHeader(value="Content-Type",required=false) String type,InputStream body){storage.write(decode(encoded),body);}
 @GetMapping("/{encoded}") public ResponseEntity<byte[]> get(@PathVariable String encoded){String key=decode(encoded);try(InputStream in=storage.open(key)){return ResponseEntity.ok().contentType(MediaType.parseMediaType(storage.contentType(key)==null?"application/octet-stream":storage.contentType(key))).body(in.readAllBytes());}catch(IOException e){return ResponseEntity.notFound().build();}}
}
