package com.legally.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DatabaseStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupLogger.class);

    private final LegallyProperties properties;
    private final Environment environment;

    public DatabaseStartupLogger(LegallyProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDatabaseMode() {
        String mode = properties.getDatabase().getMode();
        switch (mode.toLowerCase()) {
            case "cloud-sql" -> log.info(
                    "Database mode: cloud-sql (Firebase SQL Connect → Cloud SQL). "
                            + "Instance={}, db={}",
                    properties.getDatabase().getCloudSqlInstanceConnectionName(),
                    properties.getDatabase().getName());
            case "direct" -> log.info(
                    "Database mode: direct JDBC — url={}",
                    environment.getProperty("spring.datasource.url", "(not set)"));
            default -> log.info(
                    "Database mode: local — url={}",
                    environment.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/legally"));
        }
    }
}
