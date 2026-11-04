package com.collabsphere_first.community.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class CommunityChannelMessageDto {
    UUID id;
    UUID communityId;
    UUID channelId;
    Long senderId;
    String body;
    UUID parentMessageId;
    @Builder.Default
    List<String> attachmentUrls = List.of();

    @Builder.Default
    List<CommunityChannelMessageReactionDto> reactions = List.of();
    boolean removed;
    Instant createdAt;
    Instant updatedAt;
}
