package com.legally.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Request-scoped session id from {@link #HEADER_NAME}, stored in a ThreadLocal.
 */
public final class SessionContext {

    /** Client header that carries the session UUID. */
    public static final String HEADER_NAME = "X-Legally-Session-Id";

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private SessionContext() {
    }

    /** Binds the session id for the current request thread. */
    public static void set(UUID sessionId) {
        CURRENT.set(sessionId);
    }

    /** current. */
    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /** Returns the session id or throws if the header was missing or invalid. */
    public static UUID require() {
        return current().orElseThrow(() -> new IllegalStateException("Missing " + HEADER_NAME + " header"));
    }

    /** Clears the ThreadLocal after the request completes. */
    public static void clear() {
        CURRENT.remove();
    }
}
