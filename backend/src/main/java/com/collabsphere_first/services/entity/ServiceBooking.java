package com.collabsphere_first.services.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_bookings", indexes = {
        @Index(name = "idx_service_bookings_service", columnList = "service_id"),
        @Index(name = "idx_service_bookings_customer", columnList = "customer_id"),
        @Index(name = "idx_service_bookings_provider", columnList = "provider_id"),
        @Index(name = "idx_service_bookings_status", columnList = "status"),
        @Index(name = "idx_service_bookings_starts", columnList = "service_id, starts_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "order_id", length = 64)
    private String orderId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "tier_name", length = 128)
    @Builder.Default
    private String tierName = "Standard Package";

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "amount_cents", nullable = false)
    @Builder.Default
    private Long amountCents = 0L;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "delivery_mode", length = 64)
    @Builder.Default
    private String deliveryMode = "Remote";

    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private String status = "requested";

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "service_address")
    private String serviceAddress;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "milestones", columnDefinition = "TEXT")
    private String milestones;

    @Column(name = "payment_order_id")
    private UUID paymentOrderId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

