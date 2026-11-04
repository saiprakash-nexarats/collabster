# Platform Security Authorization Matrix

## 1. Overview & Architectural Principles

This document defines the authoritative, platform-wide access control and security authorization rules for Collobster (Phase 10 Hardening).

### Invariants:
1. **Actor Identity Authority**: In all authenticated contexts, the actor identity is derived exclusively from `SecurityContext` / `CurrentUserService` (never accepted from client-supplied request bodies such as `userId`, `authorId`, `senderId`, `ownerId`, or `reporterId`).
2. **Platform Role vs Community Role**: Platform administrative authorities (`ROLE_ADMIN`) and Community moderator/admin authorities (`COMMUNITY:OWNER`, `COMMUNITY:ADMIN`, `COMMUNITY:MODERATOR`, `COMMUNITY:MEMBER`) are strictly segregated. Community roles never grant platform administrative rights, and community role checks are scoped strictly to a specific `communityId`.
3. **Block Rule Primacy**: Blocking between User A and User B (`NetworkAccessService.isBlockedEitherDirection(A, B)`) is absolute for all new interpersonal interactions (following, connections, DMs, reactions, typing, notifications, passive Feed/Explore discovery).
4. **Defense in Depth**: Authorization checks occur prior to sensitive DTO hydration to prevent Insecure Direct Object References (IDOR).

---

## 2. Global Authorization Matrix

| Resource | Action | Actor | Required Relationship / Role | Visibility Rule | Block Rule | Ownership Rule | Moderator/Admin Override | Failure Response |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **User/Profile** | `Create Profile` | Authenticated | `ROLE_GUEST` | Self | N/A | Must be caller | None | `400 Bad Request` / `409 Conflict` |
| **User/Profile** | `View My Profile` | Authenticated | Any active role | Self | N/A | Must be caller | None | `401 Unauthorized` |
| **User/Profile** | `Update Profile` | Authenticated | Any active role | Self | N/A | Must be caller; cannot select `ROLE_ADMIN` | None | `400 Bad Request` / `403 Forbidden` |
| **User/Profile** | `Deactivate Account` | Authenticated | Any active role | Self | N/A | Must be caller | Platform `ROLE_ADMIN` can suspend/deactivate | `403 Forbidden` |
| **User/Profile** | `Delete Account` | Authenticated | Any active role | Self | N/A | Must be caller | Platform `ROLE_ADMIN` | `403 Forbidden` |
| **User/Profile** | `View Public Profile` | Authenticated | Any active role | Public safe fields | Blocked user hidden / suppressed | N/A | Platform `ROLE_ADMIN` | `404 Not Found` |
| **PlatformPost** | `Create Post` | Authenticated | Non-guest active role | Configured visibility | N/A | Created with actor as author | None | `403 Forbidden` |
| **PlatformPost** | `Get Post By ID` | Authenticated | Any active role | `PUBLIC`: All non-blocked; `FOLLOWERS_ONLY`: Must follow author; `PRIVATE`: Author only | Denied if caller and author blocked either direction | Caller must be author if `PRIVATE` | Platform `ROLE_ADMIN` | `403 Forbidden` / `404 Not Found` |
| **PlatformPost** | `Update Post` | Authenticated | Active author | Any | N/A | Must be author | None | `403 Forbidden` |
| **PlatformPost** | `Delete Post` | Authenticated | Active author or Admin | Any | N/A | Author or Platform `ROLE_ADMIN` | Platform `ROLE_ADMIN` (soft-delete with audit) | `403 Forbidden` |
| **PlatformPost** | `List My Posts` | Authenticated | Active author | Self | N/A | Scoped to caller | None | `401 Unauthorized` |
| **Community** | `Create Community` | Authenticated | Non-guest active role | Rate-limited | N/A | Actor becomes `COMMUNITY:OWNER` | None | `429 Rate Limit` / `400 Bad Request` |
| **Community** | `View Public Community` | Authenticated | Any active role | `PUBLIC` & `ACTIVE` | Membership independent | N/A | Platform `ROLE_ADMIN` | `404 Not Found` |
| **Community** | `View Private Community` | Authenticated | Active member | `RESTRICTED`/`PRIVATE` | Membership independent | Must have `COMMUNITY:MEMBER` or higher | Platform `ROLE_ADMIN` | `403 Forbidden` |
| **Community** | `Update Community Settings` | Authenticated | Community Admin | Community | N/A | Must have `COMMUNITY:ADMIN` or `OWNER` | Platform `ROLE_ADMIN` | `403 Forbidden` |
| **Community** | `Archive/Delete Community` | Authenticated | Community Owner | Community | N/A | Must have `COMMUNITY:OWNER` | Platform `ROLE_ADMIN` | `403 Forbidden` |
| **CommunityPost** | `Create Post` | Authenticated | Active member | Community | Membership independent | Actor is author | None | `403 Forbidden` / `429 Rate Limit` |
| **CommunityPost** | `Get Post Details` | Authenticated | Member (or public community) | `VISIBLE` or (author or moderator if pending) | Passive feed drops blocked author; direct get enforces community read | Author or `COMMUNITY:MODERATOR` if pending | Platform `ROLE_ADMIN` | `403 Forbidden` / `404 Not Found` |
| **CommunityPost** | `Moderate Post` | Authenticated | Community Moderator | Community | N/A | Requires `COMMUNITY:MODERATOR` or higher | Platform `ROLE_ADMIN` | `403 Forbidden` |
| **CommunityComment**| `Create Comment` | Authenticated | Active member | Post must be visible | Blocked either direction rejected | Actor is author | None | `403 Forbidden` / `429 Rate Limit` |
| **CommunityResource** | `Upload/Read Resource` | Authenticated | Active member | Community | N/A | Requires `COMMUNITY:MEMBER` | `COMMUNITY:MODERATOR` / Platform `ROLE_ADMIN` | `403 Forbidden` |
| **CommunityEvent** | `Create/RSVP Event` | Authenticated | Active member | Community | N/A | Requires `COMMUNITY:MEMBER` | `COMMUNITY:MODERATOR` / Platform `ROLE_ADMIN` | `403 Forbidden` |
| **CommunityPoll** | `Create/Vote Poll` | Authenticated | Active member | Community | N/A | Requires `COMMUNITY:MEMBER` | `COMMUNITY:MODERATOR` / Platform `ROLE_ADMIN` | `403 Forbidden` |
| **CommunityChannel** | `List/View Channel` | Authenticated | Active member | Community | N/A | Requires `COMMUNITY:MEMBER` | `COMMUNITY:ADMIN` / Platform `ROLE_ADMIN` | `403 Forbidden` |
| **CommunityChannelMessage** | `Send Message` | Authenticated | Active writable member | Channel | N/A | Member status != `MUTED` | `COMMUNITY:MODERATOR` / Platform `ROLE_ADMIN` | `403 Forbidden` |
| **DirectConversation** | `Start Conversation` | Authenticated | Active user | Target user | Denied if either direction blocked | Cannot create conversation with self | None | `403 Forbidden` / `400 Bad Request` |
| **DirectConversation** | `List Conversations` | Authenticated | Active participant | Scoped to caller | Displays active + archived | Scoped to caller | None | `401 Unauthorized` |
| **DirectMessage** | `Send Message` | Authenticated | Active participant | Conversation | Denied if either direction blocked; conversation read-only while blocked | Actor is sender | None | `403 Forbidden` |
| **DirectMessage** | `React to Message` | Authenticated | Active participant | Message in conversation | Denied if either direction blocked | Participant only | None | `403 Forbidden` |
| **DirectMessage** | `Send Typing Indicator` | Authenticated | Active participant | Conversation | Denied if either direction blocked | Participant only | None | `403 Forbidden` |
| **DirectMessage** | `Get Attachment URL` | Authenticated | Active participant | Conversation attachment | Scoped to participants | Must be conversation participant | Platform `ROLE_ADMIN` under explicit audit | `403 Forbidden` / `404 Not Found` |
| **Notification** | `List/Get Notifications`| Authenticated | Recipient user | Recipient only | Suppressed if generated by blocked actor | Scoped to `recipientUserId == caller.id` | None | `403 Forbidden` / `404 Not Found` |
| **Notification** | `Mark Read/Seen/Archive` | Authenticated | Recipient user | Recipient only | N/A | Scoped to `recipientUserId == caller.id` | None | `403 Forbidden` / `404 Not Found` |
| **NotificationDevice** | `Register Device` | Authenticated | Device owner | Scoped to caller | N/A | Device bound to `caller.id` | None | `403 Forbidden` |
| **NotificationDevice** | `Revoke Device` | Authenticated | Device owner | Scoped to caller | N/A | `device.userId == caller.id` | None | `403 Forbidden` / `404 Not Found` |
| **NotificationPref** | `Update Preferences` | Authenticated | Preference owner | Scoped to caller | N/A | `pref.userId == caller.id` | None | `403 Forbidden` |
| **UploadedMedia** | `Register / Upload` | Authenticated | Active user | Quota / MIME verified | N/A | `ownerUserId == caller.id` | None | `403 Forbidden` / `400 Bad Request` |
| **UploadedMedia** | `Attach Media to Resource` | Authenticated | Asset owner | Validated storage key | N/A | Must match `ownerUserId == caller.id` | None | `403 Forbidden` |
| **Interaction** | `Like / Save / Share` | Authenticated | Active user | Target content must be visible | Denied if actor and content author blocked either direction | Actor is interactor | None | `403 Forbidden` |
| **Report (Platform)** | `File Report` | Authenticated | Active user | Target must be accessible | Anti-spam uniqueness enforced | Reporter ID from `SecurityContext` | None | `403 Forbidden` / `409 Conflict` / `429 Rate Limit` |
| **Report (Platform)** | `List/Resolve Reports` | Authenticated | Platform Admin | Admin moderation queue | Reporter identity protected from reported user | Scoped to Platform Admin | Platform `ROLE_ADMIN` only | `403 Forbidden` |

---

## 3. Block Semantics Across Domains

1. **Interpersonal Actions**:
   - Following: Rejected with `403 Forbidden` / `NetworkAccessDeniedException` if blocked either direction.
   - Connection Requests: Rejected with `403 Forbidden` / `NetworkAccessDeniedException` if blocked either direction.
   - Direct Messages: Initiating or sending new DMs is blocked with `BlockedUserException`. Existing message history remains readable (read-only state).
   - Reactions & Typing: Blocked with `BlockedUserException`.
2. **Passive Discovery & Ranking**:
   - Feed Hydration: Drops content authored by blocked or blocking users.
   - Explore Trending / Recent / Search: Drops content authored by blocked or blocking users.
   - Explore People: Suppresses blocked or blocking users from people search and candidate discovery.
   - Python Recommendations: Java final eligibility hydration drops any candidates authored by blocked users.
3. **Community Navigation**:
   - Community membership is independent of interpersonal blocks. A block does not evict either user from a shared community, but direct interpersonal messaging/interaction remains blocked.

---

## 4. Mute Semantics Matrix

| Mute Type | Target | Stored In | Scope / Effect | Does It Block Interpersonal Action? |
| :--- | :--- | :--- | :--- | :--- |
| **Network Content Mute** | User | `user_mutes` | Suppresses author's posts from viewer's Home Feed and Explore | No (messaging, profile, following intact) |
| **Messaging Conversation Mute** | Conversation | `conversation_participants.notifications_muted` | Suppresses push notifications and alerts for new messages in conversation | No (can still read and send messages) |
| **Notification Category Preference** | Category | `notification_preferences` | Disables push/email delivery for specific notification category | No (in-app center notifications still persist) |
| **Community Channel Mute** | Community / Channel | `memberships.status == MUTED` | Prevents writing/posting in community channels (read-only) | No |
| **Block** | User | `user_blocks` | Total severance of new interpersonal actions, DMs, follows, reactions, discovery | **Yes** (absolute interpersonal block) |

---

## 5. Phase 10.1 Hardening: Privacy Erasure, Device Logout, WebSocket Expiry & Trusted Proxies

### 5.1 Account Deletion & Downstream Erasure
- **Immediate Local Invalidation**: Account marked `deleted = true`, PII scrubbed, refresh/device tokens revoked. Access terminated immediately in Java.
- **Asynchronous Privacy Erasure**: Durable job queued in `user_data_erasure_outbox` (Flyway V29). Dispatched by background worker to internal Python endpoint `POST /api/v1/internal/recommend/privacy/erase`.
- **Decoupled Failure Semantics**: Python outage causes worker retry with backoff; Java account deletion never rolls back or fails.
- **Scope Restriction**: Python deletes only user's own `recommendation_events` records (`user_id == deletedUserId`) and Redis keys (`user:{id}:interactions:*`, `online_features:user:{id}`). Events authored by other users on deleted user's content are not deleted.

### 5.2 Device Logout vs. Logout-All
- **Current-Device Logout (`POST /api/v1/auth/logout`)**: Accepts optional `LogoutRequest` with `deviceId` or `pushToken`. Revokes the active session's refresh token and only the specified device's push token. Other devices remain active.
- **Global Logout (`POST /api/v1/auth/logout-all`)**: Revokes all refresh tokens, revokes all push devices, and evicts presence.
- **Device Ownership Validation**: A user cannot revoke another user's `deviceId` during logout (enforced with `403 Forbidden`).

### 5.3 WebSocket JWT Expiry Hardening
- **Derived Expiry**: STOMP `CONNECT` derives token expiration from validated JWT claims and registers it in `WebSocketAuthenticatedSessionRegistry`.
- **Inbound Gate**: All subsequent inbound frames (`SEND`, `SUBSCRIBE`) are rejected with `IllegalArgumentException` once expired.
- **Passive Subscription Leak Protection**: `WebSocketOutboundExpiryInterceptor` intercepts outbound frames; if session is expired, message is dropped and underlying `WebSocketSession` closed (code 4401).
- **Session Reaper**: Periodic reaper closes expired transport sessions and cleans presence.
- **Multi-Device Isolation**: Sessions expire independently per device token.

### 5.4 Trusted Proxy & Rate Limiting Hardening
- **Zero-Trust Default**: `X-Forwarded-For`, `X-Real-IP`, and `Forwarded` headers from arbitrary internet clients are strictly ignored. Only `request.getRemoteAddr()` is used unless peer matches `collobster.security.trusted-proxies`.
- **Right-to-Left Hop Resolution**: Trusted proxy chains are traversed right-to-left to determine the first untrusted IP. Malformed headers fail safe to `remoteAddr`.
- **Abuse Key Priority**: Authenticated requests key on `user:{userId}`, preventing IP rotation from bypassing rate limits. Anonymous requests key on `ip:{resolvedClientIp}`.
