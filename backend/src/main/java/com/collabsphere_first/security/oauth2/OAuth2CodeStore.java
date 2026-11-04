package com.collabsphere_first.security.oauth2;

import com.collabsphere_first.auth.dto.response.AuthenticationResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuth2CodeStore {

    private record CachedAuth(AuthenticationResponse response, Instant expiresAt) {}

    private final Map<String, CachedAuth> store = new ConcurrentHashMap<>();

    public void saveCode(String code, AuthenticationResponse response, long ttlSeconds) {
        cleanExpired();
        store.put(code, new CachedAuth(response, Instant.now().plusSeconds(ttlSeconds)));
    }

    public AuthenticationResponse consumeCode(String code) {
        cleanExpired();
        if (code == null) {
            return null;
        }
        CachedAuth cached = store.remove(code);
        if (cached == null) {
            return null;
        }
        if (Instant.now().isAfter(cached.expiresAt())) {
            return null;
        }
        return cached.response();
    }

    private void cleanExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }
}

