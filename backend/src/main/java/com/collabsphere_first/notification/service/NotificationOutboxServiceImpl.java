package com.collabsphere_first.notification.service;

import com.collabsphere_first.notification.config.NotificationProperties;
import com.collabsphere_first.notification.entity.NotificationDelivery;
import com.collabsphere_first.notification.entity.NotificationOutbox;
import com.collabsphere_first.notification.enums.NotificationChannel;
import com.collabsphere_first.notification.enums.NotificationDeliveryStatus;
import com.collabsphere_first.notification.enums.OutboxStatus;
import com.collabsphere_first.notification.repository.NotificationDeliveryRepository;
import com.collabsphere_first.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxServiceImpl implements NotificationOutboxService {

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationProperties properties;

    @Override
    @Transactional
    public NotificationOutbox enqueue(UUID notificationId, NotificationChannel channel) {
        Optional<NotificationOutbox> existing = outboxRepository.findByNotificationIdAndChannel(notificationId, channel);
        if (existing.isPresent()) {
            return existing.get();
        }

        NotificationOutbox outbox = NotificationOutbox.builder()
                .notificationId(notificationId)
                .channel(channel)
                .status(OutboxStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        NotificationOutbox saved = outboxRepository.save(outbox);
        log.debug("Enqueued notification outbox job (notificationId={}, channel={})", notificationId, channel);
        return saved;
    }

    @Override
    @Transactional
    public List<NotificationOutbox> claimJobs(String nodeId, int limit) {
        Instant now = Instant.now();
        Instant staleLockCutoff = now.minus(Duration.ofSeconds(properties.getOutbox().getLockLeaseSeconds()));

        List<UUID> claimableIds = outboxRepository.findClaimableJobIds(now, staleLockCutoff, limit);
        if (claimableIds.isEmpty()) {
            return Collections.emptyList();
        }

        outboxRepository.claimJobs(claimableIds, nodeId, now);
        return outboxRepository.findByIdIn(claimableIds);
    }

    @Override
    @Transactional
    public void handleJobSuccess(UUID outboxId, UUID notificationId, NotificationChannel channel,
                                 String destinationRef, String providerMessageId, int attemptCount) {
        handleJobSuccess(outboxId, null, notificationId, channel, destinationRef, providerMessageId, attemptCount);
    }

    @Override
    @Transactional
    public void handleJobSuccess(UUID outboxId, String nodeId, UUID notificationId, NotificationChannel channel,
                                 String destinationRef, String providerMessageId, int attemptCount) {
        Instant now = Instant.now();
        Optional<NotificationOutbox> jobOpt = outboxRepository.findById(outboxId);
        if (jobOpt.isEmpty()) {
            return;
        }

        NotificationOutbox job = jobOpt.get();
        if (nodeId != null && job.getLockedBy() != null && !nodeId.equals(job.getLockedBy())) {
            log.warn("Stale worker {} rejected for success on job {}: currently locked by {}",
                    nodeId, outboxId, job.getLockedBy());
            return;
        }
        if (job.getStatus() == OutboxStatus.SENT || job.getStatus() == OutboxStatus.FAILED || job.getStatus() == OutboxStatus.SKIPPED) {
            log.warn("Stale worker {} ignored on completed job {} (status={})", nodeId, outboxId, job.getStatus());
            return;
        }

        job.markSuccess(now);
        outboxRepository.save(job);

        NotificationDelivery delivery = NotificationDelivery.builder()
                .notificationId(notificationId)
                .channel(channel)
                .destinationReference(destinationRef)
                .status(NotificationDeliveryStatus.SUCCESS)
                .providerMessageId(providerMessageId)
                .attemptCount(attemptCount)
                .sentAt(now)
                .build();
        deliveryRepository.save(delivery);
        log.info("Notification outbox job succeeded (outboxId={}, notificationId={}, channel={}, attempts={})",
                outboxId, notificationId, channel, attemptCount);
    }

    @Override
    @Transactional
    public void handleJobRetry(UUID outboxId, int attemptCount, String errorCode, String errorMessage) {
        handleJobRetry(outboxId, null, attemptCount, errorCode, errorMessage);
    }

    @Override
    @Transactional
    public void handleJobRetry(UUID outboxId, String nodeId, int attemptCount, String errorCode, String errorMessage) {
        Instant now = Instant.now();
        Optional<NotificationOutbox> jobOpt = outboxRepository.findById(outboxId);
        if (jobOpt.isEmpty()) {
            return;
        }

        NotificationOutbox job = jobOpt.get();
        if (nodeId != null && job.getLockedBy() != null && !nodeId.equals(job.getLockedBy())) {
            log.warn("Stale worker {} rejected for retry on job {}: currently locked by {}",
                    nodeId, outboxId, job.getLockedBy());
            return;
        }
        if (job.getStatus() == OutboxStatus.SENT || job.getStatus() == OutboxStatus.FAILED || job.getStatus() == OutboxStatus.SKIPPED) {
            log.warn("Stale worker {} retry ignored on completed job {} (status={})", nodeId, outboxId, job.getStatus());
            return;
        }

        int maxAttempts = properties.getOutbox().getMaxAttempts();
        if (attemptCount >= maxAttempts) {
            log.warn("Exceeded max attempts ({}) for outbox job {}. Marking as FAILED.", maxAttempts, outboxId);
            job.markFailed(errorCode, errorMessage, now);
            outboxRepository.save(job);

            NotificationDelivery delivery = NotificationDelivery.builder()
                    .notificationId(job.getNotificationId())
                    .channel(job.getChannel())
                    .status(NotificationDeliveryStatus.FAILED)
                    .attemptCount(attemptCount)
                    .errorCode(errorCode)
                    .sentAt(now)
                    .build();
            deliveryRepository.save(delivery);
            return;
        }

        long backoffSeconds = calculateBackoffSeconds(attemptCount);
        Instant nextAttempt = now.plus(Duration.ofSeconds(backoffSeconds));

        job.markRetry(errorCode, errorMessage, nextAttempt, now);
        outboxRepository.save(job);
        log.info("Scheduled retry for outbox job {} in {} seconds (attempt={})",
                outboxId, backoffSeconds, attemptCount);
    }

    @Override
    @Transactional
    public void handleJobFailed(UUID outboxId, UUID notificationId, NotificationChannel channel,
                                String destinationRef, int attemptCount, String errorCode, String errorMessage) {
        handleJobFailed(outboxId, null, notificationId, channel, destinationRef, attemptCount, errorCode, errorMessage);
    }

    @Override
    @Transactional
    public void handleJobFailed(UUID outboxId, String nodeId, UUID notificationId, NotificationChannel channel,
                                String destinationRef, int attemptCount, String errorCode, String errorMessage) {
        Instant now = Instant.now();
        Optional<NotificationOutbox> jobOpt = outboxRepository.findById(outboxId);
        if (jobOpt.isEmpty()) {
            return;
        }

        NotificationOutbox job = jobOpt.get();
        if (nodeId != null && job.getLockedBy() != null && !nodeId.equals(job.getLockedBy())) {
            log.warn("Stale worker {} rejected for failed on job {}: currently locked by {}",
                    nodeId, outboxId, job.getLockedBy());
            return;
        }
        if (job.getStatus() == OutboxStatus.SENT || job.getStatus() == OutboxStatus.FAILED || job.getStatus() == OutboxStatus.SKIPPED) {
            log.warn("Stale worker {} fail ignored on completed job {} (status={})", nodeId, outboxId, job.getStatus());
            return;
        }

        job.markFailed(errorCode, errorMessage, now);
        outboxRepository.save(job);

        NotificationDelivery delivery = NotificationDelivery.builder()
                .notificationId(notificationId)
                .channel(channel)
                .destinationReference(destinationRef)
                .status(NotificationDeliveryStatus.FAILED)
                .attemptCount(attemptCount)
                .errorCode(errorCode)
                .sentAt(now)
                .build();
        deliveryRepository.save(delivery);
        log.warn("Notification outbox job permanently failed (outboxId={}, notificationId={}, channel={}, errorCode={})",
                outboxId, notificationId, channel, errorCode);
    }

    @Override
    @Transactional
    public void handleJobSkipped(UUID outboxId, UUID notificationId, NotificationChannel channel, String reason) {
        handleJobSkipped(outboxId, null, notificationId, channel, reason);
    }

    @Override
    @Transactional
    public void handleJobSkipped(UUID outboxId, String nodeId, UUID notificationId, NotificationChannel channel, String reason) {
        Instant now = Instant.now();
        Optional<NotificationOutbox> jobOpt = outboxRepository.findById(outboxId);
        if (jobOpt.isEmpty()) {
            return;
        }

        NotificationOutbox job = jobOpt.get();
        if (nodeId != null && job.getLockedBy() != null && !nodeId.equals(job.getLockedBy())) {
            log.warn("Stale worker {} rejected for skipped on job {}: currently locked by {}",
                    nodeId, outboxId, job.getLockedBy());
            return;
        }
        if (job.getStatus() == OutboxStatus.SENT || job.getStatus() == OutboxStatus.FAILED || job.getStatus() == OutboxStatus.SKIPPED) {
            log.warn("Stale worker {} skipped ignored on completed job {} (status={})", nodeId, outboxId, job.getStatus());
            return;
        }

        job.markSkipped(reason, now);
        outboxRepository.save(job);

        NotificationDelivery delivery = NotificationDelivery.builder()
                .notificationId(notificationId)
                .channel(channel)
                .status(NotificationDeliveryStatus.SKIPPED)
                .attemptCount(0)
                .errorCode("SKIPPED")
                .sentAt(now)
                .build();
        deliveryRepository.save(delivery);
        log.info("Notification outbox job skipped (outboxId={}, notificationId={}, channel={}, reason={})",
                outboxId, notificationId, channel, reason);
    }

    @Override
    @Transactional
    public void recordDelivery(UUID notificationId, NotificationChannel channel, String destinationRef,
                               NotificationDeliveryStatus status, String providerMessageId,
                               int attemptCount, String errorCode) {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .notificationId(notificationId)
                .channel(channel)
                .destinationReference(destinationRef)
                .status(status)
                .providerMessageId(providerMessageId)
                .attemptCount(attemptCount)
                .errorCode(errorCode)
                .sentAt(Instant.now())
                .build();
        deliveryRepository.save(delivery);
    }

    private long calculateBackoffSeconds(int attemptCount) {
        long base = properties.getOutbox().getBaseBackoffSeconds();
        // 2^(attempt-1) bounded backoff, capped at 3600 seconds
        long factor = 1L << Math.min(attemptCount - 1, 10);
        return Math.min(base * factor, 3600L);
    }
}
