package com.collabsphere_first.recommendation;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.recommendation.client.HttpRecommendationClient;
import com.collabsphere_first.recommendation.client.RecommendationCircuitBreaker;
import com.collabsphere_first.recommendation.config.RecommendationProperties;
import com.collabsphere_first.recommendation.dto.*;
import com.collabsphere_first.recommendation.exception.RecommendationBulkheadFullException;
import com.collabsphere_first.recommendation.exception.RecommendationCircuitBreakerOpenException;
import com.collabsphere_first.recommendation.exception.RecommendationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HttpRecommendationClientTest {

    private RecommendationProperties properties;
    private RecommendationCircuitBreaker circuitBreaker;
    private RestClient restClient;
    private MockRestServiceServer mockServer;
    private HttpRecommendationClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        properties = new RecommendationProperties();
        properties.setBaseUrl("http://internal-recommendation:8000");
        properties.setInternalToken("test-secret-token");
        properties.getCircuitBreaker().setEnabled(true);
        properties.getCircuitBreaker().setMinimumNumberOfCalls(3);
        properties.getCircuitBreaker().setSlidingWindowSize(5);
        properties.getCircuitBreaker().setFailureRateThreshold(50.0f);
        properties.getCircuitBreaker().setWaitDurationInOpenStateMs(10000);

        circuitBreaker = new RecommendationCircuitBreaker(properties);

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();

        client = new HttpRecommendationClient(restClient, properties, circuitBreaker);
    }

    private RecommendationRankRequest sampleRequest(UUID reqId) {
        return new RecommendationRankRequest(
                reqId,
                new RecommendationUserContext(100L),
                List.of(new RecommendationCandidatePayload(
                        ContentRef.platformPost(UUID.randomUUID()),
                        42L,
                        Instant.now(),
                        List.of("FOLLOWING"),
                        new RecommendationFeatures(0.9, null, 0.5, null, null, 1.0, 0.0, null, 10.0, null)
                ))
        );
    }

    @Test
    @DisplayName("Successful recommendation HTTP ranking call with authorization header")
    void testSuccessfulRankingCall() throws Exception {
        UUID reqId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        RecommendationRankResponse mockResponse = new RecommendationRankResponse(
                "1",
                reqId,
                new RecommendationModelInfo("lightgbm-feed-ranker", "v2", "lightgbm_lambdarank"),
                List.of(new RankedItemResponse(ContentRef.platformPost(candidateId), 1, 0.95))
        );

        mockServer.expect(requestTo("http://internal-recommendation:8000/api/v1/internal/recommend/feed/rank"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-secret-token"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        RecommendationRankResponse response = client.rank(sampleRequest(reqId));

        assertThat(response).isNotNull();
        assertThat(response.contractVersion()).isEqualTo("1");
        assertThat(response.featureContractVersion()).isEqualTo("1");
        assertThat(response.requestId()).isEqualTo(reqId);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).contentRef().id()).isEqualTo(candidateId);


        mockServer.verify();
        assertThat(circuitBreaker.isClosed()).isTrue();
    }

    @Test
    @DisplayName("HTTP 500 error triggers RecommendationException")
    void testHttp500ServerError() {
        UUID reqId = UUID.randomUUID();

        mockServer.expect(requestTo("http://internal-recommendation:8000/api/v1/internal/recommend/feed/rank"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.rank(sampleRequest(reqId)))
                .isInstanceOf(RecommendationException.class)
                .hasMessageContaining("Recommendation ranking failed");

        mockServer.verify();
    }

    @Test
    @DisplayName("HTTP 401 Unauthorized triggers RecommendationException")
    void testHttp401Unauthorized() {
        UUID reqId = UUID.randomUUID();

        mockServer.expect(requestTo("http://internal-recommendation:8000/api/v1/internal/recommend/feed/rank"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"detail\":\"Invalid token\"}"));

        assertThatThrownBy(() -> client.rank(sampleRequest(reqId)))
                .isInstanceOf(RecommendationException.class);

        mockServer.verify();
    }

    @Test
    @DisplayName("Circuit breaker opens after repeated failures and short-circuits subsequent calls")
    void testCircuitBreakerOpenTransition() {
        // Declare expectations upfront: 3 server error responses
        mockServer.expect(org.springframework.test.web.client.ExpectedCount.times(3),
                        requestTo("http://internal-recommendation:8000/api/v1/internal/recommend/feed/rank"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        // Execute 3 failed calls
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> client.rank(sampleRequest(UUID.randomUUID())))
                    .isInstanceOf(RecommendationException.class);
        }

        // Circuit breaker should now be OPEN
        assertThat(circuitBreaker.isOpen()).isTrue();

        // 4th call should immediately fail with RecommendationCircuitBreakerOpenException
        // without hitting mockServer (no expectation set)
        assertThatThrownBy(() -> client.rank(sampleRequest(UUID.randomUUID())))
                .isInstanceOf(RecommendationCircuitBreakerOpenException.class)
                .hasMessageContaining("circuit breaker is OPEN");
    }

    @Test
    @DisplayName("401 Unauthorized causes Java fallback exception but does NOT trip circuit breaker")
    void testHttp401UnauthorizedDoesNotTripCircuitBreaker() {
        mockServer.expect(org.springframework.test.web.client.ExpectedCount.times(3),
                        requestTo("http://internal-recommendation:8000/api/v1/internal/recommend/feed/rank"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"detail\":\"Internal token mismatch\"}"));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> client.rank(sampleRequest(UUID.randomUUID())))
                    .isInstanceOf(RecommendationException.class)
                    .hasMessageContaining("401 Unauthorized");
        }

        mockServer.verify();
        // Circuit breaker MUST remain CLOSED despite repeated 401s
        assertThat(circuitBreaker.isClosed()).isTrue();
        assertThat(circuitBreaker.isOpen()).isFalse();
    }

    @Test
    @DisplayName("422 Unprocessable Entity causes Java fallback exception but does NOT trip circuit breaker")
    void testHttp422UnprocessableEntityDoesNotTripCircuitBreaker() {
        mockServer.expect(org.springframework.test.web.client.ExpectedCount.times(3),
                        requestTo("http://internal-recommendation:8000/api/v1/internal/recommend/feed/rank"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"detail\":\"Schema validation error\"}"));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> client.rank(sampleRequest(UUID.randomUUID())))
                    .isInstanceOf(RecommendationException.class)
                    .hasMessageContaining("422 Unprocessable Entity");
        }

        mockServer.verify();
        // Circuit breaker MUST remain CLOSED despite repeated 422s
        assertThat(circuitBreaker.isClosed()).isTrue();
        assertThat(circuitBreaker.isOpen()).isFalse();
    }

    @Test
    @DisplayName("When circuit is OPEN, calls fail-fast immediately without sending HTTP request")
    void testCircuitBreakerOpenFastFailsWithoutCallingServer() {
        circuitBreaker.transitionToOpenState();
        assertThat(circuitBreaker.isOpen()).isTrue();

        // No mockServer expectations defined: if HTTP call is attempted, test fails
        assertThatThrownBy(() -> client.rank(sampleRequest(UUID.randomUUID())))
                .isInstanceOf(RecommendationCircuitBreakerOpenException.class)
                .hasMessageContaining("circuit breaker is OPEN");

        mockServer.verify();
    }

    @Test
    @DisplayName("Section 9: Application-scoped client and transport factory are reused across requests")
    void testReusableClientTransportArchitecture() {
        // Verify same RestClient reference is maintained by the client instance across calls
        RestClient initialRestClient = client.getRestClient();
        assertThat(initialRestClient).isNotNull();

        // Instantiate production client with custom pool and timeout configuration
        RecommendationProperties prodProps = new RecommendationProperties();
        prodProps.setBaseUrl("http://localhost:8000");
        prodProps.setConnectTimeoutMs(750);
        prodProps.setReadTimeoutMs(1500);
        prodProps.setMaxConcurrentRequests(25);

        RecommendationCircuitBreaker cb = new RecommendationCircuitBreaker(prodProps);
        HttpRecommendationClient prodClient = new HttpRecommendationClient(prodProps, cb);

        assertThat(prodClient.getRestClient()).isNotNull();
        assertThat(prodClient.getProperties().getConnectTimeoutMs()).isEqualTo(750);
        assertThat(prodClient.getProperties().getReadTimeoutMs()).isEqualTo(1500);
        assertThat(prodClient.getProperties().getMaxConcurrentRequests()).isEqualTo(25);

        // Multiple invocations on the same bean reuse the exact same client transport reference
        assertThat(prodClient.getRestClient()).isSameAs(prodClient.getRestClient());
    }

    @Test
    @DisplayName("Bulkhead bounds concurrency to configured limit and rejects saturated N+1 request with RecommendationBulkheadFullException")
    void testBulkheadBoundsConcurrentCallsAndRejectsSaturatedWithBulkheadFullException() throws Exception {
        RecommendationProperties bulkheadProps = new RecommendationProperties();
        bulkheadProps.setBaseUrl("http://internal-recommendation:8000");
        bulkheadProps.setInternalToken("test-token");
        bulkheadProps.setMaxConcurrentRequests(2);
        bulkheadProps.setBulkheadAcquireTimeoutMs(100L);

        RecommendationCircuitBreaker cb = new RecommendationCircuitBreaker(bulkheadProps);

        CountDownLatch enteredLatch = new CountDownLatch(2);
        CountDownLatch holdLatch = new CountDownLatch(1);

        RestClient customRestClient = RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    enteredLatch.countDown();
                    try {
                        holdLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return execution.execute(request, body);
                })
                .build();

        HttpRecommendationClient bulkheadClient = new HttpRecommendationClient(customRestClient, bulkheadProps, cb);
        assertThat(bulkheadClient.getAvailableBulkheadPermits()).isEqualTo(2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Launch 2 concurrent calls that will block inside the interceptor
            executor.submit(() -> bulkheadClient.rank(sampleRequest(UUID.randomUUID())));
            executor.submit(() -> bulkheadClient.rank(sampleRequest(UUID.randomUUID())));

            // Wait until both calls have acquired permits and entered transport
            boolean bothEntered = enteredLatch.await(5, TimeUnit.SECONDS);
            assertThat(bothEntered).isTrue();

            // Verify permits are exhausted
            assertThat(bulkheadClient.getAvailableBulkheadPermits()).isEqualTo(0);

            // N+1 call should fail-fast with RecommendationBulkheadFullException within 100ms
            long acquireStart = System.currentTimeMillis();
            assertThatThrownBy(() -> bulkheadClient.rank(sampleRequest(UUID.randomUUID())))
                    .isInstanceOf(RecommendationBulkheadFullException.class)
                    .hasMessageContaining("bulkhead capacity saturated (2)");
            long elapsed = System.currentTimeMillis() - acquireStart;
            assertThat(elapsed).isGreaterThanOrEqualTo(80L);

            // Unblock the 2 running calls
            holdLatch.countDown();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Bulkhead releases permit on success and maintains zero permit leak")
    void testBulkheadPermitsReleasedOnSuccess() throws Exception {
        UUID reqId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        RecommendationRankResponse mockResponse = new RecommendationRankResponse(
                "1",
                reqId,
                new RecommendationModelInfo("lightgbm-feed-ranker", "v2", "lightgbm_lambdarank"),
                List.of(new RankedItemResponse(ContentRef.platformPost(candidateId), 1, 0.95))
        );

        mockServer.expect(requestTo("http://internal-recommendation:8000/api/v1/internal/recommend/feed/rank"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        int initialPermits = client.getAvailableBulkheadPermits();
        RecommendationRankResponse resp = client.rank(sampleRequest(reqId));

        assertThat(resp).isNotNull();
        assertThat(client.getAvailableBulkheadPermits()).isEqualTo(initialPermits);
    }

    @Test
    @DisplayName("Bulkhead releases permit on HTTP 500 error exception and leaves permits intact")
    void testBulkheadPermitsReleasedOnException() {
        mockServer.expect(requestTo("http://internal-recommendation:8000/api/v1/internal/recommend/feed/rank"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        int initialPermits = client.getAvailableBulkheadPermits();

        assertThatThrownBy(() -> client.rank(sampleRequest(UUID.randomUUID())))
                .isInstanceOf(RecommendationException.class);

        // Permit must be released even after server error
        assertThat(client.getAvailableBulkheadPermits()).isEqualTo(initialPermits);
    }

    @Test
    @DisplayName("No permit leak occurs after repeated consecutive failures")
    void testNoPermitLeakAfterRepeatedFailures() {
        RecommendationProperties props = new RecommendationProperties();
        props.setBaseUrl("http://internal-recommendation:8000");
        props.setMaxConcurrentRequests(1);
        props.setBulkheadAcquireTimeoutMs(100L);

        RecommendationCircuitBreaker cb = new RecommendationCircuitBreaker(props);

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer testServer = MockRestServiceServer.bindTo(builder).build();
        HttpRecommendationClient singlePermitClient = new HttpRecommendationClient(builder.build(), props, cb);

        assertThat(singlePermitClient.getAvailableBulkheadPermits()).isEqualTo(1);

        // Expect 5 consecutive 500 errors
        testServer.expect(org.springframework.test.web.client.ExpectedCount.times(5),
                        requestTo("http://internal-recommendation:8000/api/v1/internal/recommend/feed/rank"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> singlePermitClient.rank(sampleRequest(UUID.randomUUID())))
                    .isInstanceOf(RecommendationException.class);
            // Verify permit is released every single time
            assertThat(singlePermitClient.getAvailableBulkheadPermits()).isEqualTo(1);
        }

        assertThat(singlePermitClient.getAvailableBulkheadPermits()).isEqualTo(1);
    }
}
