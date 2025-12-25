package vn.hoadon.controllers.base;

import vn.hoadon.entity.UserEntity;
import vn.hoadon.services.PermissionService;
import vn.hoadon.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;

public abstract class BaseController {

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected UserRepository userRepository;

    protected UserEntity currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity principal = auth != null && auth.getPrincipal() instanceof UserEntity
                ? (UserEntity) auth.getPrincipal()
                : null;
        if (principal == null || principal.getId() == null) return principal;
        
        // Re-fetch to attach to session and initialize lazy proxies safely
        return userRepository.findById(principal.getId()).orElse(principal);
    }

    protected void permission(String keys) {
        UserEntity user = currentUser();
        boolean allowed = permissionService.hasPermission(user, keys, false, true);
        if (!allowed) {
            throw new AccessDeniedException("Bạn không có quyền thao tác cho hành động này");
        }
    }

    // Optional helper to query without throwing
    protected boolean hasPermission(String keys) {
        return permissionService.hasPermission(currentUser(), keys, false, true);
    }
}