package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateResourceRequest {

    @NotNull(message = "Resource type is required")
    private ResourceType type;

    @NotBlank(message = "Title is required")
    private String title;

    private String storageKeyOrUrl;
    private String body;
    private String category;
}
