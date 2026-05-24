package com.legally.security;

import java.util.Optional;
import java.util.UUID;

public final class SessionContext {

    public static final String HEADER_NAME = "X-Legally-Session-Id";

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private SessionContext() {
    }

    public static void set(UUID sessionId) {
        CURRENT.set(sessionId);
    }

    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UUID require() {
        return current().orElseThrow(() -> new IllegalStateException("Missing " + HEADER_NAME + " header"));
    }

    public static void clear() {
        CURRENT.remove();
    }
}
