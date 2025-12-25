package vn.hoadon.dto.auth;

public class AuthRequest {
    private String username; // deprecated: kept for backward compatibility
    private String email;    // preferred for login
    private String password; // for regular login

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}