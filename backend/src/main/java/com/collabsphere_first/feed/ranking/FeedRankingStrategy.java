package com.collabsphere_first.feed.ranking;

import com.collabsphere_first.feed.candidate.FeedCandidate;

import java.util.List;

/**
 * Strategy SPI for ordering feed candidates.
 * In Phase 3, BaselineFeedRankingStrategy provides deterministic Java scoring.
 * In Phase 4, RecommendationFeedRankingStrategy will implement this contract using Python ML models.
 */
public interface FeedRankingStrategy {

    List<FeedCandidate> rank(List<FeedCandidate> candidates, FeedRankingContext context);
}
