# Public Endpoint Inventory

## 1. Overview

This document provides the definitive inventory of endpoints in Collobster that are intentionally accessible without authentication (`permitAll`), along with the security rationale and protection mechanisms for each.

Any endpoint not listed in this inventory is **strictly authenticated** by default via Spring Security filter chains (`anyRequest().authenticated()`).

---

## 2. Unauthenticated Endpoints Inventory

| Method | Endpoint | Description | Security Controls & Rate Limiting |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | New user account registration | Rate-limited by IP/email; password complex-hashed (BCrypt); requires OTP verification before account enable. |
| `POST` | `/api/v1/auth/login` | User credential login (email + password) | Rate-limited to prevent brute-force attacks; generic failure message to resist account enumeration. |
| `POST` | `/api/v1/auth/verify-otp` | Verification of registration OTP | Expiring OTP (10 mins); rate-limited attempt counter; invalidated upon verification. |
| `POST` | `/api/v1/auth/resend-otp` | Resend verification OTP | Cooldown period enforced; rate-limited. |
| `POST` | `/api/v1/auth/refresh-token` | Exchange rotating refresh token for access token | HttpOnly cookie / body validation; revoked/expired tokens rejected; token rotation on every exchange. |
| `POST` | `/api/v1/auth/forgot-password` | Request password reset OTP | Rate-limited; generic confirmation message ("OTP sent if account exists") to prevent enumeration. |
| `POST` | `/api/v1/auth/verify-reset-otp` | Verify password reset OTP | Expiring OTP; bounded attempts; marks OTP as verified for password change. |
| `POST` | `/api/v1/auth/reset-password` | Reset password using verified OTP | Enforces verified OTP; updates password hash; revokes all active refresh tokens for the account. |
| `GET` | `/oauth2/**` | OAuth2 authorization initialization (Google) | Handled by Spring Security OAuth2 Client; state parameter prevents CSRF. |
| `GET` | `/login/oauth2/**` | OAuth2 redirect / authorization callback | Spring Security OAuth2 callback processing. |
| `GET` | `/ws/community` | WebSocket HTTP Handshake for Community STOMP | **Handshake only**. STOMP `CONNECT` frame requires valid JWT Bearer token in headers. Unauthorized connection closed. |
| `GET` | `/ws/messages` | WebSocket HTTP Handshake for Messaging STOMP | **Handshake only**. STOMP `CONNECT` frame requires valid JWT Bearer token in headers. Private user queue isolation enforced. |
| `GET` | `/ws/notifications` | WebSocket HTTP Handshake for Notification STOMP | **Handshake only**. STOMP `CONNECT` frame requires valid JWT Bearer token in headers. Strictly scoped to `/user/queue/notifications`. |
| `GET` | `/swagger-ui/**` | OpenAPI / Swagger UI interactive documentation | Development / API exploration documentation. Exposes schema definitions only, no data. |
| `GET` | `/v3/api-docs/**` | OpenAPI v3 specification JSON | Schema metadata only; no runtime data or secrets. |
| `*` | `/error` | Spring Boot framework error dispatcher | Configured with `server.error.include-stacktrace=never` and `server.error.include-message=never` to prevent leakage. |

---

## 3. Important Handshake Distinction (WebSockets)

Notice regarding `/ws/community`, `/ws/messages`, and `/ws/notifications`:
- The initial HTTP GET handshake is `permitAll()` purely to allow the WebSocket upgrade handshake to negotiate with the server before full framing begins.
- **Zero business data or messaging can occur over the unauthenticated HTTP handshake.**
- The STOMP protocol begins with a `CONNECT` frame. Every STOMP channel interceptor (`CommunityStompAuthorizationInterceptor`, `MessagingStompAuthorizationInterceptor`, `NotificationStompAuthorizationInterceptor`) intercepts this frame and demands a valid Bearer JWT.
- If the token is missing, expired, or invalid, the interceptor immediately terminates the STOMP session.
- Subscribing to destinations (`/topic/...`, `/queue/...`, `/user/queue/...`) and sending messages (`/app/...`) is strictly rejected for unauthenticated or unauthorized sessions.

---

## 4. Internal Service Endpoints (Service-to-Service Only)

These endpoints reside on the internal Python Recommendation Service and are never exposed to the public internet or mobile clients:

| Method | Endpoint | Service | Auth Requirement |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/internal/recommend/feed/rank` | Python FastAPI | Internal Bearer Token (`RECOMMENDATION_INTERNAL_TOKEN`); timing-safe comparison (`secrets.compare_digest`). |
| `POST` | `/api/v1/internal/recommend/feedback/batch` | Python FastAPI | Internal Bearer Token; rejects missing or invalid token with 401; batch size bounded (max 500). |
| `POST` | `/api/v1/internal/recommend/privacy/erase` | Python FastAPI | Internal Bearer Token; synchronous user event erasure and Redis cache invalidation for user deletion outbox. |
| `DELETE` | `/api/v1/internal/recommend/privacy/users/{user_id}` | Python FastAPI | Internal Bearer Token; RESTful alias for user event erasure and Redis cache invalidation. |
| `GET` | `/health/live` | Python FastAPI | Liveness probe (unauthenticated internal health check). |
| `GET` | `/health/ready` | Python FastAPI | Readiness probe (ranking model readiness). |
| `GET` | `/health/feedback` | Python FastAPI | Feedback storage connectivity probe. |
| `GET` | `/health` | Python FastAPI | Aggregated health status (degrades gracefully if feedback DB is down). |

---

## 5. Authenticated Session & Device Lifecycle Endpoints (Phase 10.1)

While the following endpoints are strictly **authenticated** (never `permitAll`), they form the perimeter for device and session revocation:

| Method | Endpoint | Description | Revocation & Privacy Behavior |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/logout` | Single-device session logout | Revokes current refresh token cookie and, if `deviceId` or `pushToken` is provided in `LogoutRequest`, validates ownership and revokes only that specific device's push token. Preserves all other active sessions and push registrations. |
| `POST` | `/api/v1/auth/logout-all` | Global multi-device logout | Revokes all refresh tokens for the user across all devices, revokes all registered push notification device tokens, and forces presence to offline immediately. |
| `POST` | `/api/v1/auth/delete-account` | GDPR/Privacy account deletion | Marks account deleted, scrubs PII, revokes all tokens, evicts presence, and enqueues an asynchronous durable outbox job (`user_data_erasure_outbox`) to purge all recommendation events and cache keys in Python recommendation storage. |

