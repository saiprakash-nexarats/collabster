package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.CommunityType;
import com.collabsphere_first.community.enums.CommunityCategory;
import com.collabsphere_first.community.enums.LifecycleState;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.enums.CommunityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommunityDto {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String logoUrl;
    private String coverUrl;
    private CommunityType type;
    private CommunityCategory category;
    private LifecycleState lifecycle;
    private CommunityStatus status;
    private Visibility visibility;
    private Long ownerId;
    private boolean requiresModeration;
    private List<String> tags;
    private int memberCount;
    private BigDecimal activityScore;
    private UUID linkedEventId;
    private UUID linkedCourseId;
    private Instant createdAt;
    private Instant archivedAt;
}
