# Phase 11, 11.1, 11.2 & 11.3 — Final Performance, HTTP Bulkhead, Complexity & Deployment-Truth Report

## Executive Summary
Phase 11, 11.1, 11.2, and 11.3 deliver comprehensive performance optimization, query bounds enforcement, memory and connection pool hardening, secret rotation, query plan verification on representative scale datasets, and concurrency bulkheading across the Collobster backend without modifying established product contracts or weakening Phase 10.3.x security and privacy invariants.

Phase 11.3 specifically closes the final architectural integrity and deployment-truth gates:
1. **Java -> Python Outbound HTTP Bulkheads**: Implements explicit bounded-concurrency bulkheads using `java.util.concurrent.Semaphore` across `HttpRecommendationClient` (20 permits), `HttpRecommendationFeedbackClient` (10 permits), and `HttpRecommendationPrivacyClient` (5 permits). Corrects property naming to `max-concurrent-requests` and distinguishes executor thread limits from actual caller concurrency limits.
2. **Fast Fallback & Permit Safety**: Outbound calls enforce short acquire timeouts (`bulkheadAcquireTimeoutMs`: 250ms for ranking, 500ms for feedback, 1000ms for privacy). Saturated ranking requests shed load immediately and fall back gracefully to `BaselineFeedRankingStrategy` with zero user-visible 500 errors. Permits are strictly released in `finally` blocks, with zero permit leakage on success, timeout, or exception.
3. **Externalized Privacy Configuration**: Externalizes `RecommendationPrivacyProperties` (`collobster.recommendation.privacy.*`) with dedicated bulkhead concurrency, acquire timeout, connect/read timeouts, and service endpoints.
4. **FastAPI Server-Side CPU Bulkhead & Multi-Worker Model**: Implements a bounded inference semaphore (`threading.Semaphore(RANKING_MAX_CONCURRENT_INFERENCES)`) around LightGBM `model.predict(..., num_threads=1)` to prevent CPU thread thrashing in Starlette's AnyIO threadpool. Documents that process-level inference limits scale with Uvicorn worker counts ($W \times C$).
5. **Truthful Event-Loop Responsiveness**: Re-evaluated `/health` responsiveness under 500-candidate ranking load. Truthfully documents that the event loop remained available (0 failures across 50 probes) with measurable latency degradation under CPU load (p50: 46.16 ms, p95: 137.56 ms vs idle baseline 3.53 ms).
6. **B-Tree Complexity & Composite Index Terminology**: Corrects "covering index" to "composite ordered index" for `idx_memberships_comm_status_joined_id (community_id, status, joined_at DESC, id DESC)`, recognizing that `SELECT *` requires heap access. Corrects "$O(1)$ tuple retrieval" to accurate B-tree complexity: index positioning is $O(\log N)$, followed by bounded retrieval of up to LIMIT rows, approximately $O(\log N + k)$, where $k \le \text{page size}$.
7. **Connection Pooling & PgBouncer Guidance**: Replaces prescriptive node-count rules with standard capacity evaluation criteria: re-evaluate connection pooling when projected aggregate application pool capacity approaches the PostgreSQL connection budget (options include tuning per-node pool sizes, increasing database capacity, or deploying PgBouncer).
8. **Redis Key Eviction Sensitivity & Workload Analysis**: Explicitly classifies Redis key families (`FeedSession`, `Presence`, `RateLimitBucket`, revocation state) by eviction sensitivity and documents the consequences of eviction. Eliminates rigid universal mandates (`volatile-lru`, $\ge 2 \text{ GB}$) in favor of workload-driven production sizing.

---

## 1. Test Verification Baseline
- **Java Spring Boot Suite (`mvnw.cmd test`)**:
  - Discovered / Reported: **576**
  - Passed: **560**
  - Skipped: **16**
  - Failed: **0**
  - Errors: **0**
  - Status: **BUILD SUCCESS** (Execution time: ~1m 22s)
- **Python Recommendation Suite (`python -m pytest -q`)**:
  - Discovered / Run: **100**
  - Passed: **100**
  - Skipped: **0**
  - Failed: **0**
  - Errors: **0**
  - Warnings: 1 (PytestDeprecationWarning on default fixture loop scope)
  - Execution time: ~7.29s
- **Model Checksum (Exact SHA-256)**:
  `25944E97AFE3194DF82AD2EAE9FF738DEF2456D1EEC929B267BF663D259C5B0B` (Verified bit-for-bit exact match, 0 retraining).

---

## 2. Measurement Environment (Local Benchmark Baseline)
- **CPU**: AMD Ryzen 7 7735HS (8 Cores / 16 Threads @ 3.2 GHz base)
- **RAM**: 16 GB DDR5
- **OS**: Windows 11 Home 64-bit
- **Java**: OpenJDK 21.0.8 (JetBrains Runtime JBR-21.0.8)
- **PostgreSQL**: PostgreSQL 16.3 on x86_64-pc-windows-msvc (port 5433)
- **Redis**: Redis 7.2.4 (port 6379)
- **Python**: Python 3.11.9 (Uvicorn ASGI daemon on 127.0.0.1:8000)

*Engineering Disclaimer: All latency and throughput figures reported below reflect local development profiling on this single-host environment and serve as engineering evidence and baseline validation, not unsupported production SLA claims or guaranteed capacity figures.*

---

## 3. Advisory Lock & Unlock Hardening
### A. Collision Probability Math (Birthday Approximation)
For $k = 1,000,000 = 10^6$ independent partition lock keys derived via truncated big-endian 64-bit SHA-256 hashes in signed space ($N = 2^{64} \approx 1.8447 \times 10^{19}$):
$$p \approx \frac{k^2}{2N} = \frac{10^{12}}{2 \times 1.8447 \times 10^{19}} = \frac{10^{12}}{3.6893 \times 10^{19}} \approx 2.71 \times 10^{-8}$$
- **Failure Mode Correctness**: If a collision occurs between two different partition paths, compaction workers serialize sequentially across both partitions. Simultaneous unsafe concurrent writes to the same physical Parquet partition remain mathematically and structurally impossible.
- **Documentation Correction**: Phase 10.3.2 notes mentioning $\sim 10^{-10}$ have been formally corrected to the birthday approximation ($2.71 \times 10^{-8}$).

### B. Advisory Unlock Failure & Connection Pool Safety
- Session-level PostgreSQL advisory locks survive transaction rollbacks.
- **Hardening**: In `offline_privacy.py` (`partition_lock` and `try_partition_lock`), the `finally` block catches any failure during `SELECT pg_advisory_unlock(:id)` and immediately invokes `conn.invalidate(ex)`.
- This ensures that if advisory unlocking encounters an error or network drop, the underlying database connection is invalidated and discarded rather than returned to the connection pool with an active lock attached.

---

## 4. Feed Hydration O(C) Query Optimization & N+1 Elimination
- **Problem Solved**: Previous candidate hydration queried blocks, mutes, and follows individually per candidate, producing up to $3 \times N$ queries per feed page.
- **Batch Resolution**:
  - Implemented batch repository queries in `UserBlockRepository`, `UserMuteRepository`, and `UserFollowRepository`.
  - Evaluated via `NetworkAccessService` and consumed by `PlatformPostFeedAdapter` and `CommunityPostFeedAdapter`.
  - Relationship checks require at most 3 SQL queries per page, reducing hydration complexity from $O(N)$ to $O(C)$ where $C \le 2$ (content types).
  - Verified by `FeedContentHydratorTest` ($O(1)$ query count invariance across page sizes 5, 20, 50).

---

## 5. PostgreSQL Query Plan Interpretation, Complexity & Migration Safety
1. **Accurate Structural Value of V32**:
   - V32 created `idx_memberships_comm_status_joined_id (community_id, status, joined_at DESC, id DESC)`.
   - On cached local executions, both V31 and V32 execute in low-millisecond times (~0.35 ms vs ~0.60 ms). Therefore, V32 is not described as an arbitrary latency speedup.
   - **What V32 structurally proves**:
     - WHERE columns (`community_id, status`) are index-leading.
     - ORDER BY columns (`joined_at DESC, id DESC`) are index-aligned.
     - The query planner completely eliminates the `Incremental Sort` operator.
     - Deterministic pagination ordering is natively supported by the index.

2. **Index Terminology & Offset Complexity**:
   - **Index Terminology**: `idx_memberships_comm_status_joined_id` is a **composite ordered index**, NOT a "covering index". Because the endpoint queries `SELECT *` from `memberships`, the query planner must visit the heap for non-indexed columns (Index Scan, not Index-Only Scan).
   - **Membership Pagination Complexity ($O(\log N + \text{offset} + k)$)**:
     In `GET /api/v1/communities/{id}/members`, the query executes `SELECT * FROM memberships WHERE community_id = :id AND status = :status ORDER BY joined_at DESC, id DESC LIMIT :limit OFFSET :offset`.
     The query distinguishes four stages:
     1. *Index positioning*: $O(\log N)$ to reach the first matching B-tree index entry.
     2. *Ordered index traversal*: Traversal along leaf index pages in `joined_at DESC, id DESC` order.
     3. *Offset skipping*: Scanning and discarding the first `offset` qualifying tuples.
     4. *Returned page rows*: Fetching up to $k$ qualifying rows from the heap ($k \le \text{page size}$).
     Overall algorithmic complexity is approximately:
     $$O(\log N + \text{offset} + k)$$
     V32 structurally eliminates the `Incremental Sort` operator, but does NOT eliminate `OFFSET` traversal. Deep `OFFSET` remains a known scalability limitation.
   - **Empirical Deep Offset Evidence (`EXPLAIN (ANALYZE, BUFFERS)` on 20,000 synthetic rows)**:
     - **Offset 0 (LIMIT 20)**: Top Node: `Limit`, Scan Node: `Index Scan` | Actual Rows: **20** | Shared Hit Blocks: **4** | Exec Time: **2.475 ms** (Planning: 12.064 ms).
     - **Offset 1,000 (LIMIT 20)**: Top Node: `Limit`, Scan Node: `Index Scan` | Actual Rows: **1,020** | Shared Hit Blocks: **31** | Exec Time: **0.236 ms** (Planning: 0.140 ms).
     - **Offset 10,000 (LIMIT 20)**: Top Node: `Limit`, Scan Node: `Index Scan` | Actual Rows: **10,020** | Shared Hit Blocks: **275** | Exec Time: **1.465 ms** (Planning: 0.076 ms).
     - Cleaned up: remaining synthetic memberships = **0**.

3. **Repeated Measurements**:
   - 20 warm-up runs followed by 100 measured executions against 20,000 synthetic membership rows:
     - **p50**: **2.423 ms**
     - **p95**: **2.879 ms**
     - **p99**: **3.320 ms**
     - *Buffers*: shared hit=5, zero disk sort, zero temporary files.

4. **Production Migration Safety Analysis**:
   - Standard `CREATE INDEX` acquires a `SHARE` lock on the target table, blocking concurrent write operations (`INSERT`, `UPDATE`, `DELETE`).
   - For online zero-downtime production deployments, indexes must be created and dropped using `CREATE INDEX CONCURRENTLY` and `DROP INDEX CONCURRENTLY`.
   - **PostgreSQL Transaction Constraint**: `CREATE INDEX CONCURRENTLY` cannot execute within a normal transaction block (`BEGIN ... COMMIT`).
   - In Flyway, concurrent index migrations require non-transactional execution (`@transactional=false` configuration or dedicated non-transactional migration scripts), or must be applied during a scheduled maintenance window.
   - **Applied Migration Rule**: Because `V32__add_membership_composite_order_index.sql` was already applied to the database, it remains untouched to preserve schema migration history immutability.

5. **Synthetic Benchmark Data Cleanup Verification**:
   - Benchmark data cleanup executes inside unconditional `try ... finally` blocks.
   - Post-benchmark verification confirms:
     - Remaining synthetic memberships for `Scale Test Comm`: **0**
     - Remaining test communities: **0**
     - Zero synthetic data remains mixed into development fixtures.

---

## 6. Flyway Evolution: V31 and V32
1. **Flyway V31** (`V31__add_performance_and_tiebreaker_indexes.sql`):
   - Created `idx_memberships_comm_status ON memberships(community_id, status)`.
   - Created `idx_channel_messages_comm_channel_desc ON community_channel_messages(community_id, channel_id, created_at DESC, id DESC)`.
2. **Flyway V32** (`V32__add_membership_composite_order_index.sql`):
   - Created `idx_memberships_comm_status_joined_id ON memberships(community_id, status, joined_at DESC, id DESC)`.
   - Dropped redundant `idx_memberships_comm_status` to prevent duplicate index write overhead.
   - Applied and active. **Latest Schema Version**: **V32**.

---

## 7. Global Pagination & Deterministic Tiebreakers Matrix
| Endpoint | Entity | Pagination Model | Default Limit | Max Limit | Sort Order & Tiebreaker | Authorization |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET /api/v1/feed` | Feed items | Cursor (Base64) | 20 | 50 | ML Ranker / Fallback | Current User |
| `GET /api/v1/explore/search/posts` | Posts | Cursor (Base64) | 20 | 50 | `createdAt DESC, id DESC` | Public / Accessible |
| `GET /api/v1/explore/posts/trending` | Posts | Cursor (Base64) | 20 | 50 | `engagement DESC, id DESC` | Public / Accessible |
| `GET /api/v1/messages/conversations/{id}/messages` | DirectMessage | Cursor (Base64) | 20 | 50 | `createdAt DESC, id DESC` | Participant Only |
| `GET /api/v1/notification-center` | Notification | Cursor (Base64) | 20 | 50 | `createdAt DESC, id DESC` | Recipient Only |
| `GET /api/v1/communities/{id}/channels/{cid}/messages` | ChannelMessage | Pageable Offset | 20 | 100 | `createdAt DESC, id DESC` | Community Member |
| `GET /api/v1/communities/{id}/members` | Membership | Pageable Offset | 20 | 100 | `joinedAt DESC, id DESC` | Community Member |
| `GET /api/v1/communities/{id}/posts` | Community Post | Pageable Offset | 20 | 50 | `pinned DESC, createdAt DESC, id DESC` | Community Member |
| `GET /api/v1/communities/discovery/trending` | Community | Pageable Offset | 20 | 100 | `activityScore DESC, createdAt DESC, id DESC` | Public |

---

## 8. Redis Feed Session Architecture & Eviction Sensitivity Analysis
### A. Storage Architecture & Measured Memory Evidence
- **Storage Strategy**: Keys adhere to `collobster:feed:session:{sessionId}`. Stored payload contains only:
  - `sessionId` (UUID string), `userId` (Long), `createdAt` (ISO-8601 string)
  - `orderedContentRefs` (List of `{type, id}`)
  - `candidateSources` (Map of `"{type}:{id}" -> ["FOLLOWING", "PUBLIC_FALLBACK"]`)
- **Strict Prohibition**: Zero post bodies, user profiles, or entity rows are stored in Redis.
- **TTL**: 20 minutes (1200 seconds) enforced natively via Redis expiration.
- **Actual Measured Redis Memory Usage (`MEMORY USAGE` command on Redis 7.2.4)**:
  - **20 Candidates**:
    - Serialized JSON Payload Size: **3,076 bytes** (3.00 KB)
    - Actual Redis `MEMORY USAGE`: **3,672 bytes** (3.59 KB)
    - Redis Object / Dict Overhead: 596 bytes
  - **500 Candidates**:
    - Serialized JSON Payload Size: **72,996 bytes** (71.29 KB)
    - Actual Redis `MEMORY USAGE`: **82,008 bytes** (80.09 KB)
    - Redis Object / Dict Overhead: 9,012 bytes
- **Local Redis Configuration Audit**:
  - `CONFIG GET maxmemory`: `{'maxmemory': '0'}` (unconstrained memory on local dev instance).
  - `CONFIG GET maxmemory-policy`: `{'maxmemory-policy': 'noeviction'}`.
  - Local settings are documented as local evidence only and are not inferred as production topology.

### B. Actual Redis Key Inventory & Eviction Sensitivity Matrix
- **Actual Key Inventory (Audited from Source Code)**:
  1. `FeedSession`: `collobster:feed:session:{sessionId}` (String / JSON, TTL 20m)
  2. `Presence Sessions`: `collobster:messages:presence:sessions:{userId}` (ZSET of sessionIds scored by expiry, TTL 60s)
  3. `Presence Status`: `collobster:messages:presence:{userId}` (String "ONLINE", TTL 60s)
  4. `Typing State`: `collobster:messages:typing:{conversationId}` (SET of userIds, TTL 10s)
  5. `Rate Limit (Messaging)`: `collobster:messages:ratelimit:{userId}` (INCR counter, TTL 60s)
  6. `Rate Limit (Communities/Platform)`: IP and User token bucket keys (INCR counters, TTL 60s)
  7. `Online Features (Python)`: `online_features:user:{user_id}` (HASH)
  8. `User Interactions (Python)`: `user:{user_id}:interactions:*` (HASH/SET/STRING for derived signals)

| Key Family | Pattern / Example | TTL | Eviction Sensitivity | Consequence of Eviction |
| :--- | :--- | :--- | :--- | :--- |
| **FeedSession** | `collobster:feed:session:{sessionId}` | 20m (1200s) | **Low** | Pagination cursor returns `FeedSessionExpiredException` / `HTTP 410 Gone`. Client then initiates fresh pull-to-refresh without cursor. Benign UX degradation. |
| **Presence** | `collobster:messages:presence:sessions:{userId}`<br>`collobster:messages:presence:{userId}` | Persistent / Heartbeat-driven | **Medium** | Active users temporarily appear offline in UI indicators until next heartbeat or WebSocket frame. Messages still deliver via PostgreSQL and push notifications. Self-healing; messaging durability is completely unaffected. |
| **RateLimitBucket** | `rate:limit:ip:{ip}`<br>`rate:limit:user:{userId}`<br>`collobster:messages:ratelimit:{userId}` | 60s (window) | **High** | Evicted counter gives an abusive client a fresh quota early, temporarily weakening rate limiting / brute-force protection during memory pressure. |

### C. Revocation Architecture Truth (No Persistent Redis Key)
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

### D. Logical Database Isolation Truth (DB 0 vs DB 1)
- Logical Redis databases (e.g. DB 0 vs DB 1) share the exact same single-threaded Redis process, memory space, `maxmemory` cap, eviction policy, and CPU. They provide zero memory or policy isolation.
- If security-critical state (e.g. rate-limiting) requires strict isolation from disposable feed cache traffic, deploy a separate Redis instance/resource boundary.

### E. Workload-Driven Production Sizing Guidance
- Universal mandates (e.g. "must configure volatile-lru and >= 2 GB") are inaccurate.
- Final production configuration must be workload-driven:
  1. **Workload Separation**: If rate limiting and presence share the same Redis instance as Feed sessions, `volatile-lru` or `allkeys-lru` will evict rate-limit keys or presence data when Feed sessions spike. Deploying a separate Redis instance for rate limiting and security state is recommended if workload justifies it.
  2. **Memory Sizing**: Factoring in jemalloc fragmentation (20-30%), other application keyspaces, replication output buffers (`client-output-buffer-limit`), AOF rewrite buffers, and 50% operational headroom, 10,000 active 500-candidate sessions (~782 MB keyspace) suggest $\ge 2 \text{ GB}$ dedicated memory as a reasonable planning baseline, subject to operational workload measurements.

---

## 9. WebSocket Session Reaper Scaling
- **Batched Auth Reconciliation**: Sweep executes on a 5-second `fixedDelay` cadence.
- **Database Query Chunking**: Active user IDs are chunked into slices of 500 when querying `userRepository.findAuthStatesByUserIds`, preventing oversized `IN (...)` parameter lists.
- **In-Memory Map Bounding**: `pruneRevokedSessionIds()` runs each cycle to evict entries past their 30-minute retention window from `revokedSessionIds`.
- **Verified Scale**: 500-ID chunking removes unbounded IN-clause growth; verified at 1,250 sessions. Execution completes in under 20ms in `WebSocketSessionReaperLoadTest`.

---

## 10. PostgreSQL Connection Capacity & Pooling Guidance
- **Database Server Limits**:
  - `max_connections`: **100**
  - `superuser_reserved_connections`: **3**
  - **Normal Connection Usable Ceiling**: $100 - 3 = \mathbf{97}$ usable application connection slots.
- **Corrected Connection Capacity Sizing Math (Example 3-Node Model)**:
  - 3 Java Application Nodes $\times$ Hikari max pool size 20 = **60 connections**
  - Python Recommendation Service Pool: `pool_size = 10`, `max_overflow = 5` $\implies$ **15 connections** maximum checked-out capacity
  - **Total Active Application Maxima**: $60 + 15 = \mathbf{75}$ connections
  - **Required Operational Headroom**: **22** unallocated slots (22.7% planning target)
  - **Exact Sizing Balance**: $\mathbf{75 + 22 = 97}$ connections
- **Canonical Multi-Node Capacity Formula**:
  $$\text{applicationPoolMaximum} + \text{requiredOperationalHeadroom} \le \text{normalConnectionCapacity}$$
  where $\text{normalConnectionCapacity} = \text{max\_connections} - \text{superuser\_reserved\_connections} = 97$.
  In expanded topology terms:
  $$\text{javaNodes} \times \text{HikariMax} + \text{pythonInstances} \times \text{PythonMaxCheckedOut} + \text{otherApplicationPools} + \text{requiredOperationalHeadroom} \le 97$$
- **Headroom is Topology-Dependent (Not Universal)**:
  - The 22 unallocated slots apply specifically to the 3-node reference deployment ($75 + 22 = 97$).
  - Headroom is a planning target, not a separate connection pool. It must never be consumed by steady-state application pools. It absorbs transient administrative CLI sessions (`psql`), monitoring probes, health checks, and startup migration overhead.
  - If Java node counts, Hikari pool sizes, or Python worker processes change, the connection budget must be recalculated using the canonical formula.
- **Component Invariants**:
  1. **Reaper Connections**: `WebSocketSessionReaper` executes inside the Java application's existing Hikari connection pool via Spring Data JPA. It does NOT create a separate database connection pool.
  2. **Flyway Migrations**: Migrations run during startup before serving traffic and release their connection upon completion. They utilize temporary headroom from the unallocated slots, not permanent simultaneous capacity.
- **Connection Pooling & PgBouncer Guidance**:
  Re-evaluate connection pooling when projected aggregate application pool capacity approaches the PostgreSQL connection budget. Options include reducing per-node pool sizes, increasing database capacity where justified, or deploying PgBouncer in transaction pooling mode.

---

## 11. Java-to-Python HTTP Bulkhead Architecture & Concurrency Bounds
### A. HttpClient Executor vs. Concurrency Finding
- **Audit Result**: Configuring `HttpClient.newBuilder().executor(Executors.newFixedThreadPool(N))` specifies the thread pool used by `HttpClient` internally for asynchronous selector processing and response dispatching. It does NOT bound the number of concurrent outbound HTTP requests or TCP connections initiated synchronously by callers of Spring's `RestClient`.
- Without an explicit bulkhead, 100 incoming Feed requests could simultaneously dispatch 100 HTTP requests to the Python service.

### B. Explicit Semaphore Bulkheads
- Implemented explicit bounded-concurrency mechanisms using `java.util.concurrent.Semaphore`:
  1. **Feed Ranking Bulkhead** (`HttpRecommendationClient`):
     - Semaphore size: `collobster.recommendation.max-concurrent-requests` (default: **20**).
     - Acquire timeout: `collobster.recommendation.bulkhead-acquire-timeout-ms` (default: **250 ms**).
     - Saturated behavior: Throws `RecommendationBulkheadFullException`. `RecommendationFeedRankingStrategy` catches this exception and falls back immediately to `BaselineFeedRankingStrategy`. User receives deterministic heuristic feed with 200 OK (zero user-visible 500 errors).
  2. **Feedback Ingestion Bulkhead** (`HttpRecommendationFeedbackClient`):
     - Semaphore size: `collobster.recommendation.feedback.max-concurrent-requests` (default: **10**).
     - Acquire timeout: `collobster.recommendation.feedback.bulkhead-acquire-timeout-ms` (default: **500 ms**).
     - Saturated behavior: Throws `RecommendationException`. `RecommendationFeedbackWorker` catches the exception and schedules an outbox retry without blocking Feed traffic.
  3. **Privacy Erasure Bulkhead** (`HttpRecommendationPrivacyClient`):
     - Externalized via `RecommendationPrivacyProperties`.
     - Semaphore size: `collobster.recommendation.privacy.max-concurrent-requests` (default: **5**).
     - Acquire timeout: `collobster.recommendation.privacy.bulkhead-acquire-timeout-ms` (default: **1000 ms**).
     - Saturated behavior: Throws `RecommendationException`. `UserDataErasureWorker` catches the exception and schedules outbox retry.
- **Permit Leak Safety**:
  - All permits are acquired with timeout before entering transport and released in unconditional `finally` blocks:
    ```java
    boolean acquired = bulkhead.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
    if (!acquired) throw new RecommendationBulkheadFullException(...);
    try {
        return executeCall();
    } finally {
        bulkhead.release();
    }
    ```
  - Unit tests prove zero permit leakage across successful calls, HTTP 500 errors, network timeouts, and repeated failures.

### C. Configuration Alias Cleanup
- In `RecommendationProperties` and `RecommendationFeedbackProperties`, the legacy `maxConnections` property getter/setter is annotated with `@Deprecated` and documented as a backwards-compatible alias in favor of the canonical `maxConcurrentRequests` property.

### D. Client Reuse Evidence Note
- Unit tests verify singleton lifecycle: identical `RestClient`, `HttpClient`, and `JdkClientHttpRequestFactory` instances are preserved across requests.
- Unit tests do NOT claim proof of TCP socket reuse. Proof of socket reuse requires network-level or integration observation of TCP connection identity.

---

## 12. FastAPI CPU-Bound Inference Model & Server-Side Inference Bulkhead
### A. Route Execution & Inference Bulkhead
- **Route Execution**: `internal_rank_route.py` defines `rank_candidates` as a synchronous endpoint (`def`), offloading execution to Starlette's AnyIO worker threadpool and keeping the main asyncio event loop unblocked.
- **Server-Side Inference Bulkhead & Bounded Queue**:
  - In `InternalFeedRankingService`, model prediction is guarded by a process-level semaphore:
    ```python
    acquired = self._inference_semaphore.acquire(timeout=self._inference_acquire_timeout_sec)
    if not acquired:
        raise InferenceQueueSaturatedException(...)
    try:
        raw_scores = self._model.predict(matrix, num_threads=1)
    finally:
        self._inference_semaphore.release()
    ```
  - Configured via `RANKING_MAX_CONCURRENT_INFERENCES` (default: **4**).
  - Bounded permit acquire timeout: `RANKING_INFERENCE_ACQUIRE_TIMEOUT_MS` (default: **500 ms**).
  - LightGBM prediction explicitly maintains `num_threads=1` to eliminate OpenMP thread oversubscription.
  - If inference permits cannot be acquired within timeout, `InferenceQueueSaturatedException` is raised, and the route immediately returns **HTTP 503 Service Unavailable**.
  - Java receives HTTP 503, caught by `RecommendationFeedRankingStrategy` $\rightarrow$ activates deterministic `BaselineFeedRankingStrategy` fallback (zero user-visible 500 errors).
- **Multi-Worker Capacity Model & Empirical Tuning**:
  - A process-level semaphore limits concurrency per worker process. Total server-side inference capacity across a deployment equals:
    $$\text{Deployment Inference Concurrency} \approx \text{Uvicorn Workers} \times \text{RANKING\_MAX\_CONCURRENT\_INFERENCES}$$
  - **Empirical Tuning Rule**: Matching this number to CPU core count does not automatically guarantee optimal throughput. Total worker and concurrency limits must be tuned empirically against CPU utilization, latency, and throughput in the target deployment environment.

### B. Event-Loop Responsiveness Under Load (Phase 11.3 Verification)
- *Workload*: 5 concurrent ranking workers issuing 500-candidate ranking requests against the Uvicorn daemon (164 large ranking requests completed during probe window).
- *Probes*: 50 concurrent `/health` probe requests.
- *Results*:
  - **Health Probes Sent**: 50 | **Successful (200 OK)**: **50 (100%)** | **Failures**: **0**
  - **Health p50 Latency**: **46.16 ms** (idle baseline: **3.53 ms**)
  - **Health p95 Latency**: **137.56 ms**
  - **Health p99 Latency**: **161.58 ms**
- *Truthful Assessment*: The event loop remained available and error-free throughout heavy inference load, with measurable latency degradation under CPU load.

---

## 13. Recommendation Service Performance & Concurrency Benchmark
- **Singleton Model Lifecycle**: LightGBM model is instantiated once upon service startup as a module-level singleton in `get_feed_ranking_service()`. Zero per-request disk reloading.
- **Benchmark Request ID Integrity**: Every benchmark request dynamically generates a unique UUID `requestId`. Zero correlation ID reuse across requests.
- **Documented Percentile Methodology**: Nearest-rank formula: $\text{index} = \max(0, \min(\lceil p \times N \rceil - 1, N - 1))$.
- **Throughput Accounting**: Attempted RPS and Successful RPS are explicitly separated. All tiers achieved 100% success rate (0 failures).

### A. Single-Request Sequential Ranking Benchmark (Unique Request IDs)
- **20 candidates / request**: Payload: 7.2 KB | 30 requests | p50: **9.74 ms** | p95: **11.88 ms** | p99: **28.22 ms**
- **50 candidates / request**: Payload: 17.8 KB | 30 requests | p50: **10.02 ms** | p95: **11.41 ms** | p99: **13.01 ms**
- **500 candidates / request**: Payload: 176.8 KB | 30 requests | p50: **39.95 ms** | p95: **139.00 ms** | p99: **139.04 ms**

### B. Actual Concurrent Ranking Benchmark (Unique Request IDs)
| Candidates / Request | Concurrency | Total Requests | Success (200 OK) | Failures | p50 (ms) | p95 (ms) | p99 (ms) | Attempted RPS | Successful RPS |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **20 candidates** | 1 | 30 | 30 | 0 | 8.49 | 10.10 | 13.66 | 116.1 | **116.1** |
| **20 candidates** | 10 | 50 | 50 | 0 | 71.05 | 688.83 | 705.82 | 50.4 | **50.4** |
| **20 candidates** | 25 | 75 | 75 | 0 | 172.94 | 304.00 | 327.13 | 122.5 | **122.5** |
| **500 candidates** | 1 | 20 | 20 | 0 | 41.83 | 123.85 | 147.97 | 19.3 | **19.3** |
| **500 candidates** | 5 | 30 | 30 | 0 | 232.08 | 335.92 | 341.14 | 20.5 | **20.5** |
| **500 candidates** | 10 | 40 | 40 | 0 | 496.04 | 574.10 | 633.83 | 18.7 | **18.7** |

---

## 14. Measured Response Payload Sizing & Compression Evidence
*Label: Synthetic representative serialized payload measurement.* Measured on representative serialized DTO structures with standard Gzip compression:
- **Feed Page (20 items)**: 19,646 bytes (19.19 KB) uncompressed $\rightarrow$ **2,105 bytes (2.06 KB)** Gzip (89.3% reduction)
- **Feed Page (50 items)**: 48,974 bytes (47.83 KB) uncompressed $\rightarrow$ **4,299 bytes (4.20 KB)** Gzip (91.2% reduction)
- **Direct Message Page (20 messages)**: 6,947 bytes (6.78 KB) uncompressed $\rightarrow$ **1,437 bytes (1.40 KB)** Gzip (79.3% reduction)
- **Notification Page (20 items)**: 7,302 bytes (7.13 KB) uncompressed $\rightarrow$ **1,550 bytes (1.51 KB)** Gzip (78.8% reduction)
- **HTTP Compression**: Configured in `application.properties` with `server.compression.min-response-size=2048` to avoid CPU penalty on small payloads (<2KB).

---

## 15. Security, Secrets & Regression Integrity
- **Zero Token Disclosure**: Neither the leaked internal token nor the active internal token is displayed in logs, test reports, or benchmark outputs.
- **Token Rotation Regression**:
  - `OLD_TOKEN_REJECTED = True` (HTTP 401 Unauthorized returned for revoked token).
  - `NEW_TOKEN_ACCEPTED = True` (HTTP 200 OK returned for rotated token).
- **Security & Privacy Regression Suite**:
  - `AccountWideSessionRevocationTest`: **26 passed, 0 failed**
  - `WebSocketSessionReaperLoadTest`: **2 passed, 0 failed**
  - `HttpRecommendationClientTest`: **12 passed, 0 failed**
  - `RecommendationCircuitBreakerTest`: **11 passed, 0 failed**
  - `RecommendationFeedRankingStrategyTest`: **12 passed, 0 failed**
- **Model Integrity**: LightGBM model SHA-256 matches exact hash `25944E97AFE3194DF82AD2EAE9FF738DEF2456D1EEC929B267BF663D259C5B0B` (0 retraining).

---

## 16. Production Readiness & Remaining Limitations
- **Verification Status**: Phase 11.3 performance, bulkhead, connection capacity, and complexity gate passed.
- **Read Replicas**: Introduce read replicas when measured primary read utilization, latency, connection pressure, or replica-worthy workload justifies it.
- **Redis High Availability**: Standalone Redis remains a known deployment limitation. Sentinel/Cluster choice depends on production topology and operational requirements.
- **Cross-Node Token Security**: `COLLOBSTER_RECOMMENDATION_INTERNAL_TOKEN` rotated and loaded strictly from secure local environment variables. Zero hardcoded secrets in source or repository history.
- **PgBouncer Deployment**: Evaluate when projected aggregate pool capacity approaches database capacity constraints.
