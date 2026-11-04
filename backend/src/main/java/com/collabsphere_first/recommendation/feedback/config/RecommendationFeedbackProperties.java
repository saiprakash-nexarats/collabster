package com.collabsphere_first.recommendation.feedback.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "collobster.recommendation.feedback")
public class RecommendationFeedbackProperties {

    /**
     * Master switch for capturing recommendation feedback into the outbox.
     * When true, authoritative interactions write immutable events to recommendation_feedback_outbox.
     */
    private boolean enabled = true;

    /**
     * Master switch for background worker delivery to Python service.
     * Allows capturing events in the outbox even if delivery is temporarily suspended.
     */
    private boolean deliveryEnabled = true;

    /**
     * Base URL of the internal Python recommendation service (e.g. http://localhost:8000).
     */
    private String baseUrl = "http://localhost:8000";

    /**
     * Internal service-to-service Bearer authentication token.
     */
    private String internalToken = "";

    /**
     * Maximum number of events transmitted in a single HTTP batch.
     */
    private int batchSize = 100;

    /**
     * Maximum concurrent outbound feedback batch requests permitted through the feedback bulkhead.
     */
    private int maxConcurrentRequests = 10;

    /**
     * Maximum time in milliseconds to wait for a feedback bulkhead permit.
     */
    private long bulkheadAcquireTimeoutMs = 500L;

    /**
     * Backwards-compatible legacy alias for maxConcurrentRequests.
     * @deprecated since Phase 11.4 in favor of canonical {@link #maxConcurrentRequests}.
     */
    @Deprecated
    public int getMaxConnections() {
        return maxConcurrentRequests;
    }

    /**
     * Backwards-compatible legacy alias for maxConcurrentRequests.
     * @deprecated since Phase 11.4 in favor of canonical {@link #maxConcurrentRequests}.
     */
    @Deprecated
    public void setMaxConnections(int maxConnections) {
        this.maxConcurrentRequests = maxConnections;
    }

    /**
     * Polling interval in milliseconds for the background delivery worker.
     */
    private long workerIntervalMs = 2000L;

    /**
     * Connection timeout in milliseconds for HTTP calls to Python.
     */
    private int connectTimeoutMs = 500;

    /**
     * Read timeout in milliseconds for HTTP calls to Python.
     */
    private int readTimeoutMs = 1500;

    /**
     * Maximum retry attempts before marking an outbox job FAILED.
     */
    private int maxAttempts = 5;

    /**
     * Duration in seconds before a job locked by an in-progress worker is considered stale and reclaimable.
     */
    private long lockLeaseSeconds = 30L;

    /**
     * Retention period in days for DELIVERED and FAILED outbox events before cleanup.
     */
    private int retentionDays = 14;
}
