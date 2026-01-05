package vn.hoadon.dto;

import java.time.LocalDateTime;

public class SignatureVatDTO {
    public Integer id;
    public Integer companyId;
    public Integer invoiceId;
    public String xml;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
