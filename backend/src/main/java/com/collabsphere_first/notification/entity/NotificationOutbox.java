package com.collabsphere_first.notification.entity;

import com.collabsphere_first.notification.enums.NotificationChannel;
import com.collabsphere_first.notification.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_outbox", uniqueConstraints = {
        @UniqueConstraint(name = "uq_notification_outbox_channel", columnNames = {"notification_id", "channel"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "next_attempt_at", nullable = false)
    @Builder.Default
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by", length = 128)
    private String lockedBy;

    @Column(name = "last_error_code", length = 64)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 512)
    private String lastErrorMessage;

    public void markSuccess(Instant now) {
        this.status = OutboxStatus.SENT;
        this.lockedAt = null;
        this.lockedBy = null;
        this.updatedAt = now != null ? now : Instant.now();
    }

    public void markSkipped(String reason, Instant now) {
        this.status = OutboxStatus.SKIPPED;
        this.lockedAt = null;
        this.lockedBy = null;
        this.lastErrorCode = "SKIPPED";
        this.lastErrorMessage = reason;
        this.updatedAt = now != null ? now : Instant.now();
    }

    public void markFailed(String errorCode, String errorMessage, Instant now) {
        this.status = OutboxStatus.FAILED;
        this.lockedAt = null;
        this.lockedBy = null;
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage != null && errorMessage.length() > 512
                ? errorMessage.substring(0, 512) : errorMessage;
        this.updatedAt = now != null ? now : Instant.now();
    }

    public void markRetry(String errorCode, String errorMessage, Instant nextAttempt, Instant now) {
        this.status = OutboxStatus.PENDING;
        this.lockedAt = null;
        this.lockedBy = null;
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage != null && errorMessage.length() > 512
                ? errorMessage.substring(0, 512) : errorMessage;
        this.nextAttemptAt = nextAttempt;
        this.updatedAt = now != null ? now : Instant.now();
    }
}
