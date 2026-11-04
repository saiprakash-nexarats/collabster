# PHASE 10.2: MULTI-DEVICE WEBSOCKET EXPIRY + OFFLINE RECOMMENDATION DATA ERASURE HARDENING REPORT

## Executive Summary
Phase 10.2 delivers the final security and privacy hardening prior to Phase 11. It resolves two critical production invariants:
1. **Multi-Device WebSocket Expiry Correctness**: Fixed presence leakage where expiring one STOMP session (e.g. Phone) prematurely forced a multi-device user completely OFFLINE and killed notifications to valid sessions (e.g. Desktop). Session-scoped cleanup (`removeSession`, `handleSessionDisconnected`) now cleanly closes only the expired socket, prunes only that transport session from the Redis presence ZSET, and maintains the user ONLINE with uninterrupted `/user/queue/**` fanout to valid sessions until the final session expires.
2. **Offline Recommendation Data Erasure Lifecycle**: Built a durable two-stage privacy erasure architecture for the Recommendation Data Lake. Implemented Option A for JSONL (strictly prohibited in production via startup validation, with bounded retention/erasure for development), durable offline privacy manifests (`privacy_erasure_manifest.json`), atomic and retry-safe Parquet partition compaction (`OfflineParquetCompactor`), and pre-compaction training dataset filtering (`TrainingDataLoader`) that guarantees newly trained models cannot consume tombstoned user data.

---

## 1. WebSocket Multi-Device Root Cause Audit
Prior to Phase 10.2:
- When a STOMP session expired or disconnected, `WebSocketSessionReaper` and outbound interceptors called `MessagingPresenceService.setUserOffline(userId)` directly.
- `setUserOffline(userId)` was an account-wide operation that executed `delete(presenceKey(userId))` and cleared all session members from the user's active session tracking.
- As a consequence, if User A had a Phone session (expired JWT) and Desktop session (valid JWT), the expiration of the Phone session incorrectly marked User A OFFLINE in Redis and stripped Desktop presence.
- Furthermore, outbound message delivery handling lacked granular session isolation, risking suppression of private user notifications across all devices.

## 2. Session-Scoped Presence Implementation
- **New Session-Scoped APIs**: Added `removeSession(Long userId, String sessionId)` and `handleSessionDisconnected(Long userId, String sessionId)` to `MessagingPresenceService` and `MessagingPresenceServiceImpl`.
- **Redis ZSET Pruning**: `removeSession` removes only the specific `sessionId` entry from `messages:presence:{userId}:sessions` via `zRem(sessionKey, sessionId)`.
- **Presence State Evaluation**: If `zCard(sessionKey) > 0`, the user remains active in `messages:presence:users` and the user presence key TTL is refreshed. Only when `zCard(sessionKey) == 0` is the user removed from `messages:presence:users` and marked OFFLINE.
- **Preserved `forceOffline(userId)`**: Strictly preserved for true account-wide operations:
  - Account deletion
  - Account deactivation
  - `POST /api/v1/auth/logout-all`
  - Admin suspension

## 3. WebSocket Session Reaper & Outbound Interceptor Hardening
- **`WebSocketSessionReaper`**:
  - Unregisters only the expired session from `WebSocketAuthenticatedSessionRegistry`.
  - Closes only the underlying transport socket with CloseStatus 4401 ("Access token expired").
  - Invokes `presenceService.handleSessionDisconnected(userId, sessionId)` to remove only that session.
  - Queries `registry.getActiveSessionCountForUser(userId)` to log remaining valid sessions.
- **`WebSocketOutboundExpiryInterceptor`**:
  - Injected with `MessagingPresenceService`.
  - When an outbound frame targets an expired session, it drops delivery to that socket, closes the socket, and prunes only that session's presence.
  - Leaves other sessions for the user untouched.
- **Spring User Destination Fanout**: Spring STOMP user queue destinations (`/user/queue/**`) resolve target sessions individually. With session-scoped expiry, frames targeting the valid Desktop session are delivered without hindrance, while frames for the expired Phone session are intercepted and dropped.

## 4. Multi-Device Expiry Verification
- Created `MultiDeviceWebSocketExpiryTest.java` verifying:
  1. `twoSessions_oneExpires_otherRemainsAuthorized`: User 501 connects with Phone (session 1, expires +10s) and Desktop (session 2, expires +3600s). Phone expires. Reaper closes Phone socket and removes its session. User presence remains **ONLINE** (`isUserOnline == true`). Desktop session remains connected. Inbound frame on Desktop is authorized. Private notification delivered to Desktop succeeds. Outbound frame to Phone is dropped. Expired Phone session cannot SEND or SUBSCRIBE.
  2. `finalSessionExpiry`: Desktop session later expires. Reaper closes Desktop socket. Active session registry count reaches 0. User presence transitions to **OFFLINE** (`isUserOnline == false`). Redis session representation is empty.
- Updated `WebSocketTokenExpiryTest.java` (8/8 tests pass).

## 5. Offline Recommendation Data Erasure Lifecycle
- **JSONL Policy (Option A - Development Only)**:
  - Enforced in `app/events/factory.py`: If `APP_ENV=production` and `EVENT_PUBLISHER=jsonl`, startup fails immediately with `ValueError`.
  - Development retention and atomic erasure provided by `JsonlRetentionManager` (default retention: 48h; atomic rewrite and rename on erasure).
- **Parquet Data Lake Erasure**:
  - Implemented `PrivacyErasureManifest` (`data_lake/privacy_manifests/privacy_erasure_manifest.json`) tracking `user_id`, `requested_at`, `status` (`PENDING`, `COMPLETED`, `FAILED`), `completed_at`, `affected_partitions`, `rows_removed`, `error_message`.
  - Implemented `OfflineParquetCompactor`: Atomic partition compaction using `pyarrow.parquet` and `pyarrow.compute`. Scans partitions, filters target user rows, writes to atomic `.tmp` files, and renames. Preserves all other users' rows. Idempotent and retry-safe.
  - Implemented `TrainingDataLoader`: Guarded loader that checks active tombstones in `PrivacyErasureManifest` and automatically filters out any tombstoned users before training data is fed into model pipelines.
- **Model Training Manifest PII Audit**:
  - Audited `app/pipeline/training_manifest.py` and `scripts_create_training_manifest.py`.
  - Verified manifests record only `run_id`, `created_at`, `dataset`, `features`, and `config`. Zero direct or indirect user identifiers. Retained without modification.
- **LightGBM Model Checksum Integrity**:
  - Verified SHA-256: `25944E97AFE3194DF82AD2EAE9FF738DEF2456D1EEC929B267BF663D259C5B0B`.
  - Zero model retraining was performed.

## 6. Truthful Privacy Completion Semantics
- Internal privacy endpoint (`POST /api/v1/internal/recommend/privacy/erase`) returns structured breakdown:
  ```json
  {
    "status": "COMPLETED",
    "userId": 879291,
    "deletedEventsCount": 1,
    "postgresEventsDeleted": 1,
    "purgedRedisKeysCount": 0,
    "redisKeysDeleted": 0,
    "offlineErasureScheduled": true,
    "offlineErasureStatus": "PENDING",
    "details": "Erased 1 PostgreSQL events, purged 0 Redis keys, and scheduled offline Parquet compaction for user 879291"
  }
  ```
- **Java Semantics**: Outbox job `status = COMPLETED` means online recommendation stores (`recommendation_events`, Redis feature keys) are erased and offline erasure is durably scheduled in the manifest.
- **Offline Semantics**: Manifest `status = COMPLETED` means Parquet physical partition files have been compacted and rewritten.

---

## 7. Verification Test Results Summary

| Suite / Test Profile | Tests Run | Passed | Failed | Errors | Skipped | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Java Full Maven Test Suite** (`mvnw.cmd test`) | 537 | 521 | 0 | 0 | 16 | **PASS** |
| - `MultiDeviceWebSocketExpiryTest` | 2 | 2 | 0 | 0 | 0 | **PASS** |
| - `WebSocketTokenExpiryTest` | 8 | 8 | 0 | 0 | 0 | **PASS** |
| - `MessagingControllerTest` | 9 | 9 | 0 | 0 | 0 | **PASS** |
| - `MessagingPresenceAndTypingTest` | 8 | 8 | 0 | 0 | 0 | **PASS** |
| - `MessagingWebSocketSecurityTest` | 10 | 10 | 0 | 0 | 0 | **PASS** |
| - `MultiDeviceLogoutTest` | 5 | 5 | 0 | 0 | 0 | **PASS** |
| - `UserDataErasureOutboxTest` | 3 | 3 | 0 | 0 | 0 | **PASS** |
| **Python Pytest Suite** (`pytest -q`) | 79 | 79 | 0 | 0 | 0 | **PASS** |
| - `test_offline_privacy.py` | 6 | 6 | 0 | 0 | 0 | **PASS** |
| - `test_recommendation_privacy.py` | 4 | 4 | 0 | 0 | 0 | **PASS** |
| **Live Integration: Recommendation Ranking** | 1 | 1 | 0 | 0 | 0 | **PASS** |
| **Live Integration: Feedback Batch Delivery** | 16 | 16 | 0 | 0 | 0 | **PASS** |
| **Live Integration: Privacy Erasure** | 1 | 1 | 0 | 0 | 0 | **PASS** |
| **Model SHA-256 Checksum** | 1 | 1 | 0 | 0 | 0 | **MATCH** |
| **Flyway Database Migration Version** | 1 | 1 | 0 | 0 | 0 | **V29 (No V30)** |
