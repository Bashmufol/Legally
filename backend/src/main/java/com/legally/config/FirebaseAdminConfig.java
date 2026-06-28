package com.legally.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
/**
 * Initializes Firebase Admin SDK when enabled.
 */
public class FirebaseAdminConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAdminConfig.class);

    private final LegallyProperties properties;

    public FirebaseAdminConfig(LegallyProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() throws IOException {
        if (!properties.getFirebase().isEnabled()) {
            log.info("Firebase Admin SDK disabled");
            return;
        }
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        String credentialsPath = properties.getFirebase().getCredentialsPath();
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("FIREBASE_ENABLED=true but FIREBASE_CREDENTIALS_PATH is empty");
            return;
        }

        try (InputStream stream = new FileInputStream(credentialsPath)) {
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream));

            if (properties.getFirebase().getProjectId() != null
                    && !properties.getFirebase().getProjectId().isBlank()) {
                builder.setProjectId(properties.getFirebase().getProjectId());
            }
            if (properties.getFirebase().getStorageBucket() != null
                    && !properties.getFirebase().getStorageBucket().isBlank()) {
                builder.setStorageBucket(properties.getFirebase().getStorageBucket());
            }

            FirebaseApp.initializeApp(builder.build());
            log.info("Firebase Admin SDK initialized");
        }
    }
}
