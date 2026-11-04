package com.collabsphere_first.security.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;

/**
 * Trustworthy session authentication metadata derived at STOMP CONNECT time from validated JWT.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketSessionMetadata {
    private String sessionId;
    private Long userId;
    private String username;
    private Instant connectedAt;
    private Instant expiresAt;
    private Long authVersion;
    private volatile boolean revoked;
    private String endpoint;
    private volatile WebSocketSession transportSession;
}
