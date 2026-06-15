package vn.hoadon.services;

import vn.hoadon.messaging.MailJobMessage;

/**
 * Đưa job gửi mail vào hàng đợi cơ sở dữ liệu SQL Server.
 * Tách phần tạo job ở controller hoặc tiến trình nền khỏi phần xử lý gửi mail.
 */
public interface MailQueueService {

    /**
     * Đưa job gửi mail vào hàng đợi và trả về ngay, việc gửi mail xử lý bất đồng bộ.
     *
     * @param message dữ liệu job gồm key mẫu, người nhận và biến thay thế
     */
    void enqueue(MailJobMessage message);
}
