package com.collabsphere_first.feed.service.impl;

import com.collabsphere_first.feed.service.FeedService;
import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.feed.candidate.*;
import com.collabsphere_first.feed.config.FeedProperties;
import com.collabsphere_first.feed.cursor.FeedCursor;
import com.collabsphere_first.feed.cursor.FeedCursorCodec;
import com.collabsphere_first.feed.dto.*;
import com.collabsphere_first.feed.exception.FeedSessionAccessDeniedException;
import com.collabsphere_first.feed.exception.FeedSessionExpiredException;
import com.collabsphere_first.feed.hydration.FeedContentHydrator;
import com.collabsphere_first.feed.hydration.HydratedFeedItem;
import com.collabsphere_first.feed.ranking.FeedRankingContext;
import com.collabsphere_first.feed.ranking.FeedRankingStrategy;
import com.collabsphere_first.feed.session.FeedSession;
import com.collabsphere_first.feed.session.FeedSessionStore;
import com.collabsphere_first.interaction.dto.InteractionSummary;
import com.collabsphere_first.interaction.service.InteractionService;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final List<FeedCandidateProvider> candidateProviders;
    private final FeedRankingStrategy rankingStrategy;
    private final FeedSessionStore sessionStore;
    private final FeedCursorCodec cursorCodec;
    private final FeedContentHydrator contentHydrator;
    private final NetworkAccessService networkAccessService;
    private final InteractionService interactionService;
    private final FeedProperties feedProperties;

    @Override
    public FeedPageResponse getFeed(User currentUser, String cursor, int limit) {
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        int safeLimit = sanitizeLimit(limit);
        FeedSession session;
        int currentPosition;

        if (cursor == null || cursor.isBlank()) {
            // First page or pull-to-refresh: create a brand new feed session
            session = generateNewSession(currentUser);
            currentPosition = 0;
        } else {
            // Continuation: load and validate existing session
            FeedCursor decoded = cursorCodec.decode(cursor);
            session = sessionStore.getSession(decoded.sessionId())
                    .orElseThrow(() -> new FeedSessionExpiredException("Feed session expired or not found. Please refresh the feed."));

            // Verify session ownership server-side
            if (!currentUser.getId().equals(session.userId())) {
                log.warn("User {} attempted to access feed session belonging to user {}", currentUser.getId(), session.userId());
                throw new FeedSessionAccessDeniedException("You do not have permission to access this feed session.");
            }
            currentPosition = decoded.position();
        }

        List<ContentRef> orderedRefs = session.orderedContentRefs();
        List<HydratedFeedItem> validHydratedItems = new ArrayList<>();
        int scanPosition = currentPosition;

        // Iterate through session references, skipping any stale or deleted items
        while (validHydratedItems.size() < safeLimit && scanPosition < orderedRefs.size()) {
            int batchWindow = Math.min(safeLimit * 2, orderedRefs.size() - scanPosition);
            List<ContentRef> candidateSlice = orderedRefs.subList(scanPosition, scanPosition + batchWindow);

            Map<ContentRef, HydratedFeedItem> hydratedSlice = contentHydrator.hydrate(candidateSlice, currentUser);

            for (ContentRef ref : candidateSlice) {
                scanPosition++;
                HydratedFeedItem hydrated = hydratedSlice.get(ref);
                if (hydrated != null) {
                    validHydratedItems.add(hydrated);
                    if (validHydratedItems.size() == safeLimit) {
                        break;
                    }
                } else {
                    log.debug("Skipping inaccessible or stale feed content {} for user {}", ref, currentUser.getId());
                }
            }
        }

        // Enrich with interaction state in bulk (liked, saved, likeCount)
        List<ContentRef> validRefs = validHydratedItems.stream()
                .map(HydratedFeedItem::contentRef)
                .collect(Collectors.toList());

        Map<ContentRef, InteractionSummary> summaries = interactionService.getInteractionSummaries(
                currentUser.getId(),
                validRefs
        );

        List<FeedItemResponse> feedItemResponses = new ArrayList<>(validHydratedItems.size());
        for (HydratedFeedItem item : validHydratedItems) {
            ContentRef ref = item.contentRef();
            InteractionSummary summary = summaries.getOrDefault(ref, new InteractionSummary(false, false, 0L));
            Set<FeedSourceType> sources = session.getSources(ref);

            FeedItemResponse response = new FeedItemResponse(
                    ref,
                    item.contentType(),
                    new ArrayList<>(sources),
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
            feedItemResponses.add(response);
        }

        boolean hasMore = scanPosition < orderedRefs.size();
        String nextCursor = hasMore ? cursorCodec.encode(session.sessionId(), scanPosition) : null;

        return new FeedPageResponse(feedItemResponses, nextCursor, hasMore);
    }

    private FeedSession generateNewSession(User currentUser) {
        Long userId = currentUser.getId();

        // 1. Gather relationship and security bounds via narrow SPI
        Set<Long> followingUserIds = new HashSet<>(networkAccessService.findFollowingUserIds(
                userId,
                feedProperties.getCandidates().getFollowingLimit()
        ));

        Set<Long> connectedUserIds = new HashSet<>(networkAccessService.findConnectedUserIds(
                userId,
                feedProperties.getCandidates().getConnectionLimit()
        ));

        Set<Long> mutedUserIds = networkAccessService.findMutedUserIds(userId);
        Set<Long> blockedUserIds = networkAccessService.findBlockedOrBlockingUserIds(userId);
        Set<ContentRef> suppressedRefs = interactionService.findSuppressedContentRefs(userId);

        FeedCandidateContext context = new FeedCandidateContext(
                currentUser,
                feedProperties,
                followingUserIds,
                connectedUserIds,
                mutedUserIds,
                blockedUserIds
        );

        // 2. Candidate collection from all registered providers
        List<FeedCandidate> rawCandidates = new ArrayList<>();
        PublicFallbackCandidateProvider fallbackProvider = null;

        for (FeedCandidateProvider provider : candidateProviders) {
            if (provider instanceof PublicFallbackCandidateProvider p) {
                fallbackProvider = p;
                continue; // Collect personalized sources first
            }
            try {
                List<FeedCandidate> collected = provider.collect(context);
                if (collected != null) {
                    rawCandidates.addAll(collected);
                }
            } catch (Exception ex) {
                log.error("Provider {} failed candidate collection for user {}: {}", provider.sourceType(), userId, ex.getMessage(), ex);
            }
        }

        // 3. Deduplication by ContentRef while merging sources
        Map<ContentRef, FeedCandidate> candidateMap = new LinkedHashMap<>();
        for (FeedCandidate candidate : rawCandidates) {
            mergeCandidate(candidateMap, candidate);
        }

        // 4. Fallback fill if personalized pool is below threshold
        int minThreshold = feedProperties.getCandidates().getMinCandidateThreshold();
        if (candidateMap.size() < minThreshold && fallbackProvider != null) {
            try {
                List<FeedCandidate> fallbackCandidates = fallbackProvider.collect(context);
                if (fallbackCandidates != null) {
                    for (FeedCandidate candidate : fallbackCandidates) {
                        mergeCandidate(candidateMap, candidate);
                    }
                }
            } catch (Exception ex) {
                log.error("Public fallback provider failed for user {}: {}", userId, ex.getMessage(), ex);
            }
        }

        // 5. Eligibility filtering
        List<FeedCandidate> eligibleCandidates = new ArrayList<>();
        for (FeedCandidate candidate : candidateMap.values()) {
            Long authorId = candidate.authorId();

            // Author block check (either direction)
            if (authorId != null && (blockedUserIds.contains(authorId) || userId.equals(authorId) && blockedUserIds.contains(userId))) {
                continue;
            }

            // Mute check
            if (authorId != null && !userId.equals(authorId) && mutedUserIds.contains(authorId)) {
                continue;
            }

            // HIDE and NOT_INTERESTED suppression
            if (suppressedRefs.contains(candidate.contentRef())) {
                continue;
            }

            eligibleCandidates.add(candidate);
        }

        // Bound to max candidate pool
        int maxPool = feedProperties.getMaxCandidatePool();
        if (eligibleCandidates.size() > maxPool) {
            eligibleCandidates = eligibleCandidates.subList(0, maxPool);
        }

        // 6. Ranking
        List<ContentRef> eligibleRefs = eligibleCandidates.stream()
                .map(FeedCandidate::contentRef)
                .collect(Collectors.toList());

        Map<ContentRef, InteractionSummary> initialSummaries = interactionService.getInteractionSummaries(
                userId,
                eligibleRefs
        );

        Map<ContentRef, Long> likeCounts = new HashMap<>();
        for (Map.Entry<ContentRef, InteractionSummary> entry : initialSummaries.entrySet()) {
            likeCounts.put(entry.getKey(), entry.getValue().likeCount());
        }

        FeedRankingContext rankingContext = new FeedRankingContext(feedProperties, Instant.now(), likeCounts, userId);
        List<FeedCandidate> rankedCandidates = rankingStrategy.rank(eligibleCandidates, rankingContext);

        // 7. Session generation
        String sessionId = UUID.randomUUID().toString();
        List<ContentRef> orderedRefs = rankedCandidates.stream()
                .map(FeedCandidate::contentRef)
                .collect(Collectors.toList());

        Map<ContentRef, Set<FeedSourceType>> sourcesMap = new HashMap<>();
        for (FeedCandidate c : rankedCandidates) {
            sourcesMap.put(c.contentRef(), c.sources());
        }

        FeedSession session = FeedSession.create(
                sessionId,
                userId,
                Instant.now(),
                orderedRefs,
                sourcesMap
        );

        sessionStore.saveSession(session, feedProperties.getSessionTtl());
        log.info("Created new feed session {} for user {} with {} ranked items", sessionId, userId, orderedRefs.size());
        return session;
    }

    private void mergeCandidate(Map<ContentRef, FeedCandidate> candidateMap, FeedCandidate candidate) {
        if (candidate == null || candidate.contentRef() == null) {
            return;
        }
        candidateMap.compute(candidate.contentRef(), (ref, existing) -> {
            if (existing == null) {
                Set<FeedSourceType> sources = new HashSet<>(candidate.sources());
                return new FeedCandidate(ref, candidate.authorId(), candidate.createdAt(), sources);
            } else {
                existing.sources().addAll(candidate.sources());
                return existing;
            }
        });
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return feedProperties.getDefaultPageSize();
        }
        return Math.min(limit, feedProperties.getMaxPageSize());
    }
}
