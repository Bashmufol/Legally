package com.legally.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

/**
 * Spring Security token holding the Firebase UID after token verification.
 */
public class FirebaseAuthentication extends AbstractAuthenticationToken {

    private final String uid;
    private final boolean anonymous;

    public FirebaseAuthentication(String uid, boolean anonymous) {
        super(Collections.emptyList());
        this.uid = uid;
        this.anonymous = anonymous;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return uid;
    }

    public boolean isAnonymous() {
        return anonymous;
    }
}
