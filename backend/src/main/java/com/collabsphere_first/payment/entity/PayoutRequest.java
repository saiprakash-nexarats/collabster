package com.collabsphere_first.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payout_requests", indexes = {
        @Index(name = "idx_payout_requests_provider", columnList = "provider_id"),
        @Index(name = "idx_payout_requests_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "method", nullable = false, length = 32)
    @Builder.Default
    private String method = "bank_account";

    @Column(name = "destination_reference", nullable = false)
    private String destinationReference;

    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private String status = "pending";

    @Column(name = "gateway_payout_id", length = 128)
    private String gatewayPayoutId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "processed_at")
    private Instant processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

