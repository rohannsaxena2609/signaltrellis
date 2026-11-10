# Architecture decisions

## Product boundary

SignalTrellis turns personal information into explainable, approval-controlled action. Its differentiator is the path from connected context to a signal with evidence and a proposed next step.

## Initial architecture

Start as a modular monolith. Keep domain rules in Java and use PostgreSQL as the authoritative store. Introduce asynchronous workers and a transactional outbox when processing workloads require them; do not create distributed services before there is a concrete need.

Planned modules:

- Identity and workspace access
- Capture and document lifecycle
- Context entities and relationships
- Events, commitments and timeline
- Deterministic consequence signals and evidence
- AI-assisted retrieval and recommendations
- Action proposals, approvals and audit trail

## Key invariants

1. A signal must have inspectable evidence and a deterministic reason.
2. Model output is an untrusted proposal; validate it before persistence.
3. Actions with external or consequential effects require explicit user approval.
4. Workspace ownership is enforced at the service and persistence boundaries.
5. Preserve the original source and provenance for extracted facts.

## Milestone 1 decisions

- Java 21 and Spring Boot 4.1.2.
- Maven project coordinates `com.signaltrellis:signaltrellis`.
- PostgreSQL with Flyway migrations; Hibernate validates schema rather than creating it.
- OpenAPI UI for interactive API inspection.
- One deployable application; AI, Kafka, Redis and authentication arrive in later milestones.

## Identity and authentication

- Normalize email addresses to lowercase before storage and lookup.
- Store BCrypt password hashes only; never return hashes from API responses.
- Keep access tokens stateless, signed and short-lived (15 minutes); do not store bearer tokens in the database.
- Registration creates a user and an owned Personal workspace in one transaction.
- Put user and workspace UUIDs into the signed token. Protected feature services must scope every query to the authenticated workspace.
- The configured fallback signing key is for local development only; deployment must provide `SIGNALTRELLIS_JWT_SECRET`.

## Capture and document storage

- `source_items` is the workspace-owned capture record; `documents` stores file metadata and lifecycle state.
- Content is written to a local storage adapter using generated UUID keys. The original filename is metadata only and never becomes a filesystem path.
- Accept only PDF, PNG and JPEG signatures and matching filename extensions; limit uploads to 10 MB.
- Store the server-detected media type, byte count and SHA-256 checksum for later integrity checks.
- Scope document search, metadata, inbox and download queries through the source item's workspace ID from the authenticated token.
- Persist the file before metadata in one request and remove it if the database transaction fails. A process crash between those operations can leave an orphaned local file; a cleanup job can be added with asynchronous processing later.
- Local filesystem storage is the development adapter. A later deployment can replace it with private object storage without exposing storage keys to the API.

## Personal context graph and timeline

- Store life entities and directed relationships in PostgreSQL. Relationship endpoints use composite foreign keys containing `workspace_id`, so the database rejects cross-workspace edges even if an application check is missed.
- Relationship traversal is undirected for exploration, capped at depth 3 and at a caller-selected node limit (maximum 200); responses indicate truncation.
- Events use calendar dates and may link to a workspace-owned entity.
- Commitments use `NUMERIC(14,2)` for amounts, optional ISO-style three-letter currency codes, and a required due date. Amount and currency must be provided together.
- Open and overdue commitment totals are deterministic Java/PostgreSQL calculations; overdue is derived from `status = OPEN` and `due_date < today`.
- Every list and lookup is scoped by the authenticated workspace. Entity IDs supplied on event and commitment requests are checked against that workspace, and composite database constraints enforce the same boundary.

## Deterministic consequence signals

- Five rules evaluate current commitments and scheduled events: overdue commitments, commitments due within seven days, clusters of open commitments with neighboring due dates no more than three days apart, scheduled events missed in the prior seven days, and events approaching in the next 30 days.
- Each rule candidate has a stable workspace-scoped deduplication key. A pessimistic lock on the workspace serializes recomputations and signal status transitions.
- Recompute upserts current matches, refreshes their structured evidence, reopens resolved matches, and resolves active or dismissed signals whose rule no longer matches. Dismissed signals remain dismissed while still applicable.
- Evidence rows reference the source event or commitment through composite workspace foreign keys. Explanations are generated from stored values and rule outcomes, not from an LLM.
- `signal_history` records detection, reopening, dismissal and resolution with actor and timestamp. Repeated identical evaluations do not append redundant history rows.
- Recompute currently runs on an authenticated API request. Scheduling, event publication and asynchronous delivery remain later work.

## API error contract

Errors use a stable JSON shape with timestamp, HTTP status, machine-readable code, safe message, request path and optional field errors. Unexpected exceptions do not expose stack traces or internal exception messages.
