package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.ModerationStatus;
import com.collabsphere_first.community.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourceDto {
    private UUID id;
    private UUID communityId;
    private Long uploadedBy;
    private ResourceType type;
    private String title;
    private String storageKeyOrUrl;
    private String body;
    private String category;
    private int version;
    private ModerationStatus moderationStatus;
    private Instant createdAt;
}
