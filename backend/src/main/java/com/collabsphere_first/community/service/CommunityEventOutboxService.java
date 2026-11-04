package com.collabsphere_first.community.service;

import com.collabsphere_first.community.entity.CommunityOutboxDelivery;
import com.collabsphere_first.community.entity.CommunityOutboxEvent;
import com.collabsphere_first.community.event.CommunityOutboxConsumer;
import com.collabsphere_first.community.repository.CommunityOutboxDeliveryRepository;
import com.collabsphere_first.community.repository.CommunityOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityEventOutboxService {

    private final CommunityOutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final List<CommunityOutboxConsumer> consumers;
    private final CommunityOutboxDeliveryRepository deliveryRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            UUID communityId,
            String eventType,
            String aggregateId,
            Map<String, ?> payload
    ) {
        if (communityId == null) {
            throw new IllegalArgumentException("Community ID is required");
        }

        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Event type is required");
        }

        try {
            Instant now = Instant.now();

            /*
             * IMPORTANT:
             * Do not manually assign the ID here.
             * CommunityOutboxEvent uses @GeneratedValue(strategy = UUID),
             * so Hibernate will generate the UUID when the entity is persisted.
             */
            CommunityOutboxEvent event = CommunityOutboxEvent.builder()
                    .communityId(communityId)
                    .eventType(eventType)
                    .aggregateId(aggregateId)
                    .payload(objectMapper.writeValueAsString(payload))
                    .attempts(0)
                    .availableAt(now)
                    .createdAt(now)
                    .build();

            CommunityOutboxEvent saved = repository.save(event);

            var matching = consumers.stream()
                    .filter(c -> c.supports(eventType))
                    .toList();

            matching.forEach(c ->
                    deliveryRepository.save(
                            CommunityOutboxDelivery.pending(
                                    saved.getId(),
                                    c.name(),
                                    now
                            )
                    )
            );

        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Could not serialize community domain event",
                    exception
            );
        }
    }
}