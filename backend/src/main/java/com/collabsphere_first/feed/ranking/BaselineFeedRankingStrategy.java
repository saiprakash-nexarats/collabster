package com.collabsphere_first.feed.ranking;

import com.collabsphere_first.feed.candidate.FeedCandidate;
import com.collabsphere_first.feed.candidate.FeedSourceType;
import com.collabsphere_first.feed.config.FeedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Baseline deterministic Java ranking strategy for Phase 3.
 * Combines bounded recency decay, relationship/source strength, and lightweight engagement.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BaselineFeedRankingStrategy implements FeedRankingStrategy {

    @Override
    public List<FeedCandidate> rank(List<FeedCandidate> candidates, FeedRankingContext context) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        FeedProperties.Ranking rankingProps = context.properties().getRanking();
        Instant now = context.now();

        Map<FeedCandidate, Double> scored = new HashMap<>(candidates.size());
        for (FeedCandidate candidate : candidates) {
            double score = computeScore(candidate, context, rankingProps, now);
            scored.put(candidate, score);
        }

        List<FeedCandidate> ranked = new ArrayList<>(candidates);
        ranked.sort((a, b) -> {
            int scoreCompare = Double.compare(scored.get(b), scored.get(a)); // Higher score first
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            // Secondary tie-breaker: createdAt DESC
            int dateCompare = b.createdAt().compareTo(a.createdAt());
            if (dateCompare != 0) {
                return dateCompare;
            }
            // Final deterministic tie-breaker: ContentRef string representation
            return a.contentRef().toString().compareTo(b.contentRef().toString());
        });

        return ranked;
    }

    private double computeScore(
            FeedCandidate candidate,
            FeedRankingContext context,
            FeedProperties.Ranking rankingProps,
            Instant now
    ) {
        double recencyScore = computeRecencyScore(candidate.createdAt(), now, rankingProps.getRecencyHalfLifeHours());
        double relationshipScore = computeRelationshipScore(candidate.sources(), rankingProps);
        long likeCount = context.likeCounts().getOrDefault(candidate.contentRef(), 0L);
        double engagementScore = computeEngagementScore(likeCount);

        return (recencyScore * rankingProps.getRecencyWeight())
                + (relationshipScore * rankingProps.getRelationshipWeight())
                + (engagementScore * rankingProps.getEngagementWeight());
    }

    private double computeRecencyScore(Instant createdAt, Instant now, double halfLifeHours) {
        if (createdAt == null) {
            return 0.0;
        }
        long ageSeconds = Math.max(0, Duration.between(createdAt, now).getSeconds());
        double halfLifeSeconds = Math.max(3600.0, halfLifeHours * 3600.0);
        double score = Math.pow(0.5, (double) ageSeconds / halfLifeSeconds);
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, score));
    }

    private double computeRelationshipScore(Set<FeedSourceType> sources, FeedProperties.Ranking rankingProps) {
        if (sources == null || sources.isEmpty()) {
            return rankingProps.getFallbackSourceWeight();
        }

        double maxWeight = 0.0;
        for (FeedSourceType source : sources) {
            double weight = switch (source) {
                case CONNECTION -> rankingProps.getConnectionSourceWeight();
                case FOLLOWING -> rankingProps.getFollowingSourceWeight();
                case JOINED_COMMUNITY -> rankingProps.getCommunitySourceWeight();
                case SELF -> rankingProps.getSelfSourceWeight();
                case PUBLIC_FALLBACK -> rankingProps.getFallbackSourceWeight();
            };
            if (weight > maxWeight) {
                maxWeight = weight;
            }
        }

        if (sources.size() > 1) {
            maxWeight = Math.min(1.0, maxWeight + 0.05 * (sources.size() - 1));
        }

        return Math.max(0.0, Math.min(1.0, maxWeight));
    }

    private double computeEngagementScore(long likeCount) {
        if (likeCount <= 0) {
            return 0.0;
        }
        double score = Math.log1p(likeCount) / Math.log1p(100.0);
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, score));
    }
}
