package com.collabsphere_first.security.oauth2;

import com.collabsphere_first.auth.dto.response.AuthenticationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OAuth2CodeStore {

    public record CachedAuth(AuthenticationResponse response, String clientFingerprint, long expiresAtMillis) {}

    private final Map<String, CachedAuth> memoryStore = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Value("${collabster.oauth2.require-shared-store:#{null}}")
    private Boolean requireSharedStore;

    @Autowired
    public OAuth2CodeStore(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Autowired(required = false) Environment environment) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    public OAuth2CodeStore() {
        this(null, new ObjectMapper(), null);
    }

    public boolean isSharedStoreRequired() {
        if (requireSharedStore != null) {
            return requireSharedStore;
        }
        if (environment != null) {
            String active = environment.getProperty("spring.profiles.active", "");
            String def = environment.getProperty("spring.profiles.default", "prod");
            String effective = active.isBlank() ? def : active;
            return effective != null && !effective.toLowerCase().contains("dev") && !effective.toLowerCase().contains("test");
        }
        return false;
    }

    public boolean isRedisAvailable() {
        if (redisTemplate == null) {
            return false;
        }
        try {
            RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
            if (factory == null) {
                return false;
            }
            try (RedisConnection conn = factory.getConnection()) {
                return "PONG".equalsIgnoreCase(conn.ping());
            }
        } catch (Exception ex) {
            log.warn("Redis availability check failed: {}", ex.getMessage());
            return false;
        }
    }

    public void saveCode(String code, AuthenticationResponse response, String clientFingerprint, long ttlSeconds) {
        if (code == null || response == null) {
            return;
        }

        boolean sharedRequired = isSharedStoreRequired();
        long expiresAtMillis = System.currentTimeMillis() + (ttlSeconds * 1000);

        // Distributed Redis persistence
        if (redisTemplate != null) {
            try {
                CachedAuth cached = new CachedAuth(response, clientFingerprint, expiresAtMillis);
                String json = objectMapper.writeValueAsString(cached);
                redisTemplate.opsForValue().set("oauth_code:" + code, json, Duration.ofSeconds(ttlSeconds));
                return;
            } catch (Exception e) {
                log.error("Failed to store OAuth authorization code in Redis: {}", e.getMessage(), e);
                if (sharedRequired) {
                    throw new IllegalStateException("Shared Redis store is required for OAuth code exchange in production multi-instance deployment, but Redis write failed", e);
                }
            }
        } else if (sharedRequired) {
            throw new IllegalStateException("Shared Redis store is required for OAuth code exchange in production multi-instance deployment, but StringRedisTemplate is not configured or unavailable");
        }

        // Local memory fallback ONLY when shared store is not required (e.g. dev/test)
        cleanExpired();
        memoryStore.put(code, new CachedAuth(response, clientFingerprint, expiresAtMillis));
    }

    public void saveCode(String code, AuthenticationResponse response, long ttlSeconds) {
        saveCode(code, response, null, ttlSeconds);
    }

    public AuthenticationResponse consumeCode(String code, String clientFingerprint) {
        if (code == null) {
            return null;
        }

        boolean sharedRequired = isSharedStoreRequired();

        // 1. Atomic single-use redemption via Redis GETDEL
        if (redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().getAndDelete("oauth_code:" + code);
                if (json != null && !json.isBlank()) {
                    CachedAuth cached = objectMapper.readValue(json, CachedAuth.class);
                    if (System.currentTimeMillis() > cached.expiresAtMillis()) {
                        log.warn("OAuth authorization code {} has expired", code);
                        return null;
                    }
                    if (cached.clientFingerprint() != null && !Objects.equals(cached.clientFingerprint(), clientFingerprint)) {
                        log.warn("OAuth authorization code browser binding mismatch for code {}", code);
                        return null;
                    }
                    return cached.response();
                }
                return null;
            } catch (Exception e) {
                log.error("Failed to consume OAuth authorization code from Redis: {}", e.getMessage(), e);
                if (sharedRequired) {
                    throw new IllegalStateException("Shared Redis store is required for OAuth code exchange in production multi-instance deployment, but Redis getAndDelete failed", e);
                }
            }
        } else if (sharedRequired) {
            throw new IllegalStateException("Shared Redis store is required for OAuth code exchange in production multi-instance deployment, but StringRedisTemplate is not configured or unavailable");
        }

        // 2. Thread-safe atomic single-use memory consumption (dev/test only)
        cleanExpired();
        CachedAuth cached = memoryStore.remove(code);
        if (cached == null) {
            return null;
        }
        if (System.currentTimeMillis() > cached.expiresAtMillis()) {
            log.warn("OAuth authorization code {} expired in memory", code);
            return null;
        }
        if (cached.clientFingerprint() != null && !Objects.equals(cached.clientFingerprint(), clientFingerprint)) {
            log.warn("OAuth authorization code browser binding mismatch in memory for code {}", code);
            return null;
        }
        return cached.response();
    }

    public AuthenticationResponse consumeCode(String code) {
        return consumeCode(code, null);
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        memoryStore.entrySet().removeIf(entry -> now > entry.getValue().expiresAtMillis());
    }
}
