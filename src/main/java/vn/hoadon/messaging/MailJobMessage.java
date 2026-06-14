package vn.hoadon.messaging;

import java.io.Serializable;
import java.util.Map;

/**
 * Message payload stored in the SQL Server mail_jobs queue.
 * Jackson serializes this to JSON before saving it in the database.
 *
 * Variables in the template content use [KEY] or {{variable_name}} syntax.
 * Common keys: buyer_name, invoice_no, date_export, amount, code_cqt,
 *              form_serial, company_name, pdf_link, xml_link.
 */
public class MailJobMessage implements Serializable {

    /** Template key (e.g. ISSUE_INVOICE_MAIL) used to look up mail_templates. */
    private String templateKey;

    /** Company that owns the template (looks up company-specific template first,
     *  then falls back to system template). */
    private Long companyId;

    /** Related invoice id, used for mail send history. */
    private Long invoiceId;

    /** Destination email address. */
    private String toEmail;

    /** Recipient display name (used in salutation). */
    private String toName;

    /** Variables to substitute into [KEY] or {{key}} placeholders in the template. */
    private Map<String, String> variables;

    // ── Constructors ──────────────────────────────────────────────────────────

    public MailJobMessage() {}

    public MailJobMessage(String templateKey, Long companyId, String toEmail,
                          String toName, Map<String, String> variables) {
        this.templateKey = templateKey;
        this.companyId   = companyId;
        this.toEmail     = toEmail;
        this.toName      = toName;
        this.variables   = variables;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getTemplateKey()               { return templateKey; }
    public void setTemplateKey(String k)         { this.templateKey = k; }

    public Long getCompanyId()                   { return companyId; }
    public void setCompanyId(Long id)            { this.companyId = id; }

    public Long getInvoiceId()                   { return invoiceId; }
    public void setInvoiceId(Long id)            { this.invoiceId = id; }

    public String getToEmail()                   { return toEmail; }
    public void setToEmail(String e)             { this.toEmail = e; }

    public String getToName()                    { return toName; }
    public void setToName(String n)              { this.toName = n; }

    public Map<String, String> getVariables()    { return variables; }
    public void setVariables(Map<String, String> v) { this.variables = v; }
}
