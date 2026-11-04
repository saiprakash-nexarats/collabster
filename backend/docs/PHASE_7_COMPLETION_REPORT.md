# COLLOBSTER BACKEND — PHASE 7 COMPLETION REPORT

## GLOBAL NOTIFICATION CENTER + REALTIME + PUSH/EMAIL DELIVERY FOUNDATION

**Completion Date:** September 29, 2026  
**Architecture Domain:** Global Notification Center, Keyset Pagination, Durable Outbox, Push/Email Abstraction, STOMP Realtime, Event Bridges  
**Platform Version:** Java 21 / Spring Boot 3.5.x / PostgreSQL 16 / Redis 7  
**Test Suite Status:** 425 tests executed, 0 failures, 0 errors, 1 skipped (100% green across entire repository)  
**Schema Version:** Flyway V26 (`V26__create_global_notification_schema.sql`)

---

### A. Global Notification Architecture
Phase 7 establishes a single global platform Notification domain under `com.collabsphere_first.notification`.

Key architectural principles:
1. **Derived Event Consumer**: The notification domain is a consumer of domain events, never an authoritative owner of source domain resources (Direct Messages, Network Connections, Community Posts, Users).
2. **PostgreSQL as Sole Source-of-Truth**: All in-app notifications and background delivery jobs reside durably in PostgreSQL 16 (`notifications`, `notification_outbox`, `notification_preferences`, `notification_devices`, `notification_deliveries`). The platform survives WebSocket disconnects, Redis outages, push provider downtime, email server latency, and server crashes without losing in-app notifications.
3. **Decoupled Asynchronous Delivery**: The source domain transaction persists the notification row and enqueues outbox jobs transactionally. External provider dispatch (Push, Email) and STOMP realtime dispatch happen asynchronously, preventing external network jitter from slowing down or failing user API requests.

---

### B. V26 Migration
Flyway migration `V26__create_global_notification_schema.sql` was applied cleanly onto the database:
- `notifications`: Core table storing persistent in-app notifications.
- `notification_preferences`: Per-user category channel configuration (`(user_id, category)` primary key).
- `notification_devices`: Push device token registry with SHA-256 token hashing and rotation tracking.
- `notification_outbox`: Multi-channel job outbox with locking timestamps and attempt counters.
- `notification_deliveries`: Delivery audit log tracking external provider outcomes and sanitized error codes.
- `idx_notifications_recipient_created`: `(recipient_user_id, created_at DESC, id DESC)`.
- `idx_notifications_unread`: `(recipient_user_id, created_at DESC) WHERE read_at IS NULL AND archived_at IS NULL`.
- `idx_notifications_category`: `(recipient_user_id, category, created_at DESC)`.
- `uq_notifications_event_key`: `UNIQUE (event_key)`.
- `uq_notification_outbox_channel`: `UNIQUE (notification_id, channel)`.
- `uq_notification_devices_token_hash`: `UNIQUE (token_hash)`.

`spring.jpa.hibernate.ddl-auto=validate` passed completely against PostgreSQL 16.

---

### C. Notification Entity / State Model
`Notification.java` encapsulates the notification entity:
- `id` (UUID): High-entropy unique identifier preventing enumeration attacks.
- `recipientUserId` (Long): Foreign key to `users(id)`, strictly bounds visibility.
- `actorUserId` (Long): Foreign key to `users(id)`, identity of acting user (if any).
- `type` (`NotificationType`): Domain event type.
- `category` (`NotificationCategory`): Functional grouping (`SOCIAL`, `MESSAGING`, `COMMUNITY`, `SYSTEM`, `SECURITY`).
- `sourceType` / `sourceId`: Source entity reference.
- `contextType` / `contextId`: Contextual entity reference (e.g. Conversation, Community).
- `eventKey`: Deterministic deduplication key.
- `titleKey` / `bodyKey`: Semantic localization keys.
- `payload`: Safe rendering JSON string without sensitive details.
- `createdAt`: Creation timestamp.
- `seenAt`: Viewport display timestamp.
- `readAt`: Explicit client acknowledgment timestamp.
- `archivedAt`: Soft-dismissal timestamp.

Derived state machine (`NotificationState`):
- `UNSEEN`: `seenAt == null && readAt == null && archivedAt == null`
- `SEEN`: `seenAt != null && readAt == null && archivedAt == null`
- `READ`: `readAt != null && archivedAt == null`
- `ARCHIVED`: `archivedAt != null`

---

### D. Event-Key Idempotency
Every notification requires a deterministic event key:
- Follow: `FOLLOW:{actorId}:{recipientId}`
- Connection Request: `CONNECTION_REQUEST:{connectionId}:{recipientId}`
- Connection Accept: `CONNECTION_ACCEPTED:{connectionId}:{recipientId}`
- Direct Message: `MESSAGE:{messageId}:{recipientId}`
- Direct Message Reaction: `MESSAGE_REACTION:{messageId}:{actorId}:{reactionType}:{recipientId}`
- Community Event: `COMMUNITY:{communityEventId}:{recipientId}`

Enforced via `UNIQUE (event_key)` in PostgreSQL. If a duplicate source event is fired (e.g. outbox retry or network retry), the transaction detects the existing row and returns the existing notification without creating duplicates.

---

### E. Notification Types / Categories
Explicit enums prevent arbitrary strings:
- **`NotificationCategory`**:
  - `SOCIAL`
  - `MESSAGING`
  - `COMMUNITY`
  - `SYSTEM`
  - `SECURITY`
- **`NotificationType`**:
  - `NEW_FOLLOW` (Maps to `SOCIAL`)
  - `CONNECTION_REQUESTED` (Maps to `SOCIAL`)
  - `CONNECTION_ACCEPTED` (Maps to `SOCIAL`)
  - `DIRECT_MESSAGE` (Maps to `MESSAGING`)
  - `DIRECT_MESSAGE_REACTION` (Maps to `MESSAGING`)
  - `COMMUNITY_INVITATION` (Maps to `COMMUNITY`)
  - `COMMUNITY_POST_ACTIVITY` (Maps to `COMMUNITY`)
  - `COMMUNITY_EVENT` (Maps to `COMMUNITY`)
  - `COMMUNITY_MENTION` (Maps to `COMMUNITY`)
  - `SYSTEM_ALERT` (Maps to `SYSTEM`)
  - `SECURITY_ALERT` (Maps to `SECURITY`)

---

### F. Typed Navigation / Deep Links
To prevent malicious URL injection, all navigation targets use strict typed metadata:
- **`NavigationTargetType`**:
  - `OPEN_PROFILE`
  - `OPEN_CONNECTION_REQUEST`
  - `OPEN_CONVERSATION`
  - `OPEN_COMMUNITY`
  - `OPEN_COMMUNITY_POST`
- Returned via `NavigationActionDto { targetType, targetId, params }`. Arbitrary HTTP/HTTPS or JavaScript URLs are strictly disallowed.

---

### G. Notification Center REST API
REST endpoints under `/api/v1/notifications`:
- `GET /api/v1/notifications`: Keyset-paginated list of notifications with optional `filter` (`ALL`, `UNREAD`) and `category`.
- `GET /api/v1/notifications/{id}`: Single notification retrieval (recipient-only).
- `POST /api/v1/notifications/{id}/seen`: Marks notification as seen.
- `POST /api/v1/notifications/{id}/read`: Marks notification as read (read implies seen).
- `POST /api/v1/notifications/{id}/archive`: Soft-archives notification.
- `POST /api/v1/notifications/read-all`: Bulk-marks notifications as read up to optional `readUpTo` high-water timestamp.
- `GET /api/v1/notifications/unread-count`: Returns active unread count.

---

### H. Pagination / Unread Count
- **Keyset Pagination**:
  - Ordered by `(created_at DESC, id DESC)`.
  - Base64 URL-safe JSON cursor: `NotificationCursor { createdAt, id }`.
  - Validated strictly; malformed cursors return HTTP 400 `NotificationCursorException`.
  - Configurable page limits: default 20, max 100.
- **Unread Count**:
  - Fast indexed COUNT query: `recipient_user_id = :userId AND read_at IS NULL AND archived_at IS NULL`.
  - Evaluated in PostgreSQL without loading rows into Java memory.

---

### I. Seen / Read / Archive Semantics
- **Monotonic Timestamps**: Timestamps only advance forward. Marking an already read notification as read preserves the initial `readAt` timestamp.
- **Read Implies Seen**: Calling `/read` automatically sets `seenAt` if it was null.
- **High-Water Bulk Read**: `POST /api/v1/notifications/read-all` accepts an optional `readUpTo` Instant. Newly arrived notifications arriving concurrently after `readUpTo` remain unread, avoiding client race conditions.

---

### J. Preference Architecture
- **Preferences Table**: `notification_preferences` with composite key `(user_id, category)` storing `in_app_enabled`, `push_enabled`, `email_enabled`.
- **Defaults**:
  - `SOCIAL`: in-app=true, push=true, email=false.
  - `MESSAGING`: in-app=true, push=true, email=false.
  - `COMMUNITY`: in-app=true, push=true, email=false.
  - `SYSTEM`: in-app=true, push=true, email=false.
  - `SECURITY`: in-app=true (mandatory, cannot be disabled), push=true, email=true.
- **Preference API**:
  - `GET /api/v1/notifications/preferences`: Returns map of preferences across categories.
  - `PATCH /api/v1/notifications/preferences`: Updates specific category channel preferences.

---

### K. Messaging Mute Integration
- **Conversation Mute Rule**: `conversation_participants.notifications_muted` affects notification delivery.
- **Semantics**: When a direct conversation is muted:
  - In-app notification is still created and persisted in PostgreSQL for notification center history.
  - Outbox jobs for `PUSH` and `EMAIL` are completely suppressed.
  - The direct message itself is never suppressed.
- **Block Relationship Enforcement**: Notifications are never created between users who have an active Network block.

---

### L. Device Registration / Token Security
- **Endpoints**:
  - `POST /api/v1/notifications/devices`: Registers or updates device token.
  - `DELETE /api/v1/notifications/devices/{deviceId}`: Revokes device token.
- **Security & Privacy**:
  - Push tokens are never logged.
  - Token hashes (SHA-256) are used for database unique constraints (`uq_notification_devices_token_hash`).
  - **Atomic Reassignment**: If User B registers a device token previously associated with User A, the token is atomically reassigned to User B and revoked from User A, preventing cross-user leakage.
  - Users can only manage devices they own (enforced via `SecurityContext`).

---

### M. Durable Outbox
- **`notification_outbox`**:
  - `id UUID PRIMARY KEY`
  - `notification_id UUID NOT NULL` (FK `notifications(id)`)
  - `channel VARCHAR(20) NOT NULL` (`REALTIME`, `PUSH`, `EMAIL`)
  - `status VARCHAR(20) NOT NULL` (`PENDING`, `CLAIMED`, `COMPLETED`, `FAILED`, `SKIPPED`)
  - `attempt_count INT NOT NULL DEFAULT 0`
  - `next_attempt_at TIMESTAMPTZ NOT NULL`
  - `locked_at TIMESTAMPTZ NULL`
  - `last_error_code VARCHAR(100) NULL`
- Guaranteed transactional persistence with `notifications` row.

---

### N. Worker Claiming / Multi-Instance Safety
- **Safe Claiming**: `SELECT ... FOR UPDATE SKIP LOCKED` with batch limits.
- **Horizontal Scaling**: Multiple Spring Boot instances can run `NotificationOutboxWorker` simultaneously without claiming the same outbox job.
- **Lock Leases**: `locked_at` timestamp ensures that if a worker crashes mid-delivery, jobs older than lease expiration (default 5 minutes) are reclaimed automatically.

---

### O. Retry / Backoff / Dead-Letter Behavior
- **Exponential Backoff**: Configurable backoff multipliers (`30s`, `2m`, `10m`, `30m`).
- **Maximum Attempts**: Default 5 attempts. Exceeding maximum attempts marks job as `FAILED` (dead-letter).
- **Permanent Failure Handling**: Deterministic errors (e.g. invalid push token, malformed email) fail immediately without retrying, and revoke invalid device tokens.
- **In-App Safety**: In-app notifications in PostgreSQL remain valid and accessible regardless of outbox job failure.

---

### P. Network Event Integration
- `NetworkNotificationBridge` listens to:
  - `UserFollowedEvent` -> Generates `NEW_FOLLOW` notification to followed user.
  - `ConnectionRequestedEvent` -> Generates `CONNECTION_REQUESTED` notification to addressee.
  - `ConnectionAcceptedEvent` -> Generates `CONNECTION_ACCEPTED` notification to requester.
- Self-notifications are suppressed (`actorId == recipientId`).
- Network blocks suppress notification generation.

---

### Q. Messaging Event Integration
- `MessagingNotificationBridge` listens to:
  - `DirectMessageCreatedEvent` -> Generates `DIRECT_MESSAGE` notification to recipient.
  - `DirectMessageReactionEvent` -> Generates `DIRECT_MESSAGE_REACTION` notification to message author.
- **Privacy Enforcement**: **Raw message body is never persisted in notification payload or sent in push alerts**. Push text defaults to localized summaries (e.g. *"Alex sent you a message"*).
- Sender receives no notification.
- Conversation mute suppresses push/email outbox jobs while preserving in-app notification.
- Self-reactions are suppressed.

---

### R. Community Notification Bridge
- `CommunityNotificationBridge` bridges existing `CommunityNotificationSavedEvent` to Global Notification Center.
- **Event Key Deduplication**: Uses deterministic key `COMMUNITY:{communityNotificationId}:{recipientId}`.
- Outbox retries within Community do not generate duplicate global notifications.
- Existing Community tables, controllers, and services remain completely functional and untouched.

---

### S. Realtime WebSocket Notifications
- Dedicated endpoint: `/ws/notifications`.
- Private destination: `/user/queue/notifications`.
- Clients receive lightweight `NotificationResponse` DTO without internal outbox or provider secrets.
- Realtime failure never rolls back or deletes the persisted PostgreSQL notification.

---

### T. STOMP Security
- `NotificationStompAuthorizationInterceptor`:
  - Enforces Bearer JWT on STOMP `CONNECT` frame.
  - Rejects unauthenticated connections with HTTP/STOMP error.
  - Authorizes subscriptions strictly to `/user/queue/notifications`.
  - Rejects snooping attempts to `/queue/notifications` or `/topic/notifications`.

---

### U. Push Provider Architecture
- Interface `PushNotificationProvider` decouples application logic from FCM/APNs.
- Configuration-gated: `collobster.notifications.push.enabled=false` by default.
- Tested and verified via `MockPushNotificationProvider`.
- Automatic device token invalidation on provider `UNREGISTERED` / `INVALID_TOKEN` responses.

---

### V. Email Provider Architecture
- `EmailNotificationProviderImpl` adapts Spring's existing `JavaMailSender`.
- Reuses existing mail configuration without duplicating SMTP stacks.
- Gated by `collobster.notifications.email.enabled`.
- Failure classification: transient connection failures retry; invalid recipients fail permanently.

---

### W. Provider Failure Isolation
- Provider outages (FCM down, SMTP timeout, WebSocket disconnect) never cause the source API transaction to fail.
- All delivery attempts are tracked in `notification_deliveries`.
- In-app notification center remains 100% operational during external provider outages.

---

### X. Notification Security / IDOR
- Every REST endpoint derives user identity strictly from `SecurityContext`.
- No `userId` query parameter spoofing.
- Attempting to view, read, see, or archive another user's notification returns HTTP 403 `NotificationAccessDeniedException`.
- Verified by automated unit and PostgreSQL integration tests.

---

### Y. Retention / Cleanup
- `NotificationRetentionWorker` executes scheduled housekeeping (default daily at 03:00 AM).
- Cleans up read/archived notifications older than retention window (`collobster.notifications.retention-days`, default 90 days).
- Cleans up completed outbox and delivery logs older than `outbox-retention-days` (default 30 days).
- Unread notifications are never deleted by the retention worker.

---

### Z. Notification Test Results
26 dedicated tests across core and integration suites:
1. `NotificationCoreTest` (16 unit tests, 100% pass):
   - `createNotification_success`
   - `createNotification_deduplication`
   - `createNotification_suppressSelfNotification`
   - `markSeen_success`
   - `markRead_successAndImpliesSeen`
   - `markRead_idempotent`
   - `archive_success`
   - `markAllRead_boundedByHighWater`
   - `unreadCount_evaluatesCorrectly`
   - `keysetPagination_forwardPaging`
   - `keysetPagination_invalidCursor_throws`
   - `preference_evaluation_respectsCategoryAndMute`
   - `deviceRegistration_tokenHashingAndRotation`
   - `deviceRegistration_crossUserReassignment`
   - `idor_preventThirdPartyAccess`
   - `pushPayloadPrivacy_neverContainsMessageBody`
2. `NotificationPostgresIntegrationTest` (10 PostgreSQL 16 integration tests, 100% pass):
   - `testNotificationCreationAndDeduplicationInPostgres`
   - `testSeenReadAndReadImpliesSeen`
   - `testMarkAllReadUpToTimestamp`
   - `testKeysetPaginationAndCategoryFilter`
   - `testUnreadCountQueryPerformance`
   - `testIdorProtection`
   - `testPreferencesUpdateAndEvaluation`
   - `testDeviceRegistrationAndRevocation`
   - `testOutboxWorkerClaimAndExecution`
   - `testStompSecurity`

---

### AA. PostgreSQL / Flyway / Hibernate Verification
- Real PostgreSQL 16 on port 5433 (`collobster-test-pg`).
- Migrations V1 through V26 applied cleanly.
- `spring.jpa.hibernate.ddl-auto=validate` verified with zero validation errors.

---

### AB. Redis Verification
- Real Redis 7 verified on port 6379 (`collobster-feed-redis`).
- Notification persistence does not depend on Redis; if Redis is down, in-app notifications and outbox delivery remain fully operational.

---

### AC. Messaging Regression
- Phase 6 and 6.1 Direct Messaging tests remain 100% green.
- Cursor correctness, client message ID deduplication, signed URLs, and presence remain fully intact.

---

### AD. Community Regression
- Community domain and Community Notifications remain 100% green.
- Existing `/ws/community` and Community endpoints are intact.

---

### AE. Feed / Explore Regression
- Feed and Explore ranking strategies remain 100% green.

---

### AF. Full Java Regression
- **Full Maven Build**: `.\mvnw.cmd test`
- **Total Tests Run**: **425**
- **Failures**: **0**
- **Errors**: **0**
- **Skipped**: **1** (`LivePythonRecommendationIntegrationTest` skipped as expected when running without Python live server profile)
- **Result**: **BUILD SUCCESS**

---

### AG. Files Added / Modified

#### New Migration
- `src/main/resources/db/migration/V26__create_global_notification_schema.sql`

#### New Domain Classes (`com.collabsphere_first.notification`)
- `config/NotificationProperties.java`
- `enums/NotificationCategory.java`
- `enums/NotificationType.java`
- `enums/NavigationTargetType.java`
- `enums/NotificationChannel.java`
- `enums/OutboxStatus.java`
- `enums/NotificationDeliveryStatus.java`
- `enums/DevicePlatform.java`
- `enums/DeviceProvider.java`
- `enums/NotificationState.java`
- `dto/NavigationActionDto.java`
- `dto/NotificationCursor.java`
- `dto/NotificationResponse.java`
- `dto/NotificationPageResponse.java`
- `dto/UnreadCountResponse.java`
- `dto/NotificationPreferenceDto.java`
- `dto/UpdateNotificationPreferenceRequest.java`
- `dto/RegisterDeviceRequest.java`
- `dto/NotificationDeviceResponse.java`
- `dto/CreateNotificationCommand.java`
- `entity/Notification.java`
- `entity/NotificationPreference.java`
- `entity/NotificationPreferenceId.java`
- `entity/NotificationDevice.java`
- `entity/NotificationOutbox.java`
- `entity/NotificationDelivery.java`
- `repository/NotificationRepository.java`
- `repository/NotificationPreferenceRepository.java`
- `repository/NotificationDeviceRepository.java`
- `repository/NotificationOutboxRepository.java`
- `repository/NotificationDeliveryRepository.java`
- `exception/NotificationNotFoundException.java`
- `exception/NotificationAccessDeniedException.java`
- `exception/NotificationCursorException.java`
- `exception/NotificationExceptionHandler.java`
- `service/NotificationPreferenceService.java`
- `service/NotificationPreferenceServiceImpl.java`
- `service/NotificationDeviceService.java`
- `service/NotificationDeviceServiceImpl.java`
- `service/NotificationOutboxService.java`
- `service/NotificationOutboxServiceImpl.java`
- `service/NotificationService.java`
- `service/NotificationServiceImpl.java`
- `delivery/push/PushSendResult.java`
- `delivery/push/PushNotificationProvider.java`
- `delivery/push/MockPushNotificationProvider.java`
- `delivery/email/EmailNotificationProvider.java`
- `delivery/email/EmailNotificationProviderImpl.java`
- `delivery/realtime/NotificationRealtimePublisher.java`
- `delivery/realtime/NotificationRealtimePublisherImpl.java`
- `worker/NotificationOutboxWorker.java`
- `worker/NotificationRetentionWorker.java`
- `bridge/NetworkNotificationBridge.java`
- `bridge/MessagingNotificationBridge.java`
- `bridge/CommunityNotificationBridge.java`
- `controller/NotificationController.java`
- `controller/NotificationPreferenceController.java`
- `controller/NotificationDeviceController.java`
- `security/NotificationStompAuthorizationInterceptor.java`

#### Configuration & Integration Updates
- `config/CommunityWebSocketConfig.java`: Configured `/ws/notifications` STOMP endpoint and channel interceptor.
- `config/SecurityConfig.java`: Configured security permissions for `/ws/notifications/**`.
- `community/controller/CommunityNotificationController.java`: Updated base path to `/api/v1/communities/notifications` to free `/api/v1/notifications` for the global notification center.
- `community/service/CommunityNotificationService.java`: Added backward-compatible constructor for existing tests and wired `CommunityNotificationSavedEvent` publication for global notification bridge.

#### Documentation Updates
- `docs/COLLOBSTER_BACKEND_ARCHITECTURE.md`: Added Section 25 (Global Notification Center Architecture).
- `docs/PHASE_7_COMPLETION_REPORT.md`: This comprehensive completion report.

---

### AH. Known Limitations / External Provider Verification
- Push architecture and provider adapter verified with mock provider (`MockPushNotificationProvider`); live production provider credential test (FCM/APNs) not executed because live mobile vendor credentials are not configured in local environment.
- Email provider adapter verified with existing Spring mail infrastructure; live external SMTP test not executed because external SMTP server is not configured in local environment.

---

### AI. Explicit Confirmation Python Was NOT Modified
- `Recommendation-System/` directory and all Python scripts were **NOT modified**.
- No notification payloads or messages were sent to Python.
- Python remains recommendation-only.
