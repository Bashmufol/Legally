package com.legally.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Firebase-authenticated user stored in PostgreSQL.
 */
@Entity
@Table(name = "app_users")
public class AppUser {

    /** Firebase UID from the ID token. */
    @Id
    @Column(name = "firebase_uid", length = 128)
    private String firebaseUid;

    /** When this user row was first created. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** Last API activity for this user. */
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    protected AppUser() {
    }

    public AppUser(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    /** Updates lastSeenAt to the current time. */
    public void touch() {
        this.lastSeenAt = Instant.now();
    }
}
