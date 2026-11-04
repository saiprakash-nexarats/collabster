package com.collabsphere_first.recommendation.client;

import com.collabsphere_first.recommendation.config.RecommendationProperties;
import com.collabsphere_first.recommendation.dto.RecommendationRankRequest;
import com.collabsphere_first.recommendation.dto.RecommendationRankResponse;
import com.collabsphere_first.recommendation.exception.RecommendationCircuitBreakerOpenException;
import com.collabsphere_first.recommendation.exception.RecommendationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Slf4j
@Component
public class HttpRecommendationClient implements RecommendationClient {

    private final RestClient restClient;
    private final RecommendationProperties properties;
    private final RecommendationCircuitBreaker circuitBreaker;
    private final URI rankEndpointUri;
    private final java.util.concurrent.Semaphore rankingBulkhead;
    private final long bulkheadAcquireTimeoutMs;

    @org.springframework.beans.factory.annotation.Autowired
    public HttpRecommendationClient(
            RecommendationProperties properties,
            RecommendationCircuitBreaker circuitBreaker
    ) {
        this.properties = properties;
        this.circuitBreaker = circuitBreaker;
        this.rankingBulkhead = new java.util.concurrent.Semaphore(
                Math.max(1, properties.getMaxConcurrentRequests()), true);
        this.bulkheadAcquireTimeoutMs = properties.getBulkheadAcquireTimeoutMs();

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(java.time.Duration.ofMillis(properties.getConnectTimeoutMs()))
                .executor(java.util.concurrent.Executors.newFixedThreadPool(
                        Math.max(1, properties.getMaxConcurrentRequests()),
                        r -> {
                            Thread t = new Thread(r, "rec-http-pool-" + r.hashCode());
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
        this.rankEndpointUri = URI.create(base + "/api/v1/internal/recommend/feed/rank");
    }

    public RestClient getRestClient() {
        return this.restClient;
    }

    public RecommendationProperties getProperties() {
        return this.properties;
    }

    public int getAvailableBulkheadPermits() {
        return this.rankingBulkhead.availablePermits();
    }

    /**
     * Public constructor for testing with custom RestClient or MockRestServiceServer.
     */
    public HttpRecommendationClient(
            RestClient restClient,
            RecommendationProperties properties,
            RecommendationCircuitBreaker circuitBreaker
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.circuitBreaker = circuitBreaker;
        this.rankingBulkhead = new java.util.concurrent.Semaphore(
                Math.max(1, properties.getMaxConcurrentRequests()), true);
        this.bulkheadAcquireTimeoutMs = properties.getBulkheadAcquireTimeoutMs();
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        this.rankEndpointUri = URI.create(base + "/api/v1/internal/recommend/feed/rank");
    }

    @Override
    public RecommendationRankResponse rank(RecommendationRankRequest request) {
        boolean acquired = false;
        try {
            acquired = rankingBulkhead.tryAcquire(bulkheadAcquireTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new com.collabsphere_first.recommendation.exception.RecommendationBulkheadFullException(
                    "Interrupted acquiring ranking bulkhead permit for requestId=" + request.requestId(), e);
        }

        if (!acquired) {
            log.warn("Recommendation ranking bulkhead saturated for requestId={} (limit={}, timeout={}ms). Shedding load.",
                    request.requestId(), properties.getMaxConcurrentRequests(), bulkheadAcquireTimeoutMs);
            throw new com.collabsphere_first.recommendation.exception.RecommendationBulkheadFullException(
                    "Recommendation ranking bulkhead capacity saturated (" + properties.getMaxConcurrentRequests() + ")");
        }

        long startMs = System.currentTimeMillis();
        try {
            return circuitBreaker.execute(() -> {
                ResponseEntity<RecommendationRankResponse> responseEntity = restClient.post()
                        .uri(rankEndpointUri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getInternalToken())
                        .body(request)
                        .retrieve()
                        .toEntity(RecommendationRankResponse.class);

                RecommendationRankResponse body = responseEntity.getBody();
                if (body == null) {
                    throw new RecommendationException("Recommendation ranker returned empty response body");
                }
                long durationMs = System.currentTimeMillis() - startMs;
                log.info("Recommendation ranking completed for requestId={}, candidates={}, durationMs={}",
                        request.requestId(), request.candidates().size(), durationMs);
                return body;
            });
        } catch (RecommendationCircuitBreakerOpenException ex) {
            log.warn("Recommendation ranking rejected for requestId={}: {}", request.requestId(), ex.getMessage());
            throw ex;
        } catch (RecommendationException ex) {
            long durationMs = System.currentTimeMillis() - startMs;
            log.warn("Recommendation ranking failed for requestId={}, candidates={}, durationMs={}: {}",
                    request.requestId(), request.candidates().size(), durationMs, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - startMs;
            log.warn("Recommendation ranking HTTP call failed for requestId={}, candidates={}, durationMs={}: {}",
                    request.requestId(), request.candidates().size(), durationMs, ex.getMessage());
            throw new RecommendationException("Recommendation ranking failed: " + ex.getMessage(), ex);
        } finally {
            rankingBulkhead.release();
        }
    }
}
