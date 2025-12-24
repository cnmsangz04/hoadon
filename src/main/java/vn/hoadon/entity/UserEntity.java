package vn.hoadon.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false, length = 32)
    private String username;

    // Add display name for the user
    @Column(name = "name", length = 255)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 64)
    private String password;

    @Column(name = "admin_password", length = 64)
    private String adminPassword;

    @Column(length = 255)
    private String avatar;

    @Column(nullable = false)
    private Integer role;

    // Phone number for the user
    @Column(name = "phone", length = 32)
    private String phone;

    // 1 = active, 0 = inactive
    @Column(nullable = false)
    private Byte status;

    @Column(columnDefinition = "nvarchar(max)")
    private String info;

    @Column(name = "ip_login", columnDefinition = "nvarchar(max)")
    private String ipLogin;

    @Column(name = "forget_token", length = 32)
    private String forgetToken;

    @Column(name = "time_reset")
    private LocalDateTime timeReset;

    @Column(name = "remember_token", length = 100)
    private String rememberToken;

    @Column(name = "api_token", length = 100)
    private String apiToken;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Remove direct ManyToMany mapping to user_permissions and use explicit entity with allowed bit
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserPermissionEntity> userPermissionOverrides;

    // Working role (vai trò làm việc)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_role_id")
    private RoleEntity workRole;

    // Expose raw foreign key for serialization without forcing join
    @Column(name = "work_role_id", insertable = false, updatable = false)
    private Long workRoleId;

    @Column(name = "admin_type")
    private Byte adminType; // 0: none, 1: system admin, 2: company admin

    protected UserEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof UserEntity) {
            return (UserEntity) auth.getPrincipal();
        }
        return null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getIpLogin() {
        return ipLogin;
    }

    public void setIpLogin(String ipLogin) {
        this.ipLogin = ipLogin;
    }

    public String getForgetToken() {
        return forgetToken;
    }

    public void setForgetToken(String forgetToken) {
        this.forgetToken = forgetToken;
    }

    public LocalDateTime getTimeReset() {
        return timeReset;
    }

    public void setTimeReset(LocalDateTime timeReset) {
        this.timeReset = timeReset;
    }

    public String getRememberToken() {
        return rememberToken;
    }

    public void setRememberToken(String rememberToken) {
        this.rememberToken = rememberToken;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<UserPermissionEntity> getUserPermissionOverrides() {
        return userPermissionOverrides;
    }

    public void setUserPermissionOverrides(List<UserPermissionEntity> userPermissionOverrides) {
        this.userPermissionOverrides = userPermissionOverrides;
    }

    public RoleEntity getWorkRole() {
        return workRole;
    }

    public void setWorkRole(RoleEntity workRole) {
        this.workRole = workRole;
    }

    public Long getWorkRoleId() {
        return workRoleId;
    }

    @JsonProperty("work_role_id")
    public Long getWorkRoleIdSnake() {
        return workRoleId;
    }

    public Byte getAdminType() {
        return adminType;
    }

    public void setAdminType(Byte adminType) {
        this.adminType = adminType;
    }

    // Compute effective permissions: start with workRole permissions, then apply per-user overrides
    // allowed = 1 → ensure permission present; allowed = 0 → ensure permission removed
    public Set<PermissionEntity> getPermissions() {
        Set<PermissionEntity> effective = new HashSet<>();
        // Base from work role
        if (this.workRole != null && this.workRole.getPermissions() != null) {
            effective.addAll(this.workRole.getPermissions());
        }
        // Apply overrides
        if (this.userPermissionOverrides != null && !this.userPermissionOverrides.isEmpty()) {
            // Build a map of current permission IDs in the set for quick lookup/remove
            Set<Long> currentIds = new HashSet<>();
            for (PermissionEntity p : effective) {
                if (p != null && p.getId() != null) currentIds.add(p.getId());
            }
            for (UserPermissionEntity ov : this.userPermissionOverrides) {
                if (ov == null || ov.getPermission() == null || ov.getPermission().getId() == null) continue;
                Long pid = ov.getPermission().getId();
                byte allowed = ov.getAllowed() != null ? ov.getAllowed() : (byte)0;
                if (allowed == 1) {
                    // Add permission entity if not already present
                    if (!currentIds.contains(pid)) {
                        effective.add(ov.getPermission());
                        currentIds.add(pid);
                    }
                } else {
                    // Remove any permission in the set with same id
                    if (currentIds.contains(pid)) {
                        effective.removeIf(p -> p != null && pid.equals(p.getId()));
                        currentIds.remove(pid);
                    }
                }
            }
        }
        return effective;
    }
}