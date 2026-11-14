package vn.hoadon.invoice.template.service;

import org.springframework.http.HttpStatus;

public class TemplateException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public TemplateException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }

    public static TemplateException invalid(String code, String message) {
        return new TemplateException(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static TemplateException missing() {
        return new TemplateException("TEMPLATE_NOT_FOUND", "Không tìm thấy mẫu hoặc không có quyền truy cập", HttpStatus.NOT_FOUND);
    }

    public static TemplateException conflict(String message) {
        return new TemplateException("DRAFT_CONFLICT", message, HttpStatus.CONFLICT);
    }
}
