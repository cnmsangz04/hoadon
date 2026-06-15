package vn.hoadon.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtil {

    private ClientIpUtil() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) return "";
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String value = request.getHeader(header);
            String ip = firstIp(value);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return normalize(ip);
            }
        }
        return normalize(request.getRemoteAddr());
    }

    public static String normalize(String ip) {
        if (ip == null) return "";
        String value = ip.trim();
        if (value.isEmpty()) return "";
        if (value.contains(",")) {
            value = firstIp(value);
        }
        if ("0:0:0:0:0:0:0:1".equals(value) || "::1".equals(value)) {
            return "127.0.0.1";
        }
        return value;
    }

    private static String firstIp(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return "";
        int comma = trimmed.indexOf(',');
        if (comma >= 0) {
            return trimmed.substring(0, comma).trim();
        }
        return trimmed;
    }
}
