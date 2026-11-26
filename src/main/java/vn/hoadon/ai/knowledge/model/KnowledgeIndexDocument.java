package vn.hoadon.ai.knowledge.model;

public record KnowledgeIndexDocument(long chunkId, long documentId, long versionId, Long companyId,
                                     boolean externalShareAllowed, String title, String heading, String content) {
}
