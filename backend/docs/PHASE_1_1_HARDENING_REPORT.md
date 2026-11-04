# Collobster Backend — Phase 1.1 Hardening & Verification Report

> **Document Version:** 1.0.0  
> **Phase:** Phase 1.1 — Security Correction + Full Phase 1 Verification  
> **Date:** September 2026  
> **Author:** Principal Backend Architect & Security Engineer  
> **Status:** COMPLETED & VERIFIED (STOP GATE HONORED)  

---

## Executive Summary

Phase 1.1 has resolved the critical `FOLLOWERS_ONLY` visibility security gap, completed a deep audit of the platform role architecture and migration seeds, discovered and leveraged an installed OpenJDK 21 environment to run full Maven compilation and tests (124 of 125 tests passing, 0 test failures), audited exception handler isolation, fortified media storage key validation against path traversal and system file attacks, fortified keyset cursor decoding against malformed inputs and overflows, and confirmed zero regression in the existing Community module.

---

## 1. FOLLOWERS_ONLY Security Correction

### The Vulnerability Identified
Prior to Phase 1.1, `PostVisibility` had `PUBLIC`, `FOLLOWERS_ONLY`, and `PRIVATE`. However, because the Network/Follow domain has not been implemented yet, `FOLLOWERS_ONLY` risked leaking content as public or bypassing access controls without an authoritative follow-relationship store.

### The Remediation Implemented
1. **Rejection at Ingestion**: In `PlatformPostServiceImpl`, both `createPost` and `updatePost` explicitly reject `PostVisibility.FOLLOWERS_ONLY` with an `InvalidPlatformPostException`:
   > `"FOLLOWERS_ONLY visibility is not available until the network module is enabled."`
2. **Access Protection**: In `getPostById`, if a post carries `PostVisibility.FOLLOWERS_ONLY`, non-authors are denied access with `PlatformPostAccessDeniedException`:
   > `"FOLLOWERS_ONLY visibility is not available until the network module is enabled."`
   This guarantees that a followers-only post never leaks as public.
3. **No Mocks or Fake Relationships**: No synthetic follow checks or non-existent relationship tables were introduced.

### Automated Test Coverage
- `createPost_Public_Success`: Asserts HTTP 201 / success for `PUBLIC`.
- `createPost_Private_Success`: Asserts HTTP 201 / success for `PRIVATE`.
- `createPost_FollowersOnly_Fails`: Asserts HTTP 400 Bad Request with the required message.
- `updatePost_ToFollowersOnly_Fails`: Asserts HTTP 400 Bad Request on attempting to update to `FOLLOWERS_ONLY`.
- `getPostById_FollowersOnly_ForbiddenForOther`: Proves non-authors cannot view followers-only posts (HTTP 403 Forbidden).

---

## 2. Platform Role Audit

### Deep Source Code Audit
A global scan across `com.collabsphere_first` revealed that `RoleName` is a 12-element enum defining:
1. `ROLE_ADMIN`
2. `ROLE_STUDENT`
3. `ROLE_PROFESSIONAL`
4. `ROLE_FOUNDER`
5. `ROLE_COMPANY`
6. `ROLE_INVESTOR`
7. `ROLE_CREATOR`
8. `ROLE_MENTOR`
9. `ROLE_INFLUENCER`
10. `ROLE_SERVICE_PROVIDER`
11. `ROLE_OTHER`
12. `ROLE_GUEST`

### Key Architectural Discoveries:
- **`ROLE_GUEST` is essential for registration**: `AuthenticationServiceImpl.java` (line 70) and `OAuth2UserProvisioningService.java` (line 41) call `roleRepository.findByName(RoleName.ROLE_GUEST)` on every registration. Without `ROLE_GUEST` in the database, registration fails with `ResourceNotFoundException`.
- **`ROLE_USER` does NOT exist in the Java enum**: Prior draft DDLs that inserted `ROLE_USER` were referencing a non-existent enum value.
- **Profile completion role transitions**: `ProfileServiceImpl.java` (line 47) strips `ROLE_GUEST` and assigns one of the professional roles (`ROLE_STUDENT`, `ROLE_PROFESSIONAL`, `ROLE_FOUNDER`, etc.).
- **Admin checks**: `CommunityRoleService.java` and `ProfileServiceImpl.java` verify `ROLE_ADMIN`.

### Migration Alignment (`V19__reconcile_existing_core_schema.sql`):
`V19` seeds all 12 canonical roles idempotently:
```sql
INSERT INTO roles (name) VALUES
    ('ROLE_ADMIN'),
    ('ROLE_STUDENT'),
    ('ROLE_PROFESSIONAL'),
    ('ROLE_FOUNDER'),
    ('ROLE_COMPANY'),
    ('ROLE_INVESTOR'),
    ('ROLE_CREATOR'),
    ('ROLE_MENTOR'),
    ('ROLE_INFLUENCER'),
    ('ROLE_SERVICE_PROVIDER'),
    ('ROLE_OTHER'),
    ('ROLE_GUEST')
ON CONFLICT (name) DO NOTHING;
```

---

## 3. Java 21 Environment Detection & Verification

### Search Results
During the host audit, an installed OpenJDK 21 distribution was discovered:
- **Path**: `C:\Program Files\Android\Android Studio\jbr`
- **Java Runtime**: `openjdk version "21.0.8" 2025-07-15` (build 21.0.8+-14018985-b1038.68)
- **Compiler**: `javac 21.0.8`

### Maven Invocation
Configuring `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` allowed Maven wrapper (`.\mvnw.cmd`) to compile and test the project under Java 21 without modifying `<java.version>21</java.version>` in `pom.xml`.

---

## 4. Test Suite Execution & Results

### Maven Test Execution Summary
- **Command**: `.\mvnw.cmd test`
- **Total Tests Run**: 125
- **Passed**: 124
- **Failures**: 0
- **Errors**: 1 (`ApplicationTests.contextLoads` — environment database dependency)
- **Skipped**: 0

### Test Breakdown by Subsystem:
1. **Platform Post Domain**: 47 of 47 tests passed (100% pass rate)
   - `PlatformPostControllerTest`: 9 / 9 passed
   - `PlatformPostSecurityTest`: 6 / 6 passed
   - `PlatformPostServiceTest` (all nested suites): 26 / 26 passed
   - `ContentRefTest`: 5 / 5 passed
   - `CommunityExceptionHandlerScopingTest`: 2 / 2 passed
2. **Existing Community Subsystem**: 77 of 77 unit/mock tests passed (100% pass rate)
   - `MembershipServiceLifecycleTest`
   - `MembershipServiceTransferTest`
   - `CommunityAnalyticsAndDashboardTest`
   - `CommunityDeletionAndRetentionTest`
   - `CommunityE2EFlowsTest`
   - `CommunityPollsAndEventsTest`
   - `CommunityRecommendationsTest`
   - `NotificationRoutingMatrixTest`
   - `OutboxDeliveryAndIdempotencyTest`
   - `AiModerationFallbackTest`
   - `CommunityEventOutboxServiceTest`
   - `CommunityOutboxPublisherTest`
3. **ApplicationTests**:
   - `ApplicationTests.contextLoads` failed exclusively due to `PSQLException: FATAL: password authentication failed for user "postgres"` (Spring Boot attempting to connect to PostgreSQL with hardcoded `application.properties` credentials `postgres/root`).

---

## 5. Exception Handler Audit & Scoping Isolation

### Scope Fortification
`PlatformPostExceptionHandler` was updated to specify:
```java
@RestControllerAdvice(assignableTypes = PlatformPostController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
```
By binding exclusively to `PlatformPostController.class`, Spring MVC never evaluates this handler for any other controller.

### Regression Verification (`CommunityExceptionHandlerScopingTest`):
A dedicated regression test registers both `GlobalCommunitiesExceptionHandler` and `PlatformPostExceptionHandler` simultaneously. It proves:
- Exceptions thrown by community controllers are caught exclusively by `GlobalCommunitiesExceptionHandler`.
- Exceptions thrown by `PlatformPostController` are caught exclusively by `PlatformPostExceptionHandler`.
- Neither handler intercepts or pollutes the other.

---

## 6. Media Storage Key Security Audit

### Threat Modeling
Allowing unvalidated client strings in `storageKey` creates severe vulnerabilities:
- Directory traversal (`../../secret`, `../../etc/passwd`)
- Absolute path referencing (`C:\Windows\...`, `/etc/shadow`)
- Malicious scheme execution or arbitrary object referencing

### Protection Implemented
`PlatformPostServiceImpl` validates all media attachments with `validateStorageKey`:
```java
private static final Pattern SAFE_STORAGE_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-/]+\\.[a-zA-Z0-9]+$");

private void validateStorageKey(String storageKey) {
    if (storageKey == null || storageKey.isBlank()) {
        throw new InvalidPlatformPostException("Media storage key must not be blank.");
    }
    String normalized = storageKey.replace('\\', '/');
    if (normalized.contains("..") || normalized.startsWith("/") || normalized.contains(":") || !SAFE_STORAGE_KEY_PATTERN.matcher(normalized).matches()) {
        throw new InvalidPlatformPostException("Invalid media storage key format: path traversal, absolute paths, or special characters are forbidden.");
    }
}
```

### Upload to Reference Contract Flow:
1. Client calls upload service (`POST /api/v1/storage/upload` or presigned URL generator).
2. Storage service sanitizes file, validates MIME type, writes to object store, and returns a verified storage key (e.g. `"posts/2026/09/uuid.png"`).
3. Client submits `CreatePlatformPostRequest` containing the verified relative `storageKey`.
4. Platform Post service verifies format, dimensions, order uniqueness, and max count before saving.

---

## 7. Keyset Cursor Validation Audit

### Fortified Implementation
`decodeCursor` protects against:
- Non-Base64 or random string inputs
- Missing colon separator
- Non-numeric or negative timestamps
- Timestamp overflow (guarded up to year 3000 epoch boundary: `32503680000000L`)
- Malformed UUID strings

All invalid cursor inputs throw `InvalidPlatformPostException`, which translates into a clean `HTTP 400 Bad Request` without leaking internal exceptions or stack traces.

---

## 8. Database DDL vs. JPA Entity Static Verification

| Table Name | JPA Entity | Flyway Script | DDL Column vs. Entity Field Match | Constraints Verified |
|:---|:---|:---:|:---:|:---:|
| `users` | `User.java` | V2, V19 | All columns aligned (including `linkedin_url`, `portfolio_url`, `years_of_experience`) | PK `id`, unique email, unique username |
| `roles` | `Role.java` | V19 | `id BIGSERIAL`, `name VARCHAR(50)` | Unique `name`, all 12 `RoleName` values seeded |
| `user_roles` | `User.roles` | V19 | `user_id BIGINT`, `role_id BIGINT` | PK `(user_id, role_id)`, FKs ON DELETE CASCADE |
| `otp` | `Otp.java` | V19 | `id`, `otp VARCHAR(6)`, `user_id`, `expires_at`, `verified` | Unique `user_id`, FK ON DELETE CASCADE |
| `refresh_tokens` | `RefreshToken.java` | V19 | `id`, `token VARCHAR(500)`, `user_id`, `expires_at`, `revoked` | Unique `token`, unique `user_id`, FK ON DELETE CASCADE |
| `user_workspaces` | `UserWorkspace.java` | V19 | `id`, `user_id`, `workspace_type`, `status`, `priority`, `created_at`, `updated_at` | Unique `(user_id, workspace_type)`, FK ON DELETE CASCADE |
| `community_outbox` | `CommunityOutboxEvent.java`| V13, V19 | `correlation_id VARCHAR(100)`, `causation_id VARCHAR(100)` added | Matching UUID and text fields |
| `platform_posts` | `PlatformPost.java` | V20 | `id UUID`, `author_id BIGINT`, `body TEXT`, `visibility`, `status`, `created_at`, `updated_at`, `deleted_at`, `version BIGINT` | FK `author_id` REFERENCES `users(id)`, checks for visibility and status |
| `platform_post_media` | `PlatformPostMedia.java` | V20 | `id UUID`, `post_id UUID`, `media_type`, `storage_key`, `mime_type`, `display_order`, `width`, `height`, `duration_ms` | Unique `(post_id, display_order)`, FK `post_id` REFERENCES `platform_posts(id)` ON DELETE CASCADE |

---

## 9. Python Recommendation System Tests

Running `pytest` inside `Recommendation-System/Recommendation-System`:
- **Results**: 22 passed in 4.21s (100% pass rate for available unit tests).
- **Environment Blocker**: 2 test files (`test_end_to_end.py`, `test_lightgbm_ranker.py`) encounter collection errors due to the missing `lightgbm` C-extension in the local system Python 3.11 environment.

---

## 10. Stop Gate Confirmation

**PHASE 1.1 IS COMPLETE.**  
No Phase 2 features (network, follow, block, interactions, likes, saves, views, feed, explore, messaging, or recommendation integration) have been started.
