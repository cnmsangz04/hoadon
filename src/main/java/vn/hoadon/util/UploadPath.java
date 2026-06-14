package vn.hoadon.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class UploadPath {
    private UploadPath() {}

    public static final String BASE_DIR = "uploads";

    // Nếu cần che companyId, có thể đổi hàm này sang dạng mã hóa như Base64 hoặc hash.
    public static String encodeCompanyId(Long companyId) {
        return String.valueOf(companyId);
    }

    public static Path baseDir() throws IOException {
        Path base = Paths.get(BASE_DIR);
        Files.createDirectories(base);
        return base;
    }

    public static Path resolveCompanyTypeDir(Long companyId, String type) throws IOException {
        Path dir = Paths.get(BASE_DIR, encodeCompanyId(companyId), type);
        Files.createDirectories(dir);
        return dir;
    }

    public static String publicUrl(Long companyId, String type, String fileName) {
        return "/" + BASE_DIR + "/" + encodeCompanyId(companyId) + "/" + type + "/" + fileName;
    }
}
