# User Data Lifecycle & Privacy Inventory (Phase 10.2 Hardening)

## 1. Overview & Policy Invariants

This document establishes the user data inventory, retention policy, and lifecycle operations (Deactivation, Anonymization, Deletion, Device Logout, WebSocket Session Expiry, and Downstream Recommendation Erasure) across all Collobster storage layers:
- PostgreSQL (Application Authority & Flyway V1–V29)
- Redis (Session, Presence, Ephemeral Caches, Rate Limiting)
- Local / Cloud File Storage (Uploaded Media)
- Python Recommendation Storage (`recommendation_events`, Redis feature keys, Parquet offline lake)

**Compliance Statement**:
This system is designed to support user-data deletion requirements and implements the documented user-data erasure lifecycle. It does not assert certified legal compliance conclusions outside this engineering specification.

### Lifecycle States:
1. **UNVERIFIED**: Registered user who has not yet verified email via OTP (`email_verified = false`, `account_enabled = false`). Cannot log in.
2. **ACTIVE**: Verified and enabled user (`email_verified = true`, `account_enabled = true`, `deactivated = false`, `deleted = false`). Full access.
3. **DEACTIVATED**: Temporary departure initiated by the user or administrator (`deactivated = true`, `deactivated_at = timestamp`).
   - Active refresh tokens revoked.
   - Active push device tokens revoked.
   - Realtime presence evicted via `MessagingPresenceService.forceOffline(userId)`.
   - User hidden from passive discovery (Explore people, Feed recommendations).
   - Login blocked.
   - Historical user content preserved (recoverable upon reactivation).
4. **DELETED**: Permanent account erasure (`deleted = true`, `deleted_at = timestamp`, `account_enabled = false`).
   - Personal profile PII permanently erased / anonymized.
   - Email rewritten to `deleted_<id>@deleted.collobster.invalid` (releases email address for re-registration while maintaining unique constraint).
   - Display name rewritten to "Deleted User", username rewritten to `deleted_user_<id>`.
   - Refresh tokens and push devices purged.
   - Realtime presence completely evicted via `MessagingPresenceService.forceOffline(userId)`.
   - Conversation history retained for other conversation participants under the anonymous pseudonym "Deleted User" to preserve thread readability without PII exposure.
   - **Durable Downstream Erasure**: Enqueues an outbox record in `user_data_erasure_outbox` (Flyway V29). Background Java privacy worker dispatches to internal authenticated Python privacy endpoint (`POST /api/v1/internal/recommend/privacy/erase`), which deletes all rows from `recommendation_events` where `user_id = :userId`, purges all user-scoped Redis keys (`user:{userId}:interactions:*`, `online_features:user:{userId}`), and records an offline erasure tombstone in `privacy_erasure_manifest.json`.

---

## 2. Comprehensive Store-by-Store Lifecycle Inventory

| Store / Domain | Production Status | Owner | User Identifier | Purpose | Retention | Erasure Mechanism | Erasure Timing | Verification Test |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`users`** | Active Production | Java Spring Boot | `id` (Long) | Core identity, authentication, profile metadata | Indefinite until deleted | Anonymization: PII nulled/scrubbed, email anonymized, password nulled, `deleted = true` | Synchronous on account deletion | `UserDataErasureOutboxTest` |
| **`refresh_tokens`** | Active Production | Java Spring Boot | `user_id` (Long) | Active authentication session refresh tokens | Until token expiry | Hard deletion (`deleteByUser`) | Synchronous on deletion/logout-all | `AuthenticationIntegrationTest` |
| **`notification_devices`** | Active Production | Java Spring Boot | `user_id` (Long) | Push notification tokens (FCM/APNS) | Until revoked/logged out | Hard deletion (`revokeAllForUser`) | Synchronous on deletion/logout-all | `MultiDeviceLogoutTest` |
| **`user_data_erasure_outbox`** | Active Production | Java Spring Boot | `user_id` (Long) | Outbox audit log for downstream erasure dispatch | Permanent operational audit | Status updated to `COMPLETED` | Retained as operational audit record | `UserDataErasureOutboxTest` |
| **`direct_messages`** | Active Production | Java Spring Boot | `sender_id` (Long) | 1-on-1 private messaging | Retained for recipient | Preserved for other party; sender profile displays as "Deleted User" | Retained | `MessagingServiceTest` |
| **Redis Realtime Presence** | Active Production | Java Spring Boot | `userId` / `sessionId` | Multi-device presence tracking in Redis ZSET | Ephemeral (60s TTL) | Session-scoped via `removeSession(userId, sessionId)` or global via `forceOffline(userId)` | Immediate on disconnect / reaper | `MultiDeviceWebSocketExpiryTest` |
| **`recommendation_events`** | Active Production | Python Rec Engine | `user_id` (BigInteger) | Historical interactions for recommendation scoring | Indefinite until erased | Hard deletion via `DELETE FROM recommendation_events WHERE user_id = :userId` | Synchronous on privacy dispatch | `LivePythonPrivacyErasureIntegrationTest` |
| **Redis Interaction State** | Active Production | Python Rec Engine | `user_id` in key namespace | Realtime interaction signals & affinities | Dynamic rolling window | Namespaced multi-key deletion pipeline (`user:{id}:interactions:*`) | Synchronous on privacy dispatch | `test_recommendation_privacy.py` |
| **Redis Online Features** | Active Production | Python Rec Engine | `user_id` in key namespace | Online feature vector cache for LightGBM | Ephemeral / Active window | Key deletion (`DEL online_features:user:{id}`) | Synchronous on privacy dispatch | `test_recommendation_privacy.py` |
| **Parquet Data Lake** | Active Offline | Python Offline Pipeline | `user_id` (int64 column) | Historical training datasets partitioned by date | Analytical lifecycle (90-365d) | Deletion tombstone in `privacy_erasure_manifest.json` + Atomic partition compaction via `OfflineParquetCompactor` | Synchronous tombstone + asynchronous compaction | `test_offline_privacy.py` |
| **`event_stream.jsonl`** | **Development Only (Option A)** | Python Rec Engine | `user_id` (JSON field) | Local debugging scratch file | Ephemeral dev only | Startup validation blocks in production. Rewritten/compacted via `JsonlRetentionManager` in dev | Immediate in dev | `test_offline_privacy.py` |
| **Model Manifest** | Active Production | Python ML Pipeline | **None (Zero PII)** | Hyperparameters, run IDs, dataset checksums | Permanent model lifecycle | Retained (PII audit verified zero user identifiers) | N/A | `test_offline_privacy.py` |
| **Kafka Event Stream** | **Not Enabled** | Infrastructure | N/A | Optional streaming backend | N/A | Inactive in current production architecture | N/A | Documented Invariant |

---

## 3. Multi-Device WebSocket Token Expiry & Presence Correctness

### A. Root Cause Resolution
Prior to Phase 10.2, STOMP session disconnect or reaper invocation called `MessagingPresenceService.setUserOffline(userId)` directly. In a multi-device scenario (e.g. Phone + Desktop), this improperly cleared ALL active sessions for that user from the Redis ZSET, marking the user OFFLINE across all devices even though the Desktop session's token remained valid.

### B. Session-Scoped Presence Invariant
Phase 10.2 introduces explicit session-scoped presence operations:
- `removeSession(Long userId, String sessionId)`
- `handleSessionDisconnected(Long userId, String sessionId)`

When a single STOMP session expires (e.g., Phone):
1. **Transport Closed**: The Phone's `WebSocketSession` is closed with status code 4401 ("Access token expired").
2. **Registry Evicted**: Only the Phone's session entry is removed from `WebSocketAuthenticatedSessionRegistry`.
3. **Session Record Pruned**: `handleSessionDisconnected(userId, sessionId)` removes only that specific session key from the user's Redis presence ZSET.
4. **Presence Evaluation**: The presence service and reaper evaluate remaining active sessions for `userId`:
   - If `remainingSessions > 0`: User presence remains **ONLINE**.
   - If `remainingSessions == 0`: User presence transitions to **OFFLINE**.
5. **Outbound Interception**: `WebSocketOutboundExpiryInterceptor` drops outbound messages destined for the expired Phone session, but leaves outbound messages destined for the Desktop session completely unhindered.
6. **Inbound Enforcement**: The expired Phone session cannot `SEND` messages or `SUBSCRIBE` to destinations.

### C. Account-Wide `forceOffline` Preservation
The account-wide method `forceOffline(userId)` is strictly preserved for true account-wide administrative and security events:
- Account deletion
- Account deactivation
- `POST /api/v1/auth/logout-all`
- Administrator suspension

It is strictly prohibited from being called for single-device token expiry, single network disconnect, or transport socket close.

---

## 4. Multi-Device Logout Semantics

1. **Current-Device Logout (`POST /api/v1/auth/logout`)**:
   - Revokes only the caller's active refresh token cookie.
   - If `deviceId` is provided in the request body, validates device ownership and revokes only that device's push notification token (`notification_devices`).
   - Other registered devices continue receiving push notifications.
   - *Limitation Documentation*: Because REST HTTP requests and WebSocket STOMP transport connections operate over decoupled network layers, standard REST logout without an associated STOMP session ID does not terminate unrelated WebSocket connections. Clients closing their app disconnect their local STOMP transport directly.
2. **Global Logout (`POST /api/v1/auth/logout-all`)**:
   - Revokes ALL refresh tokens for the user (`deleteByUser`).
   - Revokes ALL registered push notification devices for the user (`revokeAllForUser`).
   - Clears realtime presence across all devices (`MessagingPresenceService.forceOffline`).
   - Clears authentication session cookies.

---

## 5. Offline Recommendation Data Erasure Lifecycle

### A. Two-Stage Erasure Architecture
1. **Stage 1: Online Synchronous Erasure & Tombstone Dispatch**:
   - Java outbox worker calls Python `POST /api/v1/internal/recommend/privacy/erase`.
   - Python deletes rows in `recommendation_events` where `user_id = :userId`.
   - Python purges user-scoped Redis interaction and online feature keys.
   - Python records an erasure tombstone in `data_lake/privacy_manifests/privacy_erasure_manifest.json` with `status: "PENDING"`.
   - Endpoint returns HTTP 200 with truthful status reporting (`postgresEventsDeleted`, `redisKeysDeleted`, `offlineErasureScheduled: true`, `offlineErasureStatus: "PENDING"`).
   - Java marks the outbox job `COMPLETED`.
2. **Stage 2: Offline Partition Compaction & Training Guards**:
   - **Training-Time Guard**: Before physical compaction occurs, `TrainingDataLoader` checks `PrivacyErasureManifest.get_tombstoned_user_ids()` and automatically filters out any tombstoned users from datasets. Newly trained models can never consume data requested for erasure.
   - **Atomic Parquet Compaction**: Scheduled compaction worker (`OfflineParquetCompactor`) scans Parquet partition files, removes rows for tombstoned users using `pyarrow.compute`, writes to atomic `.tmp` files, and renames them over the original files.
   - Once all affected partitions are rewritten, the manifest entry is marked `status: "COMPLETED"`.
   - Compaction is idempotent and retry-safe. If compaction encounters an error, it is recorded as `status: "FAILED"` with `error_message`, remaining visible for operator inspection.

### B. Machine Learning Model Artifacts
- The LightGBM model artifact (`lgbm_ranker_model_v2.txt`, SHA-256: `25944E97AFE3194DF82AD2EAE9FF738DEF2456D1EEC929B267BF663D259C5B0B`) is an immutable mathematical decision forest of feature split thresholds.
- No model retraining is performed during individual user account deletion requests.
- Future model training runs exclude erased users via `TrainingDataLoader` and compacted Parquet datasets.
