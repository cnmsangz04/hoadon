package vn.hoadon.ai.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class AiDataSanitizer {
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?iu)(password|passwd|mật\\s*khẩu|api[ _-]?key|access[ _-]?token|refresh[ _-]?token|secret)"
                    + "(\\s*[:=]\\s*)([^\\s,;]+)");
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]+");
    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b");
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?84|0)(?:[ .-]?\\d){9,10}(?!\\d)");
    private static final Pattern IDENTIFIER = Pattern.compile("(?<!\\d)\\d{10}(?:-?\\d{3})?(?!\\d)");
    private static final Pattern BANK_OR_CARD = Pattern.compile("(?<!\\d)(?:\\d[ -]?){14,19}(?!\\d)");

    public String sanitize(String value) {
        if (value == null || value.isBlank()) return value;

        String sanitized = SECRET_ASSIGNMENT.matcher(value).replaceAll("$1$2[REDACTED]");
        sanitized = BEARER_TOKEN.matcher(sanitized).replaceAll("Bearer [REDACTED]");
        sanitized = JWT.matcher(sanitized).replaceAll("[TOKEN]");
        sanitized = EMAIL.matcher(sanitized).replaceAll("[EMAIL]");
        sanitized = PHONE.matcher(sanitized).replaceAll("[PHONE]");
        sanitized = BANK_OR_CARD.matcher(sanitized).replaceAll("[ACCOUNT]");
        return IDENTIFIER.matcher(sanitized).replaceAll("[IDENTIFIER]");
    }
}
