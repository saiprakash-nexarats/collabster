# Community API contract

All routes below use `/api` and require an authenticated JWT unless stated otherwise. Responses use the existing `ApiResponse<T>` wrapper (`success`, `message`, `data`). Paged responses use Spring's `Page<T>` shape. Community and resource IDs are UUIDs; user IDs are numeric `Long`s. Errors use the wrapper's `success=false` and `message` fields.

## Authorization model

| Role | Scope |
|---|---|
| OWNER | Transfer ownership; archive, restore, and schedule/cancel deletion; all administrator actions |
| ADMIN | Settings, invitations, rules, audit, analytics, dashboard; member management |
| MODERATOR | Review join requests, moderate reports/content, manage channels, moderation dashboard |
| MEMBER | Create posts/comments/resources/messages/polls; participate and vote |
| FOLLOWER | Read-only membership |

Membership status is independent: `ACTIVE`, `PENDING_APPROVAL`, `LEFT`, `REJECTED`, `SUSPENDED`, `MUTED`, `BANNED`. Public community search only returns active public communities. Private community content requires active membership. Archived/inactive communities are read-only.

## Discovery

| Method / path | Auth / role | Request / query | Response |
|---|---|---|---|
| `GET /api/v1/communities` | Authenticated | `query?`, `type?`, `category?`, `visibility?`, `tag?`, `page=0`, `size=20` (max 100). Only public results are discoverable; requesting a non-public visibility returns an empty page. | `Page<CommunityDto>` |
| `GET /api/v1/communities/trending` | Authenticated | `page=0`, `size=20` (max 100) | `Page<CommunityDto>` ordered by activity/popularity |
| `GET /api/v1/communities/recommendations` | Authenticated | `page=0`, `size=20` (max 100) | Public communities ranked by the caller's active-community categories, tags, and activity |
| `GET /api/v1/communities/mine` | Authenticated | Pageable | `Page<MyCommunityDto>` with role/status and community summary |

## Community

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `POST /api/v1/communities` | Authenticated | `{name, slug, description?, type, category?, lifecycle?, visibility, tags?, requiresModeration?, linkedEventId?, linkedCourseId?, logoUrl?, coverUrl?}` | `201 CommunityDto`; category defaults to `OTHER`; creator becomes OWNER and `general` channel is created |
| `GET /api/v1/communities/{id}` | Authenticated; public or member | — | `CommunityDto`; non-member private lookup is hidden as 404 |
| `GET /api/v1/communities/slug/{slug}` | Authenticated; public or member | — | `CommunityDto` |
| `PATCH /api/v1/communities/{id}` | ADMIN+ | Partial community fields: `{name?, description?, visibility?, lifecycle?, tags?, requiresModeration?, logoUrl?, coverUrl?}` | `CommunityDto` |
| `DELETE /api/v1/communities/{id}` | OWNER / platform admin | — | Archives; preserves existing API semantics |

## Membership

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `POST /api/v1/communities/{id}/join` | Authenticated | Optional query `inviteToken` for legacy invite-only compatibility | `MembershipDto`; public joins activate, private joins pend, invite-only token must match a current invitation for this user |
| `DELETE /api/v1/communities/{id}/leave` | Authenticated member | — | Empty data; OWNER must transfer first; membership becomes `LEFT` |
| `GET /api/v1/communities/{id}/members` | MEMBER+ | Optional `status`, pageable | `Page<MembershipDto>` |
| `PATCH /api/v1/communities/{id}/members/{userId}` | ADMIN+ | `{role?, status?, notificationPref?}` | `MembershipDto` |
| `POST /api/v1/communities/{id}/members/{userId}/approve` | MODERATOR+ | — | `MembershipDto` |
| `POST /api/v1/communities/{id}/members/{userId}/reject` | MODERATOR+ | — | `MembershipDto` |

## Invitations

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `POST /api/v1/communities/{communityId}/invitations` | MEMBER if invitations enabled; otherwise ADMIN+ | `{inviteeId}` | `201 InvitationDto`; raw secure token is returned once, only its SHA-256 hash is stored |
| `GET /api/v1/communities/{communityId}/invitations` | ADMIN+ | `status=PENDING`, pageable | `Page<InvitationDto>` |
| `GET /api/v1/invitations/me` | Authenticated recipient | Pageable | `Page<InvitationDto>` for pending invitations |
| `POST /api/v1/invitations/{invitationId}/accept` | Invited recipient | — | `InvitationDto`; activates membership transactionally |
| `POST /api/v1/invitations/{invitationId}/decline` | Invited recipient | — | `InvitationDto` |
| `DELETE /api/v1/invitations/{invitationId}` | Inviter or ADMIN+ | — | Revokes pending invitation |

## Posts and comments

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `GET /api/v1/communities/{id}/posts` | Authenticated; public community or member | Pageable | `Page<PostDto>` |
| `POST /api/v1/communities/{id}/posts` | MEMBER+ | Existing `CreatePostRequest` | `201 PostDto`; follows community moderation setting |
| `GET /api/v1/posts/{id}` | Authenticated; public community or member | — | `PostDto`; hidden/flagged content requires author or moderator access |
| `PATCH /api/v1/posts/{id}/pin?pin=true|false` | ADMIN+ | — | `PostDto` |
| `PATCH /api/v1/posts/{id}/lock?lock=true|false` | MODERATOR+ | — | `PostDto` |
| `POST /api/v1/posts/{id}/comments` | MEMBER+ | `{body, parentCommentId?}` | `201 CommentDto`; reply parent must be on same post; locked posts reject comments |
| `GET /api/v1/posts/{id}/comments` | Authenticated; public community or member | Pageable | `Page<CommentDto>` |

## Resources

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `POST /api/v1/communities/{id}/resources` | MEMBER+ | Existing `CreateResourceRequest` (`FILE`, `LINK`, `WIKI`) | `201 ResourceDto`; moderated before visibility |
| `GET /api/v1/communities/{id}/resources` | FOLLOWER+ | Pageable | `Page<ResourceDto>`; private resources require membership |

## Channels and messages

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `GET /api/v1/communities/{communityId}/channels` | Member | — | `List<CommunityChannelDto>` |
| `POST /api/v1/communities/{communityId}/channels` | Configured role, minimum MODERATOR | `{name, description?, readOnly?}` | `201 CommunityChannelDto`; lowercase unique names; `general` is protected |
| `GET /api/v1/communities/{communityId}/channels/{channelId}` | Member | — | `CommunityChannelDto` |
| `PATCH /api/v1/communities/{communityId}/channels/{channelId}` | MODERATOR+ | `{name?, description?, readOnly?}` | `CommunityChannelDto` |
| `DELETE /api/v1/communities/{communityId}/channels/{channelId}` | ADMIN+ | — | Deletes channel and its messages; `general` cannot be deleted |
| `GET /api/v1/communities/{communityId}/channels/{channelId}/messages` | Member | Pageable (max 100) | `Page<CommunityChannelMessageDto>` |
| `POST /api/v1/communities/{communityId}/channels/{channelId}/messages` | Configured send role | `{body, parentMessageId?}` | `201 CommunityChannelMessageDto`; parent must be in the same channel |
| `PATCH /api/v1/communities/{communityId}/channels/{channelId}/messages/{messageId}` | Author or MODERATOR+ | `{body}` | `CommunityChannelMessageDto` |
| `DELETE /api/v1/communities/{communityId}/channels/{channelId}/messages/{messageId}` | Author or MODERATOR+ | — | Soft removes message body |
| `POST /api/v1/communities/{communityId}/channels/{channelId}/messages/{messageId}/reactions?reaction={value}` | Active writable member | — | `201 List<CommunityChannelMessageReactionDto>`; one identical reaction per user/message |
| `DELETE /api/v1/communities/{communityId}/channels/{channelId}/messages/{messageId}/reactions/{reaction}` | Active writable member | — | Remaining reaction summaries |
| `GET /api/v1/communities/{communityId}/channels/unread-counts` | Member | — | `List<ChannelUnreadDto>` for the caller only |
| `PUT /api/v1/communities/{communityId}/channels/{channelId}/read` | Member | `{lastReadMessageId}` | Marks that message and all earlier channel activity as read |

REST remains available for history and mutations. STOMP is available at `/ws/community`: clients send a `CONNECT` frame with `Authorization: Bearer <JWT>`, subscribe to `/topic/communities/{communityId}/channels/{channelId}`, and send new messages to `/app/communities/{communityId}/channels/{channelId}/messages`. Each inbound connection, subscription, and send action validates the member's active community access. Broadcast payloads are `{action, message}` and are emitted after the database transaction commits.

### WebSocket Horizontal Scaling Architecture
- **DEVELOPMENT:** By default, Spring's in-memory `SimpleBroker` handles local STOMP fanout on `/topic`.
- **PRODUCTION:** Configured with an external message broker relay (e.g. RabbitMQ, Redis STOMP, or ActiveMQ) via application configuration:
  - `collabster.communities.websocket.relay.enabled=true`
  - `collabster.communities.websocket.relay.host=${WS_RELAY_HOST:localhost}`
  - `collabster.communities.websocket.relay.port=${WS_RELAY_PORT:61613}`
  - `collabster.communities.websocket.relay.client-login=${WS_RELAY_CLIENT_LOGIN:guest}`
  - `collabster.communities.websocket.relay.client-passcode=${WS_RELAY_CLIENT_PASSCODE:guest}`
  - `collabster.communities.websocket.relay.system-login=${WS_RELAY_SYSTEM_LOGIN:guest}`
  - `collabster.communities.websocket.relay.system-passcode=${WS_RELAY_SYSTEM_PASSCODE:guest}`

## Polls

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `POST /api/v1/communities/{communityId}/polls` | Configured poll role | `{question, options:[...], allowMultiple?, closesAt?}` (2–10 options) | `201 CommunityPollDto` |
| `GET /api/v1/communities/{communityId}/polls` | Member | Pageable | `Page<CommunityPollDto>` with option vote totals |
| `GET /api/v1/polls/{pollId}` | Member | — | `CommunityPollDto` |
| `POST /api/v1/polls/{pollId}/vote` | Active member | `{optionIds:[UUID,...]}` | Updated `CommunityPollDto`; duplicate/single-choice votes rejected |
| `POST /api/v1/polls/{pollId}/close` | Creator or MODERATOR+ | — | Closed `CommunityPollDto` |

## Events

This repository has no global Events module, so a complete, self-contained Community-owned event and RSVP model is provided. Existing `CommunityDto.linkedEventId` remains available for external event references.

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `GET /api/v1/communities/{communityId}/events` | Public community or member | Pageable | `Page<CommunityEventDto>` |
| `POST /api/v1/communities/{communityId}/events` | MODERATOR+ | `{title, description?, startsAt, endsAt, location?}` | `201 CommunityEventDto`; event must start in the future |
| `GET /api/v1/events/{eventId}` | Public community or member | — | `CommunityEventDto` including caller RSVP and counts |
| `PUT /api/v1/events/{eventId}/rsvp` | Active member | `{status: GOING|MAYBE|DECLINED}` | Updated `CommunityEventDto` |
| `GET /api/v1/events/{eventId}/attendees` | Public community or member | Pageable | `Page<EventAttendeeDto>` for GOING attendees |

## Notifications

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `GET /api/v1/notifications` | Authenticated user | Pageable (max 100) | `Page<CommunityNotificationDto>` limited to the caller |
| `GET /api/v1/notifications/unread-count` | Authenticated user | — | `long` |
| `PATCH /api/v1/notifications/{id}/read` | Notification owner | — | `CommunityNotificationDto` |
| `PATCH /api/v1/notifications/read-all` | Authenticated user | — | Number updated |

All 31 Community domain lifecycle events (membership transitions, invitations, posts, comments, moderation, polls, resources, and events) are routed through `CommunityNotificationListener` with idempotency and preference enforcement (`ALL`, `DIRECT_ONLY`, `NONE`).

## Rules

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `GET /api/v1/communities/{communityId}/rules` | Public community or member | — | `List<CommunityRuleDto>` |
| `POST /api/v1/communities/{communityId}/rules` | ADMIN+ | `{title, body, position?}` | `201 CommunityRuleDto`, version 1 |
| `PATCH /api/v1/communities/{communityId}/rules/{ruleId}` | ADMIN+ | `{title?, body?, position?}` | Updated rule with incremented version |
| `DELETE /api/v1/communities/{communityId}/rules/{ruleId}` | ADMIN+ | — | Deactivates rule and records revision |
| `GET /api/v1/communities/{communityId}/rules/{ruleId}/versions` | Public community or member | — | Descending `List<CommunityRuleRevisionDto>` |

## Dashboard and analytics

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `GET /api/v1/communities/{communityId}/dashboard` | MODERATOR+ | — | Moderators get moderation counts; ADMIN+ gets available aggregate counts |
| `GET /api/v1/communities/{communityId}/analytics` | ADMIN+ | ISO-8601 `from?`, `to?` (defaults to last 30 days; max 366 days) | `CommunityAnalyticsDto` with aggregate counts, a daily activity series, top members by contribution score, and top channels by messages in range |

Analytics includes daily activity, member and channel leaderboards, and event RSVP activity.

## Settings

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `GET /api/v1/communities/{communityId}/settings` | MEMBER+ | — | `CommunitySettingsDto` |
| `PATCH /api/v1/communities/{communityId}/settings` | ADMIN+ | Partial boolean flags and role settings from `UpdateCommunitySettingsRequest` | `CommunitySettingsDto` |

Flags cover member posts, comments, channels, messages, polls, events, member invitations, and AI moderation. `PATCH /api/v1/communities/{id}` continues to manage name, description, visibility, tags, moderation, logo, and cover.

## Ownership, archive, restore, and deletion

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `POST /api/v1/communities/{id}/transfer-ownership?targetUserId={userId}` | OWNER | — | Previous owner becomes ADMIN; transfer and audit record are transactional |
| `DELETE /api/v1/communities/{id}` | OWNER / platform admin | — | Sets ARCHIVED, keeps existing route semantics |
| `POST /api/v1/communities/{id}/restore` | OWNER / platform admin | — | Restores an archived community |
| `POST /api/v1/communities/{id}/request-deletion` | OWNER / platform admin | — | Schedules deletion after a 30-day recovery period (`202 Accepted`) |
| `POST /api/v1/communities/{id}/cancel-deletion` | OWNER / platform admin | — | Cancels pending deletion |

## Reports and moderation

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `POST /api/v1/reports` | Authenticated | Existing request `{communityId, targetType, targetId, reason}`; `targetId` is a string UUID for post/comment/resource or decimal user ID for USER | `ReportDto` |
| `GET /api/v1/communities/{id}/reports` | MODERATOR+ | Pageable | `Page<ReportDto>` |
| `PATCH /api/v1/reports/{id}/resolve` | MODERATOR+ in report's community | Existing `ResolveReportRequest` | `ReportDto`; target is validated against same community |
| `GET /api/v1/communities/{id}/pending-posts` | MODERATOR+ | Pageable | `Page<PostDto>` |
| `PATCH /api/v1/posts/{id}/moderate` | MODERATOR+ in post's community | Existing approve/reject request | `PostDto` |

AI moderation defaults to manual review (`FLAGGED`). Set `COMMUNITY_AI_MODERATION_PROVIDER=openai` and `OPENAI_API_KEY` to enable the concrete OpenAI Moderations provider (`POST /v1/moderations`, default model `omni-moderation-latest`); a flagged response is routed to manual review. The generic HTTP adapter remains available via `COMMUNITY_AI_MODERATION_PROVIDER=http`. Provider failures all fall back to `FLAGGED` without blocking creation.

## Audit

| Method / path | Auth / role | Request | Response |
|---|---|---|---|
| `GET /api/v1/communities/{communityId}/audit-logs` | ADMIN+ | Pageable | `Page<CommunityAuditLogDto>` newest first |

Audit records include ownership transfer, community update/archive/restore/deletion scheduling, settings/rules/channel changes, membership changes, post pin/lock/moderation, and report resolution. Audit rows persist independently of hard-deleted communities.

## Frontend screen mapping

| Screen | API | Required role | Main data |
|---|---|---|---|
| Community Landing | `GET /api/v1/communities` | Authenticated | Public discovery page |
| Explore Communities | `GET /api/v1/communities?query=...&type=...&tag=...` | Authenticated | Filtered public page |
| Trending | `GET /api/v1/communities/trending` | Authenticated | Activity-ranked public page |
| My Communities | `GET /api/v1/communities/mine` | Authenticated | Membership role/status and community summary |
| Community Details | `GET /api/v1/communities/{id}` or `/slug/{slug}` | Public or member | Community metadata |
| Create Community | `POST /api/v1/communities` | Authenticated | CommunityDto; owner membership and `general` channel |
| Join Community | `POST /api/v1/communities/{id}/join` | Authenticated | Membership state |
| Pending Approval | `GET /api/v1/communities/{id}/members?status=PENDING_APPROVAL` and `/members/{userId}/approve|reject` | MODERATOR+ | Pending membership and decision |
| Community Home / Post Feed | `GET /api/v1/communities/{id}/posts` | Public or member | Paged visible posts |
| Post Details / Comments | `GET /api/v1/posts/{id}`, `/api/v1/posts/{id}/comments` | Public or member | Post and paged thread |
| Resources | `GET /api/v1/communities/{id}/resources` | FOLLOWER+ | Paged resources |
| Members | `GET /api/v1/communities/{id}/members` | MEMBER+ | Paged membership list |
| Channels / Chat | `/api/v1/communities/{id}/channels...` | Member; manager role for mutations | Channels and paged messages |
| Polls | `/api/v1/communities/{id}/polls`, `/api/v1/polls/{id}/vote` | Member | Poll options and results |
| Events | `/api/v1/communities/{id}/events`, `/api/v1/events/{eventId}/rsvp` | Public/member; MODERATOR+ create | Events, RSVP and attendees |
| Notifications | `/api/v1/notifications` | Authenticated | Caller-scoped notifications and unread count |
| Community Rules | `/api/v1/communities/{id}/rules` | Public or member; ADMIN+ to edit | Versioned rules |
| Moderator Dashboard | `GET /api/v1/communities/{id}/dashboard` | MODERATOR+ | Pending reports/moderation counts |
| Admin Dashboard | Same dashboard route | ADMIN+ | Aggregate counts |
| Community Settings | `/api/v1/communities/{id}/settings` | MEMBER+ read; ADMIN+ write | Feature settings |
| Audit Logs | `/api/v1/communities/{id}/audit-logs` | ADMIN+ | Administrative audit records |
| Archive / Restore | `DELETE` and `POST /api/v1/communities/{id}/restore` | OWNER | Lifecycle result |

## Production Architecture & Operational Notes

- **Realtime WebSocket / STOMP (`/ws/community`)**: Authenticated via Bearer JWT token in STOMP CONNECT headers. Channel SUBSCRIBE and SEND operations strictly enforce community and channel membership (`requireMember`, `requireWritableMember`). Single-node uses Spring's `SimpleBrokerMessageHandler`; multi-node horizontally scaled deployments should configure an external STOMP broker relay (e.g. RabbitMQ).
- **Transactional Outbox**: All domain events are recorded transactionally to the outbox database table and published asynchronously with exponential backoff and worker retry. Consumer processing is idempotent using event ID and idempotency keys.
- **Storage**: Backed by `FirebaseStorageServiceImpl` with HMAC-SHA256 URL signatures, strict expiration timestamps, MIME-type and extension validations, and path traversal protection.
- **AI Moderation**: Content is routed through OpenAI Moderations API with resilient timeout handling and safe fail-closed behavior (routes to `FLAGGED` state for manual moderator review if provider is down).
- **Redis Rate Limiting & Caching**: Atomically managed with key isolation per user and action; fails closed securely in production. Caches invalidate immediately upon membership or role transitions.
- **Events**: Community Events are self-contained with complete RSVP lifecycle and notification fan-out.

