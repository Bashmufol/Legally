package com.legally.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads {@link SessionContext#HEADER_NAME} and binds it for the duration of the request.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SessionHeaderFilter extends OncePerRequestFilter {

    @Override
    /** should not filter. */
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/actuator/");
    }

    @Override
    /** do filter internal. */
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String raw = request.getHeader(SessionContext.HEADER_NAME);
            if (raw != null && !raw.isBlank()) {
                try {
                    SessionContext.set(UUID.fromString(raw.trim()));
                } catch (IllegalArgumentException ignored) {
                    // Invalid UUID in header; proceed without a session id.
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            SessionContext.clear();
        }
    }
}
