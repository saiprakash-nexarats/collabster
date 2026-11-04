package com.collabsphere_first.messaging.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DeliveryReceiptRequest(
    @NotNull(message = "messageId is required")
    UUID messageId
) {}
