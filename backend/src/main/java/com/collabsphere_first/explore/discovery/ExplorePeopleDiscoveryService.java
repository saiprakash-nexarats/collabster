package com.collabsphere_first.explore.discovery;

import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.cursor.ExploreKeysetCursor;
import com.collabsphere_first.explore.dto.ExplorePeopleDiscoveryResponse;
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
public class ExplorePeopleDiscoveryService {

    private final ExploreProperties exploreProperties;
    private final ExploreUserRepository exploreUserRepository;
    private final NetworkAccessService networkAccessService;
    private final ExploreCursorCodec cursorCodec;

    @Transactional(readOnly = true)
    public List<ExplorePersonSummary> getPeoplePreview(User viewer, int limit) {
        ExplorePeopleDiscoveryResponse page = getPeopleDiscovery(viewer, null, limit);
        return page.items();
    }

    @Transactional(readOnly = true)
    public ExplorePeopleDiscoveryResponse getPeopleDiscovery(User viewer, String cursorString, int limit) {
        int safeLimit = sanitizeLimit(limit);
        ExploreKeysetCursor cursor = cursorCodec.decodeKeysetCursor(cursorString);
        int currentPosition = cursor != null ? cursor.position() : 0;

        // Build exclusion set: viewer, blocked/blocking users, muted users
        Set<Long> excludedUserIds = new HashSet<>(networkAccessService.findBlockedOrBlockingUserIds(viewer.getId()));
        excludedUserIds.addAll(networkAccessService.findMutedUserIds(viewer.getId()));

        if (exploreProperties.getDiscovery().isExcludeFollowing()) {
            excludedUserIds.addAll(networkAccessService.findFollowingUserIds(viewer.getId(), 500));
        }
        if (exploreProperties.getDiscovery().isExcludeConnected()) {
            excludedUserIds.addAll(networkAccessService.findConnectedUserIds(viewer.getId(), 500));
        }

        int candidateLimit = exploreProperties.getDiscovery().getCandidateLimit();
        List<User> rawCandidates = exploreUserRepository.discoverPeople(viewer.getId(), excludedUserIds, candidateLimit);

        int totalFound = rawCandidates.size();
        int from = Math.min(currentPosition, totalFound);
        int to = Math.min(from + safeLimit, totalFound);
        List<User> pageSlice = rawCandidates.subList(from, to);

        if (pageSlice.isEmpty()) {
            return new ExplorePeopleDiscoveryResponse(Collections.emptyList(), null, false);
        }

        // Batch relationship state lookups to eliminate N+1 queries
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

        return new ExplorePeopleDiscoveryResponse(summaries, nextCursor, hasMore);
    }

    private int sanitizeLimit(int limit) {
        int defaultSize = exploreProperties.getHome().getPeoplePreviewLimit();
        int maxSize = exploreProperties.getDiscovery().getMaxPageSize();
        if (limit <= 0) {
            return defaultSize;
        }
        return Math.min(limit, maxSize);
    }
}
