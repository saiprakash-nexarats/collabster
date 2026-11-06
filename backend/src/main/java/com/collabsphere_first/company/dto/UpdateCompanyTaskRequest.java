package com.collabsphere_first.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompanyTaskRequest {
    private String state;
    private String title;
    private String description;
    private Long assigneeId;
}
