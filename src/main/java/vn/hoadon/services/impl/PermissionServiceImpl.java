package vn.hoadon.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import vn.hoadon.entity.PermissionEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.services.PermissionService;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionServiceImpl.class);

    @Override
    public boolean hasPermission(
            UserEntity user,
            String keys,
            boolean requireAll,
            boolean isBoolean
    ) {

        if (user == null || user.getPermissions() == null || keys == null || keys.isBlank()) {

            log.warn(
                "[PERMISSION] Invalid check | user={} | keys={} | requireAll={} | isBoolean={}",
                user != null ? user.getUsername() : "null",
                keys,
                requireAll,
                isBoolean
            );

            if (isBoolean) return false;
            throw new AccessDeniedException("Bạn không có quyền thao tác cho hành động này");
        }

        List<String> permissionKeys = Arrays.asList(keys.split("\\|"));

        Set<String> userPermissions = user.getPermissions()
                .stream()
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());

        log.debug(
            "[PERMISSION] Checking | user={} | requireAll={} | need={} | has={}",
            user.getUsername(),
            requireAll,
            permissionKeys,
            userPermissions
        );

        boolean isPermission;

        if (requireAll) {
            // Phải có TẤT CẢ quyền
            isPermission = permissionKeys.stream()
                    .allMatch(userPermissions::contains);
        } else {
            // Chỉ cần CÓ 1 quyền
            isPermission = permissionKeys.stream()
                    .anyMatch(userPermissions::contains);
        }

        if (isPermission) {
            log.info(
                "[PERMISSION] GRANTED | user={} | keys={} | requireAll={}",
                user.getUsername(),
                keys,
                requireAll
            );
            return true;
        }

        log.warn(
            "[PERMISSION] DENIED | user={} | keys={} | requireAll={}",
            user.getUsername(),
            keys,
            requireAll
        );

        if (!isBoolean) {
            throw new AccessDeniedException("Bạn không có quyền thao tác cho hành động này");
        }

        return false;
    }
}
