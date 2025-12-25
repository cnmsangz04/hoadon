package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoadon.dto.request.TaxAuthorityRequest;
import vn.hoadon.dto.response.TaxAuthorityResponse;
import vn.hoadon.entity.TaxAuthorityEntity;
import vn.hoadon.repositories.TaxAuthorityRepository;
import vn.hoadon.services.TaxAuthorityService;
import vn.hoadon.utils.SearchCriteria; // Class tiện ích search ở câu trả lời trước
import java.util.List;
import java.util.Optional;

@Service
@Transactional // Đảm bảo tính toàn vẹn dữ liệu
public class TaxAuthorityServiceImpl implements TaxAuthorityService {

    @Autowired
    private TaxAuthorityRepository taxRepo;
    public TaxAuthorityServiceImpl(TaxAuthorityRepository repo) {
        this.taxRepo = repo;
    }

    @Override
    public List<TaxAuthorityEntity> listCities() {
        return taxRepo.findByParentIdAndStatus(0L, 1);
    }

    @Override
    public List<TaxAuthorityEntity> listByParent(Long parentId) {
        return taxRepo.findByParentId(parentId);
    }

    @Override
    public List<TaxAuthorityEntity> listByParentActive(Long parentId) {
        return taxRepo.findByParentIdAndStatus(parentId, 1);
    }

    @Override
    public Optional<TaxAuthorityEntity> findByCode(Integer code) {
        return taxRepo.findByCode(code);
    }
    @Override
    @Transactional(readOnly = true) // Tối ưu hiệu năng cho việc đọc
    public Page<TaxAuthorityResponse> search(String keyword, Pageable pageable) {
        // 1. Tạo điều kiện tìm kiếm (tìm theo mã HOẶC tên HOẶC tỉnh)
        Specification<TaxAuthorityEntity> spec = SearchCriteria.hasKeyword(keyword, "code", "name");

        // 2. Query DB (Repo đã được tối ưu fetch parent)
        Page<TaxAuthorityEntity> entities = taxRepo.findAll(spec, pageable);

        // 3. Convert Entity -> DTO
        return entities.map(this::mapToResponse);
    }

    @Override
    public TaxAuthorityResponse findById(Long id) {
        TaxAuthorityEntity entity = taxRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ quan thuế với ID: " + id));
        return mapToResponse(entity);
    }

    @Override
    public TaxAuthorityResponse create(TaxAuthorityRequest req) {
        TaxAuthorityEntity entity = new TaxAuthorityEntity();
        mapRequestToEntity(req, entity);
        TaxAuthorityEntity saved = taxRepo.save(entity);
        return mapToResponse(saved);
    }

    @Override
    public TaxAuthorityResponse update(Long id, TaxAuthorityRequest req) {
        TaxAuthorityEntity entity = taxRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CQT để cập nhật"));
        mapRequestToEntity(req, entity);
        TaxAuthorityEntity saved = taxRepo.save(entity);
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

    private TaxAuthorityResponse mapToResponse(TaxAuthorityEntity entity) {
        TaxAuthorityResponse dto = new TaxAuthorityResponse();
        // Lombok @Data tạo sẵn setter theo field định nghĩa trong DTO
        dto.setId(entity.getId());
        // Chuyển mã code Integer -> String cho DTO nếu cần
        dto.setCode(entity.getCode() != null ? String.valueOf(entity.getCode()) : null);
        dto.setName(entity.getName());
        dto.setParentId(entity.getParentId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        // managerName không thể suy ra vì Entity chỉ lưu parentId (Long)
        return dto;
    }

    private void mapRequestToEntity(TaxAuthorityRequest req, TaxAuthorityEntity entity) {
        // Request.code là String, entity.code là Integer
        if (req.getCode() != null && !req.getCode().isEmpty()) {
            try {
                entity.setCode(Integer.valueOf(req.getCode()));
            } catch (NumberFormatException ex) {
                throw new RuntimeException("Mã CQT phải là số hợp lệ");
            }
        } else {
            entity.setCode(null);
        }
        entity.setName(req.getName());
        entity.setStatus(req.getStatus());

        // Gán cha trực tiếp theo ID (Entity chỉ có Long parentId)
        entity.setParentId(req.getParentId());
    }
}