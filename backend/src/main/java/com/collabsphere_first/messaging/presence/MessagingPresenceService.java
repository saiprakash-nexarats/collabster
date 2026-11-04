package com.collabsphere_first.messaging.presence;

public interface MessagingPresenceService {

    void onSessionConnected(Long userId, String username, String sessionId);

    void onHeartbeat(Long userId, String sessionId);

    void onSessionDisconnected(Long userId, String username, String sessionId);

    void removeSession(Long userId, String sessionId);

    void handleSessionDisconnected(Long userId, String sessionId);

    boolean isUserOnline(Long userId);

    void forceOffline(Long userId);
}
