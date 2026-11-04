# Recommendation System User Data Inventory (Phase 10.2 Hardening)

## 1. Overview & System Boundary
The Collobster Recommendation System (`Recommendation-System/Recommendation-System`) operates as an auxiliary machine learning scoring and feature service. It receives behavioral feedback occurrences from the authoritative Spring Boot backend and generates candidate scores for feed and people recommendations.

This inventory provides an audited, rigorous catalog of all persistent, semi-persistent, cached, in-memory, and file-based stores within the recommendation engine that retain user-linked identifiers (`user_id`, `viewer_id`, account references).

**Compliance & Privacy Scope Statement**:
This system is designed to support user-data deletion requirements and implements the documented user-data erasure lifecycle. It does not assert certified legal compliance conclusions outside this engineering specification.

---

## 2. Comprehensive Store-by-Store Privacy Inventory

Each store is cataloged with the mandatory six operational attributes:
1. **Production Status**
2. **Retention Policy**
3. **Erasure Mechanism**
4. **Erasure Timing**
5. **Owner**
6. **Verification Test**

---

### Store 1: `recommendation_events` (PostgreSQL)
- **Production Status**: Active Production Store (Authoritative Recommendation Event Sink).
- **Owner**: Python Recommendation Engine (`RecommendationEventRepository`, `DatabaseSession`).
- **User Identifier**: `user_id` (BigInteger / PostgreSQL `BIGINT`).
- **Purpose**: Records historical interaction events (`VIEW`, `LIKE`, `SAVE`, `SHARE`, `WATCH_TIME`, `HIDE`, `NOT_INTERESTED`) for model training, candidate generation, and offline feature compilation.
- **Retention**: Indefinite until erased by user account deletion or administrative lifecycle request.
- **Erasure Mechanism**: Synchronous relational row deletion via `DELETE FROM recommendation_events WHERE user_id = :userId`. Only deletes rows where `event.user_id == :userId`. Content created by the user but interacted with by other users (e.g. `content_id == user_content`) is preserved to avoid corrupting other users' interaction graphs while scrubbing direct identifiers.
- **Erasure Timing**: Realtime / synchronous upon receipt of authenticated internal erasure dispatch (`POST /api/v1/internal/recommend/privacy/erase`).
- **Verification Test**:
  - Python: `tests/test_recommendation_privacy.py::test_erase_user_data_deletes_pg_and_redis`
  - Java Live Integration: `LivePythonPrivacyErasureIntegrationTest`

---

### Store 2: `user:{user_id}:interactions:*` (Redis Realtime Interaction State)
- **Production Status**: Active Production Cache.
- **Owner**: Python Recommendation Engine (`RedisUserInteractionRepository`).
- **User Identifier**: `user_id` (integer formatted in Redis key namespace).
- **Purpose**: Low-latency interaction state (recent like sets, skip sets, save sets, tag affinity weights, community affinity weights, total interaction counts) used for realtime candidate reranking and deduplication.
- **Retention**: Ephemeral / Dynamic rolling activity window.
- **Erasure Mechanism**: Deterministic multi-key deletion pipeline using explicit key namespaces (`user:{user_id}:interactions:initialized`, `user:{user_id}:interactions:counts`, `user:{user_id}:interactions:last_type`, `user:{user_id}:interactions:last_post`, `user:{user_id}:interactions:posts:like`, `user:{user_id}:interactions:posts:save`, `user:{user_id}:interactions:posts:skip`, `user:{user_id}:interactions:tags:*`, `user:{user_id}:interactions:communities`). Strictly avoids unbounded `KEYS *` operations in production.
- **Erasure Timing**: Realtime / synchronous during `POST /api/v1/internal/recommend/privacy/erase`.
- **Verification Test**:
  - Python: `tests/test_recommendation_privacy.py::test_erase_user_data_deletes_pg_and_redis`
  - Java Live Integration: `LivePythonPrivacyErasureIntegrationTest`

---

### Store 3: `online_features:user:{user_id}` (Redis Feature Store)
- **Production Status**: Active Production Cache.
- **Owner**: Python Recommendation Engine (`OnlineFeatureStore`).
- **User Identifier**: `user_id` (integer in key namespace).
- **Purpose**: Online user feature vector cache (e.g., activity counts, preferred categories) consulted during LightGBM inference.
- **Retention**: Ephemeral with TTL or rolling update window.
- **Erasure Mechanism**: Immediate key deletion via Redis `DEL online_features:user:{user_id}`.
- **Erasure Timing**: Realtime / synchronous during `POST /api/v1/internal/recommend/privacy/erase`.
- **Verification Test**:
  - Python: `tests/test_recommendation_privacy.py::test_erase_user_data_deletes_pg_and_redis`

---

### Store 4: `feed:session:{session_id}:meta` (Redis Candidate Session Cache)
- **Production Status**: Active Production Cache.
- **Owner**: Python Recommendation Engine (`RedisFeedSessionRepository`).
- **User Identifier**: `"user_id"` (string field in Hash).
- **Purpose**: Stores pagination cursor, candidate pool IDs, and user association for an active feed viewing session.
- **Retention**: Bounded TTL of 7,200 seconds (2 hours) from session creation.
- **Erasure Mechanism**: Natural TTL expiration. No personal profile data or interaction history is stored in this hash—only candidate post UUID lists.
- **Erasure Timing**: Automatic eviction upon TTL expiration (max 2 hours).
- **Verification Test**:
  - Python: `tests/test_feed_session.py`

---

### Store 5: `event_stream.jsonl` / `interaction_events.jsonl` (JSONL Event Stream)
- **Production Status**: **Development / Test Only (Explicit Option A Policy)**. Prohibited in Production.
- **Owner**: Python Recommendation Engine (`JsonlEventPublisher`).
- **User Identifier**: `user_id` (JSON field in event record).
- **Purpose**: Local disk event logging for standalone development and offline debugging without requiring PostgreSQL/Kafka infrastructure.
- **Retention**:
  - **Production Policy (Option A)**: In production (`APP_ENV=production`), `EVENT_PUBLISHER=jsonl` is strictly blocked at startup via validation in `app/events/factory.py`, preventing any production user data from being written to un-erased JSONL flat files.
  - **Non-Production Retention**: Development files rotate or are cleared between runs. Bounded retention and compaction are provided by `JsonlRetentionManager` (retention default: 48 hours).
- **Erasure Mechanism**: In development/testing environments, `JsonlRetentionManager.erase_user_events(user_id)` performs atomic rewriting to temporary file and replacement, stripping all lines matching `user_id`. Production deployments use PostgreSQL event tables and do not write to JSONL.
- **Erasure Timing**: Bounded compaction / immediate upon test teardown or development erasure request.
- **Verification Test**:
  - Python: `tests/test_offline_privacy.py::test_jsonl_retention_and_erasure`
  - Python: `tests/test_offline_privacy.py::test_jsonl_production_guard`

---

### Store 6: Parquet Data Lake (`data_lake/recommendation_events/**/*.parquet`)
- **Production Status**: Active Offline Storage (Date-Partitioned Immutable Analytical Snapshots).
- **Owner**: Python Offline Data Pipeline (`OfflineParquetCompactor`, `PrivacyErasureManifest`).
- **User Identifier**: `user_id` (int64 Parquet column).
- **Purpose**: Historical training datasets partitioned by date (e.g. `year=YYYY/month=MM/day=DD`) used for training future recommendation ranking models.
- **Retention**: Historical immutable snapshots retained according to offline data retention policy (typically 90–365 days).
- **Erasure Mechanism**:
  1. **Immediate Deletion Tombstone**: `POST /api/v1/internal/recommend/privacy/erase` records a durable tombstone in `data_lake/privacy_manifests/privacy_erasure_manifest.json` (`status = PENDING`).
  2. **Pre-Compaction Training Guard**: Future model training loaders (`TrainingDataLoader`) inspect active tombstones and strictly exclude any user with a `PENDING` or `COMPLETED` tombstone, preventing newly trained models from consuming data already requested for deletion.
  3. **Atomic Partition Compaction**: Scheduled batch compactor (`OfflineParquetCompactor`) scans affected Parquet partitions, filters out rows where `user_id == :userId` using `pyarrow.compute`, writes to atomic temporary files (`.tmp`), and atomically renames over the target partition. All other users' rows are preserved intact. Marks manifest entry `COMPLETED`. Idempotent and retry-safe.
- **Erasure Timing**:
  - Deletion Tombstone: Synchronous with erasure dispatch (< 50ms).
  - Training Exclusion: Immediate upon tombstone registration.
  - Physical Parquet Compaction: Asynchronous scheduled batch window (hourly/daily) or on-demand worker.
- **Verification Test**:
  - Python: `tests/test_offline_privacy.py::test_parquet_compaction_removes_user_and_preserves_others`
  - Python: `tests/test_offline_privacy.py::test_parquet_compaction_idempotency`
  - Python: `tests/test_offline_privacy.py::test_training_data_loader_excludes_tombstoned_user`

---

### Store 7: Model Training Manifest (`model_training_manifest.json` / `model_registry/training_runs/*.json`)
- **Production Status**: Active Model Metadata Registry.
- **Owner**: Python ML Training Pipeline (`TrainingManifest`).
- **User Identifier**: **None (Zero PII / Zero User Identifiers)**.
- **Purpose**: Records training hyperparameters, objective functions, feature lists, run IDs, and training dataset checksums to guarantee model reproducibility.
- **Retention**: Permanent audit artifact tied to model artifact lifecycle.
- **Erasure Mechanism**: **Retained Without Modification**. Formal PII audit confirmed that training manifests contain only aggregate metadata (e.g., `run_id`, `created_at`, `dataset`, `features`, `config`). They contain zero direct or indirect user identifiers.
- **Erasure Timing**: N/A (no user data present).
- **Verification Test**:
  - Python: `tests/test_offline_privacy.py::test_model_training_manifest_pii_audit`

---

### Store 8: Trained LightGBM Model Artifact (`lgbm_ranker_model_v2.txt`)
- **Production Status**: Active Production Inference Model (Checksum: `25944E97AFE3194DF82AD2EAE9FF738DEF2456D1EEC929B267BF663D259C5B0B`).
- **Owner**: Machine Learning Engineering.
- **User Identifier**: **None**. Tree ensembles contain split thresholds over mathematical feature aggregates (freshness, popularity, engagement counts), not stored personal records.
- **Retention**: Active until next scheduled model release cycle.
- **Erasure Mechanism**: Historical trained model artifact is unchanged during user deletion requests. Future model training runs exclude erased users via `TrainingDataLoader` and compacted Parquet datasets.
- **Erasure Timing**: Next scheduled model training cycle.
- **Verification Test**:
  - Checksum validation: Verified SHA-256 match.

---

### Store 9: Kafka Event Stream (Optional / Future Infrastructure)
- **Production Status**: **Not Enabled in Current Production Architecture**.
- **Owner**: Infrastructure / Event Streaming.
- **User Identifier**: N/A (disabled).
- **Purpose**: Optional decoupled event ingestion pipeline.
- **Retention**: N/A (not enabled).
- **Erasure Mechanism**: Documented invariant: Production privacy guarantees do not depend on unimplemented Kafka tombstones or topic compaction. Current production uses PostgreSQL outbox and table persistence.
- **Erasure Timing**: N/A.
- **Verification Test**: N/A.

---

## 3. Privacy Erasure Semantics & Two-Stage Lifecycle

### A. Java & Python Completion Semantics
The term `COMPLETED` across the boundary is strictly defined:
- **Java Outbox Job (`status = COMPLETED`)**: Means that the authoritative Java transaction has anonymized user PII, deleted refresh/push tokens, and successfully dispatched the erasure request to the Python recommendation service, which confirmed that:
  1. Online PostgreSQL interaction records are hard-deleted (`postgresEventsDeleted >= 0`).
  2. Online Redis caches and feature keys are purged (`redisKeysDeleted >= 0`).
  3. Offline erasure tombstone is durably recorded in `privacy_erasure_manifest.json` (`offlineErasureScheduled = true`).
- **Python Offline Compaction (`status = COMPLETED`)**: Means that physical Parquet partitions have been rewritten and verified free of the target user's records.

### B. Privacy Erasure Response Contract
The internal privacy endpoint (`POST /api/v1/internal/recommend/privacy/erase`) returns structured breakdown:
```json
{
  "status": "COMPLETED",
  "userId": 12345,
  "deletedEventsCount": 15,
  "postgresEventsDeleted": 15,
  "purgedRedisKeysCount": 4,
  "redisKeysDeleted": 4,
  "offlineErasureScheduled": true,
  "offlineErasureStatus": "PENDING",
  "details": "Erased 15 PostgreSQL events, purged 4 Redis keys, and scheduled offline Parquet compaction for user 12345"
}
```

This ensures truthful reporting: the endpoint does NOT claim downstream physical Parquet files are immediately rewritten synchronously; it accurately reports that online stores are erased and offline compaction is durably scheduled.
