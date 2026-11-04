package com.collabsphere_first.post.dto.response;

import com.collabsphere_first.post.entity.PostMediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPostMediaResponse {

    private UUID id;
    private PostMediaType mediaType;
    private String storageKey;
    private String mimeType;
    private int displayOrder;
    private Integer width;
    private Integer height;
    private Long durationMs;
    private Instant createdAt;
}
