package com.collabsphere_first.post.dto.request;

import com.collabsphere_first.post.constant.PostConstants;
import com.collabsphere_first.post.entity.PostVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlatformPostRequest {

    @Size(max = PostConstants.MAX_BODY_LENGTH, message = "Post body must not exceed {max} characters")
    private String body;

    @Builder.Default
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Valid
    @Size(max = PostConstants.MAX_MEDIA_COUNT, message = "Cannot exceed {max} media attachments per post")
    @Builder.Default
    private List<PlatformPostMediaRequest> media = new ArrayList<>();

    /**
     * Verifies that the post has meaningful content (non-blank body or at least one media item).
     */
    public boolean hasContent() {
        boolean hasBody = body != null && !body.trim().isEmpty();
        boolean hasMedia = media != null && !media.isEmpty();
        return hasBody || hasMedia;
    }
}
