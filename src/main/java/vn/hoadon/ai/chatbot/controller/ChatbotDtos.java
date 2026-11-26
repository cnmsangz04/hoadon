package vn.hoadon.ai.chatbot.controller;

import java.time.LocalDateTime;
import java.util.List;

public final class ChatbotDtos {
    private ChatbotDtos() {}

    public record ConversationView(Long id, String title, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record AskRequest(Long conversationId, String question) {}
    public record FeedbackRequest(Boolean helpful, String feedback) {}
    public record SourceView(long documentId, long versionId, Integer versionNumber, long chunkId, String title, String heading,
                             String sourceLocation, float score) {}
    public record MessageView(Long id, Long conversationId, String role, String content, String requestId,
                              List<SourceView> sources, Boolean helpful, String feedback, LocalDateTime createdAt) {}
    public record AskResponse(ConversationView conversation, MessageView userMessage, MessageView answer) {}
}
