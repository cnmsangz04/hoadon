package vn.hoadon.security;

public final class UserRoles {
    public static final int ROOT = 0;
    public static final int SYSTEM_ADMIN = 1;
    public static final int COMPANY_MANAGER = 2;
    public static final int COMPANY_STAFF = 3;

    private UserRoles() {
    }

    public static boolean isRoot(Integer role) {
        return Integer.valueOf(ROOT).equals(role);
    }

    public static boolean isSystemAdmin(Integer role) {
        return Integer.valueOf(SYSTEM_ADMIN).equals(role);
    }

    public static boolean isCompanyManager(Integer role) {
        return Integer.valueOf(COMPANY_MANAGER).equals(role);
    }

    public static boolean isCompanyStaff(Integer role) {
        return Integer.valueOf(COMPANY_STAFF).equals(role);
    }

    public static boolean canAccessAdminArea(Integer role) {
        return isRoot(role) || isSystemAdmin(role);
    }

    public static boolean hasCompanyManagerPrivileges(Integer role) {
        return isRoot(role) || isSystemAdmin(role) || isCompanyManager(role);
    }
}
