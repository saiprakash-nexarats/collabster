package com.collabsphere_first.messaging.dto;

import java.util.List;

public record MessagePageResponse(
    List<MessageResponse> items,
    String nextCursor,
    boolean hasMore
) {}
