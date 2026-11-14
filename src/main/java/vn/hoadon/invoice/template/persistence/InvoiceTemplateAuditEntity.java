package vn.hoadon.invoice.template.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "invoice_template_audit")
@Immutable
public class InvoiceTemplateAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = true)
    private Long companyId;

    @Column(name = "actor_id", nullable = true)
    private Long actorId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "request_key", nullable = true, length = 64)
    private String requestKey;

    @Column(name = "request_hash", nullable = true, length = 64)
    private String requestHash;

    @Column(name = "result_id", nullable = true)
    private Long resultId;

    @Column(name = "details_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String detailsJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public Long getResultId() { return resultId; }
    public void setResultId(Long resultId) { this.resultId = resultId; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
