package vn.hoadon.dto.mail;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MailTemplateDto {
    private Integer id;
    private Long companyId;
    private String companyName;
    private String key;
    private String title;
    private String content;
    private Byte status;
    private Byte system;
    private LocalDateTime updatedAt;
}
