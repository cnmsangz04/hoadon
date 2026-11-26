package vn.hoadon.ai.invoice.controller;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class InvoiceAssistantDtos {
    private InvoiceAssistantDtos() {}

    public record PreviewRequest(String command, Long invoiceId, Long formId, Integer category,
                                 JsonNode current, Map<String, Long> selections) {}
    public record ActionView(String id, String action, String description) {}
    public record CandidateView(Long id, String code, String name, String detail) {}
    public record ChoiceView(String actionId, String entityType, String query, List<CandidateView> candidates) {}
    public record PreviewResponse(Long proposalId, String status, String message, List<ActionView> actions,
                                  List<ChoiceView> choices, JsonNode before, JsonNode after,
                                  LocalDateTime expiresAt) {}
    public record ConfirmResponse(Long proposalId, Long invoiceId, boolean alreadyApplied, JsonNode invoice) {}
}
