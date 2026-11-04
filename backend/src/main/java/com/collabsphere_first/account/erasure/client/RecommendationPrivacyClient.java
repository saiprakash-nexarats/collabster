package com.collabsphere_first.account.erasure.client;

public interface RecommendationPrivacyClient {

    /**
     * Erases all user-linked recommendation records and caches from the recommendation engine.
     *
     * @param userId the user ID to erase
     * @return true if erasure completed successfully
     */
    boolean eraseUserData(Long userId);
}
