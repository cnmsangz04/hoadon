package vn.hoadon.services.impl;

import org.junit.jupiter.api.Test;
import vn.hoadon.messaging.MailJobMessage;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MailQueueServiceImplTest {

    @Test
    void systemMailJobUsesCompanyOne() {
        MailJobMessage message = new MailJobMessage();
        message.setTemplateKey("BUY_INVOICE_MAIL");
        message.setCompanyId(42L);

        assertThat(MailQueueServiceImpl.resolveJobCompanyId(message)).isEqualTo(1L);
    }

    @Test
    void invoiceIssueMailKeepsMessageCompany() {
        MailJobMessage message = new MailJobMessage();
        message.setTemplateKey("ISSUE_INVOICE_MAIL");
        message.setCompanyId(42L);

        assertThat(MailQueueServiceImpl.resolveJobCompanyId(message)).isEqualTo(42L);
    }

    @Test
    void dailyInvoiceReportMailUsesCompanyOne() {
        MailJobMessage message = new MailJobMessage();
        message.setTemplateKey("DAILY_INVOICE_REPORT_MAIL");
        message.setCompanyId(42L);

        assertThat(MailQueueServiceImpl.resolveJobCompanyId(message)).isEqualTo(1L);
    }

    @Test
    void interpolatesDailyInvoiceReportSubjectBeforeSavingJob() {
        String subject = MailQueueServiceImpl.interpolateTemplate(
                "Báo cáo hóa đơn ngày [REPORT_DATE]",
                Map.of("REPORT_DATE", "17/06/2026")
        );

        assertThat(subject).isEqualTo("Báo cáo hóa đơn ngày 17/06/2026");
    }
}
