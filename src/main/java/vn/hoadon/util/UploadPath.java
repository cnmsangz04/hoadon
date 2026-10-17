package vn.hoadon.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class UploadPath {
    private UploadPath() {}

    public static final String BASE_DIR = "uploads";
    public static final String UPLOAD_DIR_ENV = "APP_UPLOAD_DIR";
    public static final String UPLOAD_DIR_PROPERTY = "app.upload-dir";

    /**
     * Thư mục lưu file vật lý. Local mặc định dùng ./uploads; production nên
     * khai báo APP_UPLOAD_DIR bằng một đường dẫn tuyệt đối nằm ngoài source/JAR.
     */
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

    // Nếu cần che companyId, có thể đổi hàm này sang dạng mã hóa như Base64 hoặc hash.
    public static String encodeCompanyId(Long companyId) {
        return String.valueOf(companyId);
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
}
