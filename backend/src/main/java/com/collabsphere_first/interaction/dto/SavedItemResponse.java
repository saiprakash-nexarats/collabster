package com.collabsphere_first.interaction.dto;

import com.collabsphere_first.common.content.ContentRef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedItemResponse {
    private ContentRef contentRef;
    private Instant savedAt;
    private String title;
    private String summary;
    private Long authorId;
}
