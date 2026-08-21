package com.brava.memories;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EventMemoriesApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventMemoriesApplication.class, args);
    }
}
