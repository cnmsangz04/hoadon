package vn.hoadon.controllers.base;

import vn.hoadon.entity.UserEntity;
import vn.hoadon.entity.PermissionEntity;
import vn.hoadon.services.PermissionService;
import vn.hoadon.repositories.PermissionRepository;
import vn.hoadon.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseController {

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PermissionRepository permissionRepository;

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
            throw new AccessDeniedException(permissionDeniedMessage(keys));
        }
    }

    // Optional helper to query without throwing
    protected boolean hasPermission(String keys) {
        return permissionService.hasPermission(currentUser(), keys, false, true);
    }

    private String permissionDeniedMessage(String keys) {
        if (keys == null || keys.isBlank()) {
            return "Bạn không có quyền thao tác cho hành động này";
        }

        List<String> labels = Arrays.stream(keys.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .map(this::permissionLabel)
                .collect(Collectors.toList());

        if (labels.isEmpty()) {
            return "Bạn không có quyền thao tác cho hành động này";
        }

        if (labels.size() == 1) {
            return "Thiếu quyền: " + labels.get(0);
        }
        return "Thiếu một trong các quyền: " + String.join(", ", labels);
    }

    private String permissionLabel(String key) {
        try {
            return permissionRepository.findByName(key)
                    .map(permission -> {
                        String label = permissionDisplayName(permission);
                        return label != null && !label.isBlank() ? label : key;
                    })
                    .orElse(key);
        } catch (Exception ignored) {
            return key;
        }
    }

    private String permissionDisplayName(PermissionEntity permission) {
        if (permission == null) return "";
        String displayName = permission.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        String name = permission.getName();
        return name != null && !name.isBlank() ? name.trim() : "";
    }
}
