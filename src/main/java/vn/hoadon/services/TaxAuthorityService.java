package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import vn.hoadon.dto.request.TaxAuthorityRequest;
import vn.hoadon.dto.response.TaxAuthorityResponse;

public interface TaxAuthorityService {
    // Tìm kiếm phân trang + lọc theo từ khóa
    Page<TaxAuthorityResponse> search(String keyword, Pageable pageable);

    // Lấy chi tiết
    TaxAuthorityResponse findById(Long id);

    // Thêm mới
    TaxAuthorityResponse create(TaxAuthorityRequest request);

    // Cập nhật
    TaxAuthorityResponse update(Long id, TaxAuthorityRequest request);

    // Xóa
    void delete(Long id);
}