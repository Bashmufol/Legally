package com.legally.repository;

import com.legally.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JPA access for browser sessions scoped to a Firebase user. */
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    /** Finds a session owned by the given user. */
    Optional<UserSession> findByIdAndFirebaseUid(UUID sessionId, String firebaseUid);

    /** Sessions with no activity before the cutoff (used by the cleanup job). */
    List<UserSession> findByLastActivityAtBefore(Instant cutoff);
}
