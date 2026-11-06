package com.collabsphere_first.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDto {
    private UUID id;
    private Long ownerId;
    private String name;
    private String description;
    private String industry;
    private String stage;
    private String state;
    private Instant createdAt;
    private Instant updatedAt;
}
