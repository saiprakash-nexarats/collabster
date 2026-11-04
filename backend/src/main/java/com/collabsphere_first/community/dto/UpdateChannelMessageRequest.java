package com.collabsphere_first.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateChannelMessageRequest {
    @NotBlank @Size(max = 10000)
    private String body;
}
