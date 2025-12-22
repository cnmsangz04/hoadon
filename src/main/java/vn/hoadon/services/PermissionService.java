package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoadon.dto.permission.PermissionCreateDTO;
import vn.hoadon.entity.PermissionEntity;
import vn.hoadon.entity.UserEntity;

import java.util.Optional;

public interface PermissionService {
    Page<PermissionEntity> list(String keyword, Pageable pageable);
    Optional<PermissionEntity> findById(Long id);
    PermissionEntity saveOrUpdate(PermissionCreateDTO dto);
    void delete(Long id);

    /**
     * @param user        user hiện tại
     * @param keys        permission key, ngăn cách bằng |
     *                    ví dụ: "user.create|user.update"
     * @param requireAll  true = phải có TẤT CẢ quyền
     *                    false = chỉ cần 1 quyền
     * @param isBoolean   true = chỉ trả về boolean
     *                    false = throw AccessDeniedException nếu không có quyền
     */
    boolean hasPermission(UserEntity user, String keys, boolean requireAll, boolean isBoolean);
}