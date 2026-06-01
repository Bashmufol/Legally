package com.legally.llm;

public final class GeminiQuotaCircuitBreaker {

    private static final ThreadLocal<Boolean> OPEN = ThreadLocal.withInitial(() -> false);

    private GeminiQuotaCircuitBreaker() {
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
