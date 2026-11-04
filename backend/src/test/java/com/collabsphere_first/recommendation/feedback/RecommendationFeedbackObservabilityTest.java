package com.collabsphere_first.recommendation.feedback;

import com.collabsphere_first.recommendation.feedback.dto.FeedbackOutboxMetrics;
import com.collabsphere_first.recommendation.feedback.dto.SafeDeadLetterSummary;
import com.collabsphere_first.recommendation.feedback.enums.FeedbackOutboxStatus;
import com.collabsphere_first.recommendation.feedback.repository.RecommendationFeedbackOutboxRepository;
import com.collabsphere_first.recommendation.feedback.service.RecommendationFeedbackOutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationFeedbackObservabilityTest")
class RecommendationFeedbackObservabilityTest {

    @Mock
    private RecommendationFeedbackOutboxRepository outboxRepository;

    @InjectMocks
    private RecommendationFeedbackOutboxService outboxService;

    @Test
    @DisplayName("Metrics: Reports pending, retry, dead letter counts and oldest age without payload")
    void testMetricsCalculation() {
        when(outboxRepository.countByStatus(FeedbackOutboxStatus.PENDING)).thenReturn(10L);
        when(outboxRepository.countByStatus(FeedbackOutboxStatus.IN_PROGRESS)).thenReturn(2L);
        when(outboxRepository.countByStatus(FeedbackOutboxStatus.RETRY)).thenReturn(5L);
        when(outboxRepository.countByStatus(FeedbackOutboxStatus.DELIVERED)).thenReturn(100L);
        when(outboxRepository.countByStatus(FeedbackOutboxStatus.DEAD_LETTER)).thenReturn(3L);
        when(outboxRepository.countByStatus(FeedbackOutboxStatus.FAILED)).thenReturn(1L);

        Instant oldest = Instant.now().minus(120, ChronoUnit.SECONDS);
        when(outboxRepository.findOldestPendingCreatedAt()).thenReturn(Optional.of(oldest));

        FeedbackOutboxMetrics metrics = outboxService.getMetrics();

        assertEquals(10L, metrics.pendingCount());
        assertEquals(2L, metrics.inProgressCount());
        assertEquals(5L, metrics.retryCount());
        assertEquals(100L, metrics.deliveredCount());
        assertEquals(4L, metrics.deadLetterCount()); // 3 DEAD_LETTER + 1 FAILED
        assertEquals(oldest, metrics.oldestPendingCreatedAt());
        assertNotNull(metrics.oldestPendingAgeSeconds());
        assertTrue(metrics.oldestPendingAgeSeconds() >= 119 && metrics.oldestPendingAgeSeconds() <= 125);
    }

    @Test
    @DisplayName("Dead-letter summary: Safely exposes metadata without private user payload")
    void testDeadLetterSummarySafeExposure() {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now().minus(10, ChronoUnit.MINUTES);
        SafeDeadLetterSummary summary = new SafeDeadLetterSummary(
                eventId,
                "LIKE",
                "INVALID_EVENT_TYPE",
                3,
                occurredAt
        );

        when(outboxRepository.findDeadLetterSummaries(any(PageRequest.class)))
                .thenReturn(List.of(summary));

        List<SafeDeadLetterSummary> results = outboxService.getDeadLetterSummaries(50);

        assertNotNull(results);
        assertEquals(1, results.size());
        SafeDeadLetterSummary item = results.get(0);
        assertEquals(eventId, item.eventId());
        assertEquals("LIKE", item.eventType());
        assertEquals("INVALID_EVENT_TYPE", item.sanitizedErrorCode());
        assertEquals(3, item.attemptCount());
        assertEquals(occurredAt, item.occurredAt());

        // Verify that SafeDeadLetterSummary class does NOT have any payload field
        for (var field : SafeDeadLetterSummary.class.getDeclaredFields()) {
            assertNotEquals("payload", field.getName().toLowerCase(), "Dead-letter summary must not expose payload");
            assertNotEquals("context", field.getName().toLowerCase(), "Dead-letter summary must not expose context");
        }
    }
}
