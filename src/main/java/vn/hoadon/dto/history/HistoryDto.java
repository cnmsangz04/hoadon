package vn.hoadon.dto.history;

import java.time.LocalDateTime;

public class HistoryDto {
    private Long id;
    private Long companyId;
    private Long userId;
    private String tableName;
    private Long tableId;
    private String title;
    private String description;
    private Integer showNotify;
    private Integer status;
    private Integer type;
    private String xmlData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String username;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getShowNotify() { return showNotify; }
    public void setShowNotify(Integer showNotify) { this.showNotify = showNotify; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getXmlData() { return xmlData; }
    public void setXmlData(String xmlData) { this.xmlData = xmlData; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}