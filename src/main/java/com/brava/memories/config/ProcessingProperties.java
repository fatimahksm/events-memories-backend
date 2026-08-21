package com.brava.memories.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="app.processing")
public record ProcessingProperties(int coreThreads,int maxThreads,int queueCapacity,int recoveryBatchSize) {}
