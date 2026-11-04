package com.collabsphere_first.recommendation.strategy;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.feed.candidate.FeedCandidate;
import com.collabsphere_first.feed.ranking.BaselineFeedRankingStrategy;
import com.collabsphere_first.feed.ranking.FeedRankingContext;
import com.collabsphere_first.feed.ranking.FeedRankingStrategy;
import com.collabsphere_first.recommendation.client.RecommendationClient;
import com.collabsphere_first.recommendation.config.RecommendationProperties;
import com.collabsphere_first.recommendation.dto.RankedItemResponse;
import com.collabsphere_first.recommendation.dto.RecommendationRankRequest;
import com.collabsphere_first.recommendation.dto.RecommendationRankResponse;
import com.collabsphere_first.recommendation.exception.RecommendationException;
import com.collabsphere_first.recommendation.feature.RecommendationFeatureAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Production ranking strategy integrating the Python recommendation service.
 * Operates strictly on pre-filtered eligible candidates, validates response integrity,
 * rejects injected or unsubmitted candidates, and falls back gracefully to BaselineFeedRankingStrategy.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class RecommendationFeedRankingStrategy implements FeedRankingStrategy {

    private final RecommendationClient recommendationClient;
    private final BaselineFeedRankingStrategy baselineStrategy;
    private final RecommendationFeatureAssembler featureAssembler;
    private final RecommendationProperties properties;

    @Override
    public List<FeedCandidate> rank(List<FeedCandidate> candidates, FeedRankingContext context) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Disabled mode: immediately use BaselineFeedRankingStrategy without remote calls
        if (!properties.isEnabled()) {
            log.debug("Recommendation engine disabled. Using BaselineFeedRankingStrategy.");
            return baselineStrategy.rank(candidates, context);
        }

        if (candidates.size() == 1) {
            return new ArrayList<>(candidates);
        }

        UUID requestId = UUID.randomUUID();
        try {
            return doRankWithRecommendation(candidates, context, requestId);
        } catch (Exception ex) {
            if (properties.isFallbackEnabled()) {
                log.warn("Recommendation ranking failed for requestId={}: {}. Falling back to BaselineFeedRankingStrategy.",
                        requestId, ex.getMessage());
                return baselineStrategy.rank(candidates, context);
            }
            throw new RecommendationException("Recommendation ranking failed and fallback is disabled", ex);
        }
    }

    private List<FeedCandidate> doRankWithRecommendation(
            List<FeedCandidate> candidates,
            FeedRankingContext context,
            UUID requestId
    ) {
        // Enforce candidate bounds for payload
        List<FeedCandidate> boundedCandidates = candidates;
        if (candidates.size() > properties.getMaxCandidates()) {
            boundedCandidates = candidates.subList(0, properties.getMaxCandidates());
        }

        // Index submitted candidates by ContentRef
        Map<ContentRef, FeedCandidate> candidateMap = new HashMap<>(candidates.size());
        for (FeedCandidate candidate : candidates) {
            candidateMap.put(candidate.contentRef(), candidate);
        }

        // Assemble request payload
        RecommendationRankRequest request = featureAssembler.assemble(boundedCandidates, context, requestId);

        // Execute remote ranking call
        RecommendationRankResponse response = recommendationClient.rank(request);

        // Validate top-level contract integrity
        validateResponse(response, requestId);

        // Process and validate ranked items
        List<FeedCandidate> rankedCandidates = new ArrayList<>();
        Set<ContentRef> seenRefs = new HashSet<>();

        for (RankedItemResponse item : response.items()) {
            ContentRef ref = item.contentRef();
            if (ref == null) {
                log.warn("Encountered null ContentRef in recommendation response for requestId={}", requestId);
                continue;
            }

            // Validate score finiteness
            if (Double.isNaN(item.score()) || Double.isInfinite(item.score())) {
                log.warn("Invalid non-finite score {} for contentRef {} in requestId={}", item.score(), ref, requestId);
                continue;
            }

            // Deduplicate returned items
            if (seenRefs.contains(ref)) {
                log.warn("Duplicate ContentRef {} returned in response for requestId={}; ignoring duplicate", ref, requestId);
                continue;
            }

            // SECURITY GATE: Candidate MUST exist in original submitted candidates
            FeedCandidate submittedCandidate = candidateMap.get(ref);
            if (submittedCandidate == null) {
                log.warn("Security violation: Python ranker attempted to inject unknown or ineligible ContentRef {}. Dropping.", ref);
                continue;
            }

            rankedCandidates.add(submittedCandidate);
            seenRefs.add(ref);
        }

        // Graceful partial result handling: append unranked eligible candidates sorted by baseline strategy
        if (rankedCandidates.size() < candidates.size()) {
            List<FeedCandidate> unranked = new ArrayList<>();
            for (FeedCandidate c : candidates) {
                if (!seenRefs.contains(c.contentRef())) {
                    unranked.add(c);
                }
            }
            if (!unranked.isEmpty()) {
                log.debug("Python ranker returned partial ranking ({}/{}). Appending remaining candidates using baseline strategy.",
                        rankedCandidates.size(), candidates.size());
                List<FeedCandidate> remainder = baselineStrategy.rank(unranked, context);
                rankedCandidates.addAll(remainder);
            }
        }

        return rankedCandidates;
    }

    private void validateResponse(RecommendationRankResponse response, UUID expectedRequestId) {
        if (response == null) {
            throw new RecommendationException("Recommendation service returned null response body");
        }
        if (!RecommendationRankRequest.CURRENT_CONTRACT_VERSION.equals(response.contractVersion())) {
            throw new RecommendationException("Contract version mismatch: expected "
                    + RecommendationRankRequest.CURRENT_CONTRACT_VERSION + ", got " + response.contractVersion());
        }
        if (!expectedRequestId.equals(response.requestId())) {
            throw new RecommendationException("RequestId mismatch: expected " + expectedRequestId
                    + ", got " + response.requestId());
        }
        if (response.items() == null) {
            throw new RecommendationException("Recommendation response items list must not be null");
        }
    }
}
