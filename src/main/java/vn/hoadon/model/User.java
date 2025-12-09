package vn.hoadon.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(nullable = false, length = 32)
    private String username;

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

    // status: 1 = active, 0 = inactive
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

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
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
    public Byte getStatus() { return status; }
    public void setStatus(Byte status) { this.status = status; }
    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }
    public String getIpLogin() { return ipLogin; }
    public void setIpLogin(String ipLogin) { this.ipLogin = ipLogin; }
    public String getForgetToken() { return forgetToken; }
    public void setForgetToken(String forgetToken) { this.forgetToken = forgetToken; }
    public LocalDateTime getTimeReset() { return timeReset; }
    public void setTimeReset(LocalDateTime timeReset) { this.timeReset = timeReset; }
    public String getRememberToken() { return rememberToken; }
    public void setRememberToken(String rememberToken) { this.rememberToken = rememberToken; }
    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}