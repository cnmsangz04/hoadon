package vn.hoadon.ai.knowledge.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_knowledge_chunks", indexes = {
        @Index(name = "ix_ai_knowledge_chunks_document", columnList = "document_id,version_id"),
        @Index(name = "ix_ai_knowledge_chunks_version", columnList = "version_id,chunk_index")
})
public class KnowledgeChunkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "document_id", nullable = false)
    private Long documentId;
    @Column(name = "version_id", nullable = false)
    private Long versionId;
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;
    @Column(length = 500)
    private String heading;
    @Column(name = "source_location", nullable = false, length = 500)
    private String sourceLocation;
    @Lob
    @Column(nullable = false)
    private String content;
    @Column(name = "estimated_tokens", nullable = false)
    private Integer estimatedTokens;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }
    public String getSourceLocation() { return sourceLocation; }
    public void setSourceLocation(String sourceLocation) { this.sourceLocation = sourceLocation; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getEstimatedTokens() { return estimatedTokens; }
    public void setEstimatedTokens(Integer estimatedTokens) { this.estimatedTokens = estimatedTokens; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
