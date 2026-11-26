package vn.hoadon.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

import vn.hoadon.ai.service.AiServiceException;
import vn.hoadon.ai.document.service.DocumentRecognitionException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> body = new HashMap<>();
        String message = ex.getMessage();
        body.put("message", message != null && !message.isBlank() && !"Access Denied".equalsIgnoreCase(message.trim())
                ? message
                : "Bạn không có quyền thao tác cho hành động này");
        body.put("error", "forbidden");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        body.put("error", "bad_request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(NoResourceFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Không tìm thấy tài nguyên");
        body.put("error", "not_found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAiService(AiServiceException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case "RATE_LIMIT_MINUTE", "RATE_LIMIT_TOKENS", "RATE_LIMIT_DAY", "RATE_LIMIT_COMPANY",
                 "AI_QUOTA_EXHAUSTED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "AI_INPUT_TOO_LARGE", "AI_INVALID_REQUEST", "INVALID_CONTEXT" -> HttpStatus.BAD_REQUEST;
            case "AI_SCOPE_DENIED" -> HttpStatus.FORBIDDEN;
            case "AI_CONTENT_BLOCKED" -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        body.put("error", ex.getCode().toLowerCase(java.util.Locale.ROOT));
        HttpHeaders headers = new HttpHeaders();
        if (status == HttpStatus.TOO_MANY_REQUESTS) headers.set("Retry-After", "60");
        return new ResponseEntity<>(body, headers, status);
    }

    @ExceptionHandler(DocumentRecognitionException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentRecognition(DocumentRecognitionException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        body.put("error", ex.getCode());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled application error", ex);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Lỗi hệ thống");
        body.put("error", "internal_error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
