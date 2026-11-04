package com.collabsphere_first.recommendation.feedback.dto;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType;
import com.collabsphere_first.recommendation.feedback.enums.FeedbackSurface;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecommendationEventDto(
        String contractVersion,
        UUID eventId,
        FeedbackEventType eventType,
        Instant occurredAt,
        Long userId,
        ContentRef contentRef,
        EventContext context,
        Long durationMs
) {
    public static final String CURRENT_CONTRACT_VERSION = "1";

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EventContext(
            FeedbackSurface surface,
            String feedSessionId,
            String requestId,
            Integer rankPosition,
            String modelVersion
    ) {
        public static EventContext of(FeedbackSurface surface) {
            return EventContext.builder()
                    .surface(surface != null ? surface : FeedbackSurface.UNKNOWN)
                    .build();
        }

        public static EventContext of(FeedbackSurface surface, String feedSessionId) {
            return EventContext.builder()
                    .surface(surface != null ? surface : FeedbackSurface.UNKNOWN)
                    .feedSessionId(feedSessionId)
                    .build();
        }
    }
}
