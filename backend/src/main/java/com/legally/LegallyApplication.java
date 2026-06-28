package com.legally;

import com.legally.config.LegallyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Legally Spring Boot API entry point. */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(LegallyProperties.class)
public class LegallyApplication {

    /** Starts the Spring Boot application. */
    public static void main(String[] args) {
        SpringApplication.run(LegallyApplication.class, args);
    }
}
