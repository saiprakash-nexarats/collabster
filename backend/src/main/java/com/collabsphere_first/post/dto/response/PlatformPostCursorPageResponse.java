package com.collabsphere_first.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Keyset / cursor-paginated response container for Platform Posts.
 * Avoids inefficient SQL OFFSET pagination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPostCursorPageResponse {

    @Builder.Default
    private List<PlatformPostResponse> items = new ArrayList<>();

    private String nextCursor;

    private boolean hasMore;

    private int limit;
}
