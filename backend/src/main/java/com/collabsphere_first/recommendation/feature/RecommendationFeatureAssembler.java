package com.collabsphere_first.recommendation.feature;

import com.collabsphere_first.feed.candidate.FeedCandidate;
import com.collabsphere_first.feed.candidate.FeedSourceType;
import com.collabsphere_first.feed.ranking.FeedRankingContext;
import com.collabsphere_first.recommendation.dto.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Responsible for mapping eligible Java FeedCandidates and authoritative signals
 * into the canonical RecommendationRankRequest payload.
 */
@Component
public class RecommendationFeatureAssembler {

    private static final double ONE_DAY_SECONDS = 86400.0;

    public RecommendationRankRequest assemble(
            List<FeedCandidate> candidates,
            FeedRankingContext context,
            UUID requestId
    ) {
        RecommendationUserContext userContext = context.viewerUserId() != null
                ? new RecommendationUserContext(context.viewerUserId())
                : null;

        List<RecommendationCandidatePayload> candidatePayloads = new ArrayList<>(candidates.size());
        Instant now = context.now();

        for (FeedCandidate candidate : candidates) {
            RecommendationFeatures features = computeFeatures(candidate, context, now);
            List<String> sourceNames = candidate.sources().stream()
                    .map(FeedSourceType::name)
                    .toList();

            candidatePayloads.add(new RecommendationCandidatePayload(
                    candidate.contentRef(),
                    candidate.authorId(),
                    candidate.createdAt(),
                    sourceNames,
                    features
            ));
        }

        return new RecommendationRankRequest(requestId, userContext, candidatePayloads);
    }

    private RecommendationFeatures computeFeatures(FeedCandidate candidate, FeedRankingContext context, Instant now) {
        Double freshness = computeFreshness(candidate.createdAt(), now);
        Double relationship = computeRelationship(candidate.sources());
        Double community = candidate.sources().contains(FeedSourceType.JOINED_COMMUNITY) ? 1.0 : 0.0;

        long likeCount = context.likeCounts().getOrDefault(candidate.contentRef(), 0L);
        Double popularity = computePopularity(likeCount);

        // Intentionally missing signals (quality, preference, interest, userInteractionCount, postInteractionCount, creatorReputation)
        // are explicitly set to null so Python maps them to NaN.
        // In model v2 training, post_interaction_count was 1 - exp(-total_interactions / 20.0).
        // Since Java currently lacks comments/shares/views aggregation, passing null avoids range/semantic distortion.
        return new RecommendationFeatures(
                freshness,
                null,
                popularity,
                null,
                null,
                relationship,
                community,
                null,
                null,
                null
        );

    }

    private Double computeFreshness(Instant createdAt, Instant now) {
        if (createdAt == null) {
            return 0.0;
        }
        long ageSeconds = Math.max(0, Duration.between(createdAt, now).getSeconds());
        double decay = Math.pow(0.5, (double) ageSeconds / ONE_DAY_SECONDS);
        if (Double.isNaN(decay) || Double.isInfinite(decay)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, Math.round(decay * 1_000_000.0) / 1_000_000.0));
    }

    private Double computeRelationship(Set<FeedSourceType> sources) {
        if (sources == null || sources.isEmpty()) {
            return 0.20; // PUBLIC_FALLBACK
        }
        if (sources.contains(FeedSourceType.CONNECTION)) {
            return 1.0;
        }
        if (sources.contains(FeedSourceType.FOLLOWING)) {
            return 0.85;
        }
        if (sources.contains(FeedSourceType.JOINED_COMMUNITY)) {
            return 0.70;
        }
        if (sources.contains(FeedSourceType.SELF)) {
            return 0.40;
        }
        return 0.20;
    }

    private Double computePopularity(long likeCount) {
        if (likeCount <= 0) {
            return 0.0;
        }
        double normalized = Math.log1p(likeCount) * 0.1;
        return Math.max(0.0, Math.min(1.0, Math.round(normalized * 1_000_000.0) / 1_000_000.0));
    }
}
