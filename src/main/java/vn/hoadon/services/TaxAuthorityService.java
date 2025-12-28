package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import vn.hoadon.dto.taxauthority.TaxAuthorityResponse;
import vn.hoadon.dto.taxauthority.TaxAuthorityRequest;
import vn.hoadon.entity.TaxAuthorityEntity;
  import java.util.List;
import java.util.Optional;

public interface TaxAuthorityService {
  
     List<TaxAuthorityEntity> listCities();
    List<TaxAuthorityEntity> listByParent(Long parentId);
    List<TaxAuthorityEntity> listByParentActive(Long parentId);
    Optional<TaxAuthorityEntity> findByCode(Integer code);
    // Tìm kiếm phân trang + lọc theo từ khóa
    // Trong file TaxAuthorityService.java
Page<TaxAuthorityResponse> search(String keyword, Long parentId, Integer status, Pageable pageable);

    // Lấy chi tiết
    TaxAuthorityResponse findById(Long id);

    // Thêm mới
    TaxAuthorityResponse create(TaxAuthorityRequest request);

    // Cập nhật
    TaxAuthorityResponse update(Long id, TaxAuthorityRequest request);

    // Xóa
    void delete(Long id);
}