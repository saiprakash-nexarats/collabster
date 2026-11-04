package com.collabsphere_first.feed.session;

import java.time.Duration;
import java.util.Optional;

public interface FeedSessionStore {

    void saveSession(FeedSession session, Duration ttl);

    Optional<FeedSession> getSession(String sessionId);

    void deleteSession(String sessionId);
}
