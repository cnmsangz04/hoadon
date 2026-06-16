package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.InvoiceRepository;
import vn.hoadon.services.TelegramDailyInvoiceReportService;
import vn.hoadon.services.TelegramNotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TelegramDailyInvoiceReportServiceImpl implements TelegramDailyInvoiceReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<Short> REPORT_STATUSES = List.of((short) 1, (short) 2, (short) 7);

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final TelegramNotificationService telegramNotificationService;

    @Value("${telegram.time-zone:Asia/Ho_Chi_Minh}")
    private String timeZone;

    public TelegramDailyInvoiceReportServiceImpl(InvoiceRepository invoiceRepository,
                                                 CompanyRepository companyRepository,
                                                 TelegramNotificationService telegramNotificationService) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.telegramNotificationService = telegramNotificationService;
    }

    @Override
    public String buildReport(LocalDate reportDate) {
        LocalDateTime from = reportDate.atStartOfDay();
        LocalDateTime to = reportDate.plusDays(1).atStartOfDay();
        List<InvoiceRepository.InvoiceStatusCount> counts =
                invoiceRepository.countStatusByUpdatedAtBetween(REPORT_STATUSES, from, to);

        String title = "<b>Báo cáo hóa đơn ngày " + escape(reportDate.format(DATE_FORMAT)) + "</b>";
        if (counts == null || counts.isEmpty()) {
            return title + "\nKhông phát sinh hóa đơn đã ký, đã gửi thuế hoặc không đủ điều kiện cấp mã.";
        }

        Map<Integer, Map<Short, Long>> byCompany = new LinkedHashMap<>();
        for (InvoiceRepository.InvoiceStatusCount row : counts) {
            if (row.getCompanyId() == null || row.getStatus() == null) continue;
            byCompany.computeIfAbsent(row.getCompanyId(), key -> new LinkedHashMap<>())
                    .put(row.getStatus(), row.getTotal() != null ? row.getTotal() : 0L);
        }

        List<Long> companyIds = byCompany.keySet().stream()
                .map(Integer::longValue)
                .collect(Collectors.toList());
        Map<Long, CompanyEntity> companies = companyRepository.findAllById(companyIds)
                .stream()
                .collect(Collectors.toMap(CompanyEntity::getId, c -> c));

        List<String> lines = new ArrayList<>();
        lines.add(title);
        for (Map.Entry<Integer, Map<Short, Long>> entry : byCompany.entrySet()) {
            CompanyEntity company = companies.get(entry.getKey().longValue());
            String companyName = company != null && company.getName() != null && !company.getName().isBlank()
                    ? company.getName()
                    : "Công ty #" + entry.getKey();
            Map<Short, Long> statusCounts = entry.getValue();
            long signed = statusCounts.getOrDefault((short) 1, 0L);
            long taxSent = statusCounts.getOrDefault((short) 2, 0L);
            long notEligible = statusCounts.getOrDefault((short) 7, 0L);
            lines.add("");
            lines.add("<b>" + escape(companyName) + "</b>");
            lines.add("- Đã ký: " + signed);
            lines.add("- Đã gửi thuế: " + taxSent);
            lines.add("- Không đủ điều kiện cấp mã: " + notEligible);
            lines.add("- Tổng: " + (signed + taxSent + notEligible));
        }
        return String.join("\n", lines);
    }

    @Override
    public void sendReport(LocalDate reportDate) {
        telegramNotificationService.sendMessage(buildReport(reportDate));
    }

    @Override
    public void sendYesterdayReport() {
        ZoneId zoneId = ZoneId.of(timeZone == null || timeZone.isBlank() ? "Asia/Ho_Chi_Minh" : timeZone);
        sendReport(LocalDate.now(zoneId).minusDays(1));
    }

    @Scheduled(cron = "${telegram.daily-cron:0 0 1 * * *}", zone = "${telegram.time-zone:Asia/Ho_Chi_Minh}")
    public void scheduledDailyReport() {
        if (telegramNotificationService.isConfigured()) {
            sendYesterdayReport();
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
