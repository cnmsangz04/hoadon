package vn.hoadon.dto;

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
    private Integer role; // 0 root, 1 admin, 2 user
    private String phone;
    private Byte status;
    private Byte adminType; // 0 none, 1 system, 2 company
    private WorkRole workRole;
    private List<UserPermissionOverride> userPermissions;

    public static class WorkRole {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

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
    public Byte getAdminType() { return adminType; }
    public void setAdminType(Byte adminType) { this.adminType = adminType; }
    public WorkRole getWorkRole() { return workRole; }
    public void setWorkRole(WorkRole workRole) { this.workRole = workRole; }
    public List<UserPermissionOverride> getUserPermissions() { return userPermissions; }
    public void setUserPermissions(List<UserPermissionOverride> userPermissions) { this.userPermissions = userPermissions; }
}
