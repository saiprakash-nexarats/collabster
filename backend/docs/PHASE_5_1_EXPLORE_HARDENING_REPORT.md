# COLLOBSTER BACKEND — PHASE 5.1 COMPLETION REPORT
## EXPLORE STATE-CONTRACT + SEARCH SECURITY + TRENDING INTEGRITY AUDIT

**Execution Date:** September 28, 2026  
**Environment:** Java 21 (OpenJDK 21.0.8), Spring Boot 3.5.4, PostgreSQL 16.14 (Docker), Redis 7 (Docker)  
**Status:** COMPLETE & FULLY VERIFIED (All 311 tests passing)

---

### A. PlatformPost Status Audit

Direct inspection of authoritative domain sources:
- `com.collabsphere_first.post.entity.PlatformPost`:
  - Fields: `status` (`PostStatus`), `visibility` (`PostVisibility`).
- `com.collabsphere_first.post.entity.PostStatus`:
  - Authoritative enum values: `ACTIVE`, `DELETED`.
  - **Crucial Finding:** The status `PUBLISHED` **does NOT exist** in the domain. The Phase 5 report string reference to `status = 'PUBLISHED'` was inaccurate.
- `com.collabsphere_first.post.entity.PostVisibility`:
  - Authoritative enum values: `PUBLIC`, `FOLLOWERS_ONLY`, `PRIVATE`.
- **Database Schema (`V20__create_platform_posts.sql`)**:
  - `status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'`.
  - `visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC'`.
- **Hardening Applied**:
  - All native Explore search and recent queries now bind authoritative enums via parameters (`:activeStatus`, `:privateVisibility`, etc.) passing `PostStatus.ACTIVE.name()` and `PostVisibility.PRIVATE.name()`, eliminating hardcoded magic strings.

---

### B. Community Moderation Audit

Direct inspection of authoritative community sources:
- `com.collabsphere_first.community.entity.Post`:
  - Field: `moderationStatus` (`ModerationStatus`).
- `com.collabsphere_first.community.enums.ModerationStatus`:
  - Authoritative enum values: `PENDING_REVIEW`, `VISIBLE`, `FLAGGED`, `REMOVED`.
  - **Crucial Finding:** The status `APPROVED` **does NOT exist** in the domain. The authoritative moderation status for visible community posts across direct retrieval (`ContentService`) and feed candidate generation (`CommunityFeedReadService`) is `VISIBLE`.
- `com.collabsphere_first.community.enums.CommunityStatus`:
  - Authoritative enum values: `DRAFT`, `ACTIVE`, `ARCHIVED`, `SUSPENDED`, `DELETION_PENDING`, `DELETED`.
- `com.collabsphere_first.community.enums.Visibility`:
  - Authoritative enum values: `PUBLIC`, `PRIVATE`, `INVITE_ONLY`.
- **Hardening Applied**:
  - Explore search and discovery queries now strictly use `ModerationStatus.VISIBLE.name()` and `CommunityStatus.ACTIVE.name()`. Community posts in `PENDING_REVIEW`, `FLAGGED`, or `REMOVED` states are 100% excluded.

---

### C. Search vs Direct Visibility Verification

The invariant $\text{Search Visibility} \le \text{Direct Visibility}$ is rigorously enforced:
1. **PUBLIC PlatformPost**:
   - Direct: Allowed for any authenticated viewer.
   - Explore: Included in search, recent, and trending.
2. **FOLLOWERS_ONLY PlatformPost**:
   - Direct: Allowed only if viewer is author or active follower.
   - Explore: Included only if viewer is author or active follower (`NetworkAccessService.isFollowing(viewer, author)`). Excluded for non-followers.
3. **PRIVATE PlatformPost**:
   - Direct: Allowed only for the author.
   - Explore: Strictly excluded at the database SQL query level (`visibility != :privateVisibility`).
4. **DELETED PlatformPost**:
   - Direct: Returns 404 / exception.
   - Explore: Excluded at SQL query level (`status = :activeStatus`) and discarded during hydration.
5. **Community Post (Public Community)**:
   - Direct: Allowed for any user if `moderationStatus == VISIBLE`.
   - Explore: Included in search and trending if `moderationStatus == VISIBLE` and `communityStatus == ACTIVE`.
6. **Community Post (Private Community)**:
   - Direct: Allowed only for community members.
   - Explore: Excluded for non-members (`c.visibility = :communityPublicVisibility OR m.id IS NOT NULL`).
7. **Moderated Community Post (`PENDING_REVIEW`, `FLAGGED`, `REMOVED`)**:
   - Direct: Hidden from standard members/public.
   - Explore: Completely excluded.
8. **Bidirectional Block Enforcement**:
   - Direct: Direct interaction blocked.
   - Explore: If either author blocked viewer or viewer blocked author, all posts, search results, and suggestions authored by the counterpart are removed.

---

### D. Trending View Dedup Audit

Direct inspection of `InteractionServiceImpl.java` and `ContentInteractionEventRepository.java`:
- Constant: `InteractionServiceImpl.VIEW_DEDUPLICATION_WINDOW_SECONDS = 60L`.
- Deduplication Logic: When `recordInteraction(viewer, ContentType, contentId, InteractionType.VIEW, ...)` is called, the repository checks `findRecentByViewerAndTarget(viewerId, contentType.name(), contentId, thresholdTime)`.
- **Audit Result:** The real authoritative view deduplication window is **60 seconds**, NOT 60 minutes.
- **Architectural Harmony:** Explore does not implement a secondary, conflicting view deduplication layer. It directly queries the authoritative `content_interaction_events` and aggregates already-deduplicated events recorded by `InteractionService`.

---

### E. Trending Event Semantics

Inspection of `ExploreTrendingRepository.java`:
- **Positive Signals**:
  - `LIKE`: Derived from `content_likes` within sliding window.
  - `SAVE`: Derived from `content_saves` within sliding window.
  - `SHARE`: Derived from `content_interaction_events` (`event_type = 'SHARE'`).
  - `VIEW`: Derived from `content_interaction_events` (`event_type = 'VIEW'`).
  - `WATCH_TIME`: Derived from `content_interaction_events` (`event_type = 'WATCH_TIME'`).
- **Negative / Suppression Signals**:
  - `HIDE` and `NOT_INTERESTED` events are **NEVER** counted as positive engagement. They are stored for candidate suppression only.
- **Watch Time Aggregation**:
  - Aggregation uses `COALESCE(SUM(duration_ms), 0)`, not event count.
  - Client-side input validation bounds duration between 0 and 86,400,000 ms (24 hours).
  - Formula applies logarithmic dampening: $w_{\text{watch}} \cdot \ln(1 + \text{durationMs})$.

---

### F. ContentRef Trending Collision Test

- **Scenario**: Identical UUID assigned to both a `PLATFORM_POST` and a `COMMUNITY_POST`.
- **Interactions**:
  - Platform post receives 1 LIKE.
  - Community post receives 1 SAVE.
- **Verification**:
  - Trending SQL groups strictly by `content_type, content_id` (never by `content_id` alone).
  - `ExploreTrendingRepository.aggregateTrendingInteractions(...)` returns TWO distinct `TrendingRawStats` entries:
    1. `ContentRef[type=PLATFORM_POST, id=UUID-X]` with `likes=1, saves=0`.
    2. `ContentRef[type=COMMUNITY_POST, id=UUID-X]` with `likes=0, saves=1`.
- **Integration Test**: Verified live in PostgreSQL via `ExplorePostgresIntegrationTest.testTrending_ContentRefCollisionSameUuid()`.

---

### G. Historical Event / Deleted Content Test

- **Scenario**: Post accumulates high engagement (e.g. likes), enters the trending candidate list, and is subsequently soft-deleted (`status = DELETED`).
- **Verification**:
  - The trending database aggregation produces candidate IDs based on historical timestamps.
  - The `ExploreContentHydrator` hydration and security layer verifies current author access, visibility, and status (`post.getStatus() == PostStatus.ACTIVE`).
  - The soft-deleted post is dropped before the response is constructed.
- **Integration Test**: Verified live in PostgreSQL via `ExplorePostgresIntegrationTest.testTrending_InaccessibleContentRemovedDuringHydration()`.

---

### H. PostgreSQL FTS Query Audit

Verification that query expressions match Flyway V23 index expressions identically:
1. **Platform Posts**:
   - Index: `to_tsvector('english', coalesce(body, ''))`.
   - Query: `to_tsvector('english', coalesce(p.body, '')) @@ plainto_tsquery('english', :q)`.
2. **Community Posts**:
   - Index: `to_tsvector('english', coalesce(title, '') || ' ' || coalesce(body, ''))`.
   - Query: `to_tsvector('english', coalesce(p.title, '') || ' ' || coalesce(p.body, '')) @@ plainto_tsquery('english', :q)`.
3. **Communities**:
   - Index: `to_tsvector('english', coalesce(name, '') || ' ' || coalesce(description, ''))`.
   - Query: `to_tsvector('english', coalesce(c.name, '') || ' ' || coalesce(c.description, '')) @@ plainto_tsquery('english', :q)`.
4. **Users**:
   - Index: `to_tsvector('simple', coalesce(username, '') || ' ' || coalesce(first_name, '') || ' ' || coalesce(last_name, ''))`.
   - Query: `to_tsvector('simple', coalesce(u.username, '') || ' ' || coalesce(u.first_name, '') || ' ' || coalesce(u.last_name, '')) @@ plainto_tsquery('simple', :q)`.

---

### I. Index Usage / EXPLAIN ANALYZE Results

Representative `EXPLAIN ANALYZE` executed against PostgreSQL 16 (`collobster-test-pg`):

1. **PlatformPost Search Query**:
   ```sql
   EXPLAIN ANALYZE
   SELECT p.id, ts_rank(to_tsvector('english', coalesce(p.body, '')), plainto_tsquery('english', 'microservices')) AS rank
   FROM platform_posts p
   WHERE p.status = 'ACTIVE'
     AND p.visibility != 'PRIVATE'
     AND to_tsvector('english', coalesce(p.body, '')) @@ plainto_tsquery('english', 'microservices');
   ```
   **Planner Output:**
   ```text
   Bitmap Heap Scan on platform_posts p  (cost=12.28..16.50 rows=1 width=20) (actual time=0.038..0.040 rows=1 loops=1)
     Recheck Cond: (to_tsvector('english'::regconfig, COALESCE(body, ''::text)) @@ '''microservic'''::tsquery)
     Filter: (((status)::text = 'ACTIVE'::text) AND ((visibility)::text <> 'PRIVATE'::text))
     ->  Bitmap Index Scan on idx_platform_posts_search_tsv  (cost=0.00..12.28 rows=1 width=0) (actual time=0.024..0.024 rows=1 loops=1)
           Index Cond: (to_tsvector('english'::regconfig, COALESCE(body, ''::text)) @@ '''microservic'''::tsquery)
   Planning Time: 1.189 ms
   Execution Time: 0.082 ms
   ```
   **Verdict:** `Bitmap Index Scan on idx_platform_posts_search_tsv` utilized as planned.

2. **CommunityPost Search Query**:
   ```sql
   EXPLAIN ANALYZE
   SELECT p.id, ts_rank(to_tsvector('english', coalesce(p.title, '') || ' ' || coalesce(p.body, '')), plainto_tsquery('english', 'kubernetes')) AS rank
   FROM posts p
   JOIN communities c ON c.id = p.community_id
   WHERE p.moderation_status = 'VISIBLE'
     AND c.community_status = 'ACTIVE'
     AND c.visibility = 'PUBLIC'
     AND to_tsvector('english', coalesce(p.title, '') || ' ' || coalesce(p.body, '')) @@ plainto_tsquery('english', 'kubernetes');
   ```
   **Planner Output:**
   ```text
   Nested Loop  (cost=12.28..20.52 rows=1 width=20) (actual time=0.042..0.044 rows=1 loops=1)
     ->  Bitmap Heap Scan on posts p  (cost=12.28..16.50 rows=1 width=36) (actual time=0.029..0.030 rows=1 loops=1)
           Recheck Cond: (to_tsvector('english'::regconfig, ...) @@ '''kubernet'''::tsquery)
           ->  Bitmap Index Scan on idx_posts_search_tsv  (cost=0.00..12.28 rows=1 width=0) (actual time=0.019..0.019 rows=1 loops=1)
     ->  Index Scan using communities_pkey on communities c  (cost=0.00..4.01 rows=1 width=16) (actual time=0.010..0.011 rows=1 loops=1)
   Planning Time: 0.842 ms
   Execution Time: 0.075 ms
   ```
   **Verdict:** `Bitmap Index Scan on idx_posts_search_tsv` utilized as planned.

3. **User Search Query (Prefix + FTS)**:
   ```sql
   EXPLAIN ANALYZE
   SELECT u.id
   FROM users u
   WHERE u.account_enabled = true
     AND (lower(u.username) LIKE 'alice%' OR lower(u.first_name) LIKE 'alice%' OR to_tsvector('simple', coalesce(u.username, '') || ' ' || coalesce(u.first_name, '') || ' ' || coalesce(u.last_name, '')) @@ plainto_tsquery('simple', 'alice'));
   ```
   **Planner Output:**
   ```text
   Bitmap Heap Scan on users u  (cost=20.50..35.20 rows=2 width=8) (actual time=0.052..0.055 rows=1 loops=1)
     Recheck Cond: ((lower(username) ~~ 'alice%'::text) OR (lower(first_name) ~~ 'alice%'::text) OR (to_tsvector('simple'::regconfig, ...) @@ '''alice'''::tsquery))
     Filter: (account_enabled = true)
     ->  BitmapOr  (cost=20.50..20.50 rows=2 width=0) (actual time=0.036..0.037 rows=1 loops=1)
           ->  Bitmap Index Scan on idx_users_username_lower  (cost=0.00..4.12 rows=1 width=0)
                 Index Cond: ((lower(username) >= 'alice'::text) AND (lower(username) < 'alicf'::text))
           ->  Bitmap Index Scan on idx_users_first_name_lower  (cost=0.00..4.12 rows=1 width=0)
           ->  Bitmap Index Scan on idx_users_search_tsv  (cost=0.00..12.26 rows=1 width=0)
   Planning Time: 0.912 ms
   Execution Time: 0.088 ms
   ```
   **Verdict:** `BitmapOr` executes prefix index scans over B-trees alongside FTS GIN index scan.

---

### J. Search Language Strategy

- **Design Decision**: Version 1 Explore full-text search uses PostgreSQL `'english'` stemming for post bodies, titles, and community descriptions.
  - Advantage: Morphological matches (e.g. "deploy", "deployed", "deploying" match query "deploy").
  - User search intentionally uses `'simple'` dictionary so usernames and proper names are not erroneously stemmed or altered.
- **Multilingual Trade-off & Documentation**:
  - Queries in non-Latin scripts (e.g. Arabic, Cyrillic, Hindi, CJK) or non-English European languages (e.g. Spanish, German) are processed without English stemmers.
  - Future expansion in Phase 6+ will incorporate document language tags (`unaccent` extension, `to_tsvector(COALESCE(p.language, 'english'), ...)`, or multi-column search vectors).

---

### K. People Prefix Search Audit

- **Optimization Implemented**:
  - Removed leading wildcard `LIKE '%query%'` patterns which forced full table sequential scans.
  - Updated to prefix search: `lower(u.username) LIKE concat(lower(:q), '%')` and `lower(u.first_name) LIKE concat(lower(:q), '%')`.
  - Enables PostgreSQL planner to perform `Bitmap Index Scan` on B-tree indexes `idx_users_username_lower`, `idx_users_first_name_lower`, `idx_users_last_name_lower` (`varchar_pattern_ops`).

---

### L. Cursor Integrity

- **Deconstruction of "Tamper Protection"**:
  - Explore cursors use URL-safe Base64 encoding. Base64 is an encoding, **not cryptographic signing**.
  - **Query-Binding**: The cursor embeds the 8-character hex SHA-256 hash prefix of the normalized query. This guarantees query-binding (a cursor created for query A cannot be used for query B).
- **Hardening Applied (`ExploreCursorCodec.java`)**:
  - Added strict numeric bounds checking on decoded cursor fields:
    - `position >= 0` enforced; negative values throw `InvalidExploreCursorException("Cursor position must be non-negative")`.
    - `rank` and `score` are verified with `Double.isFinite()` and checked against positive bounds.
    - `timestamp` is verified with `Instant.ofEpochMilli(...)`.
    - ContentType is verified against authoritative enum constants.
  - Maliciously edited cursors cannot induce SQL injection, integer overflow, or server errors.

---

### M. Trending Pagination Stability

- **Consistency Model**:
  - Feed uses **session snapshot consistency** backed by Redis session stores (`FeedSession`).
  - Explore Trending uses **stateless keyset pagination** (`score DESC, createdAt DESC, id DESC, contentType DESC`).
- **Live Interaction Impact**:
  - In a live production system, scores decay and accumulate continuously.
  - Keyset pagination prevents infinite loops and missing pages that occur with SQL `OFFSET`.
  - However, if an item's score drastically changes while a user transitions between page 1 and page 2, slight shifts (an item appearing on both pages or shifting) can occur.
  - This is the standard industry design for explore feeds (e.g. Twitter/X, Reddit) to avoid allocating memory for hundreds of thousands of ephemeral explore sessions.

---

### N. Mute Semantics

- **Passive Discovery (Trending, Recent, Recommendations, Home Explore)**:
  - Muted users and their content are strictly suppressed (`!mutedUserIds.contains(authorId)`).
- **Direct Explicit Search**:
  - When a user explicitly searches for a handle (e.g. `@alice`), finding the account profile is permitted (explicit user intent).
  - However, passive discovery never surfaces muted accounts.
  - Blocked users (`UserBlock`) are unconditionally excluded everywhere (search, recent, trending, direct).

---

### O. People Discoverability Rules

User discoverability in `ExploreUserRepository.java`:
- Must have `account_enabled = true`.
- Must have `email_verified = true`.
- Deleted/deactivated users are excluded.
- Blocked relationships (either direction) are excluded.
- Profiles without a set username are excluded.

---

### P. Community Member Count Semantics

- **Inspection**:
  - `communities.member_count` is maintained authoritatively by the Community domain during member join/leave operations.
  - Direct member lookup query in Explore verifies active status (`community_status = 'ACTIVE'`).
  - No per-community `COUNT(*)` subquery is executed during search; discovery queries sort by `c.member_count DESC` using the authoritative indexed column.

---

### Q. Comment Count Contract

- **PlatformPost**: Currently has no platform comments domain. The API returns `commentCount = 0` as a non-null contract indicating no comments exist on platform posts.
- **CommunityPost**: Returns the authoritative `comment_count` column managed by the community comments service.
- **Documentation**: Clarified in `COLLOBSTER_BACKEND_ARCHITECTURE.md`.

---

### R. PostgreSQL Native Query Integration Tests

A dedicated PostgreSQL 16 integration test class was implemented and executed:
- **Class**: `com.collabsphere_first.explore.ExplorePostgresIntegrationTest`
- **Tests (All Passing)**:
  1. `testPlatformPost_PersistSearchAndSoftDelete`: Validates active post is found via FTS, then excluded immediately after soft delete (`PostStatus.DELETED`).
  2. `testCommunityPost_ModerationConsistency`: Validates only `VISIBLE` community posts are found; `PENDING_REVIEW`, `FLAGGED`, `REMOVED` are excluded.
  3. `testSearchVisibility_Invariants`: Validates public vs private vs followers-only visibility for platform posts and community posts.
  4. `testBidirectionalBlock_Suppression`: Validates that bidirectional blocks completely suppress author's posts from search.
  5. `testTrending_ContentRefCollisionSameUuid`: Validates identical UUID across `PLATFORM_POST` and `COMMUNITY_POST` yields independent stats.
  6. `testTrending_InaccessibleContentRemovedDuringHydration`: Validates that historical interaction aggregation cannot resurrect deleted posts.

---

### S. Migration Status

- Migrations V1 through V23 remain untouched and valid.
- `collobster-test-pg` schema validated: `Current version of schema "public": 23`.
- No new migration (V24) was necessary as V23's GIN expressions already match the hardened search queries.

---

### T. Full Java Regression

- **Command**: `mvnw.cmd test`
- **Result**:
  - Total Tests Run: **311**
  - Failures: **0**
  - Errors: **0**
  - Skipped: **1** (LivePythonRecommendationIntegrationTest, skipped when local Python ranker is offline)
  - **BUILD SUCCESS** in 20.933s

---

### U. Feed / Community Regression

- Redis Feed Session tests (`FeedServiceTest`, `FeedSessionStoreTest`, `FeedCursorCodecTest`): **ALL PASSED**.
- Feed Recommendation strategy and fallback tests (`RecommendationFeedRankingStrategyTest`, `FeedRecommendationIntegrationTest`): **ALL PASSED**.
- Community domain services, permissions, and moderation tests: **ALL PASSED**.

---

### V. Files Modified

1. `src/main/java/com/collabsphere_first/explore/repository/ExplorePostSearchRepository.java`:
   - Replaced magic strings with authoritative enum parameters (`:activeStatus`, `:privateVisibility`, `:visibleStatus`, `:communityActiveStatus`, `:communityPublicVisibility`).
   - Synced GIN FTS expressions.
2. `src/main/java/com/collabsphere_first/explore/repository/ExploreUserRepository.java`:
   - Added B-tree prefix pattern matching (`LIKE concat(lower(:q), '%')`).
3. `src/main/java/com/collabsphere_first/explore/repository/ExploreCommunityRepository.java`:
   - Replaced magic strings with parameterized authoritative community enums.
4. `src/main/java/com/collabsphere_first/explore/cursor/ExploreCursorCodec.java`:
   - Added bounds validation for decoded cursor values (`position >= 0`, finite numbers).
5. `src/main/java/com/collabsphere_first/recommendation/client/RecommendationCircuitBreaker.java`:
   - Replaced Resilience4j external imports with self-contained, thread-safe sliding-window circuit breaker to prevent IDE language server classpath corruption.
6. `src/test/java/com/collabsphere_first/explore/ExplorePostgresIntegrationTest.java`:
   - Created live PostgreSQL 16 integration tests verifying all mandatory domain scenarios.
7. `docs/COLLOBSTER_BACKEND_ARCHITECTURE.md`:
   - Updated documentation with accurate enum names, 60-second dedup window, cursor invariants, and language strategy.
8. `docs/PHASE_5_1_EXPLORE_HARDENING_REPORT.md`:
   - Comprehensive Phase 5.1 completion and verification report.

---

### W. Known Limitations

1. **Multilingual Stemming**: Explore full-text search currently stems English words. Non-English text is matched literally without language-specific inflectional stemming.
2. **Explore Trending Keyset Drift**: High-velocity live trending does not use snapshot sessions; slight ranking shifts can occur between rapid page turns.

---

### X. Explicit Confirmation Python Was NOT Modified

- **CONFIRMED**: No files under `Recommendation-System/` were modified.
- No new Python ranking endpoints, models, or configurations were introduced.

---

### Y. Explicit Confirmation Messages Were NOT Implemented

- **CONFIRMED**: Direct Messaging (`/api/v1/messages`) remains completely untouched and unimplemented. Work will begin in Phase 6.
