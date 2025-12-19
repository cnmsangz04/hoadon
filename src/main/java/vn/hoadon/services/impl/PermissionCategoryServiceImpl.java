package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.hoadon.entity.PermissionCategoryEntity;
import vn.hoadon.repositories.PermissionCategoryRepository;
import vn.hoadon.services.PermissionCategoryService;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PermissionCategoryServiceImpl implements PermissionCategoryService {

    @Autowired
    private PermissionCategoryRepository repository;

    @Override
    public Page<PermissionCategoryEntity> list(String keyword, Pageable pageable) {
        Specification<PermissionCategoryEntity> spec = (root, query, cb) -> {
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                return cb.like(root.get("name"), like);
            }
            return cb.conjunction();
        };
        return repository.findAll(spec, pageable);
    }

    @Override
    public Optional<PermissionCategoryEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public PermissionCategoryEntity saveOrUpdate(PermissionCategoryEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        if (entity.getId() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        return repository.save(entity);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
