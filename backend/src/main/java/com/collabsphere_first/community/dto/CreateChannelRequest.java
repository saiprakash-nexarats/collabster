package com.collabsphere_first.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateChannelRequest {
    @NotBlank @Size(max = 60)
    private String name;
    @Size(max = 300)
    private String description;
    private boolean readOnly;
}
