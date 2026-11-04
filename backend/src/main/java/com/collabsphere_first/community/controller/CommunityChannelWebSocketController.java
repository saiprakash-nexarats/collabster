package com.collabsphere_first.community.controller;

import com.collabsphere_first.community.dto.CreateChannelMessageRequest;
import com.collabsphere_first.community.service.CommunityChannelService;
import com.collabsphere_first.security.userdetails.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CommunityChannelWebSocketController {
    private final CommunityChannelService channelService;

    @MessageMapping("/communities/{communityId}/channels/{channelId}/messages")
    @PreAuthorize("hasPermission(#communityId, 'MEMBER')")
    public void send(@DestinationVariable UUID communityId, @DestinationVariable UUID channelId,
                     CreateChannelMessageRequest request, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalArgumentException("Authenticated STOMP session required");
        }
        channelService.send(communityId, channelId, principal.getUser().getId(), request);
    }
}
