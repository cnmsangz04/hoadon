package vn.hoadon.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp; // Thêm cái này để tự động cập nhật ngày sửa
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tax_authorities", indexes = {
    @Index(name = "idx_tax_code", columnList = "code"),
    @Index(name = "idx_tax_name", columnList = "name")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TaxAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng (Identity) trong SQL Server
    public Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code; // Mã CQT (VD: 1001)

    // --- SỬA Ở ĐÂY: Dùng nvarchar để lưu tiếng Việt có dấu ---
    @Column(name = "name", columnDefinition = "nvarchar(255)") 
    private String name; // Tên CQT

    @Column(name = "province_name", columnDefinition = "nvarchar(255)")
    private String provinceName; // Tên Cục thuế Tỉnh/Thành
    // ---------------------------------------------------------

    @Column(columnDefinition = "int default 1") // Mặc định là 1 (Hiển thị) nếu không truyền
    private Integer status; 

    // Tự tham chiếu: CQT Quản lý (Cha)
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "parent_id")
    private TaxAuthority parent;

    // Danh sách con
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<TaxAuthority> children = new ArrayList<>();

    @CreationTimestamp // Tự động điền ngày tạo
    @Column(updatable = false) // Không cho phép sửa ngày tạo
    private LocalDateTime createdAt;
    
    @UpdateTimestamp // Tự động cập nhật ngày khi sửa bản ghi (Thêm mới nếu cần)
    private LocalDateTime updatedAt;
}