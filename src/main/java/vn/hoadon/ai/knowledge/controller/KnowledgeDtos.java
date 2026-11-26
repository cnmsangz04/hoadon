package vn.hoadon.ai.knowledge.controller;

import vn.hoadon.ai.knowledge.model.KnowledgeDocumentStatus;
import vn.hoadon.ai.knowledge.model.KnowledgeDocumentType;
import vn.hoadon.ai.knowledge.model.KnowledgeReference;
import vn.hoadon.ai.knowledge.model.KnowledgeScope;

import java.time.LocalDateTime;
import java.util.List;

public final class KnowledgeDtos {
    private KnowledgeDtos() {
    }

    public record CategoryRequest(String code, String name, String description, Boolean active) {
    }

    public record MarkdownRequest(String title, Long categoryId, KnowledgeScope scope, Long companyId,
                                  Boolean externalShareAllowed, String content,
                                  LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
    }

    public record MetadataRequest(String title, Long categoryId, KnowledgeScope scope, Long companyId,
                                  Boolean externalShareAllowed, LocalDateTime effectiveFrom,
                                  LocalDateTime effectiveTo) {
    }

    public record RetrievalRequest(String query, Long companyId, Integer limit, Boolean externalOnly) {
    }

    public record AskRequest(String question, Long companyId) {
    }

    public record DocumentView(Long id, String title, Long companyId, Long categoryId,
                               KnowledgeDocumentType documentType, KnowledgeScope scope, boolean active,
                               boolean externalShareAllowed, KnowledgeDocumentStatus status,
                               Long currentVersionId, Integer currentVersion, String originalFilename,
                               boolean promptInjectionDetected, int chunkCount, String content,
                               LocalDateTime effectiveFrom, LocalDateTime effectiveTo,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record VersionView(Long id, Integer versionNumber, String originalFilename, String mimeType,
                              Long fileSize, String checksum, KnowledgeDocumentStatus status,
                              boolean promptInjectionDetected, String errorMessage,
                              LocalDateTime effectiveFrom, LocalDateTime effectiveTo,
                              LocalDateTime createdAt) {
    }

    public record RetrievalResponse(String query, List<KnowledgeReference> sources) {
    }

    public record AnswerResponse(String answer, String requestId, List<KnowledgeReference> sources) {
    }
}
