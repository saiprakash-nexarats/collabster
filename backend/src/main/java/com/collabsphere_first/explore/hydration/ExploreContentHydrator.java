package com.collabsphere_first.explore.hydration;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.feed.dto.EngagementDto;
import com.collabsphere_first.feed.dto.FeedItemResponse;
import com.collabsphere_first.feed.dto.InteractionStateDto;
import com.collabsphere_first.feed.hydration.FeedContentHydrator;
import com.collabsphere_first.feed.hydration.HydratedFeedItem;
import com.collabsphere_first.interaction.dto.InteractionSummary;
import com.collabsphere_first.interaction.service.InteractionService;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExploreContentHydrator {

    private final FeedContentHydrator feedContentHydrator;
    private final InteractionService interactionService;

    /**
     * Bulk hydrates ContentRefs with authoritative security/privacy checks
     * and enriches them with viewer interaction states in a single bulk query.
     */
    public List<FeedItemResponse> hydrateAndEnrich(List<ContentRef> refs, User viewer) {
        if (refs == null || refs.isEmpty() || viewer == null) {
            return Collections.emptyList();
        }

        // 1. Authoritative hydration (privacy, block, mute, followers-only, community membership checks)
        Map<ContentRef, HydratedFeedItem> hydratedMap = feedContentHydrator.hydrate(refs, viewer);
        if (hydratedMap.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Filter to items that passed hydration while preserving original order
        List<HydratedFeedItem> orderedHydrated = new ArrayList<>();
        List<ContentRef> validRefs = new ArrayList<>();

        for (ContentRef ref : refs) {
            HydratedFeedItem item = hydratedMap.get(ref);
            if (item != null) {
                orderedHydrated.add(item);
                validRefs.add(ref);
            }
        }

        // 3. Bulk interaction enrichment (liked, saved, likeCount) in a single query
        Map<ContentRef, InteractionSummary> summaries = interactionService.getInteractionSummaries(
                viewer.getId(),
                validRefs
        );

        // 4. Assemble final FeedItemResponse list
        List<FeedItemResponse> responses = new ArrayList<>(orderedHydrated.size());
        for (HydratedFeedItem item : orderedHydrated) {
            ContentRef ref = item.contentRef();
            InteractionSummary summary = summaries.getOrDefault(ref, new InteractionSummary(false, false, 0L));

            FeedItemResponse response = new FeedItemResponse(
                    ref,
                    item.contentType(),
                    Collections.emptyList(), // explore has no feed candidate sources
                    item.author(),
                    item.title(),
                    item.body(),
                    item.media(),
                    new InteractionStateDto(summary.liked(), summary.saved()),
                    new EngagementDto(summary.likeCount(), item.commentCount()),
                    item.community(),
                    item.createdAt(),
                    item.updatedAt()
            );
            responses.add(response);
        }

        return responses;
    }
}
