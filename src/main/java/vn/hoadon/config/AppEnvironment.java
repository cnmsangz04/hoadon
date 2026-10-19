package vn.hoadon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;

/** Nguồn nhận diện môi trường dùng chung cho toàn bộ ứng dụng. */
@Component
public class AppEnvironment {
    public enum Type { LOCAL, PRODUCTION }

    private final String backendUrl;
    private final Type type;

    public AppEnvironment(@Value("${app.backend-url:http://hoadon2er.local.vn}") String backendUrl) {
        this.backendUrl = backendUrl == null ? "" : backendUrl.trim();
        this.type = detect(this.backendUrl);
    }

    public Type getType() {
        return type;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public boolean isLocal() {
        return type == Type.LOCAL;
    }

    public boolean isProduction() {
        return type == Type.PRODUCTION;
    }

    static Type detect(String backendUrl) {
        if (backendUrl == null || backendUrl.isBlank()) return Type.LOCAL;
        try {
            String host = URI.create(backendUrl.trim()).getHost();
            if (host == null) return Type.PRODUCTION;
            host = host.toLowerCase(Locale.ROOT);
            if ("localhost".equals(host)
                    || "127.0.0.1".equals(host)
                    || "::1".equals(host)
                    || "hoadon2er.local.vn".equals(host)
                    || host.endsWith(".local")) {
                return Type.LOCAL;
            }
            return Type.PRODUCTION;
        } catch (IllegalArgumentException e) {
            return Type.PRODUCTION;
        }
    }
}
