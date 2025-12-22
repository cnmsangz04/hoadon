package vn.hoadon.services.impl;

import org.junit.jupiter.api.Test;
import vn.hoadon.entity.UserEntity;

import static org.junit.jupiter.api.Assertions.*;

class PermissionServiceImplTest {

    @Test
    void superAdminHasAllPermissions() {
        PermissionServiceImpl svc = new PermissionServiceImpl();
        UserEntity u = new UserEntity();
        u.setRole(0); // super admin
        boolean ok1 = svc.hasPermission(u, "any.permission", true, true);
        boolean ok2 = svc.hasPermission(u, "a|b|c", false, true);
        assertTrue(ok1);
        assertTrue(ok2);
    }
}
