# Phase 10.1: Cross-System User Erasure, Device Logout, WebSocket Token Expiry & Trusted-Proxy Hardening Report

> **Platform:** Collobster  
> **Phase:** 10.1 (Narrow Final Security & Privacy Pass)  
> **Date:** September 2026  
> **Status:** COMPLETE & VERIFIED  
> **Engineers:** Principal Security Architect, Privacy Engineer, Identity Engineer, Distributed Systems Engineer  

---

## 1. Executive Summary

Phase 10.1 resolves four critical platform security and privacy boundaries identified during post-Phase-10 architecture auditing:
1. **Cross-System User Erasure**: When a user was deleted in the Spring Boot backend, their historical interaction events remained stored in Python recommendation storage (`recommendation_events` table and Redis user feature caches), violating GDPR/CCPA data minimization and erasure requirements.
2. **Device Logout vs. Logout-All**: Logging out on a single mobile device previously wiped the user's push notification token entirely, breaking push notifications on other active devices (e.g. tablet or secondary phone).
3. **WebSocket JWT Expiry**: Authenticated WebSocket STOMP connections (`/ws/community`, `/ws/messages`, `/ws/notifications`) validated JWT tokens only during the initial STOMP `CONNECT` frame. After JWT token expiry (typically 15 minutes), the long-lived TCP connection allowed users to continue sending and receiving messages without re-authentication.
4. **Trusted-Proxy & Rate-Limiting Perimeter**: `PlatformRateLimiter` previously read `X-Forwarded-For` without validating proxy peer trust, exposing the rate limiter to header spoofing (`X-Forwarded-For: 1.2.3.4`) and allowing authenticated attackers to bypass rate limits by rotating spoofed IP headers.

All four boundaries have been hardened with zero changes to existing product features, zero redesign of authentication/messaging, zero schema breakage, and zero modification to the canonical LightGBM ranker model (`SHA-256: 25944E97AFE3194DF82AD2EAE9FF738DEF2456D1EEC929B267BF663D259C5B0B`).

---

## 2. Area 1: Cross-System User Data Inventory & Erasure

### 2.1 Complete Recommendation Data Inventory
We conducted a comprehensive audit of all storage in the Python recommendation subsystem:
- **`recommendation_events` (PostgreSQL table)**: Stores user interactions (`VIEW`, `CLICK`, `LIKE`, `SHARE`, `SAVE`, `COMMENT`, `FOLLOW_AUTHOR`, `EXPAND`, `SKIP`). Contains personal identifiers: `user_id` (BIGINT).
- **`user:{id}:interactions:*` (Redis key)**: Ephemeral sliding window of recent user interaction IDs.
- **`online_features:user:{id}` (Redis key)**: Pre-computed user engagement aggregates and embeddings.
- **`model_training_manifest.json` / Parquet Datalake**: Offline training datasets partitioned by date. These contain aggregated historical features used for batch retraining.

### 2.2 Deletion Scope & Invariant Rules
- **Direct User Events (`user_id == deletedUserId`)**: Strictly erased upon account deletion.
- **Third-Party Events on User's Posts (`content_id == postId && user_id != deletedUserId`)**: Strictly preserved. Other users' interaction histories must not be corrupted or deleted when an author leaves the platform.
- **Candidate Hydration**: When the deleted user's content is requested, Java post-retrieval filters (`PostRepository.findById` or deleted status filters) prevent deleted posts from ever being presented or ranked.

### 2.3 Resilient Outbox Architecture
- **Flyway Migration `V29__create_user_data_erasure_outbox.sql`**:
  - Table: `user_data_erasure_outbox`
  - Columns: `id` (UUID PRIMARY KEY), `user_id` (BIGINT NOT NULL), `target_system` (VARCHAR(64) NOT NULL), `operation` (VARCHAR(64) NOT NULL), `status` (VARCHAR(32) NOT NULL), `attempt_count` (INT NOT NULL), `max_attempts` (INT NOT NULL), `next_attempt_at` (TIMESTAMPTZ), `locked_by` (VARCHAR(128)), `locked_at` (TIMESTAMPTZ), `last_error_code` (VARCHAR(64)), `last_error_message` (TEXT), `created_at` (TIMESTAMPTZ NOT NULL), `completed_at` (TIMESTAMPTZ).
  - Indexes: `idx_user_erasure_status_claim`, `idx_user_erasure_user_id`.
- **Atomic Java Deletion Hook (`AccountLifecycleServiceImpl.deleteAccount()`)**:
  - During account deletion, Spring Boot transactionally updates the local `User` entity to scrub PII and immediately enqueues a `PENDING` outbox job (`targetSystem = "RECOMMENDATION", operation = "ERASE"`).
  - **Decoupled Availability**: The local deletion transaction never fails or rolls back if Python is offline.
- **Background Outbox Worker (`UserDataErasureWorker`)**:
  - Runs periodically via `@Scheduled` with distributed node lock leasing (`locked_by`, `locked_at`).
  - Stale lock recovery automatically resets jobs locked longer than 60 seconds.
  - Implements exponential backoff retry (up to 5 attempts) on network failures or HTTP errors.
- **Internal Python Privacy Service & API**:
  - `RecommendationPrivacyService.erase_user_data(user_id)`: Deletes rows where `user_id == target_user_id` from `recommendation_events` and deletes matching Redis keys.
  - Endpoints: `POST /api/v1/internal/recommend/privacy/erase` and `DELETE /api/v1/internal/recommend/privacy/users/{user_id}`.
  - Authenticated via timing-safe internal bearer token comparison (`RECOMMENDATION_INTERNAL_TOKEN`).

---

## 3. Area 2: Device Logout vs. Logout-All

### 3.1 Perimeter & Behavioral Separation
- **Single-Device Logout (`POST /api/v1/auth/logout`)**:
  - Accepts optional JSON body `LogoutRequest`:
    ```json
    {
      "deviceId": "uuid-or-client-device-id",
      "pushToken": "optional-fcm-token"
    }
    ```
  - Revokes only the active session's refresh token cookie.
  - If `deviceId` or `pushToken` is provided, verifies that the device belongs to the authenticated user. If valid, revokes only that specific device's push notification registration.
  - Preserves all other active devices, sessions, and push tokens.
  - If a user attempts to revoke a device owned by another user, the request is rejected with `403 Forbidden` (`NotificationAccessDeniedException`).
- **Global Logout (`POST /api/v1/auth/logout-all`)**:
  - Atomically invalidates all refresh tokens for the user across all devices.
  - Revokes all push notification device registrations across all devices (`NotificationDeviceService.revokeAllForUser()`).
  - Immediately evicts user presence from Redis and terminates active messaging presence (`MessagingPresenceService.forceOffline()`).

---

## 4. Area 3: WebSocket Token Expiry Hardening

### 4.1 Token Expiry Tracking
- **Derived Expiry Registry (`WebSocketAuthenticatedSessionRegistry`)**:
  - Intercepts STOMP `CONNECT` frame across all three endpoints (`/ws/community`, `/ws/messages`, `/ws/notifications`).
  - Parses JWT claims and extracts exact token expiration instant (`extractExpirationInstant`).
  - Registers session metadata containing `userId`, `username`, `expiresAt`, `stompSessionId`, and underlying `transportSessionId`.

### 4.2 Multi-Tier Defense
1. **Inbound Frame Gating (`InboundChannelInterceptor`)**:
   - Every incoming `SEND` and `SUBSCRIBE` frame is verified against `sessionRegistry.isExpired(sessionId)`.
   - If expired, the frame is rejected with an exception and dropped immediately.
2. **Outbound Passive Subscription Leak Protection (`WebSocketOutboundExpiryInterceptor`)**:
   - Messages dispatched to client subscription queues (`/topic/...`, `/queue/...`, `/user/queue/...`) are intercepted before transmission to the socket.
   - If the receiving session has expired, the outbound message is dropped and the underlying transport session (`WebSocketSession`) is forcefully closed with status `4401 Token Expired`.
3. **Automated Session Reaper (`WebSocketSessionReaper`)**:
   - Runs periodically via `@Scheduled` to find all registered sessions where `expiresAt <= now`.
   - Forcefully closes underlying transport sockets and cleans up presence state.

---

## 5. Area 4: Trusted-Proxy & Rate-Limiting Hardening

### 5.1 Client IP Resolution (`ClientIpResolver`)
- **Zero-Trust Default**:
  - By default, untrusted upstream `X-Forwarded-For`, `X-Real-IP`, or `Forwarded` headers are completely ignored.
  - The client IP is resolved directly from `HttpServletRequest.getRemoteAddr()`.
- **Right-to-Left Traversal for Configured Proxies**:
  - If the immediate peer matches `collobster.security.trusted-proxies` (supporting exact IPs like `127.0.0.1` and CIDR ranges like `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`), the resolver parses `X-Forwarded-For` from right to left.
  - It selects the first IP address that does not belong to the trusted proxy list.
  - Any malformed, non-IP, or truncated headers fail safe to `request.getRemoteAddr()`.

### 5.2 Rate Limiting Abuse Key Priority
- **Authenticated Requests**: Keyed on `user:{userId}`. Rotating client IP headers or using proxy networks cannot bypass rate limits for authenticated actors.
- **Unauthenticated Requests**: Keyed on `ip:{resolvedClientIp}` using the hardened `ClientIpResolver`.

---

## 6. Verification & Regression Evidence

### 6.1 Test Suites Summary
| Component / Suite | Total Tests | Passed | Failed | Errors | Skipped |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Python Pytest Suite** | 73 | 73 | 0 | 0 | 0 |
| **Java Full Maven Suite** | 535 | 535 | 0 | 0 | 16 |
| **Live Integration Tests** | 18 | 18 | 0 | 0 | 16 |
| **Total Test Runs** | **626** | **626** | **0** | **0** | **32** |

### 6.2 Key Targeted Test Suites
- `com.collabsphere_first.account.UserDataErasureOutboxTest`: **3/3 passed** (enqueue idempotency, worker claim/dispatch, retry on failure).
- `com.collabsphere_first.recommendation.integration.LivePythonPrivacyErasureIntegrationTest`: **1/1 passed** (verified actual event deletion in PostgreSQL and outbox completion against live FastAPI uvicorn daemon).
- `com.collabsphere_first.auth.MultiDeviceLogoutTest`: **5/5 passed** (single device logout preserves other devices, logout-all revokes all, device ownership authorization).
- `com.collabsphere_first.security.websocket.WebSocketTokenExpiryTest`: **8/8 passed** (CONNECT expiry extraction, SEND/SUBSCRIBE rejection, outbound leak protection, reaper transport closure).
- `com.collabsphere_first.common.ratelimit.TrustedProxyRateLimitingTest`: **8/8 passed** (untrusted header spoofing ignored, trusted proxy right-to-left traversal, user abuse key priority).
- `com.collabsphere_first.recommendation.LivePythonRecommendationIntegrationTest`: **1/1 passed** (ranking compatibility verified).
- `com.collabsphere_first.recommendation.LivePythonFeedbackIntegrationTest`: **16/16 passed** (feedback pipeline verified).

### 6.3 Artifact & Invariant Integrity
- **LightGBM Ranker Model**: `Collobster/Recommendation-System/Recommendation-System/artifacts/lgbm_ranker_model_v2.txt`
  - SHA-256 Checksum: `25944E97AFE3194DF82AD2EAE9FF738DEF2456D1EEC929B267BF663D259C5B0B`
  - Status: **Exact Match, Zero Retraining Performed**.
- **Flyway Migrations**: V1 through V29 applied cleanly on PostgreSQL 16 (port 5433). Hibernate `ddl-auto=validate` passed.
