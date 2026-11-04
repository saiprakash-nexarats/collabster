package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.LifecycleState;
import com.collabsphere_first.community.enums.CommunityCategory;
import com.collabsphere_first.community.enums.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCommunityRequest {
    private String name;
    private String description;
    private String logoUrl;
    private String coverUrl;
    private Visibility visibility;
    private LifecycleState lifecycle;
    private CommunityCategory category;
    private List<String> tags;
    private Boolean requiresModeration;
}
