package com.collabsphere_first.integration.ai.impl;

import com.collabsphere_first.integration.ai.AiModerationProvider;
import com.collabsphere_first.integration.ai.AiModerationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

/**
 * Opt-in adapter for providers that accept {"text":"..."} and return JSON
 * with a decision of PASSED, FLAGGED, or VIOLATION. Configuration supplies all
 * connection details; no credential is retained in source.
 */
@Component
@ConditionalOnProperty(prefix = "collabster.communities.ai-moderation", name = "provider", havingValue = "http")
public class HttpAiModerationProvider implements AiModerationProvider {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String endpoint;
    private final String apiKey;
    private final Duration timeout;

    public HttpAiModerationProvider(ObjectMapper objectMapper,
                                    @Value("${collabster.communities.ai-moderation.http.endpoint:}") String endpoint,
                                    @Value("${collabster.communities.ai-moderation.http.api-key:}") String apiKey,
                                    @Value("${collabster.communities.ai-moderation.http.timeout-ms:1500}") long timeoutMs) {
        this.objectMapper = objectMapper;
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.timeout = Duration.ofMillis(Math.max(100, timeoutMs));
        this.httpClient = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    @Override
    public boolean isConfigured() {
        return !endpoint.isBlank() && !apiKey.isBlank();
    }

    @Override
    public AiModerationService.ModerationResult classify(String text) {
        if (!isConfigured()) throw new IllegalStateException("HTTP AI moderation provider is not configured");
        try {
            String payload = objectMapper.writeValueAsString(java.util.Map.of("text", text == null ? "" : text));
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(timeout)
                    .header("Content-Type", "application/json").header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429 || response.statusCode() >= 500) throw new IllegalStateException("AI provider unavailable");
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalArgumentException("AI provider rejected request");
            JsonNode decision = objectMapper.readTree(response.body()).path("decision");
            return AiModerationService.ModerationResult.valueOf(decision.asText().trim().toUpperCase(Locale.ROOT));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI provider request interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("AI provider request failed", e);
        }
    }
}
