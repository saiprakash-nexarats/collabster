package com.collabsphere_first.interaction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchInteractionEventRequest {

    @NotEmpty(message = "Events list must not be empty")
    @Size(max = 50, message = "Batch events cannot exceed 50 items per request")
    @Valid
    private List<InteractionEventRequest> events;
}
