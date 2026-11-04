package com.collabsphere_first.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommunityRuleRequest {
    @NotBlank @Size(max = 160) private String title;
    @NotBlank @Size(max = 5000) private String body;
    private Integer position;
}
