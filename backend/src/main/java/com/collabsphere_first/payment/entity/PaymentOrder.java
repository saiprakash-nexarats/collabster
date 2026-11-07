package com.collabsphere_first.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_orders", indexes = {
        @Index(name = "idx_payment_orders_user", columnList = "user_id"),
        @Index(name = "idx_payment_orders_status", columnList = "status"),
        @Index(name = "idx_payment_orders_created", columnList = "created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_number", nullable = false, unique = true, length = 64)
    private String orderNumber;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private String status = "created";

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "gateway_provider", nullable = false, length = 32)
    @Builder.Default
    private String gatewayProvider = "SANDBOX";

    @Column(name = "gateway_order_id", length = 128)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 128)
    private String gatewayPaymentId;

    @Column(name = "paid_at")
    private Instant paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
