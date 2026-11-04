package com.collabsphere_first.messaging.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReadReceiptRequest(
    @NotNull(message = "lastReadMessageId is required")
    UUID lastReadMessageId
) {}
