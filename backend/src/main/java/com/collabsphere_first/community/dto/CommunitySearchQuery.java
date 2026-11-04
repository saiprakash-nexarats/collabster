package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.CommunityType;
import com.collabsphere_first.community.enums.CommunityCategory;
import com.collabsphere_first.community.enums.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommunitySearchQuery {
    private String query;
    private CommunityType type;
    private CommunityCategory category;
    private Visibility visibility;
    private String tag;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
