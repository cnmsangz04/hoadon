package vn.hoadon.dto.member;

import java.util.List;

public class MemberUpsertRequest {
    private Long id;
    private Long companyId;
    private String username;
    private String name;
    private String email;
    private String password;
    private String adminPassword;
    private String avatar;
    private Integer role; // 0 toàn quyền, 1 quản trị hệ thống, 2 quản lý DN, 3 nhân viên DN
    private String phone;
    private Byte status;
    private List<UserPermissionOverride> userPermissions;

    public static class UserPermissionOverride {
        private Long permissionId;
        private Integer allowed; // 1 allow, 0 deny
        public Long getPermissionId() { return permissionId; }
        public void setPermissionId(Long permissionId) { this.permissionId = permissionId; }
        public Integer getAllowed() { return allowed; }
        public void setAllowed(Integer allowed) { this.allowed = allowed; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Byte getStatus() { return status; }
    public void setStatus(Byte status) { this.status = status; }
    public List<UserPermissionOverride> getUserPermissions() { return userPermissions; }
    public void setUserPermissions(List<UserPermissionOverride> userPermissions) { this.userPermissions = userPermissions; }
}
