package com.collabsphere_first.recommendation.feedback.service;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.recommendation.feedback.config.RecommendationFeedbackProperties;
import com.collabsphere_first.recommendation.feedback.dto.RecommendationEventDto;
import com.collabsphere_first.recommendation.feedback.dto.RecommendationFeedbackBatchResponse;
import com.collabsphere_first.recommendation.feedback.entity.RecommendationFeedbackOutbox;
import com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType;
import com.collabsphere_first.recommendation.feedback.enums.FeedbackOutboxStatus;
import com.collabsphere_first.recommendation.feedback.enums.FeedbackSurface;
import com.collabsphere_first.recommendation.feedback.repository.RecommendationFeedbackOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationFeedbackOutboxService {

    private final RecommendationFeedbackOutboxRepository outboxRepository;
    private final RecommendationFeedbackProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Enqueue a recommendation feedback occurrence into the outbox inside the caller's transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<RecommendationFeedbackOutbox> recordFeedback(
            FeedbackEventType eventType,
            Long userId,
            ContentRef contentRef,
            Instant occurredAt,
            FeedbackSurface surface,
            String sessionId,
            Long durationMs
    ) {
        return recordFeedbackWithEventId(
                UUID.randomUUID(),
                eventType,
                userId,
                contentRef,
                occurredAt,
                surface,
                sessionId,
                durationMs
        );
    }

    /**
     * Enqueue a recommendation feedback occurrence with an explicit eventId (e.g. from an existing event entity).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<RecommendationFeedbackOutbox> recordFeedbackWithEventId(
            UUID eventId,
            FeedbackEventType eventType,
            Long userId,
            ContentRef contentRef,
            Instant occurredAt,
            FeedbackSurface surface,
            String sessionId,
            Long durationMs
    ) {
        if (!properties.isEnabled()) {
            log.debug("Recommendation feedback capture disabled; skipping event {}", eventType);
            return Optional.empty();
        }

        Instant eventTime = occurredAt != null ? occurredAt : Instant.now();
        UUID actualEventId = eventId != null ? eventId : UUID.randomUUID();

        RecommendationEventDto dto = RecommendationEventDto.builder()
                .contractVersion(RecommendationEventDto.CURRENT_CONTRACT_VERSION)
                .eventId(actualEventId)
                .eventType(eventType)
                .occurredAt(eventTime)
                .userId(userId)
                .contentRef(contentRef)
                .context(RecommendationEventDto.EventContext.of(surface, sessionId))
                .durationMs(durationMs)
                .build();

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize recommendation feedback event {}: {}", actualEventId, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to serialize feedback event payload", ex);
        }

        RecommendationFeedbackOutbox entity = RecommendationFeedbackOutbox.builder()
                .eventId(actualEventId)
                .contractVersion(RecommendationEventDto.CURRENT_CONTRACT_VERSION)
                .eventType(eventType)
                .userId(userId)
                .contentType(contentRef != null ? contentRef.type() : null)
                .contentId(contentRef != null ? contentRef.id() : null)
                .occurredAt(eventTime)
                .payload(payloadJson)
                .status(FeedbackOutboxStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(eventTime)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        RecommendationFeedbackOutbox saved = outboxRepository.save(entity);
        log.debug("Enqueued recommendation feedback outbox job (id={}, eventId={}, type={})",
                saved.getId(), saved.getEventId(), saved.getEventType());
        return Optional.of(saved);
    }

    /**
     * Claim a batch of claimable jobs using SELECT ... FOR UPDATE SKIP LOCKED in a short transaction.
     */
    @Transactional
    public List<RecommendationFeedbackOutbox> claimJobs(String nodeId, int limit) {
        Instant now = Instant.now();
        Instant staleLockCutoff = now.minusSeconds(properties.getLockLeaseSeconds());

        List<UUID> claimableIds = outboxRepository.findClaimableJobIds(now, staleLockCutoff, limit);
        if (claimableIds.isEmpty()) {
            return Collections.emptyList();
        }

        outboxRepository.claimJobs(claimableIds, nodeId, now);
        return outboxRepository.findByIdIn(claimableIds);
    }

    /**
     * Mark successfully delivered and/or per-event status items in a short transaction.
     * Enforces lockOwner matching to protect against stale worker races.
     */
    @Transactional
    public void handleBatchSuccess(
            List<UUID> jobIds,
            RecommendationFeedbackBatchResponse response,
            String lockOwner
    ) {
        Instant now = Instant.now();
        List<RecommendationFeedbackOutbox> jobs = outboxRepository.findByIdIn(jobIds);

        if (response == null || !"1".equals(response.contractVersion())) {
            String receivedVersion = response != null ? response.contractVersion() : "null";
            log.error("CONTRACT_VERSION_MISMATCH: expected contractVersion '1', received '{}'", receivedVersion);
            for (RecommendationFeedbackOutbox job : jobs) {
                if (Objects.equals(job.getLockOwner(), lockOwner)) {
                    job.markRetry(now.plusSeconds(300), "CONTRACT_VERSION_MISMATCH",
                            "Invalid contractVersion from recommendation service: " + receivedVersion, now);
                }
            }
            outboxRepository.saveAll(jobs);
            return;
        }

        Map<UUID, RecommendationFeedbackBatchResponse.EventResultItem> itemMap = Collections.emptyMap();
        if (response.items() != null) {
            itemMap = response.items().stream()
                    .filter(item -> item.eventId() != null)
                    .collect(Collectors.toMap(
                            RecommendationFeedbackBatchResponse.EventResultItem::eventId,
                            Function.identity(),
                            (first, second) -> first
                    ));
        }

        for (RecommendationFeedbackOutbox job : jobs) {
            if (!Objects.equals(job.getLockOwner(), lockOwner)) {
                log.warn("Stale lock owner on outbox job {}: expected '{}', found '{}'. Skipping update.",
                        job.getId(), lockOwner, job.getLockOwner());
                continue;
            }

            RecommendationFeedbackBatchResponse.EventResultItem itemResult = itemMap.get(job.getEventId());
            if (itemResult != null && itemResult.isRejected()) {
                job.markDeadLetter(
                        itemResult.errorCode() != null ? itemResult.errorCode() : "EVENT_REJECTED",
                        itemResult.errorMessage() != null ? itemResult.errorMessage() : "Rejected by Python ingestion",
                        now
                );
                log.warn("Recommendation feedback event {} rejected by ingestion: {} - {}",
                        job.getEventId(), itemResult.errorCode(), itemResult.errorMessage());
            } else if (itemResult != null && itemResult.isAcceptedOrDuplicate()) {
                job.markDelivered(now);
            } else {
                job.markRetry(now.plusSeconds(30), "MISSING_FROM_RESPONSE", "EventId omitted from Python response", now);
                log.warn("Recommendation feedback event {} missing from Python response items; scheduled retry", job.getEventId());
            }
        }

        outboxRepository.saveAll(jobs);
    }

    /**
     * Handle transient batch failure (network error, timeout, 5xx) with exponential backoff.
     */
    @Transactional
    public void handleBatchRetry(
            List<UUID> jobIds,
            String lockOwner,
            String errorCode,
            String errorMessage
    ) {
        Instant now = Instant.now();
        List<RecommendationFeedbackOutbox> jobs = outboxRepository.findByIdIn(jobIds);

        for (RecommendationFeedbackOutbox job : jobs) {
            if (!Objects.equals(job.getLockOwner(), lockOwner)) {
                log.warn("Stale lock owner on outbox job {} during retry handling. Skipping.", job.getId());
                continue;
            }

            if (job.getAttemptCount() >= properties.getMaxAttempts()) {
                job.markFailed("MAX_ATTEMPTS_EXCEEDED", errorMessage, now);
                log.error("Outbox job {} reached max attempts ({}); marked FAILED: {}",
                        job.getId(), properties.getMaxAttempts(), errorMessage);
            } else {
                long delaySeconds = (long) (10 * Math.pow(2, Math.max(0, job.getAttemptCount() - 1)));
                delaySeconds = Math.min(delaySeconds, 1800L); // Cap at 30 minutes
                Instant nextAttempt = now.plusSeconds(delaySeconds);
                job.markRetry(nextAttempt, errorCode, errorMessage, now);
                log.warn("Outbox job {} scheduled for retry in {}s (attempt {}/{}): {}",
                        job.getId(), delaySeconds, job.getAttemptCount(), properties.getMaxAttempts(), errorMessage);
            }
        }

        outboxRepository.saveAll(jobs);
    }

    /**
     * Handle permanent failure (401, 403, 422 contract mismatch) where retrying would be futile.
     */
    @Transactional
    public void handleBatchPermanentFailure(
            List<UUID> jobIds,
            String lockOwner,
            String errorCode,
            String errorMessage
    ) {
        Instant now = Instant.now();
        List<RecommendationFeedbackOutbox> jobs = outboxRepository.findByIdIn(jobIds);

        for (RecommendationFeedbackOutbox job : jobs) {
            if (!Objects.equals(job.getLockOwner(), lockOwner)) {
                continue;
            }
            job.markFailed(errorCode, errorMessage, now);
        }

        outboxRepository.saveAll(jobs);
        log.error("Marked {} outbox jobs permanently FAILED due to {}: {}", jobIds.size(), errorCode, errorMessage);
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        return outboxRepository.countByStatus(FeedbackOutboxStatus.PENDING)
                + outboxRepository.countByStatus(FeedbackOutboxStatus.RETRY);
    }

    @Transactional(readOnly = true)
    public long getFailedCount() {
        return outboxRepository.countByStatus(FeedbackOutboxStatus.FAILED)
                + outboxRepository.countByStatus(FeedbackOutboxStatus.DEAD_LETTER);
    }

    @Transactional(readOnly = true)
    public com.collabsphere_first.recommendation.feedback.dto.FeedbackOutboxMetrics getMetrics() {
        long pending = outboxRepository.countByStatus(FeedbackOutboxStatus.PENDING);
        long inProgress = outboxRepository.countByStatus(FeedbackOutboxStatus.IN_PROGRESS);
        long retry = outboxRepository.countByStatus(FeedbackOutboxStatus.RETRY);
        long delivered = outboxRepository.countByStatus(FeedbackOutboxStatus.DELIVERED);
        long deadLetter = outboxRepository.countByStatus(FeedbackOutboxStatus.DEAD_LETTER)
                + outboxRepository.countByStatus(FeedbackOutboxStatus.FAILED);
        Instant oldestPending = outboxRepository.findOldestPendingCreatedAt().orElse(null);
        return com.collabsphere_first.recommendation.feedback.dto.FeedbackOutboxMetrics.of(
                pending, inProgress, retry, delivered, deadLetter, oldestPending, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<com.collabsphere_first.recommendation.feedback.dto.SafeDeadLetterSummary> getDeadLetterSummaries(int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 500);
        return outboxRepository.findDeadLetterSummaries(org.springframework.data.domain.PageRequest.of(0, boundedLimit));
    }
}
