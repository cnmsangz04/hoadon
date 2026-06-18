package vn.hoadon.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.DailyInvoiceReportConfigEntity;
import vn.hoadon.messaging.MailJobMessage;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.DailyInvoiceReportConfigRepository;
import vn.hoadon.repositories.InvoiceRepository;
import vn.hoadon.services.MailQueueService;
import vn.hoadon.services.DailyInvoiceReportService;
import vn.hoadon.services.TelegramNotificationService;
import vn.hoadon.util.SystemMail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DailyInvoiceReportServiceImpl implements DailyInvoiceReportService {

    private static final Logger log = LoggerFactory.getLogger(DailyInvoiceReportServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MAIL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<Short> REPORT_STATUSES = List.of((short) 1, (short) 2, (short) 7);
    private static final String DAILY_REPORT_MAIL_TEMPLATE = "DAILY_INVOICE_REPORT_MAIL";

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final TelegramNotificationService telegramNotificationService;
    private final DailyInvoiceReportConfigRepository dailyInvoiceReportConfigRepository;
    private final MailQueueService mailQueueService;

    public DailyInvoiceReportServiceImpl(InvoiceRepository invoiceRepository,
                                         CompanyRepository companyRepository,
                                         TelegramNotificationService telegramNotificationService,
                                         DailyInvoiceReportConfigRepository dailyInvoiceReportConfigRepository,
                                         MailQueueService mailQueueService) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.telegramNotificationService = telegramNotificationService;
        this.dailyInvoiceReportConfigRepository = dailyInvoiceReportConfigRepository;
        this.mailQueueService = mailQueueService;
    }

    @Override
    public String buildReport(LocalDate reportDate) {
        return buildTelegramReport(loadReportData(reportDate));
    }

    private DailyInvoiceReportData loadReportData(LocalDate reportDate) {
        LocalDateTime from = reportDate.atStartOfDay();
        LocalDateTime to = reportDate.plusDays(1).atStartOfDay();
        List<InvoiceRepository.InvoiceStatusCount> counts =
                invoiceRepository.countStatusByUpdatedAtBetween(REPORT_STATUSES, from, to);

        Map<Integer, Map<Short, Long>> byCompany = new LinkedHashMap<>();
        if (counts != null) {
            for (InvoiceRepository.InvoiceStatusCount row : counts) {
                if (row.getCompanyId() == null || row.getStatus() == null) continue;
                byCompany.computeIfAbsent(row.getCompanyId(), key -> new LinkedHashMap<>())
                        .put(row.getStatus(), row.getTotal() != null ? row.getTotal() : 0L);
            }
        }

        if (byCompany.isEmpty()) {
            return new DailyInvoiceReportData(reportDate, byCompany, Map.of());
        }

        List<Long> companyIds = byCompany.keySet().stream()
                .map(Integer::longValue)
                .collect(Collectors.toList());
        Map<Long, CompanyEntity> companies = companyRepository.findAllById(companyIds)
                .stream()
                .collect(Collectors.toMap(CompanyEntity::getId, c -> c));
        return new DailyInvoiceReportData(reportDate, byCompany, companies);
    }

    private String buildTelegramReport(DailyInvoiceReportData data) {
        String title = "<b>Báo cáo hóa đơn ngày " + escape(data.reportDate().format(DATE_FORMAT)) + "</b>";
        if (data.byCompany().isEmpty()) {
            return title + "\nKhông có hóa đơn cần kiểm tra.";
        }

        List<String> lines = new ArrayList<>();
        lines.add(title);
        for (Map.Entry<Integer, Map<Short, Long>> entry : data.byCompany().entrySet()) {
            CompanyEntity company = data.companies().get(entry.getKey().longValue());
            String companyName = resolveCompanyName(company, entry.getKey());
            ReportNumbers numbers = resolveNumbers(entry.getValue());
            lines.add("");
            lines.add("<b>" + escape(companyName) + "</b>");
            lines.add("- Đã ký: " + numbers.signed());
            lines.add("- Đã gửi thuế: " + numbers.taxSent());
            lines.add("- Không đủ điều kiện cấp mã: " + numbers.notEligible());
            lines.add("- Tổng: " + numbers.total());
        }
        return String.join("\n", lines);
    }

    @Override
    public void sendReport(LocalDate reportDate) {
        DailyInvoiceReportData data = loadReportData(reportDate);
        try {
            telegramNotificationService.sendMessage(buildTelegramReport(data));
        } catch (Exception e) {
            log.warn("Không thể gửi báo cáo hóa đơn ngày qua Telegram: {}", e.getMessage());
        }
        sendReportMails(data);
    }

    @Override
    public void sendYesterdayReport() {
        sendReport(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1));
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Ho_Chi_Minh")
    public void scheduledDailyReport() {
        DailyInvoiceReportConfigEntity config = dailyInvoiceReportConfigRepository.findTopByOrderByIdDesc().orElse(null);
        if (!isDue(config)) return;
        LocalDate reportDate = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1);
        log.info("Bắt đầu gửi tự động báo cáo hóa đơn ngày {}", reportDate.format(DATE_FORMAT));
        sendReport(reportDate);
        config.setLastReportDate(reportDate);
        config.setLastSentAt(LocalDateTime.now());
        dailyInvoiceReportConfigRepository.save(config);
        log.info("Đã gửi tự động báo cáo hóa đơn ngày {}", reportDate.format(DATE_FORMAT));
    }

    private boolean isDue(DailyInvoiceReportConfigEntity config) {
        if (config == null || !Boolean.TRUE.equals(config.getReportEnabled())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        int hour = config.getDailyHour() != null ? config.getDailyHour() : 1;
        int minute = config.getDailyMinute() != null ? config.getDailyMinute() : 0;
        LocalDateTime scheduledAt = now.toLocalDate().atTime(hour, minute);
        if (now.isBefore(scheduledAt)) {
            return false;
        }
        LocalDate reportDate = now.toLocalDate().minusDays(1);
        return config.getLastReportDate() == null || !config.getLastReportDate().equals(reportDate);
    }

    private void sendReportMails(DailyInvoiceReportData data) {
        if (data.byCompany().isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, Map<Short, Long>> entry : data.byCompany().entrySet()) {
            Long companyId = entry.getKey().longValue();
            if (companyId == SystemMail.COMPANY_ID) {
                continue;
            }

            CompanyEntity company = data.companies().get(companyId);
            if (company == null) {
                continue;
            }

            String toEmail = cleanEmail(company.getEmail());
            if (toEmail == null) {
                continue;
            }

            String companyName = resolveCompanyName(company, entry.getKey());
            ReportNumbers numbers = resolveNumbers(entry.getValue());
            Map<String, String> vars = new HashMap<>();
            vars.put("SUBJECT", "Báo cáo hóa đơn ngày " + data.reportDate().format(MAIL_DATE_FORMAT));
            vars.put("REPORT_DATE", data.reportDate().format(MAIL_DATE_FORMAT));
            vars.put("COMPANY_NAME", companyName);
            vars.put("SIGNED_COUNT", String.valueOf(numbers.signed()));
            vars.put("TAX_SENT_COUNT", String.valueOf(numbers.taxSent()));
            vars.put("NOT_ELIGIBLE_COUNT", String.valueOf(numbers.notEligible()));
            vars.put("TOTAL_COUNT", String.valueOf(numbers.total()));

            MailJobMessage message = new MailJobMessage();
            message.setTemplateKey(DAILY_REPORT_MAIL_TEMPLATE);
            message.setCompanyId(company.getId());
            message.setToEmail(toEmail);
            message.setToName(companyName);
            message.setVariables(vars);

            try {
                mailQueueService.enqueue(message);
            } catch (Exception e) {
                log.warn("Không thể đưa mail báo cáo hóa đơn ngày vào hàng đợi cho công ty {}: {}",
                        companyId, e.getMessage());
            }
        }
    }

    private ReportNumbers resolveNumbers(Map<Short, Long> statusCounts) {
        long signed = statusCounts != null ? statusCounts.getOrDefault((short) 1, 0L) : 0L;
        long taxSent = statusCounts != null ? statusCounts.getOrDefault((short) 2, 0L) : 0L;
        long notEligible = statusCounts != null ? statusCounts.getOrDefault((short) 7, 0L) : 0L;
        return new ReportNumbers(signed, taxSent, notEligible);
    }

    private String resolveCompanyName(CompanyEntity company, Integer fallbackId) {
        if (company != null && company.getName() != null && !company.getName().isBlank()) {
            return company.getName();
        }
        return "Công ty #" + fallbackId;
    }

    private String cleanEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private record DailyInvoiceReportData(LocalDate reportDate,
                                          Map<Integer, Map<Short, Long>> byCompany,
                                          Map<Long, CompanyEntity> companies) {
    }

    private record ReportNumbers(long signed, long taxSent, long notEligible) {
        long total() {
            return signed + taxSent + notEligible;
        }
    }
}
