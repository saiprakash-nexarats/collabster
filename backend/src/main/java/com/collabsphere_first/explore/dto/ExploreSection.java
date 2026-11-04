package com.collabsphere_first.explore.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExploreSection<T>(
        List<T> items,
        boolean hasMore,
        String seeAllUrl
) {
    public static <T> ExploreSection<T> of(List<T> items, boolean hasMore, String seeAllUrl) {
        return new ExploreSection<>(items != null ? items : List.of(), hasMore, seeAllUrl);
    }

    public static <T> ExploreSection<T> of(List<T> items, boolean hasMore) {
        return new ExploreSection<>(items != null ? items : List.of(), hasMore, null);
    }
}
