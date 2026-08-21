package com.brava.memories.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.Set;

@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(
        long maxImageBytes,
        long maxVideoBytes,
        int maxFilesPerRequest,
        Set<String> imageMimeTypes,
        Set<String> videoMimeTypes,
        Duration signedUrlTtl
) {}
