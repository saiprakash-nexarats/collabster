package com.collabsphere_first.services.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceListingDto {
    private UUID id;
    private Long providerId;
    private String provider;
    private String providerRole;
    private String title;
    private String description;
    private String category;
    private String serviceArea;
    private Integer serviceRadiusKm;
    private BigDecimal startingPrice;
    private BigDecimal price;
    private Long priceCents;
    private String currency;
    private String duration;
    private String pricingUnit;
    private String deliveryMode;
    private String pricingType;
    private String highlights;
    private String state;
    private BigDecimal averageRating;
    private Integer reviewCount;
    @Builder.Default
    private Boolean remote = true;
    private Instant createdAt;
    private Instant updatedAt;
}
