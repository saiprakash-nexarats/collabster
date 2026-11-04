package com.collabsphere_first.community.service;


import com.collabsphere_first.community.repository.CommunityOutboxDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityOutboxDeliveryWorker {
    private final CommunityOutboxDeliveryRepository deliveryRepository;
    private final CommunityOutboxDeliveryAttempt deliveryAttempt;
    private final CommunityOutboxRetry retry;

    @Scheduled(fixedDelayString = "${collabster.communities.outbox.poll-ms:5000}")
    public void publishPending() {
        var due = deliveryRepository
                .findByDeliveredAtIsNullAndFailedAtIsNullAndAvailableAtLessThanEqualOrderByAvailableAtAsc(
                        Instant.now(), PageRequest.of(0, 100));

        for (var delivery : due.getContent()) {
            try {
                deliveryAttempt.deliver(delivery.getId());
            } catch (RuntimeException e) {
                retry.failed(delivery.getId(), e);
                log.warn("Outbox delivery {} will retry", delivery.getId());
            }
        }
    }
}
