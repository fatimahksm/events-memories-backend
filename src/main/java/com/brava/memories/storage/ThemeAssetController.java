package com.brava.memories.storage;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@RestController
@RequestMapping("/api/public/theme-assets")
public class ThemeAssetController {
    private final ObjectStorage storage;
    public ThemeAssetController(ObjectStorage storage){this.storage=storage;}
    @GetMapping("/{encoded}") public ResponseEntity<byte[]> get(@PathVariable String encoded){try{String key=new String(Base64.getUrlDecoder().decode(encoded),StandardCharsets.UTF_8);if(!key.startsWith("theme-assets/")||!storage.exists(key))return ResponseEntity.notFound().build();try(InputStream input=storage.open(key)){String type=storage.contentType(key);return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic()).contentType(MediaType.parseMediaType(type==null?"application/octet-stream":type)).body(input.readAllBytes());}}catch(Exception ex){return ResponseEntity.notFound().build();}}
}
