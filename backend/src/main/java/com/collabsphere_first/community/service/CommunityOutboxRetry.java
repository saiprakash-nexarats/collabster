package com.collabsphere_first.community.service;

import com.collabsphere_first.community.repository.CommunityOutboxDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityOutboxRetry {
    private final CommunityOutboxDeliveryRepository deliveries;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(UUID id, RuntimeException error) {
        var d = deliveries.findByIdForUpdate(id).orElse(null);
        if (d == null || d.getDeliveredAt() != null) return;

        int attempts = d.getAttempts() + 1;
        d.setAttempts(attempts);
        d.setLastError(error.getClass().getSimpleName());

        if (attempts >= 12) d.setFailedAt(Instant.now());
        else d.setAvailableAt(Instant.now().plusSeconds(
                Math.min(3600L, 1L << Math.min(attempts, 12))));
    }
}
