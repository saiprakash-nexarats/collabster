package com.collabsphere_first.recommendation.client;

import com.collabsphere_first.recommendation.dto.RecommendationRankRequest;
import com.collabsphere_first.recommendation.dto.RecommendationRankResponse;

/**
 * Client SPI for communicating with the internal Python recommendation engine.
 */
public interface RecommendationClient {

    /**
     * Send eligible candidates to Python ranker and return ordered items.
     *
     * @param request Typed request payload
     * @return Ranked candidates response
     * @throws com.collabsphere_first.recommendation.exception.RecommendationException upon failure or circuit open
     */
    RecommendationRankResponse rank(RecommendationRankRequest request);
}
