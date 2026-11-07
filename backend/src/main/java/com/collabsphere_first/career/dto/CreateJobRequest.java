package com.collabsphere_first.career.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @Builder.Default
    private String location = "Flexible";

    @Builder.Default
    private String workMode = "Flexible";

    @Builder.Default
    private String employmentType = "Full-time";

    private String salaryRange;
}
