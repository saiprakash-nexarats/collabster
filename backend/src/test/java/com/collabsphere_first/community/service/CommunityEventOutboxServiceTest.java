package com.collabsphere_first.community.service;

import com.collabsphere_first.community.entity.CommunityOutboxEvent;
import com.collabsphere_first.community.repository.CommunityOutboxDeliveryRepository;
import com.collabsphere_first.community.repository.CommunityOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommunityEventOutboxServiceTest {
    @Test
    void persistsAnEventEvenWhenNoSpecializedConsumerIsRegistered() {
        CommunityOutboxRepository events = mock(CommunityOutboxRepository.class);
        CommunityOutboxDeliveryRepository deliveries = mock(CommunityOutboxDeliveryRepository.class);
        when(events.save(any(CommunityOutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CommunityEventOutboxService service = new CommunityEventOutboxService(events, new ObjectMapper(), List.of(), deliveries);

        assertDoesNotThrow(() -> service.record(UUID.randomUUID(), "MEMBER_JOINED", "7", Map.of("userId", 7)));
        verify(events).save(any(CommunityOutboxEvent.class));
        verifyNoInteractions(deliveries);
    }
}
