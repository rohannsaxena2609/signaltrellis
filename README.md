# SignalTrellis

**Personal Context & Action Intelligence**  
Connect the context. See what matters. Take action.

SignalTrellis connects personal records, commitments and events into a context graph. A deterministic signal engine finds what needs attention, explains its evidence and prepares actions for user approval.

## Current milestone

Milestone 1 establishes a running Java backend, PostgreSQL with Flyway, consistent API errors, and OpenAPI documentation. Authentication, document ingestion, graph features and AI integrations are planned milestones and are not implemented yet.

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

The API listens on `http://localhost:8080`. OpenAPI UI is at `http://localhost:8080/swagger-ui.html`; the OpenAPI document is at `/v3/api-docs`. The initial migration creates the application schema. No AI provider credentials are required for this milestone.

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
