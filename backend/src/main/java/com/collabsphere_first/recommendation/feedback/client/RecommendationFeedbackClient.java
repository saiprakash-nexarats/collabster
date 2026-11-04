package com.collabsphere_first.recommendation.feedback.client;

import com.collabsphere_first.recommendation.feedback.dto.RecommendationFeedbackBatchRequest;
import com.collabsphere_first.recommendation.feedback.dto.RecommendationFeedbackBatchResponse;

public interface RecommendationFeedbackClient {

    /**
     * Transmit a batch of recommendation feedback events to the internal Python recommendation service.
     *
     * @param request the batch request containing up to 500 events
     * @return the batch response containing accepted, duplicate, and rejected counts with per-item status
     */
    RecommendationFeedbackBatchResponse sendBatch(RecommendationFeedbackBatchRequest request);
}
