# COLLOBSTER BACKEND — PHASE 3.1 HARDENING REPORT

**Date:** 2026-09-27  
**Status:** COMPLETED  
**Technology Stack:** Java 21 | Spring Boot 3.5.4 | Redis | PostgreSQL 16  
**Architecture Author:** Principal Backend Engineer & Feed Systems Architect  

---

## 1. Executive Summary

Phase 3.1 establishes strict **ContentRef Identity Integrity** (`ContentRef = (ContentType, UUID)`) and **Production Redis Safety** across the Collobster platform before Java-to-Python recommendation integration.

Key hardening accomplishments:
1. **Content Identity Enforcement**: Verified that NO query, map, set, cache, feed session, interaction summary, or suppression evaluation operates on bare UUID alone. `(ContentType, UUID)` is strictly enforced everywhere.
2. **Identical UUID Collision Safety**: Proved that `PLATFORM_POST UUID-X` and `COMMUNITY_POST UUID-X` can independently coexist on the platform with completely isolated like counts, liked states, saved states, suppression events, and source metadata.
3. **Repository Query Hardening**: Audited and updated `ContentLikeRepository` and `ContentSaveRepository` queries to explicitly select both `contentType` and `contentId`, enforce `GROUP BY l.contentType, l.contentId`, and map directly to `ContentRef`.
4. **Redis Serialization Integrity**: Verified that `FeedSession` serializes `candidateSources` with namespaced string keys (`"{contentType}:{id}"`), preventing any JSON map key collisions when identical UUIDs exist across content types.
5. **Production Redis Safeguards**: Confirmed `FeedSessionConfig` instantiates `RedisFeedSessionStore` by default in production and prevents silent fallback to in-memory storage. Verified that Redis outages return HTTP 503 Service Unavailable (`FeedSessionStorageException`).
6. **Zero Python Changes**: The Python recommendation system (`Recommendation-System/`) remains 100% untouched.

---

## 2. ContentRef Identity & Collision Audit

In a multi-domain platform supporting both Platform Posts and Community Posts, UUID collisions across content types (whether accidental or benchmark-induced) are valid and must never corrupt data:
- `ContentRef` is an immutable Java record: `(ContentType type, UUID id)` with non-null constraints and strict equality/hashcode across both fields.
- `ContentRef.platformPost(UUID-X).equals(ContentRef.communityPost(UUID-X))` is `false`.
- Every domain SPI and DTO across `feed`, `interaction`, and `post` references content strictly via `ContentRef`.

---

## 3. Interaction Queries Hardening

Audited and hardened `ContentLikeRepository` and `ContentSaveRepository`:
- **Like Counts**:
  ```java
  @Query("SELECT l.contentType, l.contentId, COUNT(l) FROM ContentLike l " +
         "WHERE l.contentType = :contentType AND l.contentId IN :contentIds " +
         "GROUP BY l.contentType, l.contentId")
  List<Object[]> countLikesByContentTypeAndIds(...);
  ```
  Returns `row[0]` as `ContentType`, `row[1]` as `UUID`, and `row[2]` as `Long count`. Unpacked directly into `new ContentRef(type, id)`.
- **Liked Status**:
  ```java
  @Query("SELECT l.contentType, l.contentId FROM ContentLike l " +
         "WHERE l.user.id = :userId AND l.contentType = :contentType AND l.contentId IN :contentIds")
  List<Object[]> findLikedContent(...);
  ```
  Returns `(ContentType, UUID)` pairs unpacked into `Set<ContentRef>`.
- **Saved Status**:
  ```java
  @Query("SELECT s.contentType, s.contentId FROM ContentSave s " +
         "WHERE s.user.id = :userId AND s.contentType = :contentType AND s.contentId IN :contentIds")
  List<Object[]> findSavedContent(...);
  ```
  Returns `(ContentType, UUID)` pairs unpacked into `Set<ContentRef>`.
- **Suppressed Content (`HIDE` & `NOT_INTERESTED`)**:
  ```java
  @Query("SELECT DISTINCT e.contentType, e.contentId FROM ContentInteractionEvent e " +
         "WHERE e.user.id = :userId AND e.eventType IN ('HIDE', 'NOT_INTERESTED')")
  List<Object[]> findSuppressedContent(@Param("userId") Long userId);
  ```
  Evaluates both fields to return `Set<ContentRef>`.

---

## 4. Candidate Deduplication & Feed Session Keying

- **Deduplication Key**: `FeedServiceImpl` deduplicates candidate pools using `Map<ContentRef, FeedCandidate> candidateMap = new LinkedHashMap<>()`. Candidates with the same UUID but different `ContentType` are stored under distinct keys and never merged or overwritten.
- **Session Sources Keying**: `FeedSession.create` serializes `Map<ContentRef, Set<FeedSourceType>>` into `Map<String, Set<FeedSourceType>>` using format `"{type}:{id}"` (e.g. `"PLATFORM_POST:11111111-1111-1111-1111-111111111111"` and `"COMMUNITY_POST:11111111-1111-1111-1111-111111111111"`). Each ContentRef retains independent source metadata in Redis.

---

## 5. Production Redis Configuration & Outage Behavior

In `FeedSessionConfig`:
```java
@Configuration
public class FeedSessionConfig {

    @Bean
    @ConditionalOnProperty(name = "collobster.feed.session-store", havingValue = "in-memory")
    public FeedSessionStore inMemoryFeedSessionStore(FeedProperties properties) {
        return new InMemoryFeedSessionStore(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "collobster.feed.session.store", havingValue = "in-memory")
    public FeedSessionStore inMemoryFeedSessionStoreDot(FeedProperties properties) {
        return new InMemoryFeedSessionStore(properties);
    }

    @Bean
    @ConditionalOnMissingBean(FeedSessionStore.class)
    public FeedSessionStore redisFeedSessionStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            FeedProperties properties
    ) {
        return new RedisFeedSessionStore(redisTemplate, objectMapper, properties);
    }
}
```
- **Default Production Store**: `RedisFeedSessionStore` is loaded automatically if no in-memory property is configured.
- **No Silent Fallback**: In production, if Redis is down or unreachable, `RedisFeedSessionStore` throws `FeedSessionStorageException`, which `FeedExceptionHandler` maps to HTTP 503 Service Unavailable (`"Feed session service is temporarily unavailable. Please retry."`).
- **In-Memory Store Isolation**: `InMemoryFeedSessionStore` is restricted to tests or explicit local configurations where `collobster.feed.session-store=in-memory` is explicitly passed.

---

## 6. Verification & Automated Test Results

### 1. New Tests Added in Phase 3.1 (11 Tests)
1. **`ContentRefCollisionTest`** (7 tests):
   - `testLikeCountSafetyWithSharedUuid`: Platform post (10 likes) and Community post (25 likes) remain independent, never merged into 35.
   - `testLikedStateSafetyWithSharedUuid`: Liking platform post does not mark community post as liked, and vice versa.
   - `testSavedStateSafetyWithSharedUuid`: Saving community post does not mark platform post as saved, and vice versa.
   - `testSuppressionSafetyWithSharedUuid`: Hiding platform post suppresses only platform post, leaving community post visible. `NOT_INTERESTED` on community post suppresses only community post.
   - `testCandidateDeduplicationAndSourceMetadata`: Both platform and community posts with identical UUID coexist in feed with independent source tags.
   - `testHydrationCollisionDelegation`: Hydration routes each item to its respective adapter and preserves order.
   - `testMandatoryCollisionScenario` (Section 21 Mandatory Scenario):
     - Shared UUID X between PlatformPost X and CommunityPost X.
     - User likes PlatformPost X.
     - User saves CommunityPost X.
     - User hides PlatformPost X.
     - Confirms Feed excludes PlatformPost X (suppressed) and includes CommunityPost X with `liked = false`, `saved = true`, and CommunityPost X's own like count.
2. **`FeedSessionStoreTest`** (+3 tests, total 6):
   - `testRedisContentRefCollisionSafety`: Live Redis test storing and retrieving session with identical UUID across `PLATFORM_POST` and `COMMUNITY_POST`, verifying independent sources and order preservation.
   - `testRedisOutageThrowsStorageException`: Proves Redis connection failures throw `FeedSessionStorageException` and never silently fallback to in-memory store.
   - `testProductionSessionStoreConfiguration`: Proves Spring context defaults to `RedisFeedSessionStore` and only enables `InMemoryFeedSessionStore` under explicit configuration.
3. **`FeedContentHydratorTest`** (+1 test, total 4):
   - `testHydrationCollisionSameUuidAcrossContentTypes`: Verifies batch hydration routes mixed items with identical UUIDs to their respective adapters and preserves distinct entries.

### 2. Full Regression Execution
```powershell
.\mvnw.cmd clean test
```
- **Total Tests Run**: 249
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0
- **Duration**: ~46 seconds

### 3. PostgreSQL & Hibernate Validation
- Tested against live PostgreSQL 16 container (`collobster-test-pg`, port 5433).
- Flyway migrations V1 through V22 validated cleanly.
- `hibernate.ddl-auto=validate` confirmed full entity-schema alignment.

### 4. Redis Live Verification
- Tested against live Redis 7 container (`collobster-feed-redis`, port 6379).
- Verified JSON serialization, TTL expiration, collision safety, and cleanup.
