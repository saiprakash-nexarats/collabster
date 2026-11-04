package com.collabsphere_first.messaging.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SendMessageRequest(
        @NotNull(message = "clientMessageId is required for idempotent delivery")
        UUID clientMessageId,

        @Size(max = 10000, message = "Message body must not exceed 10,000 characters")
        String body,

        UUID replyToMessageId,

        @Valid
        @Size(max = 10, message = "A message cannot have more than 10 attachments")
        List<AttachmentRequest> attachments
) {}
