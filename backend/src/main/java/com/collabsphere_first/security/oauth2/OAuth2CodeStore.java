package com.collabsphere_first.security.oauth2;

import com.collabsphere_first.auth.dto.response.AuthenticationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OAuth2CodeStore {

    private record CachedAuth(AuthenticationResponse response, String clientFingerprint, Instant expiresAt) {}

    private final Map<String, CachedAuth> memoryStore = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public OAuth2CodeStore(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public OAuth2CodeStore() {
        this(null, new ObjectMapper());
    }

    public void saveCode(String code, AuthenticationResponse response, String clientFingerprint, long ttlSeconds) {
        if (code == null || response == null) {
            return;
        }

        // Try persisting in distributed Redis store with TTL
        if (redisTemplate != null) {
            try {
                CachedAuth cached = new CachedAuth(response, clientFingerprint, Instant.now().plusSeconds(ttlSeconds));
                String json = objectMapper.writeValueAsString(cached);
                redisTemplate.opsForValue().set("oauth_code:" + code, json, Duration.ofSeconds(ttlSeconds));
                return;
            } catch (Exception e) {
                log.warn("Failed to store OAuth code in Redis, falling back to memory: {}", e.getMessage());
            }
        }

        cleanExpired();
        memoryStore.put(code, new CachedAuth(response, clientFingerprint, Instant.now().plusSeconds(ttlSeconds)));
    }

    public void saveCode(String code, AuthenticationResponse response, long ttlSeconds) {
        saveCode(code, response, null, ttlSeconds);
    }

    public AuthenticationResponse consumeCode(String code, String clientFingerprint) {
        if (code == null) {
            return null;
        }

        // 1. Try atomic single-use consume from Redis
        if (redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().getAndDelete("oauth_code:" + code);
                if (json != null && !json.isBlank()) {
                    CachedAuth cached = objectMapper.readValue(json, CachedAuth.class);
                    if (Instant.now().isAfter(cached.expiresAt())) {
                        return null;
                    }
                    if (cached.clientFingerprint() != null && clientFingerprint != null
                            && !Objects.equals(cached.clientFingerprint(), clientFingerprint)) {
                        log.warn("OAuth code browser binding mismatch for code {}", code);
                        return null;
                    }
                    return cached.response();
                }
            } catch (Exception e) {
                log.warn("Failed to consume OAuth code from Redis, checking memory: {}", e.getMessage());
            }
        }

        // 2. Fall back to thread-safe atomic memory consumption
        cleanExpired();
        CachedAuth cached = memoryStore.remove(code);
        if (cached == null) {
            return null;
        }
        if (Instant.now().isAfter(cached.expiresAt())) {
            return null;
        }
        if (cached.clientFingerprint() != null && clientFingerprint != null
                && !Objects.equals(cached.clientFingerprint(), clientFingerprint)) {
            log.warn("OAuth code browser binding mismatch in memory for code {}", code);
            return null;
        }
        return cached.response();
    }

    public AuthenticationResponse consumeCode(String code) {
        return consumeCode(code, null);
    }

    private void cleanExpired() {
        Instant now = Instant.now();
        memoryStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }
}
