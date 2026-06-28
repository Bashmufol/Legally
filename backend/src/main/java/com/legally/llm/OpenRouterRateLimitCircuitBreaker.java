package com.legally.llm;

/**
 * Per-request breaker: after the first OpenRouter 429/rate-limit in a consult request,
 * skip further OpenRouter calls (legal, contacts, documents) for that request.
 */
public final class OpenRouterRateLimitCircuitBreaker {

    private static final ThreadLocal<Boolean> OPEN = ThreadLocal.withInitial(() -> false);

    private OpenRouterRateLimitCircuitBreaker() {
    }

    /** True when this provider should be skipped for the rest of the request. */
    public static boolean isOpen() {
        return OPEN.get();
    }

    /** Marks this provider as rate-limited for the rest of the request. */
    public static void open() {
        OPEN.set(true);
    }

    /** Clears the per-request circuit breaker state. */
    public static void reset() {
        OPEN.set(false);
    }

    /** clear. */
    public static void clear() {
        OPEN.remove();
    }
}
