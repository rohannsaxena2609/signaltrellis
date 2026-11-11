# SignalTrellis portfolio walkthrough

SignalTrellis is a personal context and action intelligence workspace. It connects user-owned records, commitments, source documents, and deterministic signals. An optional assistant can prepare a reminder proposal, while the user keeps the approval decision.

## Suggested walkthrough

1. Create a personal account and open the workspace overview.
2. Add a commitment with a due date, then open the Signals page to inspect the rule explanation and linked evidence.
3. Upload a PDF, PNG, or JPEG and review its status in Documents. Start local extraction and indexing; background processing does not call an AI provider.
4. Review extracted candidate facts. Confirm, correct, or reject them only after checking the displayed source excerpt and page.
5. Add typed context records and inspect the bounded connections view.
6. If the assistant has been deliberately enabled by the workspace operator, submit a question and inspect the response. A prepared reminder remains pending until it is approved or rejected in the approval queue.

Use synthetic or redacted documents in portfolio screenshots. Do not capture passwords, bearer tokens, API keys, or real personal records.

## Evidence checklist

- [ ] Capture the overview at desktop width and mobile width.
- [ ] Capture a signal with its explanation and evidence visible.
- [ ] Capture a document with extraction status and a reviewed candidate.
- [ ] Capture the context graph with non-sensitive sample records.
- [ ] Capture a pending action and its approval controls.
- [ ] Record the result of registration, workspace isolation, document lifecycle, and approval-flow verification.
- [ ] Record container startup, health, persistence across restart, and shutdown results.
- [ ] Record OmniRoute assistant and embedding checks only if those provider calls are explicitly enabled for the verification session.

The unchecked items are intentionally deferred to the final testing and verification phase. No deployment or provider response is claimed by this document.

## Current implementation boundaries

- Signal detection uses deterministic application rules.
- OCR and background document processing run locally.
- LLM extraction, assistant calls, embeddings, and MCP are disabled by default.
- Reviewed facts remain separate from context records unless linked by an authenticated user action.
- The assistant can prepare reminder proposals, while approval is handled separately.
- The deployment Compose file is a starting scaffold and still needs operational and security review before public hosting.
