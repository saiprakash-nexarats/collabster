# Content Interaction Ownership Specification

## Overview

This document specifies the authoritative single-source-of-truth ownership model for all content interactions across the Collobster platform as established and hardened in **Phase 8**.

Collobster supports heterogeneous content types unified under the polymorphic identity abstraction:
```java
public record ContentRef(
    ContentType type,
    UUID id
) {
    public static ContentRef platformPost(UUID id) { return new ContentRef(ContentType.PLATFORM_POST, id); }
    public static ContentRef communityPost(UUID id) { return new ContentRef(ContentType.COMMUNITY_POST, id); }
}
```

The primary invariant enforced by this specification is:
```text
ONE USER ACTION = ONE AUTHORITATIVE SOURCE OF TRUTH
```
There must never be competing or duplicate storage rows representing the same user interaction, nor can counters disagree across surfaces (Feed, Explore, Search, Trending, Community-native views).

---

## 1. Authoritative Ownership Matrix

| Action | PLATFORM_POST Owner | COMMUNITY_POST Owner | Authoritative Storage | Rationale & Behavioral Semantics |
|:---|:---|:---|:---|:---|
| **LIKE / UNLIKE** | Global Interaction | Global Interaction via Community Adapter | `content_likes` (PostgreSQL) | Single source of truth. Both `PLATFORM_POST` and `COMMUNITY_POST` store likes in `content_likes` keyed by `(user_id, content_type, content_id)`. Prevents divergent counters. |
| **SAVE / UNSAVE** | Global Interaction | Global Interaction via Community Adapter | `content_saves` (PostgreSQL) | User-private bookmark state. Independent of Community visibility and membership lifecycle. |
| **VIEW** | Global Interaction Event | Global Interaction Event | `content_interaction_events` | Deduplicated sliding window (60s) interaction signal. Feeds Explore trending and recommendation pipeline. |
| **SHARE** | Global Interaction Event | Global Interaction Event | `content_interaction_events` | High-weight virality signal. Stored polymorphically with `ContentRef`. |
| **WATCH_TIME** | Global Interaction Event | Global Interaction Event | `content_interaction_events` | Dwell/consumption duration signal (bounded to 24h) for recommendation ranking. |
| **HIDE** | Global Personalization | Global Personalization | `content_interaction_events` | Passive surface suppression (Feed/Explore). Suppresses the specific `ContentRef` from caller's recommendations. |
| **NOT_INTERESTED** | Global Personalization | Global Personalization | `content_interaction_events` | Topic/entity-level negative recommendation signal. Stored as an event for offline/online ranking suppression. |
| **COMMENT** | Unsupported (future) | Community Domain | `comments` (PostgreSQL) | Community-owned conversational tree. Displayed via authoritative `commentCount` on global Feed/Explore surfaces. |
| **CHANNEL REACTION** | Unsupported | Community Domain | `community_channel_message_reactions` | Chat message reaction domain. Strictly scoped to channel messages, never applied to posts. |

---

## 2. Call-Site Matrix

| Operation | PLATFORM_POST Owner | COMMUNITY_POST Owner | Current Implementation | Desired Implementation (Phase 8 Hardened) |
|:---|:---|:---|:---|:---|
| **LIKE** | Global Interaction | Global Interaction (`CommunityPostInteractionAdapter`) | `InteractionServiceImpl.likeContent` / `PostController.likePost` | Dispatches via `ContentInteractionAdapterRegistry` to `PlatformPostInteractionAdapter` or `CommunityPostInteractionAdapter`. Persists to `content_likes`. |
| **UNLIKE** | Global Interaction | Global Interaction (`CommunityPostInteractionAdapter`) | `InteractionServiceImpl.unlikeContent` / `PostController.unlikePost` | Dispatches via `ContentInteractionAdapterRegistry`. Deletes from `content_likes`. |
| **SAVE** | Global Interaction | Global Interaction (`CommunityPostInteractionAdapter`) | `InteractionServiceImpl.saveContent` / `PostController.savePost` | Dispatches via `ContentInteractionAdapterRegistry`. Persists to `content_saves`. |
| **UNSAVE** | Global Interaction | Global Interaction (`CommunityPostInteractionAdapter`) | `InteractionServiceImpl.unsaveContent` / `PostController.unsavePost` | Dispatches via `ContentInteractionAdapterRegistry`. Deletes from `content_saves`. |
| **VIEW** | Global Interaction | Global Interaction | `InteractionServiceImpl.recordView` | Deduplicated (60s window) into `content_interaction_events(eventType=VIEW)`. |
| **SHARE** | Global Interaction | Global Interaction | `InteractionServiceImpl.recordShare` | Recorded into `content_interaction_events(eventType=SHARE)`. |
| **HIDE** | Global Personalization | Global Personalization | `InteractionServiceImpl.recordHide` | Recorded into `content_interaction_events(eventType=HIDE)`. Suppresses item from Feed sessions. |
| **NOT_INTERESTED** | Global Personalization | Global Personalization | `InteractionServiceImpl.recordNotInterested` | Recorded into `content_interaction_events(eventType=NOT_INTERESTED)`. Negative training/ranking signal. |
| **WATCH_TIME** | Global Interaction | Global Interaction | `InteractionServiceImpl.recordWatchTime` | Recorded into `content_interaction_events(eventType=WATCH_TIME, durationMs)`. |

---

## 3. Community Reaction Model Audit & Resolution

### Real Model Findings
An exhaustive audit of the `com.collabsphere_first.community` package and database schema revealed:
1. **No Competing Post Reaction Table**: The Community domain does **NOT** contain a table named `post_reactions`, `community_post_reactions`, or similar.
2. **Channel Message Reactions Only**: The only reaction entity in the Community domain is `ChannelMessageReaction` mapping to `community_channel_message_reactions`, which is strictly tied to real-time chat messages inside community channels (`ChannelMessage`).
3. **Pre-existing Global Table**: Schema migration `V22__create_interactions_and_social_graph.sql` created `content_likes`, `content_saves`, and `content_interaction_events` supporting polymorphic `(content_type, content_id)`.
4. **Historical Alignment**: Because Community posts were already using `content_likes` and `content_saves`, there are **zero duplicate LIKE tables** and **zero duplicate LIKE rows** in production.

### Authoritative Architecture
The Phase 8 architecture explicitly codifies `content_likes` as the single authoritative source of truth for all post likes across both `PLATFORM_POST` and `COMMUNITY_POST`.

```text
                               Client Request
                                     │
                 ┌───────────────────┴───────────────────┐
                 ▼                                       ▼
    POST /api/v1/interactions/...            PUT /api/v1/posts/{id}/like
        (Global Interaction API)                (Community Native Post API)
                 │                                       │
                 └───────────────────┬───────────────────┘
                                     ▼
                          InteractionService
                                     │
                                     ▼
                   ContentInteractionAdapterRegistry
                     │                             │
    [type == PLATFORM_POST]             [type == COMMUNITY_POST]
                     │                             │
                     ▼                             ▼
       PlatformPostInteractionAdapter   CommunityPostInteractionAdapter
                     │                             │
                     └───────────────┬─────────────┘
                                     ▼
                   Authoritative `content_likes` Table
                       (user_id, content_type, content_id)
```

---

## 4. API Convergence

To guarantee zero regression for existing mobile and web clients, both API route families are preserved and converge onto the exact same transactional domain logic:

1. **Global Interaction API**:
   - `PUT /api/v1/interactions/{contentType}/{contentId}/like`
   - `DELETE /api/v1/interactions/{contentType}/{contentId}/like`
   - `PUT /api/v1/interactions/{contentType}/{contentId}/save`
   - `DELETE /api/v1/interactions/{contentType}/{contentId}/save`
   - `POST /api/v1/interactions/events` (VIEW, SHARE, HIDE, NOT_INTERESTED, WATCH_TIME)
   - `GET /api/v1/interactions/{contentType}/{contentId}/summary`

2. **Community Native Post API** (`PostController`):
   - `PUT /api/v1/posts/{id}/like` -> delegates to `interactionService.likeContent(user, ContentRef.communityPost(id))`
   - `DELETE /api/v1/posts/{id}/like` -> delegates to `interactionService.unlikeContent(user, ContentRef.communityPost(id))`
   - `GET /api/v1/posts/{id}/like` -> returns authoritative `likeCount` and `liked` status
   - `PUT /api/v1/posts/{id}/save` -> delegates to `interactionService.saveContent(user, ContentRef.communityPost(id))`
   - `DELETE /api/v1/posts/{id}/save` -> delegates to `interactionService.unsaveContent(user, ContentRef.communityPost(id))`

**Result**: A user liking a post via `/api/v1/posts/{id}/like` or `/api/v1/interactions/COMMUNITY_POST/{id}/like` hits the exact same underlying record in `content_likes`. Reading the post through `/api/v1/posts/{id}`, `/api/v1/feed`, or `/api/v1/explore` yields identical `liked: true` and identical `likeCount`.

---

## 5. ContentRef Polymorphic Adapter Architecture

Phase 8 introduced the `ContentInteractionAdapter` SPI:

```java
public interface ContentInteractionAdapter {
    ContentType supports();
    LikeResponse like(User user, UUID contentId);
    LikeResponse unlike(User user, UUID contentId);
    SaveResponse save(User user, UUID contentId);
    SaveResponse unsave(User user, UUID contentId);
    InteractionSummary getSummary(User viewer, UUID contentId);
}
```

Registered implementations:
- `PlatformPostInteractionAdapter`: Validates visibility of `PlatformPost`, enforces access, mutates `content_likes` and `content_saves`.
- `CommunityPostInteractionAdapter`: Uses `CommunityContentAccessService` to verify community membership, visibility rules (`ACTIVE` community, `VISIBLE` post), mutates `content_likes` and `content_saves`.

The `ContentInteractionAdapterRegistry` selects the appropriate adapter at runtime in $O(1)$ time based on `ContentRef.type()`.

---

## 6. Shared Community Content Access Facade

To eliminate duplicate, scattered SQL and security checks across Feed, Explore, Search, Notifications, and Interactions, Phase 8 introduced `CommunityContentAccessService`:

```java
public interface CommunityContentAccessService {
    boolean canViewPost(Long userId, UUID postId);
    Optional<Post> getPostIfAccessible(Long userId, UUID postId);
    Map<UUID, Post> getVisiblePosts(Long userId, Collection<UUID> postIds);
    Optional<CommunitySummaryDto> getCommunitySummary(UUID communityId);
    Map<UUID, InteractionSummary> getAuthoritativeReactionSummary(Long userId, Collection<UUID> postIds);
}
```

### Invariants Enforced by Facade:
1. **Lifecycle Rules**: Community must have `status == ACTIVE`. Posts with `status != ACTIVE` or `moderation_status != VISIBLE` are inaccessible to regular members.
2. **Membership Rules**:
   - `PUBLIC` communities: visible to any authenticated user.
   - `RESTRICTED` or `PRIVATE` communities: visible only to active members (`CommunityMember` where `status == ACTIVE`).
3. **Batch Operations**: `getVisiblePosts` and `getAuthoritativeReactionSummary` execute in $O(1)$ bounded queries per batch page, eliminating N+1 query overhead.

---

## 7. Cross-Surface Consistency & Signal Pipelines

### A. Feed Hydration
`FeedContentHydrator` delegates community post candidates to `CommunityPostFeedAdapter`, which queries `CommunityContentAccessService.getVisiblePosts` and populates `InteractionSummary` from the authoritative interaction repositories.

### B. Explore Hydration & Trending
- **Trending Scoring**: Explore trending uses raw stats from `content_likes`, `content_saves`, and `content_interaction_events`.
- **Zero Double Counting**: Because `content_likes` is the single source of truth for both types, trending scoring calculates:
  $$\text{Score}(c) = w_{\text{like}} \cdot \text{count}_{\text{likes}}(c) + w_{\text{save}} \cdot \text{count}_{\text{saves}}(c) + w_{\text{share}} \cdot \text{count}_{\text{shares}}(c) + \dots$$
  No synthetic or duplicative LIKE source is summed.

### C. Recommendation Feature Vectors
`RecommendationFeatureAssembler` retrieves authoritative engagement counters for `COMMUNITY_POST` candidates directly via `ContentLikeRepository` and `ContentSaveRepository`. Features adhere strictly to `RECOMMENDATION_FEATURE_CONTRACT_V1`.

---

## 8. Cross-Surface ContentRef Collision Safety

Because IDs are UUIDs, a theoretical collision where a `PlatformPost` and a `CommunityPost` share the exact same UUID $X$ is completely isolated by composite keys:
- Likes table: `PRIMARY KEY (user_id, content_type, content_id)`
- Saves table: `PRIMARY KEY (user_id, content_type, content_id)`
- Events table: `(content_type, content_id)`

User liking `PLATFORM_POST X` creates row `(U, PLATFORM_POST, X)`.
User reacting/liking `COMMUNITY_POST X` creates row `(U, COMMUNITY_POST, X)`.
Operations on one do not mutate, overwrite, or increment the other.

---

## 9. Block and Mute Semantics

1. **Passive Surface Suppression**:
   - If User A blocks User B or User B blocks User A, B's authored community posts are filtered out of A's global Feed and Explore surfaces (`FeedContentHydrator`, `ExploreService`).
2. **Community Membership Isolation**:
   - Social blocks and mutes do **NOT** modify or delete `CommunityMember` entities. Community roles (Owner, Moderator, Member) are completely decoupled from individual user social blocks.
3. **Direct Navigation**:
   - When User A directly visits a community where User B is an author, standard Community domain moderation rules apply.
4. **Mute Semantics**:
   - Muted users/authors are suppressed from passive recommendation candidate generation without altering Community permissions.

---

## 10. Schema Migration Decision

- **Current Schema Version**: `V26`
- **Audit Outcome**: No competing post reaction tables or conflicting schema structures exist. All post interactions already reside in `content_likes`, `content_saves`, and `content_interaction_events`.
- **Migration Decision**: **NO V27 MIGRATION REQUIRED**. The schema remains at `V26`.
