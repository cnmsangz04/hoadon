package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import vn.hoadon.dto.company.CompanyFilterDTO;
import vn.hoadon.entity.CompanyEntity;

import java.util.Optional;

public interface CompanyService {
	Page<CompanyEntity> list(CompanyFilterDTO filter, Pageable pageable);
    CompanyEntity saveOrUpdate(CompanyEntity company);
    Optional<CompanyEntity> findById(Long id);
    void delete(Long id);
    void updateStatus(Long id, Integer status);
    void sendAdminCredentials(Long companyId);
}