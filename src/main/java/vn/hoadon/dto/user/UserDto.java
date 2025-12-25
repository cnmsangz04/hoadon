package vn.hoadon.dto.user;

public class UserDto {
    private Long id;
    private String username;
    private String name; // display name
    private String email;
    private String phone;
    private String avatar;
    private Integer role;
    private String emailcc;
    private String emailbcc;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
    public String getEmailcc() { return emailcc; }
    public void setEmailcc(String emailcc) { this.emailcc = emailcc; }
    public String getEmailbcc() { return emailbcc; }
    public void setEmailbcc(String emailbcc) { this.emailbcc = emailbcc; }
}