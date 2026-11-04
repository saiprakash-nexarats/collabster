package com.collabsphere_first.recommendation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "collobster.recommendation")
public class RecommendationProperties {

    /**
     * Whether Python recommendation ranking is enabled.
     * When false, Feed uses BaselineFeedRankingStrategy directly without HTTP calls.
     */
    private boolean enabled = false;

    /**
     * Base URL of the internal Python recommendation service (e.g. http://localhost:8000).
     */
    private String baseUrl = "http://localhost:8000";

    /**
     * Internal service-to-service authentication Bearer token.
     */
    private String internalToken = "";

    /**
     * Connection timeout in milliseconds for HTTP client.
     */
    private int connectTimeoutMs = 500;

    /**
     * Read/response timeout in milliseconds for HTTP client.
     */
    private int readTimeoutMs = 1000;

    /**
     * Maximum number of candidates transmitted in a single rank request.
     */
    private int maxCandidates = 500;

    /**
     * Maximum concurrent outbound ranking requests permitted through the client bulkhead.
     * Enforced via java.util.concurrent.Semaphore to bound caller concurrency.
     */
    private int maxConcurrentRequests = 20;

    /**
     * Maximum time in milliseconds to wait for a ranking bulkhead permit before shedding load.
     * Defaults to 250ms to ensure fast fallback to BaselineFeedRankingStrategy.
     */
    private long bulkheadAcquireTimeoutMs = 250L;

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
     * Whether fallback to BaselineFeedRankingStrategy is enabled upon failure/timeout.
     */
    private boolean fallbackEnabled = true;

    /**
     * Circuit breaker configuration.
     */
    private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();

    @Getter
    @Setter
    public static class CircuitBreakerProperties {
        private boolean enabled = true;
        private float failureRateThreshold = 50.0f;
        private int slidingWindowSize = 5;
        private int minimumNumberOfCalls = 3;
        private long waitDurationInOpenStateMs = 10000;
        private int permittedNumberOfCallsInHalfOpenState = 2;
    }
}
