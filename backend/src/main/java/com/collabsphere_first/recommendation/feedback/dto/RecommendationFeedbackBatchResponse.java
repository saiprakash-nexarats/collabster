package com.collabsphere_first.recommendation.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecommendationFeedbackBatchResponse(
        String contractVersion,
        int accepted,
        int duplicates,
        int rejected,
        List<EventResultItem> items
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EventResultItem(
            UUID eventId,
            String status, // "ACCEPTED", "DUPLICATE", "REJECTED"
            String errorCode,
            String errorMessage
    ) {
        public boolean isAcceptedOrDuplicate() {
            return "ACCEPTED".equalsIgnoreCase(status) || "DUPLICATE".equalsIgnoreCase(status);
        }

        public boolean isRejected() {
            return "REJECTED".equalsIgnoreCase(status);
        }
    }
}
