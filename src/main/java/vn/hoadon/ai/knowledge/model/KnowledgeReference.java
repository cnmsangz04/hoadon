package vn.hoadon.ai.knowledge.model;

public record KnowledgeReference(long documentId, long versionId, long chunkId, String title,
                                 String heading, String sourceLocation, String content, float score) {
}
