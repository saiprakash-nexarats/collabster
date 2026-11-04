package com.collabsphere_first.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReactionRequest(
    @NotBlank(message = "Reaction must not be blank")
    @Size(max = 32, message = "Reaction must not exceed 32 characters")
    String reaction
) {}
