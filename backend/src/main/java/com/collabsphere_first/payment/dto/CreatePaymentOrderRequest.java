package com.collabsphere_first.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentOrderRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be strictly positive")
    private Long amountCents;

    @Builder.Default
    private String currency = "INR";

    private String description;
    private UUID bookingId;
    private String idempotencyKey;
}
