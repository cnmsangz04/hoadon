package vn.hoadon.controller.base;

import vn.hoadon.entity.UserEntity;
import vn.hoadon.services.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;

public abstract class BaseController {

    @Autowired
    protected PermissionService permissionService;

    protected UserEntity currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UserEntity
                ? (UserEntity) auth.getPrincipal()
                : null;
    }

    protected void permission(String keys) {
        UserEntity user = currentUser();
        permissionService.hasPermission(user, keys, false, false);
    }
}
