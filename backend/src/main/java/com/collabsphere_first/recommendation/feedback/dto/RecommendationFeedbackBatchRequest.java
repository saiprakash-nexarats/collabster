package com.collabsphere_first.recommendation.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecommendationFeedbackBatchRequest(
        @NotNull(message = "contractVersion must not be null")
        String contractVersion,

        @NotEmpty(message = "events list must not be empty")
        @Size(max = 500, message = "batch size cannot exceed 500 events")
        List<RecommendationEventDto> events
) {
    public static RecommendationFeedbackBatchRequest of(List<RecommendationEventDto> events) {
        return new RecommendationFeedbackBatchRequest(RecommendationEventDto.CURRENT_CONTRACT_VERSION, events);
    }
}
