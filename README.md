# SignalTrellis

**Personal Context & Action Intelligence**  
Connect the context. See what matters. Take action.

SignalTrellis connects personal records, commitments and events into a context graph. A deterministic signal engine finds what needs attention, explains its evidence and prepares actions for user approval.

## Current milestone

Milestones 1–2 provide the Java backend, PostgreSQL with Flyway, API error handling, registration/login, short-lived JWTs and personal workspaces. Milestone 3 adds authenticated document capture, local file storage and workspace-scoped document metadata/search.

## Technology

- Java 21
- Spring Boot 4.1.2
- Maven
- PostgreSQL 17
- Flyway
- Springdoc OpenAPI
- Docker Compose

## Run locally

Requirements: Java 21+, Maven 3.9+, Docker Compose.

```sh
docker compose up -d postgres
mvn spring-boot:run
```

PostgreSQL is published on host port `5433` to avoid colliding with an existing local PostgreSQL service.

The API listens on `http://localhost:8080`. OpenAPI UI is at `http://localhost:8080/swagger-ui.html`; the OpenAPI document is at `/v3/api-docs`. The initial migration creates the application schema. No AI provider credentials are required for this milestone.

## Authentication API

- `POST /api/v1/auth/register` — create an account and its Personal workspace.
- `POST /api/v1/auth/login` — exchange email and password for a 15 minute bearer token.
- `GET /api/v1/auth/me` — return the authenticated user's identity and workspace; requires `Authorization: Bearer <token>`.

Set `SIGNALTRELLIS_JWT_SECRET` to a Base64-encoded key of at least 32 bytes before deployment. The checked-in fallback is only for local development. Passwords use BCrypt; input is limited to 72 UTF-8 bytes to avoid BCrypt's truncation behavior. Account data and workspaces are isolated by UUID ownership; feature endpoints must continue to enforce the token's workspace ID when they are added.

## Capture and document API

All document routes require the bearer token. `POST /api/v1/capture/upload` accepts `file` and optional `category` multipart fields. The service accepts PDF, PNG and JPEG files up to 10 MB, verifies the file signature and extension, stores content under a generated UUID, and records the detected media type, size and SHA-256 checksum. Files live outside the repository in `.data/documents` by default; set `SIGNALTRELLIS_DOCUMENT_STORAGE` to change the directory.

- `GET /api/v1/documents?query=invoice&category=PURCHASE&status=UPLOADED` searches only the authenticated user's workspace.
- `GET /api/v1/capture/inbox` lists documents that have not reached `PROCESSED` status.
- `GET /api/v1/documents/{id}` returns metadata; `GET /api/v1/documents/{id}/content` downloads the file as an attachment.
- `PATCH /api/v1/documents/{id}/category` changes the category. Supported values are `UNCATEGORIZED`, `FINANCIAL`, `PURCHASE`, `IDENTITY`, `HOUSING`, `INSURANCE`, `PERSONAL` and `OTHER`.

The initial processing status is `UPLOADED`. Extraction and background processing are later milestones.

## Personal context API

All routes below require authentication and derive the workspace from the access token. Entities are typed records such as products, vendors, subscriptions and policies. Relationships connect two entities and can be traversed from an entity with a bounded graph depth and node count.

- `POST /api/v1/context/entities`, `GET /api/v1/context/entities?type=PRODUCT&query=laptop`, and `GET /api/v1/context/entities/{id}` manage and search context entities.
- `POST /api/v1/context/relationships` connects two entities; `DELETE /api/v1/context/relationships/{id}` removes a connection.
- `GET /api/v1/context/entities/{id}/connections?depth=2&limit=50` traverses connected records. Depth is limited to 3 and the response marks capped results with `truncated: true`.
- `POST /api/v1/events`, `GET /api/v1/events/timeline`, and `GET /api/v1/events/upcoming` manage dated personal events.
- `POST /api/v1/commitments`, `GET /api/v1/commitments`, and `GET /api/v1/commitments/summary` manage obligations. Monetary totals use decimal values and are grouped by currency; overdue status is derived from an open commitment's due date.

Database composite foreign keys prevent relationships, events and commitments from pointing across workspace boundaries. PostgreSQL remains the graph store for this MVP.

## Consequence signals

Signal detection is deterministic Java business logic; no model-generated facts are used. `POST /api/v1/signals/recompute` evaluates the current workspace. The initial rule set detects overdue commitments, commitments due within seven days, clusters of open commitments with nearby due dates, scheduled events that passed within the last week, and events due within the next 30 days.

Signals include priority, a plain-language reason and evidence rows linked to the event or commitment that triggered them. Stable workspace-scoped keys make recomputation idempotent. A signal stays dismissed while its rule still matches, resolves automatically when the rule stops matching, and reopens with history if it matches again later.

- `GET /api/v1/signals?status=ACTIVE&priority=HIGH&type=COMMITMENT_OVERDUE` lists signals with filters.
- `GET /api/v1/signals/{id}` returns signal details, evidence and status history; `/evidence` returns just its evidence.
- `POST /api/v1/signals/{id}/dismiss` and `/resolve` record the user's decision.

Recomputation is currently initiated through the API; background scheduling is a later reliability milestone.

## Action proposals and reminders

All action routes require authentication and use the caller's workspace from the bearer token. A proposal can schedule an in-app reminder and may optionally reference an active signal. Creating a proposal never schedules the reminder immediately; the user must approve it before a reminder record is created. Pending proposals expire automatically after their approval window. Decisions, expiry and reminder scheduling are recorded in the action history.

- `POST /api/v1/actions/proposals` creates a reminder proposal. Include an `Idempotency-Key` header and JSON fields `title`, `scheduledFor`, `expiresAt`, and optional `signalId` and `details`.
- `GET /api/v1/actions/pending` lists proposals awaiting approval; `GET /api/v1/actions` lists all proposals; `GET /api/v1/actions/{id}` returns proposal details, decision, reminder and history.
- `POST /api/v1/actions/{id}/approve` records approval and schedules the in-app reminder. `POST /api/v1/actions/{id}/reject` records rejection. Both accept an optional `{ "reason": "..." }` body.
- `GET /api/v1/actions/history` lists workspace audit events.

The current reminder milestone persists an in-app reminder schedule only. It does not send notifications or let an AI system execute actions.

## Document extraction and review

`POST /api/v1/documents/{id}/extract` extracts text from a PDF locally and returns per-page text plus date and amount candidates with page excerpts. `GET /api/v1/documents/{id}/extraction` retrieves the latest extraction. Use `PATCH /api/v1/documents/{documentId}/extraction/facts/{factId}` with a review status of `CONFIRMED`, `CORRECTED` or `REJECTED`; corrected candidates require `correctedValue`. Candidate values are untrusted suggestions and stay separate from context records until a human confirms them.

This local step supports text-based PDFs and does not call an AI provider. Scanned-PDF/image OCR, structured model extraction and context-graph writes are deferred until the AI credentials and later review flow are configured.

To stop PostgreSQL:

```sh
docker compose down
```

## Project structure

```text
src/main/java/com/signaltrellis/
  api/             HTTP response and error contracts
  health/          Service health endpoint
  SignalTrellisApplication.java
src/main/resources/
  db/migration/    Versioned database migrations
```

## Product architecture

The intended shape is a modular monolith. PostgreSQL is the source of truth for entities, relationships, events, evidence and proposals. Rule-based signal detection remains deterministic. AI can help interpret information and prepare proposals, while consequential actions require explicit user approval. See [docs/architecture.md](docs/architecture.md) and [docs/roadmap.md](docs/roadmap.md).

## Status

This is an early foundation, not a portfolio-ready product. Features and claims will be marked complete only after implementation and verification.
