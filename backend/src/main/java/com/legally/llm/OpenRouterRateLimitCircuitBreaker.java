package com.legally.llm;

/**
 * Per-request breaker: after the first OpenRouter 429/rate-limit in a consult request,
 * skip further OpenRouter calls (legal, contacts, documents) for that request.
 */
public final class OpenRouterRateLimitCircuitBreaker {

    private static final ThreadLocal<Boolean> OPEN = ThreadLocal.withInitial(() -> false);

    private OpenRouterRateLimitCircuitBreaker() {
    }

    public static boolean isOpen() {
        return OPEN.get();
    }

    public static void open() {
        OPEN.set(true);
    }

    public static void reset() {
        OPEN.set(false);
    }

    public static void clear() {
        OPEN.remove();
    }
}
