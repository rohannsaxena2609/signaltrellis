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

`POST /api/v1/documents/{id}/extract` extracts text locally and returns per-page text plus date and amount candidates with page excerpts. Text-based PDFs use PDFBox; scanned PDF pages and uploaded PNG/JPEG images use the local Tesseract executable. `GET /api/v1/documents/{id}/extraction` retrieves the latest extraction. Use `PATCH /api/v1/documents/{documentId}/extraction/facts/{factId}` with a review status of `CONFIRMED`, `CORRECTED` or `REJECTED`; corrected candidates require `correctedValue`. Candidate values are untrusted suggestions and stay separate from context records until a human confirms them.

The OCR executable path defaults to `tesseract` and can be configured with `SIGNALTRELLIS_TESSERACT_PATH`; languages default to `eng` and can be changed with `SIGNALTRELLIS_OCR_LANGUAGES`. OCR is local. OCR is limited to 25 pages per request and 40 megapixels per image/page.

Structured fact extraction can use OmniRoute's OpenAI-compatible endpoint. Start OmniRoute separately, then configure the app in the same shell without putting the key in source control:

```sh
export SIGNALTRELLIS_AI_API_KEY='your-OmniRoute-key'
export SIGNALTRELLIS_AI_ENABLED=true
export SIGNALTRELLIS_AI_BASE_URL=http://localhost:20128
export SIGNALTRELLIS_AI_MODEL=auto/best-free
mvn spring-boot:run
```

The OpenAI-compatible chat path is `/v1/chat/completions`; the app sends a maximum of 20,000 text characters and 2,048 completion tokens in one request, and automatic retries are disabled to avoid repeated usage. AI extraction defaults to disabled. `auto/best-free` is selected from the models reported by your OmniRoute instance, but the route's actual availability and provider terms depend on OmniRoute and its connected providers. The request is idempotent for an already extracted document; posting `/extract` again returns the saved result without another model call.

AI output remains a candidate. The app requires an exact source quote on a valid page before saving a candidate, records model and request-size metadata, and still requires human confirmation/correction/rejection. It does not write AI output into the context graph automatically.

After reviewing a candidate as `CONFIRMED` or `CORRECTED`, link it to an existing context entity, event, or commitment with `POST /api/v1/documents/{documentId}/extraction/facts/{factId}/context-links`, using `{ "targetType": "ENTITY", "targetId": "..." }` (or `EVENT` / `COMMITMENT`). The target must belong to the authenticated workspace. `GET` on the same route returns its source evidence and linked record reference. This associates verified provenance with a record; it does not create or mutate context data.

## Document indexing and evidence retrieval

After a document has been extracted, `POST /api/v1/documents/{id}/index` creates overlapping text chunks with page and character offsets. Indexing is idempotent for an extraction. Embeddings are disabled by default (`SIGNALTRELLIS_EMBEDDINGS_ENABLED=false`), so this route only stores chunks and makes no OmniRoute call. The current limit is 20,000 extracted characters per document; configure it with `SIGNALTRELLIS_RETRIEVAL_MAX_CHARS`.

`POST /api/v1/retrieval/search` accepts `{ "query": "renewal date", "limit": 5 }` and returns ranked evidence snippets with filename, page number, chunk index, character offsets, and reference number. Search uses PostgreSQL full-text ranking by default. If embeddings are explicitly enabled and available, it embeds the query and uses hybrid lexical/semantic ranking; if embedding fails or no matching vectors exist, it falls back to lexical results. Search never sends retrieved document text to a chat model.

The local database image is `pgvector/pgvector:pg17`, required by the vector migration. If a PostgreSQL 17 container is already running from the older `postgres:17-alpine` image, recreate only its container before starting the app so the extension can be installed; the Compose volume remains the same. Do not delete the volume. The migration creates the `vector` extension and a half-precision vector index sized for 3,072 dimensions. Embedding calls remain off until you explicitly set `SIGNALTRELLIS_EMBEDDINGS_ENABLED=true` and configure the OmniRoute key/base URL. A synthetic evaluation set and suggested metrics are in `docs/retrieval-evaluation.md` and `docs/retrieval-evaluation.jsonl`.

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
