package com.legally.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly job that purges sessions past {@code SESSION_TTL_HOURS}.
 */
@Component
public class SessionCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupScheduler.class);

    private final SessionService sessionService;

    public SessionCleanupScheduler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /** Scheduled entry point for expired session cleanup. */
    @Scheduled(cron = "${legally.session.cleanup-cron:0 0 * * * *}")
    public void purgeExpiredSessions() {
        try {
            sessionService.purgeExpiredSessions();
        } catch (Exception e) {
            log.error("Session cleanup failed: {}", e.getMessage(), e);
        }
    }
}
