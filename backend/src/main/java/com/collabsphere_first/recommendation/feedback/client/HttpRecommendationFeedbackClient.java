package com.collabsphere_first.recommendation.feedback.client;

import com.collabsphere_first.recommendation.exception.RecommendationException;
import com.collabsphere_first.recommendation.feedback.config.RecommendationFeedbackProperties;
import com.collabsphere_first.recommendation.feedback.dto.RecommendationFeedbackBatchRequest;
import com.collabsphere_first.recommendation.feedback.dto.RecommendationFeedbackBatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Slf4j
@Component
public class HttpRecommendationFeedbackClient implements RecommendationFeedbackClient {

    private final RestClient restClient;
    private final RecommendationFeedbackProperties properties;
    private final URI feedbackEndpointUri;
    private final java.util.concurrent.Semaphore feedbackBulkhead;
    private final long bulkheadAcquireTimeoutMs;

    @org.springframework.beans.factory.annotation.Autowired
    public HttpRecommendationFeedbackClient(RecommendationFeedbackProperties properties) {
        this.properties = properties;
        this.feedbackBulkhead = new java.util.concurrent.Semaphore(
                Math.max(1, properties.getMaxConcurrentRequests()), true);
        this.bulkheadAcquireTimeoutMs = properties.getBulkheadAcquireTimeoutMs();

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(java.time.Duration.ofMillis(properties.getConnectTimeoutMs()))
                .executor(java.util.concurrent.Executors.newFixedThreadPool(
                        Math.max(1, properties.getMaxConcurrentRequests()),
                        r -> {
                            Thread t = new Thread(r, "rec-fb-pool-" + r.hashCode());
                            t.setDaemon(true);
                            return t;
                        }
                ))
                .build();

        org.springframework.http.client.JdkClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(java.time.Duration.ofMillis(properties.getReadTimeoutMs()));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        String base = properties.getBaseUrl().replaceAll("/+$", "");
        this.feedbackEndpointUri = URI.create(base + "/api/v1/internal/recommend/feedback/batch");
    }

    public int getAvailableBulkheadPermits() {
        return this.feedbackBulkhead.availablePermits();
    }

    /**
     * Testing constructor allowing custom RestClient or MockRestServiceServer.
     */
    public HttpRecommendationFeedbackClient(RestClient restClient, RecommendationFeedbackProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
        this.feedbackBulkhead = new java.util.concurrent.Semaphore(
                Math.max(1, properties.getMaxConcurrentRequests()), true);
        this.bulkheadAcquireTimeoutMs = properties.getBulkheadAcquireTimeoutMs();
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        this.feedbackEndpointUri = URI.create(base + "/api/v1/internal/recommend/feedback/batch");
    }

    @Override
    public RecommendationFeedbackBatchResponse sendBatch(RecommendationFeedbackBatchRequest request) {
        boolean acquired = false;
        try {
            acquired = feedbackBulkhead.tryAcquire(bulkheadAcquireTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RecommendationException("Interrupted acquiring feedback bulkhead permit", e);
        }

        if (!acquired) {
            log.warn("Recommendation feedback bulkhead saturated (limit={}, timeout={}ms). Shedding batch load.",
                    properties.getMaxConcurrentRequests(), bulkheadAcquireTimeoutMs);
            throw new RecommendationException("Feedback delivery bulkhead capacity saturated (" + properties.getMaxConcurrentRequests() + ")");
        }

        long startMs = System.currentTimeMillis();
        try {
            ResponseEntity<RecommendationFeedbackBatchResponse> responseEntity = restClient.post()
                    .uri(feedbackEndpointUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getInternalToken())
                    .body(request)
                    .retrieve()
                    .toEntity(RecommendationFeedbackBatchResponse.class);

            RecommendationFeedbackBatchResponse body = responseEntity.getBody();
            if (body == null) {
                throw new RecommendationException("Python feedback ingestion returned empty response body");
            }

            long durationMs = System.currentTimeMillis() - startMs;
            log.info("Feedback batch delivery completed: events={}, accepted={}, duplicates={}, rejected={}, durationMs={}",
                    request.events().size(), body.accepted(), body.duplicates(), body.rejected(), durationMs);
            return body;
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - startMs;
            log.warn("Feedback batch delivery HTTP call failed for {} events, durationMs={}: {}",
                    request != null && request.events() != null ? request.events().size() : 0, durationMs, ex.getMessage());
            throw new RecommendationException("Feedback batch delivery failed: " + ex.getMessage(), ex);
        } finally {
            feedbackBulkhead.release();
        }
    }
}
