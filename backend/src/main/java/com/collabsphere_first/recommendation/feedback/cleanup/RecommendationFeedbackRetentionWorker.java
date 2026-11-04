package com.collabsphere_first.recommendation.feedback.cleanup;

import com.collabsphere_first.recommendation.feedback.config.RecommendationFeedbackProperties;
import com.collabsphere_first.recommendation.feedback.repository.RecommendationFeedbackOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationFeedbackRetentionWorker {

    private final RecommendationFeedbackOutboxRepository outboxRepository;
    private final RecommendationFeedbackProperties properties;

    @Scheduled(cron = "${collobster.recommendation.feedback.cleanup-cron:0 0 3 * * *}") // Daily at 3 AM
    @Transactional
    public void cleanupDeliveredOutbox() {
        int retentionDays = properties.getRetentionDays();
        if (retentionDays <= 0) {
            return;
        }

        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = outboxRepository.deleteCompletedOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Purged {} delivered/failed recommendation feedback outbox rows older than {} days (cutoff={})",
                    deleted, retentionDays, cutoff);
        }
    }
}
