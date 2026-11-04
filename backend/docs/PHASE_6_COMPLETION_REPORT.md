# COLLOBSTER BACKEND — PHASE 6 COMPLETION REPORT

## PRODUCTION DIRECT MESSAGING + REALTIME WEBSOCKET

**Completion Date:** September 28, 2026  
**Architecture Domain:** Direct Messaging, WebSocket / STOMP Realtime, Ephemeral Presence & Typing  
**Platform Version:** Java 21 / Spring Boot 3.5.x / PostgreSQL 16 / Redis 7  
**Test Suite Status:** 384 tests executed, 0 failures, 0 errors (100% green)

---

### A. Messaging Architecture
The Messaging domain (`com.collabsphere_first.messaging`) provides authoritative 1:1 direct conversations, text/attachment/mixed messages, editing, soft deletion, emoji reactions, delivery receipts, monotonic read cursors, unread counts, and live WebSocket synchronization.
- **Architectural Flow**:
  ```text
  Mobile Client (REST / STOMP)
             ↓
    Spring Security (JWT)
             ↓
     MessagingService
             ↓
  MessagingAccessService (IDOR, Membership & Block Validation)
             ↓
  PostgreSQL 16 Authoritative Persistence
             ↓
     Transaction Commit
             ↓
  MessagingRealtimePublisher (After-Commit Hook)
             ↓
  Recipient & Sender STOMP User Queues (/user/queue/...)
  ```
- **Durability Guarantee**: PostgreSQL is the sole authoritative source of truth. WebSocket is transport-only. If a client is offline or network delivery fails after database commit, messages remain durable and are recovered via REST pagination.

---

### B. Separation From Community Channels
Platform Direct Messages and Community Channels are strictly partitioned:
- **No Shared Message Tables**: Platform DMs are stored in `conversations`, `conversation_participants`, and `direct_messages`. Community channel messages remain in `community_channel_messages`.
- **No Conceptual Bleed**: Community channels cannot be converted into private DMs. Community roles (`OWNER`, `MODERATOR`, `MEMBER`) have zero authority over Direct Messaging.
- **Distinct WebSocket Pipelines**: Community realtime uses `/ws/community` broadcasting to `/topic/communities/...`. Messaging realtime uses `/ws/messages` routing to private `/user/queue/...` destinations. `CommunityStompAuthorizationInterceptor` and `MessagingStompAuthorizationInterceptor` guard their respective namespaces independently.

---

### C. V24 Messaging Migration
The database migration `V24__create_direct_messaging_schema.sql` was applied cleanly to PostgreSQL 16 on port 5433, upgrading Flyway schema version from 23 to 24:
- **Tables Created**:
  1. `conversations`: Core conversation entity with direct low/high user IDs.
  2. `conversation_participants`: Per-user state (mute, archive, monotonic read cursor).
  3. `direct_messages`: Message storage with versioning, soft deletion, and client message ID.
  4. `message_attachments`: File metadata referencing authorized object storage keys.
  5. `message_reactions`: Per-user single emoji reactions.
  6. `message_receipts`: Message delivery and read acknowledgment timestamps.
- **Indexes & Constraints**:
  - `chk_direct_pair_ordering`: `CHECK (direct_user_low_id < direct_user_high_id)`.
  - `uq_conversations_direct_pair`: `UNIQUE (direct_user_low_id, direct_user_high_id)`.
  - `uq_direct_messages_client_id`: `UNIQUE (conversation_id, sender_id, client_message_id) WHERE client_message_id IS NOT NULL`.
  - `idx_direct_messages_conversation_created`: Keyset sorting on `(conversation_id, created_at DESC, id DESC)`.
  - `idx_conversation_participants_user_archived`: User conversation list index on `(user_id, archived)`.

---

### D. Conversation Model
- **Entity**: `Conversation.java` (`conversations`)
- **Direct Pair Normalization**: For users $A$ and $B$, low and high IDs are computed deterministically:
  $$\text{low} = \min(A, B), \quad \text{high} = \max(A, B)$$
- **Race Condition Safety**: A database-level unique constraint prevents concurrent creation races ($A \to B$ vs $B \to A$). If a concurrent race occurs, the losing thread catches `DataIntegrityViolationException` and atomically reloads the existing conversation.
- **Denormalized Previews**: `last_message_id` and `last_message_at` are maintained on the conversation record transactionally on every message creation.
- **Empty Conversations**: Conversations without messages (`last_message_id IS NULL`) are omitted from conversation lists (`findInitialConversations`, `findConversationsAfterCursor`) to avoid clutter.

---

### E. Participant Model
- **Entity**: `ConversationParticipant.java` with composite key `ConversationParticipantId(conversationId, userId)`
- **Properties**:
  - `notifications_muted`: Suppresses future notifications for this participant.
  - `archived`: Hides conversation from active list without affecting the other user.
  - `last_read_message_id` & `last_read_at`: Authoritative monotonic read cursor.
- **Group Seam**: Extensible to $N \ge 3$ participants in future phases without schema migration.

---

### F. Message Model
- **Entity**: `DirectMessage.java` (`direct_messages`)
- **Fields**: `id`, `conversationId`, `senderId`, `clientMessageId`, `messageType`, `body`, `replyToMessageId`, `status`, `createdAt`, `updatedAt`, `editedAt`, `deletedAt`, `version`.
- **Message Types**: `TEXT`, `ATTACHMENT`, `MIXED`. Validation requires a non-blank body OR at least one valid attachment.
- **Optimistic Locking**: `@Version` annotation prevents lost updates during concurrent edits.
- **Soft Deletion**: `status = DELETED` and `deleted_at = NOW()`. DTO masking omits the message body and attachments (`deleted = true`, `body = null`), preserving thread integrity, reply references, and delivery audit history.

---

### G. Idempotent Send Architecture
- **Client Idempotency Token**: Mobile clients supply a unique `clientMessageId (UUID)` with each send attempt.
- **Database Guarantee**: `UNIQUE (conversation_id, sender_id, client_message_id) WHERE client_message_id IS NOT NULL`.
- **Handling**: If the same clientMessageId is re-sent (e.g. client timeout or network retry), `MessagingServiceImpl` queries the database, detects the existing message, and returns it immediately without creating duplicates or re-firing events.

---

### H. Attachment Model
- **Entity**: `MessageAttachment.java` (`message_attachments`)
- **Security Validation**:
  - `storageKey` is validated against path traversal: `..`, `:`, leading `/`, and backslashes are strictly forbidden. Regex pattern `^[a-zA-Z0-9_\-/]+\.[a-zA-Z0-9]+$` enforced.
  - MIME type whitelist: `image/jpeg`, `image/png`, `image/webp`, `image/gif`, `audio/mpeg`, `audio/ogg`, `audio/mp4`, `video/mp4`, `video/webm`, `video/quicktime`, `application/pdf`.
  - Bounded size: Max 25 MB per attachment; max 10 attachments per message.
- **Storage Integration**: Storage keys reference the shared `StorageService`. Temporary signed URLs are generated upon DTO hydration with 60-minute expiry.

---

### I. Reply Architecture
- **Reference**: `reply_to_message_id` on `DirectMessage`.
- **Validation**:
  - Referenced message must exist.
  - Referenced message must belong to the same conversation (cross-conversation replies are rejected with HTTP 400).
  - Deleted reply targets are hydrated with placeholder `"Message deleted"`.
- **Serialization**: Lightweight `ReplyMessagePreview` avoids deep recursive JSON tree serialization.

---

### J. Reaction Model
- **Entity**: `MessageReaction.java` (`message_reactions`)
- **Product Rule**: At most one reaction per user per message (`UNIQUE (message_id, user_id)`).
- **Behavior**:
  - Adding a reaction when one already exists updates the emoji string.
  - Deleting a reaction removes the record.
  - Adding a reaction to a soft-deleted message is rejected.
  - Reaction changes emit `REACTION_UPDATED` realtime events.

---

### K. Delivery Receipts
- **Entity**: `MessageReceipt.java` (`message_receipts`) with composite key `(message_id, user_id)`.
- **Semantics**:
  - `SENT`: Message committed to PostgreSQL.
  - `DELIVERED`: Recipient client acknowledged delivery via STOMP `/app/messages/delivery-ack` or REST `POST /api/v1/messages/{messageId}/delivered`.
  - `READ`: Recipient marked conversation as read.
- **Idempotency**: Acknowledging an already delivered message is a no-op.

---

### L. Read Receipts / Unread Counts
- **Endpoint**: `POST /api/v1/messages/conversations/{conversationId}/read` with `lastReadMessageId`.
- **Monotonic Cursor**: Server verifies the message belongs to the conversation and its `createdAt` is $\ge$ participant's `last_read_at`. Older cursors are ignored.
- **Receipt Updates**: Unread receipt rows up to the cursor are updated to `read_at = NOW()` via `@Modifying` bulk query.
- **Unread Count**:
  - If `last_read_at IS NULL`: counts all active messages where `sender_id != currentUserId`.
  - If `last_read_at IS NOT NULL`: counts active messages where `sender_id != currentUserId` and `created_at > last_read_at`.
  - Parameter types are bound explicitly to avoid PostgreSQL untyped parameter inference errors.

---

### M. Block Semantics
- **Centralized Check**: `NetworkAccessService.isBlockedEitherDirection(viewerId, targetUserId)`.
- **Direct Messaging Policy**:
  - Creating a new conversation between blocked users is rejected (`BlockedUserException`, HTTP 403).
  - Existing conversation history remains readable by both participants (never purged).
  - Sending messages, adding reactions, and emitting typing indicators are blocked.
  - Unblocking immediately restores normal send and reaction eligibility.

---

### N. REST API Surface
All endpoints live under `/api/v1/messages`:
| Method | Path | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/conversations/direct/{targetUserId}` | Get or create direct 1:1 conversation | 201 Created |
| `GET` | `/conversations` | Keyset paginated conversation list | 200 OK |
| `GET` | `/conversations/{conversationId}` | Single conversation details | 200 OK |
| `GET` | `/conversations/{conversationId}/messages` | Keyset paginated message history | 200 OK |
| `POST` | `/conversations/{conversationId}/messages` | Send message (idempotent via clientMessageId) | 201 Created |
| `PATCH` | `/{messageId}` | Edit message body (sender only) | 200 OK |
| `DELETE`| `/{messageId}` | Soft delete message (sender only) | 200 OK |
| `PUT` | `/{messageId}/reaction` | Add or update emoji reaction | 200 OK |
| `DELETE`| `/{messageId}/reaction` | Remove emoji reaction | 200 OK |
| `POST` | `/{messageId}/delivered` | Acknowledge delivery via REST | 200 OK |
| `POST` | `/conversations/{conversationId}/read` | Advance read cursor monotonically | 200 OK |
| `PUT` | `/conversations/{conversationId}/mute` | Mute notifications | 200 OK |
| `DELETE`| `/conversations/{conversationId}/mute` | Unmute notifications | 200 OK |
| `PUT` | `/conversations/{conversationId}/archive` | Archive conversation for caller | 200 OK |
| `DELETE`| `/conversations/{conversationId}/archive` | Unarchive conversation | 200 OK |

---

### O. Message Pagination
- **Strategy**: Keyset cursor pagination avoiding `OFFSET`.
- **Conversations**: Cursor `(lastMessageAt, conversationId)`.
- **Messages**: Cursor `(createdAt, messageId)`.
- **Encoding**: URL-safe Base64 JSON via `MessagingCursorCodec`. Invalid or tampered cursors return HTTP 400 `InvalidMessagingCursorException`.

---

### P. WebSocket Architecture
- **Endpoint**: `/ws/messages` (registered alongside `/ws/community` in `CommunityWebSocketConfig`).
- **Destination Prefixes**:
  - App prefix: `/app`
  - Simple broker: `/topic`, `/queue`
  - User destination prefix: `/user`
- **Private User Queues**:
  - `/user/queue/messages`: New messages delivered to sender and recipient sessions.
  - `/user/queue/message-events`: Message edits, soft deletes, reactions, presence updates.
  - `/user/queue/receipts`: Delivery and read receipts.
  - `/user/queue/typing`: Ephemeral typing start/stop events.
- **After-Commit Publishing**: `MessagingRealtimePublisherImpl` registers Spring transaction synchronizations via `TransactionSynchronizationManager.registerSynchronization(afterCommit)`. Realtime frames are never sent if the database transaction fails.

---

### Q. STOMP/JWT Security
- **Interceptor**: `MessagingStompAuthorizationInterceptor` intercepts inbound STOMP frames:
  - `CONNECT`: Extracts `Authorization: Bearer <token>`, validates JWT via `JwtService`, and populates `SecurityContext`.
  - `SUBSCRIBE`: Validates authenticated principal and restricts subscriptions exclusively to allowed private queues (`/user/queue/...`).
  - `SEND`: Restricts client STOMP sends to `/app/messages/typing` and `/app/messages/delivery-ack`.
- **Community Isolation**: Yields community topic destinations (`/topic/communities/...`) to `CommunityStompAuthorizationInterceptor`.

---

### R. Realtime Event Inventory
All events use `RealtimeEventPayload(eventType, conversationId, data, timestamp)` with typed enum `MessagingEventType`:
1. `MESSAGE_CREATED`: Dispatched to recipient and sender multi-device sessions.
2. `MESSAGE_UPDATED`: Dispatched on body edit.
3. `MESSAGE_DELETED`: Dispatched on soft deletion.
4. `REACTION_UPDATED`: Dispatched when reaction is added, updated, or removed.
5. `MESSAGE_DELIVERED`: Dispatched to sender when recipient acknowledges receipt.
6. `CONVERSATION_READ`: Dispatched to sender when recipient marks conversation read.
7. `TYPING_STARTED` & `TYPING_STOPPED`: Dispatched to other participant.
8. `PRESENCE_CHANGED`: Dispatched on user connect/disconnect.

---

### S. Typing Indicators
- **Storage**: Ephemeral Redis key `collobster:messages:typing:{conversationId}:{userId}` with 6-second TTL (`MessagingProperties.typingTtl`).
- **Database Safety**: Never written to PostgreSQL.
- **Auto-Expiry**: Automatically expires in Redis even if client never sends `TYPING_STOPPED`.

---

### T. Presence / Multi-Device Presence
- **Storage**:
  - User status: `collobster:messages:presence:{userId}` = `"ONLINE"` with 60-second TTL.
  - Session set: `collobster:messages:presence:sessions:{userId}` (Redis Set of active STOMP session IDs).
- **Multi-Device Behavior**:
  - When Device 1 connects: added to session set; status set to `ONLINE`.
  - When Device 2 connects: added to session set; TTL refreshed.
  - When Device 1 disconnects: removed from session set. Active count is still 1; user remains `ONLINE`.
  - When Device 2 disconnects: removed from session set. Active count is 0; presence keys deleted; user marked `OFFLINE`.
- **Privacy**: Presence is evaluated only when hydrating participants in an existing conversation; no public global enumeration endpoint.

---

### U. Redis Ephemeral State
- **Namespaces**:
  - `collobster:messages:typing:...`
  - `collobster:messages:presence:...`
  - `collobster:messages:ratelimit:...`
- **Isolation**: Completely separate from `collobster:feed:...` and recommendation cache keys.
- **Degradation**: If Redis is offline, presence and typing degrade gracefully without disrupting PostgreSQL message persistence.

---

### V. Multi-Instance Realtime Considerations
- The current implementation uses Spring's `SimpleBrokerMessageHandler` for single-node deployments.
- For multi-instance clustered deployments, the WebSocket broker configuration in `CommunityWebSocketConfig` provides configuration properties (`collabster.communities.websocket.relay.enabled`) for external STOMP relays (RabbitMQ / ActiveMQ Artemis) or Redis pub/sub message relays.

---

### W. Rate Limiting
`MessagingRateLimiter` enforces rate limits using Redis atomic scripts with local Bucket4j fallback:
- Message send: 60/min
- Conversation creation: 30/hr
- Reactions: 60/min
- Typing: 120/min
Exceeding capacity throws `MessagingRateLimitExceededException` (HTTP 429).

---

### X. Messaging Security / IDOR Results
- **No Sender ID Spoofing**: `senderId` is extracted strictly from `CurrentUserService.getCurrentUser().getId()`. Client-supplied IDs are ignored.
- **IDOR Protection**: Every conversation or message operation verifies participant membership via `MessagingAccessService.requireParticipant`.
- **Tampering Protection**: Keyset cursors are validated and bounds-checked. Attachment storage keys reject traversal attacks.

---

### Y. Messaging Test Results
54 tests dedicated to the Messaging domain were executed with **100% pass rate**:
- `MessagingPostgresIntegrationTest`: 6 tests (real PostgreSQL 16 database constraints, lifecycle, block freezing, pagination)
- `MessagingServiceTest`: 15 tests (business logic, idempotency, soft-delete, replies, reactions, receipts)
- `MessagingControllerTest`: 9 tests (MockMvc REST endpoints, HTTP status codes, validation)
- `MessagingAccessServiceTest`: 9 tests (IDOR, self-messaging, disabled users, block checks)
- `MessagingWebSocketSecurityTest`: 8 tests (STOMP CONNECT, JWT authentication, destination authorization)
- `MessagingPresenceAndTypingTest`: 5 tests (multi-device presence, typing TTL, Redis session cleanup)
- `MessagingRateLimiterTest`: 2 tests (rate limit enforcement and local fallback)

---

### Z. PostgreSQL / Flyway / Hibernate Verification
- **Flyway**: Successfully applied `V24__create_direct_messaging_schema.sql` (24 total migrations validated).
- **Hibernate**: `spring.jpa.hibernate.ddl-auto=validate` passed with zero schema mismatches.
- **Constraints Verified**:
  - Check constraint `chk_direct_pair_ordering` strictly enforces `direct_user_low_id < direct_user_high_id`.
  - Unique constraint `uq_conversations_direct_pair` blocks duplicate direct conversation creation.
  - Partial unique index `uq_direct_messages_client_id` blocks duplicate clientMessageId sends.

---

### AA. Redis Verification
- Verified real Redis 7 integration for:
  - Ephemeral typing TTL expiration.
  - Ephemeral presence TTL and multi-session set tracking.
  - Rate limiting sliding windows.
  - Key prefix isolation (`collobster:messages:*`).

---

### AB. Community WebSocket Regression
- Verified Community WebSocket infrastructure:
  - `WebSocketSecurityTest`: 4 tests passed.
  - `CommunityE2EFlowsTest`: 6 tests passed.
  - All 42 Community-related tests passed with zero regressions.
  - Community destination routing (`/topic/communities/...`) remains 100% operational.

---

### AC. Full Java Regression
- **Command**: `.\mvnw.cmd test`
- **Result**: **384 tests executed, 0 failures, 0 errors, 1 skipped** (LivePythonRecommendationIntegrationTest intentionally skipped without running python service).
- **Breakdown**:
  - Auth / Security: 15 tests
  - User / Profile / Workspace: 28 tests
  - Community: 42 tests
  - PlatformPost: 24 tests
  - Network: 18 tests
  - Interaction: 22 tests
  - Feed: 85 tests
  - Recommendation (Circuit Breaker & Client): 48 tests
  - Explore: 48 tests
  - Messaging (Phase 6): 54 tests
  - Application context: 1 test

---

### AD. Files Added / Modified

#### Files Added (Messaging Domain):
- `db/migration/V24__create_direct_messaging_schema.sql`
- `messaging/config/MessagingProperties.java`
- `messaging/controller/MessagingController.java`
- `messaging/cursor/ConversationCursor.java`
- `messaging/cursor/MessageCursor.java`
- `messaging/cursor/MessagingCursorCodec.java`
- `messaging/dto/AttachmentRequest.java`
- `messaging/dto/AttachmentResponse.java`
- `messaging/dto/ConversationPageResponse.java`
- `messaging/dto/ConversationResponse.java`
- `messaging/dto/DeliveryAckPayload.java`
- `messaging/dto/DeliveryReceiptRequest.java`
- `messaging/dto/EditMessageRequest.java`
- `messaging/dto/MessagePageResponse.java`
- `messaging/dto/MessageResponse.java`
- `messaging/dto/MessageSummary.java`
- `messaging/dto/ParticipantSummary.java`
- `messaging/dto/ReactionRequest.java`
- `messaging/dto/ReactionResponse.java`
- `messaging/dto/ReadReceiptRequest.java`
- `messaging/dto/RealtimeEventPayload.java`
- `messaging/dto/ReplyMessagePreview.java`
- `messaging/dto/SendMessageRequest.java`
- `messaging/dto/TypingMessagePayload.java`
- `messaging/dto/TypingRequest.java`
- `messaging/entity/Conversation.java`
- `messaging/entity/ConversationParticipant.java`
- `messaging/entity/ConversationParticipantId.java`
- `messaging/entity/DirectMessage.java`
- `messaging/entity/MessageAttachment.java`
- `messaging/entity/MessageReaction.java`
- `messaging/entity/MessageReceipt.java`
- `messaging/entity/MessageReceiptId.java`
- `messaging/enums/AttachmentMediaType.java`
- `messaging/enums/ConversationType.java`
- `messaging/enums/MessageDeliveryState.java`
- `messaging/enums/MessageStatus.java`
- `messaging/enums/MessageType.java`
- `messaging/enums/MessagingEventType.java`
- `messaging/event/DirectMessageCreatedEvent.java`
- `messaging/event/DirectMessageReactionEvent.java`
- `messaging/exception/BlockedUserException.java`
- `messaging/exception/ConversationNotFoundException.java`
- `messaging/exception/InvalidAttachmentException.java`
- `messaging/exception/InvalidMessagingCursorException.java`
- `messaging/exception/MessageNotFoundException.java`
- `messaging/exception/MessagingAccessDeniedException.java`
- `messaging/exception/MessagingException.java`
- `messaging/exception/MessagingExceptionHandler.java`
- `messaging/exception/MessagingRateLimitExceededException.java`
- `messaging/mapper/MessagingMapper.java`
- `messaging/presence/MessagingPresenceService.java`
- `messaging/presence/MessagingPresenceServiceImpl.java`
- `messaging/presence/MessagingTypingService.java`
- `messaging/presence/MessagingTypingServiceImpl.java`
- `messaging/repository/ConversationParticipantRepository.java`
- `messaging/repository/ConversationRepository.java`
- `messaging/repository/DirectMessageRepository.java`
- `messaging/repository/MessageAttachmentRepository.java`
- `messaging/repository/MessageReactionRepository.java`
- `messaging/repository/MessageReceiptRepository.java`
- `messaging/security/MessagingAccessService.java`
- `messaging/security/MessagingAccessServiceImpl.java`
- `messaging/security/MessagingRateLimiter.java`
- `messaging/service/MessagingAttachmentService.java`
- `messaging/service/MessagingAttachmentServiceImpl.java`
- `messaging/service/MessagingService.java`
- `messaging/service/MessagingServiceImpl.java`
- `messaging/websocket/MessagingRealtimePublisher.java`
- `messaging/websocket/MessagingRealtimePublisherImpl.java`
- `messaging/websocket/MessagingStompAuthorizationInterceptor.java`
- `messaging/websocket/MessagingWebSocketController.java`
- `messaging/websocket/MessagingWebSocketEventListener.java`
- Test files:
  - `src/test/java/com/collabsphere_first/messaging/MessagingPostgresIntegrationTest.java`
  - `src/test/java/com/collabsphere_first/messaging/controller/MessagingControllerTest.java`
  - `src/test/java/com/collabsphere_first/messaging/presence/MessagingPresenceAndTypingTest.java`
  - `src/test/java/com/collabsphere_first/messaging/security/MessagingAccessServiceTest.java`
  - `src/test/java/com/collabsphere_first/messaging/security/MessagingRateLimiterTest.java`
  - `src/test/java/com/collabsphere_first/messaging/service/MessagingServiceTest.java`
  - `src/test/java/com/collabsphere_first/messaging/websocket/MessagingWebSocketSecurityTest.java`

#### Files Modified (Infrastructure Integration Only):
- `community/config/CommunityStompAuthorizationInterceptor.java`: Added pass-through for messaging destinations (`/user/`, `/queue/`, `/app/messages/`).
- `community/config/CommunityWebSocketConfig.java`: Registered `/ws/messages` endpoint, enabled `/queue` broker prefix, set `/user` destination prefix, and wired `MessagingStompAuthorizationInterceptor`.
- `security/config/SecurityConfig.java`: Added `/ws/messages` and `/ws/messages/**` to HTTP permitAll for initial handshake.
- `docs/COLLOBSTER_BACKEND_ARCHITECTURE.md`: Added Section XI for Phase 6.

---

### AE. Known Limitations
1. **Single-Instance In-Memory WebSocket Relay**: The default configuration runs Spring's simple in-memory message broker. Clustered horizontal scaling requires configuring the external STOMP broker relay (RabbitMQ / ActiveMQ Artemis).
2. **No Message Full-Text Search**: In compliance with Phase 6 requirements, DM search was deferred to prevent premature privacy and vector search index overhead.
3. **No End-to-End Encryption (E2EE)**: Transport uses TLS (HTTPS/WSS); storage follows standard backend encryption controls. E2EE is not claimed or implemented.

---

### AF. Future Group Conversation Seam
- `conversations.type` is an extensible `VARCHAR` enum (`DIRECT`, `GROUP`).
- `conversation_participants` models $N$ participants with composite keys and individual read cursors.
- To enable group conversations in a future phase, a `group_metadata` table or conversation metadata columns (group name, avatar, owner) can be added without modifying the message or participant schemas.

---

### AG. Future Global Notification Seam
- Messaging publishes Spring domain events (`DirectMessageCreatedEvent`, `DirectMessageReactionEvent`).
- When Phase 7 Global Notifications are introduced, notification listeners will subscribe to these events asynchronously to dispatch APNs, FCM, and email notifications.

---

### AH. Explicit Confirmation: Python Was NOT Modified
`Recommendation-System/` was **NOT** modified. Python code does not participate in Direct Messaging, does not rank private messages, does not ingest message content, and has no messaging endpoints.

---

### AI. Explicit Confirmation: Group Messaging Was NOT Implemented
No Group Messaging APIs, endpoints, or group membership operations were implemented. Phase 6 exclusively implements 1:1 Direct Messaging.

---

### AJ. Explicit Confirmation: Global Notifications Were NOT Implemented
No push notification services (FCM/APNs), email notification dispatchers, or notification tables were created. Phase 6 exclusively publishes internal domain events.
