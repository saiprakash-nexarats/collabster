package com.collabsphere_first.community.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommunityChannelMessageReactionDto {
    String reaction;
    long count;
    boolean reactedByCurrentUser;
}
