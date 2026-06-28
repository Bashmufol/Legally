package com.legally.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Browser session scoped by {@code X-Legally-Session-Id} and a Firebase user.
 */
@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_user_sessions_last_activity", columnList = "last_activity_at")
})
public class UserSession {

    /** Client-supplied session UUID from the request header. */
    @Id
    @Column(name = "session_id", nullable = false)
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, length = 128)
    private String firebaseUid;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    /** Used by the cleanup job to purge inactive sessions. */
    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt = Instant.now();

    protected UserSession() {
    }

    /** Creates a new session row for the given client session id and user. */
    public static UserSession create(UUID sessionId, String firebaseUid) {
        UserSession s = new UserSession();
        s.id = sessionId;
        s.firebaseUid = firebaseUid;
        return s;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return id;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
}
