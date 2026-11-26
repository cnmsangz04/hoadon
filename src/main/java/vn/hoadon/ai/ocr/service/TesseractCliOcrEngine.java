package vn.hoadon.ai.ocr.service;

import org.springframework.stereotype.Component;
import vn.hoadon.ai.ocr.config.OcrProperties;
import vn.hoadon.ai.ocr.model.OcrText;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

@Component
public class TesseractCliOcrEngine implements OcrEngine {
    private final OcrProperties properties;

    public TesseractCliOcrEngine(OcrProperties properties) {
        this.properties = properties;
    }

    @Override
    public OcrText recognize(BufferedImage image) {
        if (!isAvailable()) {
            throw new IllegalStateException("Tesseract chưa được cài đặt hoặc không thể thực thi");
        }
        Path directory = null;
        try {
            directory = createTempDirectory();
            Path input = directory.resolve("input.png");
            if (!ImageIO.write(image, "png", input.toFile())) {
                throw new IllegalStateException("Không thể tạo ảnh tạm cho OCR");
            }
            List<String> command = new ArrayList<>();
            command.add(safeCommand());
            command.add(input.toString());
            command.add("stdout");
            command.add("-l");
            command.add(safeLanguages());
            command.add("--psm");
            command.add("6");
            command.add("tsv");
            Process process = new ProcessBuilder(command).directory(directory.toFile()).start();
            int limit = Math.max(10_000, properties.getMaxOutputCharacters() * 4);
            FutureTask<byte[]> stdout = reader(process.getInputStream(), limit);
            FutureTask<byte[]> stderr = reader(process.getErrorStream(), 64_000);
            boolean completed = process.waitFor(Math.max(1, properties.getTimeoutSeconds()), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IllegalStateException("Tác vụ Tesseract vượt quá thời gian cho phép");
            }
            byte[] output = stdout.get(5, TimeUnit.SECONDS);
            stderr.get(5, TimeUnit.SECONDS);
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Tesseract không thể nhận diện chứng từ");
            }
            return parseTsv(new String(output, StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tác vụ OCR đã bị gián đoạn", ex);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể thực thi Tesseract", ex);
        } finally {
            deleteRecursively(directory);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder(safeCommand(), "--version").redirectErrorStream(true).start();
            boolean completed = process.waitFor(3, TimeUnit.SECONDS);
            if (!completed) process.destroyForcibly();
            return completed && process.exitValue() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private FutureTask<byte[]> reader(InputStream stream, int limit) {
        FutureTask<byte[]> task = new FutureTask<>(() -> readBounded(stream, limit));
        Thread.startVirtualThread(task);
        return task;
    }

    private byte[] readBounded(InputStream stream, int limit) throws IOException {
        try (stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8_192];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                if (output.size() + read > limit) throw new IOException("Kết quả OCR vượt quá giới hạn");
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private OcrText parseTsv(String tsv) {
        Map<String, StringBuilder> lines = new LinkedHashMap<>();
        double weightedConfidence = 0d;
        int weight = 0;
        String[] rows = tsv.split("\\R");
        for (int index = 1; index < rows.length; index++) {
            String[] columns = rows[index].split("\\t", 12);
            if (columns.length < 12 || !"5".equals(columns[0])) continue;
            String word = columns[11].trim();
            if (word.isEmpty()) continue;
            double confidence;
            try { confidence = Double.parseDouble(columns[10]); }
            catch (NumberFormatException ex) { confidence = -1d; }
            if (confidence < 0d) continue;
            String key = columns[1] + ':' + columns[2] + ':' + columns[3] + ':' + columns[4];
            StringBuilder line = lines.computeIfAbsent(key, ignored -> new StringBuilder());
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
            weightedConfidence += confidence * word.length();
            weight += word.length();
        }
        StringBuilder text = new StringBuilder();
        for (StringBuilder line : lines.values()) {
            if (text.length() > 0) text.append('\n');
            text.append(line);
            if (text.length() > properties.getMaxOutputCharacters()) {
                throw new IllegalStateException("Nội dung OCR vượt quá giới hạn cho phép");
            }
        }
        double confidence = weight == 0 ? 0d : Math.max(0d, Math.min(1d, weightedConfidence / weight / 100d));
        return new OcrText(text.toString().trim(), confidence);
    }

    private Path createTempDirectory() throws IOException {
        String configured = properties.getTempDirectory();
        if (configured == null || configured.isBlank()) return Files.createTempDirectory("hoadon-ocr-");
        Path root = Path.of(configured).toAbsolutePath().normalize();
        Files.createDirectories(root);
        return Files.createTempDirectory(root, "task-");
    }

    private String safeCommand() {
        String value = properties.getCommand();
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Lệnh Tesseract không hợp lệ");
        }
        return value.trim();
    }

    private String safeLanguages() {
        String value = properties.getLanguages();
        if (value == null || !value.matches("[A-Za-z0-9_+.-]{1,80}")) {
            throw new IllegalArgumentException("Cấu hình ngôn ngữ Tesseract không hợp lệ");
        }
        return value;
    }

    private void deleteRecursively(Path directory) {
        if (directory == null) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

}
