package com.collabsphere_first.recommendation;

import com.collabsphere_first.recommendation.client.RecommendationCircuitBreaker;
import com.collabsphere_first.recommendation.config.RecommendationProperties;
import com.collabsphere_first.recommendation.exception.RecommendationCircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 5.2 Deterministic Resilience4j Circuit Breaker Test Suite.
 * Validates CLOSED, FAILURE ACCUMULATION, OPEN, HALF_OPEN, RECOVERY, RE-FAILURE,
 * and 401/422 failure classification rules.
 */
class RecommendationCircuitBreakerTest {

    private RecommendationProperties properties;
    private RecommendationCircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        properties = new RecommendationProperties();
        properties.getCircuitBreaker().setEnabled(true);
        properties.getCircuitBreaker().setSlidingWindowSize(5);
        properties.getCircuitBreaker().setMinimumNumberOfCalls(3);
        properties.getCircuitBreaker().setFailureRateThreshold(50.0f);
        properties.getCircuitBreaker().setWaitDurationInOpenStateMs(10000);
        properties.getCircuitBreaker().setPermittedNumberOfCallsInHalfOpenState(2);

        circuitBreaker = new RecommendationCircuitBreaker(properties);
    }

    @Test
    @DisplayName("CLOSED: Healthy operations succeed and circuit remains CLOSED")
    void testClosedStateHealthyCalls() {
        assertThat(circuitBreaker.isClosed()).isTrue();
        assertThat(circuitBreaker.isOpen()).isFalse();
        assertThat(circuitBreaker.getState()).isEqualTo("CLOSED");

        for (int i = 0; i < 10; i++) {
            String result = circuitBreaker.execute(() -> "healthy-response-" + System.currentTimeMillis());
            assertThat(result).startsWith("healthy-response-");
        }

        assertThat(circuitBreaker.isClosed()).isTrue();
        assertThat(circuitBreaker.isOpen()).isFalse();
    }

    @Test
    @DisplayName("FAILURE ACCUMULATION: Server 5xx failures reach threshold and transition circuit to OPEN")
    void testFailureAccumulationTransitionsToOpen() {
        assertThat(circuitBreaker.isClosed()).isTrue();

        // 3 consecutive 500 server errors (exceeds min calls 3 and 50% threshold)
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> circuitBreaker.execute(() -> {
                throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Python ranker crashed");
            })).isInstanceOf(HttpServerErrorException.class);
        }

        // Circuit must transition to OPEN
        assertThat(circuitBreaker.isOpen()).isTrue();
        assertThat(circuitBreaker.isClosed()).isFalse();
        assertThat(circuitBreaker.getState()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("FAILURE ACCUMULATION: Network timeout/refused errors reach threshold and transition circuit to OPEN")
    void testResourceAccessExceptionTransitionsToOpen() {
        assertThat(circuitBreaker.isClosed()).isTrue();

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> circuitBreaker.execute(() -> {
                throw new ResourceAccessException("Connection refused: connect to 127.0.0.1:8000 failed");
            })).isInstanceOf(ResourceAccessException.class);
        }

        assertThat(circuitBreaker.isOpen()).isTrue();
    }

    @Test
    @DisplayName("OPEN: When OPEN, underlying operation is NOT called and fast-fails immediately")
    void testOpenStateFastFailsWithoutCallingOperation() {
        // Transition to OPEN
        circuitBreaker.transitionToOpenState();
        assertThat(circuitBreaker.isOpen()).isTrue();

        AtomicBoolean operationExecuted = new AtomicBoolean(false);

        assertThatThrownBy(() -> circuitBreaker.execute(() -> {
            operationExecuted.set(true);
            return "should-never-be-returned";
        })).isInstanceOf(RecommendationCircuitBreakerOpenException.class)
                .hasMessageContaining("circuit breaker is OPEN");

        // Verify underlying operation was NEVER invoked
        assertThat(operationExecuted.get()).isFalse();
    }

    @Test
    @DisplayName("HALF_OPEN: Transitions to HALF_OPEN and permits configured probe calls")
    void testHalfOpenStatePermittedProbes() {
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();

        assertThat(circuitBreaker.isHalfOpen()).isTrue();
        assertThat(circuitBreaker.getState()).isEqualTo("HALF_OPEN");

        AtomicInteger probeCounter = new AtomicInteger(0);

        // First probe call permitted
        String probe1 = circuitBreaker.execute(() -> {
            probeCounter.incrementAndGet();
            return "probe-1-ok";
        });
        assertThat(probe1).isEqualTo("probe-1-ok");
        assertThat(probeCounter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("RECOVERY: Successful probes in HALF_OPEN return circuit to CLOSED")
    void testRecoveryReturnsToClosed() {
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.isHalfOpen()).isTrue();

        // Execute 2 successful probes (permittedNumberOfCallsInHalfOpenState = 2)
        circuitBreaker.execute(() -> "probe-1-ok");
        circuitBreaker.execute(() -> "probe-2-ok");

        // Circuit must transition back to CLOSED
        assertThat(circuitBreaker.isClosed()).isTrue();
        assertThat(circuitBreaker.isOpen()).isFalse();
        assertThat(circuitBreaker.isHalfOpen()).isFalse();
        assertThat(circuitBreaker.getState()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("RE-FAILURE: A failed probe in HALF_OPEN immediately returns circuit to OPEN")
    void testReFailureReturnsToOpen() {
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.isHalfOpen()).isTrue();

        // In HALF_OPEN, permittedNumberOfCallsInHalfOpenState = 2
        // When probes fail, the failure rate reaches threshold (>= 50%) and transitions back to OPEN
        assertThatThrownBy(() -> circuitBreaker.execute(() -> {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Probe 1 failed");
        })).isInstanceOf(HttpServerErrorException.class);

        assertThatThrownBy(() -> circuitBreaker.execute(() -> {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Probe 2 failed");
        })).isInstanceOf(HttpServerErrorException.class);

        // Circuit must immediately re-transition to OPEN
        assertThat(circuitBreaker.isOpen()).isTrue();
        assertThat(circuitBreaker.isHalfOpen()).isFalse();
        assertThat(circuitBreaker.getState()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("FAILURE CLASSIFICATION: HTTP 401 Unauthorized is ignored and does NOT trip the circuit breaker")
    void test401UnauthorizedIgnoredByCircuitBreaker() {
        assertThat(circuitBreaker.isClosed()).isTrue();

        // 5 consecutive 401 Unauthorized errors
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> circuitBreaker.execute(() -> {
                throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Invalid internal bearer token");
            })).isInstanceOf(HttpClientErrorException.class);
        }

        // Circuit breaker MUST remain CLOSED because 401 is an integration/auth issue, not service outage
        assertThat(circuitBreaker.isClosed()).isTrue();
        assertThat(circuitBreaker.isOpen()).isFalse();
    }

    @Test
    @DisplayName("FAILURE CLASSIFICATION: HTTP 422 Unprocessable Entity is ignored and does NOT trip the circuit breaker")
    void test422UnprocessableEntityIgnoredByCircuitBreaker() {
        assertThat(circuitBreaker.isClosed()).isTrue();

        // 5 consecutive 422 Unprocessable Entity contract errors
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> circuitBreaker.execute(() -> {
                throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Contract schema mismatch");
            })).isInstanceOf(HttpClientErrorException.class);
        }

        // Circuit breaker MUST remain CLOSED
        assertThat(circuitBreaker.isClosed()).isTrue();
        assertThat(circuitBreaker.isOpen()).isFalse();
    }

    @Test
    @DisplayName("AUTOMATIC TRANSITION: OPEN transitions to HALF_OPEN after configured waitDuration")
    void testAutomaticTransitionFromOpenToHalfOpen() throws InterruptedException {
        // Set short wait duration for deterministic test without long sleep
        properties.getCircuitBreaker().setWaitDurationInOpenStateMs(50);
        RecommendationCircuitBreaker fastCircuitBreaker = new RecommendationCircuitBreaker(properties);

        fastCircuitBreaker.transitionToOpenState();
        assertThat(fastCircuitBreaker.isOpen()).isTrue();

        // Wait slightly longer than 50ms
        Thread.sleep(80);

        // Under Resilience4j with automaticTransitionFromOpenToHalfOpenEnabled,
        // the state transitions to HALF_OPEN when checked/probed
        assertThat(fastCircuitBreaker.isCallPermitted()).isTrue();
        assertThat(fastCircuitBreaker.isHalfOpen()).isTrue();
    }

    @Test
    @DisplayName("DISABLED MODE: When circuit breaker is disabled, calls execute unconditionally")
    void testDisabledCircuitBreaker() {
        properties.getCircuitBreaker().setEnabled(false);
        RecommendationCircuitBreaker disabledCb = new RecommendationCircuitBreaker(properties);

        // Execute operations
        String res = disabledCb.execute(() -> "always-executed");
        assertThat(res).isEqualTo("always-executed");
        assertThat(disabledCb.isCallPermitted()).isTrue();
    }
}
