package com.brava.memories.upload;
import com.brava.memories.common.exception.AppException;
import com.brava.memories.config.UploadProperties;
import com.brava.memories.media.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
@Service
public class UploadValidationService {
 private final UploadProperties p; public UploadValidationService(UploadProperties p){this.p=p;}
 public MediaType validateMetadata(UploadDtos.CreateSession r){boolean image=p.imageMimeTypes().contains(r.contentType());boolean video=p.videoMimeTypes().contains(r.contentType());if(!image&&!video)throw bad("UNSUPPORTED_MEDIA_TYPE","Unsupported media type");if(image&&r.size()>p.maxImageBytes())throw bad("FILE_TOO_LARGE","Image exceeds the allowed size");if(video&&r.size()>p.maxVideoBytes())throw bad("FILE_TOO_LARGE","Video exceeds the allowed size");String ext=extension(r.fileName());if(!extensionMatches(ext,r.contentType()))throw bad("INVALID_FILE","File extension does not match its media type");return image?MediaType.IMAGE:MediaType.VIDEO;}
 public void validateSignature(InputStream in,String mime){try{byte[] h=in.readNBytes(16);boolean ok=switch(mime){case "image/jpeg"->h.length>=3&&(h[0]&255)==0xFF&&(h[1]&255)==0xD8&&(h[2]&255)==0xFF;case "image/png"->h.length>=8&&(h[0]&255)==0x89&&h[1]=='P'&&h[2]=='N'&&h[3]=='G';case "image/webp"->h.length>=12&&ascii(h,0,4).equals("RIFF")&&ascii(h,8,4).equals("WEBP");case "video/mp4","video/quicktime"->h.length>=12&&ascii(h,4,4).equals("ftyp");default->false;};if(!ok)throw bad("INVALID_FILE","File content does not match the declared format");}catch(IOException e){throw bad("INVALID_FILE","Unable to validate uploaded file");}}
 private static String ascii(byte[] b,int o,int l){return new String(b,o,l,StandardCharsets.US_ASCII);} private static String extension(String n){int i=n.lastIndexOf('.');return i<0?"":n.substring(i+1).toLowerCase(Locale.ROOT);} private boolean extensionMatches(String e,String mime){return switch(mime){case "image/jpeg"->e.equals("jpg")||e.equals("jpeg");case "image/png"->e.equals("png");case "image/webp"->e.equals("webp");case "video/mp4"->e.equals("mp4");case "video/quicktime"->e.equals("mov");default->false;};} private AppException bad(String c,String m){return new AppException(c,m,HttpStatus.BAD_REQUEST);}
}
