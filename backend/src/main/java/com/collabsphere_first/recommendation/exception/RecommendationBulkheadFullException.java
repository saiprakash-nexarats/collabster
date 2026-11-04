package com.collabsphere_first.recommendation.exception;

/**
 * Thrown when outbound recommendation calls exceed the configured concurrent request bulkhead.
 * Triggers immediate graceful fallback to BaselineFeedRankingStrategy without tripping the circuit breaker.
 */
public class RecommendationBulkheadFullException extends RecommendationException {

    public RecommendationBulkheadFullException(String message) {
        super(message);
    }

    public RecommendationBulkheadFullException(String message, Throwable cause) {
        super(message, cause);
    }
}
