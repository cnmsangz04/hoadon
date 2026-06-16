package vn.hoadon.services;

import java.time.LocalDate;

public interface TelegramDailyInvoiceReportService {
    String buildReport(LocalDate reportDate);
    void sendReport(LocalDate reportDate);
    void sendYesterdayReport();
}
