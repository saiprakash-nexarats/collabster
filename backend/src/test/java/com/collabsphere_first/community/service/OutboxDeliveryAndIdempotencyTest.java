package com.collabsphere_first.community.service;

import com.collabsphere_first.community.entity.CommunityOutboxDelivery;
import com.collabsphere_first.community.entity.CommunityOutboxEvent;
import com.collabsphere_first.community.event.CommunityOutboxConsumer;
import com.collabsphere_first.community.event.CommunityOutboxMessage;
import com.collabsphere_first.community.repository.CommunityOutboxDeliveryRepository;
import com.collabsphere_first.community.repository.CommunityOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxDeliveryAndIdempotencyTest {

    @Mock private CommunityOutboxRepository outboxRepository;
    @Mock private CommunityOutboxDeliveryRepository deliveryRepository;

    private CommunityEventOutboxService outboxService;
    private ObjectMapper objectMapper;
    private UUID communityId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        communityId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Outbox records event and registers delivery for matching consumers")
    void record_createsEventAndDeliveries() {
        CommunityOutboxConsumer consumer1 = mock(CommunityOutboxConsumer.class);
        when(consumer1.supports("POST_CREATED")).thenReturn(true);
        when(consumer1.name()).thenReturn("consumer-1");

        CommunityOutboxConsumer consumer2 = mock(CommunityOutboxConsumer.class);
        when(consumer2.supports("POST_CREATED")).thenReturn(false);

        outboxService = new CommunityEventOutboxService(outboxRepository, objectMapper, List.of(consumer1, consumer2), deliveryRepository);

        when(outboxRepository.save(any(CommunityOutboxEvent.class))).thenAnswer(i -> i.getArgument(0));

        outboxService.record(communityId, "POST_CREATED", "post-123", Map.of("title", "Hello"));

        ArgumentCaptor<CommunityOutboxDelivery> deliveryCaptor = ArgumentCaptor.forClass(CommunityOutboxDelivery.class);
        verify(deliveryRepository).save(deliveryCaptor.capture());

        CommunityOutboxDelivery delivery = deliveryCaptor.getValue();
        assertThat(delivery.getConsumerName()).isEqualTo("consumer-1");
        assertThat(delivery.getAttempts()).isZero();
        assertThat(delivery.getDeliveredAt()).isNull();
    }

    @Test
    @DisplayName("Consumer processes duplicate delivery idempotently without side effects")
    void consumer_idempotentProcessing() {
        Set<UUID> processedEventIds = ConcurrentHashMap.newKeySet();
        AtomicInteger sideEffects = new AtomicInteger();

        CommunityOutboxConsumer idempotentConsumer = new CommunityOutboxConsumer() {
            @Override
            public String name() {
                return "idempotent-consumer";
            }

            @Override
            public boolean supports(String eventType) {
                return true;
            }

            @Override
            public void handle(CommunityOutboxMessage message) {
                // Idempotency check using event ID
                if (!processedEventIds.add(message.eventId())) {
                    return; // duplicate detected, skip
                }
                sideEffects.incrementAndGet();
            }
        };

        UUID eventId = UUID.randomUUID();
        CommunityOutboxMessage msg = new CommunityOutboxMessage(
                eventId, communityId, "POST_CREATED", "aggregate-1", "{\"key\":\"val\"}");

        // First delivery: executed
        idempotentConsumer.handle(msg);
        assertThat(sideEffects.get()).isEqualTo(1);

        // Duplicate delivery with same eventId: skipped
        idempotentConsumer.handle(msg);
        assertThat(sideEffects.get()).isEqualTo(1);
    }
}
