package com.collabsphere_first.payment.repository;

import com.collabsphere_first.payment.entity.PayoutRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, UUID> {
    List<PayoutRequest> findByProviderIdOrderByCreatedAtDesc(Long providerId);
    Optional<PayoutRequest> findByIdAndProviderId(UUID id, Long providerId);
}

