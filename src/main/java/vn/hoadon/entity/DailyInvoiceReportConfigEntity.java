package vn.hoadon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_invoice_report_configs")
public class DailyInvoiceReportConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "report_enabled", nullable = false)
    private Boolean reportEnabled = false;

    @Column(name = "daily_hour", nullable = false)
    private Integer dailyHour = 1;

    @Column(name = "daily_minute", nullable = false)
    private Integer dailyMinute = 0;

    @Column(name = "last_report_date")
    private LocalDate lastReportDate;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Boolean getReportEnabled() { return reportEnabled; }
    public void setReportEnabled(Boolean reportEnabled) { this.reportEnabled = reportEnabled; }

    public Integer getDailyHour() { return dailyHour; }
    public void setDailyHour(Integer dailyHour) { this.dailyHour = dailyHour; }

    public Integer getDailyMinute() { return dailyMinute; }
    public void setDailyMinute(Integer dailyMinute) { this.dailyMinute = dailyMinute; }

    public LocalDate getLastReportDate() { return lastReportDate; }
    public void setLastReportDate(LocalDate lastReportDate) { this.lastReportDate = lastReportDate; }

    public LocalDateTime getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(LocalDateTime lastSentAt) { this.lastSentAt = lastSentAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
