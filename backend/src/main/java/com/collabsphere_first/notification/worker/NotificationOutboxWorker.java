package com.collabsphere_first.notification.worker;

import com.collabsphere_first.notification.config.NotificationProperties;
import com.collabsphere_first.notification.delivery.email.EmailNotificationProvider;
import com.collabsphere_first.notification.delivery.push.PushNotificationProvider;
import com.collabsphere_first.notification.delivery.push.PushSendResult;
import com.collabsphere_first.notification.delivery.realtime.NotificationRealtimePublisher;
import com.collabsphere_first.notification.entity.Notification;
import com.collabsphere_first.notification.entity.NotificationDevice;
import com.collabsphere_first.notification.entity.NotificationOutbox;
import com.collabsphere_first.notification.enums.NotificationChannel;
import com.collabsphere_first.notification.event.NotificationCreatedEvent;
import com.collabsphere_first.notification.repository.NotificationRepository;
import com.collabsphere_first.notification.service.NotificationDeviceService;
import com.collabsphere_first.notification.service.NotificationOutboxService;
import com.collabsphere_first.notification.service.NotificationService;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.mail.MailParseException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxWorker {

    private final NotificationOutboxService outboxService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationDeviceService deviceService;
    private final NotificationRealtimePublisher realtimePublisher;
    private final PushNotificationProvider pushProvider;
    private final EmailNotificationProvider emailProvider;
    private final NotificationService notificationService;
    private final NotificationProperties properties;

    private final String nodeId = "worker-" + UUID.randomUUID();

    @Scheduled(fixedDelayString = "${collobster.notifications.outbox.worker-interval-ms:2000}")
    public void processOutbox() {
        try {
            int batchSize = properties.getOutbox().getBatchSize();
            List<NotificationOutbox> jobs = outboxService.claimJobs(nodeId, batchSize);
            if (jobs.isEmpty()) {
                return;
            }

            log.debug("Claimed {} notification outbox jobs on node {}", jobs.size(), nodeId);
            for (NotificationOutbox job : jobs) {
                processJob(job);
            }
        } catch (Exception ex) {
            log.error("Unhandled error during notification outbox processing: {}", ex.getMessage(), ex);
        }
    }

    @EventListener
    public void onNotificationCreated(NotificationCreatedEvent event) {
        // Immediate trigger to process outbox without waiting for next scheduled tick
        try {
            processOutbox();
        } catch (Exception ex) {
            log.debug("Event-triggered outbox processing yielded: {}", ex.getMessage());
        }
    }

    private void processJob(NotificationOutbox job) {
        Optional<Notification> notificationOpt = notificationRepository.findById(job.getNotificationId());
        if (notificationOpt.isEmpty()) {
            log.warn("Notification not found for outbox job {}", job.getId());
            outboxService.handleJobFailed(job.getId(), nodeId, job.getNotificationId(), job.getChannel(),
                    null, job.getAttemptCount(), "NOTIFICATION_NOT_FOUND", "Notification no longer exists");
            return;
        }

        Notification notification = notificationOpt.get();
        Optional<User> recipientOpt = userRepository.findById(notification.getRecipientUserId());

        switch (job.getChannel()) {
            case REALTIME -> processRealtime(job, notification, recipientOpt);
            case PUSH -> processPush(job, notification);
            case EMAIL -> processEmail(job, notification, recipientOpt);
        }
    }

    private void processRealtime(NotificationOutbox job, Notification notification, Optional<User> recipientOpt) {
        try {
            if (recipientOpt.isPresent()) {
                User recipient = recipientOpt.get();
                // Send to both username and numeric ID principal
                realtimePublisher.publishRealtimeNotification(recipient.getUsername(),
                        notificationService.mapToResponse(notification));
                realtimePublisher.publishRealtimeNotification(String.valueOf(recipient.getId()),
                        notificationService.mapToResponse(notification));
            }
            outboxService.handleJobSuccess(job.getId(), nodeId, notification.getId(), job.getChannel(),
                    "USER:" + notification.getRecipientUserId(), "realtime-ok", job.getAttemptCount());
        } catch (Exception ex) {
            log.warn("Realtime notification dispatch failed: {}", ex.getMessage());
            outboxService.handleJobRetry(job.getId(), nodeId, job.getAttemptCount(), "REALTIME_ERROR", ex.getMessage());
        }
    }

    private void processPush(NotificationOutbox job, Notification notification) {
        try {
            if (!pushProvider.isEnabled()) {
                outboxService.handleJobSkipped(job.getId(), nodeId, notification.getId(), job.getChannel(),
                        "PUSH_PROVIDER_DISABLED");
                return;
            }

            List<NotificationDevice> devices = deviceService.getActiveDevicesForUser(notification.getRecipientUserId());
            if (devices.isEmpty()) {
                outboxService.handleJobSkipped(job.getId(), nodeId, notification.getId(), job.getChannel(),
                        "NO_ACTIVE_DEVICES");
                return;
            }

            boolean anySuccess = false;
            boolean allPermanentFailure = true;
            String lastErrorCode = null;
            String lastErrorMessage = null;
            String providerMsgId = null;

            for (NotificationDevice device : devices) {
                PushSendResult result = pushProvider.send(device, notification);

                // Record delivery audit row per device
                outboxService.recordDelivery(
                        notification.getId(),
                        NotificationChannel.PUSH,
                        "DEVICE:" + device.getId() + ":" + device.getPlatform(),
                        result.isSuccess() ? com.collabsphere_first.notification.enums.NotificationDeliveryStatus.SUCCESS
                                           : com.collabsphere_first.notification.enums.NotificationDeliveryStatus.FAILED,
                        result.getProviderMessageId(),
                        job.getAttemptCount(),
                        result.getErrorCode()
                );

                if (result.isSuccess()) {
                    anySuccess = true;
                    allPermanentFailure = false;
                    providerMsgId = result.getProviderMessageId();
                } else {
                    lastErrorCode = result.getErrorCode();
                    lastErrorMessage = result.getErrorMessage();

                    if (result.isShouldRevokeToken()) {
                        log.info("Revoking invalid push token for device {}", device.getId());
                        deviceService.revokeDevice(device.getId(), device.getUserId());
                    }

                    if (result.isRetryable()) {
                        allPermanentFailure = false;
                    }
                }
            }

            if (anySuccess) {
                outboxService.handleJobSuccess(job.getId(), nodeId, notification.getId(), job.getChannel(),
                        "DEVICES:" + devices.size(), providerMsgId != null ? providerMsgId : "push-ok",
                        job.getAttemptCount());
            } else if (allPermanentFailure) {
                outboxService.handleJobFailed(job.getId(), nodeId, notification.getId(), job.getChannel(),
                        "DEVICES:" + devices.size(), job.getAttemptCount(),
                        lastErrorCode != null ? lastErrorCode : "PUSH_FAILED",
                        lastErrorMessage != null ? lastErrorMessage : "All push devices permanently failed");
            } else {
                outboxService.handleJobRetry(job.getId(), nodeId, job.getAttemptCount(),
                        lastErrorCode != null ? lastErrorCode : "PUSH_RETRYABLE",
                        lastErrorMessage != null ? lastErrorMessage : "Push delivery failed, will retry");
            }
        } catch (Exception ex) {
            log.error("Unhandled exception in push delivery for job {}: {}", job.getId(), ex.getMessage());
            outboxService.handleJobRetry(job.getId(), nodeId, job.getAttemptCount(), "UNHANDLED_PUSH_ERROR", ex.getMessage());
        }
    }

    private void processEmail(NotificationOutbox job, Notification notification, Optional<User> recipientOpt) {
        if (!emailProvider.isEnabled()) {
            outboxService.handleJobSkipped(job.getId(), nodeId, notification.getId(), job.getChannel(),
                    "EMAIL_PROVIDER_DISABLED");
            return;
        }

        if (recipientOpt.isEmpty() || recipientOpt.get().getEmail() == null || recipientOpt.get().getEmail().isBlank()) {
            outboxService.handleJobSkipped(job.getId(), nodeId, notification.getId(), job.getChannel(),
                    "NO_RECIPIENT_EMAIL");
            return;
        }

        User recipient = recipientOpt.get();
        try {
            emailProvider.sendNotificationEmail(recipient.getEmail(), recipient.getFirstName(), notification);
            outboxService.handleJobSuccess(job.getId(), nodeId, notification.getId(), job.getChannel(),
                    "USER:" + recipient.getId(), "email-ok", job.getAttemptCount());
        } catch (MailParseException | IllegalArgumentException ex) {
            log.warn("Permanent email delivery failure for notification {}: {}", notification.getId(), ex.getMessage());
            outboxService.handleJobFailed(job.getId(), nodeId, notification.getId(), job.getChannel(),
                    "USER:" + recipient.getId(), job.getAttemptCount(), "INVALID_EMAIL_FORMAT", ex.getMessage());
        } catch (Exception ex) {
            log.warn("Transient email delivery failure for notification {}: {}", notification.getId(), ex.getMessage());
            outboxService.handleJobRetry(job.getId(), nodeId, job.getAttemptCount(), "EMAIL_SEND_FAILED", ex.getMessage());
        }
    }
}
