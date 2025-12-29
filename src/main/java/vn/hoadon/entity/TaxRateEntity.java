package vn.hoadon.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tax_rate")
public class TaxRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "rate", nullable = false, length = 20)
    private String rate;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // --- constructor ---
    public TaxRateEntity() {}

    // --- getter / setter ---
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
