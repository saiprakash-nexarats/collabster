package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.ModerationStatus;
import com.collabsphere_first.community.enums.PostType;
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
public class PostDto {
    private UUID id;
    private UUID communityId;
    private Long authorId;
    private PostType type;
    private String title;
    private String body;
    private boolean pinned;
    private boolean locked;
    private int commentCount;
    private ModerationStatus moderationStatus;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private long likeCount = 0L;

    @Builder.Default
    private boolean liked = false;

    @Builder.Default
    private boolean saved = false;
}
