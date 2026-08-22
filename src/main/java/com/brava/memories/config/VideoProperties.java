package com.brava.memories.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.video")
public record VideoProperties(boolean transcodingEnabled, String ffmpegPath, Duration timeout) {}
