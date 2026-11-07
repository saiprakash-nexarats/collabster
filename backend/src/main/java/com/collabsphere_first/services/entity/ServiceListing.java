package com.collabsphere_first.services.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_listings", indexes = {
        @Index(name = "idx_service_listings_provider", columnList = "provider_id"),
        @Index(name = "idx_service_listings_category", columnList = "category"),
        @Index(name = "idx_service_listings_state", columnList = "state"),
        @Index(name = "idx_service_listings_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false, length = 64)
    @Builder.Default
    private String category = "software";

    @Column(name = "service_area")
    @Builder.Default
    private String serviceArea = "Remote (Global)";

    @Column(name = "service_radius_km")
    @Builder.Default
    private Integer serviceRadiusKm = 15;

    @Column(name = "starting_price", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal startingPrice = BigDecimal.ZERO;

    @Column(name = "price_cents", nullable = false)
    @Builder.Default
    private Long priceCents = 0L;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "duration", length = 64)
    @Builder.Default
    private String duration = "3 days";

    @Column(name = "pricing_unit", length = 64)
    @Builder.Default
    private String pricingUnit = "Service visit";

    @Column(name = "delivery_mode", length = 64)
    @Builder.Default
    private String deliveryMode = "Remote (Global)";

    @Column(name = "pricing_type", length = 64)
    @Builder.Default
    private String pricingType = "Fixed Package";

    @Column(name = "highlights", columnDefinition = "TEXT")
    private String highlights;

    @Column(name = "state", nullable = false, length = 32)
    @Builder.Default
    private String state = "active";

    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

