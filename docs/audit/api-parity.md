# Collobster API Parity Inventory & Architecture Contract

**Repository:** `https://github.com/saiprakash-nexarats/collabster`  
**Branch:** `main`  
**Audit Reference:** Phase 2 Acceptance & Phase 3 Contract Inventory  
**Classification Date:** 2026-11-04  

---

## 1. Classification Categories & Totals

In accordance with Phase 2 Functional Acceptance requirements, every operation in `src/api.js` is audited and categorized using the formal standard:

- **`IMPLEMENTED_AND_TESTED`**: Full business logic, database persistence, and automated security/contract tests verified.
- **`ROUTE_ONLY`**: Route mapped on the HTTP server but lacking complete business persistence or authorization logic.
- **`CONTRACT_MISMATCH`**: Endpoint schema or HTTP method diverges between client and server.
- **`MISSING`**: Client endpoint expects a route that does not exist on the server.
- **`DEFERRED`**: Intentionally scheduled for future phases (Phase 5 domains or infrastructure-dependent features such as WebRTC media calling). Handled explicitly (returning 501 / disabled controls rather than deceptive success).
- **`BLOCKED`**: Cannot proceed due to environment or third-party dependency.

### Summary Breakdown across all 141 frontend operations:

| Category | Count | Percentage | Details |
| :--- | :---: | :---: | :--- |
| **`IMPLEMENTED_AND_TESTED`** | **122** | 86.5% | Health, Auth, Identity, Social, Notifications, Realtime, Communities, Events, Core Messaging, Storage, Projects (12), Companies (11), Learning (10), Mentors (9), Career (7) |
| **`DEFERRED`** | **19** | 13.5% | 15 Remaining Domain Operations (Phase 3 Services Marketplace + Payments) + 4 WebRTC media calling endpoints |
| **`ROUTE_ONLY`** | **0** | 0.0% | None |
| **`CONTRACT_MISMATCH`** | **0** | 0.0% | None |
| **`MISSING`** | **0** | 0.0% | None |
| **`BLOCKED`** | **0** | 0.0% | None |
| **TOTAL** | **141** | **100%** | Full contract audit complete |

---

## 2. Categorization Detail by Domain

| Domain | Total | IMPLEMENTED_AND_TESTED | DEFERRED | Notes |
| :--- | :---: | :---: | :---: | :--- |
| **System & Health** | 1 | 1 | 0 | `SystemHealthController` verified |
| **Authentication & Session** | 10 | 10 | 0 | Auth, OAuth, OTP, HttpOnly cookies, session versioning |
| **Identity & Profile** | 5 | 5 | 0 | Current user profile, public profiles, profile search, update |
| **Social, Feed & Interaction** | 14 | 14 | 0 | Posts, comments, reactions, bookmarks, polls, follows, reports |
| **Explore & Discovery** | 1 | 1 | 0 | Multi-entity search with filters and keyset pagination |
| **Notifications** | 4 | 4 | 0 | Paginated notifications, read receipts, category preferences |
| **Realtime Presence** | 1 | 1 | 0 | STOMP `/ws/community` and `/realtime` presence |
| **Communities** | 9 | 9 | 0 | Community CRUD, join/leave, posts, comments, moderation |
| **Events** | 8 | 8 | 0 | Global/community events, registrations, tickets, check-in, broadcast |
| **Direct & Group Messaging** | 21 | 17 | 4 | 17 core operations verified; 4 calling operations explicitly DEFERRED |
| **Learning Module** | 10 | 10 | 0 | Implemented and verified with PostgreSQL Flyway V37 |
| **Projects Module** | 12 | 12 | 0 | Implemented and tested (P1) |
| **Services Marketplace** | 8 | 0 | 8 | Scheduled for Phase 3 |
| **Companies Module** | 11 | 11 | 0 | Implemented and tested (P1) |
| **Mentors Module** | 9 | 9 | 0 | Implemented and verified with atomic slot reservation |
| **Career & Jobs Module** | 7 | 7 | 0 | Implemented and verified with secured resume attachments |
| **Storage Adapter** | 3 | 3 | 0 | Implemented and verified (Stage 5A) |
| **Payments & Payouts** | 7 | 0 | 7 | Scheduled for Phase 3 |
| **TOTAL** | **141** | **122** | **19** | |

---

## 3. Real Messaging & Event Implementation Hardening

### A. Conversation Membership Authorization
- Authenticated caller must be an active participant in `conversationId` to list members via `GET /v1/messages/conversations/{id}/members`.
- Adding members (`POST /v1/messages/conversations/{id}/members`) verifies caller participation, rejects adding members to 1:1 DIRECT conversations (relational pair invariant), and validates target profile identity.
- Removing members (`DELETE /v1/messages/conversations/{id}/members/{profileId}`) permits self-leave or requires conversation administrator/creator authorization. Non-admin participants cannot kick other members.
- Verified by automated tests in `MessagingMembershipAuthorizationTest.java`.

### B. Durable Starred Messages
- Added Flyway migration `V33__create_starred_messages_schema.sql` creating table `starred_messages` with unique constraint `(user_id, message_id)`.
- Implemented `StarredMessage.java` entity, `StarredMessageRepository.java`, and service methods in `MessagingService`.
- `POST /conversations/{id}/messages/{msgId}/star`: verifies participant access and message ownership before persisting star.
- `DELETE /conversations/{id}/messages/{msgId}/star`: verifies access and deletes caller's star.
- `GET /starred`: retrieves caller's starred messages with full hydrated message details.

### C. Explicit Deferral of Calling Endpoints
- WebRTC media streaming requires dedicated STUN/TURN/SFU relay infrastructure.
- In accordance with production integrity standards, calling endpoints (`/conversations/{id}/calls`, `/calls/signal`, `/calls/{id}/end`, `/calls/history`) do NOT return fake success tokens.
- Endpoints return `501 NOT_IMPLEMENTED` with clear operational diagnostics (`"Audio/video calling is deferred pending WebRTC media infrastructure deployment"`).
- Categorized as `DEFERRED`.

### D. Event Ticket & Reminder Parity
- `GET /v1/events/tickets`: returns actual user event registrations from `community_event_rsvps` joined with `community_events` (`registrationState: "registered"`, `status: "GOING"`).
- `POST /v1/events/{id}/tickets/{ticketId}/check-in`: verifies ticket validity and marks attendance.
- `POST /v1/events/{id}/reminders/broadcast`: moderator-only broadcast with durable outbox recording and returned queue count.
- Verified by automated tests in `CommunityEventParityTest.java`.
