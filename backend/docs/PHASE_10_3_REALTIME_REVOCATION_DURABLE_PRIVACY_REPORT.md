# PHASE 10.3 / 10.3.1 / 10.3.2: ACCOUNT-WIDE REALTIME SESSION REVOCATION + SHARED DURABLE PRIVACY TOMBSTONES HARDENING REPORT

## Executive Summary
Phase 10.3 and Phase 10.3.1 deliver the final distributed security and privacy engineering hardening prior to Phase 11. It resolves critical distributed systems, session revocation, and data lake privacy race conditions:
1. **Immediate Account-Wide Realtime Session Revocation & Version-Aware Invalidation**: Closes the security gap where account deactivation, deletion, suspension, or `logout-all` failed to immediately terminate active WebSocket/STOMP sessions across distributed cluster nodes. Implemented durable authoritative session generation in PostgreSQL (`users.auth_version`), fast broadcast invalidation via Redis pub/sub (`collobster:security:account-revocation`), version-aware session pruning (`closeSessionsOlderThanAuthVersion`), safe per-session presence eviction, and bounded periodic reconciliation fallback via `WebSocketSessionReaper` (guaranteeing revocation within 5 seconds even under total Redis outage).
2. **REST JWT & Refresh Token Auth Version Authority**: Closes token bypass vectors by strictly validating `tokenVersion == currentAuthVersion` in `JwtService.isTokenValid` and `AuthenticationServiceImpl.refreshToken`. Missing claims are mapped to `0L`, preventing null-bypass on accounts with advanced session versions. Stale refresh tokens are actively deleted from PostgreSQL and rejected with HTTP 400 Bad Request.
3. **Shared Durable Offline Recommendation Privacy Tombstones & Race-Free Compaction**: Replaced node-local JSON file dependency (`privacy_erasure_manifest.json`) with a durable, multi-instance PostgreSQL table `recommendation_privacy_erasure`. Implemented concurrent multi-worker claiming with `SELECT ... FOR UPDATE SKIP LOCKED`, lease expiration crash recovery (300s timeout), worker lease fencing, true exponential retry backoff (`delay = min(60 * (2 ** (attempt - 1)), 3600)`), and pre-compaction training guards (`TrainingDataLoader`) strictly excluding all tombstoned statuses (`PENDING`, `IN_PROGRESS`, `RETRY`, `FAILED`, `COMPLETED`).
4. **Shared Parquet Partition Race Resolution**: Implemented partition- and file-level serialization via PostgreSQL 64-bit advisory locks (`pg_advisory_lock` on the canonical partition path hash) combined with in-process thread locks. Compaction workers re-read partition contents under lock, filter claimed users in batch, write to unique worker/operation temp files (`<filename>.tmp.<workerId>.<opId>`), and atomically replace target files via `os.replace`, with automated cleanup of stale temp files on startup.

---

## 1. Root Security Problem & Solution Architecture

### The Pre-10.3 Security & Privacy Vulnerabilities
Prior to Phase 10.3 / 10.3.1:
- In Phase 10.2, JWT token natural expiry was properly scoped to individual sessions.
- However, if an account was **deactivated**, **deleted**, **suspended by an admin**, or if the user invoked **`logout-all`**:
  - Existing WebSocket transport connections remained connected until their JWT naturally expired (up to 15-60 minutes).
  - Outbound STOMP messages continued to be pushed down open WebSocket connections.
  - Inbound STOMP frames (`SEND`, `SUBSCRIBE`) were accepted without checking account lifecycle or revocation state.
  - Multi-node clusters had no mechanism to inform other application nodes that an account was revoked.
- In distributed environments, out-of-order Redis pub/sub delivery could allow a stale revocation event to evict presence or disconnect sessions belonging to a newer, legitimate login.
- Offline privacy tombstones in ephemeral JSON could be lost on container restart, and concurrent compaction workers could overwrite shared Parquet partitions without file-level serialization.

### Phase 10.3 / 10.3.1 Solution Architecture
```text
[ Client Action ] (logoutAll, deactivate, delete, suspend)
       │
       ▼
[ AccountRevocationCoordinator ]
       │
       ├─── 1. PostgreSQL (Durable Authority): Atomically increments User.authVersion
       │       `UPDATE users SET auth_version = auth_version + 1 WHERE id = :userId`
       │
       ├─── 2. Local Session Registry: Immediately marks sessions revoked & closes WebSockets
       │       `sessionRegistry.closeAllSessionsForUser(userId, reason)`
       │       (Socket closed with CloseStatus 4401 "Account session revoked")
       │
       ├─── 3. Local & Cluster Presence: Immediately evicts presence
       │       `presenceService.forceOffline(userId)`
       │
       └─── 4. Redis Pub/Sub (Fast Broadcast): Broadcasts invalidation signal across nodes
               `PUBLISH collobster:security:account-revocation {"userId":..., "authVersion":...}`
                     │
                     ▼
               [ Remote Cluster Nodes (AccountRevocationListener) ]
                     │
                     ├── Version Check: session.authVersion < event.newAuthVersion
                     ├── Target Closure: closeSessionsOlderThanAuthVersion(...)
                     └── Safe Presence Eviction: handleSessionDisconnected(userId, sessionId)
```

### Distributed Consistency Boundaries & Eventual Consistency
The Collobster platform establishes clear consistency boundaries without false cross-system ACID claims:
- **PostgreSQL**: Strong ACID authority for durable relational state, user credentials, monotonic `auth_version` generation counters, and `recommendation_privacy_erasure` tombstones.
- **Redis Pub/Sub**: Best-effort, low-latency invalidation broadcast. It does not provide durable queuing or two-phase commit.
- **WebSocket Session Reaper**: Enforces bounded eventual consistency (within 5 seconds) across all cluster nodes by actively querying PostgreSQL authority (`findAuthStatesByUserIds`), guaranteeing full revocation even under complete Redis network partitions or broker failure.
- **Parquet Data Lake Compaction**: Enforces partition-level serialization via PostgreSQL 64-bit advisory locks (`pg_advisory_lock`) and thread-level mutexes, guaranteeing linearizable partition updates and crash-resilient atomic file replacement (`os.replace`) without distributed transactions.

---

## 2. Java Implementation Details

### A. Database Migration (Flyway V30)
- File: `V30__add_user_auth_session_version.sql`
- Adds column: `auth_version BIGINT NOT NULL DEFAULT 0` to `users` table.
- Index: `CREATE INDEX idx_users_auth_version ON users(id, auth_version)`.
- DDL Locking Behavior: Expected to be metadata-only/no full table rewrite for a constant default on supported PostgreSQL versions, but deployment must still account for brief DDL locking (`ACCESS EXCLUSIVE`) during the schema migration.
- Verified applied on PostgreSQL port 5433 (`collabster`).

### B. Core Entities & Repositories
- **`User.java`**: Added `@Builder.Default private Long authVersion = 0L;` with `@PrePersist` fallback.
- **`UserRepository.java`**:
  - `@Modifying(clearAutomatically = true, flushAutomatically = true) incrementAuthVersion(Long userId)`
  - `findAuthVersionByUserId(Long userId)`
  - `findAuthStatesByUserIds(Collection<Long> userIds)` returning `UserAuthStateProjection` (`id`, `authVersion`, `accountEnabled`, `deactivated`, `deleted`).
- **`UserPrincipal.java`**: Exposes `getAuthVersion()`.
- **`auth_version` Semantics**: Durable authoritative session generation/version (a mutable monotonic counter incremented upon account revocation events: `logout-all`, `deactivate`, `delete`, `suspend`).
- **`JwtService.java`**:
  - Encodes `auth_version` claim into access and refresh tokens.
  - Decodes claim via `extractAuthVersion(token)`.
  - In `isTokenValid`: Strictly enforces `tokenVersion == currentAuthVersion`. Missing claims are mapped to `0L` (preventing null bypass when `authVersion > 0`).
- **`AuthenticationServiceImpl.java`**:
  - In `refreshToken`: Strictly validates `tokenVersion == currentAuthVersion`. If outdated, deletes the refresh token from PostgreSQL and throws `IllegalArgumentException` (HTTP 400 Bad Request via `AuthExceptionHandler`), with `@Transactional(noRollbackFor = {IllegalArgumentException.class})` ensuring the revocation commit persists.

### C. WebSocket Session Registry & Interceptors
- **`WebSocketSessionMetadata.java`**: Stores authenticated `authVersion` and volatile `revoked` flag.
- **`WebSocketAuthenticatedSessionRegistry.java`**:
  - Added concurrent `revokedSessionIds` map with 30-minute TTL to defend against delivery race conditions.
  - `closeAllSessionsForUser(userId, reason)`: Marks all sessions revoked and closes transport sockets with code 4401.
  - `closeSessionsOlderThanAuthVersion(userId, minimumRequiredAuthVersion, reason)`: Closes only sessions with `sessionVersion < minimumRequiredAuthVersion`, shielding newer logins from stale remote events.
  - `isRevoked(sessionId)`: Checks both active session metadata and `revokedSessionIds` cache.
- **STOMP Inbound Interceptors**:
  - `MessagingStompAuthorizationInterceptor.java`
  - `CommunityStompAuthorizationInterceptor.java`
  - `NotificationStompAuthorizationInterceptor.java`
  - On `CONNECT`: Captures token `auth_version` and registers with session.
  - On `SEND` / `SUBSCRIBE`: Checks `sessionRegistry.isRevoked(sessionId)`; if true, aborts immediately with `IllegalArgumentException("Session has been revoked")`.
- **`WebSocketOutboundExpiryInterceptor.java`**:
  - Intercepts outbound STOMP message frames.
  - If `sessionRegistry.isRevoked(sessionId)` is true, drops message, closes transport socket, and prunes presence.

### D. Multi-Node Coordination & Lifecycle Wiring
- **`AccountRevocationCoordinator.java`**: Centralized service coordinating DB increment, local session closure (`closeSessionsOlderThanAuthVersion`), presence eviction, and Redis pub/sub publication after transaction commit (`afterCommit`). Local revocation strictly uses `sessionVersion < newAuthVersion` to avoid closing newly established concurrent sessions created after the version increment. Handles Redis unavailability gracefully without failing the transaction.
- **`AccountRevocationListener.java`**:
  - Subscribed to Redis channel `collobster:security:account-revocation`.
  - Validates payload integrity (`userId != null`, `authVersion != null && authVersion >= 0`); discards malformed payloads with warning logs.
  - Calls `closeSessionsOlderThanAuthVersion(userId, newAuthVersion, reason)`.
  - Evicts presence on closed sessions via `handleSessionDisconnected(userId, sessionId)` without calling `forceOffline(userId)`, avoiding destructive disconnects for active newer sessions.
- **`WebSocketSessionReaper.java`**: Includes `reapRevokedSessions()` performing batch reconciliation queries against PostgreSQL every 5 seconds.
- **Service Integration**:
  - `AuthenticationServiceImpl.logoutAll`: Calls `accountRevocationCoordinator.revokeAccountSessions(user.getId(), "Logged out from all devices")`. Normal single-device logout remains device-scoped and does not advance `authVersion`.
  - `AccountLifecycleServiceImpl.deactivateAccount`: Calls `accountRevocationCoordinator.revokeAccountSessions(userId, "Account deactivated")`.
  - `AccountLifecycleServiceImpl.deleteAccount`: Calls `accountRevocationCoordinator.revokeAccountSessions(userId, "Account deleted")`.
  - `AccountLifecycleServiceImpl.suspendAccount`: Calls `accountRevocationCoordinator.revokeAccountSessions(targetUserId, "Account suspended: " + reason)`.

---

### E. Auth Version Equality & Semantics Gate (Phase 10.3.2 Hardening)
- **Authoritative Comparison Rule**:
  - `sessionVersion < requiredAuthVersion => REVOKE`
  - `sessionVersion == requiredAuthVersion => KEEP`
  - `sessionVersion > requiredAuthVersion => KEEP`
- **Documentation Clarification**: Remote revocation strictly uses `<` (never `<=`, never `!=`). `sessionVersion == eventVersion` is VALID and preserved. Any prior documentation wording stating that equality closes matching sessions was purely a documentation erratum; production code (`WebSocketAuthenticatedSessionRegistry.closeSessionsOlderThanAuthVersion`) and tests have always enforced strict `<`.
- **Presence Eviction**: If valid sessions `>= requiredAuthVersion` remain for the user, user presence remains **ONLINE**. Only closed session IDs are pruned.
- **Origin-Node Local Revocation**: DB version increments first; only sessions belonging to previous generations (`< newAuthVersion`) are closed; newly established concurrent sessions are preserved.
- **Reaper Rule**: `WebSocketSessionReaper` uses `session.authVersion < db.authVersion` (never equality; equal version is valid).
- **Legacy Sessions**: A session without an `auth_version` claim defaults to `0L`. When DB `auth_version == 0`, it remains valid; when DB `auth_version > 0`, it is stale and revoked.

---

## 3. Shared Durable Offline Recommendation Privacy Tombstones

### The Pre-10.3 Offline Privacy Gap
Prior to Phase 10.3 / 10.3.1:
- In Phase 10.2, offline Parquet compaction was tracked in a local JSON file (`data_lake/privacy_manifests/privacy_erasure_manifest.json`).
- If multiple recommendation workers or containers ran, local file state was not shared.
- If a container crashed or restarted with an ephemeral filesystem, pending tombstones were lost.
- Multiple compaction workers could collide on the same partitions without distributed locking or fencing.

### Phase 10.3 / 10.3.1 Solution Architecture
- **PostgreSQL Durable Authority**: Created table `recommendation_privacy_erasure` in PostgreSQL:
  ```sql
  CREATE TABLE IF NOT EXISTS recommendation_privacy_erasure (
      id BIGSERIAL PRIMARY KEY,
      user_id BIGINT NOT NULL,
      status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
      requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
      started_at TIMESTAMP WITH TIME ZONE,
      completed_at TIMESTAMP WITH TIME ZONE,
      attempt_count INTEGER NOT NULL DEFAULT 0,
      next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
      lock_owner VARCHAR(128),
      locked_at TIMESTAMP WITH TIME ZONE,
      rows_removed BIGINT NOT NULL DEFAULT 0,
      affected_partitions TEXT,
      last_error_code VARCHAR(64),
      last_error_message TEXT,
      created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
  );
  CREATE UNIQUE INDEX IF NOT EXISTS uq_recommendation_privacy_user_id ON recommendation_privacy_erasure(user_id);
  CREATE INDEX IF NOT EXISTS idx_recommendation_privacy_status_schedule ON recommendation_privacy_erasure(status, next_attempt_at);
  ```
- **Concurrency & Claiming via `SELECT ... FOR UPDATE SKIP LOCKED`**:
  - Compaction workers claim batches of pending tombstones atomically.
  - Leases expire after 300 seconds; crashed workers automatically forfeit leases to healthy workers.
  - Compaction completion enforces worker lease fencing (`WHERE user_id = :userId AND lock_owner = :workerId`).
  - True exponential backoff schedule on failure: `delay = min(60 * (2 ** (attempt - 1)), 3600)`:
    - Attempt 1: 60s
    - Attempt 2: 120s
    - Attempt 3: 240s
    - Attempt 4: 480s
    - Attempt 5: 960s (capped at 3600s, max 5 attempts before permanent `FAILED` status).
- **Hardened Pre-Compaction Training Guards (`TrainingDataLoader`)**:
  - Queries `PrivacyTombstoneRepository.get_training_excluded_user_ids()` in PostgreSQL before reading Parquet partitions.
  - Automatically filters out all users with status in `["PENDING", "IN_PROGRESS", "RETRY", "FAILED", "COMPLETED"]`.
  - Guaranteed: no model can be trained on tombstoned user data even if physical Parquet compaction has failed, is in retry, or has not yet completed.
- **Shared Parquet Partition Race Resolution & Advisory Lock Lifecycle (Phase 10.3.2 Hardening)**:
  - Implemented file- and partition-level serialization using PostgreSQL 64-bit advisory locks (`pg_advisory_lock`) combined with local in-process thread mutexes.
  - **Deterministic Key Derivation**: Canonical partition path is normalized (`Path(p).resolve().as_posix().lower()`), hashed with SHA-256, and mapped to signed 64-bit integer (`struct.unpack(">q", digest[:8])[0]`) in PostgreSQL `bigint` range. Zero reliance on non-deterministic Python `hash()`.
  - **PostgreSQL Connection Pinning**: The exact database connection (`conn = db_session.connection()`) is pinned for the entire critical section (acquire -> re-read -> filter -> write temp -> atomic replace -> unlock). Unlock is unconditionally executed in a `finally` block, preventing lock leakage into pooled connections on any error or exception.
  - **Cross-Process Correctness**: Advisory lock functions across separate processes/connections independent of local `threading.Lock`.
  - **Safe Stale Temp-File Cleanup**: `cleanup_stale_temp_files(max_age_seconds=3600)` protects active worker temp files by attempting a non-blocking `try_partition_lock` on the target partition and checking `is_worker_lease_active` in DB. If the partition lock is held or worker lease is active, the temp file is preserved regardless of age. Inactive orphaned temp files older than threshold are cleaned safely.
  - Compaction protocol:
    1. Acquire advisory lock (and optional thread lock).
    2. Re-read latest Parquet partition contents from disk.
    3. Filter all claimed user IDs in a single pass.
    4. Write updated table to unique worker/operation temp file (`<filename>.tmp.<workerId>.<opId>`).
    5. Atomically replace target Parquet file via `os.replace`.
    6. Release lock in `finally` block.
- **Operational Metrics Endpoint**:
  - `GET /api/v1/internal/recommend/privacy/metrics` protected by internal service bearer token.
  - Returns aggregate counts (`pending`, `in_progress`, `retry`, `failed`, `completed`) and `oldest_pending_age_seconds`.

---

## 4. Verification Test Results Summary

| Suite / Test Profile | Tests Discovered / Run | Passed | Failed | Errors | Skipped | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Java Maven Full Test Suite** (`mvnw.cmd test`) | **567** | **551** | **0** | **0** | **16** | **BUILD SUCCESS** |
| - `AccountWideSessionRevocationTest` (Phase 10.3 / 10.3.1 / 10.3.2) | 26 | 26 | 0 | 0 | 0 | **PASS** |
| - `PlatformSecurityIntegrationTest` (Phase 10.3.1) | 8 | 8 | 0 | 0 | 0 | **PASS** |
| - `MultiDeviceWebSocketExpiryTest` (Phase 10.2) | 2 | 2 | 0 | 0 | 0 | **PASS** |
| - `WebSocketTokenExpiryTest` | 8 | 8 | 0 | 0 | 0 | **PASS** |
| - `AccountLifecycleIntegrationTest` | 5 | 5 | 0 | 0 | 0 | **PASS** |
| - `MultiDeviceLogoutTest` | 5 | 5 | 0 | 0 | 0 | **PASS** |
| - `FeedControllerTest` | 6 | 6 | 0 | 0 | 0 | **PASS** |
| - `LivePythonPrivacyErasureIntegrationTest` | 1 | 1 | 0 | 0 | 0 | **PASS** |
| - `LivePythonRecommendationIntegrationTest` | 1 | 1 | 0 | 0 | 0 | **PASS** |
| - `LivePythonFeedbackIntegrationTest` | 16 | 16 | 0 | 0 | 0 | **PASS** |
| **Python Pytest Suite** (`python -m pytest -q`) | **99** | **99** | **0** | **0** | **0** | **PASS** (3.12s) |
| - `test_privacy_compaction_hardening.py` (Phase 10.3.1 / 10.3.2) | 14 | 14 | 0 | 0 | 0 | **PASS** |
| - `test_durable_privacy_tombstones.py` (Phase 10.3) | 6 | 6 | 0 | 0 | 0 | **PASS** |
| - `test_live_offline_privacy.py` (Phase 10.3) | 1 | 1 | 0 | 0 | 0 | **PASS** |
| - `test_offline_privacy.py` | 6 | 6 | 0 | 0 | 0 | **PASS** |
| - `test_recommendation_privacy.py` | 4 | 4 | 0 | 0 | 0 | **PASS** |
| **Model SHA-256 Checksum** | 1 | 1 | 0 | 0 | 0 | **25944E97AFE3194DF82AD2EAE9FF738DEF2456D1EEC929B267BF663D259C5B0B (EXACT MATCH)** |
| **Flyway Database Migration Version** | 1 | 1 | 0 | 0 | 0 | **V30 Applied** |
