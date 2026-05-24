package com.legally;

import com.legally.config.LegallyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(LegallyProperties.class)
public class LegallyApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegallyApplication.class, args);
    }
}
