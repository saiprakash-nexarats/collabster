package com.collabsphere_first.recommendation.exception;

/**
 * Thrown when recommendation calls are short-circuited because the circuit breaker is OPEN.
 */
public class RecommendationCircuitBreakerOpenException extends RecommendationException {

    public RecommendationCircuitBreakerOpenException(String message) {
        super(message);
    }

    public RecommendationCircuitBreakerOpenException(String message, Throwable cause) {
        super(message, cause);
    }
}
