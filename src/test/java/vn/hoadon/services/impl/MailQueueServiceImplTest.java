package vn.hoadon.services.impl;

import org.junit.jupiter.api.Test;
import vn.hoadon.messaging.MailJobMessage;

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
}
