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
- [x] Inspect the OpenAPI page in a browser; Swagger UI rendered the registered API operations
- [x] Verify `GET /v3/api-docs` returns HTTP 200 and a specification containing 41 API paths on the isolated latest-source app
- [x] Add and run foundation tests for health responses, malformed request handling, and sanitized unexpected errors (3 passed)
- [x] Verify live health returns HTTP 200, malformed JSON returns structured HTTP 400, and invalid registration fields return structured HTTP 400
- [x] Recheck the updated error handler on an isolated latest-source app without changing the existing app process or database

Final verification is now in progress milestone by milestone. Later milestones remain unverified until their checks below are completed.

## Milestone 2 — Identity and security (implementation and manual verification complete)

- [x] Registration and login endpoints
- [x] BCrypt password hashing and password input guard
- [x] Short-lived signed JWT authentication
- [x] Account registration creates a personal workspace transactionally
- [x] Authenticated profile endpoint returns the caller's workspace identity
- [x] Verify registration, duplicate-email rejection, login, wrong-password rejection, authenticated profile, unauthenticated access, and separate workspaces with synthetic accounts on the isolated PostgreSQL verification database

## Milestone 3 — Capture and document lifecycle (implementation and manual verification complete)

- [x] Upload PDF, PNG and JPEG files with a 10 MB request limit
- [x] Validate file signatures and matching filename extensions
- [x] Store files outside the repository with generated UUID keys
- [x] Persist workspace-owned source and document metadata, SHA-256 and detected media type
- [x] Categorize, search and list unprocessed documents
- [x] Retrieve metadata and download content with workspace ownership enforced
- [x] Apply capture migrations and start the application against local PostgreSQL
- [x] Verify PDF, PNG and JPEG uploads, category search/reclassification, metadata and byte-for-byte download behavior with real requests; reject mismatched signatures and oversized files; confirm cross-workspace access is denied on isolated PostgreSQL
- [ ] Add automated capture and workspace-isolation tests

## Milestone 4 — Context graph, events and commitments (implementation and manual verification complete)

- [x] Add typed life entities and case-insensitive duplicate prevention within each workspace
- [x] Add directed entity relationships with database-enforced same-workspace endpoints
- [x] Add bounded graph traversal with depth/node limits and truncation indicator
- [x] Add dated life events, timeline and upcoming queries
- [x] Add commitments with decimal amounts, currency pairing, due dates and open/overdue summary
- [x] Apply context migrations and start the application against local PostgreSQL
- [x] Verify duplicate entity rejection, relationship creation/traversal, graph depth validation, cross-workspace isolation, event timeline/upcoming queries, and commitment listing/overdue summary with real requests on isolated PostgreSQL
- [ ] Add automated context, relationship and workspace-isolation tests

## Milestone 5 — Deterministic consequence signals (implementation and manual verification complete)

- [x] Add rules for overdue and soon-due commitments, due-date clusters, missed events and approaching events
- [x] Assign deterministic priorities and explainable rule messages
- [x] Persist signals with idempotent workspace-scoped dedupe keys
- [x] Attach event/commitment evidence using workspace-scoped foreign keys
- [x] Reconcile active, dismissed and resolved states during recomputation
- [x] Record detected, reopened, dismissed and resolved transitions in signal history
- [x] Add authenticated list/filter/detail/evidence/dismiss/resolve/recompute endpoints
- [x] Apply signal migrations and start the application against local PostgreSQL
- [x] Verify overdue, due-soon, payment-cluster, and approaching-event rules; evidence/history; idempotent recomputation; dismiss/resolve/reopen lifecycle; and cross-workspace isolation with live requests on isolated PostgreSQL
- [ ] Add automated signal-rule and workspace-isolation tests during final verification

## Milestone 6 — Approval-controlled reminder actions (implementation and manual verification complete)

- [x] Add workspace-owned action proposals with idempotency-key request matching
- [x] Require explicit approve/reject decisions before creating a reminder schedule
- [x] Persist in-app reminder records only after approval
- [x] Audit proposal, decision, expiry and scheduling events
- [x] Expire pending proposals with a scheduled local job
- [x] Add authenticated proposal, pending, decision, detail and history APIs
- [x] Apply action migrations and start the application against local PostgreSQL
- [x] Verify pending proposals create no reminder, idempotent replay and key conflict handling, approve/reject decisions, scheduled reminder creation only after approval, decision conflicts, history, and workspace isolation with live requests on isolated PostgreSQL
- [ ] Verify pending-proposal expiry behavior during final verification
- [ ] Add automated action workflow tests during final verification

## Milestone 7 — Document extraction and human review (local extraction and review manually verified)

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
- [x] Verify local PDF page text and date/amount candidates, disabled AI status, confirm/correct/reject review states, corrected fact linking with source-page evidence, and pending-fact link rejection using a synthetic PDF and live requests
- [ ] Add automated extraction and review tests during final verification

## Milestone 8 — Evidence-backed retrieval and evaluation (local lexical retrieval manually verified)

- [x] Store overlapping page-aware text chunks with source offsets and content hashes
- [x] Add PostgreSQL full-text search and pgvector half-precision vector storage/indexing
- [x] Add authenticated, workspace-scoped document indexing endpoint
- [x] Add authenticated lexical/hybrid retrieval endpoint with page and character citations
- [x] Keep embedding calls disabled by default and fall back to lexical search
- [x] Add a synthetic retrieval evaluation set and metric guidance
- [x] Add a forward migration aligning the document chunk hash column with its entity mapping after startup validation found a `CHAR(64)` / `VARCHAR(64)` mismatch
- [x] Verify the forward migration and clean application startup on the isolated verification database
- [x] Start a fresh isolated pgvector PostgreSQL 17 deployment container and apply all 15 migrations, including the forward hash-type migration
- [x] Verify local chunk indexing, lexical retrieval, page/character citations and workspace isolation with a synthetic PDF on isolated PostgreSQL; embeddings stayed disabled
- [ ] Enable OmniRoute embeddings and measure retrieval quality during final verification

## Milestone 9 — Agentic AI and MCP (implementation in place; verification deferred)

- [x] Add provider-independent Spring AI tool calling for a bounded assistant turn
- [x] Add workspace-scoped document search, upcoming commitment, and active signal tools
- [x] Allow the assistant to prepare a reminder proposal only; approval remains a separate user action
- [x] Add deterministic proposal idempotency to avoid duplicate proposals on retried tool calls
- [x] Expose selected capabilities through a stateless Spring AI MCP server
- [x] Require existing JWT authentication for MCP requests and derive workspace identity from authentication
- [x] Keep assistant and MCP tools disabled by default; cap tool calls per request
- [ ] Configure OmniRoute and verify assistant tool selection during final verification
- [x] Verify unauthenticated MCP requests are rejected and the authenticated assistant endpoint returns its disabled response without calling a model
- [ ] Verify MCP initialization, authenticated tool calls, workspace isolation and approval flow during final verification

## Milestone 10 — Reliable asynchronous processing (basic queue workflow manually verified)

- [x] Add a PostgreSQL-backed transactional processing queue linked to document status changes
- [x] Add an authenticated enqueue/status API for local document extraction and indexing
- [x] Run jobs through a scheduled worker that claims rows with database locks and lease expiry
- [x] Add bounded exponential retries, permanent failure handling and safe failure messages
- [x] Recover jobs abandoned by a stopped worker after their lease expires
- [x] Keep background extraction local-only and embedding-free, regardless of AI provider settings
- [x] Record job outcome counters through Micrometer when a registry is available
- [x] Verify enqueue and repeat enqueue, workspace-isolated status access, and successful worker extraction/indexing on isolated PostgreSQL; fixed JDBC timestamp binding and lazy source ownership loading found during this check
- [ ] Verify concurrent claiming, retries, lease recovery and dead-letter state during final verification
- [ ] Add automated worker and failure-recovery tests during final verification

Kafka and Redis remain deferred: PostgreSQL is already the system of record and durable queue for this modular monolith, and there is no separate consumer or cache workload yet.

## Milestone 11 — Dashboard and deployment (build and smoke checks complete; remaining verification pending)

- [x] Add the responsive React + TypeScript workspace shell and account screens
- [x] Connect the dashboard to signals, source documents, commitments, and pending approvals
- [x] Add user flows for signal review, document upload/local processing, commitments, context entities, assistant requests, and action approvals
- [x] Keep assistant requests user-triggered and display the backend's disabled state without sending AI calls automatically
- [x] Add a separate Nginx + API + pgvector PostgreSQL deployment scaffold with persistent data volumes
- [x] Require deployment database and JWT secrets and default all AI/MCP features off
- [x] Add frontend/backend build workflow and portfolio walkthrough evidence checklist
- [x] Smoke-check sign-in and navigation for Overview, Documents, Signals, Commitments, Context graph, Approvals and AI assistant in the browser
- [x] Verify dashboard list APIs for documents, entities, signals, commitments and approvals; fix nullable search parameters that caused unfiltered document and context lists to fail
- [ ] Complete accessibility, workspace-isolation UI and end-to-end workflow verification
- [x] Build API and web Docker images and validate deployment compose configuration
- [x] Start isolated deployment stack, verify Nginx UI/API health, non-root API user and successful login after API and PostgreSQL restarts; remove the disposable stack and volumes afterward
- [ ] Capture redacted portfolio screenshots and record actual verification results
- [ ] Configure and verify OmniRoute calls only during the explicitly planned provider verification session

Implementation checkboxes record code and documentation work. Behavior and deployment claims remain pending until the final verification tasks above are complete. The GitHub account action and live deployment are not part of this scaffold.
