package com.collabsphere_first.account.erasure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "collobster.recommendation.privacy")
public class RecommendationPrivacyProperties {

    /**
     * Base URL of the internal Python recommendation service (e.g. http://localhost:8000).
     */
    private String baseUrl = "http://localhost:8000";

    /**
     * Internal service-to-service Bearer authentication token.
     */
    private String internalToken = "";

    /**
     * Maximum concurrent outbound privacy erasure requests permitted through the privacy bulkhead.
     */
    private int maxConcurrentRequests = 5;

    /**
     * Maximum time in milliseconds to wait for a privacy bulkhead permit before shedding load.
     */
    private long bulkheadAcquireTimeoutMs = 1000L;

    /**
     * Connection timeout in milliseconds for privacy erasure HTTP calls.
     */
    private int connectTimeoutMs = 3000;

    /**
     * Read timeout in milliseconds for privacy erasure HTTP calls.
     */
    private int readTimeoutMs = 5000;

    /**
     * Backwards-compatible alias for maxConcurrentRequests.
     */
    public int getMaxConnections() {
        return maxConcurrentRequests;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConcurrentRequests = maxConnections;
    }
}
