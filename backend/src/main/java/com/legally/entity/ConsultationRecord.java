package com.legally.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted consult request and full JSON response for history.
 */
@Entity
@Table(name = "consultations", indexes = {
        @Index(name = "idx_consultations_user_created", columnList = "firebase_uid, created_at")
})
public class ConsultationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, length = 128)
    private String firebaseUid;

    @Column(name = "session_id")
    private UUID sessionId;

    /** Scenario slug from the client (e.g. tenancy, employment). */
    @Column(nullable = false, length = 64)
    private String scenario;

    @Column(name = "user_message", nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 32)
    private String confidence;

    /** Serialized {@code ConsultResponse} JSON. */
    @Column(name = "response_json", columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ConsultationRecord() {
    }

    /** Factory for a new consultation history row. */
    public static ConsultationRecord create(
            String firebaseUid,
            UUID sessionId,
            String scenario,
            String userMessage,
            String summary,
            String confidence,
            String responseJson) {
        ConsultationRecord r = new ConsultationRecord();
        r.firebaseUid = firebaseUid;
        r.sessionId = sessionId;
        r.scenario = scenario;
        r.userMessage = userMessage;
        r.summary = summary;
        r.confidence = confidence;
        r.responseJson = responseJson;
        return r;
    }

    public UUID getId() {
        return id;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getScenario() {
        return scenario;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getSummary() {
        return summary;
    }

    public String getConfidence() {
        return confidence;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
