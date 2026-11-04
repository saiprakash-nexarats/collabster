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
import java.util.Map;

/** Concrete adapter for OpenAI's POST /v1/moderations API. */
@Component
@ConditionalOnProperty(prefix = "collabster.communities.ai-moderation", name = "provider", havingValue = "openai")
public class OpenAiModerationProvider implements AiModerationProvider {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final Duration timeout;

    public OpenAiModerationProvider(
            ObjectMapper objectMapper,
            @Value("${collabster.communities.ai-moderation.openai.api-key:}") String apiKey,
            @Value("${collabster.communities.ai-moderation.openai.endpoint:https://api.openai.com/v1/moderations}") String endpoint,
            @Value("${collabster.communities.ai-moderation.openai.model:omni-moderation-latest}") String model,
            @Value("${collabster.communities.ai-moderation.openai.timeout-ms:2000}") long timeoutMs) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        this.model = model == null ? "omni-moderation-latest" : model.trim();
        this.timeout = Duration.ofMillis(Math.max(100, timeoutMs));
        this.httpClient = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank() && !endpoint.isBlank();
    }

    @Override
    public AiModerationService.ModerationResult classify(String text) {
        if (!isConfigured()) throw new IllegalStateException("OpenAI moderation is not configured");
        try {
            String payload = objectMapper.writeValueAsString(Map.of("input", text == null ? "" : text, "model", model));
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI moderation request failed with status " + response.statusCode());
            }
            JsonNode result = objectMapper.readTree(response.body()).path("results").path(0);
            if (result.isMissingNode() || !result.has("flagged")) {
                throw new IllegalStateException("OpenAI moderation response did not contain results[0].flagged");
            }
            return result.path("flagged").asBoolean() ? AiModerationService.ModerationResult.VIOLATION
                    : AiModerationService.ModerationResult.PASSED;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI moderation request interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("OpenAI moderation request failed", exception);
        }
    }
}
