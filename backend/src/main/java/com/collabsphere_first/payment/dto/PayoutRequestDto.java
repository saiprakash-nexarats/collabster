package com.collabsphere_first.payment.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutRequestDto {
    private UUID id;
    private Long providerId;
    private Long amountCents;
    private String currency;
    private String method;
    private String destinationReference;
    private String status;
    private String gatewayPayoutId;
    private String notes;
    private Instant processedAt;
    @Builder.Default
    private Boolean remote = true;
    private Instant createdAt;
    private Instant updatedAt;
}
