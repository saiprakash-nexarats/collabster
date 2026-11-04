package com.collabsphere_first.notification.service;

import com.collabsphere_first.notification.entity.NotificationOutbox;
import com.collabsphere_first.notification.enums.NotificationChannel;

import java.util.List;
import java.util.UUID;

public interface NotificationOutboxService {
    NotificationOutbox enqueue(UUID notificationId, NotificationChannel channel);
    List<NotificationOutbox> claimJobs(String nodeId, int limit);
    void handleJobSuccess(UUID outboxId, UUID notificationId, NotificationChannel channel, String destinationRef, String providerMessageId, int attemptCount);
    void handleJobSuccess(UUID outboxId, String nodeId, UUID notificationId, NotificationChannel channel, String destinationRef, String providerMessageId, int attemptCount);
    void handleJobRetry(UUID outboxId, int attemptCount, String errorCode, String errorMessage);
    void handleJobRetry(UUID outboxId, String nodeId, int attemptCount, String errorCode, String errorMessage);
    void handleJobFailed(UUID outboxId, UUID notificationId, NotificationChannel channel, String destinationRef, int attemptCount, String errorCode, String errorMessage);
    void handleJobFailed(UUID outboxId, String nodeId, UUID notificationId, NotificationChannel channel, String destinationRef, int attemptCount, String errorCode, String errorMessage);
    void handleJobSkipped(UUID outboxId, UUID notificationId, NotificationChannel channel, String reason);
    void handleJobSkipped(UUID outboxId, String nodeId, UUID notificationId, NotificationChannel channel, String reason);
    void recordDelivery(UUID notificationId, NotificationChannel channel, String destinationRef, com.collabsphere_first.notification.enums.NotificationDeliveryStatus status, String providerMessageId, int attemptCount, String errorCode);
}
