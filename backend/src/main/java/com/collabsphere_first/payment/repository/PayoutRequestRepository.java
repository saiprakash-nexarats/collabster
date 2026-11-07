package com.collabsphere_first.payment.repository;

import com.collabsphere_first.payment.entity.PayoutRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, UUID> {
    List<PayoutRequest> findByProviderIdOrderByCreatedAtDesc(Long providerId);
    Optional<PayoutRequest> findByIdAndProviderId(UUID id, Long providerId);
    Optional<PayoutRequest> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT COALESCE(SUM(p.amountCents), 0) FROM PayoutRequest p WHERE p.providerId = :providerId AND p.status IN ('requested', 'pending', 'processing')")
    Long calculatePendingReservations(@Param("providerId") Long providerId);
}

