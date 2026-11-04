package com.collabsphere_first.interaction.dto;

import com.collabsphere_first.common.content.ContentRef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {
    private ContentRef contentRef;
    private boolean liked;
    private long likeCount;
}
