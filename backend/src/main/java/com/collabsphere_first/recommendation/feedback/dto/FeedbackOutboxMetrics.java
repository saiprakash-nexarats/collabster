package com.collabsphere_first.recommendation.feedback.dto;

import java.time.Duration;
import java.time.Instant;

/**
 * Operational metrics for recommendation feedback outbox queue.
 * Does not expose sensitive user payloads.
 */
public record FeedbackOutboxMetrics(
        long pendingCount,
        long inProgressCount,
        long retryCount,
        long deliveredCount,
        long deadLetterCount,
        Instant oldestPendingCreatedAt,
        Long oldestPendingAgeSeconds
) {
    public static FeedbackOutboxMetrics of(
            long pendingCount,
            long inProgressCount,
            long retryCount,
            long deliveredCount,
            long deadLetterCount,
            Instant oldestPendingCreatedAt,
            Instant now
    ) {
        Long ageSeconds = null;
        if (oldestPendingCreatedAt != null && now != null) {
            ageSeconds = Math.max(0, Duration.between(oldestPendingCreatedAt, now).getSeconds());
        }
        return new FeedbackOutboxMetrics(
                pendingCount,
                inProgressCount,
                retryCount,
                deliveredCount,
                deadLetterCount,
                oldestPendingCreatedAt,
                ageSeconds
        );
    }
}
