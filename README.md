# SignalTrellis

**Personal Context & Action Intelligence**  
Connect the context. See what matters. Take action.

SignalTrellis connects personal records, commitments and events into a context graph. A deterministic signal engine finds what needs attention, explains its evidence and prepares actions for user approval.

## Current milestone

Milestone 1 laid down the Java backend, PostgreSQL with Flyway, consistent API errors, and OpenAPI documentation. Milestone 2 adds account registration, login, short-lived JWTs and a personal workspace. Document ingestion, graph features and AI integrations are later milestones.

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
