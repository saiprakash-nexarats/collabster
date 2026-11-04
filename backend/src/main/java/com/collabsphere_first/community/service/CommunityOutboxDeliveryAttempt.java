package com.collabsphere_first.community.service;

import com.collabsphere_first.community.event.CommunityOutboxConsumer;
import com.collabsphere_first.community.event.CommunityOutboxMessage;
import com.collabsphere_first.community.repository.CommunityOutboxDeliveryRepository;
import com.collabsphere_first.community.repository.CommunityOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityOutboxDeliveryAttempt {
    private final CommunityOutboxDeliveryRepository deliveries;
    private final CommunityOutboxRepository events;
    private final List<CommunityOutboxConsumer> consumers;

    @Transactional
    public void deliver(UUID deliveryId) {
        var delivery = deliveries.findByIdForUpdate(deliveryId).orElse(null);
        if (delivery == null || delivery.getDeliveredAt() != null
                || delivery.getFailedAt() != null
                || delivery.getAvailableAt().isAfter(Instant.now())) return;

        var event = events.findById(delivery.getEventId()).orElseThrow();
        var consumer = consumers.stream()
                .filter(c -> c.name().equals(delivery.getConsumerName()))
                .findFirst().orElseThrow();

        consumer.handle(new CommunityOutboxMessage(event.getId(), event.getCommunityId(),
                event.getEventType(), event.getAggregateId(), event.getPayload()));

        delivery.setDeliveredAt(Instant.now()); // commits with consumer's DB work
        if (deliveries.countByEventIdAndDeliveredAtIsNull(event.getId()) == 0) {
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
        }
    }
}

