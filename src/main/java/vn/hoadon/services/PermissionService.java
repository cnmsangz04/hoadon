package vn.hoadon.services;

import vn.hoadon.entity.UserEntity;

public interface PermissionService {

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
