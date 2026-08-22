package com.brava.memories.storage;
import com.brava.memories.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Service
@ConditionalOnProperty(name="app.storage.provider",havingValue="r2")
public class R2ObjectStorage implements ObjectStorage {
 private final S3Client s3; private final S3Presigner presigner; private final String bucket;
 public R2ObjectStorage(AppProperties props){var st=props.storage();var credentials=StaticCredentialsProvider.create(AwsBasicCredentials.create(st.r2AccessKeyId(),st.r2SecretAccessKey()));URI endpoint=URI.create("https://"+st.r2AccountId()+".r2.cloudflarestorage.com");ClientOverrideConfiguration timeouts=ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofSeconds(30)).apiCallAttemptTimeout(Duration.ofSeconds(15)).build();this.s3=S3Client.builder().endpointOverride(endpoint).region(Region.of("auto")).credentialsProvider(credentials).forcePathStyle(true).overrideConfiguration(timeouts).build();this.presigner=S3Presigner.builder().endpointOverride(endpoint).region(Region.of("auto")).credentialsProvider(credentials).build();this.bucket=st.r2Bucket();}
 public String createUploadUrl(String key,String type,long len,Duration ttl){PutObjectRequest put=PutObjectRequest.builder().bucket(bucket).key(key).contentType(type).contentLength(len).build();return presigner.presignPutObject(PutObjectPresignRequest.builder().signatureDuration(ttl).putObjectRequest(put).build()).url().toString();}
 public String createDownloadUrl(String key,Duration ttl){GetObjectRequest get=GetObjectRequest.builder().bucket(bucket).key(key).build();return presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(ttl).getObjectRequest(get).build()).url().toString();}
 public boolean exists(String key){try{s3.headObject(b->b.bucket(bucket).key(key));return true;}catch(S3Exception ex){return false;}}
 public long contentLength(String key){return s3.headObject(b->b.bucket(bucket).key(key)).contentLength();}
 public String contentType(String key){return s3.headObject(b->b.bucket(bucket).key(key)).contentType();}
 public InputStream open(String key){return s3.getObject(b->b.bucket(bucket).key(key));}
 public void write(String key,InputStream input,long contentLength,String contentType){s3.putObject(b->b.bucket(bucket).key(key).contentLength(contentLength).contentType(contentType),RequestBody.fromInputStream(input,contentLength));}
 public void move(String source,String target){s3.copyObject(b->b.copySource(bucket+"/"+source).destinationBucket(bucket).destinationKey(target));s3.deleteObject(b->b.bucket(bucket).key(source));}
 public void delete(String key){s3.deleteObject(b->b.bucket(bucket).key(key));}
}
