package com.collabsphere_first.interaction.resolver;

import com.collabsphere_first.common.content.ContentRef;

public record ResolvedContent(
        ContentRef contentRef,
        Long authorId,
        boolean accessible,
        String title,
        String summary
) {
    public static ResolvedContent inaccessible(ContentRef contentRef) {
        return new ResolvedContent(contentRef, null, false, null, null);
    }

    public static ResolvedContent of(ContentRef contentRef, Long authorId, String title, String summary) {
        return new ResolvedContent(contentRef, authorId, true, title, summary);
    }
}
