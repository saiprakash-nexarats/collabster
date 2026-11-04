package com.collabsphere_first.messaging.dto;

import java.util.UUID;

public record TypingMessagePayload(
    UUID conversationId,
    boolean typing
) {}
