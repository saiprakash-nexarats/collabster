package com.collabsphere_first.services.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateServiceListingRequest {

    @NotBlank(message = "Service title is required")
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
}

