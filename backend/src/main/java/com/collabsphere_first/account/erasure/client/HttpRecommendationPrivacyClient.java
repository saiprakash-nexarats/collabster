package com.collabsphere_first.account.erasure.client;

import com.collabsphere_first.account.erasure.config.RecommendationPrivacyProperties;
import com.collabsphere_first.recommendation.exception.RecommendationException;
import com.collabsphere_first.recommendation.feedback.config.RecommendationFeedbackProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class HttpRecommendationPrivacyClient implements RecommendationPrivacyClient {

    private final RestClient restClient;
    private final RecommendationPrivacyProperties properties;
    private final URI eraseEndpointUri;
    private final Semaphore privacyBulkhead;
    private final long bulkheadAcquireTimeoutMs;

    @Autowired
    public HttpRecommendationPrivacyClient(RecommendationPrivacyProperties properties) {
        this.properties = properties;
        this.privacyBulkhead = new Semaphore(Math.max(1, properties.getMaxConcurrentRequests()), true);
        this.bulkheadAcquireTimeoutMs = properties.getBulkheadAcquireTimeoutMs();

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(java.time.Duration.ofMillis(Math.max(properties.getConnectTimeoutMs(), 1000)))
                .executor(Executors.newFixedThreadPool(
                        Math.max(1, properties.getMaxConcurrentRequests()),
                        r -> {
                            Thread t = new Thread(r, "rec-privacy-pool-" + r.hashCode());
                            t.setDaemon(true);
                            return t;
                        }
                ))
                .build();

        org.springframework.http.client.JdkClientHttpRequestFactory factory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(java.time.Duration.ofMillis(Math.max(properties.getReadTimeoutMs(), 1000)));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();

        String base = properties.getBaseUrl().replaceAll("/+$", "");
        this.eraseEndpointUri = URI.create(base + "/api/v1/internal/recommend/privacy/erase");
    }

    /**
     * Testing constructor allowing custom RestClient and RecommendationPrivacyProperties.
     */
    public HttpRecommendationPrivacyClient(RestClient restClient, RecommendationPrivacyProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
        this.privacyBulkhead = new Semaphore(Math.max(1, properties.getMaxConcurrentRequests()), true);
        this.bulkheadAcquireTimeoutMs = properties.getBulkheadAcquireTimeoutMs();
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        this.eraseEndpointUri = URI.create(base + "/api/v1/internal/recommend/privacy/erase");
    }

    /**
     * Backwards-compatible testing constructor using RecommendationFeedbackProperties.
     */
    public HttpRecommendationPrivacyClient(RestClient restClient, RecommendationFeedbackProperties feedbackProperties) {
        RecommendationPrivacyProperties privProps = new RecommendationPrivacyProperties();
        privProps.setBaseUrl(feedbackProperties.getBaseUrl());
        privProps.setInternalToken(feedbackProperties.getInternalToken());
        privProps.setConnectTimeoutMs(feedbackProperties.getConnectTimeoutMs());
        privProps.setReadTimeoutMs(feedbackProperties.getReadTimeoutMs());
        this.restClient = restClient;
        this.properties = privProps;
        this.privacyBulkhead = new Semaphore(Math.max(1, privProps.getMaxConcurrentRequests()), true);
        this.bulkheadAcquireTimeoutMs = privProps.getBulkheadAcquireTimeoutMs();
        String base = privProps.getBaseUrl().replaceAll("/+$", "");
        this.eraseEndpointUri = URI.create(base + "/api/v1/internal/recommend/privacy/erase");
    }

    public int getAvailableBulkheadPermits() {
        return this.privacyBulkhead.availablePermits();
    }

    @Override
    public boolean eraseUserData(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        boolean acquired = false;
        try {
            acquired = privacyBulkhead.tryAcquire(bulkheadAcquireTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RecommendationException("Interrupted acquiring privacy bulkhead permit for userId=" + userId, e);
        }

        if (!acquired) {
            log.warn("Privacy erasure bulkhead saturated for userId={} (limit={}, timeout={}ms). Shedding job.",
                    userId, properties.getMaxConcurrentRequests(), bulkheadAcquireTimeoutMs);
            throw new RecommendationException("Privacy erasure bulkhead capacity saturated (" + properties.getMaxConcurrentRequests() + ")");
        }

        String token = resolveToken();
        log.info("Dispatching privacy erasure request to {} for userId={}", eraseEndpointUri, userId);

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(eraseEndpointUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("userId", userId))
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Privacy erasure succeeded for userId={}", userId);
                return true;
            } else {
                log.error("Privacy erasure returned non-2xx status {} for userId={}", response.getStatusCode(), userId);
                throw new IllegalStateException("Privacy erasure failed with status " + response.getStatusCode());
            }
        } catch (Exception ex) {
            log.error("HTTP call to recommendation privacy endpoint failed for userId={}: {}", userId, ex.getMessage());
            throw ex;
        } finally {
            privacyBulkhead.release();
        }
    }

    private String resolveToken() {
        String token = properties.getInternalToken();
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        String envToken = System.getenv("COLLOBSTER_RECOMMENDATION_INTERNAL_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            return envToken.trim();
        }
        return "";
    }
}
