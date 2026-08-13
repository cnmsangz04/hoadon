package vn.hoadon.dto.auth;

public class AuthRequest {
    private String username; // Đăng nhập chỉ bằng username
    private String password; // User password
    private Boolean remember;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getRemember() { return remember; }
    public void setRemember(Boolean remember) { this.remember = remember; }
}
