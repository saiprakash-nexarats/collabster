package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.ModerationStatus;
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
public class CommentDto {
    private UUID id;
    private UUID postId;
    private UUID communityId;
    private Long authorId;
    private String body;
    private UUID parentCommentId;
    private ModerationStatus moderationStatus;
    private Instant createdAt;
}
