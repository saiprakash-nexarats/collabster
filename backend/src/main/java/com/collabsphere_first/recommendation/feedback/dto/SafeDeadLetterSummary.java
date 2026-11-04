package com.collabsphere_first.recommendation.feedback.dto;

import com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe summary for dead-letter feedback outbox events.
 * Exposes audit metadata (eventId, eventType, sanitized errorCode, attemptCount, occurredAt)
 * without exposing sensitive user payload details.
 */
public record SafeDeadLetterSummary(
        UUID eventId,
        String eventType,
        String sanitizedErrorCode,
        int attemptCount,
        Instant occurredAt
) {
    public SafeDeadLetterSummary(UUID eventId, FeedbackEventType eventType, String sanitizedErrorCode, int attemptCount, Instant occurredAt) {
        this(eventId, eventType != null ? eventType.name() : null, sanitizedErrorCode, attemptCount, occurredAt);
    }
}
