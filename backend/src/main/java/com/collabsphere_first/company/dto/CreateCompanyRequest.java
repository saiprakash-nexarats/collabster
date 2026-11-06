package com.collabsphere_first.company.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompanyRequest {
    @NotBlank(message = "Company name is required")
    private String name;
    private String description;
    private String industry;
    private String stage;
}
