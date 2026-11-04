package com.collabsphere_first.recommendation.client;

import com.collabsphere_first.recommendation.config.RecommendationProperties;
import com.collabsphere_first.recommendation.exception.RecommendationCircuitBreakerOpenException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Production Circuit Breaker adapter wrapping Resilience4j.
 * Fast-fails remote calls to Python ranking when service error thresholds are exceeded,
 * protecting Feed latency and enabling Java baseline fallback.
 */
@Slf4j
@Getter
@Component
public class RecommendationCircuitBreaker {

    private final CircuitBreaker circuitBreaker;
    private final boolean enabled;

    public RecommendationCircuitBreaker(RecommendationProperties properties) {
        RecommendationProperties.CircuitBreakerProperties cbProps = properties.getCircuitBreaker();
        this.enabled = cbProps.isEnabled();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbProps.getFailureRateThreshold())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(cbProps.getSlidingWindowSize())
                .minimumNumberOfCalls(cbProps.getMinimumNumberOfCalls())
                .waitDurationInOpenState(Duration.ofMillis(cbProps.getWaitDurationInOpenStateMs()))
                .permittedNumberOfCallsInHalfOpenState(cbProps.getPermittedNumberOfCallsInHalfOpenState())
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .ignoreExceptions(HttpClientErrorException.class)
                .ignoreException(throwable -> {
                    Throwable current = throwable;
                    while (current != null) {
                        if (current instanceof HttpClientErrorException) {
                            return true;
                        }
                        current = current.getCause();
                    }
                    return false;
                })
                .build();

        this.circuitBreaker = CircuitBreaker.of("recommendationService", config);

        this.circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("Recommendation Circuit Breaker transitioned: {}", event.getStateTransition()));
    }

    /**
     * Executes the given operation within the circuit breaker context.
     * Throws RecommendationCircuitBreakerOpenException if the circuit is OPEN.
     */
    public <T> T execute(Supplier<T> operation) {
        if (!enabled) {
            return operation.get();
        }
        try {
            return circuitBreaker.executeSupplier(operation);
        } catch (CallNotPermittedException ex) {
            throw new RecommendationCircuitBreakerOpenException("Recommendation service circuit breaker is OPEN", ex);
        }
    }

    public boolean isCallPermitted() {
        return !enabled || circuitBreaker.tryAcquirePermission();
    }

    public void onSuccess(long durationMs) {
        if (enabled) {
            circuitBreaker.onSuccess(durationMs, TimeUnit.MILLISECONDS);
        }
    }

    public void onError(long durationMs, Throwable throwable) {
        if (enabled) {
            circuitBreaker.onError(durationMs, TimeUnit.MILLISECONDS, throwable);
        }
    }

    public String getState() {
        return circuitBreaker.getState().name();
    }

    public boolean isOpen() {
        return circuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    public boolean isClosed() {
        return circuitBreaker.getState() == CircuitBreaker.State.CLOSED;
    }

    public boolean isHalfOpen() {
        return circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN;
    }

    public void transitionToOpenState() {
        circuitBreaker.transitionToOpenState();
    }

    public void transitionToClosedState() {
        circuitBreaker.transitionToClosedState();
    }

    public void transitionToHalfOpenState() {
        circuitBreaker.transitionToHalfOpenState();
    }

    public CircuitBreaker.Metrics getMetrics() {
        return circuitBreaker.getMetrics();
    }

    public void reset() {
        circuitBreaker.reset();
    }
}
