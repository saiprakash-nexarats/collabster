package com.collabsphere_first.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedItemsCursorPageResponse {
    private List<SavedItemResponse> items;
    private String nextCursor;
    private boolean hasMore;
    private boolean hasNext;
    private int limit;

    public static class SavedItemsCursorPageResponseBuilder {
        public SavedItemsCursorPageResponseBuilder hasNext(boolean hasNext) {
            this.hasNext = hasNext;
            this.hasMore = hasNext;
            return this;
        }

        public SavedItemsCursorPageResponseBuilder hasMore(boolean hasMore) {
            this.hasMore = hasMore;
            this.hasNext = hasMore;
            return this;
        }
    }
}
