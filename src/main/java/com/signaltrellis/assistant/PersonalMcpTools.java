package com.signaltrellis.assistant;

import com.signaltrellis.identity.AuthenticatedUser;
import com.signaltrellis.retrieval.DocumentRetrievalService;
import com.signaltrellis.signals.SignalService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "spring.ai.mcp.server.annotation-scanner.enabled", havingValue = "true")
public class PersonalMcpTools {

    private final AssistantCapabilities capabilities;

    public PersonalMcpTools(AssistantCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    @McpTool(name = "signaltrellis_search_personal_documents", description = "Search the authenticated user's extracted documents and return cited evidence.")
    public DocumentRetrievalService.SearchResponse searchPersonalDocuments(
            @McpToolParam(description = "A concise search query", required = true) String query,
            @McpToolParam(description = "Maximum result count from 1 to 10", required = true) int limit) {
        return capabilities.searchDocuments(actor(), query, limit);
    }

    @McpTool(name = "signaltrellis_get_upcoming_commitments", description = "List open commitments due recently or within the requested number of days.")
    public List<AssistantCapabilities.UpcomingCommitment> getUpcomingCommitments(
            @McpToolParam(description = "Days ahead, from 0 to 365", required = true) int daysAhead) {
        return capabilities.upcomingCommitments(actor(), daysAhead);
    }

    @McpTool(name = "signaltrellis_list_active_signals", description = "List active rule-generated consequence signals in the authenticated user's workspace.")
    public List<SignalService.SignalResponse> listActiveSignals(
            @McpToolParam(description = "Maximum result count from 1 to 20", required = true) int limit) {
        return capabilities.activeSignals(actor(), limit);
    }

    @McpTool(name = "signaltrellis_prepare_reminder_proposal", description = "When explicitly requested, create a pending reminder proposal. It never schedules a reminder; authenticated user approval is required separately.")
    public AssistantCapabilities.PreparedReminderProposal prepareReminderProposal(
            @McpToolParam(description = "Short reminder title", required = true) String title,
            @McpToolParam(description = "Reason and evidence for this proposal", required = true) String details,
            @McpToolParam(description = "Reminder time as an ISO-8601 UTC instant", required = true) Instant scheduledFor,
            @McpToolParam(description = "Approval expiry as an ISO-8601 UTC instant before the reminder", required = true) Instant expiresAt,
            @McpToolParam(description = "Optional active signal UUID", required = false) UUID signalId) {
        return capabilities.prepareReminderProposal(actor(), title, details, scheduledFor, expiresAt, signalId);
    }

    private AuthenticatedUser actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) return user;
        throw new IllegalStateException("Authenticated MCP context is unavailable");
    }
}
