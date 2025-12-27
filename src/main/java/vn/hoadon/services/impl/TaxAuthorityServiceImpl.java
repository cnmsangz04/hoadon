package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoadon.dto.taxauthority.TaxAuthorityRequest;
import vn.hoadon.dto.taxauthority.TaxAuthorityResponse;
import vn.hoadon.entity.TaxAuthorityEntity;
import vn.hoadon.repositories.TaxAuthorityRepository;
import vn.hoadon.services.TaxAuthorityService;
import vn.hoadon.util.SearchCriteria;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TaxAuthorityServiceImpl implements TaxAuthorityService {

    @Autowired
    private TaxAuthorityRepository taxRepo;

    // --- Các hàm List giữ nguyên ---
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

    // --- SEARCH & CRUD ---

    @Override
    @Transactional(readOnly = true)
    public Page<TaxAuthorityResponse> search(String keyword, Long parentId, Integer status, Pageable pageable) {
        // 1. Tạo Spec tìm kiếm theo Keyword (Code hoặc Name)
        Specification<TaxAuthorityEntity> spec = SearchCriteria.hasKeyword(keyword, "code", "name");

        // 2. Ghép thêm điều kiện ParentId (nếu user có chọn combobox)
        if (parentId != null) {
            spec = spec.and(SearchCriteria.hasEqual("parentId", parentId));
        }

        // 3. Ghép thêm điều kiện Status (nếu user có chọn combobox)
        if (status != null) {
            spec = spec.and(SearchCriteria.hasEqual("status", status));
        }

        // 4. Query DB với bộ lọc tổng hợp
        Page<TaxAuthorityEntity> entities = taxRepo.findAll(spec, pageable);

        // 5. Convert sang DTO Response
        return entities.map(this::mapToResponse);
    }

    @Override
    public TaxAuthorityResponse findById(Long id) {
        TaxAuthorityEntity entity = taxRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu với ID: " + id));
        return mapToResponse(entity);
    }

    @Override
    public TaxAuthorityResponse create(TaxAuthorityRequest req) {
        TaxAuthorityEntity entity = new TaxAuthorityEntity();
        mapRequestToEntity(req, entity);
        
        // Setup thời gian tạo (nếu Entity ko tự generate)
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(java.time.LocalDateTime.now());
        }

        if(entity.getParentId() == null) {
            entity.setParentId(0L); // Mặc định nếu không có cha thì là 0 (cấp cao nhất)
        }

        TaxAuthorityEntity saved = taxRepo.save(entity);
        return mapToResponse(saved);
    }

    @Override
    public TaxAuthorityResponse update(Long id, TaxAuthorityRequest req) {
        TaxAuthorityEntity entity = taxRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu để cập nhật"));
        mapRequestToEntity(req, entity);
         if(entity.getParentId() == null) {
            entity.setParentId(0L); // Mặc định nếu không có cha thì là 0 (cấp cao nhất)
        }
        entity.setUpdatedAt(java.time.LocalDateTime.now());
        
        TaxAuthorityEntity saved = taxRepo.save(entity);
        return mapToResponse(saved);
    }

    @Override
    public void delete(Long id) {
        if (!taxRepo.existsById(id)) {
            throw new RuntimeException("ID không tồn tại");
        }
        taxRepo.deleteById(id);
    }

    // ==========================================
    // KHU VỰC MAPPING DỮ LIỆU (QUAN TRỌNG)
    // ==========================================

    private TaxAuthorityResponse mapToResponse(TaxAuthorityEntity entity) {
        TaxAuthorityResponse dto = new TaxAuthorityResponse();
        
        dto.setId(entity.getId());
        dto.setCode(entity.getCode() != null ? String.valueOf(entity.getCode()) : "");
        dto.setName(entity.getName());
        dto.setParentId(entity.getParentId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setProvinceName(entity.getProvinceName()); // Mở comment nếu Entity có trường này

        // --- LOGIC LẤY TÊN QUẢN LÝ (MANAGER NAME) ---
        if (entity.getParentId() != null && entity.getParentId() > 0) {
            // Tìm tên của cha dựa vào parentId
            Optional<TaxAuthorityEntity> parentOpt = taxRepo.findById(entity.getParentId());
            if (parentOpt.isPresent()) {
                dto.setManagerName(parentOpt.get().getName());
            } else {
                dto.setManagerName("Không tìm thấy cha");
            }
        } else {
            dto.setManagerName(null); // Hoặc để null để Frontend hiển thị "-- Cấp cao nhất --"
        }

        return dto;
    }

    private void mapRequestToEntity(TaxAuthorityRequest req, TaxAuthorityEntity entity) {
        // Convert Code String -> Integer
        if (req.getCode() != null && !req.getCode().trim().isEmpty()) {
            try {
                entity.setCode(Integer.valueOf(req.getCode().trim()));
            } catch (NumberFormatException e) {
                // Có thể throw lỗi hoặc log warning
                entity.setCode(null); 
            }
        }

        entity.setName(req.getName());
        entity.setStatus(req.getStatus());
        
        // Kiểm tra logic cha con: Không được chọn chính mình làm cha
        if (entity.getId() != null && entity.getId().equals(req.getParentId())) {
             throw new RuntimeException("Không thể chọn chính cơ quan này làm cấp quản lý");
        }
        entity.setParentId(req.getParentId());
        
        entity.setProvinceName(req.getProvinceName()); // Mở comment nếu Entity có trường này
    }
}