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

## API error contract

Errors use a stable JSON shape with timestamp, HTTP status, machine-readable code, safe message, request path and optional field errors. Unexpected exceptions do not expose stack traces or internal exception messages.
