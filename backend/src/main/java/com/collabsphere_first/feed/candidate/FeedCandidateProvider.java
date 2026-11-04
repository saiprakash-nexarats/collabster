package com.collabsphere_first.feed.candidate;

import java.util.List;

public interface FeedCandidateProvider {
    FeedSourceType sourceType();

    List<FeedCandidate> collect(FeedCandidateContext context);
}
