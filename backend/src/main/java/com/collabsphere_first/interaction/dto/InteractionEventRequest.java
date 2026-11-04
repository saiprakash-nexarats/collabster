package com.collabsphere_first.interaction.dto;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.interaction.enums.InteractionEventType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionEventRequest {

    @NotNull(message = "contentRef must not be null")
    private ContentRef contentRef;

    @NotNull(message = "eventType must not be null")
    private InteractionEventType eventType;

    @Min(value = 0, message = "durationMs must be non-negative")
    @Max(value = 86400000, message = "durationMs must not exceed 24 hours (86,400,000 ms)")
    private Long durationMs;

    private String sessionId;
}
