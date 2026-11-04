package com.collabsphere_first.feed.candidate;

import com.collabsphere_first.feed.config.FeedProperties;
import com.collabsphere_first.user.entity.User;

import java.util.Collections;
import java.util.Set;

public record FeedCandidateContext(
    User viewer,
    FeedProperties feedProperties,
    Set<Long> followingUserIds,
    Set<Long> connectedUserIds,
    Set<Long> mutedUserIds,
    Set<Long> blockedUserIds
) {
    public FeedCandidateContext {
        followingUserIds = followingUserIds != null ? followingUserIds : Collections.emptySet();
        connectedUserIds = connectedUserIds != null ? connectedUserIds : Collections.emptySet();
        mutedUserIds = mutedUserIds != null ? mutedUserIds : Collections.emptySet();
        blockedUserIds = blockedUserIds != null ? blockedUserIds : Collections.emptySet();
    }

    public Long viewerId() {
        return viewer != null ? viewer.getId() : null;
    }
}
