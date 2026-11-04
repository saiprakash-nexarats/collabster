package com.collabsphere_first.community.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCommunityRuleRequest {
    @Size(max = 160) private String title;
    @Size(max = 5000) private String body;
    private Integer position;
}
