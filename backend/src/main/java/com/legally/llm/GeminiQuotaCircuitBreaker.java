package com.legally.llm;

/**
 * Per-request flag to skip Gemini after a quota error.
 */
public final class GeminiQuotaCircuitBreaker {

    private static final ThreadLocal<Boolean> OPEN = ThreadLocal.withInitial(() -> false);

    private GeminiQuotaCircuitBreaker() {
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
