<<<<<<<< HEAD:src/main/java/vn/hoadon/dto/response/AuthResponse.java
package vn.hoadon.dto.response;
========
package vn.hoadon.dto.auth;
>>>>>>>> 1eda31a04840376f9f6b426dfa25076c59d893d1:src/main/java/vn/hoadon/dto/auth/AuthResponse.java

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