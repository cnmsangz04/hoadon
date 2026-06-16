package vn.hoadon.services;

public interface TelegramNotificationService {
    void sendMessage(String text);
    boolean isConfigured();
}
