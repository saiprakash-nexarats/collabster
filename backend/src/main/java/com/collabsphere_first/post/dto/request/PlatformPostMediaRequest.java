package com.collabsphere_first.post.dto.request;

import com.collabsphere_first.post.constant.PostConstants;
import com.collabsphere_first.post.entity.PostMediaType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPostMediaRequest {

    @NotNull(message = "Media type is required")
    private PostMediaType mediaType;

    @NotBlank(message = "Storage key is required")
    @Size(max = PostConstants.MAX_STORAGE_KEY_LENGTH, message = "Storage key must not exceed {max} characters")
    private String storageKey;

    @NotBlank(message = "MIME type is required")
    @Size(max = PostConstants.MAX_MIME_TYPE_LENGTH, message = "MIME type must not exceed {max} characters")
    private String mimeType;

    @Min(value = 0, message = "Display order must be non-negative")
    private int displayOrder;

    private Integer width;

    private Integer height;

    private Long durationMs;
}
