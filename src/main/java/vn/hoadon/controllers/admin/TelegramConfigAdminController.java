package vn.hoadon.controllers.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.TelegramConfigEntity;
import vn.hoadon.repositories.TelegramConfigRepository;
import vn.hoadon.services.TelegramNotificationService;
import vn.hoadon.util.AesEncryptor;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/administrator/telegram-config")
public class TelegramConfigAdminController extends BaseController {

    private static final String PERMISSION_KEY = "telegram-config-manage";

    private final TelegramConfigRepository telegramConfigRepository;
    private final TelegramNotificationService telegramNotificationService;
    private final AesEncryptor aesEncryptor;

    public TelegramConfigAdminController(TelegramConfigRepository telegramConfigRepository,
                                         TelegramNotificationService telegramNotificationService,
                                         AesEncryptor aesEncryptor) {
        this.telegramConfigRepository = telegramConfigRepository;
        this.telegramNotificationService = telegramNotificationService;
        this.aesEncryptor = aesEncryptor;
    }

    @GetMapping
    public ResponseEntity<?> get() {
        permission(PERMISSION_KEY);
        TelegramConfigEntity config = telegramConfigRepository.findTopByOrderByIdDesc()
                .orElseGet(TelegramConfigEntity::new);
        return ResponseEntity.ok(toResponse(config));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody TelegramConfigRequest request) {
        permission(PERMISSION_KEY);
        TelegramConfigEntity config = telegramConfigRepository.findTopByOrderByIdDesc()
                .orElseGet(TelegramConfigEntity::new);

        config.setEnabled(Boolean.TRUE.equals(request.enabled));
        config.setChatId(trimToNull(request.chatId));
        config.setDailyHour(clamp(request.dailyHour, 0, 23, 1));
        config.setDailyMinute(clamp(request.dailyMinute, 0, 59, 0));

        String token = trimToNull(request.botToken);
        if (token != null && !isMaskedToken(token)) {
            config.setBotToken(aesEncryptor.encrypt(token));
        }

        if (Boolean.TRUE.equals(config.getEnabled())) {
            if (config.getBotToken() == null || config.getBotToken().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập Bot Token"));
            }
            if (config.getChatId() == null || config.getChatId().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập Group Chat ID"));
            }
        }

        TelegramConfigEntity saved = telegramConfigRepository.save(config);
        return ResponseEntity.ok(toResponse(saved));
    }

    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestBody(required = false) Map<String, Object> body) {
        permission(PERMISSION_KEY);
        if (!telegramNotificationService.isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Chưa bật cấu hình Telegram hoặc thiếu Bot Token/Group Chat ID"
            ));
        }
        String message = body != null && body.get("message") != null
                ? String.valueOf(body.get("message"))
                : "Tin nhắn kiểm tra từ hệ thống hóa đơn điện tử.";
        telegramNotificationService.sendMessage(message);
        return ResponseEntity.ok(Map.of("message", "Đã gửi tin nhắn kiểm tra Telegram"));
    }

    private Map<String, Object> toResponse(TelegramConfigEntity config) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", config.getId());
        row.put("enabled", Boolean.TRUE.equals(config.getEnabled()));
        row.put("botTokenConfigured", config.getBotToken() != null && !config.getBotToken().isBlank());
        row.put("chatId", config.getChatId());
        row.put("dailyHour", config.getDailyHour() != null ? config.getDailyHour() : 1);
        row.put("dailyMinute", config.getDailyMinute() != null ? config.getDailyMinute() : 0);
        row.put("lastReportDate", config.getLastReportDate());
        row.put("lastSentAt", config.getLastSentAt());
        row.put("createdAt", config.getCreatedAt());
        row.put("updatedAt", config.getUpdatedAt());
        return row;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private boolean isMaskedToken(String value) {
        return value != null && value.contains("•");
    }

    private int clamp(Integer value, int min, int max, int fallback) {
        if (value == null) return fallback;
        return Math.max(min, Math.min(max, value));
    }

    public static class TelegramConfigRequest {
        public Boolean enabled;
        public String botToken;
        public String chatId;
        public Integer dailyHour;
        public Integer dailyMinute;
    }
}
