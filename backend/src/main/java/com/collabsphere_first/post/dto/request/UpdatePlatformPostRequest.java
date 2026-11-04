package com.collabsphere_first.post.dto.request;

import com.collabsphere_first.post.constant.PostConstants;
import com.collabsphere_first.post.entity.PostVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlatformPostRequest {

    @Size(max = PostConstants.MAX_BODY_LENGTH, message = "Post body must not exceed {max} characters")
    private String body;

    private PostVisibility visibility;

    @Valid
    @Size(max = PostConstants.MAX_MEDIA_COUNT, message = "Cannot exceed {max} media attachments per post")
    private List<PlatformPostMediaRequest> media;
}
