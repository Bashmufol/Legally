package com.legally.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_user_sessions_last_activity", columnList = "last_activity_at")
})
public class UserSession {

    @Id
    @Column(name = "session_id", nullable = false)
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, length = 128)
    private String firebaseUid;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt = Instant.now();

    protected UserSession() {
    }

    public static UserSession create(UUID sessionId, String firebaseUid) {
        UserSession s = new UserSession();
        s.id = sessionId;
        s.firebaseUid = firebaseUid;
        return s;
    }

    public UUID getId() {
        return id;
    }

    /** Same as {@link #getId()} — client-supplied session UUID. */
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
