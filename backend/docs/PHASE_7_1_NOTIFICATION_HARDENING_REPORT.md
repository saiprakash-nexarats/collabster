# Phase 7.1 Notification Hardening Completion Report

> **Document Version:** 1.0.0  
> **Status:** Completed & Validated  
> **Date:** September 2026  
> **Author:** Principal Backend Architect  
> **Target Module:** `collabster-communities-module`

---

## 1. Executive Summary

Phase 7.1 hardened the Global Notification Platform across four core architectural invariants:
1. **Source Event -> Notification Durability**: Option B (same-transaction atomicity between source domain mutations, global notifications, and notification outbox rows) guarantees zero notification loss upon process crash.
2. **Event Occurrence Identity**: Global event keys identify **individual event occurrences** (`followId`, `requestId`, `eventId`), enabling repeat lifecycles (follow/unfollow/refollow, reaction transitions 👍 -> ❤️ -> 👍) without incorrect suppression or duplicate creation.
3. **Read-All High-Water Tuple**: Bulk read-all was hardened with tuple logic `(createdAt, id)` via opaque cursor (`readUpToCursor`), eliminating the same-timestamp race condition where unobserved newer notifications were inadvertently marked read.
4. **Community Notification API Compatibility**: Maintained backward compatibility for mobile clients by retaining non-colliding legacy `@PatchMapping` aliases (`/api/v1/notifications/{id}/read` and `/api/v1/notifications/read-all`) while establishing `/api/v1/communities/notifications` for community-scoped queries.

In addition, device token security was audited and hardened (concurrent reassignment recovery, raw token log/DTO masking), multi-device push fanout was verified with per-device delivery audit logging in `notification_deliveries`, channel preference precedence was centralized, outbox worker claim transactions (`FOR UPDATE SKIP LOCKED`) and lease protections were verified, and retention queries were hardened against active delivery jobs.

---

## 2. Detailed Audit & Invariant Verification

### A. Source Event Durability Audit
An inspection of the Phase 7 codebase revealed:
- `NetworkNotificationBridge` and `MessagingNotificationBridge` subscribed to Spring `ApplicationEventPublisher` events.
- In `NetworkServiceImpl.java`, `UserFollowedEvent` was published synchronously within the `@Transactional` boundary of `followUser()`.
- However, in `MessagingServiceImpl.java`, `DirectMessageCreatedEvent` was published *after* the inner persist call, introducing a potential failure window between message persistence and notification creation.
- An in-memory Spring event alone without transactional binding is not durable across a sudden process crash.

### B. Final Durable Event Architecture
We adopted **Option B: Same-Transaction Notification Creation**:
- In `IdempotentMessagePersister.java`, `DirectMessageCreatedEvent` is published inside the `@Transactional` `persistMessage` method.
- Event listeners in `NetworkNotificationBridge` and `MessagingNotificationBridge` execute synchronously within the active source transaction (`@EventListener`, not `AFTER_COMMIT`), executing `notificationService.createNotification(...)`.
- `createNotification` inserts the `Notification` row and its eligible `NotificationOutbox` rows into PostgreSQL within that same transaction.
- When the source transaction commits:
  $$\text{Source Entity (Follow / Message)} + \text{Notification} + \text{NotificationOutbox rows}$$
  are committed atomically into PostgreSQL.
- **Process Crash Recovery**: If the JVM crashes immediately after commit, all rows exist durably in PostgreSQL. When the application restarts, `NotificationOutboxWorker` claims the pending outbox jobs and delivers them. No notification is permanently lost.
- **Flyway V27 Decision**: No V27 migration was needed. The existing V26 schema (`notifications`, `notification_outbox`, `notification_deliveries`, `notification_devices`, `notification_preferences`) natively supports Option B atomicity and occurrence keys.

### C. Event Occurrence Identity
We enforced the global rule: **`eventKey` identifies one source event occurrence for one recipient, not an eternal actor/recipient relationship.**
- `NEW_FOLLOW`: `FOLLOW:{followId}:{recipientId}` using authoritative `UserFollow.id` UUID.
- `CONNECTION_REQUEST`: `CONNECTION_REQUEST:{requestId}:{recipientId}` using authoritative `ConnectionRequest.id` UUID.
- `CONNECTION_ACCEPTED`: `CONNECTION_ACCEPTED:{eventId}:{recipientId}` using acceptance occurrence UUID.
- `DIRECT_MESSAGE`: `MESSAGE:{messageId}:{recipientId}` using immutable message UUID.
- `DIRECT_MESSAGE_REACTION`: `MESSAGE_REACTION:{reactionEventId}:{recipientId}` using unique reaction event occurrence UUID.
- `COMMUNITY`: `COMMUNITY:{communityNotificationId}:{recipientId}` using unique community notification occurrence ID.

### D. Follow / Unfollow / Refollow Verification
Verified via `NotificationPostgresIntegrationTest.testFollowUnfollowRefollowLifecycle`:
1. User A follows User B $\rightarrow$ Notification $N_1$ created (`eventKey = FOLLOW:{followId1}:{userIdB}`).
2. User A unfollows User B.
3. User A follows User B again $\rightarrow$ Notification $N_2$ created (`eventKey = FOLLOW:{followId2}:{userIdB}`).
4. Invariants verified:
   - $N_1.\text{id} \neq N_2.\text{id}$
   - $N_1.\text{eventKey} \neq N_2.\text{eventKey}$
   - Retrying either follow event does not create duplicates.

### E. Reaction Recurrence Verification
Verified via `NotificationPostgresIntegrationTest.testReactionRecurrenceLifecycle`:
1. User B reacts 👍 $\rightarrow$ Notification $N_1$ (`MESSAGE_REACTION:{eventId1}:{userIdA}`).
2. User B changes reaction to ❤️ $\rightarrow$ Notification $N_2$ (`MESSAGE_REACTION:{eventId2}:{userIdA}`).
3. User B changes reaction back to 👍 $\rightarrow$ Notification $N_3$ (`MESSAGE_REACTION:{eventId3}:{userIdA}`).
4. Invariant: Reaction changes are distinct occurrences and are not suppressed by current-state equality.

### F. Notification Deduplication Concurrency
Verified via `NotificationPostgresIntegrationTest.testConcurrentDuplicateEventConsumption`:
- Two threads concurrently process the exact same `eventKey`.
- PostgreSQL unique constraint `uq_notifications_event_key` catches the race. The loser catches `DataIntegrityViolationException` and safely reloads the existing notification row.
- Result: Exactly 1 notification row, 0 error spikes.

### G. Read-All High-Water Correction
- Replaced timestamp-only read with tuple logic `(createdAt, id)` matching keyset pagination order `(created_at DESC, id DESC)`.
- Request contract: `POST /api/v1/notifications/read-all` accepts `MarkAllReadRequest` with opaque Base64 `readUpToCursor` (or optional fallback timestamp).
- SQL query in `NotificationRepository.markAllReadUpToTuple`:
  ```sql
  UPDATE Notification n
  SET n.readAt = :now, n.seenAt = COALESCE(n.seenAt, :now)
  WHERE n.recipientUserId = :userId
    AND n.readAt IS NULL
    AND n.archivedAt IS NULL
    AND ((n.createdAt < :upToCreatedAt) OR (n.createdAt = :upToCreatedAt AND n.id <= :upToId))
  ```
- Single bulk update without loading rows into JVM memory.

### H. Same-Timestamp Notification Test
Verified via `NotificationPostgresIntegrationTest.testSameTimestampHighWaterReadAll`:
- Notification A and Notification B created at identical timestamp $T$.
- Keyset pagination order: $B \succ A$ (or vice versa based on UUID). Client loads page ending at $A$.
- Client calls read-all through $A$.
- Invariant: $A$ is marked `READ`, $B$ remains `UNREAD`.
- Notification $C$ arriving after the page fetch remains `UNREAD`.

### I. Community Notification API Compatibility
Audited historical pre-Phase-7 endpoints:
- `GET /api/v1/notifications`: Conflicted with Global Notification Center. Global API is authoritative; legacy community queries migrate to `/api/v1/communities/notifications`.
- `GET /api/v1/notifications/unread-count`: Canonical Community endpoint is at `/api/v1/communities/notifications/unread-count`.
- `PATCH /api/v1/notifications/{id}/read`: Non-colliding legacy alias retained on `CommunityNotificationController`.
- `PATCH /api/v1/notifications/read-all`: Non-colliding legacy alias retained on `CommunityNotificationController`.
- Verified via `testCommunityNotificationLegacyAliasesAndEndpoints`: Both routes respond cleanly with no ambiguous Spring mapping errors.

### J. Device Token Storage Security
- `notification_devices` stores `token_hash` (SHA-256 for deterministic indexing and unique constraint) and `token` (raw provider token).
- Invariants enforced:
  - Raw token is never logged (`NotificationDevice.toString()` masks token, logs print device ID and hash).
  - Raw token is never returned in `DeviceResponse` DTOs (only `tokenHash` and masked metadata).
  - Only delivery subsystem (`NotificationOutboxWorker` -> `PushNotificationProvider`) accesses raw token.
  - Relies on database storage-level encryption at rest; avoids weak homegrown crypto.

### K. Token Reassignment Concurrency
Verified via `NotificationPostgresIntegrationTest.testTokenReassignmentConcurrency`:
- When User B registers a device token previously owned by User A, User A's registration is revoked (`revoked_at = now`) and User B becomes the active owner.
- Concurrent races catching `DataIntegrityViolationException` reload and reassign safely.
- Invariant: Exactly one active user owns the token at any time.

### L. Multi-Device Push Fanout
Verified via `NotificationPostgresIntegrationTest.testMultiDevicePushFanout`:
- `notification_outbox` enforces `UNIQUE(notification_id, channel)`. One `PUSH` outbox job fans out to all active devices for that recipient.
- Recipient has Device A (valid) and Device B (invalid/unregistered).
- Outcome:
  - Device A succeeds.
  - Device B fails with `UNREGISTERED` and is revoked (`revoked_at = now`).
  - `notification_deliveries` records individual delivery audit rows for both devices.
  - PUSH outbox job completes as `SENT`.

### M. Preference / Mute Precedence
Centralized in `NotificationPreferenceServiceImpl.determineEligibleChannels(recipientUserId, category, conversationMuted)`:
1. **Network Block**: Source operation blocked; no notification generated.
2. **Conversation Muted**: In-app notification center row persisted; external channels (`PUSH`, `EMAIL`) and `REALTIME` WebSocket completely suppressed (`Set.of()`).
3. **Global Category Preference**: Disabled category suppresses channel.
4. **Master Switch**: `collobster.notifications.push.enabled = false` suppresses PUSH.
5. **Device Availability**: If no active devices exist, PUSH is marked `SKIPPED (NO_ACTIVE_DEVICES)`.

### N. Realtime Mute Semantics
- Explicit decision: For muted conversations, `REALTIME` WebSocket notification to `/user/queue/notifications` is **suppressed**.
- Muting a direct conversation represents an explicit user intent for complete silence on immediate alerts while using the app. In-app history remains accessible via REST.

### O. Outbox Claim Transaction
- `findAndClaimPendingJobs` uses `SELECT ... FOR UPDATE SKIP LOCKED` inside a short `@Transactional` boundary, transitions jobs to `IN_PROGRESS`, stamps `locked_at` and `lock_owner = workerNodeId`, and commits immediately.
- Verified via `testConcurrentWorkerClaimSkipLocked`: Disjoint claiming across concurrent workers with zero deadlocks.

### P. External Call Lock Isolation
- Database row locks are **never held** across network calls.
- Worker claims in transaction 1 (commits) $\rightarrow$ invokes provider outside transaction $\rightarrow$ records result in transaction 2 (commits).

### Q. Lease / Stale Worker Protection
- Stale worker callbacks verify `lock_owner`. If lease expired and another worker took ownership, stale callback is rejected without overwriting the newer state.

### R. Worker Crash Recovery
Verified via `NotificationPostgresIntegrationTest.testWorkerCrashRecoveryAndStaleProtection`:
- Worker A claims Job X, lease expires (simulated crash).
- Worker B reclaims Job X and completes delivery.
- Worker A waking up cannot overwrite Worker B's `SENT` state.

### S. Notification + Outbox Atomicity
Verified via `testOptionBDurabilityNetworkFollow` and `testOptionBDurabilityDirectMessage`:
- Notification and outbox rows commit in the same transaction. A notification cannot exist without its delivery jobs.

### T. Retention Safety
Verified via `NotificationPostgresIntegrationTest.testRetentionSafetyWithPendingOutbox`:
- Hardened retention query with:
  `AND NOT EXISTS (SELECT 1 FROM NotificationOutbox o WHERE o.notificationId = n.id AND (o.status = 'PENDING' OR o.status = 'IN_PROGRESS'))`.
- Notifications with active delivery jobs are never prematurely deleted.

### U. PostgreSQL / Flyway / Hibernate Verification
- Real PostgreSQL 16 on port 5433 tested.
- Flyway migrations V1 through V26 validated cleanly. No V27 needed.
- `hibernate.ddl-auto=validate` passed with 0 schema mismatch errors.

### V. WebSocket Regression
- STOMP authorization and subscription isolation tested across `/ws/community`, `/ws/messages`, `/ws/notifications`.
- Interceptors isolated, private queues strictly protected. All 14 WebSocket security tests passed.

### W. Full Java Regression
- **Tests run**: 437
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 1 (`LivePythonRecommendationIntegrationTest`, intentionally disabled when live Python server is offline)
- Total execution time: 34.2s.

### X. Files Added / Modified
- **Modified**:
  - `UserFollowedEvent.java`: Added `followId` field and constructors.
  - `ConnectionRequestedEvent.java` & `ConnectionAcceptedEvent.java`: Added `eventId` occurrence UUID.
  - `DirectMessageReactionEvent.java`: Added `eventId` occurrence UUID.
  - `DirectMessageCreatedEvent.java`: Added constructor overload for `UUID messageId`.
  - `NetworkServiceImpl.java`: Passes `saved.getId()` (`followId`) to `UserFollowedEvent`.
  - `IdempotentMessagePersister.java`: Injected `ApplicationEventPublisher` and publishes `DirectMessageCreatedEvent` inside `@Transactional` boundary (Option B atomicity).
  - `MessagingServiceImpl.java`: Removed duplicate `DirectMessageCreatedEvent` publishing.
  - `NetworkNotificationBridge.java`: Uses `FOLLOW:{followId}:{recipientId}` and `CONNECTION_...:{occurrenceId}:{recipientId}`.
  - `MessagingNotificationBridge.java`: Uses `MESSAGE_REACTION:{occurrenceId}:{recipientId}`.
  - `NotificationRepository.java`: Added `markAllReadUpToTuple` and hardened retention queries.
  - `NotificationService.java` & `NotificationServiceImpl.java`: Implemented tuple-based `markAllRead`.
  - `NotificationController.java`: Updated `read-all` endpoint to accept `MarkAllReadRequest`.
  - `CommunityNotificationController.java`: Added legacy `@PatchMapping` aliases for `/read` and `/read-all`.
  - `NotificationPreferenceServiceImpl.java`: Centralized `determineEligibleChannels` with conversation mute handling.
  - `NotificationOutboxServiceImpl.java`: Added lease ownership checks and `recordDelivery`.
  - `NotificationOutboxWorker.java`: Passes `nodeId` to callbacks; records per-device delivery audit in `notification_deliveries`.
  - `NotificationDeviceServiceImpl.java`: Handles race conditions in `registerDevice` via `DataIntegrityViolationException` recovery.
  - `MockPushNotificationProvider.java`: Added hook for test invalid tokens.
  - `docs/COLLOBSTER_BACKEND_ARCHITECTURE.md`: Added Section 26 and updated Table of Contents.
- **Added**:
  - `NotificationPostgresIntegrationTest.java`: 18 integration tests covering all Phase 7.1 invariants.
  - `docs/PHASE_7_1_NOTIFICATION_HARDENING_REPORT.md`: This completion report.

### Y. Known Limitations
- External Push (FCM / APNs) and Email (SMTP) providers use mock implementations until production vendor credentials are provided.
- WebSocket STOMP broker uses in-memory SimpleBroker; multi-instance deployment requires external STOMP relay or Redis pub/sub.

### Z. Explicit Confirmation Python Was NOT Modified
- Confirmed: No files in `Recommendation-System/` were modified. No notification data is sent to Python.
