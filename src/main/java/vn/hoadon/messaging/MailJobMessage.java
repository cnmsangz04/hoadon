package vn.hoadon.messaging;

import java.io.Serializable;
import java.util.Map;

/**
 * Dữ liệu message được lưu trong hàng đợi mail_jobs của SQL Server.
 * Jackson chuyển object này thành JSON trước khi lưu vào cơ sở dữ liệu.
 *
 * Biến trong nội dung mẫu dùng cú pháp [KEY] hoặc {{variable_name}}.
 * Các key thường dùng: buyer_name, invoice_no, date_export, amount, code_cqt,
 *                      form_serial, company_name, pdf_link, xml_link.
 */
public class MailJobMessage implements Serializable {

    /** Key template, ví dụ ISSUE_INVOICE_MAIL, dùng để tra trong mail_templates. */
    private String templateKey;

    /** Công ty sở hữu template, ưu tiên template riêng trước rồi đến template hệ thống. */
    private Long companyId;

    /** ID hóa đơn liên quan, dùng cho lịch sử gửi mail. */
    private Long invoiceId;

    /** Địa chỉ email nhận. */
    private String toEmail;

    /** Tên hiển thị của người nhận, dùng trong lời chào. */
    private String toName;

    /** Các biến thay vào placeholder dạng [KEY] hoặc {{key}} trong template. */
    private Map<String, String> variables;

    // Hàm khởi tạo
    public MailJobMessage() {}

    public MailJobMessage(String templateKey, Long companyId, String toEmail,
                          String toName, Map<String, String> variables) {
        this.templateKey = templateKey;
        this.companyId = companyId;
        this.toEmail = toEmail;
        this.toName = toName;
        this.variables = variables;
    }

    // Getter / setter
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String k) { this.templateKey = k; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long id) { this.companyId = id; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long id) { this.invoiceId = id; }

    public String getToEmail() { return toEmail; }
    public void setToEmail(String e) { this.toEmail = e; }

    public String getToName() { return toName; }
    public void setToName(String n) { this.toName = n; }

    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> v) { this.variables = v; }
}
