package vn.hoadon.services.impl;

import org.junit.jupiter.api.Test;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.messaging.MailJobMessage;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.DailyInvoiceReportConfigRepository;
import vn.hoadon.repositories.InvoiceRepository;
import vn.hoadon.services.MailQueueService;
import vn.hoadon.services.TelegramNotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyInvoiceReportServiceImplTest {

    @Test
    void sendReportMailUsesMatchingCompanyRecipientAndContent() {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        TelegramNotificationService telegramService = mock(TelegramNotificationService.class);
        DailyInvoiceReportConfigRepository configRepository = mock(DailyInvoiceReportConfigRepository.class);
        CapturingMailQueueService mailQueueService = new CapturingMailQueueService();

        LocalDate reportDate = LocalDate.of(2026, 6, 17);
        when(invoiceRepository.countStatusByUpdatedAtBetween(
                eq(List.of((short) 1, (short) 2, (short) 7)),
                eq(LocalDateTime.of(2026, 6, 17, 0, 0)),
                eq(LocalDateTime.of(2026, 6, 18, 0, 0))
        )).thenReturn(List.of(
                statusCount(21, (short) 1, 2L),
                statusCount(21, (short) 7, 1L),
                statusCount(22, (short) 2, 5L)
        ));
        when(companyRepository.findAllById(any())).thenReturn(List.of(
                company(21L, "Công ty A", "a@example.com"),
                company(22L, "Công ty B", "b@example.com")
        ));

        DailyInvoiceReportServiceImpl service = new DailyInvoiceReportServiceImpl(
                invoiceRepository,
                companyRepository,
                telegramService,
                configRepository,
                mailQueueService
        );

        service.sendReport(reportDate);

        assertThat(mailQueueService.messages).hasSize(2);

        MailJobMessage companyA = mailQueueService.messages.get(0);
        assertThat(companyA.getCompanyId()).isEqualTo(21L);
        assertThat(companyA.getToEmail()).isEqualTo("a@example.com");
        assertThat(companyA.getToName()).isEqualTo("Công ty A");
        assertThat(companyA.getVariables()).containsEntry("COMPANY_NAME", "Công ty A");
        assertThat(companyA.getVariables()).containsEntry("SIGNED_COUNT", "2");
        assertThat(companyA.getVariables()).containsEntry("TAX_SENT_COUNT", "0");
        assertThat(companyA.getVariables()).containsEntry("NOT_ELIGIBLE_COUNT", "1");
        assertThat(companyA.getVariables()).containsEntry("TOTAL_COUNT", "3");

        MailJobMessage companyB = mailQueueService.messages.get(1);
        assertThat(companyB.getCompanyId()).isEqualTo(22L);
        assertThat(companyB.getToEmail()).isEqualTo("b@example.com");
        assertThat(companyB.getToName()).isEqualTo("Công ty B");
        assertThat(companyB.getVariables()).containsEntry("COMPANY_NAME", "Công ty B");
        assertThat(companyB.getVariables()).containsEntry("SIGNED_COUNT", "0");
        assertThat(companyB.getVariables()).containsEntry("TAX_SENT_COUNT", "5");
        assertThat(companyB.getVariables()).containsEntry("NOT_ELIGIBLE_COUNT", "0");
        assertThat(companyB.getVariables()).containsEntry("TOTAL_COUNT", "5");

        verify(telegramService).sendMessage(any());
    }

    @Test
    void sendReportDoesNotQueueMailWhenReportHasNoData() {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        TelegramNotificationService telegramService = mock(TelegramNotificationService.class);
        DailyInvoiceReportConfigRepository configRepository = mock(DailyInvoiceReportConfigRepository.class);
        CapturingMailQueueService mailQueueService = new CapturingMailQueueService();

        when(invoiceRepository.countStatusByUpdatedAtBetween(any(), any(), any())).thenReturn(List.of());

        DailyInvoiceReportServiceImpl service = new DailyInvoiceReportServiceImpl(
                invoiceRepository,
                companyRepository,
                telegramService,
                configRepository,
                mailQueueService
        );

        service.sendReport(LocalDate.of(2026, 6, 17));

        assertThat(mailQueueService.messages).isEmpty();
        verify(telegramService).sendMessage("<b>Báo cáo hóa đơn ngày 17/06/2026</b>\nKhông có hóa đơn cần kiểm tra.");
    }

    private static InvoiceRepository.InvoiceStatusCount statusCount(Integer companyId, Short status, Long total) {
        return new InvoiceRepository.InvoiceStatusCount() {
            @Override
            public Integer getCompanyId() {
                return companyId;
            }

            @Override
            public Short getStatus() {
                return status;
            }

            @Override
            public Long getTotal() {
                return total;
            }
        };
    }

    private static CompanyEntity company(Long id, String name, String email) {
        CompanyEntity company = new CompanyEntity();
        company.setId(id);
        company.setName(name);
        company.setEmail(email);
        return company;
    }

    private static class CapturingMailQueueService implements MailQueueService {
        private final List<MailJobMessage> messages = new ArrayList<>();

        @Override
        public void enqueue(MailJobMessage message) {
            messages.add(message);
        }
    }
}
