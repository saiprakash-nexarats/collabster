package com.collabsphere_first.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record ReactionResponse(
    UUID id,
    Long userId,
    String username,
    String reaction,
    Instant createdAt
) {}
