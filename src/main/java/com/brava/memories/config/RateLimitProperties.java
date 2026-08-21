package com.brava.memories.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="app.rate-limit")
public record RateLimitProperties(int publicMutationsPerMinute,int loginAttemptsPerFiveMinutes){}
