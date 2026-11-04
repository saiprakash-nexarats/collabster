package com.collabsphere_first.system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dependency-aware readiness endpoint.
 * Satisfies both /api/v1/system/health and /v1/system/health for web/mobile probes.
 */
@RestController
public class SystemHealthController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    @org.springframework.beans.factory.annotation.Value("${collabster.system.health.cache-required:false}")
    private boolean cacheRequired = false;

    public SystemHealthController(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping({"/api/v1/system/health", "/v1/system/health"})
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        boolean databaseUp = isDatabaseUp();
        boolean cacheUp = isCacheUp();
        boolean ready = databaseUp && (!cacheRequired || cacheUp);
        health.put("status", (ready && cacheUp) ? "healthy" : "degraded");
        health.put("version", "1.0.0");
        health.put("service", "Collobster-Platform");
        health.put("database", databaseUp ? "connected" : "unavailable");
        health.put("cache", cacheUp ? "operational" : "unavailable");
        health.put("timestamp", Instant.now().toString());
        return ready ? ResponseEntity.ok(health) : ResponseEntity.status(503).body(health);
    }

    private boolean isDatabaseUp() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isCacheUp() {
        try (var connection = redisConnectionFactory.getConnection()) {
            return "PONG".equalsIgnoreCase(connection.ping());
        } catch (Exception ignored) {
            return false;
        }
    }
}
