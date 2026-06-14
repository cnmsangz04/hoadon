package vn.hoadon.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mail_servers")
public class MailServerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    /** SMTP host, e.g. smtp.gmail.com */
    @Column(name = "host", nullable = false, length = 255)
    private String host;

    /** SMTP port, e.g. 587 */
    @Column(name = "port", nullable = false)
    private Integer port;

    /** Login username / email address */
    @Column(name = "username", nullable = false, length = 255)
    private String username;

    /** AES-encrypted SMTP password */
    @Column(name = "password", nullable = false, length = 512)
    private String password;

    /** Display name shown in the From header */
    @Column(name = "from_name", columnDefinition = "NVARCHAR(255)")
    private String fromName;

    /** From email address (defaults to username if null) */
    @Column(name = "from_email", length = 255)
    private String fromEmail;

    /** 1 = STARTTLS, 2 = SSL/TLS, 0 = none */
    @Column(name = "encryption", nullable = false)
    private Short encryption;

    /** 1 = active */
    @Column(name = "status", nullable = false)
    private Short status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters & setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public String getFromEmail() { return fromEmail; }
    public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }

    public Short getEncryption() { return encryption; }
    public void setEncryption(Short encryption) { this.encryption = encryption; }

    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
