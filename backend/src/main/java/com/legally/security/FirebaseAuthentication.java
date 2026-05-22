package com.legally.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class FirebaseAuthentication extends AbstractAuthenticationToken {

    private final String uid;
    private final boolean anonymous;

    public FirebaseAuthentication(String uid, boolean anonymous) {
        super(null);
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
