package com.collabsphere_first.explore.search;

import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.cursor.ExploreSearchCursor;
import com.collabsphere_first.explore.dto.ExplorePeopleSearchResponse;
import com.collabsphere_first.explore.dto.ExplorePersonSummary;
import com.collabsphere_first.explore.repository.ExploreUserRepository;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PeopleSearchService {

    private final ExploreProperties exploreProperties;
    private final ExploreUserRepository exploreUserRepository;
    private final NetworkAccessService networkAccessService;
    private final ExploreCursorCodec cursorCodec;
    private final ExploreQueryValidator queryValidator;

    @Transactional(readOnly = true)
    public List<ExplorePersonSummary> searchPreview(String rawQuery, User viewer, int limit) {
        ExplorePeopleSearchResponse page = searchPeople(rawQuery, viewer, null, limit);
        return page.items();
    }

    @Transactional(readOnly = true)
    public ExplorePeopleSearchResponse searchPeople(String rawQuery, User viewer, String cursorString, int limit) {
        String normalizedQuery = queryValidator.validateAndNormalize(rawQuery);
        int safeLimit = sanitizeLimit(limit);
        ExploreSearchCursor cursor = cursorCodec.decodeSearchCursor(cursorString, normalizedQuery);
        int currentPosition = cursor != null ? cursor.position() : 0;

        // Block filtering: respect bidirectional blocks
        Set<Long> blockedOrBlockingUserIds = networkAccessService.findBlockedOrBlockingUserIds(viewer.getId());

        int fetchLimit = Math.max(safeLimit * 3, 50);
        List<User> rawUsers = exploreUserRepository.searchUsers(
                normalizedQuery,
                viewer.getId(),
                blockedOrBlockingUserIds,
                fetchLimit
        );

        int totalFound = rawUsers.size();
        int from = Math.min(currentPosition, totalFound);
        int to = Math.min(from + safeLimit, totalFound);
        List<User> pageSlice = rawUsers.subList(from, to);

        if (pageSlice.isEmpty()) {
            return new ExplorePeopleSearchResponse(Collections.emptyList(), null, false);
        }

        // Batch social relationship lookups to eliminate N+1 queries
        List<Long> targetUserIds = pageSlice.stream().map(User::getId).toList();
        Set<Long> followingIds = exploreUserRepository.findFollowingUserIds(viewer.getId(), targetUserIds);
        Map<Long, ExploreUserRepository.ConnectionStatusSummary> connectionStatuses =
                exploreUserRepository.findConnectionStatuses(viewer.getId(), targetUserIds);

        List<ExplorePersonSummary> summaries = new ArrayList<>(pageSlice.size());
        for (User u : pageSlice) {
            boolean following = followingIds.contains(u.getId());
            ExploreUserRepository.ConnectionStatusSummary connStatus = connectionStatuses.get(u.getId());
            boolean connected = connStatus != null && connStatus.connected();
            boolean connectionPending = connStatus != null && connStatus.pending();

            summaries.add(new ExplorePersonSummary(
                    u.getId(),
                    u.getUsername(),
                    u.getFullName(),
                    u.getProfilePicture(),
                    u.getBio(),
                    following,
                    connected,
                    connectionPending
            ));
        }

        boolean hasMore = to < totalFound;
        String nextCursor = null;
        if (hasMore) {
            ExploreSearchCursor next = new ExploreSearchCursor(
                    ExploreCursorCodec.computeQueryHash(normalizedQuery),
                    0.0,
                    null,
                    null,
                    null,
                    to
            );
            nextCursor = cursorCodec.encodeSearchCursor(next);
        }

        return new ExplorePeopleSearchResponse(summaries, nextCursor, hasMore);
    }

    private int sanitizeLimit(int limit) {
        int defaultSize = exploreProperties.getSearch().getDefaultPageSize();
        int maxSize = exploreProperties.getSearch().getMaxPageSize();
        if (limit <= 0) {
            return defaultSize;
        }
        return Math.min(limit, maxSize);
    }
}
