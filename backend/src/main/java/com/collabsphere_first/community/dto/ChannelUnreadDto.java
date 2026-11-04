package com.collabsphere_first.community.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class ChannelUnreadDto {
    UUID channelId;
    long unreadCount;
}
