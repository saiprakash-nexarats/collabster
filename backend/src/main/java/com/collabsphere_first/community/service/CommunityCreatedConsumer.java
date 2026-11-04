package com.collabsphere_first.community.service;

import com.collabsphere_first.community.event.CommunityOutboxConsumer;
import com.collabsphere_first.community.event.CommunityOutboxMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityCreatedConsumer implements CommunityOutboxConsumer {
    private final ObjectMapper objectMapper;
    private final org.springframework.beans.factory.ObjectProvider<CommunityService> communityServiceProvider;

    @Override
    public String name() {
        return "community-created";
    }

    @Override
    public boolean supports(String eventType) {
        return "COMMUNITY_CREATED".equals(eventType);
    }

    @Override
    public void handle(CommunityOutboxMessage message) {
        try {
            var payload = objectMapper.readValue(
                    message.payload(),
                    CommunityCreatedPayload.class
            );

            if (payload.name() == null || payload.name().isBlank()) {
                throw new IllegalArgumentException("Community name is required");
            }

            CommunityService communityService = communityServiceProvider.getObject();
            communityService.handleCommunityCreated(
                    message.eventId(),
                    message.communityId(),
                    payload.name()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid COMMUNITY_CREATED payload", e);
        }
    }

    public record CommunityCreatedPayload(String name) {}
}