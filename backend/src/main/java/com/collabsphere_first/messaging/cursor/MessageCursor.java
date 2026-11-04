package com.collabsphere_first.messaging.cursor;

import java.time.Instant;
import java.util.UUID;

public record MessageCursor(
    Instant createdAt,
    UUID messageId
) {}
