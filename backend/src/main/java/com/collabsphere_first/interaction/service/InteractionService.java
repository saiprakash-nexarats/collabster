package com.collabsphere_first.interaction.service;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.interaction.dto.*;
import com.collabsphere_first.user.entity.User;

import java.util.List;

public interface InteractionService {

    LikeResponse likeContent(User user, ContentRef contentRef);

    LikeResponse unlikeContent(User user, ContentRef contentRef);

    SaveResponse saveContent(User user, ContentRef contentRef);

    SaveResponse unsaveContent(User user, ContentRef contentRef);

    SavedItemsCursorPageResponse getSavedContent(User user, String cursor, int limit);

    void recordShare(User user, ContentRef contentRef);

    void recordEvent(User user, InteractionEventRequest request);

    int recordBatchEvents(User user, List<InteractionEventRequest> requests);

    boolean isLiked(Long userId, ContentRef contentRef);

    boolean isSaved(Long userId, ContentRef contentRef);

    long getLikeCount(ContentRef contentRef);

    long getSaveCount(ContentRef contentRef);

    java.util.Set<ContentRef> findSuppressedContentRefs(Long userId);

    java.util.Map<ContentRef, InteractionSummary> getInteractionSummaries(Long userId, java.util.Collection<ContentRef> contentRefs);
}
