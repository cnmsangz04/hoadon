package vn.hoadon.ai.knowledge.persistence;

import jakarta.persistence.*;
import vn.hoadon.ai.knowledge.model.KnowledgeDocumentStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_knowledge_document_versions", uniqueConstraints =
        @UniqueConstraint(name = "uk_ai_knowledge_version", columnNames = {"document_id", "version_number"}))
public class KnowledgeDocumentVersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "document_id", nullable = false)
    private Long documentId;
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    @Column(name = "mime_type", length = 150)
    private String mimeType;
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    @Column(nullable = false, length = 64)
    private String checksum;
    @Lob
    @Column(name = "source_blob", nullable = false)
    private byte[] sourceBlob;
    @Lob
    @Column(name = "extracted_content")
    private String extractedContent;
    @Column(nullable = false, length = 24)
    @Enumerated(EnumType.STRING)
    private KnowledgeDocumentStatus status;
    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;
    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;
    @Column(name = "prompt_injection_detected", nullable = false)
    private Boolean promptInjectionDetected = false;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public byte[] getSourceBlob() { return sourceBlob; }
    public void setSourceBlob(byte[] sourceBlob) { this.sourceBlob = sourceBlob; }
    public String getExtractedContent() { return extractedContent; }
    public void setExtractedContent(String extractedContent) { this.extractedContent = extractedContent; }
    public KnowledgeDocumentStatus getStatus() { return status; }
    public void setStatus(KnowledgeDocumentStatus status) { this.status = status; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDateTime getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDateTime effectiveTo) { this.effectiveTo = effectiveTo; }
    public Boolean getPromptInjectionDetected() { return promptInjectionDetected; }
    public void setPromptInjectionDetected(Boolean promptInjectionDetected) { this.promptInjectionDetected = promptInjectionDetected; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
