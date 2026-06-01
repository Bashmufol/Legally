package com.legally.llm;

public final class LlmHttpErrors {

    private LlmHttpErrors() {
    }

    public static boolean isRateLimited(Throwable e) {
        if (e == null) {
            return false;
        }
        String msg = e.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            if (lower.contains("429")
                    || lower.contains("too many requests")
                    || lower.contains("rate-limited")
                    || lower.contains("rate limit")) {
                return true;
            }
        }
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            return isRateLimited(cause);
        }
        return false;
    }

    public static boolean isQuotaExceeded(Throwable e) {
        if (isRateLimited(e)) {
            return true;
        }
        if (e == null) {
            return false;
        }
        String msg = e.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            if (lower.contains("resource_exhausted") || lower.contains("quota")) {
                return true;
            }
        }
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            return isQuotaExceeded(cause);
        }
        return false;
    }
}
