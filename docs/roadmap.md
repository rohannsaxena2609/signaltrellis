# Roadmap

## Milestone 1 — Foundation

- [x] Java 21 / Spring Boot Maven application skeleton
- [x] PostgreSQL local service definition
- [x] Flyway migration baseline
- [x] Consistent validation and unexpected-error response shape
- [x] Health endpoint and OpenAPI UI configuration
- [x] Product and architecture documentation
- [x] Publish a GitHub repository
- [x] Start the service against local PostgreSQL and apply migrations
- [ ] Inspect the OpenAPI page in a browser

## Milestone 2 — Identity and security (implementation in place; verification pending)

- [x] Registration and login endpoints
- [x] BCrypt password hashing and password input guard
- [x] Short-lived signed JWT authentication
- [x] Account registration creates a personal workspace transactionally
- [x] Authenticated profile endpoint returns the caller's workspace identity
- [ ] Unauthorized-access and registration-flow tests
- [ ] Run against PostgreSQL and verify registration, login and protected-route behavior

## Milestone 3 — Capture and document lifecycle (implementation in place; functional verification pending)

- [x] Upload PDF, PNG and JPEG files with a 10 MB request limit
- [x] Validate file signatures and matching filename extensions
- [x] Store files outside the repository with generated UUID keys
- [x] Persist workspace-owned source and document metadata, SHA-256 and detected media type
- [x] Categorize, search and list unprocessed documents
- [x] Retrieve metadata and download content with workspace ownership enforced
- [x] Apply capture migrations and start the application against local PostgreSQL
- [ ] Verify upload/search/download behavior with real requests
- [ ] Add automated capture and workspace-isolation tests

## Later milestones

4. Context entities, relationships, events and commitments.
5. Deterministic signals with evidence.
6. Action proposals, approval states, scheduling and audit.
7. Document extraction with provenance and human correction.
8. Evidence-backed retrieval and evaluation.
9. Tool-using assistant and MCP integration with approval checks.
10. Asynchronous processing, outbox, retries and observability where justified.
11. React + TypeScript dashboard, deployment and portfolio evidence.

Features remain planned until implemented and verified. The GitHub account action and live deployment are not part of this local scaffold.
