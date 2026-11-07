package com.collabsphere_first.payment.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReconciliationDto {
    private UUID orderId;
    private String orderNumber;
    private String orderStatus;
    private String gatewayStatus;
    private Long amountCents;
    private String currency;
    private Boolean reconciled;
    private Boolean match;
    private Instant reconciledAt;
}

