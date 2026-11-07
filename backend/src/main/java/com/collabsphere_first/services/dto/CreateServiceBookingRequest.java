package com.collabsphere_first.services.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateServiceBookingRequest {
    private String tierName;
    private BigDecimal price;
    private Long amountCents;
    private String currency;
    private String deliveryMode;
    private Instant startsAt;
    private String serviceAddress;
    private String notes;
    private String milestones;
}
