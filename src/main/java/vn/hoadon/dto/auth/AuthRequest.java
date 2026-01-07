package vn.hoadon.dto.auth;

public class AuthRequest {
    private String username; // Login with username only
    private String password; // User password

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}