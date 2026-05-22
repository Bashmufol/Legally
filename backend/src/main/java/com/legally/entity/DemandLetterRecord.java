package com.legally.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "demand_letters")
public class DemandLetterRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, length = 128)
    private String firebaseUid;

    @Column(length = 64)
    private String scenario;

    @Column(columnDefinition = "TEXT")
    private String facts;

    @Column(name = "letter_text", columnDefinition = "TEXT")
    private String letterText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected DemandLetterRecord() {
    }

    public static DemandLetterRecord create(String firebaseUid, String scenario, String facts, String letterText) {
        DemandLetterRecord r = new DemandLetterRecord();
        r.firebaseUid = firebaseUid;
        r.scenario = scenario;
        r.facts = facts;
        r.letterText = letterText;
        return r;
    }

    public UUID getId() {
        return id;
    }
}
