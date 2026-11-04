package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.CommunityChannelMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/** Sends committed channel changes to authorized STOMP subscribers. */
@Component
@RequiredArgsConstructor
public class CommunityRealtimePublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public void messageChanged(UUID communityId, UUID channelId, String action, CommunityChannelMessageDto message) {
        Runnable publish = () -> messagingTemplate.convertAndSend(destination(communityId, channelId),
                new ChannelMessageEvent(action, message));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { publish.run(); }
            });
        } else {
            publish.run();
        }
    }

    private String destination(UUID communityId, UUID channelId) {
        return "/topic/communities/" + communityId + "/channels/" + channelId;
    }

    public record ChannelMessageEvent(String action, CommunityChannelMessageDto message) { }
}
