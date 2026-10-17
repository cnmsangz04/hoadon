package vn.hoadon.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadPathTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearUploadDirProperty() {
        System.clearProperty(UploadPath.UPLOAD_DIR_PROPERTY);
    }

    @Test
    void usesConfiguredExternalUploadDirectory() throws Exception {
        System.setProperty(UploadPath.UPLOAD_DIR_PROPERTY, tempDir.toString());

        Path avatarDir = UploadPath.resolveCompanyTypeDir(1L, "avatars");

        assertEquals(tempDir.toAbsolutePath().normalize().resolve("1/avatars"), avatarDir);
        assertTrue(avatarDir.toFile().isDirectory());
        assertEquals("/uploads/1/avatars/avatar.png",
                UploadPath.publicUrl(1L, "avatars", "avatar.png"));
    }
}
