# Target architecture alignment

## Current verified state

- **Web:** React 18 + Vite single-page application. It is not a Next.js application.
- **Backend:** Java 17, Spring Boot 3.5 modular monolith, Spring Security/JWT, JPA, Flyway, PostgreSQL, Redis, and STOMP/WebSocket endpoints.
- **Storage:** backend storage abstraction plus local upload fallback; cloud object-storage credentials are deployment configuration.
- **AI:** Spring already has a protected HTTP contract for a FastAPI recommendation service, but this repository does not contain that FastAPI service.
- **Mobile:** no Flutter project exists in this repository.

## Approved target boundaries

| Surface | Target | Boundary |
|---|---|---|
| Mobile | Flutter | Calls the versioned Spring API and uses the same OIDC/JWT contract; never accesses PostgreSQL or Redis directly. |
| Web | React + Next.js | SSR/BFF is optional; browser requests use the versioned Spring API. The current Vite client remains supported until a deliberate migration is completed. |
| Platform | Spring Boot modular monolith | Owns authorization, transactions, PostgreSQL schema/Flyway migrations, Redis cache/rate-limit state, and WebSocket authorization. |
| AI | FastAPI | Internal-only service; accepts the existing `/api/v1/internal/recommend/*` contract using a service token. |
| Storage | Cloud object storage | Browser/mobile clients use short-lived signed URLs issued by Spring, never provider credentials. |

## Migration rules

1. Do not replace the working Vite frontend in-place with Next.js. Create a separate `web/` Next.js application, migrate routes incrementally, and switch ingress only after parity and E2E verification.
2. Create Flutter as a separate `mobile/` application with generated API models from the Spring OpenAPI document; do not duplicate business rules in Dart.
3. Create the FastAPI service as a separate deployable with health checks, bounded inference concurrency, a service-to-service token, and its own tests. Keep Spring's deterministic feed ranking fallback enabled until it passes contract tests.
4. PostgreSQL is the durable source of truth. Redis may hold only cache, presence, rate limits, and short-lived feed/session material.
5. Production must provide `POSTGRES_PASSWORD`, `JWT_SECRET`, and storage/recommendation secrets through the deployment secret manager. Docker Compose now refuses insecure defaults.

## Existing backend contracts

- Public health: `GET /api/v1/system/health`
- Browser/mobile API: `/api/v1/**` and compatibility `/v1/**`
- Internal ranker: `POST /api/v1/internal/recommend/feed/rank`
- Internal feedback: `POST /api/v1/internal/recommend/feedback/batch`
- Internal privacy erase: `POST /api/v1/internal/recommend/privacy/erase`

## Required next implementation work

- Build and test the separate Next.js and Flutter clients; neither can be truthfully marked complete in this repository today.
- Add the missing FastAPI source and contract tests before enabling `COLLOBSTER_RECOMMENDATION_ENABLED` in production.
- Configure a managed PostgreSQL, Redis, object-storage bucket/CORS policy, email provider, and secret manager in the target environment.
