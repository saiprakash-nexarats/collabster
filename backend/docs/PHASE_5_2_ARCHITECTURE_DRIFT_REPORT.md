# COLLOBSTER BACKEND — PHASE 5.2 ARCHITECTURE DRIFT & RESTORATION REPORT

## EXECUTIVE SUMMARY

During Phase 5.1 Explore hardening, the recommendation resilience circuit breaker was unexpectedly rewritten from its intended Phase 4 library-backed `resilience4j-circuitbreaker` implementation into a custom homemade sliding-window class. 

Phase 5.2 was executed to perform a thorough forensic audit of this architecture drift, identify the root cause, restore the official Resilience4j 2.2.0 library-backed circuit-breaker architecture, establish explicit failure classification policies, and lock down a comprehensive pre-messaging test baseline across all 330 Java tests, PostgreSQL 16 schema V23, Redis 7 caching/sessions, and the untouched Python recommendation service.

---

## 1. ROOT CAUSE OF CIRCUIT BREAKER REWRITE IN PHASE 5.1

### Forensic Analysis
During Phase 5.1, the Red Hat Java language server extension (`redhat.java`) inside the user's IDE automatically compiled workspace sources into `target/classes`. Because the Maven project lacked an Eclipse `.classpath` descriptor linking external dependencies to the language server's build path, the IDE's internal Eclipse/JDT compiler failed to resolve external imports for `io.github.resilience4j.circuitbreaker.CircuitBreaker`.

As a result, the IDE compiler generated a stub class throwing:
```
java.lang.Error: Unresolved compilation problems:
    io.github.resilience4j.circuitbreaker.CircuitBreaker cannot be resolved to a type
```
During test troubleshooting in Phase 5.1, this error was mistakenly treated as an external library conflict or classpath defect rather than an IDE-only build-path issue. To unblock test execution quickly, `RecommendationCircuitBreaker.java` was rewritten into a custom self-contained implementation that removed all external Resilience4j imports.

### Remediation
1. Executed `mvn eclipse:eclipse` to generate a valid `.classpath` file mapping `resilience4j-circuitbreaker-2.2.0.jar` and `resilience4j-core-2.2.0.jar` into the IDE's build path.
2. Deleted the homemade circuit-breaker code and restored the library-backed Resilience4j adapter.
3. Verified clean compilation under both JDK 21 `javac` via Maven Surefire and the IDE language server without errors.

---

## 2. FINAL CIRCUIT BREAKER ARCHITECTURE

The restored architecture cleanly encapsulates Resilience4j inside `RecommendationCircuitBreaker`:

```text
                     Java Feed Service (GET /api/v1/feed)
                                      ↓
                     RecommendationFeedRankingStrategy
                                      ↓
                          HttpRecommendationClient
                                      ↓
                 RecommendationCircuitBreaker (Resilience4j)
                 ┌─────────────────────────────────────────┐
                 │ State: CLOSED / HALF_OPEN / OPEN        │
                 │ Sliding Window: 5 calls (Count-based)   │
                 │ Failure Threshold: 50%                  │
                 │ Open Wait Duration: 10,000 ms           │
                 │ Half-Open Permitted Probes: 2           │
                 └─────────────────────────────────────────┘
                                      │
            ┌─────────────────────────┴─────────────────────────┐
       [Permitted]                                         [Rejected]
            ↓                                                   ↓
   FastAPI Recommendation Server                       Fast-Fail (0ms network)
 (POST /api/v1/internal/recommend/feed/rank)        RecommendationCircuitBreakerOpenException
            │                                                   │
            ├───────────────┬─────────────────┐                 │
         Success        Transient         Client 4xx            │
         (200 OK)       (5xx, Timeout)    (401, 422)            │
            │               │                 │                 │
     Record Success   Record Failure    Ignored by CB           │
            │               │                 │                 │
       Return Items         └────────┬────────┘                 │
            │                        ↓                          │
            │               RecommendationException             │
            │                        │                          │
            │                        └──────────┬───────────────┘
            ↓                                   ↓
   Order Feed Candidates               Fallback Gracefully
                                                ↓
                               BaselineFeedRankingStrategy
                               (Recency + Relationship + Likes)
                                                ↓
                               Return HTTP 200 FeedPageResponse
```

---

## 3. RESILIENCE4J CONFIGURATION

All circuit breaker properties are centralized in `RecommendationProperties.CircuitBreakerProperties` and exposed via Spring `application.properties`:

| Configuration Key | Property Field | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `collobster.recommendation.circuit-breaker.enabled` | `enabled` | `true` | Enables/disables circuit breaking entirely |
| `collobster.recommendation.circuit-breaker.sliding-window-size` | `slidingWindowSize` | `5` | Count-based sliding window call count |
| `collobster.recommendation.circuit-breaker.minimum-number-of-calls` | `minimumNumberOfCalls` | `3` | Minimum calls before evaluating failure threshold |
| `collobster.recommendation.circuit-breaker.failure-rate-threshold` | `failureRateThreshold` | `50.0%` | Failure percentage required to trip OPEN |
| `collobster.recommendation.circuit-breaker.wait-duration-in-open-state-ms` | `waitDurationInOpenStateMs` | `10000` | Duration (ms) before transitioning to HALF_OPEN |
| `collobster.recommendation.circuit-breaker.permitted-number-of-calls-in-half-open-state` | `permittedNumberOfCallsInHalfOpenState` | `2` | Number of probe calls evaluated in HALF_OPEN |

---

## 4. FAILURE CLASSIFICATION POLICY

### Rationale
A circuit breaker exists to protect latency and isolate downstream services undergoing **overload, resource starvation, or transient crashes**. It must NOT trip due to deterministic client/integration defects that affect only individual requests.

| Exception Category | Specific Errors | Circuit Health Impact | Feed Fallback Impact | Log Level & Security |
| :--- | :--- | :--- | :--- | :--- |
| **Transient Service Outage** | HTTP 500, 502, 503, 504 (`HttpServerErrorException`) | **RECORD AS FAILURE** | Safe fallback to Baseline ranking | `WARN` with duration and status code |
| **Network / Timeout** | Connection refused, connect/read timeout (`ResourceAccessException`) | **RECORD AS FAILURE** | Safe fallback to Baseline ranking | `WARN` with duration and error message |
| **Malformed Response** | Null body, non-finite scores, unparseable JSON | **RECORD AS FAILURE** | Safe fallback to Baseline ranking | `WARN` identifying malformed payload |
| **Contract / Validation Error** | HTTP 422 Unprocessable Entity (`HttpClientErrorException.UnprocessableEntity`) | **IGNORED BY CIRCUIT** | Safe fallback to Baseline ranking | `WARN` with detail; does not trip breaker |
| **Authentication Defect** | HTTP 401 Unauthorized (`HttpClientErrorException.Unauthorized`) | **IGNORED BY CIRCUIT** | Safe fallback to Baseline ranking | `WARN` with status; **internal secret token is NEVER logged** |

Implementation:
```java
CircuitBreakerConfig.custom()
    .failureRateThreshold(cbProps.getFailureRateThreshold())
    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
    .slidingWindowSize(cbProps.getSlidingWindowSize())
    .minimumNumberOfCalls(cbProps.getMinimumNumberOfCalls())
    .waitDurationInOpenState(Duration.ofMillis(cbProps.getWaitDurationInOpenStateMs()))
    .permittedNumberOfCallsInHalfOpenState(cbProps.getPermittedNumberOfCallsInHalfOpenState())
    .automaticTransitionFromOpenToHalfOpenEnabled(true)
    .ignoreExceptions(HttpClientErrorException.class)
    .ignoreException(throwable -> {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpClientErrorException) return true;
            current = current.getCause();
        }
        return false;
    })
    .build();
```

---

## 5. TIMEOUT OWNERSHIP & RETRY STORM MITIGATION

- **Timeout Ownership**: Handled strictly by the HTTP client request factory (`SimpleClientHttpRequestFactory`) with:
  - `connectTimeoutMs = 500`
  - `readTimeoutMs = 1000`
- **No Duplicate Timeouts**: No redundant Resilience4j `TimeLimiter` is placed around the call to prevent thread coordination overhead.
- **No Retries / No Retry Storms**: Collobster Feed is latency-critical. Automatic HTTP retries on timeout or 5xx are explicitly forbidden because retrying an overloaded Python service causes cascading queue saturation. Instead, requests immediately fail over to Java `BaselineFeedRankingStrategy`.

---

## 6. TEST SUITE & VERIFICATION RESULTS

### New / Restored Tests
1. **`RecommendationCircuitBreakerTest` (11 tests, 0.11s runtime)**:
   - `testClosedStateHealthyCalls`: Healthy calls succeed; circuit remains CLOSED.
   - `testFailureAccumulationTransitionsToOpen`: 3 consecutive 500 errors exceed 50% threshold; circuit transitions to OPEN.
   - `testResourceAccessExceptionTransitionsToOpen`: Network timeout/refused errors transition circuit to OPEN.
   - `testOpenStateFastFailsWithoutCallingOperation`: In OPEN state, supplier is never called; fails fast with `RecommendationCircuitBreakerOpenException`.
   - `testHalfOpenStatePermittedProbes`: In HALF_OPEN state, permits configured probes.
   - `testRecoveryReturnsToClosed`: 2 successful probes in HALF_OPEN return circuit to CLOSED.
   - `testReFailureReturnsToOpen`: Failed probes in HALF_OPEN return circuit immediately to OPEN.
   - `test401UnauthorizedIgnoredByCircuitBreaker`: 5 repeated 401s do NOT increment failure stats; circuit remains CLOSED.
   - `test422UnprocessableEntityIgnoredByCircuitBreaker`: 5 repeated 422s do NOT increment failure stats; circuit remains CLOSED.
   - `testAutomaticTransitionFromOpenToHalfOpen`: Verifies automatic background timer transitions from OPEN to HALF_OPEN after wait duration.
   - `testDisabledCircuitBreaker`: When `enabled = false`, calls execute unconditionally.

2. **`HttpRecommendationClientTest` (7 tests, 0.11s runtime)**:
   - `testSuccessfulRankingCall`: Verifies 200 OK with Bearer authorization and response DTO parsing.
   - `testHttp500ServerError`: Verifies 500 error wraps into `RecommendationException`.
   - `testHttp401Unauthorized`: Verifies 401 error wraps into `RecommendationException`.
   - `testCircuitBreakerOpenTransition`: Verifies 3 consecutive 500s trip circuit to OPEN and 4th call fast-fails without hitting server.
   - `testHttp401UnauthorizedDoesNotTripCircuitBreaker`: Repeated 401s do NOT trip breaker.
   - `testHttp422UnprocessableEntityDoesNotTripCircuitBreaker`: Repeated 422s do NOT trip breaker.
   - `testCircuitBreakerOpenFastFailsWithoutCallingServer`: Open breaker prevents HTTP dispatch.

3. **`RecommendationFeedRankingStrategyTest` (11 tests, 0.03s runtime)**:
   - Verified candidate security gate: unsubmitted or injected ContentRefs are completely dropped.
   - Verified partial responses: missing candidates appended in baseline ranking order.
   - Verified fallback on timeout, connection refused, circuit breaker OPEN, wrong contract version, wrong requestId, and null response body.

4. **`FeedRecommendationIntegrationTest` (4 tests, 1.54s runtime)**:
   - `testFeedPipelineWithCircuitBreakerOpeningUnderRepeatedDownFailures`: Executed 3 feed calls against an unavailable Python endpoint (`http://127.0.0.1:59999`). Every call returned HTTP 200 with baseline ranking, the circuit transitioned to OPEN, the 4th call fast-failed without connection latency, and Redis `FeedSession` remained 100% valid and uncorrupted.

5. **`LivePythonRecommendationIntegrationTest` (1 test, 0.92s runtime)**:
   - Executed against the running FastAPI recommendation server (`http://127.0.0.1:8000`). Verified live HTTP ranking, internal secret token authentication, LightGBM v2 model execution, ContentRef translation, and circuit remaining CLOSED.

### Full Java Regression Summary
- **Command**: `.\mvnw.cmd test`
- **Total Tests Executed**: **330 tests**
- **Failures**: **0**
- **Errors**: **0**
- **Skipped**: **1** (`LivePythonRecommendationIntegrationTest`, which activates only when Python is running)
- **Time Elapsed**: 22.2 seconds

---

## 7. DEPENDENCY TREE CONFIRMATION

Inspection of `pom.xml` via `mvn dependency:tree -Dincludes=io.github.resilience4j:*`:
```
[INFO] com.collabsphere:collabsphere_first:jar:0.0.1-SNAPSHOT
[INFO] \- io.github.resilience4j:resilience4j-circuitbreaker:jar:2.2.0:compile
[INFO]    \- io.github.resilience4j:resilience4j-core:jar:2.2.0:compile
```
No custom replacement, duplicate libraries, or unused resilience artifacts exist in the repository.

---

## 8. PRE-MESSAGING SYSTEM BASELINE STATUS

| Domain | Status | Key Artifacts |
| :--- | :--- | :--- |
| **Auth / User** | PASSED | JWT, OAuth2, Password hashing, User entity |
| **Profile** | PASSED | Profile, Workspaces, Skills |
| **Workspace** | PASSED | User workspaces, projects, roles |
| **Community** | PASSED | Communities, memberships, roles, posts, comments |
| **PlatformPost** | PASSED | `PostStatus.ACTIVE`, moderation rules |
| **Network** | PASSED | Following, connections, blocks, mutes |
| **Interaction** | PASSED | Likes, saves, view counts, 60s dedup, suppression |
| **Feed** | PASSED | Feed sessions, Redis cache, cursors, baseline ranking |
| **Recommendation** | PASSED | Resilience4j circuit breaker, HTTP client, LightGBM contract, baseline fallback |
| **Explore** | PASSED | Phase 5.1 hardened, trending, full-text search, prefix search, keyset pagination |
| **PostgreSQL** | PASSED | Schema V1-V23 up to date, `hibernate.ddl-auto=validate` passed |
| **Redis** | PASSED | Session storage, rate limiting, no corruption |
| **Python** | UNTOUCHED | Phase 4.1 intact, models unchanged |
| **Messages** | UNIMPLEMENTED | Ready for Phase 6 |
