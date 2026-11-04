package com.collabsphere_first.messaging.websocket;

import com.collabsphere_first.messaging.dto.DeliveryAckPayload;
import com.collabsphere_first.messaging.dto.TypingMessagePayload;
import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.messaging.presence.MessagingTypingService;
import com.collabsphere_first.messaging.security.MessagingRateLimiter;
import com.collabsphere_first.messaging.service.MessagingService;
import com.collabsphere_first.security.userdetails.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MessagingWebSocketController {

    private final MessagingTypingService typingService;
    private final MessagingService messagingService;
    private final MessagingRateLimiter rateLimiter;
    private final MessagingPresenceService presenceService;

    @MessageMapping("/messages/typing")
    public void handleTyping(@Payload TypingMessagePayload payload, Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth &&
                auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            Long userId = userPrincipal.getUser().getId();
            String username = userPrincipal.getUsername();

            try {
                rateLimiter.checkTyping(userId);
                typingService.handleTyping(payload.conversationId(), userId, username, payload.typing());
            } catch (Exception ex) {
                log.warn("Failed to process typing message for user {}: {}", username, ex.getMessage());
            }
        }
    }

    @MessageMapping("/messages/delivery-ack")
    public void handleDeliveryAck(@Payload DeliveryAckPayload payload, Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth &&
                auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            Long userId = userPrincipal.getUser().getId();

            try {
                messagingService.markDelivered(payload.messageId(), userId);
            } catch (Exception ex) {
                log.warn("Failed to process delivery ack for message {} from user {}: {}",
                        payload.messageId(), userId, ex.getMessage());
            }
        }
    }

    @MessageMapping("/messages/heartbeat")
    public void handleHeartbeat(Principal principal, @Header("simpSessionId") String sessionId) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth &&
                auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            Long userId = userPrincipal.getUser().getId();
            try {
                presenceService.onHeartbeat(userId, sessionId);
            } catch (Exception ex) {
                log.warn("Failed to process heartbeat for user {}: {}", userId, ex.getMessage());
            }
        }
    }
}
