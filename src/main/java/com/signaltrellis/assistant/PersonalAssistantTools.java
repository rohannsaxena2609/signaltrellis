package com.signaltrellis.assistant;

import com.signaltrellis.identity.AuthenticatedUser;
import com.signaltrellis.retrieval.DocumentRetrievalService;
import com.signaltrellis.signals.SignalService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PersonalAssistantTools {

    private final AssistantCapabilities capabilities;

    public PersonalAssistantTools(AssistantCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    @Tool(name = "searchPersonalDocuments", description = "Search the user's extracted documents and return evidence snippets with source citations.")
    public DocumentRetrievalService.SearchResponse searchPersonalDocuments(
            @ToolParam(description = "A concise search query") String query,
            @ToolParam(description = "Maximum number of evidence results, from 1 to 10") int limit,
            ToolContext context) {
        return capabilities.searchDocuments(actor(context), query, limit);
    }

    @Tool(name = "getUpcomingCommitments", description = "List open commitments due in the recent past or within the requested number of days. Use this for due dates and renewals.")
    public List<AssistantCapabilities.UpcomingCommitment> getUpcomingCommitments(
            @ToolParam(description = "Number of days ahead to include, from 0 to 365") int daysAhead,
            ToolContext context) {
        return capabilities.upcomingCommitments(actor(context), daysAhead);
    }

    @Tool(name = "listActiveSignals", description = "List active, rule-generated consequence signals in the user's workspace, including their explanations.")
    public List<SignalService.SignalResponse> listActiveSignals(
            @ToolParam(description = "Maximum results, from 1 to 20") int limit,
            ToolContext context) {
        return capabilities.activeSignals(actor(context), limit);
    }

    @Tool(name = "prepareReminderProposal", description = "Prepare a reminder proposal only when the user explicitly asks. This creates a pending proposal and never schedules a reminder. A user must approve it through the action approval API.")
    public AssistantCapabilities.PreparedReminderProposal prepareReminderProposal(
            @ToolParam(description = "Short title for the proposed reminder") String title,
            @ToolParam(description = "Why the reminder is useful and its supporting evidence") String details,
            @ToolParam(description = "Reminder time as an ISO-8601 instant in UTC") Instant scheduledFor,
            @ToolParam(description = "Approval expiry as an ISO-8601 instant before scheduledFor") Instant expiresAt,
            @ToolParam(description = "Optional active signal UUID to associate", required = false) UUID signalId,
            ToolContext context) {
        return capabilities.prepareReminderProposal(actor(context), title, details, scheduledFor, expiresAt, signalId);
    }

    private AuthenticatedUser actor(ToolContext context) {
        Object actor = context.getContext().get("actor");
        if (actor instanceof AuthenticatedUser authenticatedUser) return authenticatedUser;
        throw new IllegalStateException("Authenticated assistant context is unavailable");
    }
}
