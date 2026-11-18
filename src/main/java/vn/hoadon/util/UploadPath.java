package vn.hoadon.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class UploadPath {
    private UploadPath() {}

    public static final String BASE_DIR = "uploads";
    public static final String UPLOAD_DIR_ENV = "APP_UPLOAD_DIR";
    public static final String UPLOAD_DIR_PROPERTY = "app.upload-dir";

    public static Path basePath() {
        String configured = System.getProperty(UPLOAD_DIR_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(UPLOAD_DIR_ENV);
        }
        if (configured == null || configured.isBlank()) {
            configured = BASE_DIR;
        }
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    public static String encodeCompanyId(Long companyId) {
        if (companyId == null || companyId < 0) {
            throw new IllegalArgumentException("companyId không hợp lệ");
        }
        return UUID.nameUUIDFromBytes(("hoadon-company:" + companyId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static Path baseDir() throws IOException {
        Path base = basePath();
        Files.createDirectories(base);
        return base;
    }

    public static Path resolveCompanyTypeDir(Long companyId, String type) throws IOException {
        Path dir = basePath().resolve(Paths.get(encodeCompanyId(companyId), type)).normalize();
        if (!dir.startsWith(basePath())) {
            throw new IOException("Invalid upload path");
        }
        Files.createDirectories(dir);
        return dir;
    }

    public static String publicUrl(Long companyId, String type, String fileName) {
        return "/" + BASE_DIR + "/" + encodeCompanyId(companyId) + "/" + type + "/" + fileName;
    }

    public static boolean deletePublicUrl(String url) throws IOException {
        if (url == null || url.isBlank()) return false;
        String prefix = "/" + BASE_DIR + "/";
        if (!url.startsWith(prefix)) return false;
        Path relative = Paths.get(url.substring(prefix.length())).normalize();
        if (relative.isAbsolute() || relative.getNameCount() < 3 || relative.startsWith("..")) return false;
        Path file = basePath().resolve(relative).normalize();
        if (!file.startsWith(basePath())) return false;
        return Files.deleteIfExists(file);
    }
}
