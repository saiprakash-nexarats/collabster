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
public class ServiceBookingDto {
    private UUID id;
    private String orderId;
    private UUID serviceId;
    private Long customerId;
    private Long providerId;
    private String provider;
    private String title;
    private String category;
    private String tierName;
    private BigDecimal price;
    private Long amountCents;
    private String currency;
    private String deliveryMode;
    private String status;
    private String orderStatus;
    private Instant startsAt;
    private String serviceAddress;
    private String notes;
    private String milestones;
    private UUID paymentOrderId;
    private Instant createdAt;
    private Instant updatedAt;
}

