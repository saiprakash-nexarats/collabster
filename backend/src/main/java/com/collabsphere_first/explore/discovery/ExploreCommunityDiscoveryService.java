package com.collabsphere_first.explore.discovery;

import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.cursor.ExploreKeysetCursor;
import com.collabsphere_first.explore.dto.ExploreCommunityDiscoveryResponse;
import com.collabsphere_first.explore.dto.ExploreCommunitySummary;
import com.collabsphere_first.explore.repository.ExploreCommunityRepository;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExploreCommunityDiscoveryService {

    private final ExploreProperties exploreProperties;
    private final ExploreCommunityRepository communityRepository;
    private final ExploreCursorCodec cursorCodec;

    @Transactional(readOnly = true)
    public List<ExploreCommunitySummary> getCommunityPreview(User viewer, int limit) {
        ExploreCommunityDiscoveryResponse page = getCommunityDiscovery(viewer, null, limit);
        return page.items();
    }

    @Transactional(readOnly = true)
    public ExploreCommunityDiscoveryResponse getCommunityDiscovery(User viewer, String cursorString, int limit) {
        int safeLimit = sanitizeLimit(limit);
        ExploreKeysetCursor cursor = cursorCodec.decodeKeysetCursor(cursorString);
        int currentPosition = cursor != null ? cursor.position() : 0;

        // Find community IDs viewer has already joined to optionally prioritize unjoined communities
        Set<UUID> allJoinedIds = communityRepository.findAllActiveCommunityIdsForUser(viewer.getId());

        int fetchLimit = Math.max(safeLimit * 3, 50);
        List<Community> rawCommunities = communityRepository.discoverCommunities(
                allJoinedIds,
                fetchLimit
        );

        // Fallback: If unjoined communities are fewer than safeLimit, include popular communities overall
        if (rawCommunities.size() < safeLimit) {
            List<Community> fallback = communityRepository.discoverCommunities(Collections.emptySet(), fetchLimit);
            Set<UUID> seen = new HashSet<>();
            List<Community> combined = new ArrayList<>();
            for (Community c : rawCommunities) {
                if (seen.add(c.getId())) {
                    combined.add(c);
                }
            }
            for (Community c : fallback) {
                if (seen.add(c.getId())) {
                    combined.add(c);
                }
            }
            rawCommunities = combined;
        }

        int totalFound = rawCommunities.size();
        int from = Math.min(currentPosition, totalFound);
        int to = Math.min(from + safeLimit, totalFound);
        List<Community> pageSlice = rawCommunities.subList(from, to);

        if (pageSlice.isEmpty()) {
            return new ExploreCommunityDiscoveryResponse(Collections.emptyList(), null, false);
        }

        // Batch membership enrichment
        List<UUID> targetIds = pageSlice.stream().map(Community::getId).toList();
        Set<UUID> joinedIds = communityRepository.findJoinedCommunityIds(viewer.getId(), targetIds);

        List<ExploreCommunitySummary> summaries = new ArrayList<>(pageSlice.size());
        for (Community c : pageSlice) {
            boolean joined = joinedIds.contains(c.getId());
            boolean canJoin = c.getVisibility() != Visibility.INVITE_ONLY || joined;

            summaries.add(new ExploreCommunitySummary(
                    c.getId(),
                    c.getName(),
                    c.getSlug(),
                    c.getDescription(),
                    c.getCoverUrl(),
                    c.getMemberCount(),
                    c.getCategory() != null ? c.getCategory().name() : null,
                    c.getTags() != null ? new ArrayList<>(c.getTags()) : Collections.emptyList(),
                    joined,
                    canJoin
            ));
        }

        boolean hasMore = to < totalFound;
        String nextCursor = null;
        if (hasMore) {
            ExploreKeysetCursor next = new ExploreKeysetCursor(
                    null,
                    null,
                    null,
                    null,
                    null,
                    to
            );
            nextCursor = cursorCodec.encodeKeysetCursor(next);
        }

        return new ExploreCommunityDiscoveryResponse(summaries, nextCursor, hasMore);
    }

    private int sanitizeLimit(int limit) {
        int defaultSize = exploreProperties.getHome().getCommunityPreviewLimit();
        int maxSize = exploreProperties.getDiscovery().getMaxPageSize();
        if (limit <= 0) {
            return defaultSize;
        }
        return Math.min(limit, maxSize);
    }
}
