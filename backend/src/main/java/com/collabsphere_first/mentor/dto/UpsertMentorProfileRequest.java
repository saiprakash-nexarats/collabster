package com.collabsphere_first.mentor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertMentorProfileRequest {
    @NotBlank(message = "Headline must not be blank")
    private String headline;

    private String bio;
    private BigDecimal hourlyRate;
    private String currency;
    private String timezone;
    private String skills;
}
