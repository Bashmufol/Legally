package com.legally.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the authenticated Firebase UID from the Spring Security context.
 */
public final class AuthContext {

    /** Principal id when Firebase auth is disabled or no token was sent. */
    public static final String GUEST_UID = "guest";

    private AuthContext() {
    }

    /** Returns the Firebase UID, or {@link #GUEST_UID} when unauthenticated. */
    public static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return GUEST_UID;
        }
        return auth.getPrincipal().toString();
    }

    /** True when a real Firebase user is authenticated (not guest). */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && auth.getPrincipal() != null
                && !GUEST_UID.equals(auth.getPrincipal().toString());
    }
}
