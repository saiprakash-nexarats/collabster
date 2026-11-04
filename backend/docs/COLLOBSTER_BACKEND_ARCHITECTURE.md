# Collobster Backend Architecture Specification

> **Document Version:** 1.1.0  
> **Status:** Architecture Contract & Phase 1 Foundation  
> **Date:** September 2026  
> **Author:** Principal Backend Architect  

---

## Table of Contents

1. [Current Architecture](#1-current-architecture)
2. [Current Package / Module Map](#2-current-package--module-map)
3. [Current Database Ownership](#3-current-database-ownership)
4. [Current Community Architecture](#4-current-community-architecture)
5. [Current Recommendation Architecture](#5-current-recommendation-architecture)
6. [Confirmed Technical Debt & Architectural Gaps](#6-confirmed-technical-debt--architectural-gaps)
7. [Target Architecture](#7-target-architecture)
8. [Module Boundaries & Domain Responsibilities](#8-module-boundaries--domain-responsibilities)
9. [Dependency Direction](#9-dependency-direction)
10. [Data Ownership Rules](#10-data-ownership-rules)
11. [Implemented ContentRef Abstraction (Phase 1)](#11-implemented-contentref-abstraction-phase-1)
12. [Feed Responsibilities](#12-feed-responsibilities)
13. [Explore Responsibilities](#13-explore-responsibilities)
14. [Messaging Responsibilities](#14-messaging-responsibilities)
15. [Community Responsibilities](#15-community-responsibilities)
16. [Java / Python Boundary & Contract](#16-java--python-boundary--contract)
17. [PostgreSQL & Redis Responsibilities](#17-postgresql--redis-responsibilities)
18. [API Ownership Principles](#18-api-ownership-principles)
19. [Security Principles](#19-security-principles)
20. [Database Migration Strategy](#20-database-migration-strategy)
21. [Phased Implementation Roadmap](#21-phased-implementation-roadmap)
22. [Platform Post Domain Architecture & Implementation (Phase 1)](#22-platform-post-domain-architecture--implementation-phase-1)
23. [Direct Messaging Domain Architecture (Phase 6)](#23-direct-messaging-domain-architecture-phase-6)
24. [Direct Messaging Phase 6.1 Hardening Invariants](#24-direct-messaging-phase-61-hardening-invariants)
25. [Global Notification Center Architecture (Phase 7)](#25-global-notification-center-architecture-phase-7)
26. [Notification Durability, Deduplication Identity & Compatibility Hardening (Phase 7.1)](#26-notification-durability-deduplication-identity--compatibility-hardening-phase-71)


---

## 1. Current Architecture

The Collobster backend ecosystem currently consists of two distinct subsystems:

```
+-------------------------------------------------------------------------+
|                           Mobile Application                            |
+------------------------------------+------------------------------------+
                                     |
                           HTTPS / WSS (Port 8080)
                                     v
+-------------------------------------------------------------------------+
|             Spring Boot Monolith (collabster-communities-module)        |
|  - Framework: Spring Boot 3.5.4 (Java 21)                               |
|  - Packaging: JAR (com.collabsphere_first)                              |
|  - Role: Authoritative Backend, Client Gateway, Security Guardian      |
|  - Persistence: PostgreSQL 16 (Durable Source of Truth)                 |
|  - Cache & Rate Limiting: Redis 7 + Bucket4j In-Memory Fallback         |
|  - Messaging: Spring WebSocket + STOMP (/ws/community)                  |
+-------------------------------------------------------------------------+
                                     :
                    (Gap: Integration Not Yet Wired)
                                     :
+-------------------------------------------------------------------------+
|             Python Recommendation Engine (Recommendation-System)        |
|  - Framework: FastAPI + Uvicorn (Python 3.11)                           |
|  - Role: Candidate Generation, Feature Extraction, LightGBM / Two-Tower |
|  - Persistence (Current): Direct connection to 'collabsterdb' (Mismatch)|
|  - Cache: Redis 7 (feed session storage)                                |
+-------------------------------------------------------------------------+
```

### Key Architectural Baseline Findings
1. **Spring Boot is the authoritative backend and client gateway**: Mobile applications must never access internal ML microservices directly. All authentication, authorization, business rules, transactional guarantees, and data hydration occur within Spring Boot.
2. **Current package namespace**: `com.collabsphere_first` is preserved. No cosmetic renaming is performed during this phase.
3. **Existing Community Module is a complete production subsystem**: Includes 23 domain models, transactional outbox pattern, audit logs, fine-grained RBAC, channels, reactions, attachments, polls, events, and rules.
4. **Recommendation Engine is decoupled from Spring**: Implemented in Python FastAPI with ML pipelines (LightGBM, ANN, collaborative filtering), but currently assumes a separate database schema with integer IDs.

---

## 2. Current Package / Module Map

The Spring Boot application code is organized under `com.collabsphere_first`:

```
com.collabsphere_first/
├── Application.java                      # Spring Boot main class
├── auth/                                 # Authentication & Registration
│   ├── controller/AuthenticationController.java
│   ├── dto/request/                      # Login, Register, OTP, Password Reset DTOs
│   ├── dto/response/                     # Auth tokens, status responses
│   ├── exception/                        # AuthException & AuthExceptionHandler
│   └── service/                          # AuthenticationService, AuthenticationServiceImpl
├── common/                               # Cross-cutting utilities & exceptions
├── community/                            # Complete Community Domain Subsystem
│   ├── config/                           # WebSocket, Cache, RateLimiting configs
│   ├── controller/                       # 15 REST Controllers (Posts, Channels, etc.)
│   ├── dto/                              # Request / Response payloads
│   ├── entity/                           # 23 JPA Entities (Community, Post, etc.)
│   ├── enums/                            # Roles, Statuses, Types
│   ├── exception/                        # GlobalCommunitiesExceptionHandler
│   ├── mapper/                           # Entity-to-DTO mappers
│   ├── repository/                       # Spring Data JPA Repositories
│   ├── security/                         # CommunityPermissionEvaluator, AccessService
│   ├── service/                          # 25+ Domain Services (Lifecycle, Outbox, etc.)
│   └── util/                             # Slug generation, Token utils
├── integration/                          # External adapters (File Storage / Firebase)
├── mail/                                 # Email delivery & local fallback service
├── otp/                                  # 6-digit numeric OTP generation & verification
│   ├── entity/Otp.java
│   ├── repository/OtpRepository.java
│   └── service/OtpService.java
├── profile/                              # User Profile management
│   ├── controller/ProfileController.java
│   ├── dto/                              # ProfileRequest, ProfileResponse
│   └── service/ProfileService.java
├── refreshtoken/                         # Refresh token lifecycle & rotation
│   ├── entity/RefreshToken.java
│   └── repository/RefreshTokenRepository.java
├── role/                                 # Platform user roles
│   ├── entity/Role.java
│   └── entity/RoleName.java              # ROLE_USER, ROLE_ADMIN
├── security/                             # Core Security Infrastructure
│   ├── config/SecurityConfig.java        # SecurityFilterChain, MethodSecurity
│   ├── jwt/                              # JwtService, JwtCookieService, JwtAuthFilter
│   ├── oauth2/                           # Social login, success/failure handlers
│   └── userdetails/                      # CustomUserDetailsService, UserPrincipal
├── user/                                 # Authoritative User Entity
│   ├── entity/User.java
│   ├── entity/AuthProvider.java
│   ├── entity/Gender.java
│   └── repository/UserRepository.java
└── workspace/                            # User Workspace management
    ├── controller/WorkspaceController.java
    ├── entity/UserWorkspace.java
    └── service/WorkspaceService.java
```

---

## 3. Current Database Ownership

PostgreSQL is the durable source of truth for the entire platform. Database schema migrations are strictly managed through Flyway.

### Flyway Migration Inventory (V1 – V18)
- **V1__create_communities_schema.sql**: Base tables (`communities`, `memberships`, `posts`, `comments`, `resources`, `reports`)
- **V2__create_users_table.sql**: Base `users` table
- **V3__add_community_moderation_toggle.sql**: Adds `requires_moderation` column
- **V4__community_phase_one.sql**: `community_invitations`, unique owner index
- **V5__community_audit_logs.sql**: `community_audit_logs` table
- **V6__community_channels_messages.sql**: `community_channels`, `community_channel_messages`
- **V7__report_target_ids_as_text.sql**: Migrates `reports.target_id` to `VARCHAR(100)`
- **V8__community_polls.sql**: `community_polls`, `community_poll_options`, `community_poll_votes`
- **V9__community_status_lifecycle.sql**: Adds `community_status`, `deletion_requested_at`
- **V10__community_rules.sql**: `community_rules`, `community_rule_revisions`
- **V11__community_settings.sql**: `community_settings`, `logo_url`, `cover_url`
- **V12__community_notifications.sql**: `community_notifications` table
- **V13__community_outbox.sql**: `community_outbox` transactional outbox table
- **V14__community_events.sql**: `community_events`, `community_event_rsvps`
- **V15__community_channel_reactions_and_read_states.sql**: Message reactions & read states
- **V16__community_outbox_delivery.sql**: `community_outbox_delivery` consumer tracking
- **V17__community_categories_and_message_attachments.sql**: `category` on communities, attachments table
- **V18__analytics_performance_indexes.sql**: Analytics aggregation indexes

---

## 4. Current Community Architecture

The Community module is an existing production-ready domain with rich subdomains:

```
+-----------------------------------------------------------------------------------+
|                                Community Domain                                   |
+-----------------------------------------------------------------------------------+
|  Aggregate Root: Community (UUID id, slug, owner_id, type, visibility, category)  |
|                                                                                   |
|  [Governance & Rules]       [Content & Discussion]        [Real-time Channels]    |
|  - CommunityRule            - Post (community_id NOT NULL) - CommunityChannel      |
|  - CommunityRuleRevision    - Comment                     - ChannelMessage        |
|  - CommunitySettings        - Resource                    - MessageAttachment     |
|  - CommunityAuditLog        - Report                      - MessageReaction       |
|                                                           - ChannelReadState      |
|  [Social & Engagement]      [Reliable Delivery]           [Security & Auth]       |
|  - Membership (Roles/Rank)  - CommunityOutboxEvent        - PermissionEvaluator   |
|  - CommunityInvitation      - CommunityOutboxDelivery     - StompAuthInterceptor  |
|  - CommunityPoll / Vote     - Idempotency consumer        - AccessService         |
|  - CommunityEvent / RSVP    - Polling Publisher                                   |
+-----------------------------------------------------------------------------------+
```

### Critical Invariants of the Community Module:
1. **Community Post Ownership**: `Post` entity requires a `community_id` foreign key (`nullable = false`). All queries, moderation checks, and indexes rely on this relationship.
2. **Access Control**: Evaluated via `CommunityPermissionEvaluator`, calculating role hierarchy: `OWNER (4) > ADMIN (3) > MODERATOR (2) > MEMBER (1)`.
3. **STOMP Destination Isolation**: Real-time channel messages are published to `/topic/communities/{communityId}/channels/{channelId}/messages` and guarded by `CommunityStompAuthorizationInterceptor`.
4. **Transactional Outbox**: Mutation events are inserted into `community_outbox` within the same DB transaction and published asynchronously to decouple event subscribers.

---

## 5. Current Recommendation Architecture

The recommendation engine is an isolated Python/FastAPI service with a multi-stage ML pipeline:

```
+-----------------------------------------------------------------------------------+
|                           Python Recommendation Pipeline                          |
+-----------------------------------------------------------------------------------+
| Stage 1: Candidate Generation                                                     |
|   - PreferenceRetriever (tag/interest matching)                                   |
|   - TrendingRetriever (time-decayed engagement velocity)                          |
|   - RecentRetriever (pure temporal freshness)                                     |
|   - CollaborativeFilteringRetriever (user-item matrix factorization)              |
|   - TwoTowerANNRetriever (vector embeddings similarity)                           |
|                                                                                   |
| Stage 2: Merging & Candidate Filtering                                            |
|   - Deduplication across generators                                               |
|   - VisibilityFilter (profile visibility, block relationships)                    |
|                                                                                   |
| Stage 3: Feature Extraction                                                       |
|   - Extracts 10 signals: freshness, quality, popularity, preference, interest,    |
|     relationship, community, user_interaction_count, post_interaction_count,       |
|     creator_reputation                                                            |
|                                                                                   |
| Stage 4: Scoring & Ranking Engine                                                 |
|   - WeightedSumStrategy (baseline heuristic weighting)                            |
|   - LightGBMRankerStrategy (trained LambdaMART ranker)                            |
|                                                                                   |
| Stage 5: Re-ranking & Diversity                                                   |
|   - Author fatigue penalty                                                        |
|   - Topic / community diversity enforcement                                       |
|                                                                                   |
| Stage 6: Session Management                                                       |
|   - RedisFeedSessionRepository (persists session candidates under                 |
|     'feed_session:{user_id}:{session_id}')                                        |
+-----------------------------------------------------------------------------------+
```

### Exposed Endpoints in Python FastAPI:
- `GET /api/v1/recommend/feed`: Full diagnostics feed response (`FeedResponse`).
- `GET /api/v1/recommend/feed/backend`: Lean backend response (`RecommendationBackendResponse` with `post_id`, `rank`, `score`).
- `POST /api/v1/recommend/interaction`: Ingests user interactions (`UserPostInteraction`).
- `POST /api/v1/recommend/people`: Recommends social connections (`PeopleResponse`).

---

## 6. Confirmed Technical Debt & Architectural Gaps

An exhaustive codebase audit revealed the following concrete technical gaps:

| ID | Category | Discovered Issue | Impact |
|:---|:---|:---|:---|
| **GAP-01** | Database Migration | Tables `roles`, `user_roles`, `refresh_tokens`, `otp`, `user_workspaces` exist as JPA entities but are completely missing from Flyway V1–V18. | Spring Boot startup with `hibernate.ddl-auto=validate` fails on a fresh database. |
| **GAP-02** | Database Migration | Columns `linkedin_url`, `portfolio_url`, `years_of_experience` on `users` entity are missing from Flyway V2. | Hibernate validation error on `users` table. |
| **GAP-03** | Database Migration | Columns `correlation_id` and `causation_id` on `community_outbox` entity are missing from Flyway V13. | Hibernate validation error on `community_outbox` table. |
| **GAP-04** | Identifier Mismatch | Spring uses `UUID` for Post IDs and `Long` for User IDs. Python assumes `Integer` for both `post_id` and `user_id`. | Spring cannot pass UUID posts or Long users directly to FastAPI without type conversion or schema alignment. |
| **GAP-05** | Schema Mismatch | Python's `posts` model declares `likes`, `comments`, `shares`, `saves`, `watch_time`, `tags` (JSON), `communities` (JSON), `spam_probability`, `creator_reputation`. None exist on Spring's `posts` table. | Pointing Python to Spring's database results in SQL crashes. |
| **GAP-06** | Direct DB Coupling | Python directly queries PostgreSQL (`collabsterdb`), bypassing Spring Boot. Spring's datasource defaults to `collabster1`. | Violates single source of truth; creates shadow data and dual-write divergence. |
| **GAP-07** | Post Monolith | `Post` entity is strictly tied to `community_id NOT NULL`. Global user posts (`PlatformPost`) have no representation. | Attempting to use `Post` for platform feed would break community invariants. |
| **GAP-08** | Build Environment | `pom.xml` targets Java 21 (`<java.version>21</java.version>`), but local machine environment has JDK 20 installed. | `mvnw compile` fails with `Fatal error compiling: error: release version 21 not supported`. |
| **GAP-09** | Python Environment | `lightgbm` package is missing in system Python, causing 2 test files to fail collection. | Full test suite cannot execute without installing project dependencies. |
| **GAP-10** | Infrastructure | Local Redis is not running on port 6379. | Spring rate limiting falls back to in-memory Bucket4j; Python session caching fails unless mocked. |

---

## 7. Target Architecture

The target architecture establishes a clean, decoupled modular monolith in Spring Boot with an externalized Python ML recommendation service:

```
+===================================================================================+
|                                Mobile Clients                                     |
|         (Feed, Explore, +, Messages, Community, Profile / Side Navigation)        |
+=========================================+=========================================+
                                          |
                              HTTPS / WSS (Port 8080)
                                          v
+===================================================================================+
|                          SPRING BOOT BACKEND GATEWAY                              |
+===================================================================================+
| Core Identity & Security Infrastructure:                                          |
|   - JWT Token Authentication Filter                                               |
|   - HttpOnly Refresh Token Cookie Service                                         |
|   - OAuth2 Social Provisioning                                                    |
|   - Method Security & Permission Evaluator                                        |
+-----------------------------------------------------------------------------------+
| Authoritative Core Domains:                                                       |
|   [ auth ]      [ user ]       [ role ]       [ profile ]     [ workspace ]       |
+-----------------------------------------------------------------------------------+
| Platform Content & Social Domains:                                                |
|   [ post ]                 [ network ]               [ interaction ]              |
|   - PlatformPost (UUID)    - Follow / Connection     - Likes, Saves, Shares       |
|   - Media & Visibility     - Blocks & Mutes          - Watch time signals         |
|   - Drafts / Scheduling                                                           |
+-----------------------------------------------------------------------------------+
| Community Domain (Preserved & Encapsulated):                                      |
|   [ community ]                                                                   |
|   - CommunityPost (UUID, community_id NOT NULL)                                   |
|   - Channels, Messages, Reactions, Polls, Events, Rules, Outbox                   |
+-----------------------------------------------------------------------------------+
| Platform Aggregation & Discovery Domains:                                         |
|   [ feed ]                                  [ explore ]                           |
|   - Home Feed Orchestrator                  - Multi-faceted Discovery Engine      |
|   - ContentRef Aggregator                   - People / Topic / Community Search   |
|   - Session Continuity & Hydration          - Trending Content Aggregator         |
+-----------------------------------------------------------------------------------+
| Communication & Notification Domains:                                             |
|   [ messaging ]                             [ notification ]                      |
|   - User <-> User Conversations (1:1)       - Platform Notification Engine        |
|   - Dedicated WebSocket Stomp Broker        - Device Push Notification Dispatch   |
+-----------------------------------------------------------------------------------+
| ML Integration Layer:                                                             |
|   [ recommendation ]                                                              |
|   - RecommendationServiceClient (Spring WebClient / HTTP)                         |
|   - Candidate Pre-Filter & Post-Hydration Adapter                                 |
+=========================================+=========================================+
                                          |
                              Internal REST / gRPC
                                          v
+===================================================================================+
|                    INTERNAL PYTHON RECOMMENDATION SERVICE                         |
|   - Multi-stage Candidate Retrieval                                               |
|   - ML Feature Store & Online Scoring                                             |
|   - LightGBM / Two-Tower Ranking Models                                           |
|   - Stateless candidate ranking (candidate IDs + scores in, ranked IDs out)       |
+===================================================================================+
```

---

## 8. Module Boundaries & Domain Responsibilities

To avoid God-services and maintain clean encapsulation, every domain has unambiguous boundaries:

### 1. `post/` (Platform Post Domain)
- **Owns**: Platform user posts (`PlatformPost`), post attachments, user-level visibility settings (`PUBLIC`, `CONNECTIONS_ONLY`, `PRIVATE`).
- **Does NOT own**: Community posts, home feed timeline assembly, search indexation.

### 2. `community/` (Community Domain)
- **Owns**: Communities, memberships, roles, permissions, `CommunityPost`, comments, community channels, channel messages, polls, events, rules, moderation, community outbox.
- **Does NOT own**: Platform-wide user posts, 1:1 user messages, global personalized home feed.

### 3. `network/` (Social Graph Domain)
- **Owns**: User-to-user relationships (`FOLLOW`, `CONNECTION_REQUEST`, `CONNECTION_ACCEPTED`, `BLOCK`, `MUTE`).
- **Does NOT own**: User profiles, post content, community memberships.

### 4. `interaction/` (Engagement & Signal Domain)
- **Owns**: Content engagement records (likes, saves, shares, watch duration, hide/not-interested signals) referenced by `ContentRef`.
- **Does NOT own**: Post authoring, feed ranking algorithms.

### 5. `feed/` (Feed Aggregation Domain)
- **Owns**: Home feed orchestration, feed session tracking, client pagination cursor management, content hydration across `PlatformPost` and `CommunityPost`, negative feedback filtering.
- **Does NOT own**: Primary post database tables (Feed is an aggregator, not an authoring store).

### 6. `explore/` (Discovery Domain)
- **Owns**: Discovery surfaces across people, topics, communities, and trending content.
- **Does NOT own**: Chronological home feed, user-specific private messaging.

### 7. `messaging/` (Direct Messaging Domain)
- **Owns**: User-to-user 1:1 conversations, direct messages, read receipts, typing indicators.
- **Does NOT own**: Community channel messaging (which stays in `community/`).

### 8. `notification/` (Platform Notification Domain)
- **Owns**: Cross-domain user notification inbox, push delivery triggers, device token management.
- **Does NOT own**: Community outbox event persistence.

### 9. `recommendation/` (Spring ML Client Domain)
- **Owns**: Integration adapter to the Python recommendation service, DTO translation, circuit breaking, fallback candidate ranking.
- **Does NOT own**: ML model training or LightGBM inference.

---

## 9. Dependency Direction

Dependencies must flow strictly downward and inward. Circular dependencies are strictly forbidden:

```
[auth] [user] [profile] [workspace] (Core Foundations)
                  ↓
  +---------------+---------------+
  |               |               |
[post]       [community]      [network]      [interaction]
  |               |               |                |
  +-------+-------+---------------+----------------+
          |                       |
          v                       v
       [feed]                 [explore]
          |                       |
          +-----------+-----------+
                      |
                      v
             [recommendation] (Spring Client)
                      |
                      v
          [Python ML Microservice]

* [messaging] depends only on [user], [security], and common infrastructure.
* [notification] depends on [user] and event listeners.
```

---

## 10. Data Ownership Rules

1. **PostgreSQL is the durable source of truth**:
   - All state mutations (post creation, profile edits, social connections, community memberships, direct messages, audit logs) must commit to PostgreSQL.
   - No data may exist solely in Redis without a backing durable store or reproducible derivation.

2. **Redis is derived and temporary state only**:
   - Permitted Redis usages:
     - Ephemeral cache (user permissions, public profile summaries).
     - Distributed rate-limiting counters (with in-memory fallback).
     - Active WebSocket presence & typing indicators.
     - Recommendation session buffers (`feed_session:{userId}:{sessionId}`).
     - Unread message counters (recalculated from DB if lost).
   - Invariant: **Flushing Redis completely (`FLUSHALL`) must cause zero data loss and zero permanent corruption.**

---

## 11. Implemented ContentRef Abstraction (Phase 1)

To support both `PlatformPost` and `CommunityPost` in future Feed, Explore, and Interaction modules without polluting entity relationships or breaking Community encapsulation, the canonical `ContentRef` abstraction is established in `com.collabsphere_first.common.content`:

```java
package com.collabsphere_first.common.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record ContentRef(
    @JsonProperty("type") ContentType type,
    @JsonProperty("id") UUID id
) implements Serializable {

    public ContentRef {
        Objects.requireNonNull(type, "ContentType must not be null");
        Objects.requireNonNull(id, "Content ID must not be null");
    }

    public static ContentRef platformPost(UUID id) {
        return new ContentRef(ContentType.PLATFORM_POST, id);
    }

    public static ContentRef communityPost(UUID id) {
        return new ContentRef(ContentType.COMMUNITY_POST, id);
    }
}
```

With its associated enum:

```java
package com.collabsphere_first.common.content;

public enum ContentType {
    PLATFORM_POST,
    COMMUNITY_POST
}
```

### Feed Item Presentation Model (Phase 5 Target):
```java
public record FeedItemDto(
    ContentRef contentRef,
    AuthorSummaryDto author,
    String title,
    String snippet,
    List<String> mediaUrls,
    CommunitySummaryDto community,   // null if PLATFORM_POST
    EngagementMetricsDto engagement, // likes, comments, shares, saves
    ViewerInteractionDto viewerState,// hasLiked, hasSaved
    Instant publishedAt,
    double relevanceScore
) {}
```

### Architectural Benefits:
- **Zero Schema Coupling**: `CommunityPost` preserves its strict `community_id NOT NULL` constraint without hacky nullable foreign keys.
- **Independent Aggregates**: `PlatformPost` (`platform_posts`) and `CommunityPost` (`posts`) evolve independently as JPA aggregates.
- **Polymorphic Hydration**: Downstream modules (`FeedService`, `InteractionService`) store or process `ContentRef` references and hydrate through domain-specific read repositories.


---

## 12. Feed Responsibilities

The Feed domain answers: **"What should this authenticated user consume right now?"**

1. **Candidate Gathering**:
   - Query Network domain for recent content from connections.
   - Query Community domain for eligible public or joined community posts.
   - Request personalized candidate rankings from the Recommendation integration client.
2. **Filtering & Deduplication**:
   - Exclude author-blocked content.
   - Exclude hidden / "not interested" content.
   - Apply user session deduplication.
3. **Hydration & Enrichment**:
   - Batch-fetch posts by ID.
   - Batch-fetch author cards and community badges.
   - Attach viewer interaction flags (`isLiked`, `isSaved`).
4. **Pagination**:
   - Deterministic cursor-based pagination (e.g. `cursor = timestamp:score:id`).
   - Session continuity preserving rank across subsequent page requests.

---

## 13. Explore Responsibilities

The Explore domain answers: **"What new creators, communities, topics, and ideas should this user discover?"**

1. **Discovery Tabs**:
   - **Trending Content**: Velocity-based ranking of high-engagement platform and community posts.
   - **People to Connect With**: Social recommendation (mutual connections, shared skills/interests).
   - **Communities**: Recommended communities by category and activity score.
   - **Topics / Tags**: Curated and trending hashtags/skills.
2. **Search Integration**: Full-text indexing and query routing across people, posts, and communities.
3. **Exploration Diversity**: High novelty factor, intentionally breaking filter bubbles.

---

## 14. Messaging Responsibilities

Platform Messages represents **User-to-User private communication**:

1. **Separation from Community Channels**:
   - Community Channels = Public/restricted discussion topics owned by a Community.
   - Direct Messages = Private conversations owned by participating Users.
2. **Capabilities**:
   - 1:1 direct conversations.
   - Text messages, rich attachments, message delivery receipts, read states.
3. **Infrastructure**:
   - Dedicated WebSocket STOMP endpoint or topic prefix (e.g. `/user/queue/messages`).
   - Strict conversation participation authorization.

---

## 15. Community Responsibilities

The Community domain remains the sovereign boundary for collective collaboration:

1. **Preserve Existing Capabilities**:
   - All 23 entities remain untouched.
   - Moderation workflows, AI moderation fallbacks, reports, audit logs.
   - Channel hierarchy, real-time channel messages, attachments, reactions.
   - Event scheduling, RSVPs, Polls, multi-choice voting.
2. **Participation in Wider Platform**:
   - Emits domain events via `community_outbox` when public posts or events are published.
   - Exposes clean read interfaces (`CommunityContentProvider`) for the Feed and Explore domains to query public content.

---

## 16. Java / Python Boundary & Contract

### Invariant:
**Mobile clients communicate exclusively with Spring Boot. Spring Boot orchestrates the Python Recommendation Service internally.**

```
Mobile App ----(HTTPS)----> Spring Boot ----(Internal HTTP)----> Python FastAPI
```

### Proposed Recommendation Service Contract:
- **Endpoint**: `POST /api/v1/internal/rank-candidates`
- **Request Payload**:
  ```json
  {
    "userId": 42,
    "sessionId": "a8f3b4c1-...",
    "page": 1,
    "limit": 20,
    "userInterests": ["technology", "cloud"],
    "candidateItems": [
      {
        "contentId": "b1a2c3d4-...",
        "contentType": "PLATFORM_POST",
        "authorId": 105,
        "tags": ["java", "spring"],
        "createdAt": 1727438400,
        "likeCount": 15,
        "commentCount": 3
      }
    ]
  }
  ```
- **Response Payload**:
  ```json
  {
    "userId": 42,
    "sessionId": "a8f3b4c1-...",
    "rankedItems": [
      {
        "contentId": "b1a2c3d4-...",
        "rank": 1,
        "score": 0.892,
        "features": {
          "freshness": 0.95,
          "preference": 0.82
        }
      }
    ]
  }
  ```

### Key Architectural Resolutions:
1. **Stateless Candidate Ranking**: Python does not query production tables directly. Spring supplies candidate metadata or Python queries a read-only analytics replica / data lake.
2. **ID Mapping**: Spring passes string representations of UUIDs or structured candidate payloads, eliminating the integer ID limitation.
3. **Fault Tolerance**: If the Python service times out (e.g. > 200ms) or is unreachable, Spring Boot immediately falls back to a deterministic heuristic ranking (chronological + connection affinity) without impacting the mobile user.

---

## 17. PostgreSQL & Redis Responsibilities

| Subsystem | PostgreSQL (Durable Source of Truth) | Redis (Ephemeral / Derived State) |
|:---|:---|:---|
| **Users & Auth** | `users`, `roles`, `user_roles`, `otp`, `refresh_tokens`, `user_workspaces` | Invalidation blacklists, short-lived login attempts |
| **Platform Posts** | `platform_posts`, `post_attachments` | Post engagement hot counters |
| **Communities** | `communities`, `memberships`, `community_posts`, `comments`, `resources`, `reports`, `community_channels`, `messages`, `polls`, `events`, `rules`, `outbox` | Membership permission cache, rate-limiting tokens |
| **Social Graph** | `user_relationships` (follows, friends, blocks) | Graph mutual-connection cache |
| **Feed & Explore** | None (aggregated on-the-fly) | `feed_session:{userId}:{sessionId}` cache, negative feedback set |
| **Messaging** | `conversations`, `direct_messages`, `message_attachments` | Real-time presence, typing indicators |
| **Interactions** | `content_interactions` (likes, saves, watch duration) | Derived recommendation feature vectors |

---

## 18. API Ownership Principles

All REST endpoints follow a standardized, domain-driven resource hierarchy under `/api/v1`:

```
/api/v1/auth/**               -> Auth Domain
/api/v1/users/**              -> User Domain
/api/v1/profile/**            -> Profile Domain
/api/v1/workspaces/**         -> Workspace Domain
/api/v1/posts/**              -> Platform Post Domain
/api/v1/communities/**        -> Community Domain (including canonical /api/v1/communities/notifications/**)
/api/v1/network/**            -> Social Graph Domain
/api/v1/feed/**               -> Feed Domain
/api/v1/explore/**            -> Explore Domain
/api/v1/messages/**           -> Direct Messaging Domain
/api/v1/notification-center/** -> Global Notification Center Domain
/api/v1/notifications/**      -> Legacy Community Notification Aliases (Deprecated but supported)
```

---

## 19. Security Principles

1. **Authenticated Context Resolution**:
   - Every API for Posts, Feeds, Explore, and Messages **must derive the active user from `SecurityContextHolder`** (via `UserPrincipal`).
   - Client-provided `userId` parameters in headers or query strings must never be trusted for identity.
2. **Community Security**:
   - Enforced by `@PreAuthorize("hasPermission(#communityId, 'COMMUNITY', 'ROLE')")`.
3. **WebSocket Security**:
   - JWT validation during the STOMP `CONNECT` handshake.
   - Strict destination validation during `SUBSCRIBE` and `SEND`.
4. **Token Security**:
   - Short-lived Access Tokens (Bearer header).
   - Long-lived Refresh Tokens stored in secure `HttpOnly`, `SameSite=Lax` cookies.

---

## 20. Database Migration Strategy

1. **V1 – V18 are Immutable**: Existing applied migrations must not be modified, deleted, or re-sequenced.
2. **Flyway V19 Migration Implemented (`V19__reconcile_existing_core_schema.sql`)**:
   - Reconciles tables missing from the auth baseline: `roles`, `user_roles`, `refresh_tokens`, `otp`, `user_workspaces`.
   - Adds missing columns to `users`: `linkedin_url`, `portfolio_url`, `years_of_experience`.
   - Adds missing columns to `community_outbox`: `correlation_id`, `causation_id`.
   - Uses idempotent DDL (`CREATE TABLE IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`) to guarantee safety across varied database states.
3. **Flyway V20 Migration Implemented (`V20__create_platform_posts.sql`)**:
   - Creates `platform_posts` table with keyset cursor indexes (`idx_platform_posts_cursor`, `idx_platform_posts_author_cursor`).
   - Creates `platform_post_media` table with foreign key cascading and ordered display indexes.
4. **Future Domain Migrations**:
   - `V21__create_network_relationships_schema.sql` (Follows / Connections)
   - `V22__create_platform_interactions_schema.sql` (Likes / Saves / Signals)
   - `V23__create_direct_messaging_schema.sql` (1:1 Conversations)

---

## 21. Phased Implementation Roadmap

```
+-------------------------------------------------------------------------+
| PHASE 0: Baseline & Architecture Contract (COMPLETED)                   |
| - Repository audit, test baselines, security model, gap documentation   |
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
| PHASE 1: Canonical DB Foundation + Platform Post + ContentRef (COMPLETED|
| - Flyway V19 (schema reconciliation) & V20 (platform posts)             |
| - Implemented ContentRef record & ContentType enum                      |
| - Implemented PlatformPost aggregate, repositories, service & REST APIs |
| - Keyset cursor pagination (GET /api/v1/posts/me) & IDOR security       |
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
| PHASE 2: User Social Graph & Interaction Domains                        |
| - Implement network domain (follow, connect, block)                     |
| - Implement interaction domain (like, save, watch time signals)         |
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
| PHASE 3: Recommendation Client & Python Contract Alignment              |
| - Implement Spring RecommendationServiceClient with circuit-breaking    |
| - Adapt Python recommendation service to accept candidate IDs / UUIDs   |
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
| PHASE 4: Feed & Explore Aggregation Domains                             |
| - Implement personalized Feed with cursor pagination & fallbacks        |
| - Implement Explore search and discovery surfaces                       |
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
| PHASE 5: Direct Messaging Domain                                        |
| - Implement user-to-user private conversations and WebSocket broker     |
+-------------------------------------------------------------------------+
```

---

## 22. Platform Post Domain Architecture & Implementation (Phase 1)

### 1. Domain Overview
The `post` package (`com.collabsphere_first.post`) provides the first-class platform authoring domain, separate from `community` posts. It manages creator broadcasts, text articles, and multimedia attachments intended for platform-wide distribution, follower feeds, and user profiles.

### 2. Database Schema (`V20__create_platform_posts.sql`)
```sql
CREATE TABLE IF NOT EXISTS platform_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id BIGINT NOT NULL,
    body TEXT,
    visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_platform_posts_author FOREIGN KEY (author_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_platform_posts_visibility CHECK (visibility IN ('PUBLIC', 'FOLLOWERS_ONLY', 'PRIVATE')),
    CONSTRAINT chk_platform_posts_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

CREATE TABLE IF NOT EXISTS platform_post_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    media_type VARCHAR(30) NOT NULL,
    storage_key VARCHAR(1000) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    width INT,
    height INT,
    duration_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_platform_post_media_post FOREIGN KEY (post_id)
        REFERENCES platform_posts(id) ON DELETE CASCADE,
    CONSTRAINT chk_post_media_type CHECK (media_type IN ('IMAGE', 'VIDEO', 'DOCUMENT')),
    CONSTRAINT uq_platform_post_media_order UNIQUE (post_id, display_order)
);
```

### 3. Keyset Pagination & Cursor Architecture
- **Pagination Strategy**: Deterministic composite cursor based on `(created_at DESC, id DESC)`.
- **Cursor Format**: Base64 URL-safe (unpadded) token encoding `"{createdAtEpochMilli}:{uuid}"`.
- **Query Method**: `findAuthorPostsAfterCursor` executes:
  ```sql
  WHERE author_id = :authorId AND status = :status
    AND (created_at < :cursorCreatedAt OR (created_at = :cursorCreatedAt AND id < :cursorId))
  ORDER BY created_at DESC, id DESC
  ```
- **Windowing**: Queries request `limit + 1` rows to calculate `hasMore` and construct `nextCursor` without an expensive `COUNT(*)` scan.

### 4. Soft-Deletion Semantics
- Platform posts are never physically deleted on standard client requests.
- Calling `DELETE /api/v1/posts/{id}` sets `status = PostStatus.DELETED` and timestamps `deleted_at = Instant.now()`.
- Repository methods enforce active state filters (`status = 'ACTIVE' AND deleted_at IS NULL`), preventing deleted posts from appearing in queries while preserving historical data for feed caches and audit logs.

### 5. Security & Authorization
- **No Client User ID**: Client requests never include a `userId` field. The author is resolved strictly via `CurrentUserService` from `SecurityContextHolder`.
- **IDOR Protection**: Modifying or deleting a post verifies `post.getAuthor().getId().equals(currentUser.getId())`. Violations throw `PlatformPostAccessDeniedException` (HTTP 403 Forbidden).
- **Visibility Enforcement**: `PRIVATE` posts can only be viewed by their author. Unauthorized attempts trigger HTTP 403 Forbidden.

### 6. Production REST API Endpoints
| HTTP Verb | Path | Security | Description |
|:---|:---|:---|:---|
| `POST` | `/api/v1/posts` | Authenticated | Create a new platform post with optional media attachments. |
| `GET` | `/api/v1/posts/{id}` | Authenticated | Fetch a platform post by UUID (enforcing visibility rules). |
| `PATCH` | `/api/v1/posts/{id}` | Author Only | Update body, visibility, or media attachments. |
| `DELETE` | `/api/v1/posts/{id}` | Author Only | Soft-delete a platform post. |
| `GET` | `/api/v1/posts/me` | Authenticated | Keyset cursor-paginated authored posts for the current user. |

---

# PART V: USER SOCIAL GRAPH & NETWORK ARCHITECTURE (PHASE 2)

## 1. Architectural Scope & Semantic Principles
The Network domain (`com.collabsphere_first.network`) models interpersonal user relationships supporting future Feed, Explore, Messaging, Profile counts, and Security Visibility. 

Four relationship types with distinct semantics are modeled:
1. **FOLLOW (Directional)**: Asymmetric subscription. User A follows User B without requiring User B to follow or approve. Efficient indexing enables bidirectional querying: *Who does A follow?* (`following_id = :A`) and *Who follows A?* (`follower_id = :A`).
2. **CONNECTION (Mutual Lifecycle)**: Symmetric peer relationship requiring explicit consent (`PENDING` -> `ACCEPTED` / `DECLINED` / `CANCELLED`). Dual-direction pair uniqueness is enforced at the database level using a PostgreSQL normalized expression index (`LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id)`). Reciprocal pending requests automatically transition to `ACCEPTED`.
3. **BLOCK (Authoritative Security Primitive)**: Directional security constraint with destructive cascades. Blocking User B transactionally severs all existing follows in both directions and cancels/deletes any active or pending connection. Blocked users cannot re-follow, request connections, view `FOLLOWERS_ONLY` content, or interact with content created by the blocker.
4. **MUTE (Private Feed Suppression)**: Directional and private account management tool. Muting suppresses content in future feeds without severing follows or connections, and never notifies the muted user.

## 2. Database Schema (Flyway V21)
- `user_follows`: `(id UUID, follower_id BIGINT, following_id BIGINT, created_at TIMESTAMPTZ)`
  - Constraints: `UNIQUE(follower_id, following_id)`, `follower_id != following_id`, Foreign keys with `ON DELETE CASCADE`.
  - Keyset Indexing: `(follower_id, created_at DESC, id DESC)`, `(following_id, created_at DESC, id DESC)`.
- `user_connections`: `(id UUID, requester_id BIGINT, addressee_id BIGINT, status VARCHAR(20), created_at TIMESTAMPTZ, responded_at TIMESTAMPTZ, updated_at TIMESTAMPTZ)`
  - Constraints: Unique partial index `idx_user_connections_active_pair` on `(LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id)) WHERE status IN ('PENDING', 'ACCEPTED')`.
- `user_blocks`: `(id UUID, blocker_id BIGINT, blocked_id BIGINT, created_at TIMESTAMPTZ)`
  - Constraints: `UNIQUE(blocker_id, blocked_id)`, `blocker_id != blocked_id`.
- `user_mutes`: `(id UUID, muter_id BIGINT, muted_id BIGINT, created_at TIMESTAMPTZ)`
  - Constraints: `UNIQUE(muter_id, muted_id)`, `muter_id != muted_id`.

## 3. Network Access SPI (`NetworkAccessService`)
Decoupled query interface consumed across domain boundaries (`PlatformPostService`, `ContentResolutionService`, future `ProfileService` and `FeedService`) without directly exposing entity tables:
- `isFollowing(viewerId, authorId)`
- `isConnected(userA, userB)`
- `isBlocked(blockerId, blockedId)`
- `isBlockedEitherDirection(userA, userB)`
- `isMuted(muterId, mutedId)`
- `getSocialCounts(userId)` (`followersCount`, `followingCount`, `connectionsCount`)

## 4. FOLLOWERS_ONLY Visibility Re-Enabled
With authoritative Follow relationships established, `PlatformPost.visibility == FOLLOWERS_ONLY` is fully enabled:
- **Author**: Unconditionally allowed to view and update.
- **Followers**: Allowed to view if `networkAccessService.isFollowing(viewerId, authorId)` returns true and no active block exists.
- **Non-Followers / Blocked Users**: Denied with HTTP 403 Forbidden (`PlatformPostAccessDeniedException`).

---

# PART VI: SHARED CONTENT INTERACTION FOUNDATION (PHASE 2)

## 1. Architectural Scope & Polymorphic ContentRef
The Interaction domain (`com.collabsphere_first.interaction`) manages user interactions with shared content without database-level inheritance or tight coupling to post tables.

Interactions reference content polymorphically via:
```json
{
  "contentType": "PLATFORM_POST" | "COMMUNITY_POST",
  "contentId": "UUID"
}
```

## 2. ContentResolver SPI & Centralized Security
Content validation is mediated by the `ContentResolver` interface:
```java
public interface ContentResolver {
    boolean supports(ContentType type);
    ResolvedContent resolveVisible(ContentRef contentRef, User viewer);
}
```
Two authoritative adapters implement this contract:
1. `PlatformPostContentResolver`: Delegates to `PlatformPostService.getPostById`, enforcing platform visibility rules.
2. `CommunityPostContentResolver`: Delegates to `ContentService.getPost`, enforcing community membership, visibility, and moderation status.

`ContentResolutionService` centralizes access control: before any interaction is recorded, it verifies that content exists, is not deleted, is accessible to the viewer, and that no active block exists between viewer and author in either direction.

## 3. Database Schema (Flyway V22)
- **Stateful Likes** (`content_likes`): `(id UUID, user_id BIGINT, content_type VARCHAR(30), content_id UUID, created_at TIMESTAMPTZ)`
  - Constraint: `UNIQUE(user_id, content_type, content_id)`. Idempotent `PUT` and `DELETE`.
- **Stateful Saves** (`content_saves`): `(id UUID, user_id BIGINT, content_type VARCHAR(30), content_id UUID, created_at TIMESTAMPTZ)`
  - Constraint: `UNIQUE(user_id, content_type, content_id)`. Idempotent `PUT` and `DELETE`. Powers the mobile side drawer "Saved" list.
- **Append-Only Telemetry Events** (`content_interaction_events`):
  - Fields: `(id UUID, user_id BIGINT, content_type VARCHAR(30), content_id UUID, event_type VARCHAR(30), duration_ms BIGINT, session_id VARCHAR(100), created_at TIMESTAMPTZ)`
  - Event Types: `SHARE`, `VIEW`, `WATCH_TIME`, `HIDE`, `NOT_INTERESTED`.
  - View Deduplication: Server suppresses identical `(user, content_type, content_id)` view events within a 60-second window to prevent view inflation.
  - Watch/Dwell Validation: `0 <= duration_ms <= 86,400,000` (24h max).
  - Batching: `POST /api/v1/interactions/events/batch` enforces strict maximum of 50 events per batch.

## 4. Keyset Pagination
All listings (`/saved`, `/followers`, `/following`, `/connections`, `/blocks`, `/mutes`) utilize deterministic keyset pagination powered by `CursorUtils` with Base64 URL-safe encoding of `(created_at, id)` or `(updated_at, id)` and `limit + 1` windowing.

---

# PART VII: PRODUCTION HOME FEED ARCHITECTURE (PHASE 3)

## 1. Architectural Scope & Orchestration Domain (`com.collabsphere_first.feed`)
The Home Feed module provides an aggregation and orchestration layer combining content from multiple existing domains (`post`, `community`, `network`, `interaction`, `user`).

Feed is **NOT** a new storage table for posts, **NOT** the Explore discovery module, and **NOT** a direct database reader. It implements a strict 8-step pipeline:
```text
candidate collection
        ↓
deduplication (by ContentRef)
        ↓
eligibility & security filtering (blocks, mutes, hide, not-interested, visibility)
        ↓
baseline Java ranking (deterministic multi-signal scoring)
        ↓
stable feed session creation (Redis-backed session store)
        ↓
bulk content hydration (batched domain adapters)
        ↓
interaction state enrichment (bulk liked/saved/likeCount projection)
        ↓
mobile unified response (FeedItemResponse)
```

Authoritative data remains in `platform_posts` and `community_posts`. Feed references content polymorphically via `ContentRef(ContentType, UUID)`.

## 2. Candidate Provider Architecture
Candidate collection is modularized via the `FeedCandidateProvider` SPI:
```java
public interface FeedCandidateProvider {
    FeedSourceType sourceType();
    List<FeedCandidate> collect(FeedCandidateContext context);
}
```

Five dedicated providers implement candidate discovery with strict bounds configured via `@ConfigurationProperties(prefix = "collobster.feed")`:
1. `SelfPostCandidateProvider`: Collects recent `ACTIVE` public/followers-only platform posts authored by the viewer (capped at `self-limit`, default 30). `PRIVATE` posts are excluded from Home Feed to maintain clean product semantics.
2. `FollowingPostCandidateProvider`: Resolves viewer's followed author IDs via `NetworkAccessService.findFollowingUserIds(limit)` and fetches eligible active posts (capped at `following-limit`, default 200).
3. `ConnectionPostCandidateProvider`: Resolves viewer's accepted connection IDs via `NetworkAccessService.findConnectedUserIds(limit)` and fetches eligible active posts (capped at `connection-limit`, default 200).
4. `JoinedCommunityPostCandidateProvider`: Resolves accessible community posts via `CommunityFeedReadService.findRecentEligiblePosts(user, limit)` from communities where the viewer is an approved member (capped at `community-limit`, default 200).
5. `PublicFallbackCandidateProvider`: In cold-start scenarios or when personalized pools are insufficient, fetches recent `PUBLIC` platform posts excluding viewer, muted, and blocked authors (capped at `public-fallback-limit`, default 100).

## 3. Deduplication Strategy
A single post may originate from multiple sources (e.g., an author who is both followed and connected). Deduplication operates strictly on `ContentRef`:
- Candidates sharing the same `(contentType, contentId)` are merged into a single candidate entry.
- All contributing sources are aggregated into `Set<FeedSourceType> sources` (e.g., `[FOLLOWING, CONNECTION]`).
- Candidates with matching UUIDs across different `ContentType` values (`PLATFORM_POST` vs `COMMUNITY_POST`) are never merged.

## 4. Eligibility & Security Filtering Pipeline
Every candidate is screened through authoritative security checks prior to session persistence and ranking:
- **Block Rule**: Candidates authored by any user in `NetworkAccessService.findBlockedOrBlockingUserIds(viewerId)` are purged.
- **Mute Rule**: Candidates authored by any user in `NetworkAccessService.findMutedUserIds(viewerId)` are purged.
- **Hide & Not-Interested Rules**: Exact content refs present in `InteractionService.findSuppressedContentRefs(viewerId)` (via `HIDE` or `NOT_INTERESTED` events) are removed from candidate sets.
- **Visibility Safeguards**: Only `PUBLIC` and authorized `FOLLOWERS_ONLY` posts are admitted. `PRIVATE` posts are strictly excluded.

## 5. Baseline Deterministic Java Ranking (`BaselineFeedRankingStrategy`)
A deterministic Java ranking engine computes composite scores without machine learning dependencies:
$$\text{Score} = w_{\text{recency}} \cdot S_{\text{recency}} + w_{\text{rel}} \cdot S_{\text{rel}} + w_{\text{eng}} \cdot S_{\text{eng}}$$

- **Recency Decay**: $S_{\text{recency}} = 0.5^{\frac{\Delta t}{t_{\text{half}}}}$ where $t_{\text{half}}$ defaults to 86,400 seconds (24 hours). Future timestamps are clamped to 0 age.
- **Relationship Hierarchy**: Evaluates the highest-priority source for each candidate:
  - `CONNECTION`: 1.0
  - `FOLLOWING`: 0.85
  - `JOINED_COMMUNITY`: 0.70
  - `SELF`: 0.40
  - `PUBLIC_FALLBACK`: 0.20
- **Engagement Signal**: $S_{\text{eng}} = \min(1.0, \ln(1 + \text{likeCount}) \cdot 0.1)$.
- **Deterministic Tie-Breaker**: In case of identical scores, candidates are ordered by `(createdAt DESC, contentType ASC, contentId DESC)`.
- **Phase 4 Integration Seam**: The `FeedRankingStrategy` interface allows plug-and-play substitution with `RecommendationFeedRankingStrategy` in Phase 4 without modifying candidate collection, hydration, or controller layers.

## 6. Feed Session Architecture & Redis Storage
To prevent feed churn and jumping on pagination, Feed uses stable server-side sessions:
- **Session Data**: `FeedSession(sessionId, userId, createdAt, orderedContentRefs, sourceMetadata)`. Only lightweight references and metadata are stored—never raw serialized post bodies.
- **Production Store**: `RedisFeedSessionStore` serializes sessions under the namespaced key `collobster:feed:session:{sessionId}` with a configurable TTL (`collobster.feed.session.ttl`, default: 20 minutes).
- **Session Security**: Session ownership is strictly validated. Submitting a cursor for another user's session triggers HTTP 403 Forbidden (`FeedSessionAccessDeniedException`).

## 7. Cursor Pagination Contract
- **Endpoint**: `GET /api/v1/feed?cursor={cursor}&limit={limit}`
  - No cursor: Generates a new `FeedSession`, returning page 1.
  - With cursor: Decodes Base64 URL-safe `"{sessionId}:{position}"`, verifies ownership, and fetches subsequent items.
- **Stale Content Revalidation**: If an item in the session was deleted, muted, or blocked after session creation, the hydrator skips it and continues reading from the session until the requested page size is filled.
- **Status Codes**:
  - `400 Bad Request`: Malformed or negative cursor (`InvalidFeedCursorException`).
  - `410 Gone`: Expired session (`FeedSessionExpiredException` with error code `FEED_SESSION_EXPIRED`), prompting the mobile app to refresh cleanly.
  - `403 Forbidden`: Mismatched session owner (`FeedSessionAccessDeniedException`).
  - `503 Service Unavailable`: Temporary session store failure (`FeedSessionStorageException`).

## 8. Bulk Content Hydration & Interaction State Enrichment
Hydration resolves lightweight `ContentRef` items into complete presentation models without N+1 queries:
- `FeedContentHydrator`: Groups candidates by `ContentType` and dispatches to batch adapters (`PlatformPostFeedAdapter` and `CommunityPostFeedAdapter`).
- `InteractionService.getInteractionSummaries`: Executes batch grouped queries across `content_likes` and `content_saves` to produce an `InteractionSummary(liked, saved, likeCount)` mapping in a single round-trip.

## 9. Mobile Unified Response (`FeedItemResponse`)
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "contentRef": {
          "type": "PLATFORM_POST",
          "id": "3a1c8f9b-1234-5678-9abc-def012345678"
        },
        "source": ["FOLLOWING"],
        "author": {
          "id": 42,
          "username": "alex",
          "displayName": "Alex Chen",
          "avatarUrl": "https://cdn.collobster.com/avatars/42.jpg"
        },
        "title": null,
        "body": "Excited to launch our new project!",
        "media": [],
        "interactionState": {
          "liked": true,
          "saved": false
        },
        "engagement": {
          "likeCount": 15
        },
        "community": null,
        "createdAt": "2026-09-27T12:00:00Z",
        "updatedAt": "2026-09-27T12:00:00Z"
      }
    ],
    "nextCursor": "YWJjLTEyMzQ6MjA",
    "hasMore": true
  }
}
```

---

# IX. PRODUCTION JAVA ↔ PYTHON RECOMMENDATION INTEGRATION (PHASE 4)

## 1. Architectural Boundary & Service Ownership
Collobster maintains a strict architectural boundary between the transactional backend and ML recommendation engine:
- **Java 21 + Spring Boot Backend**: Authoritative owner of user accounts, authentication/authorization, social graph/network access, content CRUD, candidate generation, security filtering, feed sessions, Redis caching, content hydration, and mobile client delivery.
- **Python ML Engine**: Pure ranking service operating statelessly over candidate feature payloads submitted by Java. Python never connects directly to mobile clients, never manages sessions, and does not query the operational database during online feed generation.
- **Security Invariant**: Java performs 100% of candidate eligibility screening (blocks, mutes, hide/not-interested, private visibility, community membership) *before* invoking Python. Java verifies that every `ContentRef` returned by Python existed in the submitted candidate set; any unknown, unexpected, or unauthorized refs injected by Python are dropped immediately with security warnings logged.

## 2. Canonical Identity Contract (`ContentRef`)
- The online integration contract exclusively uses `ContentRef(type, id)` where `type` is `PLATFORM_POST` or `COMMUNITY_POST`, and `id` is a canonical UUID string (RFC 4122).
- Integer `post_id` schemas from legacy Python implementations are deprecated and eliminated from online ranking.
- If a `PLATFORM_POST` and `COMMUNITY_POST` share the exact same UUID, both systems treat them as strictly distinct entities with independent feature payloads, scores, and rankings.

## 3. Internal HTTP Ranking API Contract
- **Endpoint**: `POST /api/v1/internal/recommend/feed/rank`
- **Authentication**: Protected via internal service bearer token (`Authorization: Bearer <internalToken>`). Unauthorized calls return `401 Unauthorized`.
- **Contract Versioning**: Request and response enforce `"contractVersion": "1"`. Unsupported versions return `422 Unprocessable Entity`.
- **Payload Limits**: Candidate list length is bounded (max 500 candidates). Requests exceeding limits or containing duplicate candidate refs are rejected.
- **Request Schema**:
```json
{
  "contractVersion": "1",
  "requestId": "4e6bab7c-4e22-4f6c-8176-ea6e4064ab86",
  "user": { "id": 105 },
  "candidates": [
    {
      "contentRef": { "type": "PLATFORM_POST", "id": "13c09480-ba67-4471-8222-e39b2757cb16" },
      "authorId": 42,
      "createdAt": "2026-09-28T05:00:00Z",
      "sources": ["FOLLOWING", "CONNECTION"],
      "features": {
        "freshness": 0.95,
        "quality": null,
        "popularity": 0.42,
        "preference": null,
        "interest": null,
        "relationship": 1.0,
        "community": 0.0,
        "userInteractionCount": null,
        "postInteractionCount": 15,
        "creatorReputation": null
      }
    }
  ]
}
```
- **Response Schema**:
```json
{
  "contractVersion": "1",
  "requestId": "4e6bab7c-4e22-4f6c-8176-ea6e4064ab86",
  "model": {
    "name": "lightgbm-feed-ranker",
    "version": "2.0.0",
    "strategy": "lightgbm_lambdarank"
  },
  "items": [
    {
      "contentRef": { "type": "PLATFORM_POST", "id": "13c09480-ba67-4471-8222-e39b2757cb16" },
      "rank": 1,
      "score": 0.884
    }
  ]
}
```

## 4. Online Feature Assembly & Missing Value Contract
- Feature extraction is encapsulated in Java within `RecommendationFeatureAssembler`.
- **Authoritative Signals**:
  - `freshness`: Continuous decay $0.5^{(\Delta t / 86400)}$ computed in Java from candidate `createdAt`.
  - `relationship`: Derived deterministically from source hierarchy (`CONNECTION` = 1.0, `FOLLOWING` = 0.85, `JOINED_COMMUNITY` = 0.70, `SELF` = 0.40, `PUBLIC_FALLBACK` = 0.20).
  - `community`: 1.0 if candidate originates from `JOINED_COMMUNITY`, else 0.0.
  - `popularity`: Log-scaled like count: $\min(1.0, \ln(1 + \text{likeCount}) \cdot 0.1)$.
  - `postInteractionCount`: Raw interaction count from authoritative Java interaction summaries.
- **Missing Value Handling**: Unobserved or unavailable signals (`quality`, `preference`, `interest`, `userInteractionCount`, `creatorReputation`) are transmitted explicitly as `null` by Java. Python maps numerical nulls to `np.nan`, natively handled by LightGBM's default split directions without synthetic data fabrication.

## 5. Model Inference, Fallback Hierarchy & Resilience
Ranking executes through a multi-tier resilience hierarchy:
```text
Java Candidate Generation + Security Filtering
                  ↓
RecommendationFeedRankingStrategy
                  ↓
       Python LightGBM Inference
                  ↓ (if model load error / inference exception)
       Python WeightedSum Fallback
                  ↓ (if HTTP timeout / 5xx / 4xx / malformed response / circuit open)
       Java BaselineFeedRankingStrategy (Deterministic Recency + Relationship + LikeCount)
```

- **HTTP Client Timeouts**: Configured via `collobster.recommendation.connect-timeout` (default 500ms) and `read-timeout` (default 1000ms).
- **Circuit Breaker (`Resilience4j`)**:
  - Encapsulated in `RecommendationCircuitBreaker` wrapping `io.github.resilience4j:resilience4j-circuitbreaker:2.2.0`.
  - Policy: Count-based sliding window of 5 calls, 50% failure rate threshold, 10s open wait duration, 2 permitted half-open probes.
  - Failure Classification:
    - *Recorded as Circuit Failures*: Transient infrastructure/server outages (connection refused, I/O timeouts via `ResourceAccessException`, HTTP 5xx `HttpServerErrorException`, malformed/empty response body).
    - *Ignored by Circuit Health*: Client-side configuration/contract errors (`HttpClientErrorException`, e.g. 401 Unauthorized, 422 Unprocessable Entity). These trigger safe Java baseline fallback for the specific request without tripping the circuit breaker for healthy traffic.
  - Fast-fails when OPEN directly to `BaselineFeedRankingStrategy` without incurring network latency or thread exhaustion.
  - Timeout Ownership: Strictly governed by `SimpleClientHttpRequestFactory` (connect 500ms, read 1000ms). No redundant time limiters or retry loops (preventing retry storms in Feed latency path).
- **Partial Response Handling**: If Python returns a subset of candidates, Python-ranked items appear first, followed by the remaining eligible candidates ordered deterministically by `BaselineFeedRankingStrategy`.
- **Zero Mobile Impact**: Mobile client always receives `FeedPageResponse` from `GET /api/v1/feed` with identical schema and HTTP 200 regardless of whether ranking was ML-driven or baseline fallback.

---

# X. MODEL FEATURE CONTRACT & RANKING QUALITY HARDENING (PHASE 4.1)

## 1. Feature Contract Versioning (`featureContractVersion`)
To decouple the HTTP JSON schema from evolving ML feature definitions, the integration contract explicitly enforces:
- `"contractVersion": "1"`: The structural API request/response format.
- `"featureContractVersion": "1"`: The semantic feature definition contract.
Requests specifying unsupported feature contract versions are rejected with HTTP 422 Unprocessable Entity, allowing independent iteration of ML model generations without breaking existing API integrations.

## 2. Training Alignment & Compatibility Audit
LightGBM ranker v2 (`lgbm_ranker_model_v2.txt`) was trained over 10 feature inputs. The production feature assembler adheres to strict compatibility boundaries:
- **`freshness` (VERIFIED MATCH)**: Continuous decay $0.5^{\Delta t / 86400}$ mathematically mirrors the training function $\exp(-\frac{\ln 2}{24} \cdot \text{ageHours})$.
- **`quality` (NOT AVAILABLE)**: Transmitted as `null`, handled natively as `NaN` by LightGBM.
- **`popularity` (PARTIAL MATCH)**: Logarithmic like-count $\min(1.0, \ln(1 + \text{likes}) \cdot 0.1)$.
- **`preference` (NOT AVAILABLE)**: Transmitted as `null`, handled as `NaN`.
- **`interest` (NOT AVAILABLE)**: Transmitted as `null`, handled as `NaN`.
- **`relationship` (PARTIAL MATCH)**: Source proximity tiers (1.0, 0.85, 0.70, 0.40, 0.20) align with decision split thresholds in the model trees.
- **`community` (PARTIAL MATCH)**: Binary membership indicator (1.0 for member, 0.0 otherwise).
- **`user_interaction_count` (NOT AVAILABLE)**: Transmitted as `null`, handled as `NaN`.
- **`post_interaction_count` (NOT AVAILABLE)**: Corrected in Phase 4.1. Training model required exponentially normalized saturation $1 - \exp(-\text{cnt}/20) \in [0.0, 1.0]$. Raw like counts were unscaled ($> 1.0$) and exceeded model threshold bounds ($\le 0.5392$). Set to `null` to avoid model distortion until full multi-modal interaction tracking is available.
- **`creator_reputation` (NOT AVAILABLE)**: Transmitted as `null`, handled as `NaN`.

## 3. Machine-Readable Model Metadata & Startup Integrity
- Model artifact metadata is formalized in `artifacts/lgbm_ranker_model_v2.metadata.json`, declaring `modelName`, `modelVersion`, `featureContractVersion`, `features`, `objective`, and `featureRanges`.
- Python's `InternalFeedRankingService` validates that the loaded Booster's feature names and ordering strictly match the metadata declaration. Any mismatch or corruption immediately marks the model as degraded and gracefully switches ranking to `python_weighted_sum_fallback`.

## 4. Feature Range Boundary Enforcement
- All normalized signals (`freshness`, `relationship`, `community`, `popularity`, `quality`, `preference`, `interest`, `creator_reputation`) must strictly reside in $[0.0, 1.0]$.
- Counts (`user_interaction_count`, `post_interaction_count`) must be non-negative ($\ge 0.0$).
- Out-of-bounds or non-finite inputs are rejected at the FastAPI schema boundary with HTTP 422, cleanly falling back to Java baseline ranking without crashing Feed generation.

## 5. Live Test Reproducibility
- An explicit Maven profile `python-integration` allows deterministic execution of real end-to-end integration tests:
  ```powershell
  mvnw.cmd test -Ppython-integration -Dtest=LivePythonRecommendationIntegrationTest
  ```

---

# XI. PRODUCTION EXPLORE & DISCOVERY CORE (PHASE 5)

## 1. Product Context & Boundaries
The Collobster mobile bottom navigation separates **Feed** from **Explore**:
- **Feed (`GET /api/v1/feed`)**: "What content should I see now from my network and communities?" (Personalized, session-backed, ML-ranked).
- **Explore (`/api/v1/explore/...`)**: "What new content, people, and communities can I discover across the platform?" (Global search, trending, recent, discovery).

### Technology & Architectural Boundaries:
- **100% Java 21 + Spring Boot 3.5.x**: All Explore business logic, search query pipelines, trending calculations, and keyset cursor handling execute entirely in Java.
- **Python ML Isolation**: The Python recommendation service is **NOT** invoked for Explore search, trending, or candidate generation. No new Python endpoints were introduced in Phase 5.
- **No Direct Messaging**: Messages (`/api/v1/messages`) remain completely unimplemented until Phase 6.
- **Feed Unmodified**: Feed candidate generation, LightGBM integration, Redis session contracts, and polymorphic hydration were preserved without regression.

## 2. Package Structure (`com.collabsphere_first.explore`)
```text
explore/
  config/       # ExploreProperties (@ConfigurationProperties("collobster.explore"))
  controller/   # ExploreController (9 REST endpoints)
  cursor/       # ExploreSearchCursor, ExploreKeysetCursor, ExploreCursorCodec
  dto/          # Section, home, search, trending, and discovery DTOs
  exception/    # ExploreException, InvalidExploreQueryException, InvalidExploreCursorException
  hydration/    # ExploreContentHydrator (polymorphic post hydration & interaction enrichment)
  repository/   # Native Postgres full-text search & window aggregation repositories
  scoring/      # TrendingScoreCalculator (deterministic multi-signal decay formula)
  service/      # PostSearch, PeopleSearch, CommunitySearch, Trending, Recent, Discovery services
```

## 3. Authoritative Privacy & Security Invariants
Search visibility is strictly a subset of direct content retrieval visibility ($\text{Search Visibility} \le \text{Direct Visibility}$):
1. **Platform Post Status & Privacy**: Only `status = 'ACTIVE'` (from authoritative `PostStatus` enum) platform posts are eligible for search and discovery. Soft-deleted (`status = 'DELETED'`) posts are excluded at the SQL level and rejected during hydration. `visibility = 'PRIVATE'` posts are strictly hidden from search.
2. **Followers-Only Posts**: `visibility = 'FOLLOWERS_ONLY'` posts are only discoverable if `viewerId == authorId` or if the viewer actively follows the author according to `NetworkAccessService`.
3. **Community Posts & Moderation**: Community posts must have `moderation_status = 'VISIBLE'` (from authoritative `ModerationStatus` enum; there is no `APPROVED` enum) and belong to an active community (`community_status = 'ACTIVE'`) that is publicly accessible (`visibility = 'PUBLIC'`) or where the viewer holds an active membership. Non-visible moderation states (`PENDING_REVIEW`, `FLAGGED`, `REMOVED`) are completely excluded.
4. **Bidirectional Block Enforcement**: If User A blocks User B or User B blocks User A, all posts, profile summaries, search results, and trending items authored by the counterpart are completely eliminated.
5. **Mute Boundary**:
   - **Passive Explore (Trending, Recent, Discovery)**: Content and profiles from muted users are strictly suppressed.
   - **Direct Explicit Search**: Direct search queries allow finding muted accounts if explicitly searched by handle/name (intentional intent), but their content remains suppressed from passive recommendations.
6. **Polymorphic ContentRef Safety**: Platform posts and Community posts share identical UUID spaces without collision. Every post result is strongly typed with `ContentRef[type, id]`. Grouping and aggregation in trending always use `(content_type, content_id)`.
7. **Comment Count Contract**: `PlatformPost` does not currently support comments (`commentCount = 0` returned as API contract indicating un-commentable content). `CommunityPost` returns the authoritative comment count from the community domain.

## 4. PostgreSQL 16 Search & Index Strategy (Flyway Migration V23)
Flyway migration `V23__add_explore_search_indexes.sql` establishes native PostgreSQL full-text search without requiring external search engines:
- **Platform Posts GIN**: `idx_platform_posts_search_tsv` on `to_tsvector('english', coalesce(body, ''))`. Queries match this exact expression with `plainto_tsquery('english', :q)`.
- **Community Posts GIN**: `idx_posts_search_tsv` on `to_tsvector('english', coalesce(title, '') || ' ' || coalesce(body, ''))`.
- **Communities GIN**: `idx_communities_search_tsv` on `to_tsvector('english', coalesce(name, '') || ' ' || coalesce(description, ''))`.
- **Users GIN & B-tree**:
  - `idx_users_search_tsv` on `to_tsvector('simple', coalesce(username, '') || ' ' || coalesce(first_name, '') || ' ' || coalesce(last_name, ''))`.
  - `idx_users_username_lower`, `idx_users_first_name_lower`, `idx_users_last_name_lower` (`varchar_pattern_ops`) for prefix matches (`lower(username) LIKE concat(lower(:q), '%')`).
- **Language Strategy**:
  - Version 1 Explore full-text search intentionally uses `'english'` stemming dictionary for posts and communities (e.g. "running" matches "run").
  - User search uses `'simple'` dictionary to preserve exact tokenization without stemming names.
  - Multilingual search is a documented Phase 6+ evolution requirement that will introduce language detection and unaccented/simple vector fallback.
- **Trending Window Indexes**:
  - `idx_content_likes_trending` on `content_likes(created_at DESC, content_type, content_id)`.
  - `idx_content_saves_trending` on `content_saves(created_at DESC, content_type, content_id)`.
  - `idx_interaction_events_trending` on `content_interaction_events(event_timestamp DESC, event_type, content_type, content_id)`.

## 5. Keyset & Query-Bound Cursor Engine
Pagination across Explore avoids large SQL `OFFSET` scans by utilizing opaque Base64 URL-safe keyset cursors:
- **Search Keyset Cursor (`ExploreSearchCursor`)**:
  - Encodes: `(queryHash, rank, createdAtMs, id, contentType, position)`.
  - **Query-Binding**: The cursor contains the SHA-256 hash prefix of the normalized query. A cursor generated for query `"rust"` is strictly rejected with HTTP 400 `InvalidExploreCursorException` if submitted with query `"java"`.
  - **Input Validation**: All decoded numeric fields are strictly bounds-checked (e.g., non-negative integer position, finite float rank/score).
- **Discovery Keyset Cursor (`ExploreKeysetCursor`)**:
  - Encodes: `(timestampMs, id, contentType, score)`.
  - Supports practical keyset pagination for trending, recent, people discovery, and community discovery.
  - **Pagination Consistency Level**: Unlike Feed which guarantees snapshot session consistency via Redis session stores, Explore uses live keyset cursors. In high-concurrency environments where scores shift continuously, a client paginating live trending may encounter minor item shifts across page boundaries. This trade-off preserves low memory consumption and high scalability without session overhead.

## 6. Deterministic Trending Formula
Trending scores are computed dynamically from recent interactions over a sliding time window (default 24h, configured in `ExploreProperties`):
$$\text{RawScore} = (w_{\text{like}} \cdot L) + (w_{\text{share}} \cdot S) + (w_{\text{view}} \cdot V) + (w_{\text{save}} \cdot K) + (w_{\text{watch}} \cdot \ln(1 + W))$$
$$\text{Decay} = 0.5^{\frac{\Delta t}{T_{\text{half-life}}}}$$
$$\text{FinalScore} = \text{RawScore} \cdot \text{Decay}$$
- **Default Weights**: Like = 2.0, Share = 4.0, View = 0.2, Save = 3.0, WatchTime = 0.05, Half-life = 12h.
- **Abuse Resistance**: View counts and interaction events reuse existing Interaction module deduplication (authoritative 60-second temporal deduplication window per viewer/content pair in `InteractionServiceImpl`). Repeated rapid clicks within 60 seconds are dropped by the Interaction service.
- **Positive Signals Only**: `HIDE` and `NOT_INTERESTED` events are negative suppression signals and are never counted as positive engagement in trending aggregation.
- **Watch Time Aggregation**: Watch time uses `SUM(duration_ms)` bounded by client-side limits ($0 \le \text{durationMs} \le 86,400,000$).
- **Authoritative Hydration Revalidation**: Historical interaction aggregation can produce candidate IDs for posts that have subsequently been deleted or made private. The Explore hydration layer strictly drops any candidates whose current state is not accessible to the viewer.

## 7. Performance & N+1 Prevention
- **Batch Relationship State**: User discovery and people search query social states in bulk (`findAllBySourceUserIdAndTargetUserIdIn`, `findActiveConnectionsBetween`) rather than executing 1 query per result.
- **Batch Community Membership**: Community queries retrieve viewer membership states in a single parameterized `IN` clause.
- **Polymorphic Hydration**: Platform posts and Community posts are queried in single batched ID lookups, followed by bulk interaction summary enrichment (`InteractionService.getInteractionSummaries`).

## 8. Explore REST API Surface
| Endpoint | Description | Pagination |
| :--- | :--- | :--- |
| `GET /api/v1/explore` | Bounded landing page previews (trending, recent, people, communities) | Bounded limit per section |
| `GET /api/v1/explore/search?q=...` | Grouped search preview across posts, people, communities | Bounded preview (max 5 each) |
| `GET /api/v1/explore/search/posts?q=...&cursor=...&limit=...` | Typed full-text post search across Platform and Community posts | Keyset query-bound cursor |
| `GET /api/v1/explore/search/people?q=...&cursor=...&limit=...` | Typed people search by handle, first/last name | Keyset query-bound cursor |
| `GET /api/v1/explore/search/communities?q=...&cursor=...&limit=...` | Typed community search by name, description, tags | Keyset query-bound cursor |
| `GET /api/v1/explore/posts/trending?cursor=...&limit=...` | Paginated platform trending posts | Score & recency keyset cursor |
| `GET /api/v1/explore/posts/recent?cursor=...&limit=...` | Paginated recent public & accessible posts | CreatedAt keyset cursor |
| `GET /api/v1/explore/people?cursor=...&limit=...` | People discovery suggestions | Stable keyset cursor |
| `GET /api/v1/explore/communities?cursor=...&limit=...` | Community discovery suggestions | Stable keyset cursor |

---

# XI. PRODUCTION DIRECT MESSAGING & REALTIME WEBSOCKET (PHASE 6)

## 1. Product Boundary: Direct Messages vs. Community Channels
Collobster enforces a strict separation between Platform Direct Messages (`messaging/`) and Community Channels (`community/`):
- **Community Channels**: Context-bound to a specific Community and Channel. Group-oriented, permissions governed by community roles (OWNER, MODERATOR, MEMBER) via `CommunityAccessService`, realtime published to `/topic/communities/{communityId}/channels/{channelId}` over `/ws/community`.
- **Platform Direct Messages**: Private user-to-user conversations. 1:1 direct pair normalization, participant-level access control via `MessagingAccessService`, realtime published to private user queues (`/user/queue/...`) over `/ws/messages`.
- **Isolation Invariant**: Direct messages are NEVER stored in community message tables. Community channels are NEVER reused as private DMs. The WebSocket endpoints `/ws/community` and `/ws/messages` maintain distinct STOMP interceptor security pipelines.

## 2. Database Schema & Migration (`V24`)
PostgreSQL 16 is the authoritative source of truth. Ephemeral realtime state (typing, presence) lives in Redis, while all message durability and conversation history are backed by PostgreSQL:
- **`conversations`**:
  - `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
  - `type VARCHAR(32) NOT NULL` (strictly `'DIRECT'` in Phase 6; extensible to `'GROUP'` in future phases)
  - `direct_user_low_id BIGINT`
  - `direct_user_high_id BIGINT`
  - `last_message_id UUID`
  - `last_message_at TIMESTAMPTZ`
  - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - **Constraints**:
    - `chk_direct_pair_ordering`: `CHECK (direct_user_low_id < direct_user_high_id)`
    - `uq_conversations_direct_pair`: `UNIQUE (direct_user_low_id, direct_user_high_id)`
- **`conversation_participants`**:
  - `conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE`
  - `user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE`
  - `joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - `last_read_message_id UUID`
  - `last_read_at TIMESTAMPTZ`
  - `notifications_muted BOOLEAN NOT NULL DEFAULT FALSE`
  - `archived BOOLEAN NOT NULL DEFAULT FALSE`
  - `deleted_for_user_at TIMESTAMPTZ`
  - `PRIMARY KEY (conversation_id, user_id)`
- **`direct_messages`**:
  - `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
  - `conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE`
  - `sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE`
  - `client_message_id UUID` (client idempotency identifier)
  - `message_type VARCHAR(32) NOT NULL` (`'TEXT'`, `'ATTACHMENT'`, `'MIXED'`)
  - `body TEXT`
  - `reply_to_message_id UUID REFERENCES direct_messages(id) ON DELETE SET NULL`
  - `status VARCHAR(32) NOT NULL` (`'ACTIVE'`, `'DELETED'`)
  - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - `edited_at TIMESTAMPTZ`
  - `deleted_at TIMESTAMPTZ`
  - `version BIGINT NOT NULL DEFAULT 0` (`@Version` optimistic locking)
  - **Index**: `uq_direct_messages_client_id`: `UNIQUE (conversation_id, sender_id, client_message_id) WHERE client_message_id IS NOT NULL`
- **`message_attachments`**:
  - `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
  - `message_id UUID NOT NULL REFERENCES direct_messages(id) ON DELETE CASCADE`
  - `media_type VARCHAR(32) NOT NULL` (`'IMAGE'`, `'VIDEO'`, `'AUDIO'`, `'DOCUMENT'`)
  - `storage_key VARCHAR(512) NOT NULL`
  - `mime_type VARCHAR(128) NOT NULL`
  - `file_name VARCHAR(255) NOT NULL`
  - `file_size BIGINT NOT NULL`
  - `width INTEGER`, `height INTEGER`, `duration_ms INTEGER`
  - `display_order INTEGER NOT NULL DEFAULT 0`
  - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
- **`message_reactions`**:
  - `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
  - `message_id UUID NOT NULL REFERENCES direct_messages(id) ON DELETE CASCADE`
  - `user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE`
  - `reaction VARCHAR(64) NOT NULL`
  - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - `UNIQUE (message_id, user_id)` (one reaction per user per message)
- **`message_receipts`**:
  - `message_id UUID NOT NULL REFERENCES direct_messages(id) ON DELETE CASCADE`
  - `user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE`
  - `delivered_at TIMESTAMPTZ`
  - `read_at TIMESTAMPTZ`
  - `PRIMARY KEY (message_id, user_id)`

## 3. Direct Pair Identity & Concurrency Safety
- When two users $A$ and $B$ interact, their IDs are normalized:
  $$\text{low} = \min(A, B), \quad \text{high} = \max(A, B)$$
- Database check constraint `direct_user_low_id < direct_user_high_id` guarantees consistent ordering.
- Database unique constraint `UNIQUE (direct_user_low_id, direct_user_high_id)` guarantees that concurrent race conditions ($A \to B$ and $B \to A$ simultaneously) yield exactly one conversation row. The losing thread catches `DataIntegrityViolationException` and reloads the existing conversation atomically.

## 4. Message Lifecycle & Idempotency
- **Idempotent Send**: Mobile clients generate a `clientMessageId (UUID)` per send attempt. The database enforces `UNIQUE (conversation_id, sender_id, client_message_id)`. If a mobile client retries after a network timeout, the server detects the idempotency key and returns the already committed message without duplicating it.
- **Message Editing**: Only the original sender may edit an active message. The body is updated, `edited_at` is stamped, and a `MESSAGE_UPDATED` realtime event is broadcast. Attachments and creation metadata remain immutable.
- **Soft Deletion**: Only the original sender may delete a message. Soft deletion sets `status = DELETED` and `deleted_at = NOW()`. The body is nulled in DTO responses and attachments are hidden, preserving conversation ordering, reply integrity, and auditability.
- **Replies**: Messages can reference `reply_to_message_id`. Replies are strictly verified to exist within the same conversation (cross-conversation replies are rejected).
- **Reactions**: Participants can add, update, or remove a reaction. At most one reaction per user per message is maintained. Reactions on deleted messages are rejected.

## 5. Delivery & Read Receipts
- **`SENT`**: Successfully committed to PostgreSQL.
- **`DELIVERED`**: The recipient client or session acknowledged delivery via STOMP `/app/messages/delivery-ack` or REST `POST /api/v1/messages/{messageId}/delivered`. `delivered_at` is set idempotently.
- **`READ`**: The recipient explicitly marked the conversation read via REST `POST /api/v1/messages/conversations/{conversationId}/read`. Monotonic advancement ensures read cursors never move backward. Receipts for messages up to the cursor are updated transactionally.
- **Unread Count**: Calculated via indexed queries on `direct_messages(conversation_id, sender_id, status, created_at)` filtering messages sent by other participants created after the user's `last_read_at`.

## 6. Authoritative Block Semantics
- Bidirectional block enforcement checks `NetworkAccessService.isBlockedEitherDirection(viewerId, otherUserId)`.
- **Policy**: Block freezes historical direct conversations to **read-only**:
  - Existing message history remains readable by its participants.
  - Neither participant can send new messages (`BlockedUserException`).
  - Neither participant can add reactions.
  - Neither participant can send typing indicators.
  - New direct conversations between blocked pairs are strictly rejected.
  - Unblocking immediately restores normal send and reaction eligibility without mutating historical messages.

## 7. Realtime WebSocket Architecture (`/ws/messages`)
- **Transport**: Spring WebSocket + STOMP over SockJS/pure WebSocket.
- **Authentication**: JWT validation on STOMP `CONNECT` frame via `MessagingStompAuthorizationInterceptor`.
- **Private User Destinations**: All messages and events are delivered via private user queues:
  - `/user/queue/messages` (new message delivery)
  - `/user/queue/message-events` (edits, soft deletions, reaction updates, presence)
  - `/user/queue/receipts` (delivery acknowledgments, read receipts)
  - `/user/queue/typing` (ephemeral typing indicators)
- **Security Interception**: Clients cannot subscribe to arbitrary topic destinations or snooped queues. Direct subscriptions to `/topic/**` outside community or foreign user queues are rejected with `IllegalArgumentException`.
- **Transactional After-Commit Publishing**: Realtime events are published exclusively after the PostgreSQL transaction commits via `TransactionSynchronizationManager.registerSynchronization(afterCommit)`. If WebSocket delivery fails, the committed message remains durably stored and recoverable via REST.

## 8. Ephemeral State: Redis Typing & Presence
- **Typing Indicators**: Ephemeral state stored in Redis under `collobster:messages:typing:{conversationId}:{userId}` with a 6-second TTL. Automatically expires if `TYPING_STOPPED` is not received. Never written to PostgreSQL.
- **Multi-Device Presence**: Tracked in Redis via session sets `collobster:messages:presence:sessions:{userId}` and status key `collobster:messages:presence:{userId}` with a 60-second heartbeat TTL. Disconnecting one device leaves the user `ONLINE` as long as active sessions remain in the set. The user transitions to `OFFLINE` only when all sessions disconnect or expire.
- **Presence Privacy**: Presence is queryable only in the context of an existing direct conversation, never globally enumerable.
- **Graceful Degradation**: If Redis is unavailable, ephemeral typing and presence gracefully degrade without interrupting PostgreSQL message persistence.

## 9. Keyset Cursor Pagination
- **Conversations**: Paginated via `(last_message_at DESC, conversation_id DESC)` with opaque Base64 URL-safe JSON cursors (`ConversationCursor`). Empty conversations (without messages) are suppressed from conversation lists.
- **Messages**: Paginated chronologically via `(created_at DESC, id DESC)` with opaque Base64 URL-safe JSON cursors (`MessageCursor`). Malformed cursors return HTTP 400 `InvalidMessagingCursorException`.

## 10. Rate Limiting & Abuse Prevention
- Configurable rate limits managed by `MessagingRateLimiter` using Redis token-bucket sliding windows with local Bucket4j fallback:
  - Message send: 60/min
  - Conversation creation: 30/hr
  - Reactions: 60/min
  - Typing events: 120/min
- Body size strictly bounded to 10,000 characters. Attachments bounded to 10 items and 25 MB max per file with whitelisted MIME types.
- Strict path traversal protection on storage keys: absolute paths, `:`, and `..` sequences are forbidden.

## 11. Seams for Future Evolution
- **Future Group Messaging Seam**: `conversations.type` supports `GROUP`, and `conversation_participants` supports $N \ge 3$ members. No group endpoints or group mutation APIs are exposed in Phase 6.
- **Future Global Notification Seam**: Messaging emits internal Spring Application Events (`DirectMessageCreatedEvent`, `DirectMessageReactionEvent`). Phase 7 Notification infrastructure will consume these asynchronously for mobile push and email.
- **Multi-Instance Broker Seam**: The current simple in-memory broker is isolated to single-instance deployments. For horizontal scaling, `CommunityWebSocketConfig` provides configuration seams for STOMP broker relay (RabbitMQ / ActiveMQ) or Redis pub/sub.

---

## 24. Direct Messaging Phase 6.1 Hardening Invariants

### 1. Canonical Read Cursor Ordering & Disambiguation
- **Authoritative Cursor**: `conversation_participants.last_read_message_id` is the sole authoritative cursor pointer. `last_read_at` is preserved purely as an audit timestamp indicating when the acknowledgment was processed.
- **Canonical Ordering Tuple**: Ordered strictly by $(t, u) \equiv (\text{createdAt}, \text{id})$ matching PostgreSQL index collation: `ORDER BY createdAt DESC, id DESC`.
- **Tiebreaker Comparator**: Disambiguates same-millisecond messages using unsigned 64-bit integer comparisons (`Long.compareUnsigned`) matching PostgreSQL binary collation.
- **Monotonic Advance**: Read ACKs advance the cursor only if $(t_{\text{target}}, u_{\text{target}}) \succ (t_{\text{current}}, u_{\text{current}})$. Delayed or out-of-order ACKs are absorbed idempotently without regressing.
- **Unread Count**: Evaluated strictly after $(t_{\text{cursor}}, u_{\text{cursor}})$, excluding the viewer's own messages (`senderId != userId`) and soft-deleted messages.

### 2. Concurrent Send Idempotency & Conflict Resolution
- **Unique Partial Index**: `uq_direct_messages_client_id` on `(conversation_id, sender_id, client_message_id) WHERE client_message_id IS NOT NULL`.
- **Race Condition Handling**: Concurrently racing threads catching `DataIntegrityViolationException` roll back the inner persist transaction and reload the committed message. Both threads return HTTP 200/201 with identical message IDs. Exactly one database row and one `DirectMessageCreatedEvent` / WebSocket notification is emitted.
- **Payload Re-use Conflict**: Reusing an existing `clientMessageId` with an altered body, reply target, or attachments throws `IdempotencyConflictException`, returning HTTP 409 Conflict (`IDEMPOTENCY_KEY_REUSED`).

### 3. Attachment Ownership & Signed URL Authorization
- **Uploaded Media Registry (`V25`)**: `uploaded_media` table tracks `(owner_user_id, storage_key, media_type, mime_type, file_size)`.
- **Attachment Pre-Validation**: Senders can only attach media assets registered to their own user ID. Attempting to attach another user's asset or an unregistered key throws HTTP 403 `MessagingAccessDeniedException`.
- **Signed URL Access**: `GET /api/v1/messages/conversations/{id}/attachments/{attId}/signed-url` strictly enforces conversation participation via `accessService.requireParticipant(conversationId, currentUserId)`.

### 4. Ephemeral Presence: Redis ZSET Multi-Session Modeling
- **Data Structure**: `collobster:messages:presence:sessions:{userId}` Sorted Set storing `member = sessionId`, `score = epochExpiryMillis`.
- **Heartbeat Protocol**: Refreshed via `/app/messages/heartbeat` and on any incoming STOMP frame.
- **Multi-Device State**: A user remains `ONLINE` as long as at least one unexpired session remains in the ZSET.
- **Stale Pruning**: Expired sessions (`score < now`) are pruned automatically on connect, disconnect, and presence queries via `ZREMRANGEBYSCORE`.

### 5. Delivery ACK Authorization & Receipt Idempotency
- **Anti-Self-ACK**: Senders cannot mark their own messages as delivered (`MessagingAccessDeniedException`).
- **Participant Guard**: Non-participants cannot acknowledge delivery.
- **Idempotency**: Repeated delivery ACKs preserve the initial `delivered_at` timestamp.

### 6. Message Edit Window
- **Constraint**: `collabster.messaging.message-edit-window` enforces a maximum 15-minute edit window from message creation time, preventing post-facto message tampering.

---

## 25. Global Notification Center Architecture (Phase 7)

### 1. Architectural Philosophy & Boundaries
- **Derived Event Consumer**: The Global Notification domain (`com.collabsphere_first.notification`) acts as a platform-wide consumer and notification aggregator. It never becomes an authoritative owner of source domain entities (messages, posts, connections, communities, or users).
- **PostgreSQL Durability as Source-of-Truth**: The in-app notification center stored in PostgreSQL 16 (`notifications` table via `V26__create_global_notification_schema.sql`) is the definitive source of truth. Notifications survive WebSocket disconnects, Redis outages, mobile offline states, push provider failures, email provider timeouts, and server restarts.
- **Provider & Delivery Decoupling**: External notifications (Push, Email) and realtime pushes are dispatched asynchronously via a durable transactional outbox (`notification_outbox`), guaranteeing that external failures never block or fail the primary user transaction.

### 2. Schema & Entity Model (`V26`)
- **`notifications`**:
  - `id UUID PRIMARY KEY`: High-entropy identifier preventing IDOR enumeration.
  - `recipient_user_id BIGINT NOT NULL` (FK `users(id)`): Strictly owns notification visibility.
  - `actor_user_id BIGINT NULL` (FK `users(id)`): Identity of acting user, if applicable.
  - `type VARCHAR(60) NOT NULL`: Explicit type enum (`NEW_FOLLOW`, `CONNECTION_REQUESTED`, `CONNECTION_ACCEPTED`, `DIRECT_MESSAGE`, `DIRECT_MESSAGE_REACTION`, `COMMUNITY_INVITATION`, `COMMUNITY_POST_ACTIVITY`, `COMMUNITY_EVENT`, `COMMUNITY_MENTION`, `SYSTEM_ALERT`, `SECURITY_ALERT`).
  - `category VARCHAR(40) NOT NULL`: Stable category enum (`SOCIAL`, `MESSAGING`, `COMMUNITY`, `SYSTEM`, `SECURITY`).
  - `source_type VARCHAR(60)` / `source_id VARCHAR(128)`: Domain source entity references.
  - `context_type VARCHAR(60)` / `context_id VARCHAR(128)`: Surrounding contextual entity references (e.g. Conversation, Community).
  - `event_key VARCHAR(200) NOT NULL UNIQUE`: Deterministic deduplication key preventing duplicate notifications across retries.
  - `title_key VARCHAR(120)` / `body_key VARCHAR(120)`: Semantic localization keys for client-side rendering.
  - `payload JSONB NULL`: Minimal, safe rendering metadata. **Never stores private message text, JWTs, passwords, emails, or raw attachment URLs**.
  - `created_at TIMESTAMPTZ NOT NULL`: Keyset pagination tiebreaker.
  - `seen_at TIMESTAMPTZ NULL`: Marks notification display in viewport/badge.
  - `read_at TIMESTAMPTZ NULL`: Marks explicit client user read interaction.
  - `archived_at TIMESTAMPTZ NULL`: Soft-deleted / dismissed state.
- **Indexes**:
  - `idx_notifications_recipient_created`: `(recipient_user_id, created_at DESC, id DESC)` for fast keyset pagination.
  - `idx_notifications_unread`: `(recipient_user_id, created_at DESC) WHERE read_at IS NULL AND archived_at IS NULL` for fast unread count and unread filter.
  - `idx_notifications_category`: `(recipient_user_id, category, created_at DESC)` for category-scoped views.
  - `uq_notifications_event_key`: `UNIQUE (event_key)` for absolute idempotency.

### 3. Event-Key Idempotency
- Notifications are keyed deterministically by event context rather than random UUIDs:
  - Follow: `FOLLOW:{actorId}:{recipientId}`
  - Connection Request: `CONNECTION_REQUEST:{connectionId}:{recipientId}`
  - Connection Accept: `CONNECTION_ACCEPTED:{connectionId}:{recipientId}`
  - Direct Message: `MESSAGE:{messageId}:{recipientId}`
  - Direct Message Reaction: `MESSAGE_REACTION:{messageId}:{actorId}:{reactionType}:{recipientId}`
  - Community Event: `COMMUNITY:{communityEventId}:{recipientId}`
- If a duplicate event is processed (due to retry, outbox replay, or network glitch), the unique constraint ensures the existing notification is returned without creating duplicates.

### 4. Monotonic State Machine
- **State Evaluation**:
  - `UNSEEN`: `seen_at == null && read_at == null && archived_at == null`
  - `SEEN`: `seen_at != null && read_at == null && archived_at == null`
  - `READ`: `read_at != null && archived_at == null`
  - `ARCHIVED`: `archived_at != null`
- **Monotonic Rule**: Timestamps advance strictly forward (`seen_at` is set on first seen, `read_at` automatically implies and updates `seen_at`). Re-reading or re-seeing is idempotent and preserves the earliest interaction timestamp.
- **Bulk Read Semantics**: `POST /api/v1/notification-center/read-all` accepts an optional `readUpToCursor` representing client high-water state, preventing race conditions where newly arrived notifications would be accidentally marked read before the client observed them.

### 5. Keyset Cursor Pagination (`GET /api/v1/notification-center`)
- **Ordering**: Strict descending tuple `(created_at DESC, id DESC)`.
- **Cursor**: URL-safe Base64 JSON token `NotificationCursor { createdAt, id }`.
- **Validation**: Strict parsing prevents SQL injection and malformed cursor manipulation (HTTP 400 `NotificationCursorException`).
- **Filters**: Supports `filter=ALL` or `filter=UNREAD`, and optional `category` scoping.

### 6. User Preferences & Mute Integration
- **`notification_preferences`**: Composite primary key `(user_id, category)` declaring `(in_app_enabled, push_enabled, email_enabled)`.
- **Defaults**: Sensible platform defaults apply when no row exists (`SOCIAL` and `MESSAGING` default to in-app=true, push=true, email=false; `SECURITY` in-app is mandatory).
- **Messaging Mute Semantics**: When a user mutes a direct conversation (`conversation_participants.notifications_muted = true`), global in-app notification persistence is preserved for center history, but external outbox jobs (`PUSH`, `EMAIL`) are completely suppressed.
- **Block Relationship Enforcement**: New notifications are never generated between users where an active network block exists.

### 7. Device Token Registration & Security
- **`notification_devices`**: Tracks user push destinations (`id UUID`, `user_id`, `platform`, `provider`, `token_hash`, `enabled`, `last_seen_at`, `revoked_at`).
- **Token Hash Protection**: Tokens are hashed with SHA-256 for deterministic indexing and deduplication.
- **Atomic Reassignment**: If User B logs into a mobile device previously used by User A (registering the same provider token), the token is atomically revoked from User A and assigned to User B, preventing cross-user notification leakage.
- **IDOR Protection**: Device registration endpoints derive ownership strictly from `SecurityContext`. Users can only manage their own registered devices.

### 8. Durable Outbox & Worker Architecture
- **`notification_outbox`**:
  - Tracks background delivery jobs per external channel (`REALTIME`, `PUSH`, `EMAIL`).
  - Idempotent unique constraint: `UNIQUE (notification_id, channel)`.
  - State machine: `PENDING` -> `CLAIMED` -> `COMPLETED` / `FAILED` / `SKIPPED`.
- **Multi-Instance Worker Safety**: Outbox claiming executes via `SELECT ... FOR UPDATE SKIP LOCKED` with bounded batches, preventing duplicate deliveries across horizontally scaled Spring Boot instances.
- **Exponential Backoff**: Transient errors trigger bounded backoff (`30s`, `2m`, `10m`, `30m`). Permanent failures (e.g. invalid push token, malformed email) fail immediately without wasteful retries.
- **Stale Lock Recovery**: Locks held longer than the lease duration (default 5 minutes) are automatically reclaimed to protect against worker process crashes.
- **Delivery Audit History**: `notification_deliveries` records channel, destination reference, attempts, response status, provider message IDs, and sanitized error codes.

### 9. Realtime WebSocket Delivery (`/ws/notifications`)
- **Transport**: Spring WebSocket + STOMP endpoint `/ws/notifications`.
- **STOMP Authorization Interceptor**: Enforces Bearer JWT on `CONNECT` frames.
- **Subscription Isolation**: Clients are restricted strictly to `/user/queue/notifications`. Direct subscription to `/queue/notifications` or `/topic/notifications` is rejected with `IllegalArgumentException`.
- **Resilience**: If WebSocket delivery fails (e.g. user offline or network dropped), the committed in-app notification remains durable in PostgreSQL and is retrieved on reconnect via REST.

### 10. Channel Provider Implementations
- **Push Provider Abstraction**: `PushNotificationProvider` interface decoupling notification logic from mobile vendor APIs. Gated by `collobster.notifications.push.enabled`. Verified using `MockPushNotificationProvider`.
- **Push Payload Privacy**: Push alerts use safe localized summaries (e.g. *"Alex sent you a message"*) and never expose raw private message contents on lock-screens.
- **Email Provider Adapter**: `EmailNotificationProvider` backed by Spring's existing `JavaMailSender` and thymeleaf template infrastructure.

### 11. Domain Bridge Integration
- **Network Bridge (`NetworkNotificationBridge`)**: Subscribes to `UserFollowedEvent`, `ConnectionRequestedEvent`, `ConnectionAcceptedEvent`.
- **Messaging Bridge (`MessagingNotificationBridge`)**: Subscribes to `DirectMessageCreatedEvent` and `DirectMessageReactionEvent`. Enforces anti-self notification suppression, privacy protection, and conversation mute rules.
- **Community Bridge (`CommunityNotificationBridge`)**: Subscribes to `CommunityNotificationSavedEvent` using event key format `COMMUNITY:{id}:{userId}`, preventing duplicate notifications across retries while keeping existing Community notification infrastructure intact.

---

## 26. Notification Durability, Deduplication Identity & Compatibility Hardening (Phase 7.1)

### 1. Source Event Durability Boundary (Option B)
- **Same-Transaction Atomicity**: In-app notifications and their eligible delivery outbox jobs are created in the **exact same database transaction** as the primary source domain mutation.
  - **Network Follows**: `NetworkServiceImpl.followUser()` saves `UserFollow` entity, publishes `UserFollowedEvent(actorId, targetUserId, followId)`. The event listener `NetworkNotificationBridge.onUserFollowed()` runs synchronously within the active transaction, inserting `Notification` and `NotificationOutbox` rows. All rows commit atomically.
  - **Network Connections**: `NetworkServiceImpl.sendConnectionRequest()` and `acceptConnectionRequest()` publish `ConnectionRequestedEvent` / `ConnectionAcceptedEvent` synchronously within their `@Transactional` boundaries, persisting `Notification` and `NotificationOutbox` rows before commit.
  - **Direct Messages**: `IdempotentMessagePersister.persistMessage()` publishes `DirectMessageCreatedEvent` inside its `@Transactional` method. `MessagingNotificationBridge.onDirectMessageCreated()` executes synchronously, persisting `Notification` and `NotificationOutbox` rows before the message transaction commits.
  - **Crash Resilience**: If the JVM process crashes immediately after commit, the source domain row, the global notification row, and the notification outbox rows are already safely persisted in PostgreSQL. Upon application restart, the outbox worker claims the pending outbox jobs and delivers them. No notification is ever lost.
  - **Flyway V27 Decision**: Because existing V26 schema (`notifications`, `notification_outbox`, `notification_deliveries`, `notification_devices`, `notification_preferences`) fully supports Option B atomicity and occurrence keys, no V27 migration was needed.

### 2. Event Occurrence Identity Invariants
- **Global Event-Key Rule**: `eventKey` identifies **one source event occurrence for one recipient**, not an eternal actor/recipient relationship.
- **Occurrence Identity Mapping**:
  - `NEW_FOLLOW`: `FOLLOW:{followId}:{recipientId}`. Uses authoritative `UserFollow.id` UUID. If User A unfollows User B and follows User B again, the new follow row generates a fresh UUID, producing a distinct event key (`FOLLOW:uuid2:B`) and distinct notification. Retrying the same follow event remains strictly idempotent.
  - `CONNECTION_REQUEST`: `CONNECTION_REQUEST:{requestId}:{recipientId}`. Uses authoritative `ConnectionRequest.id` UUID representing that specific request lifecycle.
  - `CONNECTION_ACCEPTED`: `CONNECTION_ACCEPTED:{eventId}:{recipientId}`. Uses unique acceptance occurrence event UUID.
  - `DIRECT_MESSAGE`: `MESSAGE:{messageId}:{recipientId}`. Message ID is an immutable occurrence identifier.
  - `DIRECT_MESSAGE_REACTION`: `MESSAGE_REACTION:{reactionEventId}:{recipientId}`. Uses unique reaction event UUID, ensuring reaction changes (e.g. 👍 -> ❤️ -> 👍) trigger distinct legitimate notifications rather than being permanently suppressed by current-state equality.
  - `COMMUNITY`: `COMMUNITY:{communityNotificationId}:{recipientId}`. Uses unique community notification occurrence ID.

### 3. Read-All High-Water Tuple Semantics
- **The Problem with Timestamps Alone**: Notification keyset pagination orders by `(created_at DESC, id DESC)`. If multiple notifications share the exact same timestamp $T$, a simple `createdAt <= T` bulk read updates unobserved notifications that arrived at $T$ but were excluded from the client's current page.
- **Canonical High-Water Tuple**: Bulk read-all accepts an opaque base64 cursor (`readUpToCursor`) or fallback timestamp. The server decodes `(upToCreatedAt, upToId)` and executes a single efficient bulk SQL update:
  ```sql
  UPDATE Notification n
  SET n.readAt = :now, n.seenAt = COALESCE(n.seenAt, :now)
  WHERE n.recipientUserId = :userId
    AND n.readAt IS NULL
    AND n.archivedAt IS NULL
    AND ((n.createdAt < :upToCreatedAt) OR (n.createdAt = :upToCreatedAt AND n.id <= :upToId))
  ```
- **Concurrency & Order Safety**: Notifications at timestamp $T$ with `id > upToId` remain strictly unread until observed. Newly arriving notifications after page fetch remain unread.

### 4. Community Notification Backward Compatibility & Global API Contract Lock (Phase 7.2)
- **Architectural Priority**: Older established APIs win backward-compatibility priority over newly introduced routes. Community notifications existed before Phase 7; therefore, historical Community endpoints are preserved with their exact legacy schemas and semantics.
- **REST Namespace Separation**:
  - **Canonical Global Notification Center (`/api/v1/notification-center`)**:
    - `GET /api/v1/notification-center`: Keyset-paginated global notifications (`NotificationPageResponse`).
    - `GET /api/v1/notification-center/{id}`: Single global notification details (`NotificationResponse`).
    - `POST /api/v1/notification-center/{id}/seen`: Marks notification as seen.
    - `POST /api/v1/notification-center/{id}/read`: Marks notification as read.
    - `POST /api/v1/notification-center/{id}/archive`: Soft-archives notification.
    - `POST /api/v1/notification-center/read-all`: Bulk read-all up to keyset tuple cursor (`MarkAllReadRequest`).
    - `GET /api/v1/notification-center/unread-count`: Returns `UnreadCountResponse` (`unreadCount`).
    - `GET /api/v1/notification-center/summary`: Alias returning `UnreadCountResponse`.
    - `/api/v1/notification-center/preferences`: User channel preferences (`GET`, `PATCH`).
    - `/api/v1/notification-center/devices`: User push devices (`POST`, `GET`, `DELETE /{deviceId}`).
  - **Canonical Community Notifications (`/api/v1/communities/notifications`)**:
    - `GET /api/v1/communities/notifications`: Pageable list of `CommunityNotificationDto`.
    - `GET /api/v1/communities/notifications/unread-count`: Returns `ApiResponse<Long>`.
    - `PATCH /api/v1/communities/notifications/{id}/read`: Marks single community notification read.
    - `PATCH /api/v1/communities/notifications/read-all`: Bulk marks all community notifications read.
  - **Restored Historical Legacy Community Routes (`/api/v1/notifications`)**:
    - `GET /api/v1/notifications`: Restored to return `Page<CommunityNotificationDto>` (deprecated but supported).
    - `GET /api/v1/notifications/unread-count`: Restored to return `ApiResponse<Long>` (deprecated but supported).
    - `PATCH /api/v1/notifications/{id}/read`: Legacy alias marking community notification read.
    - `PATCH /api/v1/notifications/read-all`: Legacy alias marking all community notifications read.
- **Strict Domain Independence**:
  - Unread counts are separate: Community unread returns caller's unread community events (`Long`); Global unread returns platform notifications (`UnreadCountResponse`).
  - Read states are separate: Marking a Global notification read does not alter underlying `CommunityNotification.read_at`, and vice-versa.
  - Zero Spring request mapping ambiguity: `CommunityNotificationController` and `NotificationController` have completely non-overlapping URI mappings.

### 5. Push Token Storage & Security Model
- **Device Token Storage**: `notification_devices` stores `token_hash` (SHA-256 for deterministic indexing and deduplication) and `token` (raw provider token required by FCM / APNs).
- **Security Boundaries**:
  - Raw device tokens are **never logged** (`toString()` and log messages only print device ID and token hash).
  - Raw device tokens are **never returned** in user-facing DTOs (`DeviceResponse` returns `tokenHash` and masked metadata).
  - Raw device tokens are accessed strictly by the background delivery subsystem (`NotificationOutboxWorker` -> `PushNotificationProvider`).
  - Database encryption at rest is enforced by infrastructure storage layers; no weak homegrown crypto is introduced.
- **Concurrent Reassignment**: `registerDevice()` enforces atomic single-ownership. If User B registers a device token previously registered to User A, the existing record is revoked/reassigned. Concurrent races catching `DataIntegrityViolationException` recover safely, guaranteeing exactly one active user owns the token.

### 6. Multi-Device Push Fanout & Delivery Auditing
- **Fanout Invariant**: `notification_outbox` enforces `UNIQUE(notification_id, channel)`. For a recipient with multiple active devices (e.g. phone and tablet), a single `PUSH` outbox job fans out to all active devices.
- **Per-Device Delivery Auditing**: `notification_deliveries` records individual delivery attempts for every targeted device.
- **Fault Isolation**: If Device A is valid and Device B has an expired or revoked token (provider returns `UNREGISTERED`), Device B is marked `revoked_at = now`, Device A succeeds, and the PUSH outbox job completes as `SENT`. Revoked tokens are never retried.

### 7. Centralized Preference & Mute Precedence
- **Precedence Hierarchy**: Evaluated centrally in `NotificationPreferenceServiceImpl.determineEligibleChannels(recipientUserId, category, conversationMuted)`:
  1. **Block Relationship**: Source action rejected at domain service; no notification created.
  2. **Conversation Muted**: In-app notification center history is created (`Notification` row persisted), but external delivery channels (`PUSH`, `EMAIL`) and `REALTIME` WebSocket push are **completely suppressed** (`Set.of()`).
  3. **Global Category Preference**: If `push_enabled = false` in `notification_preferences`, PUSH channel is omitted.
  4. **Provider Master Switch**: If `collobster.notifications.push.enabled = false`, PUSH channel is omitted.
  5. **Device Availability**: If no active devices exist, PUSH outbox job is marked `SKIPPED (NO_ACTIVE_DEVICES)` without error.

### 8. Realtime Mute Semantics
- **Explicit Invariant**: For muted direct conversations, `REALTIME` WebSocket notification to `/user/queue/notifications` is **suppressed**. When a user explicitly mutes a conversation, they expect complete silence across all push and realtime alert channels while they have the application open. The notification remains accessible in their notification center via REST.

### 9. Outbox Claiming, Worker Leases & DB Lock Isolation
- **Short Claim Transactions**: Outbox worker executes `findAndClaimPendingJobs()` with `SELECT ... FOR UPDATE SKIP LOCKED` inside a short transaction, updates status to `IN_PROGRESS`, records `locked_at` and `lock_owner = workerNodeId`, and commits immediately.
- **No DB Locks During External Calls**: Database locks are **never held** across network I/O calls to FCM, APNs, SMTP, or WebSocket brokers.
- **Stale Worker Protection**: Completion callbacks (`handleJobSuccess`, `handleJobRetry`, etc.) verify `lock_owner`. If Worker A stalls, its lease expires, and Worker B reclaims and completes the job, Worker A waking up cannot overwrite Worker B's result.

### 10. Notification + Outbox Atomicity & Retention Safety
- **Atomic Creation**: Creating a global notification and its eligible channel outbox rows commits in a single transaction. A notification cannot exist without its delivery jobs.
- **Retention Worker Safety**: `NotificationRetentionWorker` query is hardened with `AND NOT EXISTS (SELECT 1 FROM NotificationOutbox o WHERE o.notificationId = n.id AND (o.status = 'PENDING' OR o.status = 'IN_PROGRESS'))`, preventing archival or deletion while delivery attempts are active.

---

## 20. Community ↔ Global Platform Integration & Interaction Ownership (Phase 8)

### 1. Architectural Invariant: Strict Domain Boundaries
- **Community Post Integrity**: The Community module remains an autonomous bounded domain. The `posts` table requires a strictly non-null `community_id`.
- **No Universal Post Entity**: Platform posts (`platform_posts`) and Community posts (`posts`) remain distinct entities in distinct database tables. No universal table, nullable `community_id`, or JPA inheritance hierarchy is permitted.
- **Polymorphic Content Identity**: Cross-surface aggregation is performed strictly via the value type:
  ```java
  public record ContentRef(
      ContentType type,
      UUID id
  ) {}
  ```
  Supported post types: `PLATFORM_POST` and `COMMUNITY_POST`.

### 2. Single Source of Truth for Interactions
Every user action maps to exactly one authoritative source of truth. Double-counting or competing tables are strictly forbidden:
- **Likes**: Authoritative source is `content_likes` (PostgreSQL) keyed by `(user_id, content_type, content_id)`. There is no separate post reaction table in Community (the only community reactions exist on channel chat messages).
- **Saves**: Authoritative source is `content_saves` (PostgreSQL) keyed by `(user_id, content_type, content_id)`.
- **Events**: Authoritative source is `content_interaction_events` for `VIEW` (deduplicated sliding 60s window), `SHARE`, `WATCH_TIME`, `HIDE`, and `NOT_INTERESTED`.
- **Comments**: Authoritative source is the Community domain `comments` table. Feed and Explore read the authoritative count without duplicating storage globally.

### 3. ContentInteractionAdapter SPI & Registry
Phase 8 introduces a polymorphic dispatch layer decoupling controllers from storage details:
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
- `PlatformPostInteractionAdapter`: Operates on `PLATFORM_POST`.
- `CommunityPostInteractionAdapter`: Operates on `COMMUNITY_POST`, validating community access via `CommunityContentAccessService`.
- `ContentInteractionAdapterRegistry`: Dispatches operations in $O(1)$ time based on `ContentRef.type()`.

### 4. API Route Convergence
Existing mobile/web clients may interact via either the Global Interaction API or the Community Native Post API:
- `PUT /api/v1/interactions/{contentType}/{contentId}/like` $\leftrightarrow$ `PUT /api/v1/posts/{id}/like`
- `PUT /api/v1/interactions/{contentType}/{contentId}/save` $\leftrightarrow$ `PUT /api/v1/posts/{id}/save`
Both routes converge onto the identical transactional service and mutate the same underlying row in `content_likes` / `content_saves`.

### 5. Community Content Access Facade (`CommunityContentAccessService`)
A unified read facade centralizes authorization and retrieval:
- `canViewPost(Long userId, UUID postId)`: Enforces active community status, post visibility, and membership checks for restricted/private communities.
- `getVisiblePosts(Long userId, Collection<UUID> postIds)`: Bounded batch query returning accessible posts.
- `getAuthoritativeReactionSummary(Long userId, Collection<UUID> postIds)`: Bounded batch query returning `InteractionSummary` with authoritative `likeCount`, `liked`, and `saved` states.

### 6. Cross-Surface Collision & Block Isolation
- **Collision Safety**: A `PLATFORM_POST` and `COMMUNITY_POST` sharing the same UUID are completely isolated by composite primary keys `(user_id, content_type, content_id)` in `content_likes` and `content_saves`.
- **Social Block Semantics**: When User A blocks User B, posts authored by B are stripped from A's passive Feed, Explore, and Notifications. Social blocking does **not** alter Community membership or community roles.
- **Schema Stability**: Schema remains at version `V26`. No migration is needed because duplicate storage was avoided by design.

---

## 21. Recommendation Feedback Subsystem & Asynchronous Pipeline (Phase 9)

### 1. Architectural Mission & Invariants
- **No Synchronous Dual Writes**: User interaction endpoints (`/api/v1/interactions/...` and `/api/v1/posts/...`) never call the Python recommendation service synchronously. Feedback events are committed atomically to PostgreSQL within the primary business transaction.
- **No Mobile Access to Python**: Mobile and web clients are forbidden from invoking the Python recommendation service directly. All actions flow through authoritative Spring Boot services.
- **PostgreSQL Transactional Outbox**: The authoritative Java backend writes durable feedback intent into `recommendation_feedback_outbox` (`V27`).
- **Idempotence & At-Least-Once Delivery**: Events carry client/backend generated UUIDv4 `eventId` values. Python persists events in `recommendation_events` table using `ON CONFLICT (event_id) DO NOTHING`.
- **Network / Outage Tolerance**: If the Python recommendation service is degraded or offline, user interactions proceed without disruption or latency; events accumulate safely in the PostgreSQL outbox and are dispatched with exponential backoff upon service restoration.

### 2. Database Schemas
#### Java PostgreSQL Outbox (`V27__create_recommendation_feedback_outbox.sql`)
```sql
CREATE TABLE IF NOT EXISTS recommendation_feedback_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    contract_version VARCHAR(16) NOT NULL DEFAULT '1',
    event_type VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    content_type VARCHAR(64),
    content_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    last_error_code VARCHAR(64),
    last_error_message VARCHAR(1000),
    locked_at TIMESTAMPTZ,
    lock_owner VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rec_feedback_outbox_event_id UNIQUE (event_id)
);

CREATE INDEX idx_rec_fb_outbox_claimable ON recommendation_feedback_outbox(status, next_attempt_at)
    WHERE status IN ('PENDING', 'RETRY');
CREATE INDEX idx_rec_fb_outbox_in_progress ON recommendation_feedback_outbox(status, locked_at)
    WHERE status = 'PROCESSING';
CREATE INDEX idx_rec_fb_outbox_retention ON recommendation_feedback_outbox(status, updated_at)
    WHERE status = 'SENT';
CREATE INDEX idx_rec_fb_outbox_user_time ON recommendation_feedback_outbox(user_id, occurred_at DESC);
```

#### Python PostgreSQL Schema (`recommendation_events`)
```sql
CREATE TABLE IF NOT EXISTS recommendation_events (
    id SERIAL PRIMARY KEY,
    event_id UUID UNIQUE NOT NULL,
    contract_version VARCHAR(16) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    content_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    surface VARCHAR(64),
    session_id VARCHAR(128),
    position INT,
    recommendation_score DOUBLE PRECISION,
    duration_ms BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rec_events_user_occurred ON recommendation_events(user_id, occurred_at);
CREATE INDEX idx_rec_events_content_occurred ON recommendation_events(content_type, content_id, occurred_at);
CREATE INDEX idx_rec_events_type_occurred ON recommendation_events(event_type, occurred_at);
```

### 3. Outbox Processing, Claiming & Lock Lease Lifecycle
1. **Atomic Enqueue**: Interaction adapters and services invoke `RecommendationFeedbackOutboxService.recordFeedback(...)` inside active transactions.
2. **Disjoint Worker Claiming**: Worker nodes invoke `claimJobs(workerNodeId, batchSize)`:
   - Claims pending records using `SELECT ... FOR UPDATE SKIP LOCKED` inside a short transaction.
   - Sets `status = 'IN_PROGRESS'`, `lock_owner = workerNodeId`, `locked_at = NOW()`.
   - Commits transaction immediately, releasing DB row locks before initiating HTTP transmission.
3. **HTTP Dispatch Outside DB Locks**: The worker sends `POST /api/v1/internal/recommend/feedback/batch` with Bearer auth to the Python service.
4. **Status Transitions & Poison-Pill Quarantine**:
   - `ACCEPTED`: Transitions to `DELIVERED`.
   - `DUPLICATE`: Transitions to `DELIVERED` idempotently.
   - `REJECTED` (client error / malformed): Transitions immediately to `DEAD_LETTER` with `last_error_code` and `last_error_message`, preventing retry loops.
   - Network failure / 5xx: Transitions to `RETRY` with exponential backoff ($2^{\text{attempt}} \times 2\text{s}$) up to `maxAttempts` (5), after which it transitions to `DEAD_LETTER`.
5. **Stale Lock Recovery & Fencing**:
   - Orphaned jobs locked for longer than `lockLeaseSeconds` (60s) automatically become eligible for reclaiming by surviving workers.
   - Stale worker completion callbacks verify that `lock_owner` matches their own node ID before applying updates, fencing out zombies.
6. **Retention Cleanup**:
   - `RecommendationFeedbackRetentionWorker` runs daily, deleting `DELIVERED` outbox records older than `retentionDays` (7 days), while preserving `DEAD_LETTER` for diagnostic audit.

---

## 21. Phase 9.1 Live Feedback Pipeline & Recovery Verification

Phase 9.1 provides comprehensive live end-to-end integration and resilience verification between the Spring Boot backend (`collabster-communities-module`) and the running FastAPI recommendation service (`Recommendation-System`):

1. **Live Integration Profile (`-Ppython-feedback-integration`)**:
   - Dedicated Maven profile executing `LivePythonFeedbackIntegrationTest` against real running services without mocked HTTP transport.
   - Standard unit suite (`mvn test`) runs completely isolated and independent of Python uptime.
2. **Database Ownership & Table Boundary**:
   - Python owns `recommendation_events` table in PostgreSQL.
   - Java application code (`src/main`) contains 0 references or SQL queries for `recommendation_events`.
   - Python recommendation service never writes or mutates Java authoritative application tables (`content_likes`, `content_saves`, `content_interaction_events`, `platform_posts`, `posts`, `users`).
3. **Fault Tolerance & Zero-Degradation Uptime**:
   - When Python is offline, authoritative Java interactions (likes, saves, views, hides) continue without user-facing errors or added latency.
   - Outbox records safely buffer with status `RETRY` and exponential backoff.
   - Upon service restart, workers automatically recover pending events without manual replay.
4. **Chronology & Poison-Pill Isolation**:
   - Out-of-order deliveries retain original occurrence timestamps (`occurred_at`) without timestamp rewriting.
   - Ingestion timestamp (`ingested_at`) is recorded separately from occurrence time.
   - Single invalid events within a batch (e.g. invalid duration) are quarantined to `DEAD_LETTER` without failing valid neighboring events in the same batch.
5. **Model Artifact Stability**:
   - Ranking model artifact canonical path verified: `artifacts/lgbm_ranker_model_v2.txt` (SHA256: `25944E97AFE3194DF82AD2EAE9FF738DEF2456D1EEC929B267BF663D259C5B0B`).
   - Zero model retraining or ranking semantic modifications.

---

## 22. Phase 9.2 Secret Hygiene, Health Separation & Feedback Operational Hardening

Phase 9.2 implements operational hardening, secret hygiene, and service health separation across the Collobster Java and Python architectures:

### 1. Internal Credential Hygiene & Unification
- **Single Source of Truth**: The internal service-to-service Bearer authentication token is unified under the `COLLOBSTER_RECOMMENDATION_INTERNAL_TOKEN` environment variable across both Java (`collabster-communities-module`) and Python (`Recommendation-System`).
- **No Hardcoded Production Secrets**: Removed all hardcoded token fallbacks (`collobster-internal-secret-token`) from Java production classes (`RecommendationProperties`, `RecommendationFeedbackProperties`) and configuration (`application.properties`).
- **Python Fast-Failure**: In production, Python `get_internal_token()` requires environment configuration and fails fast with a `RuntimeError` rather than falling back to an insecure default.
- **Constant-Time Comparison**: Python continues to enforce constant-time digest comparison (`secrets.compare_digest`) to prevent timing side-channel attacks.
- **Credential Rotation**: The previously exposed development credential was rotated. Test suites obtain dedicated, explicitly designated non-production test fixtures (`test-fixture-internal-token-not-production`).

### 2. Environment File & Docker Hygiene
- **Git Tracking Exclusions**: `.gitignore` files at the repository root, Java module, and Python module enforce `.env` and `.env.*` exclusions while explicitly tracking `.env.example`.
- **Safe Template (`.env.example`)**: Documented configuration template containing placeholders only (`change-me-internal-token`, dummy database URLs), preventing accidental secret commits.
- **Docker Build Context**: `.dockerignore` excludes `.env` and `.env.*`, ensuring images do not bundle local development secrets.

### 3. Service Health Separation (Liveness vs. Readiness)
To prevent accidental availability coupling where a feedback database loss causes orchestrators/load balancers to eject healthy ranking inference, health semantics are decoupled into three distinct tiers:

1. **Liveness (`GET /health/live`, alias `/healthz`)**:
   - Evaluates process vitality only.
   - Zero external dependency calls (no PostgreSQL, no Redis, no disk inference).
   - Returns `200 OK` (`{"status": "ok", "service": "collobster-recommendation", "liveness": "alive"}`) unless the process runtime is critically broken.

2. **Ranking Readiness (`GET /health/ready`, alias `/readyz`)**:
   - Evaluates whether the service can safely serve candidate scoring requests for `POST /api/v1/internal/recommend/feed/rank`.
   - Validates LightGBM model readiness or deterministic `WeightedSum` fallback availability.
   - **Crucially decoupled from PostgreSQL**: An outage in feedback event persistence **never** makes ranking unready. Returns `200 OK` if ranking inference is operational.

3. **Feedback Ingestion Readiness (`GET /health/feedback`)**:
   - Evaluates feedback persistence availability via a bounded `SELECT 1` ping (1.5s timeout).
   - Returns `200 OK` when PostgreSQL is reachable.
   - Returns `503 Service Unavailable` (`{"status": "degraded", ...}`) when feedback storage is unreachable.
   - **Not used** as the primary load-balancer probe for Feed ranking traffic.

4. **Detailed Component Health (`GET /health`)**:
   - Reports full component health matrix (`ranking`, `feedbackIngestion`, `modelLoaded`, `modelVersion`, `strategy`).
   - Short-lived caching (2.0s TTL) protects PostgreSQL from ping exhaustion during high-frequency health polling.
   - **Availability Invariant**: If ranking is ready but feedback PostgreSQL is down, `/health` returns **HTTP 200 OK** with top-level `status: "degraded"`. HTTP 503 is returned only if ranking inference itself fails (`status: "unhealthy"`).

### 4. Zero Event Loss During Feedback Storage Outage
- When Python's feedback database is down, batch ingestion attempts to `POST /api/v1/internal/recommend/feedback/batch` catch `OperationalError`/`DatabaseError` and raise **HTTP 503 Service Unavailable** (`{"detail": "Feedback storage is temporarily unavailable"}`).
- Python never returns HTTP 200 with `REJECTED` status for connectivity failures, preventing Java from falsely classifying transient outages as permanent poison pills (`DEAD_LETTER`).
- Java's `RecommendationFeedbackWorker` detects HTTP 503, applies exponential backoff, and leaves outbox events in `RETRY` status.
- Upon database restoration, the Python health probe automatically recovers to `ready` without a process restart, and the Java worker replays the retrying events, persisting each event idempotently (`DELIVERED`).

### 5. Outbox Worker Observability & Payload Privacy
- **Metrics Exposure**: `FeedbackOutboxService.getMetrics()` provides high-level operational visibility:
  - `pendingCount`, `inProgressCount`, `retryCount`, `deliveredCount`, `deadLetterCount`.
  - `oldestPendingCreatedAt` and calculated `oldestPendingAgeSeconds`.
- **Safe Dead-Letter Audit**: `FeedbackOutboxService.getDeadLetterSummaries(limit)` returns sanitized diagnostic audit records (`SafeDeadLetterSummary`) containing:
  - `eventId`, `eventType`, `sanitizedErrorCode`, `attemptCount`, `occurredAt`.
  - Explicitly omits sensitive interaction payloads and user context, preserving privacy.

### 6. Architectural Invariants Verified
- **LightGBM Model Integrity**: Checksum SHA-256 for `artifacts/lgbm_ranker_model_v2.txt` verified unchanged: `25944E97AFE3194DF82AD2EAE9FF738DEF2456D1EEC929B267BF663D259C5B0B`.
- **Contract Invariance**: Ranking feature contract (`RECOMMENDATION_FEATURE_CONTRACT_V1`, version `1`) and feedback contract (`RECOMMENDATION_FEEDBACK_CONTRACT_V1`, version `1`) remain unchanged.
- **Schema Invariance**: Flyway migration version remains at `V27`. No Flyway V28 created during Phase 9.

---

## 23. Platform-Wide Security, Privacy, Abuse & Account Lifecycle Hardening (Phase 10)

Phase 10 establishes a unified, platform-wide security, authorization, privacy, abuse prevention, and account lifecycle architecture across all Collobster subsystems (Auth, User/Profile, PlatformPost, Community, Messaging, Notifications, Interaction, and Feed/Explore), without compromising any bounded-domain invariants or ML ranking contracts.

### 1. Security Filter Chain & Entrypoint Hardening
- **Authentication Entrypoint**: Reconfigured Spring Security to attach an explicit `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)` mapped to `/api/**`. Unauthenticated REST and WebSocket API requests return `401 Unauthorized` JSON responses immediately, preventing unwanted `302 Found` OAuth2 browser login redirects.
- **Security Headers**: Enforced platform-wide HTTP response security headers via `SecurityConfig`:
  - `X-Frame-Options: DENY` (clickjacking mitigation)
  - `X-Content-Type-Options: nosniff` (MIME sniffing prevention)
  - `Referrer-Policy: strict-origin-when-cross-origin`
- **CORS Architecture**: Replaced open wildcard configurations with environment-configurable trusted origins (`corsConfigurationSource()`) supporting safe credentials handling across authorized frontends.
- **Sanitized Global Exception Handling**: `GlobalPlatformSecurityExceptionHandler` catches and sanitizes all uncaught security, validation, rate-limiting, and internal exceptions across the platform, returning structured error payloads (`status`, `errorCode`, `message`, `timestamp`) with zero stack traces, SQL syntax, or internal class disclosures.

### 2. Token Hardening & Lifecycle Verification
- **Explicit Token Typing**: `JwtService` issues and validates explicit `token_type` claims (`ACCESS` vs `REFRESH`).
  - Access tokens used at `/api/v1/auth/refresh` are rejected with `401 Unauthorized`.
  - Refresh tokens presented in the `Authorization: Bearer` header on protected API endpoints are rejected by `JwtService.isTokenValid()`.
- **Account State Verification on Token Ingestion**: `JwtService.isTokenValid()` and `AuthenticationServiceImpl.refreshToken()` verify `UserDetails.isEnabled()`, rejecting tokens issued to deactivated, disabled, or soft-deleted accounts immediately without waiting for token expiry.

### 3. Segregation of Platform Administration vs Community Moderation
- **Strict Role Boundaries**: Platform administration (`ROLE_ADMIN`) and Community moderation roles (`COMMUNITY:OWNER`, `COMMUNITY:ADMIN`, `COMMUNITY:MODERATOR`, `COMMUNITY:MEMBER`) are strictly segregated.
  - A Community administrator has zero administrative authority over Platform Posts, Global Reports, or User Profiles.
  - Community roles are scoped strictly to specific `communityId` path parameters and evaluated via `CommunityAuthorizationService`.
- **Privilege Escalation Defense**: `ProfileServiceImpl.createProfile()` and `updateProfile()` enforce server-side validation rejecting any attempt by non-admins or callers to assign `ROLE_ADMIN` to themselves.

### 4. Cross-Domain Block Authority & Semantics
- **Authoritative Block Service**: `NetworkAccessService.isBlockedEitherDirection(userIdA, userIdB)` serves as the single source of truth for interpersonal block relationships.
- **Cross-Domain Block Invariants**:
  - **Social Follows & Connections**: Blocked users cannot follow or send connection requests in either direction (`403 Forbidden`).
  - **Direct Messaging**: New DM conversations cannot be initiated between blocked users; existing conversations are placed in read-only mode (new messages, reactions, and typing indicators are rejected).
  - **Passive Surfaces (Feed & Explore)**: Content authored by blocked users is stripped during retrieval and pre-hydration.
  - **Direct Platform Post Retrieval**: `PlatformPostServiceImpl.getPostById()` evaluates `isBlockedEitherDirection` and denies access (`404 Not Found` anti-enumeration) if a block exists between caller and author.
  - **Community Independence**: Social blocks do not alter Community membership or community channel structures, preserving independent community governance.

### 5. Distributed Abuse Prevention & Rate Limiting
- **Dual-Tier Rate Limiter (`PlatformRateLimiter`)**:
  - **Distributed Redis Tier**: Executes atomic Redis Lua token-bucket scripts (`rate_limit.lua`) tracking caller IP and user keys with configurable window TTLs.
  - **In-Memory Resilience Fallback**: If Redis is offline or partitioned, automatically degrades to in-memory `Bucket4j` token buckets, guaranteeing that rate-limit protection remains active per-instance without failing legitimate traffic.
- **Rate-Limited Actions**:
  - Authentication (login, registration, OTP, password reset)
  - Content Creation (platform posts, community posts, comments)
  - Interpersonal Interactions (follows, connection requests, reports)
  - Search queries and high-frequency discovery
- **Standardized Response**: Blocked requests throw `PlatformRateLimitExceededException` handled by `GlobalPlatformSecurityExceptionHandler`, returning HTTP `429 Too Many Requests` with a compliant `Retry-After: {seconds}` header.

### 6. Global Platform Reporting & Moderation Domain
- **Segregated Report Domain**: Introduced `com.collabsphere_first.report` providing platform-wide reporting without colliding with or altering mature Community report workflows (`ReportController` for communities).
- **Schema & Persistence (`platform_reports`)**:
  - Columns: `id` (UUID), `reporter_user_id` (BIGINT), `target_type` (`USER`, `PLATFORM_POST`, `COMMUNITY_POST`, `DIRECT_MESSAGE`), `target_id` (VARCHAR), `reason` (`SPAM`, `HARASSMENT`, `HATE_OR_ABUSE`, `IMPERSONATION`, `SCAM_OR_FRAUD`, `SEXUAL_CONTENT`, `VIOLENCE_OR_THREAT`, `PRIVACY`, `OTHER`), `details`, `status` (`PENDING`, `IN_REVIEW`, `RESOLVED`, `DISMISSED`), `resolution_notes`, `resolved_by`, `resolved_at`, `created_at`, `updated_at`.
- **Abuse Prevention**: `PlatformReportServiceImpl.createReport()` validates target existence and enforces that a reporter cannot file duplicate pending reports against the same target.
- **Reporting REST Endpoints**:
  - `POST /api/v1/platform/reports`: User report submission (actor identity derived exclusively from `SecurityContext`).
  - `GET /api/v1/admin/reports`: Admin report queue filtered by status and target type (`ROLE_ADMIN` only).
  - `PATCH /api/v1/admin/reports/{reportId}/resolve`: Admin report resolution (`ROLE_ADMIN` only), supporting actions `DISMISS_REPORT`, `REMOVE_PLATFORM_POST`, `SUSPEND_ACCOUNT`, `RESTRICT_ACCOUNT`.

### 7. Immutable Privileged Action Audit Logging
- **Audit Domain (`platform_audit_logs`)**: Records administrative interventions and high-impact lifecycle actions.
  - Columns: `id` (UUID), `admin_user_id` (BIGINT), `action` (VARCHAR), `target_type` (VARCHAR), `target_id` (VARCHAR), `details` (TEXT), `created_at` (TIMESTAMPTZ).
- **Audit Integrity**: Append-only log. No update or delete endpoints exist; records are immutable.
- **Logged Events**: Report resolutions, post takedowns, account deactivations, account suspensions, and administrative reactivations.

### 8. Account Lifecycle & Data Erasure Hardening
- **Authoritative State Model**: `User` entity hardened with `deactivated` (BOOLEAN), `deleted` (BOOLEAN), `deactivated_at` (TIMESTAMPTZ), `deleted_at` (TIMESTAMPTZ), indexed via `idx_users_lifecycle_status`.
- **Account Deactivation**:
  - Marks `deactivated = true` and `accountEnabled = false`.
  - Immediately revokes all active push notification device tokens (`NotificationDeviceService.revokeAllForUser()`).
  - Evicts user from Redis messaging presence and session ZSETs (`MessagingPresenceService.forceOffline()`).
  - `UserPrincipal.isEnabled()` returns false, terminating existing sessions and rejecting refresh attempts.
  - User profile and posts are excluded from passive discovery and feed ranking.
- **Account Deletion (PII Scrubbing)**:
  - Marks `deleted = true`, `deactivated = true`, `accountEnabled = false`.
  - Scrubs PII from `User` and `Profile` (anonymizing email to `deleted_{uuid}@collobster.local`, username to `deleted_{uuid}`, clearing phone, bio, avatar, and social links).
  - Conversation Integrity: Direct messages sent by deleted users are preserved for conversation counterparties, but sender identity is anonymized to *"Deleted User"* with zero personal profile disclosure.
- **Recommendation Data Scrubbing**:
  - Recommendation outbox feedback rows linked to deleted user IDs are scrubbed or deleted.
  - Python ranking candidate hydration in Java automatically drops any content authored by deactivated or deleted accounts.

### 9. Database Evolution (Flyway V28)
- **Migration Script**: `V28__platform_security_privacy_reporting.sql`:
  - Adds lifecycle flags and timestamps to `users` with index `idx_users_lifecycle_status`.
  - Creates `platform_reports` table with compound indexes on `(status, created_at)` and `(target_type, target_id)`.
  - Creates `platform_audit_logs` table with compound indexes on `(admin_user_id, created_at)` and `(target_type, target_id)`.
- **Schema Validation**: Verified with PostgreSQL 16 on port 5433 using `spring.jpa.hibernate.ddl-auto=validate`.

### 10. Architectural Invariants Verified
- **LightGBM Model Checksum**: Canonical ranking model `Recommendation-System/Recommendation-System/artifacts/lgbm_ranker_model_v2.txt` verified unchanged (SHA-256: `25944E97AFE3194DF82AD2EAE9FF738DEF2456D1EEC929B267BF663D259C5B0B`). Zero retraining performed.
- **PostgreSQL vs Redis Invariant**: PostgreSQL remains the sole authoritative datastore for all accounts, posts, interactions, reports, and audit logs. Redis is strictly limited to ephemeral token-bucket rate limiting and messaging presence.
- **Domain Independence**: `PlatformPost` != `CommunityPost`, Community Channels != Direct Messages, Feed != Explore.

---

## 27. Cross-System Privacy Erasure, Multi-Device Revocation, WebSocket Expiry & Trusted-Proxy Hardening (Phase 10.1)

### 1. Cross-System User Erasure Outbox Architecture
- **Durable Asynchronous Outbox**: Account deletion in Spring Boot (`AccountLifecycleServiceImpl.deleteAccount()`) transactionally scrubs PII locally and enqueues a durable job in `user_data_erasure_outbox` (Flyway V29).
- **Decoupled Failure & Resilience**: The Java account deletion transaction completes immediately without waiting synchronously on Python. If the Python recommendation microservice is unreachable, partitioned, or recovering, the outbox worker (`UserDataErasureWorker`) retries with exponential backoff (up to 5 attempts) without rolling back the local deletion.
- **Python Privacy Endpoint (`POST /api/v1/internal/recommend/privacy/erase`)**:
  - Authenticated via internal service bearer token (`RECOMMENDATION_INTERNAL_TOKEN`) using timing-safe comparison (`secrets.compare_digest`).
  - Purges all interaction events for `user_id == deletedUserId` from `recommendation_events` table in PostgreSQL.
  - Clears user interaction cache keys (`user:{id}:interactions:*`) and online feature caches (`online_features:user:{id}`) in Redis.
  - Strictly preserves interaction events authored by third parties on posts created by the deleted user, maintaining recommendation graph integrity without violating individual GDPR/privacy erasure boundaries.

### 2. Multi-Device Logout vs. Logout-All Perimeter
- **Current-Device Logout (`POST /api/v1/auth/logout`)**:
  - Accepts optional `LogoutRequest` containing `deviceId` or `pushToken`.
  - Invalidate the current session's refresh token cookie.
  - Validates that the provided `deviceId` belongs to the authenticated user. If valid, revokes only that specific device's push notification token (`NotificationDeviceService.revokeDevice()`).
  - All other active device sessions (mobile, tablet, desktop) and push notification registrations remain active.
  - Attempting to revoke a device belonging to another user returns `403 Forbidden` (`NotificationAccessDeniedException`).
- **Global Logout (`POST /api/v1/auth/logout-all`)**:
  - Atomically invalidates all refresh tokens associated with the user account.
  - Revokes all registered push notification device tokens across all devices (`NotificationDeviceService.revokeAllForUser()`).
  - Immediately evicts user presence from Redis and terminates active messaging presence (`MessagingPresenceService.forceOffline()`).

### 3. WebSocket Session Token Expiry Hardening
- **Derived Expiry Registry (`WebSocketAuthenticatedSessionRegistry`)**:
  - On STOMP `CONNECT` frame, the interceptor parses and validates the JWT, derives its exact expiration instant, and registers session metadata indexed by STOMP session ID and transport session ID.
- **Inbound Frame Gating**:
  - Every inbound `SEND` and `SUBSCRIBE` frame is checked against its derived token expiration.
  - Once expired, frame processing is rejected with an explicit error and the session is flagged for termination.
- **Outbound Passive Leak Protection (`WebSocketOutboundExpiryInterceptor`)**:
  - Outbound messages queued on broker channels (`/topic/...`, `/queue/...`, `/user/queue/...`) are checked against token expiration before transmission to the socket.
  - If the token has expired, outbound delivery is blocked and the underlying `WebSocketSession` is immediately closed with status `4401 Token Expired`.
- **Active Reaper Daemon (`WebSocketSessionReaper`)**:
  - Runs periodically via `@Scheduled` to find and terminate stale/expired transport sessions, evicting presence and reclaiming connection resources.

### 4. Zero-Trust Client IP & Trusted Proxy Hardening
- **Zero-Trust Default (`ClientIpResolver`)**:
  - `X-Forwarded-For`, `X-Real-IP`, and `Forwarded` headers from untrusted internet clients are strictly ignored. The client IP is resolved exclusively from `HttpServletRequest.getRemoteAddr()`.
- **Right-to-Left Chain Evaluation**:
  - When the immediate peer IP matches configured trusted proxies or CIDR blocks (`collobster.security.trusted-proxies`), the `X-Forwarded-For` chain is traversed from right to left to identify the first untrusted upstream IP.
  - Malformed or spoofed header values fail safely to `request.getRemoteAddr()`.
- **Abuse Key Invariant**:
  - Authenticated requests resolve abuse keys as `user:{userId}`, preventing malicious users from bypassing rate limits through proxy rotation or IPv6 cycling.
  - Unauthenticated requests resolve abuse keys as `ip:{resolvedClientIp}` using the trusted IP resolver.

---

## 28. Multi-Device WebSocket Expiry & Offline Recommendation Data Erasure Hardening (Phase 10.2)

### 1. Per-Session WebSocket Expiry & Multi-Device Presence Invariant
- **Root Cause Identification**:
  - Previously, `WebSocketSessionReaper` and outbound interceptors called `MessagingPresenceService.setUserOffline(userId)` when a single STOMP session expired.
  - `setUserOffline(userId)` was an account-wide operation that cleared all active sessions from the user's presence tracking in Redis. In a multi-device scenario (e.g., Phone token expires while Desktop token remains valid), this incorrectly forced the user OFFLINE across all devices.
- **Session-Scoped Presence Architecture**:
  - Introduced `removeSession(Long userId, String sessionId)` and `handleSessionDisconnected(Long userId, String sessionId)` in `MessagingPresenceService`.
  - Redis presence tracking (`messages:presence:{userId}:sessions`) removes only the expired `sessionId` member via `ZREM`.
  - **Presence Evaluation**: If `ZCARD(sessionKey) > 0`, the user remains in `messages:presence:users` and their presence state is **ONLINE**. Only when the final valid session expires (`ZCARD == 0`) does presence transition to **OFFLINE**.
- **Preserved `forceOffline(userId)` Scope**:
  - `forceOffline(userId)` is strictly reserved for true account-wide operations:
    1. Account deletion (`AccountLifecycleServiceImpl.deleteAccount()`)
    2. Account deactivation (`AccountLifecycleServiceImpl.deactivateAccount()`)
    3. Global logout (`POST /api/v1/auth/logout-all`)
    4. Administrator suspension
  - It is strictly prohibited from being called for single-session token expiry or network socket disconnection.

### 2. Targeted Outbound Expiry & Spring STOMP Fanout
- **Granular Outbound Interception**:
  - `WebSocketOutboundExpiryInterceptor` inspects the destination STOMP session for each outgoing message.
  - If the target session has expired, the frame is dropped, the socket is closed, and only that specific session's presence is removed.
  - Sibling sessions for the same user remain unaffected and continue receiving traffic.
- **User Destination Fanout (`/user/queue/**`)**:
  - Verified Spring STOMP user queue dispatch: messages fan out to target sessions independently. A stale or expired session does not poison or reject message delivery to concurrent valid sessions belonging to the same user.

### 3. Offline Recommendation Data Erasure Lifecycle
- **JSONL Event Policy (Option A — Development Only)**:
  - Enforced via startup validation in `app/events/factory.py`: If `APP_ENV=production` and `EVENT_PUBLISHER=jsonl`, service initialization raises `ValueError`.
  - `event_stream.jsonl` / `interaction_events.jsonl` are development-only scratch sinks and are not part of production storage.
  - For non-production environments, `JsonlRetentionManager` provides bounded retention (48h) and atomic user event erasure.
- **Durable Parquet Data Lake Erasure**:
  - **Privacy Erasure Manifest (`privacy_erasure_manifest.json`)**: Durable JSON tracking manifest recording tombstones (`user_id`, `requested_at`, `status`, `completed_at`, `affected_partitions`, `rows_removed`, `error_message`).
  - **Pre-Compaction Training Guard (`TrainingDataLoader`)**: Training loaders check active tombstones and strictly exclude tombstoned users from datasets before compaction occurs. Newly trained models are structurally prevented from consuming data marked for deletion.
  - **Atomic Partition Compactor (`OfflineParquetCompactor`)**: Scans historical date-partitioned Parquet files, filters out user rows using `pyarrow.compute`, writes to atomic `.tmp` files, and renames them over original partitions. All other users' rows are preserved intact. Operation is idempotent and retry-safe.
- **Model Training Manifest PII Audit**:
  - Formally audited `app/pipeline/training_manifest.py` and `scripts_create_training_manifest.py`.
  - Verified manifests record only `run_id`, `created_at`, `dataset`, `features`, and `config`. Zero direct or indirect user identifiers. Retained without modification.
- **LightGBM Model Artifact Integrity**:
  - Verified canonical model SHA-256: `25944E97AFE3194DF82AD2EAE9FF738DEF2456D1EEC929B267BF663D259C5B0B`.
  - Zero model retraining was performed.

### 4. Two-Stage Privacy Completion Semantics
- **Truthful Status Reporting**:
  - Internal privacy endpoint (`POST /api/v1/internal/recommend/privacy/erase`) returns:
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
  - Java Outbox `status = COMPLETED` means online recommendation stores (`recommendation_events`, Redis feature keys) are erased and offline erasure is durably scheduled.
  - Parquet Compaction `status = COMPLETED` means physical offline partition files have been compacted and rewritten.

### 5. Architectural Invariants Verified
- **Flyway Database Migration Version**: Maintained at V29 (`V29__user_data_erasure_outbox.sql`). No unnecessary V30 created.
- **Test Pass Rate**: 537/537 Java unit/integration tests passing (0 failures, 0 errors), 79/79 Python pytest passing (0 failures, 0 errors), and all live integration profiles passing.
- **Compliance Wording**: All privacy documentation strictly uses technically accurate language ("implements the documented user-data erasure lifecycle", "designed to support user-data deletion requirements") without unsupported legal conclusions.

---

## 29. Account-Wide Realtime Session Revocation & Shared Durable Privacy Verification (Phase 10.3 / 10.3.1 / 10.3.2)

### 1. Authoritative Generation Semantics (`auth_version`)
- **Minimum Valid Generation**: `auth_version` represents the durable, monotonically increasing minimum valid authentication generation.
- **Authoritative Invalidation Rule**:
  - `sessionVersion < requiredAuthVersion => REVOKE`
  - `sessionVersion == requiredAuthVersion => KEEP (VALID)`
  - `sessionVersion > requiredAuthVersion => KEEP (VALID)`
- **Strict Inequality for Revocation**: Remote and local revocation logic strictly compares `sessionVersion < minimumRequiredAuthVersion`. Under no circumstances is `<=` or `!=` used.
- **Why Equality Must Survive**:
  - When `logout-all` or an account revocation event occurs, DB `auth_version` increments (e.g., 4 -> 5), and Redis broadcasts event with `authVersion = 5`.
  - Stale sessions carrying generation 4 are revoked because `4 < 5`.
  - When the user immediately logs in again on a new device, the new session receives generation 5.
  - If a delayed duplicate Redis broadcast with `authVersion = 5` subsequently arrives, `5 < 5` evaluates to `false`, guaranteeing the newly established session remains active and uninterrupted.
- **Multi-Device Presence Invariant**: Presence eviction does not blindly call `forceOffline(userId)`. If any valid session (`sessionVersion >= requiredAuthVersion`) remains connected, the user remains **ONLINE** in presence tracking. Only closed session IDs are removed.
- **Origin-Node Local Revocation**: DB version increments first in PostgreSQL. The origin node invokes `closeSessionsOlderThanAuthVersion(userId, newAuthVersion, reason)`, closing only sessions belonging to previous generations and preserving concurrent sessions established with the new generation.
- **WebSocket Session Reaper**: `WebSocketSessionReaper` queries PostgreSQL authority every 5 seconds and enforces `session.authVersion < db.authVersion` (never equality).
- **Legacy Session Semantics**: Tokens without an `auth_version` claim default to `0L`. When DB `auth_version == 0`, legacy sessions remain valid. As soon as DB `auth_version > 0`, legacy sessions are considered stale and revoked.

### 2. Distributed Advisory Lock Key Derivation & Compaction Lifecycle
- **Canonical Partition Path**: Before lock derivation, the partition file path is strictly canonicalized via `Path(path).resolve().as_posix().lower()`. Multiple path representations resolving to the same physical file (e.g. `data/../data/p1.parquet` vs `data/p1.parquet`) resolve to the identical lock key.
- **Deterministic 64-Bit Derivation**:
  - Algorithm: SHA-256 digest of UTF-8 encoded canonical path string, unpacked as a signed big-endian 64-bit integer (`struct.unpack(">q", digest[:8])[0]`).
  - Guaranteed deterministic across Python processes, worker instances, application restarts, and Python hash randomization (zero use of built-in `hash()`).
  - Maps directly into PostgreSQL `bigint` signed range for `pg_advisory_lock` and `pg_advisory_unlock`.
- **Advisory Lock Collision Math (Birthday Approximation)**:
  - For $k = 1,000,000$ independent partition lock keys mapped into a 64-bit space ($N = 2^{64} \approx 1.8447 \times 10^{19}$), the birthday collision probability is:
    $$p \approx \frac{k^2}{2N} \approx \frac{10^{12}}{2 \times 1.8447 \times 10^{19}} \approx 2.71 \times 10^{-8}$$
  - **Collision Failure Mode**: Even in the astronomical event of a 64-bit hash collision between two distinct partition paths, the failure mode is **conservative over-serialization** (compaction workers serialize sequentially across the two partitions), NEVER concurrent unsafe rewriting of the same partition.
- **PostgreSQL Connection Pinning & Unlock Pool-Safety**:
  - Because `pg_advisory_lock` is session-scoped, the compaction critical section explicitly pins the underlying PostgreSQL connection (`conn = db_session.connection()`) for the entire sequence:
    1. Acquire advisory lock (`SELECT pg_advisory_lock(:id)`).
    2. Re-read Parquet partition contents under lock.
    3. Filter out claimed tombstone user rows.
    4. Write partition to unique worker temp file (`<filename>.tmp.<workerId>.<opId>`).
    5. Atomically replace original partition via `os.replace`.
    6. Unconditionally release advisory lock (`SELECT pg_advisory_unlock(:id)`).
  - **Connection Invalidation on Unlock Failure**: If `pg_advisory_unlock` fails or raises an error, `conn.invalidate(ex)` is immediately executed on the pinned connection. This guarantees that the dirty connection is discarded and closed upon pool checkout return, preventing any connection pool leakage with retained advisory locks.
- **Exception & Unlock Safety**:
  - The lock acquisition is enclosed in a strict `try ... finally` block.
  - Any runtime exception, PyArrow disk error, or OS failure unconditionally triggers the `finally` block, releasing the advisory lock and preventing connection pool lock leakage.
- **Cross-Process Correctness**:
  - PostgreSQL advisory locking provides distributed mutual exclusion across independent processes and containers.
  - In-process `threading.Lock` is purely an intra-process optimization and is not required for multi-process distributed correctness.
- **Safe Stale Temp-File Cleanup**:
  - During worker startup and scheduled sweeps, temporary files older than `max_age_seconds` (3600s) are audited.
  - Worker protects active compaction operations: before unlinking any `.tmp` file, the cleaner attempts a non-blocking `try_partition_lock` on the target partition and checks `is_worker_lease_active` in PostgreSQL.
  - If the partition is currently locked or the worker lease is active, the temp file is preserved regardless of timestamp. Inactive orphaned temp files are safely unlinked.

### 3. Model Integrity Invariant
- Canonical LightGBM Ranker Model SHA-256 Checksum:
  `25944E97AFE3194DF82AD2EAE9FF738DEF2456D1EEC929B267BF663D259C5B0B`
- Verified bit-for-bit exact match. Zero retraining performed.

### 4. Verification Test Results
- **Java Full Maven Test Suite** (`mvnw.cmd test`):
  - 570 tests discovered/run by Maven reporting, 554 passed, 16 skipped, 0 failed, 0 errors.
  - `FeedContentHydratorTest`: 5/5 passed (including $O(1)$ query count test proving zero N+1 queries across page sizes 5 -> 20 -> 50).
  - `WebSocketSessionReaperLoadTest`: 2/2 passed (including 1,250 active session chunking test and memory bounded pruning).
  - `AccountWideSessionRevocationTest`: 26/26 passed.
- **Python Pytest Suite** (`python -m pytest -q`):
  - 100 tests discovered and passed (0 failed, 0 errors in 4.25s).
  - `test_privacy_compaction_hardening.py`: 15/15 passed (including connection invalidation on advisory unlock failure).
- **Flyway Database Migration**: V31 applied and active.

---

## Phase 11 — Performance, Scalability & Production Hardening Architecture

### 1. Feed Hydration O(C) Query Optimization & N+1 Elimination
- **Problem**: In previous phases, hydrating feed candidates evaluated block, mute, and follow relationships inside per-candidate loops, generating up to $3 \times N$ database queries per feed page.
- **Resolution**:
  - Added batch querying methods in `UserBlockRepository`, `UserMuteRepository`, and `UserFollowRepository`:
    - `findBlockedOrBlockingUserIdsAmong(Long userId, Collection<Long> candidateIds)`
    - `findMutedUserIdsAmong(Long userId, Collection<Long> candidateIds)`
    - `findFollowingUserIdsAmong(Long followerId, Collection<Long> candidateIds)`
  - Exposed through `NetworkAccessService` and consumed by `PlatformPostFeedAdapter` and `CommunityPostFeedAdapter`.
  - Batch relationship checks execute at most 3 SQL queries per page, reducing hydration complexity from $O(N)$ to $O(C)$ where $C \le 2$ (content types).
  - Interaction enrichment via `InteractionService.getInteractionSummaries` remains batched via single SQL `IN (:ids)` queries per content type.

### 2. Global Pagination & Deterministic Sorting
- **Maximum Page Size Bounds**: All high-cardinality endpoints enforce explicit upper bounds:
  - Feed: default 20, max 50 (cursor-based).
  - Explore: default 20, max 50 (cursor-based).
  - Direct Messages: default 20, max 50 (cursor-based).
  - Notifications: default 20, max 50 (cursor-based).
  - Community Channel Messages: default 20, max 100 (page-based).
  - Community Members: default 20, max 100 (page-based).
  - Community Feed: default 20, max 50 (page-based).
- **Deterministic Stable Tiebreakers**: Every timestamp-sorted query now includes `, id DESC` as a strict tiebreaker (e.g. `ORDER BY createdAt DESC, id DESC`). This prevents skip/duplicate pagination anomalies when items share identical timestamps.

### 3. Redis Ephemeral Feed Session Architecture & Key Eviction Matrix
- **Minimal Metadata Storage**: Redis Feed sessions (`collobster:feed:session:{sessionId}`) store only ordered lists of lightweight `ContentRef` references and source tags. No post bodies, author PII, or full entity rows are cached in Redis. PostgreSQL remains the single durable authority for content.
- **TTL Enforcement**: Strict 20-minute TTL is set at creation and expires naturally via Redis native TTL keyspace eviction. Zero `KEYS` commands in request paths.
- **Measured Redis Memory Footprint (`MEMORY USAGE` command)**:
  - 20 Candidates: 3,076 bytes serialized payload | 3,672 bytes (3.59 KB) Redis `MEMORY USAGE` | 596 bytes overhead | 1200s TTL.
  - 500 Candidates: 72,996 bytes serialized payload | 82,008 bytes (80.09 KB) Redis `MEMORY USAGE` | 9,012 bytes overhead | 1200s TTL.
- **Extrapolations (Labelled Estimates from Measured Footprint)**:
  - 100 Concurrent Sessions (500 items): ~7.82 MB (estimated from per-session measured footprint).
  - 1,000 Concurrent Sessions (500 items): ~78.21 MB (estimated from per-session measured footprint).
  - 10,000 Concurrent Sessions (500 items): ~782.09 MB (estimated from per-session measured footprint).
- **Actual Redis Key Inventory (Audited from Source Code)**:
  1. `FeedSession`: `collobster:feed:session:{sessionId}` (String / JSON, TTL 20m)
  2. `Presence Sessions`: `collobster:messages:presence:sessions:{userId}` (ZSET of sessionIds scored by expiry, TTL 60s)
  3. `Presence Status`: `collobster:messages:presence:{userId}` (String "ONLINE", TTL 60s)
  4. `Typing State`: `collobster:messages:typing:{conversationId}` (SET of userIds, TTL 10s)
  5. `Rate Limit (Messaging)`: `collobster:messages:ratelimit:{userId}` (INCR counter, TTL 60s)
  6. `Rate Limit (Communities/Platform)`: IP and User token bucket keys (INCR counters, TTL 60s)
  7. `Online Features (Python)`: `online_features:user:{user_id}` (HASH)
  8. `User Interactions (Python)`: `user:{user_id}:interactions:*` (HASH/SET/STRING for derived signals)
- **Redis Key Family Eviction Sensitivity Matrix**:
  - `FeedSession` (`collobster:feed:session:{sessionId}`): TTL 20m. Sensitivity: Low. Eviction results in `FeedSessionExpiredException` / `HTTP 410 Gone`. Client then refreshes without cursor to generate a fresh feed session. Benign UX degradation.
  - `Presence` (`collobster:messages:presence:sessions:{userId}`, `collobster:messages:presence:{userId}`): Persistent / Heartbeat-driven. Sensitivity: Medium. Eviction causes active users to temporarily appear offline in UI indicators until next client heartbeat or WebSocket frame. Messages still deliver via PostgreSQL and push notifications. Self-healing; messaging durability is completely unaffected.
  - `RateLimitBucket` (`rate:limit:ip:{ip}`, `rate:limit:user:{userId}`, `collobster:messages:ratelimit:{userId}`): Window-scoped TTL (60s). Sensitivity: High. Premature eviction resets client counters early, giving abusive clients fresh quotas and weakening rate-limiting / brute-force protections under memory pressure.
- **Revocation Architecture Truth (No Persistent Redis Key)**:
  - There is NO persistent `revocation:user:{userId}` Redis key in Collobster.
  - Revocation architecture operates as:
    1. **PostgreSQL Authority**: Increments authoritative `auth_version` on `users` table.
    2. **Redis Pub/Sub Channel**: Broadcasts `AccountRevocationEvent` on `collobster:security:account-revocation` for instantaneous sub-millisecond cluster-wide notification.
    3. **Local JVM Registry**: `AccountRevocationListener` receives the message and triggers `WebSocketAuthenticatedSessionRegistry.closeSessionsOlderThan(userId, authVersion)`.
    4. **WebSocket Session Reaper**: Background scheduled sweeper queries PostgreSQL for updated `auth_version` every 5 seconds, providing an authoritative fallback if any node missed a transient Pub/Sub broadcast.
- **Pub/Sub Semantics vs. Key Eviction**:
  - Redis Pub/Sub channels are transient communication buses, not keys stored in Redis keyspace.
  - Pub/Sub messages are not evictable by `maxmemory-policy`.
  - If Redis restarts or buffers overflow, Pub/Sub messages may be dropped, which is why the background `WebSocketSessionReaper` provides an authoritative fallback.
- **Logical Database Isolation Truth (DB 0 vs DB 1)**:
  - Logical Redis databases (e.g. DB 0 vs DB 1) share the exact same single-threaded Redis process, memory space, `maxmemory` cap, eviction policy, and CPU. They provide zero memory or policy isolation.
  - If security-critical state (e.g. rate-limiting) requires strict isolation from disposable feed cache traffic, deploy a separate Redis instance/resource boundary.

### 4. WebSocket Session Reaper Scaling
- **Batched Auth Reconciliation**: Sweep executes on a 5-second `fixedDelay` cadence (preventing execution pileup).
- **Database Query Chunking**: Active user IDs are chunked into slices of 500 when querying `userRepository.findAuthStatesByUserIds`, preventing oversized `IN (...)` parameter lists.
- **In-Memory Map Bounding**: `pruneRevokedSessionIds()` runs each cycle to evict entries past their 30-minute retention window from `revokedSessionIds`.
- **Verified Scale**: 500-ID chunking removes unbounded IN-clause growth; verified at 1,250 sessions. Execution completes in under 20ms in `WebSocketSessionReaperLoadTest`.

### 5. Connection Pool Math & Deployment Assumptions
- **PostgreSQL Database Server Limits**:
  - `max_connections`: **100**
  - `superuser_reserved_connections`: **3**
  - **Normal Connection Usable Ceiling**: $100 - 3 = \mathbf{97}$ usable application connection slots.
- **HikariCP Configuration (Environment-Overridable)**:
  - `maximumPoolSize`: `${HIKARI_MAX_POOL_SIZE:20}`
  - `minimumIdle`: `${HIKARI_MIN_IDLE:10}`
  - `connectionTimeout`: `${HIKARI_CONN_TIMEOUT_MS:10000}`
  - `idleTimeout`: `${HIKARI_IDLE_TIMEOUT_MS:300000}`
  - `maxLifetime`: `${HIKARI_MAX_LIFETIME_MS:1800000}`
  - `leakDetectionThreshold`: `${HIKARI_LEAK_DETECTION_MS:10000}`
- **Corrected Connection Capacity Sizing Math (Example 3-Node Model)**:
  - 3 Java Application Nodes $	imes$ Hikari max pool size 20 = **60 connections**
  - Python Recommendation Service Pool: `pool_size = 10`, `max_overflow = 5` $\implies$ **15 connections** maximum checked-out capacity
  - **Total Active Application Maxima**: $60 + 15 = \mathbf{75}$ connections
  - **Required Operational Headroom**: **22** unallocated slots (22.7% planning target)
  - **Exact Sizing Balance**: $\mathbf{75 + 22 = 97}$ connections
- **Canonical Multi-Node Capacity Formula**:
  $$	ext{applicationPoolMaximum} + 	ext{requiredOperationalHeadroom} \le 	ext{normalConnectionCapacity}$$
  where $	ext{normalConnectionCapacity} = 	ext{max\_connections} - 	ext{superuser\_reserved\_connections} = 97$.
  In expanded topology terms:
  $$	ext{javaNodes} 	imes 	ext{HikariMax} + 	ext{pythonInstances} 	imes 	ext{PythonMaxCheckedOut} + 	ext{otherApplicationPools} + 	ext{requiredOperationalHeadroom} \le 97$$
- **Headroom is Topology-Dependent (Not Universal)**:
  - The 22 unallocated slots apply specifically to the 3-node reference deployment ($75 + 22 = 97$).
  - Headroom is a planning target, not a separate connection pool. It must never be consumed by steady-state application pools. It absorbs transient administrative CLI sessions (`psql`), monitoring probes, health checks, and startup migration overhead.
  - If Java node counts, Hikari pool sizes, or Python worker processes change, the connection budget must be recalculated using the canonical formula.
- **Component Invariants**:
  1. **Reaper Connections**: `WebSocketSessionReaper` executes inside the Java application's existing Hikari connection pool via Spring Data JPA. It does **not** create a separate database connection pool.
  2. **Flyway / Admin Capacity**: Flyway migrations run during startup before serving traffic and release their connection upon completion. They utilize temporary headroom from the unallocated slots, not permanent simultaneous capacity.
- **PgBouncer Guidance**:
  Re-evaluate connection pooling when projected aggregate application pool capacity approaches the PostgreSQL connection budget. Options include reducing per-node pool sizes, increasing database server capacity where justified by hardware resources, or deploying PgBouncer in transaction pooling mode.
- **SQLAlchemy Terminology**:
  - `pool_size=10, max_overflow=5` indicates a baseline pool of 10 connections with up to 5 additional overflow connections permitted during spikes, representing a maximum simultaneous checked-out capacity of 15 connections (not "5 minimum idle").
- **Lettuce Connection Model**:
  - Lettuce operates in its native thread-safe, non-blocking multiplexed connection model over Netty. All concurrent Spring Boot threads share the underlying multiplexed TCP connection with command pipelining. No explicit commons-pool2 pooling is active or required.

### 6. Database Evolution, V32 Query Plan Interpretation & Offset Complexity
- **Flyway V31** (`V31__add_performance_and_tiebreaker_indexes.sql`):
  - Created initial `idx_memberships_comm_status ON memberships(community_id, status)`.
  - Created `idx_channel_messages_comm_channel_desc ON community_channel_messages(community_id, channel_id, created_at DESC, id DESC)` providing exact index alignment for channel message pagination with tiebreaker.
- **Flyway V32** (`V32__add_membership_composite_order_index.sql`):
  - Created `idx_memberships_comm_status_joined_id ON memberships(community_id, status, joined_at DESC, id DESC)`.
  - **Structural Proof & Scope of V32**:
    `idx_memberships_comm_status_joined_id` is a composite ordered index (not an index-only covering index, as `SELECT *` visits the heap). It aligns WHERE columns (`community_id, status`) and ORDER BY columns (`joined_at DESC, id DESC`), structurally eliminating the in-memory `Incremental Sort` operator and temporary sorting memory (`work_mem`).
  - Dropped redundant `idx_memberships_comm_status` to prevent duplicate index write/storage overhead.
- **Membership Pagination Complexity ($O(\log N + 	ext{offset} + k)$)**:
  - In `GET /api/v1/communities/{id}/members`, the query executes `SELECT * FROM memberships WHERE community_id = :id AND status = :status ORDER BY joined_at DESC, id DESC LIMIT :limit OFFSET :offset`.
  - The query execution distinguishes four stages:
    1. **Index positioning**: $O(\log N)$ to traverse the B-tree to the qualifying slice.
    2. **Ordered index traversal**: Linear traversal along the leaf index pages in `joined_at DESC, id DESC` order.
    3. **Offset skipping**: Traverses and discards the first `offset` qualifying tuples.
    4. **Returned page rows**: Fetches up to $k$ qualifying heap tuples (where $k \le 	ext{page size}$).
  - Overall algorithmic complexity is approximately:
    $$O(\log N + 	ext{offset} + k)$$
    V32 does NOT eliminate `OFFSET` traversal. Deep `OFFSET` remains a known scalability limitation.
- **Deep Offset Empirical Evidence (`EXPLAIN (ANALYZE, BUFFERS)` on 20,000 synthetic rows)**:
  - **Offset 0 (LIMIT 20)**: Top Node: `Limit`, Scan Node: `Index Scan` | Actual Rows: **20** | Shared Hit Blocks: **4** | Exec Time: **2.475 ms** (Planning: 12.064 ms).
  - **Offset 1,000 (LIMIT 20)**: Top Node: `Limit`, Scan Node: `Index Scan` | Actual Rows: **1,020** | Shared Hit Blocks: **31** | Exec Time: **0.236 ms** (Planning: 0.140 ms).
  - **Offset 10,000 (LIMIT 20)**: Top Node: `Limit`, Scan Node: `Index Scan` | Actual Rows: **10,020** | Shared Hit Blocks: **275** | Exec Time: **1.465 ms** (Planning: 0.076 ms).
  - Cleaned up: remaining synthetic memberships = **0**.
- **Pagination Contract Invariant**:
  - The existing API contract (`Pageable` with offset pagination) is preserved. No silent conversion to keyset/cursor pagination. Deep offset pagination is documented as a known boundary limitation; a future versioned API may introduce keyset pagination if high-cardinality access patterns warrant it.
- **Production Migration Safety**:
  - Standard `CREATE INDEX` acquires a `SHARE` lock on `memberships`, which blocks concurrent write operations (`INSERT`, `UPDATE`, `DELETE`).
  - Online zero-downtime indexing in production requires `CREATE INDEX CONCURRENTLY` and `DROP INDEX CONCURRENTLY`, which cannot run inside transactional blocks (requiring non-transactional Flyway execution or scheduled maintenance windows).
  - Applied migration `V32` remains untouched in the database to preserve migration history immutability.

### 7. Java-to-Python HTTP Bulkhead Architecture & Concurrency Bounds
- **HttpClient Executor vs. Caller Concurrency Distinction**:
  - Configuring an executor with N threads on Java 21 `HttpClient` only bounds the internal worker threads used for asynchronous selector and response handling. It does NOT limit how many calling threads can concurrently execute synchronous `RestClient` calls.
- **Explicit Semaphore Bulkheads**:
  - Implemented explicit bounded-concurrency bulkheads using `java.util.concurrent.Semaphore`:
    1. **Feed Ranking Bulkhead** (`HttpRecommendationClient`): Sized by `collobster.recommendation.max-concurrent-requests` (default: **20**), acquire timeout `collobster.recommendation.bulkhead-acquire-timeout-ms` (default: **250 ms**). Saturated calls throw `RecommendationBulkheadFullException` and immediately fall back to `BaselineFeedRankingStrategy` (zero user-visible 500 errors).
    2. **Feedback Ingestion Bulkhead** (`HttpRecommendationFeedbackClient`): Sized by `collobster.recommendation.feedback.max-concurrent-requests` (default: **10**), acquire timeout `collobster.recommendation.feedback.bulkhead-acquire-timeout-ms` (default: **500 ms**). Saturated calls throw `RecommendationException` and retry via the outbox worker.
    3. **Privacy Erasure Bulkhead** (`HttpRecommendationPrivacyClient`): Externalized in `RecommendationPrivacyProperties`, sized by `collobster.recommendation.privacy.max-concurrent-requests` (default: **5**), acquire timeout `collobster.recommendation.privacy.bulkhead-acquire-timeout-ms` (default: **1000 ms**). Saturated calls throw `RecommendationException` and retry via the erasure outbox worker.
  - **Permit Leak Safety**: Permits are acquired with timeout before entering transport and released in unconditional `finally` blocks, guaranteeing zero permit leakage on success, exception, or timeout.
  - **Client Reuse Note**: Unit tests prove singleton lifecycle (`RestClient`, `HttpClient`, and `JdkClientHttpRequestFactory` references preserved). They do not claim proof of TCP socket reuse, which requires network-level or integration observation.
- **Configuration Alias Cleanup**:
  - `collobster.recommendation.max-connections` and `collobster.recommendation.feedback.max-connections` are marked `@Deprecated` in favor of canonical `max-concurrent-requests`.

### 8. FastAPI CPU-Bound Inference Model, Server-Side Bulkhead & Empirical Tuning
- **Route Execution Off Event Loop**:
  - In `internal_rank_route.py`, `rank_candidates` is declared as a synchronous endpoint `def rank_candidates(...)` (NOT `async def`).
  - FastAPI / Starlette automatically offloads synchronous route execution to AnyIO's worker threadpool (`anyio.to_thread.run_sync`), ensuring CPU-intensive LightGBM `model.predict()` execution never blocks the main asyncio event-loop thread.
- **Server-Side Inference Bulkhead & Bounded Queue**:
  - Model prediction is guarded by `threading.Semaphore(RANKING_MAX_CONCURRENT_INFERENCES)` (default: **4**) in `InternalFeedRankingService`.
  - LightGBM prediction explicitly maintains `num_threads=1`, preventing OpenMP thread oversubscription.
  - Bounded permit acquire timeout: `RANKING_INFERENCE_ACQUIRE_TIMEOUT_MS` (default: **500 ms**).
  - If inference permits cannot be acquired within timeout, `InferenceQueueSaturatedException` is raised, and the route immediately returns **HTTP 503 Service Unavailable**.
  - Java receives HTTP 503, caught by `RecommendationFeedRankingStrategy` $ightarrow$ activates deterministic `BaselineFeedRankingStrategy` fallback (zero user-visible 500 errors).
- **Multi-Worker Capacity Model & Empirical Tuning**:
  - Total deployment inference capacity is approximately:
    $$	ext{Deployment Inference Concurrency} pprox 	ext{Uvicorn Workers} 	imes 	ext{RANKING\_MAX\_CONCURRENT\_INFERENCES}$$
  - **Empirical Tuning Rule**: Matching this number to CPU core count does not automatically guarantee optimal throughput. Total worker and concurrency limits must be tuned empirically against CPU utilization, latency, and throughput in the target deployment environment.
- **Event-Loop Responsiveness Evidence**:
  - Under continuous 500-candidate ranking load (5 concurrent workers, 164 large rank requests completed during probe window), 50 concurrent `/health` probe requests achieved **100% success rate (0 failures)** with median latency of **46.16 ms** and p95 of **137.56 ms** (idle baseline: **3.53 ms**).
  - The event loop remained available and error-free, with measurable latency degradation under heavy CPU inference load.

### 9. Benchmark Request ID Integrity & Percentile Methodology
- **Unique Request IDs**:
  - Every benchmark request dynamically generates a unique UUID `requestId` via payload template cloning (`copy.deepcopy(template)`). Zero correlation ID reuse across concurrent requests.
- **Documented Nearest-Rank Percentiles**:
  - Nearest-rank percentile formula: $	ext{index} = \max(0, \min(\lceil p 	imes N ceil - 1, N - 1))$, applied consistently across all benchmark tiers.
- **Throughput Accounting**:
  - Attempted RPS and Successful RPS are explicitly separated. All tiers achieved 100% success rate (0 failures).

### 10. Internal Service Authentication & Security Invariants
- **Secret Management**:
  - Internal service-to-service Bearer token (`COLLOBSTER_RECOMMENDATION_INTERNAL_TOKEN`) rotated to cryptographically strong secret via `secrets.token_urlsafe(32)`.
  - Stored strictly in local environment configurations (`.env`). Excluded by `.gitignore`. Zero plaintext secrets in source code, tests, or documentation.
  - Timing-safe verification (`secrets.compare_digest`) enforced. Old token strictly rejected (401); new token accepted (200).
- **Read Replicas & High Availability**:
  - Introduce read replicas when measured primary read utilization, latency, connection pressure, or replica-worthy workload justifies it.
  - Standalone Redis remains a known deployment limitation; Sentinel/Cluster choice depends on production topology and operational requirements.
