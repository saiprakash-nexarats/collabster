package com.collabsphere_first.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePayoutRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be strictly positive")
    private Long amountCents;

    @Builder.Default
    private String currency = "INR";

    @Builder.Default
    private String method = "bank_account";

    @NotBlank(message = "Destination reference is required")
    private String destinationReference;

    private String idempotencyKey;

    private String notes;
}

