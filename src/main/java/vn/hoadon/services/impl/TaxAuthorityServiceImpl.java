package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoadon.dto.request.TaxAuthorityRequest;
import vn.hoadon.dto.response.TaxAuthorityResponse;
import vn.hoadon.model.TaxAuthority;
import vn.hoadon.repositories.TaxAuthorityRepository;
import vn.hoadon.services.TaxAuthorityService;
import vn.hoadon.utils.SearchCriteria; // Class tiện ích search ở câu trả lời trước

@Service
@Transactional // Đảm bảo tính toàn vẹn dữ liệu
public class TaxAuthorityServiceImpl implements TaxAuthorityService {

    @Autowired
    private TaxAuthorityRepository taxRepo;

    @Override
    @Transactional(readOnly = true) // Tối ưu hiệu năng cho việc đọc
    public Page<TaxAuthorityResponse> search(String keyword, Pageable pageable) {
        // 1. Tạo điều kiện tìm kiếm (tìm theo mã HOẶC tên HOẶC tỉnh)
        Specification<TaxAuthority> spec = SearchCriteria.hasKeyword(keyword, "code", "name");

        // 2. Query DB (Repo đã được tối ưu fetch parent)
        Page<TaxAuthority> entities = taxRepo.findAll(spec, pageable);

        // 3. Convert Entity -> DTO
        return entities.map(this::mapToResponse);
    }

    @Override
    public TaxAuthorityResponse findById(Long id) {
        TaxAuthority entity = taxRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ quan thuế với ID: " + id));
        return mapToResponse(entity);
    }

    @Override
    public TaxAuthorityResponse create(TaxAuthorityRequest req) {
        // Check trùng mã nếu cần
        // if (taxRepo.existsByCode(req.getCode())) throw ...

        TaxAuthority entity = new TaxAuthority();
        if (entity.getParent() != null && entity.getParent().id != null) {
            TaxAuthority parent = taxRepo.findById(entity.getParent().id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CQT cha"));
            entity.setParent(parent);
        } else {
            entity.setParent(null); // Nếu chọn "Không có"
        }
        mapRequestToEntity(req, entity);
        
        TaxAuthority saved = taxRepo.save(entity);
        return mapToResponse(saved);
    }

    @Override
    public TaxAuthorityResponse update(Long id, TaxAuthorityRequest req) {
        TaxAuthority entity = taxRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CQT để cập nhật"));
        if (entity.getParent() != null && entity.getParent().id != null) {
            TaxAuthority parent = taxRepo.findById(entity.getParent().id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CQT cha"));
            entity.setParent(parent);
        } else {
            entity.setParent(null); // Nếu chọn "Không có"
        }
        mapRequestToEntity(req, entity);
        
        TaxAuthority saved = taxRepo.save(entity);
        return mapToResponse(saved);
    }

    @Override
    public void delete(Long id) {
        if (!taxRepo.existsById(id)) {
            throw new RuntimeException("ID không tồn tại");
        }
        // Logic mở rộng: Kiểm tra xem CQT này có con không trước khi xóa?
        taxRepo.deleteById(id);
    }

    // --- Helper Methods: Mapping ---

    private TaxAuthorityResponse mapToResponse(TaxAuthority entity) {
        TaxAuthorityResponse dto = new TaxAuthorityResponse();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setProvinceName(entity.getProvinceName());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());

        // Xử lý quan hệ cha (để tránh NullPointer)
        if (entity.getParent() != null) {
            dto.setParentId(entity.getParent().getId());
            dto.setManagerName(entity.getParent().getName());
        }
        return dto;
    }

    private void mapRequestToEntity(TaxAuthorityRequest req, TaxAuthority entity) {
        entity.setCode(req.getCode());
        entity.setName(req.getName());
        entity.setProvinceName(req.getProvinceName());
        entity.setStatus(req.getStatus());

        // Xử lý gán cha
        if (req.getParentId() != null) {
            // Nếu có gửi parentId lên -> tìm cha gán vào
            TaxAuthority parent = taxRepo.findById(req.getParentId())
                    .orElseThrow(() -> new RuntimeException("Cơ quan quản lý không tồn tại"));
            entity.setParent(parent);
        } else {
            entity.setParent(null);
        }
    }
}