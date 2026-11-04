package com.collabsphere_first.messaging.dto;

import java.util.List;

public record ConversationPageResponse(
    List<ConversationResponse> items,
    String nextCursor,
    boolean hasMore
) {}
