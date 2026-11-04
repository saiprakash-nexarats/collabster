package com.collabsphere_first.post.dto.response;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Public response DTO for a Platform Post.
 * Never exposes internal security credentials, passwords, or raw JPA proxy references.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPostResponse {

    private ContentRef contentRef;
    private UUID id;
    private AuthorSummaryResponse author;
    private String body;
    private PostVisibility visibility;
    private PostStatus status;
    @Builder.Default
    private List<PlatformPostMediaResponse> media = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
    private boolean edited;
    private Long version;
}
