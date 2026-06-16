package vn.hoadon.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.hoadon.entity.TelegramConfigEntity;
import vn.hoadon.repositories.TelegramConfigRepository;
import vn.hoadon.services.TelegramNotificationService;
import vn.hoadon.util.AesEncryptor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class TelegramNotificationServiceImpl implements TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationServiceImpl.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final TelegramConfigRepository telegramConfigRepository;
    private final AesEncryptor aesEncryptor;

    public TelegramNotificationServiceImpl(ObjectMapper objectMapper,
                                           TelegramConfigRepository telegramConfigRepository,
                                           AesEncryptor aesEncryptor) {
        this.objectMapper = objectMapper;
        this.telegramConfigRepository = telegramConfigRepository;
        this.aesEncryptor = aesEncryptor;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return telegramConfigRepository.findTopByOrderByIdDesc()
                .map(this::isUsable)
                .orElse(false);
    }

    @Override
    public void sendMessage(String text) {
        TelegramConfigEntity config = telegramConfigRepository.findTopByOrderByIdDesc().orElse(null);
        if (!isUsable(config)) {
            log.info("[Telegram] Notification skipped because telegram config is not enabled or incomplete");
            return;
        }
        try {
            String token = aesEncryptor.decrypt(config.getBotToken());
            String body = objectMapper.writeValueAsString(Map.of(
                    "chat_id", config.getChatId().trim(),
                    "text", text,
                    "parse_mode", "HTML",
                    "disable_web_page_preview", true
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + token.trim() + "/sendMessage"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (!json.path("ok").asBoolean(false)) {
                String description = json.path("description").asText("Unknown Telegram error");
                throw new IllegalStateException(description);
            }
        } catch (Exception ex) {
            log.warn("[Telegram] Failed to send notification: {}", ex.getMessage());
            throw new RuntimeException("Không thể gửi thông báo Telegram", ex);
        }
    }

    private boolean isUsable(TelegramConfigEntity config) {
        return config != null
                && Boolean.TRUE.equals(config.getEnabled())
                && config.getBotToken() != null && !config.getBotToken().isBlank()
                && config.getChatId() != null && !config.getChatId().isBlank();
    }
}
