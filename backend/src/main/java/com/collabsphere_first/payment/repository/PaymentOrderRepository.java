package com.collabsphere_first.payment.repository;

import com.collabsphere_first.payment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {
    Optional<PaymentOrder> findByIdAndUserId(UUID id, Long userId);
    Optional<PaymentOrder> findByOrderNumber(String orderNumber);
    Optional<PaymentOrder> findByIdempotencyKey(String idempotencyKey);
    List<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<PaymentOrder> findByGatewayOrderId(String gatewayOrderId);
    Optional<PaymentOrder> findByGatewayPaymentId(String gatewayPaymentId);
    Optional<PaymentOrder> findByGatewayPaymentIdAndStatus(String gatewayPaymentId, String status);
}

