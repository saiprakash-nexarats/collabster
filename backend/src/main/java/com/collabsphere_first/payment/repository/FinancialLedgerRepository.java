package com.collabsphere_first.payment.repository;

import com.collabsphere_first.payment.entity.FinancialLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FinancialLedgerRepository extends JpaRepository<FinancialLedgerEntry, UUID> {
    List<FinancialLedgerEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.entryType IN ('PAYMENT_CREDIT', 'ESCROW_RELEASE') THEN e.amountCents ELSE -e.amountCents END), 0) FROM FinancialLedgerEntry e WHERE e.userId = :userId")
    Long calculateUserBalanceCents(@Param("userId") Long userId);
}

