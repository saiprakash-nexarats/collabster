# Phase 7.2 Community Notification Backward Compatibility & Global API Contract Lock Report

> **Document Version:** 1.0.0  
> **Status:** Completed & Validated  
> **Date:** September 2026  
> **Author:** Principal Backend Architect  
> **Target Module:** `collabster-communities-module`

---

## 1. Executive Summary

Phase 7.2 restored complete backward compatibility for historical Community Notification clients and permanently locked the Global Notification Center contract into a dedicated, non-conflicting REST namespace (`/api/v1/notification-center`).

Key architectural invariants achieved:
1. **Established API Priority**: Older established Community Notification APIs retain precedence over Phase 7 routes. Legacy Community endpoints (`GET /api/v1/notifications`, `GET /api/v1/notifications/unread-count`, `PATCH /api/v1/notifications/{id}/read`, `PATCH /api/v1/notifications/read-all`) are fully restored to serve historical `Page<CommunityNotificationDto>` and `Long` unread counts.
2. **Canonical Community Namespace**: `/api/v1/communities/notifications/**` is retained as the explicit canonical Community namespace, backed by the same `CommunityNotificationService`.
3. **Global Notification Center Namespace**: All Global Notification Center endpoints are mounted under `/api/v1/notification-center/**` (`/`, `/{id}`, `/{id}/seen`, `/{id}/read`, `/{id}/archive`, `/read-all`, `/unread-count`, `/summary`, `/preferences`, `/devices`).
4. **Zero Route Collisions & Ambiguities**: Spring Boot context loads with 0 ambiguous request mappings.
5. **Domain Independence**: Community unread counts and Global unread counts remain strictly decoupled (e.g. Community = 3, Global = 8). Read mutations in one domain never modify entity rows in the other.
6. **Full Regression Validation**: All 446 automated tests pass on Java 21 against real PostgreSQL 16 on port 5433 with 0 failures, 0 errors, and 1 skipped test (offline live Python test). Python subsystem (`Recommendation-System/`) was completely untouched.

---

## 2. Detailed Audit & Invariant Verification

### A. Historical Community API Audit
Inspection of the pre-Phase-7 Community notification codebase and documentation (`docs/community-api.md`) confirmed the complete historical REST contract:

| HTTP Method | Historical Path | Query Params | Request DTO | Response DTO | Status Codes | Authorization |
|---|---|---|---|---|---|---|
| `GET` | `/api/v1/notifications` | `page`, `size`, `sort` (Spring Data `Pageable`, max 100) | None | `ApiResponse<Page<CommunityNotificationDto>>` | 200 OK, 401 Unauthorized | Authenticated caller (`SecurityContext`) |
| `GET` | `/api/v1/notifications/unread-count` | None | None | `ApiResponse<Long>` | 200 OK, 401 Unauthorized | Authenticated caller |
| `PATCH` | `/api/v1/notifications/{id}/read` | None | None | `ApiResponse<CommunityNotificationDto>` | 200 OK, 400 Bad Request (InvalidJoinException), 401 | Notification owner |
| `PATCH` | `/api/v1/notifications/read-all` | None | None | `ApiResponse<Integer>` (count updated) | 200 OK, 401 Unauthorized | Authenticated caller |

---

### B. API Collision Matrix

| HTTP Method | Historical Community Path | Phase 7 Global Path | Collision Status | Resolution in Phase 7.2 |
|---|---|---|---|---|
| `GET` | `/api/v1/notifications` | `/api/v1/notifications` | **CRITICAL COLLISION** (Different response schemas: `Page<CommunityNotificationDto>` vs `NotificationPageResponse`) | Historical Community restored on `/api/v1/notifications`. Global moved to `/api/v1/notification-center`. |
| `GET` | `/api/v1/notifications/unread-count` | `/api/v1/notifications/unread-count` | **CRITICAL COLLISION** (Different response schemas: `Long` vs `UnreadCountResponse`) | Historical Community restored on `/api/v1/notifications/unread-count`. Global moved to `/api/v1/notification-center/unread-count`. |
| `PATCH` vs `POST` | `/api/v1/notifications/{id}/read` | `/api/v1/notifications/{id}/read` | **COLLISION** (Method & Schema mismatch) | Community retains `PATCH /api/v1/notifications/{id}/read`. Global uses `POST /api/v1/notification-center/{id}/read`. |
| `PATCH` vs `POST` | `/api/v1/notifications/read-all` | `/api/v1/notifications/read-all` | **COLLISION** (Method & Schema mismatch) | Community retains `PATCH /api/v1/notifications/read-all`. Global uses `POST /api/v1/notification-center/read-all`. |
| `POST` | *None* | `/api/v1/notifications/{id}/seen` | Path pollution on Community root | Global moved to `POST /api/v1/notification-center/{id}/seen`. |
| `POST` | *None* | `/api/v1/notifications/{id}/archive` | Path pollution on Community root | Global moved to `POST /api/v1/notification-center/{id}/archive`. |
| `GET` | *None* | `/api/v1/notifications/{id}` | Path pollution on Community root | Global moved to `GET /api/v1/notification-center/{id}`. |
| Sub-resources | *None* | `/api/v1/notifications/preferences` | Path pollution on Community root | Global moved to `/api/v1/notification-center/preferences`. |
| Sub-resources | *None* | `/api/v1/notifications/devices` | Path pollution on Community root | Global moved to `/api/v1/notification-center/devices`. |

---

### C. Final Global Notification Namespace
The entire Global Notification Center REST API has been established under:
```text
/api/v1/notification-center/**
```
This isolates global notifications from Community endpoints, adhering strictly to REST resource naming and domain boundaries.

---

### D. Restored Legacy Community Routes
Mapped in `CommunityNotificationController.java` (marked as **DEPRECATED BUT SUPPORTED**):
- `GET /api/v1/notifications`: Paginated `CommunityNotificationDto` list.
- `GET /api/v1/notifications/unread-count`: Returns `Long` count of unread community notifications.
- `PATCH /api/v1/notifications/{id}/read`: Marks single community notification read.
- `PATCH /api/v1/notifications/read-all`: Bulk marks community notifications read.

---

### E. Canonical Community Routes
Mapped in `CommunityNotificationController.java`:
- `GET /api/v1/communities/notifications`: Pageable `CommunityNotificationDto` list.
- `GET /api/v1/communities/notifications/unread-count`: Returns `Long` count.
- `PATCH /api/v1/communities/notifications/{id}/read`: Marks single community notification read.
- `PATCH /api/v1/communities/notifications/read-all`: Bulk marks community notifications read.

Both legacy and canonical routes invoke the same `CommunityNotificationService` methods.

---

### F. Global Notification Routes
Mapped across Global controllers:
- In `NotificationController` (`@RequestMapping("/api/v1/notification-center")`):
  - `GET /api/v1/notification-center`: Keyset-paginated global notifications (`NotificationPageResponse`).
  - `GET /api/v1/notification-center/{notificationId}`: Single notification details (`NotificationResponse`).
  - `POST /api/v1/notification-center/{id}/seen`: Mark as seen.
  - `POST /api/v1/notification-center/{id}/read`: Mark as read.
  - `POST /api/v1/notification-center/{id}/archive`: Soft-archive notification.
  - `POST /api/v1/notification-center/read-all`: Bulk read-all up to keyset tuple cursor.
  - `GET /api/v1/notification-center/unread-count`: Unread count (`UnreadCountResponse`).
  - `GET /api/v1/notification-center/summary`: Summary alias (`UnreadCountResponse`).
- In `NotificationPreferenceController` (`@RequestMapping("/api/v1/notification-center/preferences")`):
  - `GET /api/v1/notification-center/preferences`: User category preferences (`List<NotificationPreferenceDto>`).
  - `PATCH /api/v1/notification-center/preferences`: Update preferences.
- In `NotificationDeviceController` (`@RequestMapping("/api/v1/notification-center/devices")`):
  - `POST /api/v1/notification-center/devices`: Register device token (`NotificationDeviceResponse`).
  - `GET /api/v1/notification-center/devices`: List active devices.
  - `DELETE /api/v1/notification-center/devices/{deviceId}`: Revoke device token.

---

### G. Community / Global State Separation
Verified in `NotificationApiCompatibilityIntegrationTest.testDomainStateSeparation`:
- Marking a Global notification read via `POST /api/v1/notification-center/{id}/read` sets `notifications.read_at = now`, but leaves `community_notifications.read_at` strictly `null`.
- Marking a Community notification read via `PATCH /api/v1/notifications/{id}/read` sets `community_notifications.read_at = now`, and does not alter any global notification rows.

---

### H. Unread Count Separation
Verified in `NotificationApiCompatibilityIntegrationTest.testUnreadCountSeparation`:
- Setup: 3 unread `CommunityNotification` rows and 8 unread `Notification` rows for user Alice.
- Results:
  - `GET /api/v1/notifications/unread-count` $\rightarrow$ `3`
  - `GET /api/v1/communities/notifications/unread-count` $\rightarrow$ `3`
  - `GET /api/v1/notification-center/unread-count` $\rightarrow$ `{"unreadCount": 8}`
  - `GET /api/v1/notification-center/summary` $\rightarrow$ `{"unreadCount": 8}`

---

### I. Controller Mapping Verification
Verified in `NotificationApiCompatibilityIntegrationTest.testNoAmbiguousSpringMappings`:
- Spring's `RequestMappingHandlerMapping` was inspected at application startup.
- All routes coexist with 0 ambiguous mapping conflicts:
  - `/api/v1/notifications` maps to `CommunityNotificationController`
  - `/api/v1/communities/notifications` maps to `CommunityNotificationController`
  - `/api/v1/notification-center` maps to `NotificationController`
  - `/api/v1/notification-center/preferences` maps to `NotificationPreferenceController`
  - `/api/v1/notification-center/devices` maps to `NotificationDeviceController`

---

### J. Security / IDOR Regression
Verified in `NotificationApiCompatibilityIntegrationTest.testIdorSecurityProtection`:
- User identity is resolved strictly from `SecurityContext` via JWT Bearer token.
- If Bob attempts to access or mutate Alice's Community notification: HTTP 400 (`InvalidJoinException`).
- If Bob attempts to read or mutate Alice's Global notification: HTTP 403 Forbidden (`NotificationAccessDeniedException`).
- If Bob attempts to revoke Alice's registered device: HTTP 403 Forbidden.

---

### K. Client / Repository Call-Site Audit
- Searched entire repository for all occurrences of `/api/v1/notifications`.
- Identified that internal tests were the only consumers of the newly introduced Phase 7 global endpoints.
- Updated all Global consumers and tests to target `/api/v1/notification-center/**`.
- Preserved historical Community notification consumers against `/api/v1/notifications` and `/api/v1/communities/notifications`.

---

### L. OpenAPI / Documentation
- Updated `docs/COLLOBSTER_BACKEND_ARCHITECTURE.md`:
  - Section 18: API Routing Table updated with canonical Community, legacy Community, and Global Notification Center namespaces.
  - Section 25: Keyset pagination and bulk read endpoints updated to `/api/v1/notification-center`.
  - Section 26.4: Documented Phase 7.2 API compatibility and contract lock.
- Springdoc / Swagger OpenAPI automatically generates distinct endpoint definitions without collisions.

---

### M. Notification Durability Regression
- All Phase 7.1 durability guarantees remain completely intact:
  - Option B same-transaction atomicity between source mutations, notification, and outbox rows.
  - Event occurrence identity keys (`FOLLOW:{followId}:{recipientId}`, `MESSAGE_REACTION:{eventId}:{recipientId}`).
  - Worker claiming with `SELECT ... FOR UPDATE SKIP LOCKED`.
  - Stale worker lease protection and worker crash recovery.
  - Retention safety query preventing deletion of notifications with active delivery jobs.

---

### N. PostgreSQL / Flyway / Hibernate Verification
- Real PostgreSQL 16 on port 5433 (`collobster-test-pg`) validated.
- Flyway migrations V1 through V26 validated cleanly. No V27 migration was created.
- Hibernate schema validation (`spring.jpa.hibernate.ddl-auto=validate`) passed with 0 errors.

---

### O. WebSocket Regression
- Realtime WebSocket routes remain:
  - `/ws/community`
  - `/ws/messages`
  - `/ws/notifications` (STOMP destination: `/user/queue/notifications`)
- All 14 WebSocket security tests passed.

---

### P. Full Java Regression
- **Command Executed**: `.\mvnw.cmd test`
- **Total Tests Run**: 446
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 1 (`LivePythonRecommendationIntegrationTest`, intentionally disabled when live Python server is offline)
- **Build Status**: `BUILD SUCCESS` (35.48s execution time)

---

### Q. Files Added / Modified

#### Modified Files
1. `community/controller/CommunityNotificationController.java`: Restored historical `@GetMapping({"/api/v1/communities/notifications", "/api/v1/notifications"})` and unread-count, added `@PathVariable("id")`.
2. `notification/controller/NotificationController.java`: Changed base path to `@RequestMapping("/api/v1/notification-center")`, added explicit parameter names, added `/summary` alias.
3. `notification/controller/NotificationPreferenceController.java`: Changed base path to `@RequestMapping("/api/v1/notification-center/preferences")`.
4. `notification/controller/NotificationDeviceController.java`: Changed base path to `@RequestMapping("/api/v1/notification-center/devices")`, added `@PathVariable("deviceId")`.
5. `notification/NotificationPostgresIntegrationTest.java`: Added Pageable argument resolver to standalone MockMvc, expanded compatibility tests to include GET endpoints.
6. `docs/COLLOBSTER_BACKEND_ARCHITECTURE.md`: Updated Section 18, Section 25, and Section 26.4 with Phase 7.2 namespaces.

#### Added Files
1. `notification/NotificationApiCompatibilityIntegrationTest.java`: 9 end-to-end integration tests verifying zero ambiguous mappings, Community legacy/canonical contract, Global REST contract, unread count separation, domain state separation, and IDOR protection.
2. `docs/PHASE_7_2_NOTIFICATION_API_COMPATIBILITY_REPORT.md`: This completion report.

---

### R. Known Limitations / Deprecations
- Legacy Community endpoints under `/api/v1/notifications` are marked **DEPRECATED BUT SUPPORTED**. They will be maintained throughout v1 for client backward compatibility and scheduled for removal in v2.
- Mobile push and email providers continue using mock adapters until production vendor credentials are provided.

---

### S. Explicit Confirmation Python Was NOT Modified
- **Confirmed**: `Recommendation-System/` was **NOT modified**.
- Zero Python files were altered or impacted.

---

**PHASE 7.2 IS COMPLETE. STOPPING EXECUTION AS DIRECTED.**
