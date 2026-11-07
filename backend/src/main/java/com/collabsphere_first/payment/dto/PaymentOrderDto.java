package com.collabsphere_first.payment.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrderDto {
    private UUID id;
    private Long userId;
    private String orderNumber;
    private Long amountCents;
    private String currency;
    private String status;
    private String description;
    private UUID bookingId;
    private String gatewayProvider;
    private String gatewayOrderId;
    private String gatewayPaymentId;
    private Instant paidAt;
    @Builder.Default
    private Boolean remote = true;
    private Instant createdAt;
    private Instant updatedAt;
}
