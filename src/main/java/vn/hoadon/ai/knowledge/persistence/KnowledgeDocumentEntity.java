package vn.hoadon.ai.knowledge.persistence;

import jakarta.persistence.*;
import vn.hoadon.ai.knowledge.model.KnowledgeDocumentStatus;
import vn.hoadon.ai.knowledge.model.KnowledgeDocumentType;
import vn.hoadon.ai.knowledge.model.KnowledgeScope;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_knowledge_documents", indexes = {
        @Index(name = "ix_ai_knowledge_documents_company", columnList = "company_id,active"),
        @Index(name = "ix_ai_knowledge_documents_category", columnList = "category_id")
})
public class KnowledgeDocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "category_id")
    private Long categoryId;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(name = "document_type", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private KnowledgeDocumentType documentType;
    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private KnowledgeScope scope;
    @Column(nullable = false)
    private Boolean active = false;
    @Column(name = "external_share_allowed", nullable = false)
    private Boolean externalShareAllowed = false;
    @Column(nullable = false, length = 24)
    @Enumerated(EnumType.STRING)
    private KnowledgeDocumentStatus status = KnowledgeDocumentStatus.PROCESSING;
    @Column(name = "current_version_id")
    private Long currentVersionId;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public KnowledgeDocumentType getDocumentType() { return documentType; }
    public void setDocumentType(KnowledgeDocumentType documentType) { this.documentType = documentType; }
    public KnowledgeScope getScope() { return scope; }
    public void setScope(KnowledgeScope scope) { this.scope = scope; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getExternalShareAllowed() { return externalShareAllowed; }
    public void setExternalShareAllowed(Boolean externalShareAllowed) { this.externalShareAllowed = externalShareAllowed; }
    public KnowledgeDocumentStatus getStatus() { return status; }
    public void setStatus(KnowledgeDocumentStatus status) { this.status = status; }
    public Long getCurrentVersionId() { return currentVersionId; }
    public void setCurrentVersionId(Long currentVersionId) { this.currentVersionId = currentVersionId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
