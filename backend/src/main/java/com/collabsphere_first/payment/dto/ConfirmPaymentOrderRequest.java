package com.collabsphere_first.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmPaymentOrderRequest {
    private String gatewayPaymentId;
}
