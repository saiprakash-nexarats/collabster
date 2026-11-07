package com.collabsphere_first.payment.repository;

import com.collabsphere_first.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
