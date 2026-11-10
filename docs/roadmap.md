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

## Milestone 2 — Identity and security (implementation started)

- [x] Registration and login endpoints
- [x] BCrypt password hashing and password input guard
- [x] Short-lived signed JWT authentication
- [x] Account registration creates a personal workspace transactionally
- [x] Authenticated profile endpoint returns the caller's workspace identity
- [ ] Unauthorized-access and registration-flow tests
- [ ] Run against PostgreSQL and verify registration, login and protected-route behavior

## Next milestones

2. Complete identity verification and workspace isolation checks.
3. Capture and document lifecycle.
4. Context entities, relationships, events and commitments.
5. Deterministic signals with evidence.
6. Action proposals, approval states, scheduling and audit.
7. Document extraction with provenance and human correction.
8. Evidence-backed retrieval and evaluation.
9. Tool-using assistant and MCP integration with approval checks.
10. Asynchronous processing, outbox, retries and observability where justified.
11. React + TypeScript dashboard, deployment and portfolio evidence.

Features remain planned until implemented and verified. The GitHub account action and live deployment are not part of this local scaffold.
