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

Automated and real-request behavior testing for these implementation milestones is deferred to final verification, per the project workflow.

## Milestone 2 — Identity and security (implementation in place; verification deferred)

- [x] Registration and login endpoints
- [x] BCrypt password hashing and password input guard
- [x] Short-lived signed JWT authentication
- [x] Account registration creates a personal workspace transactionally
- [x] Authenticated profile endpoint returns the caller's workspace identity
- [ ] Unauthorized-access and registration-flow tests
- [ ] Run against PostgreSQL and verify registration, login and protected-route behavior

## Milestone 3 — Capture and document lifecycle (implementation in place; verification deferred)

- [x] Upload PDF, PNG and JPEG files with a 10 MB request limit
- [x] Validate file signatures and matching filename extensions
- [x] Store files outside the repository with generated UUID keys
- [x] Persist workspace-owned source and document metadata, SHA-256 and detected media type
- [x] Categorize, search and list unprocessed documents
- [x] Retrieve metadata and download content with workspace ownership enforced
- [x] Apply capture migrations and start the application against local PostgreSQL
- [ ] Verify upload/search/download behavior with real requests
- [ ] Add automated capture and workspace-isolation tests

## Milestone 4 — Context graph, events and commitments (implementation in place; verification deferred)

- [x] Add typed life entities and case-insensitive duplicate prevention within each workspace
- [x] Add directed entity relationships with database-enforced same-workspace endpoints
- [x] Add bounded graph traversal with depth/node limits and truncation indicator
- [x] Add dated life events, timeline and upcoming queries
- [x] Add commitments with decimal amounts, currency pairing, due dates and open/overdue summary
- [x] Apply context migrations and start the application against local PostgreSQL
- [ ] Verify entity isolation, graph traversal, event and commitment APIs with real requests
- [ ] Add automated context, relationship and workspace-isolation tests

## Milestone 5 — Deterministic consequence signals (implementation in place; verification deferred)

- [x] Add rules for overdue and soon-due commitments, due-date clusters, missed events and approaching events
- [x] Assign deterministic priorities and explainable rule messages
- [x] Persist signals with idempotent workspace-scoped dedupe keys
- [x] Attach event/commitment evidence using workspace-scoped foreign keys
- [x] Reconcile active, dismissed and resolved states during recomputation
- [x] Record detected, reopened, dismissed and resolved transitions in signal history
- [x] Add authenticated list/filter/detail/evidence/dismiss/resolve/recompute endpoints
- [x] Apply signal migrations and start the application against local PostgreSQL
- [ ] Verify rule outcomes and lifecycle transitions during final verification
- [ ] Add automated signal-rule and workspace-isolation tests during final verification

## Milestone 6 — Approval-controlled reminder actions (implementation in place; verification deferred)

- [x] Add workspace-owned action proposals with idempotency-key request matching
- [x] Require explicit approve/reject decisions before creating a reminder schedule
- [x] Persist in-app reminder records only after approval
- [x] Audit proposal, decision, expiry and scheduling events
- [x] Expire pending proposals with a scheduled local job
- [x] Add authenticated proposal, pending, decision, detail and history APIs
- [x] Apply action migrations and start the application against local PostgreSQL
- [ ] Verify approval, idempotency, expiry and workspace-isolation behavior during final verification
- [ ] Add automated action workflow tests during final verification

## Milestone 7 — Document extraction and human review (implementation in place; verification deferred)

- [x] Extract per-page text from uploaded PDFs using local Apache PDFBox
- [x] Persist page-level text with extraction and workspace provenance
- [x] Identify date and amount candidates with source-page excerpts
- [x] Add human confirm, correct and reject review states with reviewer/timestamp
- [x] Add authenticated extraction and review endpoints
- [x] Add local Tesseract OCR for scanned PDF pages and uploaded PNG/JPEG files
- [x] Add configurable Spring AI OpenAI-compatible client for OmniRoute
- [x] Extract typed candidate facts with source page and exact-quote validation
- [x] Keep AI extraction disabled by default, cap request/response size and disable retries
- [x] Persist AI status, model identifier, input size and safe failure information
- [x] Add provider-backed structured LLM extraction, enabled only when runtime credentials are configured
- [ ] Configure the OmniRoute key in a local shell and verify a real model response during final verification
- [x] Link confirmed/corrected facts to existing workspace entities, events, and commitments
- [x] Enforce workspace ownership for linked context records and retain source page evidence in link responses
- [ ] Verify extraction and review behavior and add automated tests during final verification

## Milestone 8 — Evidence-backed retrieval and evaluation (implementation in place; verification deferred)

- [x] Store overlapping page-aware text chunks with source offsets and content hashes
- [x] Add PostgreSQL full-text search and pgvector half-precision vector storage/indexing
- [x] Add authenticated, workspace-scoped document indexing endpoint
- [x] Add authenticated lexical/hybrid retrieval endpoint with page and character citations
- [x] Keep embedding calls disabled by default and fall back to lexical search
- [x] Add a synthetic retrieval evaluation set and metric guidance
- [ ] Recreate the local PostgreSQL container with pgvector and apply the new migration
- [ ] Verify indexing, citation offsets, workspace isolation and retrieval quality during final verification
- [ ] Enable OmniRoute embeddings and measure retrieval quality during final verification

## Later milestones

9. Tool-using assistant and MCP integration with approval checks.
10. Asynchronous processing, outbox, retries and observability where justified.
11. React + TypeScript dashboard, deployment and portfolio evidence.

Features remain planned until implemented and verified. The GitHub account action and live deployment are not part of this local scaffold.
