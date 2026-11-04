package com.collabsphere_first.network.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ConnectionRequestedEvent extends ApplicationEvent {
    private final UUID requestId;
    private final Long requesterId;
    private final Long addresseeId;
    private final UUID eventId;
    private final Instant occurredAt;

    public ConnectionRequestedEvent(Object source, UUID requestId, Long requesterId, Long addresseeId, UUID eventId) {
        super(source);
        this.requestId = requestId;
        this.requesterId = requesterId;
        this.addresseeId = addresseeId;
        this.eventId = eventId;
        this.occurredAt = Instant.now();
    }

    public ConnectionRequestedEvent(Object source, UUID requestId, Long requesterId, Long addresseeId) {
        this(source, requestId, requesterId, addresseeId, null);
    }
}
