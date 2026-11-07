package com.collabsphere_first.mentor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorProfileDto {
    private UUID id;
    private Long profileId;
    private String headline;
    private String bio;
    private BigDecimal hourlyRate;
    private String currency;
    private String timezone;
    private String skills;
    private String state;
}
