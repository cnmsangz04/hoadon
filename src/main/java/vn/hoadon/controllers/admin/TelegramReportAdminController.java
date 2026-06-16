package vn.hoadon.controllers.admin;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.services.TelegramDailyInvoiceReportService;
import vn.hoadon.services.TelegramNotificationService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/v1/administrator/telegram-report")
public class TelegramReportAdminController {

    private final TelegramDailyInvoiceReportService reportService;
    private final TelegramNotificationService telegramNotificationService;

    public TelegramReportAdminController(TelegramDailyInvoiceReportService reportService,
                                         TelegramNotificationService telegramNotificationService) {
        this.reportService = reportService;
        this.telegramNotificationService = telegramNotificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestParam(name = "date", required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (!telegramNotificationService.isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Chưa bật TELEGRAM_ENABLED hoặc thiếu TELEGRAM_BOT_TOKEN/TELEGRAM_CHAT_ID"
            ));
        }
        LocalDate reportDate = date != null ? date : LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1);
        reportService.sendReport(reportDate);
        return ResponseEntity.ok(Map.of(
                "message", "Đã gửi báo cáo Telegram",
                "date", reportDate.toString()
        ));
    }
}
