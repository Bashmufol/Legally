package com.legally.security;

import com.legally.llm.GeminiQuotaCircuitBreaker;
import com.legally.llm.OpenRouterRateLimitCircuitBreaker;
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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GeminiQuotaCircuitBreakerFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        return request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        GeminiQuotaCircuitBreaker.reset();
        OpenRouterRateLimitCircuitBreaker.reset();
        try {
            filterChain.doFilter(request, response);
        } finally {
            GeminiQuotaCircuitBreaker.clear();
            OpenRouterRateLimitCircuitBreaker.clear();
        }
    }
}
