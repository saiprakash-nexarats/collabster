package com.collabsphere_first.community.event;

public interface CommunityOutboxConsumer {
    String name();
    boolean supports(String eventType);
    void handle(CommunityOutboxMessage message); // throw on failure
}
