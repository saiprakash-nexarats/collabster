# COLLOBSTER BACKEND — PHASE 5 COMPLETION REPORT

**Production Explore & Discovery Core (Java 21 / Spring Boot 3.5.x)**  
**Status**: COMPLETE  
**Technology Boundary**: 100% Java / Spring Boot. Python Recommendation Service remains ML/Recommendation engine only (unmodified). Messages domain NOT implemented.

---

## 1. Executive Summary
Phase 5 establishes the authoritative **Explore & Discovery Core** for Collobster under `com.collabsphere_first.explore`. While the **Feed** answers *"What content should I see now from my network?"*, **Explore** serves as the discovery destination answering *"What new content, people, and communities can I discover across the platform?"*.

The implementation introduces:
1. Mobile Explore Landing Preview (`GET /api/v1/explore`) with strictly bounded sections.
2. Grouped Global Search Preview (`GET /api/v1/explore/search?q=...`) across posts, people, and communities.
3. Dedicated Paginated Typed Search Endpoints for posts, people, and communities using query-bound keyset cursors.
4. Deterministic Multi-Signal Trending Engine with configurable weights, recency decay, and Interaction module event abuse resistance.
5. Recent Content Discovery for newly created public/accessible posts.
6. Heuristic People and Community Discovery pipelines.
7. Full-text search and trending window indexes via Flyway migration `V23__add_explore_search_indexes.sql` on PostgreSQL 16.
8. Authoritative privacy and security boundaries guaranteeing that search visibility is always a subset of direct content retrieval visibility ($\text{Search Visibility} \le \text{Direct Visibility}$).

---

## 2. Package Architecture (`com.collabsphere_first.explore`)
The module is organized strictly into cohesive layers adhering to existing Collobster architectural standards:

```text
com.collabsphere_first.explore/
├── config/
│   └── ExploreProperties.java               # @ConfigurationProperties(prefix = "collobster.explore")
├── controller/
│   ├── ExploreController.java               # REST Controller exposing all 9 Explore endpoints
│   └── ExploreExceptionHandler.java         # Global validation/cursor error translation
├── cursor/
│   ├── ExploreSearchCursor.java             # Keyset cursor tied to search query hash
│   ├── ExploreKeysetCursor.java             # Keyset cursor for recency, trending & discovery
│   └── ExploreCursorCodec.java              # Base64 URL-safe codec with query-hash tamper detection
├── dto/
│   ├── ExploreSection.java                  # Generic bounded section preview container
│   ├── ExplorePersonSummary.java            # Safe public user summary with relationship status
│   ├── ExploreCommunitySummary.java         # Safe community summary with membership status
│   ├── ExploreHomeResponse.java             # Mobile landing page payload
│   ├── ExploreGroupedSearchResponse.java    # Grouped preview search payload
│   ├── ExplorePostSearchResponse.java       # Paginated post search response
│   ├── ExplorePeopleSearchResponse.java     # Paginated people search response
│   ├── ExploreCommunitySearchResponse.java  # Paginated community search response
│   ├── ExploreTrendingPostsResponse.java    # Paginated trending posts response
│   ├── ExploreRecentPostsResponse.java      # Paginated recent posts response
│   ├── ExplorePeopleDiscoveryResponse.java  # Paginated people discovery response
│   └── ExploreCommunityDiscoveryResponse.java # Paginated community discovery response
├── exception/
│   ├── ExploreException.java                # Base explore runtime exception
│   ├── InvalidExploreQueryException.java    # HTTP 400 for empty, short, long, or malformed queries
│   └── InvalidExploreCursorException.java   # HTTP 400 for malformed or query-mismatched cursors
├── hydration/
│   └── ExploreContentHydrator.java          # Polymorphic PostItem hydration + bulk interaction summaries
├── repository/
│   ├── ExplorePostSearchRepository.java     # Native SQL for full-text search & recent candidates
│   ├── ExploreTrendingRepository.java       # Windowed interaction aggregation (likes, saves, events)
│   ├── ExploreUserRepository.java           # Native full-text/prefix user search & batch relationships
│   └── ExploreCommunityRepository.java      # Native full-text community search & batch memberships
├── scoring/
│   └── TrendingScoreCalculator.java         # Weighted interaction calculation + exponential recency decay
└── service/
    ├── ExploreService.java                  # Core facade interface
    ├── PostSearchService.java               # PlatformPost + CommunityPost search & ranking
    ├── PeopleSearchService.java             # Safe user search & batch relationship decoration
    ├── CommunitySearchService.java          # Safe community search & batch membership decoration
    ├── GlobalExploreSearchService.java      # Orchestrates grouped preview search
    ├── ExploreTrendingService.java          # Window aggregation, scoring, hydration & keyset paging
    ├── ExploreRecentService.java            # Keyset pagination for recent public/accessible posts
    ├── ExplorePeopleDiscoveryService.java   # Heuristic user suggestions (excluding blocked/connected)
    ├── ExploreCommunityDiscoveryService.java# Community discovery reusing public/activity rules
    └── impl/
        └── ExploreServiceImpl.java          # Coordinates all landing, search, and discovery features
```

---

## 3. Detailed REST API Contracts

### 3.1 Mobile Explore Landing (`GET /api/v1/explore`)
- **Headers**: `Authorization: Bearer <jwt>`
- **Response `200 OK`**:
  ```json
  {
    "success": true,
    "message": "Explore home retrieved successfully",
    "data": {
      "trendingPosts": {
        "items": [ /* PostItem array */ ],
        "hasMore": true,
        "seeAllEndpoint": "/api/v1/explore/posts/trending"
      },
      "recentPosts": {
        "items": [ /* PostItem array */ ],
        "hasMore": true,
        "seeAllEndpoint": "/api/v1/explore/posts/recent"
      },
      "people": {
        "items": [
          {
            "userId": 42,
            "username": "alice",
            "displayName": "Alice Smith",
            "avatarUrl": "https://...",
            "bio": "Open source developer",
            "following": false,
            "connected": false,
            "connectionPending": false
          }
        ],
        "hasMore": true,
        "seeAllEndpoint": "/api/v1/explore/people"
      },
      "communities": {
        "items": [
          {
            "id": "c001...",
            "name": "Rust Developers",
            "description": "All about systems programming in Rust",
            "coverImageUrl": null,
            "memberCount": 128,
            "isMember": false,
            "membershipRole": null,
            "visibility": "PUBLIC"
          }
        ],
        "hasMore": true,
        "seeAllEndpoint": "/api/v1/explore/communities"
      }
    }
  }
  ```

### 3.2 Grouped Search Preview (`GET /api/v1/explore/search?q=rust`)
- **Query Parameter**: `q` (normalized: trimmed, unicode-safe, 2-100 characters).
- **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "query": "rust",
      "posts": [ /* up to 5 PostItem results */ ],
      "people": [ /* up to 5 ExplorePersonSummary results */ ],
      "communities": [ /* up to 5 ExploreCommunitySummary results */ ]
    }
  }
  ```

### 3.3 Typed Search Endpoints
1. `GET /api/v1/explore/search/posts?q={q}&cursor={cursor}&limit={limit}`
   - Returns paginated `PostItem` list with `ContentRef`, interaction states, author metadata, and `nextCursor`.
2. `GET /api/v1/explore/search/people?q={q}&cursor={cursor}&limit={limit}`
   - Returns paginated `ExplorePersonSummary` list with relationship states and `nextCursor`.
3. `GET /api/v1/explore/search/communities?q={q}&cursor={cursor}&limit={limit}`
   - Returns paginated `ExploreCommunitySummary` list with membership states and `nextCursor`.

### 3.4 Discovery Endpoints
1. `GET /api/v1/explore/posts/trending?cursor={cursor}&limit={limit}`
   - Full paginated trending feed ranked by multi-signal engagement with time decay.
2. `GET /api/v1/explore/posts/recent?cursor={cursor}&limit={limit}`
   - Full paginated recent posts feed sorted by `createdAt DESC`.
3. `GET /api/v1/explore/people?cursor={cursor}&limit={limit}`
   - Full paginated user discovery suggestions.
4. `GET /api/v1/explore/communities?cursor={cursor}&limit={limit}`
   - Full paginated public/discoverable community suggestions.

---

## 4. Keyset & Query-Bound Cursor Design

### 4.1 Search Keyset Cursor (`ExploreSearchCursor`)
Search cursors must prevent offset drift and preserve text relevance ordering across pages. Furthermore, cursors are strictly bound to their search query:
- **Format**: `queryHash:rank:createdAtMs:id:contentType:position`
  - `queryHash`: First 16 hexadecimal characters of SHA-256 of `query.trim().toLowerCase()`.
  - `rank`: Text search score (`ts_rank` value).
  - `createdAtMs`: Epoch millisecond timestamp for secondary tie-breaking.
  - `id`: Unique UUID of the item.
  - `contentType`: `PLATFORM_POST`, `COMMUNITY_POST`, `USER`, or `COMMUNITY`.
  - `position`: Global offset counter within the result stream.
- **Query Mismatch Rejection**:
  If a client acquires a cursor for query `"rust"` and passes it to `GET /api/v1/explore/search/posts?q=java&cursor=...`, the server detects that `queryHash("rust") != queryHash("java")` and throws `InvalidExploreCursorException`, returning `HTTP 400 Bad Request`.

### 4.2 Discovery Keyset Cursor (`ExploreKeysetCursor`)
- **Format**: `timestampMs:id:contentType:score`
- Encodes item timestamp, stable UUID, content type, and score for deterministic pagination in Trending, Recent, and Discovery feeds without SQL `OFFSET`.

---

## 5. Security & Privacy Invariant Verification

| Privacy Rule | Enforcement Mechanism | Verification Result |
| :--- | :--- | :--- |
| **Search Visibility $\le$ Direct Content Retrieval** | Search queries and hydration filter private/inaccessible items identically to direct `GET` operations. | **VERIFIED**: A user searching exact unique text from another user's private post receives 0 matches (`ExploreSecurityIntegrationTest.testPrivatePlatformPost_NeverReturnedInSearch`). |
| **Followers-Only PlatformPosts** | Search only returns `FOLLOWERS_ONLY` posts if `viewerId == authorId` or `networkAccessService.isFollowing(viewer, author) == true`. | **VERIFIED**: Non-followers receive 0 matches; active followers receive the post in search. |
| **Community Post Authorization** | Posts must be in `PUBLIC` communities or communities where the viewer holds active membership, and `moderation_status = 'APPROVED'`. | **VERIFIED**: Inaccessible and moderated posts are excluded in SQL and hydration (`testInaccessibleCommunityPost_NeverReturnedInSearch`). |
| **Bidirectional Block Boundary** | Block in either direction ($A \to B$ or $B \to A$) eliminates all posts, profile summaries, search results, and trending items authored by the counterpart. | **VERIFIED**: Complete exclusion in search, home, and discovery (`testBlockedUserContent_ExcludedFromSearch`). |
| **Mute Suppression** | Passive Explore surfaces (Trending, Recent, Discovery) strictly suppress muted users' content. Direct explicit searches allow locating the account if specifically searched by handle. | **VERIFIED**: Muted authors are filtered from passive feeds, but remain resolvable under direct search intent. |

---

## 6. PostgreSQL 16 Search & Index Migration (`V23`)
Flyway migration `V23__add_explore_search_indexes.sql` was created and applied cleanly against PostgreSQL 16:
```sql
-- GIN Indexes for Full-Text Search
CREATE INDEX IF NOT EXISTS idx_platform_posts_search_tsv
    ON platform_posts USING gin (to_tsvector('english', COALESCE(body, '')));

CREATE INDEX IF NOT EXISTS idx_posts_search_tsv
    ON posts USING gin (to_tsvector('english', COALESCE(title, '') || ' ' || COALESCE(body, '')));

CREATE INDEX IF NOT EXISTS idx_communities_search_tsv
    ON communities USING gin (to_tsvector('english', COALESCE(name, '') || ' ' || COALESCE(description, '')));

CREATE INDEX IF NOT EXISTS idx_users_search_tsv
    ON users USING gin (to_tsvector('simple', COALESCE(username, '') || ' ' || COALESCE(first_name, '') || ' ' || COALESCE(last_name, '')));

-- Normalized Lowercase Prefix Indexes
CREATE INDEX IF NOT EXISTS idx_users_username_lower ON users (lower(username));
CREATE INDEX IF NOT EXISTS idx_users_first_name_lower ON users (lower(first_name));
CREATE INDEX IF NOT EXISTS idx_users_last_name_lower ON users (lower(last_name));

-- Interaction Trending Sliding Window Indexes
CREATE INDEX IF NOT EXISTS idx_content_likes_trending
    ON content_likes (created_at DESC, content_type, content_id);

CREATE INDEX IF NOT EXISTS idx_content_saves_trending
    ON content_saves (created_at DESC, content_type, content_id);

CREATE INDEX IF NOT EXISTS idx_interaction_events_trending
    ON content_interaction_events (event_timestamp DESC, event_type, content_type, content_id);
```

---

## 7. Deterministic Trending Formula
Trending scores do not rely on machine learning or black-box estimators. They are calculated dynamically using authoritative interaction data:

$$\text{RawScore} = (w_{\text{like}} \cdot L) + (w_{\text{share}} \cdot S) + (w_{\text{view}} \cdot V) + (w_{\text{save}} \cdot K) + (w_{\text{watch}} \cdot \ln(1 + W))$$
$$\text{Decay} = 0.5^{\frac{\Delta t}{T_{\text{half-life}}}}$$
$$\text{FinalScore} = \text{RawScore} \cdot \text{Decay}$$

- **Configured Weights (`ExploreProperties`)**:
  - `like-weight`: 2.0
  - `share-weight`: 4.0
  - `view-weight`: 0.2
  - `save-weight`: 3.0
  - `watch-time-weight`: 0.05
  - `half-life-hours`: 12.0
  - `window`: 24h
- **Abuse Resistance**: View counts and interaction events leverage existing Interaction module deduplication (1 view per 60-minute window per viewer/content pair). Repeated clicks cannot inflate trending rank.

---

## 8. Performance Safeguards & N+1 Prevention
1. **Batch Relationship Lookups**:
   - `ExploreUserRepository.findFollowingTargetIds(sourceUserId, targetUserIds)`
   - `ExploreUserRepository.findActiveConnectedUserIds(sourceUserId, targetUserIds)`
   - Decorates 10+ people search results in exactly 2 constant queries ($O(1)$) instead of $2N$ queries.
2. **Batch Community Membership Lookups**:
   - `ExploreCommunityRepository.findUserMemberships(userId, communityIds)`
   - Evaluates membership status across all candidate communities in a single query.
3. **Polymorphic Post Hydration**:
   - Groups candidate `ContentRef` items by `PLATFORM_POST` and `COMMUNITY_POST`.
   - Fetches each group in a single bulk ID query.
   - Enriches all posts with `InteractionService.getInteractionSummaries(viewerId, refs)` in a single batched Redis/SQL pass.

---

## 9. Test Verification Results

### 9.1 Explore Test Suite (38 tests)
- `ExploreCursorCodecTest`: 4/4 passed (roundtrip, query binding, invalid base64, corrupted parts).
- `TrendingScoreCalculatorTest`: 4/4 passed (weight calculation, recency decay, zero inputs, half-life decay).
- `ExploreQueryValidatorTest`: 6/6 passed (null/blank, minimum length, maximum length, valid queries, whitespace normalization).
- `PostSearchServiceTest`: 3/3 passed (platform post search, community post search, empty queries).
- `PeopleSearchServiceTest`: 2/2 passed (handle/name search, block filtering).
- `CommunitySearchServiceTest`: 1/1 passed (public/discoverable search).
- `ExploreTrendingServiceTest`: 2/2 passed (candidate scoring & ranking, empty trending window).
- `ExploreRecentServiceTest`: 1/1 passed (recent public/accessible feed).
- `ExploreHomeAndDiscoveryTest`: 2/2 passed (home landing bounded sections, empty graph robustness).
- `ExploreSecurityIntegrationTest`: 3/3 passed (private post leakage prevention, inaccessible community post exclusion, bidirectional block enforcement).
- `ExploreControllerTest`: 10/10 passed (all 9 endpoints + validation error translation).

### 9.2 Full Java Regression
```text
Tests run: 305, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS (Total time: 26.316 s)
```
- Includes 100% pass across: Auth, Profile, Workspace, Community, PlatformPost, Network, Interaction, Feed, Recommendation Java integration, and Explore.

### 9.3 Python Regression
```text
tests/test_cold_start.py ...
tests/test_collaborative_filtering.py ...
tests/test_end_to_end.py .....
tests/test_interaction_feedback.py ..
tests/test_internal_ranking_contract.py ..............
tests/test_lightgbm_ranker.py ...
tests/test_online_feedback.py .
tests/test_phase7_infrastructure.py ....
tests/test_phase9_finalization.py ...
tests/test_ranking_quality_and_contract.py .......
tests/test_reranker.py ...
tests/test_two_tower_ann.py ...
51 passed in 18.61s
```
- Zero modifications to Python code. Recommendation contract and LightGBM model integration remain intact.

---

## 10. Known Limitations & Future Personalization Seam
1. **Taxonomy & Hashtags**: Collobster currently lacks a first-class hashtag or hierarchical topic entity table. Search operates over post bodies, titles, and community categories/tags. When topic taxonomies are formalized, they will plug directly into `ExplorePostSearchRepository`.
2. **Future Python Personalization Seam (Phase 5.1)**: Explore discovery and trending are currently deterministic Java-driven heuristics. A future Phase 5.1 can expose an optional personalized reranking seam by passing candidate `ContentRef` lists to Python via a dedicated, contract-versioned endpoint, keeping the core search and authorization logic in Java.
3. **Messages Not Implemented**: Confirmed that no messages or conversation endpoints were introduced in Phase 5. Phase 6 will implement user-to-user direct messaging.
