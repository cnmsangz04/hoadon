package vn.hoadon.dto;

public class AuthResponse {
    private final String token;
    private final String tokenType;
    private final Long userId;
    private final Integer role;

    public AuthResponse(String token, String tokenType, Long userId, Integer role) {
        this.token = token;
        this.tokenType = tokenType;
        this.userId = userId;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getRole() {
        return role;
    }
}