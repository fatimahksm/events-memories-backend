package com.brava.memories.upload;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/public/events/{slug}/uploads")
public class PublicUploadController {
 private final PublicUploadService service; public PublicUploadController(PublicUploadService service){this.service=service;}
 @PostMapping("/session") @ResponseStatus(HttpStatus.CREATED) public UploadDtos.Session create(@PathVariable String slug,@Valid @RequestBody UploadDtos.CreateSession req){return service.create(slug,req);}
 @PostMapping("/{mediaId}/finalize") public UploadDtos.Finalize finish(@PathVariable String slug,@PathVariable UUID mediaId){return service.finalizeUpload(slug,mediaId);}
}
