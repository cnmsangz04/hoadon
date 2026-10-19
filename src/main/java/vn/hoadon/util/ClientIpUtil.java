package vn.hoadon.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtil {

    private ClientIpUtil() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) return "";
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
