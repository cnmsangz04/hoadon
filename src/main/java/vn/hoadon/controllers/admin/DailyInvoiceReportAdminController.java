package vn.hoadon.controllers.admin;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.DailyInvoiceReportConfigEntity;
import vn.hoadon.repositories.DailyInvoiceReportConfigRepository;
import vn.hoadon.services.DailyInvoiceReportService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/administrator/daily-invoice-report")
public class DailyInvoiceReportAdminController extends BaseController {

    private static final String PERMISSION_KEY = "telegram-config-manage";

    private final DailyInvoiceReportService reportService;
    private final DailyInvoiceReportConfigRepository dailyInvoiceReportConfigRepository;

    public DailyInvoiceReportAdminController(DailyInvoiceReportService reportService,
                                             DailyInvoiceReportConfigRepository dailyInvoiceReportConfigRepository) {
        this.reportService = reportService;
        this.dailyInvoiceReportConfigRepository = dailyInvoiceReportConfigRepository;
    }

    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        permission(PERMISSION_KEY);
        DailyInvoiceReportConfigEntity config = dailyInvoiceReportConfigRepository.findTopByOrderByIdDesc()
                .orElseGet(DailyInvoiceReportConfigEntity::new);
        return ResponseEntity.ok(toResponse(config));
    }

    @PostMapping("/config")
    public ResponseEntity<?> saveConfig(@RequestBody DailyInvoiceReportConfigRequest request) {
        permission(PERMISSION_KEY);
        DailyInvoiceReportConfigEntity config = dailyInvoiceReportConfigRepository.findTopByOrderByIdDesc()
                .orElseGet(DailyInvoiceReportConfigEntity::new);
        boolean oldEnabled = Boolean.TRUE.equals(config.getReportEnabled());
        int oldHour = config.getDailyHour() != null ? config.getDailyHour() : 1;
        int oldMinute = config.getDailyMinute() != null ? config.getDailyMinute() : 0;

        boolean newEnabled = Boolean.TRUE.equals(request.reportEnabled);
        int newHour = clamp(request.dailyHour, 0, 23, 1);
        int newMinute = clamp(request.dailyMinute, 0, 59, 0);

        config.setReportEnabled(newEnabled);
        config.setDailyHour(newHour);
        config.setDailyMinute(newMinute);
        if (oldEnabled != newEnabled || oldHour != newHour || oldMinute != newMinute) {
            config.setLastReportDate(null);
            config.setLastSentAt(null);
        }
        DailyInvoiceReportConfigEntity saved = dailyInvoiceReportConfigRepository.save(config);
        return ResponseEntity.ok(toResponse(saved));
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestParam(name = "date", required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        permission(PERMISSION_KEY);
        LocalDate reportDate = date != null ? date : LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1);
        reportService.sendReport(reportDate);
        return ResponseEntity.ok(Map.of(
                "message", "Đã gửi báo cáo hóa đơn ngày",
                "date", reportDate.toString()
        ));
    }

    private Map<String, Object> toResponse(DailyInvoiceReportConfigEntity config) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", config.getId());
        row.put("reportEnabled", Boolean.TRUE.equals(config.getReportEnabled()));
        row.put("dailyHour", config.getDailyHour() != null ? config.getDailyHour() : 1);
        row.put("dailyMinute", config.getDailyMinute() != null ? config.getDailyMinute() : 0);
        row.put("lastReportDate", config.getLastReportDate());
        row.put("lastSentAt", config.getLastSentAt());
        return row;
    }

    private int clamp(Integer value, int min, int max, int fallback) {
        if (value == null) return fallback;
        return Math.max(min, Math.min(max, value));
    }

    public static class DailyInvoiceReportConfigRequest {
        public Boolean reportEnabled;
        public Integer dailyHour;
        public Integer dailyMinute;
    }
}
