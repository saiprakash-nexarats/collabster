package com.collabsphere_first.recommendation.feedback.entity;

import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType;
import com.collabsphere_first.recommendation.feedback.enums.FeedbackOutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recommendation_feedback_outbox", uniqueConstraints = {
        @UniqueConstraint(name = "uq_rec_feedback_outbox_event_id", columnNames = {"event_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationFeedbackOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "contract_version", nullable = false, length = 32)
    @Builder.Default
    private String contractVersion = "1";

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private FeedbackEventType eventType;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 64)
    private ContentType contentType;

    @Column(name = "content_id")
    private UUID contentId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private FeedbackOutboxStatus status = FeedbackOutboxStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "next_attempt_at", nullable = false)
    @Builder.Default
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "lock_owner", length = 128)
    private String lockOwner;

    @Column(name = "last_error_code", length = 64)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 512)
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    public void markDelivered(Instant now) {
        this.status = FeedbackOutboxStatus.DELIVERED;
        this.lockedAt = null;
        this.lockOwner = null;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
        this.updatedAt = now != null ? now : Instant.now();
    }

    public void markRetry(Instant nextAttempt, String errorCode, String errorMessage, Instant now) {
        this.status = FeedbackOutboxStatus.RETRY;
        this.lockedAt = null;
        this.lockOwner = null;
        this.nextAttemptAt = nextAttempt;
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage != null && errorMessage.length() > 512 ? errorMessage.substring(0, 512) : errorMessage;
        this.updatedAt = now != null ? now : Instant.now();
    }

    public void markDeadLetter(String errorCode, String errorMessage, Instant now) {
        this.status = FeedbackOutboxStatus.DEAD_LETTER;
        this.lockedAt = null;
        this.lockOwner = null;
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage != null && errorMessage.length() > 512 ? errorMessage.substring(0, 512) : errorMessage;
        this.updatedAt = now != null ? now : Instant.now();
    }

    public void markFailed(String errorCode, String errorMessage, Instant now) {
        markDeadLetter(errorCode, errorMessage, now);
    }
}
