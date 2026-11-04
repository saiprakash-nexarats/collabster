# COLLOBSTER BACKEND — PHASE 6.1 HARDENING REPORT

## READ CURSOR CORRECTNESS, CONCURRENT IDEMPOTENCY, ATTACHMENT AUTHORIZATION & PRESENCE LIFECYCLE HARDENING

**Completion Date:** September 28, 2026  
**Architecture Domain:** Direct Messaging Hardening, Cursor Correctness, Idempotency Races, Media Authorization, Redis ZSET Presence  
**Platform Version:** Java 21 / Spring Boot 3.5.x / PostgreSQL 16 / Redis 7  
**Test Suite Status:** 399 tests executed, 0 failures, 0 errors, 1 skipped (100% green across entire repository)  
**Schema Version:** Flyway V25 (`uploaded_media` registry and authorization)

---

### A. Executive Summary
Phase 6 established production 1:1 Direct Messaging with PostgreSQL persistence and WebSocket/STOMP realtime synchronization. Phase 6.1 represents a dedicated correctness and security hardening pass executing prior to Phase 7 Global Notifications.

Four critical messaging invariants were audited, redesigned, and verified:
1. **Read Cursor Correctness**: Wall-clock timestamp comparators (`lastReadAt`) were eliminated as the cursor comparator in favor of the authoritative `(createdAt, id)` canonical ordering tuple matching database index and pagination order. Same-timestamp messages are strictly disambiguated by unsigned UUID tiebreaker. Monotonic advancement is guaranteed across delayed ACKs and out-of-order delivery.
2. **Concurrent Send Idempotency & Conflict**: Concurrently racing threads attempting duplicate sends with the same `(conversationId, senderId, clientMessageId)` are safely absorbed via PostgreSQL partial unique index `uq_direct_messages_client_id` and isolated transaction rollback/re-fetch without HTTP 500 errors or duplicate WebSocket broadcasts. Reusing a `clientMessageId` with an altered payload is rejected with HTTP 409 Conflict (`IdempotencyConflictException`).
3. **Attachment Ownership & Authorization**: Storage path syntax validation was elevated to cryptographic and relational ownership validation. Flyway migration `V25__harden_message_attachment_authorization.sql` introduces the `uploaded_media` registry. Media assets must be registered to the sender before attachment. Signed URL generation strictly enforces conversation participant authorization.
4. **Presence Lifecycle Hardening**: Ephemeral presence was migrated to Redis Sorted Sets (`collobster:messages:presence:sessions:{userId}`) where score equals epoch millisecond expiry. Multi-device sessions are tracked independently, heartbeats refresh expiry, stale sessions are pruned on query/event, and disconnecting one device maintains an `ONLINE` state if other active sessions remain. Direct client subscriptions to private `/queue/**` destinations are strictly rejected by the STOMP authorization interceptor.

The entire backend test suite (399 tests) is 100% green with zero failures, community channels remain completely untouched, and Python ML recommendation services were isolated with zero modification.

---

### B. Exact Root-Cause Analysis of Phase 6 Read Cursor Flaw
In Phase 6, `markConversationRead` updated `participant.setLastReadAt(Instant.now())` and relied on `m.createdAt.isAfter(lastReadAt)` for unread counts and receipt updates.
This introduced three critical edge cases:
1. **The Delayed ACK Flaw**: If User A sends Message 1 at 12:00, Message 2 at 12:01, and Message 3 at 12:02, and User B acknowledges Message 1 at 12:10, setting `lastReadAt = 12:10` incorrectly stamped the user's cursor *ahead* of Messages 2 and 3 (12:01 and 12:02). Consequently, Messages 2 and 3 were erroneously treated as already read even though User B had only acknowledged Message 1.
2. **The Same-Timestamp Ambiguity**: If two messages were inserted within the exact same microsecond timestamp $T$, a pure timestamp comparison `m.createdAt > cursorTime` cannot differentiate between them. A user marking the first message read would either mark both read or leave both unread.
3. **Out-of-Order Regression**: A delayed read receipt request for an older message could regress the cursor if network delays reordered HTTP requests.

**Root-Cause Resolution**:
- `last_read_message_id` is now the authoritative cursor pointer.
- `last_read_at` is preserved purely as an audit timestamp recording when the acknowledgment was processed.
- Cursor comparison and pagination are aligned strictly to the database ordering tuple: `(createdAt, id)` matching `ORDER BY createdAt DESC, id DESC`.

---

### C. Monotonic Read Cursor Architecture & Same-Timestamp Tiebreaker Design
The read cursor comparator is defined by the strict total order over all messages in a conversation:
$$(t_1, u_1) \prec (t_2, u_2) \iff (t_1 < t_2) \lor (t_1 = t_2 \land u_1 < u_2)$$

In Java, standard `UUID.compareTo` performs signed comparison on the 64-bit `mostSigBits`, causing UUIDs with leading hex digits `8..f` to be evaluated as negative (smaller than positive `0..7`), which directly conflicts with PostgreSQL's unsigned byte-by-byte collation.
Phase 6.1 implements unsigned UUID comparison in `MessagingServiceImpl.java`:
```java
private int compareMessageOrder(DirectMessage m1, DirectMessage m2) {
    if (m1 == null && m2 == null) return 0;
    if (m1 == null) return -1;
    if (m2 == null) return 1;
    int timeCmp = m1.getCreatedAt().compareTo(m2.getCreatedAt());
    if (timeCmp != 0) {
        return timeCmp;
    }
    int cmp = Long.compareUnsigned(m1.getId().getMostSignificantBits(), m2.getId().getMostSignificantBits());
    if (cmp != 0) {
        return cmp;
    }
    return Long.compareUnsigned(m1.getId().getLeastSignificantBits(), m2.getId().getLeastSignificantBits());
}
```
When `markConversationRead` is invoked:
1. If `participant.getLastReadMessageId()` is present, target message is compared against the current cursor message.
2. If `compareMessageOrder(targetMessage, currentLastRead) <= 0`, the target is older than or equal to current cursor; the request is safely ignored idempotently without regressing.
3. If strictly later, `participant.setLastReadMessageId(targetMessage.getId())` is committed and receipts up to `(targetMessage.getCreatedAt(), targetMessage.getId())` are updated.

---

### D. Keyset Pagination Tuple & Unread Count Proof of Correctness
Keyset pagination in `DirectMessageRepository` uses the canonical tuple:
```sql
SELECT m FROM DirectMessage m
WHERE m.conversationId = :conversationId
  AND (m.createdAt < :cursorTime OR (m.createdAt = :cursorTime AND m.id < :cursorId))
ORDER BY m.createdAt DESC, m.id DESC
```
Unread count computation in `DirectMessageRepository` mirrors this order exactly:
```sql
SELECT COUNT(m) FROM DirectMessage m
WHERE m.conversationId = :conversationId
  AND m.senderId != :userId
  AND m.status = :status
  AND (m.createdAt > :cursorTime OR (m.createdAt = :cursorTime AND m.id > :cursorId))
```
**Invariants Guaranteed**:
- A message sent by the user themselves (`senderId == userId`) is NEVER counted as unread.
- Soft-deleted messages (`status = 'DELETED'`) are excluded.
- Messages created at the exact same microsecond as the cursor message are counted as unread if and only if their UUID is greater than the cursor UUID in unsigned collation.

---

### E. Delayed ACK & Monotonic Advancement Verification
Verified in integration test `MessagingHardeningIntegrationTest.delayedAck_advancesMonotonicallyToC`:
- Setup: Message A at $T_0$, Message B at $T_0 + 60s$, Message C at $T_0 + 120s$. Recipient unread count = 3.
- Recipient issues delayed read ACK for Message A at $T_0 + 600s$:
  - `participant.lastReadMessageId` advances to A.
  - Recipient unread count drops to 2 (Messages B and C remain unread).
  - Receipt for A has `readAt != null`; receipts for B and C remain null.
- Recipient issues read ACK for Message C at $T_0 + 660s$:
  - `participant.lastReadMessageId` advances to C.
  - Recipient unread count drops to 0.
  - Receipts for A, B, and C all have `readAt != null`.
- Recipient re-issues delayed read ACK for Message B:
  - System detects $B \prec C$; request is ignored idempotently.
  - Cursor remains at C.

---

### F. Concurrent Send Race Condition & Idempotency Conflict Resolution
Under high network latency or client retries, multiple concurrent HTTP threads may race to send a message with the same `(conversationId, senderId, clientMessageId)`.
- **Database Boundary**: `uq_direct_messages_client_id (conversation_id, sender_id, client_message_id)`.
- **Persister Isolation**: Write operations are encapsulated in `IdempotentMessagePersister.persistMessage`.
- **Handling Race Condition**:
  1. Thread 1 inserts `direct_messages`, updates `conversations`, creates `message_receipts`, and commits.
  2. Thread 2 hits `DataIntegrityViolationException` on `uq_direct_messages_client_id`.
  3. `IdempotentMessagePersister` catches the unique constraint violation and throws `ConcurrentIdempotentSendException`.
  4. Thread 2 rolls back its isolated write transaction.
  5. `MessagingServiceImpl.sendMessage` catches `ConcurrentIdempotentSendException`, initiates a read query to reload the committed message from Thread 1.
  6. Payload equality is validated (`validatePayloadEquality`).
  7. Thread 2 returns the existing message DTO with HTTP 200/201.
  8. Only Thread 1 fires `MESSAGE_CREATED` / `DirectMessageCreatedEvent` and STOMP broadcasts. Thread 2 emits zero duplicate events.
- **Idempotency Conflict**:
  If the same `clientMessageId` is reused with a different body, reply target, or attachments, `validatePayloadEquality` detects the divergence and throws `IdempotencyConflictException`, returning **HTTP 409 Conflict** with message `IDEMPOTENCY_KEY_REUSED`.

---

### G. Idempotent Database Constraint & Unique Index Architecture
From `V24__create_direct_messaging_schema.sql`:
```sql
CREATE UNIQUE INDEX uq_direct_messages_client_id 
    ON direct_messages (conversation_id, sender_id, client_message_id)
    WHERE client_message_id IS NOT NULL;
```
- **Scope**: Scoped to `(conversation_id, sender_id)`. A user in conversation 1 cannot collide with conversation 2, but client message IDs cannot be reused within the same conversation.
- **Null Safety**: Partial index ensures messages sent without a client message ID (if permitted) do not collide.
- **Checked Foreign Keys & Cascades**: Foreign keys reference `conversations(id)` with `ON DELETE CASCADE`.

---

### H. Attachment Ownership & Storage Key Path Authorization Boundary
In Phase 6, attachment validation only checked storage key format regex. In Phase 6.1, asset ownership authorization is mandatory:
1. **Pre-Upload Registration**: Clients upload or register media via `POST /api/v1/messages/media/register` or `POST /api/v1/messages/media/upload`.
2. **Registry Persistence**: Record stored in `uploaded_media` table with `owner_user_id`, `storage_key`, `media_type`, `mime_type`, `file_size`, and `upload_status`.
3. **Send Message Validation**: When `sendMessage` is called with attachments:
   - Regex validates key format (`direct/{uuid}/filename`).
   - `UploadedMedia` must exist for the key in PostgreSQL.
   - `media.getOwnerUserId().equals(currentUserId)` is strictly verified.
   - If User B attempts to attach media uploaded by User A, or an unregistered storage key, the request is rejected with HTTP 403 `MessagingAccessDeniedException`.

---

### I. Attachment Signed URL Generation & Conversation Membership Enforcement
Endpoint: `GET /api/v1/messages/conversations/{conversationId}/attachments/{attachmentId}/signed-url`
- **Controller**: `MessagingController.getAttachmentSignedUrl`
- **Authorization Enforcement**:
  ```java
  accessService.requireParticipant(conversationId, currentUserId);
  ```
  The user requesting the signed URL must be an active participant in `conversation_participants` for that `conversationId`.
- **Validation**: Attachment must belong to a message within the specified conversation.
- **Signing**: Invokes `StorageService.generateSignedUrl(storageKey, expirationMinutes)` with HMAC-SHA256 signature and expiry timestamp. Non-participants receive HTTP 403 Forbidden.

---

### J. Presence Architecture: Redis ZSET Multi-Session Modeling & Heartbeat Protocol
Phase 6 tracked presence as a single string key per user. Phase 6.1 migrates presence to a multi-device, score-ordered Redis Sorted Set:
- **Redis Key**: `collobster:messages:presence:sessions:{userId}`
- **Member**: STOMP / WebSocket `sessionId` (String)
- **Score**: Milliseconds since Unix epoch indicating session expiration ($T_{\text{now}} + 60,000\text{ms}$).
- **Status Key**: `collobster:messages:presence:{userId}` string key storing `"ONLINE"` with a 60-second TTL.
- **Heartbeat Endpoint**: `@MessageMapping("/messages/heartbeat")` and REST `POST /api/v1/messages/presence/heartbeat`.
- **Heartbeat Processing**:
  ```java
  long expiry = System.currentTimeMillis() + (sessionTtlSeconds * 1000L);
  redisTemplate.opsForZSet().add(sessionsKey, sessionId, expiry);
  redisTemplate.opsForValue().set(userStatusKey, "ONLINE", Duration.ofSeconds(sessionTtlSeconds));
  ```
- **STOMP Activity Interception**: In `MessagingStompAuthorizationInterceptor`, any incoming frame (`SEND`, `SUBSCRIBE`) from an authenticated session automatically refreshes the session's score in the ZSET.

---

### K. Presence Lifecycle Transitions & Stale Session TTL Eviction
- **Connect (`onSessionConnected`)**:
  - Adds `sessionId` with score $T_{\text{now}} + 60s$ to ZSET.
  - Updates user status to `ONLINE`.
  - Publishes `PRESENCE_CHANGED (ONLINE)` to `/user/queue/message-events`.
- **Disconnect (`onSessionDisconnected`)**:
  - Removes `sessionId` from ZSET via `zrem(sessionsKey, sessionId)`.
  - Prunes any expired sessions: `zremrangeByScore(sessionsKey, 0, now)`.
  - Checks remaining cardinality: `zcard(sessionsKey)`.
  - **Multi-Device Invariant**: If `cardinality > 0`, the user remains `ONLINE` (no offline transition emitted).
  - If `cardinality == 0`, status key is deleted, and `PRESENCE_CHANGED (OFFLINE)` is published.
- **Query (`isUserOnline`)**:
  - Lazily prunes expired sessions: `zremrangeByScore(sessionsKey, 0, now)`.
  - Returns `zcard(sessionsKey) > 0`.

---

### L. STOMP Principal Validation & Direct /queue Subscription Rejection Rules
`MessagingStompAuthorizationInterceptor` enforces strict channel boundaries:
1. **Authentication Attachment**: On `CONNECT`, validates JWT, extracts user ID and email, attaches `UsernamePasswordAuthenticationToken` to `SimpAttributesContextHolder` and session attributes.
2. **Rejection of Direct `/queue/**` Subscriptions**:
   - Clients must ONLY subscribe to user-scoped destinations: `/user/queue/...`.
   - Spring's `UserDestinationMessageHandler` translates `/user/queue/messages` into internal broker queues (e.g. `/queue/messages-user123`).
   - If a client attempts to subscribe directly to `/queue/...` (bypassing translation or attempting to eavesdrop on other users), the interceptor immediately rejects the frame with `IllegalArgumentException("Direct subscription to /queue/ is forbidden. Use /user/queue/...")`.
3. **Application Destination Guard**:
   - Only allowed prefix `/app/messages/**` is permitted.
   - Allowed destinations: `/app/messages/typing`, `/app/messages/delivery-ack`, `/app/messages/heartbeat`.
   - Unauthorized destinations are rejected.

---

### M. Message Delivery ACK Authorization & Idempotent Receipt Behavior
Endpoint: REST `POST /api/v1/messages/{messageId}/delivered` and STOMP `/app/messages/delivery-ack`.
- **Authorization Boundary**:
  1. Recipient check: `accessService.requireParticipant(message.getConversationId(), currentUserId)`.
  2. Anti-Self-ACK check:
     ```java
     if (message.getSenderId().equals(currentUserId)) {
         throw new MessagingAccessDeniedException("Sender cannot acknowledge delivery of own message");
     }
     ```
  3. Non-participant check: Users who are not participants in the conversation cannot acknowledge delivery.
- **Idempotent Timestamp**:
  ```java
  if (receipt.getDeliveredAt() == null) {
      receipt.setDeliveredAt(Instant.now());
      receiptRepository.save(receipt);
      realtimePublisher.publishDeliveryReceipt(...);
  }
  ```
  Subsequent delivery acknowledgments preserve the original `deliveredAt` timestamp and do not emit duplicate realtime receipts.

---

### N. Message Edit Window Enforcement Architecture
- **Property**: `collabster.messaging.message-edit-window = 15m` in `MessagingProperties.java`.
- **Enforcement**: In `MessagingServiceImpl.editMessage`:
  ```java
  Duration editWindow = properties.getMessageEditWindow();
  if (editWindow != null && !editWindow.isZero() && !editWindow.isNegative()) {
      if (Duration.between(message.getCreatedAt(), Instant.now()).compareTo(editWindow) > 0) {
          throw new IllegalArgumentException("Message can only be edited within " + editWindow.toMinutes() + " minutes of creation");
      }
  }
  ```
- Protects historical conversation integrity against post-facto message tampering while allowing immediate correction of typos.

---

### O. Resiliency Invariant: Realtime Failure After Commit Isolation
- **Architectural Principle**: PostgreSQL is the durable system of record; WebSocket/STOMP is an ephemeral push notification transport.
- **Isolation Guarantee**:
  - Message persistence (`IdempotentMessagePersister.persistMessage`) commits atomically before realtime publication.
  - Event publication in `MessagingServiceImpl` is executed after database commit.
  - In `MessagingRealtimePublisherImpl`, all broker deliveries are wrapped in individual try-catch blocks logging warnings on failure.
  - If a recipient is disconnected, network drops, or the broker encounters an error, the message is **never rolled back**.
  - The recipient recovers all unread messages reliably upon reconnection via REST Keyset pagination (`GET /api/v1/messages/conversations/{id}/messages`).

---

### P. Database Migration V25 Details
**File**: `src/main/resources/db/migration/V25__harden_message_attachment_authorization.sql`  
Applied to PostgreSQL 16 on port 5433.

```sql
CREATE TABLE IF NOT EXISTS uploaded_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    storage_key VARCHAR(512) NOT NULL,
    media_type VARCHAR(32) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    upload_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_uploaded_media_storage_key UNIQUE (storage_key)
);

CREATE INDEX IF NOT EXISTS idx_uploaded_media_owner 
    ON uploaded_media (owner_user_id, created_at DESC);
```
- Guarantees each storage key is globally unique and tied to an authoritative owner.
- Fast index lookup on owner user ID for user media quota audits and validation.

---

### Q. Security Audit: Channel Separation, Block Enforcement, Signed URLs, and Rate Limiting
1. **Channel Separation**:
   - `/ws/community` operates under `CommunityStompAuthorizationInterceptor`, routing to `/topic/communities/...`.
   - `/ws/messages` operates under `MessagingStompAuthorizationInterceptor`, routing to `/user/queue/...`.
   - No path or topic crosstalk is possible.
2. **Block Enforcement**:
   - Evaluated via `NetworkAccessService.isBlockedEitherDirection(viewerId, otherUserId)`.
   - Froze conversations to read-only: messages, reactions, and typing are blocked with HTTP 403 `BlockedUserException`.
3. **Signed URLs**:
   - Signed with HMAC-SHA256 and expiration.
   - Enforces participant validation (`accessService.requireParticipant`).
4. **Rate Limiting**:
   - Token bucket sliding window in Redis with Bucket4j local fallback: 60 messages/min, 30 conversations/hr, 60 reactions/min, 120 typing/min.

---

### R. All Files Created
1. `src/main/resources/db/migration/V25__harden_message_attachment_authorization.sql`
2. `src/main/java/com/collabsphere_first/messaging/entity/UploadedMedia.java`
3. `src/main/java/com/collabsphere_first/messaging/repository/UploadedMediaRepository.java`
4. `src/main/java/com/collabsphere_first/messaging/dto/RegisterMediaRequest.java`
5. `src/main/java/com/collabsphere_first/messaging/dto/UploadedMediaResponse.java`
6. `src/main/java/com/collabsphere_first/messaging/dto/AttachmentSignedUrlResponse.java`
7. `src/main/java/com/collabsphere_first/messaging/dto/PersistMessageResult.java`
8. `src/main/java/com/collabsphere_first/messaging/exception/IdempotencyConflictException.java`
9. `src/main/java/com/collabsphere_first/messaging/exception/ConcurrentIdempotentSendException.java`
10. `src/main/java/com/collabsphere_first/messaging/service/IdempotentMessagePersister.java`
11. `src/test/java/com/collabsphere_first/messaging/MessagingHardeningIntegrationTest.java`
12. `docs/PHASE_6_1_MESSAGING_HARDENING_REPORT.md`

---

### S. All Files Modified
1. `DirectMessageRepository.java` (added `countUnreadAfterCursor` with tuple tiebreaker).
2. `MessageReceiptRepository.java` (updated `markReadForUserUpTo` with tuple tiebreaker and `senderId != userId`).
3. `MessagingProperties.java` (added `messageEditWindow` configured to 15 minutes).
4. `MessagingAttachmentService.java` & `MessagingAttachmentServiceImpl.java` (added ownership validation, media registration, upload handling, safe ID generation).
5. `MessagingPresenceServiceImpl.java` (migrated to Redis ZSET with epoch millisecond scores and multi-device session pruning).
6. `MessagingWebSocketController.java` (added `@MessageMapping("/messages/heartbeat")`).
7. `MessagingStompAuthorizationInterceptor.java` (forbids direct `/queue/...` client subscriptions, attaches principal, refreshes presence on active frames).
8. `MessagingService.java` & `MessagingServiceImpl.java` (read cursor comparison by `(createdAt, id)` tuple, unread count by message cursor, delivery ack authorization, signed url endpoint, idempotency conflict checks, transaction decoupling).
9. `MessagingController.java` (endpoints for signed url, media register/upload, presence heartbeat).
10. `MessagingExceptionHandler.java` (added 409 Conflict handler for `IdempotencyConflictException`).
11. `MessagingServiceTest.java` (updated mocks and test cases).
12. `MessagingPresenceAndTypingTest.java` & `MessagingWebSocketSecurityTest.java` (updated for ZSET presence and STOMP direct queue rejection).
13. `MessagingPostgresIntegrationTest.java` (verified against PostgreSQL 16 schema V25).
14. `docs/COLLOBSTER_BACKEND_ARCHITECTURE.md` (updated Sections 23 & 24).

---

### T. Exact Diff Snippets for Critical Invariants

#### 1. Unsigned UUID Comparison in Monotonic Order (`MessagingServiceImpl.java`)
```diff
+    private int compareMessageOrder(DirectMessage m1, DirectMessage m2) {
+        if (m1 == null && m2 == null) return 0;
+        if (m1 == null) return -1;
+        if (m2 == null) return 1;
+        int timeCmp = m1.getCreatedAt().compareTo(m2.getCreatedAt());
+        if (timeCmp != 0) {
+            return timeCmp;
+        }
+        int cmp = Long.compareUnsigned(m1.getId().getMostSignificantBits(), m2.getId().getMostSignificantBits());
+        if (cmp != 0) {
+            return cmp;
+        }
+        return Long.compareUnsigned(m1.getId().getLeastSignificantBits(), m2.getId().getLeastSignificantBits());
+    }
```

#### 2. Tuple Tiebreaker in Unread Count Query (`DirectMessageRepository.java`)
```diff
+    @Query("SELECT COUNT(m) FROM DirectMessage m " +
+           "WHERE m.conversationId = :conversationId " +
+           "AND m.senderId != :userId " +
+           "AND m.status = :status " +
+           "AND (m.createdAt > :cursorTime OR (m.createdAt = :cursorTime AND m.id > :cursorId))")
+    long countUnreadAfterCursor(
+            @Param("conversationId") UUID conversationId,
+            @Param("userId") Long userId,
+            @Param("status") MessageStatus status,
+            @Param("cursorTime") Instant cursorTime,
+            @Param("cursorId") UUID cursorId
+    );
```

#### 3. Idempotent Concurrent Send Recovery (`MessagingServiceImpl.java`)
```diff
+        PersistMessageResult result;
+        try {
+            result = persister.persistMessage(conversationId, currentUserId, recipientId, request, messageType, hasBody);
+        } catch (ConcurrentIdempotentSendException ex) {
+            DirectMessage existing = messageRepository.findByConversationIdAndSenderIdAndClientMessageId(
+                    conversationId, currentUserId, request.clientMessageId()
+            ).orElseThrow(() -> new IllegalStateException("Expected existing message on idempotency conflict", ex));
+            result = new PersistMessageResult(existing, false);
+        }
+
+        if (!result.isNew()) {
+            log.info("Idempotent hit: message already exists for clientMessageId {}", request.clientMessageId());
+            validatePayloadEquality(result.message(), request);
+            return hydrateSingleMessage(result.message(), currentUserId);
+        }
```

#### 4. STOMP Direct Queue Subscription Rejection (`MessagingStompAuthorizationInterceptor.java`)
```diff
+        if (SimpMessageType.SUBSCRIBE.equals(messageType)) {
+            if (destination != null && destination.startsWith("/queue/")) {
+                log.warn("Rejected direct /queue subscription attempt by user {}: {}", userEmail, destination);
+                throw new IllegalArgumentException("Direct subscription to /queue/ is forbidden. Use /user/queue/...");
+            }
+        }
```

---

### U. Full Test Suite Execution Results
- **Full Repository Regression**:
  ```text
  [INFO] Tests run: 399, Failures: 0, Errors: 0, Skipped: 1
  [INFO] BUILD SUCCESS
  [INFO] Total time: 37.631 s
  ```
- **Messaging Suite Breakdown (69 Tests)**:
  - `MessagingHardeningIntegrationTest`: 10/10 passed (PostgreSQL 16 + Redis 7 live integration)
  - `MessagingPostgresIntegrationTest`: 6/6 passed (PostgreSQL 16 check/unique constraints, blocking, keyset pagination)
  - `MessagingServiceTest`: 15/15 passed (Unit test logic, monotonic cursor advance, reaction limits)
  - `MessagingPresenceAndTypingTest`: 8/8 passed (Redis ZSET presence, heartbeat renewal, typing TTL)
  - `MessagingWebSocketSecurityTest`: 10/10 passed (STOMP JWT auth, queue protection, destination filtering)
  - `MessagingAccessServiceTest`: 9/9 passed (IDOR prevention, participant checks, block enforcement)
  - `MessagingRateLimiterTest`: 2/2 passed (Token bucket sliding window)
  - `MessagingControllerTest`: 9/9 passed (WebMvc REST endpoints, HTTP 409 conflict, HTTP 403 forbidden)

---

### V. Phase 6.1 Test Matrix & Verification Scenarios
| Scenario ID | Test Name | Invariant Verified | Outcome |
| :--- | :--- | :--- | :--- |
| **TC-6.1-01** | `delayedAck_advancesMonotonicallyToC` | Delayed ACK A at 12:10, C at 12:11 advances cursor A -> C, unread count = 0 | **PASS** |
| **TC-6.1-02** | `sameTimestamp_tiebreakerByUuid` | Messages at exact same millisecond disambiguated by unsigned UUID tiebreaker | **PASS** |
| **TC-6.1-03** | `outOfOrderReadRequest_doesNotRegressCursor` | Read C then delayed read B leaves cursor at C | **PASS** |
| **TC-6.1-04** | `concurrentSend_identicalClientMessageId_resolvesCleanly` | 4 racing threads on same clientMessageId insert 1 row, both return same message ID | **PASS** |
| **TC-6.1-05** | `idempotencyConflict_reusedClientMessageIdWithDifferentPayload_throws409` | Same clientMessageId with modified body throws HTTP 409 Conflict | **PASS** |
| **TC-6.1-06** | `attachmentOwnership_rejectsForeignMedia` | User B attempting to attach User A's media throws HTTP 403 Forbidden | **PASS** |
| **TC-6.1-07** | `attachmentSignedUrl_rejectsNonParticipant` | Non-participant requesting signed URL throws HTTP 403 Forbidden | **PASS** |
| **TC-6.1-08** | `deliveryAck_authorizationAndIdempotency` | Sender cannot mark own message delivered; receipt deliveredAt is idempotent | **PASS** |
| **TC-6.1-09** | `presenceLifecycle_zsetMultiSession` | Multi-device ZSET: disconnecting device 1 keeps user online if device 2 active | **PASS** |
| **TC-6.1-10** | `realtimeFailure_doesNotRollbackCommittedMessage` | Realtime failure after commit does not roll back persisted message | **PASS** |

---

### W. Community Realtime & Feed Non-Regression Verification
- `WebSocketSecurityTest`: 4/4 passed (Community STOMP authorization at `/ws/community` completely intact).
- `RecommendationFeedRankingStrategyTest`, `RecommendationCircuitBreakerTest`, `HttpRecommendationClientTest`: All 32 recommendation resilience tests completely passed.
- Community Channel messaging (`community_channel_messages`) and event outbox publishers executed without error.

---

### X. Python Isolation Verification
- **Directory**: `Recommendation-System/`
- **Verification**: Zero files created, modified, or deleted in Python.
- **Protocol**: Python ML recommendation service does not participate in Direct Messaging, presence, or WebSocket delivery.

---

### Y. Architecture Drift Assessment
- **Drift Detected**: None.
- **Alignment**:
  - Technology stack remains strictly Java 21 / Spring Boot 3.5.x / PostgreSQL 16 / Redis 7.
  - No premature implementation of Phase 7 Global Notifications or Group Messaging.
  - All Phase 5.2 recommendation resilience architectural patterns preserved.

---

### Z. Operational Runbook & Metrics Guidelines
1. **Database Monitoring**:
   - Monitor index usage on `uq_direct_messages_client_id` and `idx_direct_messages_conversation_created`.
   - Alert on sustained spikes in `DataIntegrityViolationException` which indicate aggressive client retry loops.
2. **Redis Memory & Eviction**:
   - Monitor memory on `collobster:messages:presence:sessions:*`.
   - Stale sessions automatically pruned on each event/query via `ZREMRANGEBYSCORE ... 0 (now)`.
3. **Storage Signatures**:
   - Verify `collabster.storage.signing-secret` is securely rotated via environment variables.

---

### AA. Final Recommendation & Readiness for Phase 7 Global Notifications
The messaging domain has satisfied all architectural, mathematical, and cryptographic correctness criteria:
- Monotonic read cursors are provably sound against network delay and microsecond collisions.
- Concurrent send idempotency eliminates duplicate database rows and race condition exceptions.
- Media authorization establishes strict ownership boundaries.
- Ephemeral presence accurately tracks multi-device lifecycles.

**Phase 6.1 Hardening is COMPLETE. The Collobster backend is fully certified and ready for Phase 7 Global Notifications.**
