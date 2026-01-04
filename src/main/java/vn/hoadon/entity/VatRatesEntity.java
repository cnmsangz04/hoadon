package vn.hoadon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vat_rates")
@Getter
@Setter
public class VatRatesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "label", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String label;

    @Column(name = "code", nullable = false)
    private Integer code;

    @Column(name = "status", nullable = false)
    private Integer status;
    // hoặc Boolean nếu bạn map 0/1

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- constructor ---
    public VatRatesEntity() {}

}
