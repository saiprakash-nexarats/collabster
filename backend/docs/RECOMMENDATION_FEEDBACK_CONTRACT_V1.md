# RECOMMENDATION FEEDBACK CONTRACT V1

**Status:** APPROVED & ACTIVE  
**Contract Version:** `1`  
**Endpoint:** `POST /api/v1/internal/recommend/feedback/batch`  
**Authoritative Source:** Java Application Backend (`collabster-communities-module`)  
**Consumer:** Python Recommendation Subsystem (`Recommendation-System`)

---

## 1. Architectural Principles & Guarantees

1. **Zero Client Direct Access:**
   Mobile clients and frontend web applications **NEVER** call the Python recommendation service directly. All user actions (likes, saves, views, hides, shares, watch times) are submitted exclusively to Java backend endpoints (`/api/v1/interactions/...`).
2. **Asynchronous Non-Blocking Pipeline:**
   User-facing interaction latency is completely decoupled from Python service availability. All events are transactionally committed to PostgreSQL (`recommendation_feedback_outbox`) within the authoritative business transaction. An asynchronous worker process batches and forwards events to Python.
3. **Idempotence & At-Least-Once Delivery:**
   Every event carries a unique `eventId` (UUIDv4). Delivery is at-least-once. The Python ingestion pipeline processes events idempotently: duplicate `eventId` submissions are safely detected and skipped without error.
4. **Surface and Entity Isolation:**
   Every event includes a strongly typed `contentRef` (`contentType` + `contentId`). A platform post and community post sharing the same UUID value are completely isolated and never collated or confused.
5. **Contract Versioning:**
   Every payload envelope declares `contractVersion: "1"`. Payloads with unknown or unsupported contract versions are rejected with `INVALID_CONTRACT_VERSION`.

---

## 2. Authentication & Headers

Internal communication between the Java backend and the Python recommendation service is authenticated via a shared Bearer token.

| Header | Type | Value / Description | Required |
|---|---|---|---|
| `Authorization` | String | `Bearer <REC_FEEDBACK_INTERNAL_API_KEY>` | Yes |
| `Content-Type` | String | `application/json` | Yes |
| `Accept` | String | `application/json` | Yes |

---

## 3. Ingestion Request Schema

### 3.1 Batch Request Envelope

```json
{
  "contractVersion": "1",
  "batchId": "65b93d39-6547-4976-b328-971c66708ab9",
  "sentAt": "2026-09-29T12:00:00Z",
  "events": [
    {
      "eventId": "c928a3f1-d0e5-4f46-81a1-9a70650965e5",
      "eventType": "LIKE",
      "occurredAt": "2026-09-29T11:59:58Z",
      "userId": 1042,
      "contentRef": {
        "contentType": "PLATFORM_POST",
        "contentId": "0b15da32-720c-4fa2-bc4a-9db10aa4a66e"
      },
      "context": {
        "surface": "FEED",
        "sessionId": "sess-9912",
        "position": 3,
        "recommendationScore": 0.875
      },
      "durationMs": null
    }
  ]
}
```

### 3.2 Field Specifications

#### Root Envelope (`RecommendationFeedbackBatchRequest`)
- `contractVersion` (String, required): Must equal `"1"`.
- `batchId` (UUID string, optional): Unique ID representing the dispatch batch.
- `sentAt` (ISO-8601 string, required): Timestamp when the batch was dispatched.
- `events` (List, required): List of events (minimum 1, maximum 500 per batch).

#### Event Item (`RecommendationEventDto`)
- `eventId` (UUID string, required): Unique identifier generated at occurrence time. Used for idempotent deduplication.
- `eventType` (String, required): One of:
  - `LIKE` — User positively liked content.
  - `UNLIKE` — User removed a previously placed like.
  - `SAVE` — User bookmarked/saved content.
  - `UNSAVE` — User removed a bookmark/saved item.
  - `SHARE` — User shared content externally or internally.
  - `VIEW` — Validated impression/view (outside deduplication window).
  - `WATCH_TIME` — Video / media consumption duration.
  - `HIDE` — Explicit negative signal: hide this specific post.
  - `NOT_INTERESTED` — Explicit negative signal: do not recommend similar content.
- `occurredAt` (ISO-8601 string, required): UTC timestamp when the user action occurred.
- `userId` (Long, required): Authoritative user ID in PostgreSQL `users` table.
- `contentRef` (Object, required):
  - `contentType` (String, required): `PLATFORM_POST`, `COMMUNITY_POST`, `RESOURCE`, `PROJECT`.
  - `contentId` (UUID string, required): Entity ID in its respective bounded context.
- `context` (Object, optional):
  - `surface` (String, optional): `FEED`, `EXPLORE`, `COMMUNITY`, `SEARCH`, `DIRECT_LINK`, `EXTERNAL`.
  - `sessionId` (String, optional): Client browsing session identifier.
  - `position` (Integer, optional): Zero-indexed feed/list slot position where item was rendered.
  - `recommendationScore` (Double, optional): Original model inference score if served via recommendation engine.
- `durationMs` (Long, optional): Milliseconds consumed. Required for `WATCH_TIME` (must be between 1 and 86,400,000 ms). Must be `null` or 0 for other event types.

---

## 4. Ingestion Response Schema

### 4.1 Response Envelope

```json
{
  "status": "SUCCESS",
  "batchSize": 1,
  "acceptedCount": 1,
  "rejectedCount": 0,
  "results": [
    {
      "eventId": "c928a3f1-d0e5-4f46-81a1-9a70650965e5",
      "status": "ACCEPTED",
      "errorCode": null,
      "errorMessage": null
    }
  ]
}
```

### 4.2 Status Enums & Error Codes

#### Batch Status (`status`)
- `SUCCESS`: All events in the batch were accepted (or deduplicated as idempotent no-ops).
- `PARTIAL_SUCCESS`: Some events were accepted, and some events were permanently rejected (e.g. malformed or invalid).
- `FAILED`: Entire batch could not be processed (e.g. database connectivity error or batch limit exceeded).

#### Item Status (`results[i].status`)
- `ACCEPTED`: Successfully ingested or recognized as already ingested (idempotent).
- `REJECTED`: Permanent client-side error. Will not be retried by Java worker.

#### Item Error Codes (`errorCode`)
- `MALFORMED_PAYLOAD`: Missing required fields, invalid UUID format, or parse error.
- `INVALID_EVENT_TYPE`: Event type not supported in contract v1.
- `INVALID_CONTENT_REF`: Missing content type or content ID.
- `INVALID_DURATION`: `WATCH_TIME` event missing or has out-of-bounds duration (`<= 0` or `> 86400000`).
- `INVALID_TIMESTAMP`: `occurredAt` is in the future (> 5 minutes clock skew) or unparseable.
- `UNAUTHORIZED`: Invalid or missing Bearer token (HTTP 401).
- `RATE_LIMITED`: Ingestion rate limit exceeded (HTTP 429).

---

## 5. Storage Schema (Python `recommendation_events`)

```sql
CREATE TABLE recommendation_events (
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

---

## 6. Worker Delivery & Retry Semantics

1. **Transactional Enqueue:** Java services enqueue outbox records in the same DB transaction that mutates authoritative state (`content_likes`, `content_saves`, `content_interaction_events`).
2. **Worker Polling:** Background worker claims up to 100 `PENDING` / `RETRY` records using `SELECT ... FOR UPDATE SKIP LOCKED` inside a short transaction and sets status to `PROCESSING` with a lease timestamp.
3. **HTTP Dispatch:** Dispatches HTTP `POST` batch outside DB locks to Python `/api/v1/internal/recommend/feedback/batch`.
4. **Response Processing:**
   - For `ACCEPTED` items: marks status `SENT`, clears `lock_owner`.
   - For `REJECTED` items: marks status `DEAD_LETTER`, records `last_error_code` and `last_error_message`.
   - For Network / 5xx failures: increments `attempt_count`, applies exponential backoff (`next_attempt_at`), marks `RETRY`. If `attempt_count >= max_retries` (default 5), transitions to `DEAD_LETTER`.
5. **Stale Lock Recovery:** If a worker crashes while processing, locks older than `lockLeaseSeconds` (default 60s) automatically become claimable by surviving worker nodes. Outdated completion callbacks verify lock ownership to prevent duplicate delivery.
6. **Retention:** Background cleanup task purges `SENT` records older than `retentionDays` (default 7 days).
