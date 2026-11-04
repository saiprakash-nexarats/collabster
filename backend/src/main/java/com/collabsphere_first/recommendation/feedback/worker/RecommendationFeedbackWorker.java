package com.collabsphere_first.recommendation.feedback.worker;

import com.collabsphere_first.recommendation.feedback.client.RecommendationFeedbackClient;
import com.collabsphere_first.recommendation.feedback.config.RecommendationFeedbackProperties;
import com.collabsphere_first.recommendation.feedback.dto.RecommendationEventDto;
import com.collabsphere_first.recommendation.feedback.dto.RecommendationFeedbackBatchRequest;
import com.collabsphere_first.recommendation.feedback.dto.RecommendationFeedbackBatchResponse;
import com.collabsphere_first.recommendation.feedback.entity.RecommendationFeedbackOutbox;
import com.collabsphere_first.recommendation.feedback.service.RecommendationFeedbackOutboxService;
import com.collabsphere_first.recommendation.exception.RecommendationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationFeedbackWorker {

    private final RecommendationFeedbackOutboxService outboxService;
    private final RecommendationFeedbackClient feedbackClient;
    private final RecommendationFeedbackProperties properties;
    private final ObjectMapper objectMapper;

    private final String nodeId = "rec-worker-" + UUID.randomUUID();

    @Scheduled(fixedDelayString = "${collobster.recommendation.feedback.worker-interval-ms:2000}")
    public void processOutbox() {
        if (!properties.isDeliveryEnabled()) {
            return;
        }

        try {
            int batchSize = properties.getBatchSize();
            List<RecommendationFeedbackOutbox> jobs = outboxService.claimJobs(nodeId, batchSize);
            if (jobs.isEmpty()) {
                return;
            }

            log.debug("Claimed {} recommendation feedback outbox jobs on node {}", jobs.size(), nodeId);

            List<UUID> jobIds = new ArrayList<>();
            List<RecommendationEventDto> eventDtos = new ArrayList<>();

            for (RecommendationFeedbackOutbox job : jobs) {
                jobIds.add(job.getId());
                try {
                    RecommendationEventDto dto = objectMapper.readValue(job.getPayload(), RecommendationEventDto.class);
                    eventDtos.add(dto);
                } catch (Exception ex) {
                    log.error("Failed to parse stored payload for outbox job {}: {}", job.getId(), ex.getMessage(), ex);
                    // Mark poison entity as failed immediately
                    outboxService.handleBatchPermanentFailure(
                            List.of(job.getId()),
                            nodeId,
                            "CORRUPT_PAYLOAD",
                            "Unable to parse stored JSON payload: " + ex.getMessage()
                    );
                }
            }

            if (eventDtos.isEmpty()) {
                return;
            }

            RecommendationFeedbackBatchRequest request = new RecommendationFeedbackBatchRequest(
                    RecommendationEventDto.CURRENT_CONTRACT_VERSION,
                    eventDtos
            );

            try {
                // Call Python service outside DB transaction
                RecommendationFeedbackBatchResponse response = feedbackClient.sendBatch(request);
                outboxService.handleBatchSuccess(jobIds, response, nodeId);
            } catch (HttpClientErrorException ex) {
                int status = ex.getStatusCode().value();
                if (status == 401 || status == 403 || status == 422 || status == 400) {
                    log.error("Permanent HTTP error ({}) delivering recommendation feedback batch: {}", status, ex.getMessage());
                    outboxService.handleBatchPermanentFailure(jobIds, nodeId, "HTTP_" + status, ex.getMessage());
                } else {
                    log.warn("Client HTTP error ({}) delivering recommendation feedback batch: {}", status, ex.getMessage());
                    outboxService.handleBatchRetry(jobIds, nodeId, "HTTP_" + status, ex.getMessage());
                }
            } catch (HttpServerErrorException ex) {
                log.warn("Server HTTP error ({}) delivering recommendation feedback batch: {}", ex.getStatusCode().value(), ex.getMessage());
                outboxService.handleBatchRetry(jobIds, nodeId, "HTTP_" + ex.getStatusCode().value(), ex.getMessage());
            } catch (ResourceAccessException ex) {
                log.warn("Network/timeout delivering recommendation feedback batch: {}", ex.getMessage());
                outboxService.handleBatchRetry(jobIds, nodeId, "NETWORK_TIMEOUT", ex.getMessage());
            } catch (RecommendationException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof HttpClientErrorException clientEx) {
                    int status = clientEx.getStatusCode().value();
                    if (status == 401 || status == 403 || status == 422 || status == 400) {
                        log.error("Permanent HTTP error ({}) delivering recommendation feedback batch: {}", status, clientEx.getMessage());
                        outboxService.handleBatchPermanentFailure(jobIds, nodeId, "HTTP_" + status, clientEx.getMessage());
                    } else {
                        log.warn("Client HTTP error ({}) delivering recommendation feedback batch: {}", status, clientEx.getMessage());
                        outboxService.handleBatchRetry(jobIds, nodeId, "HTTP_" + status, clientEx.getMessage());
                    }
                } else if (cause instanceof HttpServerErrorException serverEx) {
                    log.warn("Server HTTP error ({}) delivering recommendation feedback batch: {}", serverEx.getStatusCode().value(), serverEx.getMessage());
                    outboxService.handleBatchRetry(jobIds, nodeId, "HTTP_" + serverEx.getStatusCode().value(), serverEx.getMessage());
                } else if (cause instanceof ResourceAccessException resourceEx) {
                    log.warn("Network/timeout delivering recommendation feedback batch: {}", resourceEx.getMessage());
                    outboxService.handleBatchRetry(jobIds, nodeId, "NETWORK_TIMEOUT", resourceEx.getMessage());
                } else {
                    log.warn("Delivery error delivering recommendation feedback batch: {}", ex.getMessage());
                    outboxService.handleBatchRetry(jobIds, nodeId, "DELIVERY_ERROR", ex.getMessage());
                }
            } catch (Exception ex) {
                log.error("Unexpected error delivering recommendation feedback batch: {}", ex.getMessage(), ex);
                outboxService.handleBatchRetry(jobIds, nodeId, "DELIVERY_ERROR", ex.getMessage());
            }

        } catch (Exception ex) {
            log.error("Unhandled error in recommendation feedback outbox worker: {}", ex.getMessage(), ex);
        }
    }
}
