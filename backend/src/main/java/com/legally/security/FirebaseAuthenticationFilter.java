package com.legally.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.legally.config.LegallyProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final LegallyProperties properties;

    public FirebaseAuthenticationFilter(LegallyProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = extractBearer(request);

        if (token != null && properties.getFirebase().isEnabled()) {
            try {
                FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
                if (properties.getFirebase().isAnonymousOnly() && !isAnonymousSignIn(decoded)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only anonymous sign-in is permitted");
                    return;
                }
                SecurityContextHolder.getContext().setAuthentication(
                        new FirebaseAuthentication(decoded.getUid(), isAnonymousSignIn(decoded)));
            } catch (FirebaseAuthException e) {
                if (properties.getFirebase().isRequireAuth()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Firebase token");
                    return;
                }
                setGuest();
            }
        } else if (properties.getFirebase().isRequireAuth()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authorization required");
            return;
        } else {
            setGuest();
        }

        filterChain.doFilter(request, response);
    }

    private void setGuest() {
        SecurityContextHolder.getContext().setAuthentication(
                new FirebaseAuthentication(AuthContext.GUEST_UID, true));
    }

    private String extractBearer(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private boolean isAnonymousSignIn(FirebaseToken token) {
        Object firebaseClaim = token.getClaims().get("firebase");
        if (firebaseClaim instanceof Map<?, ?> firebase) {
            Object provider = firebase.get("sign_in_provider");
            return "anonymous".equals(provider);
        }
        return false;
    }
}
