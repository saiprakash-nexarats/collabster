package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.CommunityType;
import com.collabsphere_first.community.enums.CommunityCategory;
import com.collabsphere_first.community.enums.LifecycleState;
import com.collabsphere_first.community.enums.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateCommunityRequest {

    @NotBlank(message = "Community name is required")
    private String name;

    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens")
    private String slug;

    private String description;
    private String logoUrl;
    private String coverUrl;

    @NotNull(message = "Type is required")
    private CommunityType type;

    @NotNull(message = "Category is required")
    @Builder.Default
    private CommunityCategory category = CommunityCategory.OTHER;

    @Builder.Default
    private LifecycleState lifecycle = LifecycleState.PERMANENT;

    @NotNull(message = "Visibility is required")
    private Visibility visibility;

    private List<String> tags;
    private Boolean requiresModeration;
    private UUID linkedEventId;
    private UUID linkedCourseId;
}
