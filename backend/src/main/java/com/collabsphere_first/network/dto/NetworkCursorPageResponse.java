package com.collabsphere_first.network.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkCursorPageResponse<T> {
    private List<T> items;
    private String nextCursor;
    private boolean hasMore;
    private boolean hasNext;
    private int limit;

    public static class NetworkCursorPageResponseBuilder<T> {
        public NetworkCursorPageResponseBuilder<T> hasNext(boolean hasNext) {
            this.hasNext = hasNext;
            this.hasMore = hasNext;
            return this;
        }

        public NetworkCursorPageResponseBuilder<T> hasMore(boolean hasMore) {
            this.hasMore = hasMore;
            this.hasNext = hasMore;
            return this;
        }
    }
}
