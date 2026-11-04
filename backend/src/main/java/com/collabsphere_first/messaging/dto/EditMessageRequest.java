package com.collabsphere_first.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditMessageRequest(
        @NotBlank(message = "Message body cannot be blank")
        @Size(max = 10000, message = "Message body must not exceed 10,000 characters")
        String body
) {}
