package vn.hoadon.ai.ocr.service;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import vn.hoadon.ai.ocr.config.OcrProperties;
import vn.hoadon.ai.ocr.model.OcrDocument;
import vn.hoadon.ai.ocr.model.OcrPage;
import vn.hoadon.ai.ocr.model.OcrText;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class LocalOcrService {
    private final OcrProperties properties;
    private final OcrEngine engine;
    private final ImagePreprocessor preprocessor;
    private final Semaphore slots;

    public LocalOcrService(OcrProperties properties, OcrEngine engine, ImagePreprocessor preprocessor) {
        this.properties = properties;
        this.engine = engine;
        this.preprocessor = preprocessor;
        this.slots = new Semaphore(Math.max(1, properties.getMaxConcurrentTasks()), true);
        ImageIO.setUseCache(false);
    }

    public OcrDocument extract(byte[] source, String filename) {
        return extract(source, filename, Math.max(1, properties.getMaxPdfPages()), Math.max(1, properties.getMaxPdfPages()));
    }

    public OcrDocument extractKnowledgePdf(byte[] source, int maxSourcePages) {
        if (!isPdf(source)) throw new IllegalArgumentException("Nội dung file không phải PDF hợp lệ");
        return extract(source, "knowledge.pdf", Math.max(1, maxSourcePages), Math.max(1, properties.getMaxPdfPages()));
    }

    private OcrDocument extract(byte[] source, String filename, int maxSourcePages, int maxOcrPages) {
        validateSource(source);
        boolean acquired = false;
        long started = System.nanoTime();
        try {
            acquired = slots.tryAcquire(Math.max(1, properties.getQueueWaitSeconds()), TimeUnit.SECONDS);
            if (!acquired) throw new IllegalStateException("Hệ thống OCR đang bận, vui lòng thử lại sau");
            if (isPdf(source)) return pdf(source, started, maxSourcePages, maxOcrPages);
            if (isPng(source) || isJpeg(source)) return image(source, started);
            throw new IllegalArgumentException("Chỉ hỗ trợ file JPG, JPEG, PNG hoặc PDF hợp lệ");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tác vụ OCR đã bị gián đoạn", ex);
        } finally {
            if (acquired) slots.release();
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isEngineAvailable() {
        return engine.isAvailable();
    }

    private OcrDocument image(byte[] source, long started) {
        requireOcrAvailable();
        ensureWithinDeadline(started);
        BufferedImage decoded = decodeImage(source);
        BufferedImage prepared = preprocessor.prepare(decoded);
        OcrText result = engine.recognize(prepared);
        ensureWithinDeadline(started);
        OcrPage page = new OcrPage(1, limit(result.text()), result.confidence(), "TESSERACT");
        return document(List.of(page), true, true, started);
    }

    private OcrDocument pdf(byte[] source, long started, int maxSourcePages, int maxOcrPages) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(source),
                MemoryUsageSetting.setupMixed(32L * 1024L * 1024L))) {
            if (document.isEncrypted()) throw new IllegalArgumentException("Không hỗ trợ PDF được mã hóa");
            int count = document.getNumberOfPages();
            if (count < 1) throw new IllegalArgumentException("PDF không có trang nội dung");
            if (count > maxSourcePages) {
                throw new IllegalArgumentException("PDF vượt quá giới hạn số trang xử lý");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            PDFRenderer renderer = new PDFRenderer(document);
            List<OcrPage> pages = new ArrayList<>(count);
            boolean ocrUsed = false;
            boolean complete = true;
            for (int index = 0; index < count; index++) {
                ensureWithinDeadline(started);
                int pageNumber = index + 1;
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String direct = normalize(stripper.getText(document));
                if (compactLength(direct) >= Math.max(1, properties.getMinPdfTextCharacters())) {
                    pages.add(new OcrPage(pageNumber, limit(direct), 1d, "PDF_TEXT"));
                    continue;
                }
                if (pageNumber > maxOcrPages || !properties.isEnabled() || !engine.isAvailable()) {
                    pages.add(new OcrPage(pageNumber, direct, 0d, "OCR_REQUIRED"));
                    complete = false;
                    continue;
                }
                validateRenderedSize(document.getPage(index));
                BufferedImage rendered = renderer.renderImageWithDPI(index,
                        Math.max(96, properties.getRenderDpi()), ImageType.RGB);
                OcrText recognized = engine.recognize(preprocessor.prepare(rendered));
                ensureWithinDeadline(started);
                pages.add(new OcrPage(pageNumber, limit(recognized.text()), recognized.confidence(), "TESSERACT"));
                ocrUsed = true;
                if (recognized.text() == null || recognized.text().isBlank()) complete = false;
            }
            return document(pages, ocrUsed, complete, started);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không thể đọc nội dung PDF", ex);
        }
    }

    private OcrDocument document(List<OcrPage> pages, boolean ocrUsed, boolean complete, long started) {
        StringBuilder combined = new StringBuilder();
        double confidenceTotal = 0d;
        int confidenceWeight = 0;
        for (OcrPage page : pages) {
            String text = page.text() == null ? "" : page.text().trim();
            if (!text.isEmpty()) {
                if (!combined.isEmpty()) combined.append("\n\n");
                combined.append("## Trang ").append(page.pageNumber()).append("\n\n").append(text);
            }
            int weight = Math.max(1, text.length());
            confidenceTotal += page.confidence() * weight;
            confidenceWeight += weight;
            if (combined.length() > properties.getMaxOutputCharacters()) {
                throw new IllegalArgumentException("Nội dung nhận diện vượt quá giới hạn xử lý");
            }
        }
        double confidence = confidenceWeight == 0 ? 0d : confidenceTotal / confidenceWeight;
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        return new OcrDocument(combined.toString(), pages.size(), ocrUsed, complete, confidence, duration,
                List.copyOf(pages));
    }

    private BufferedImage decodeImage(byte[] source) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            if (stream == null) throw new IllegalArgumentException("Không đọc được hình ảnh chứng từ");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException("Định dạng hình ảnh không được hỗ trợ");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateImageSize(width, height);
                BufferedImage image = reader.read(0);
                if (image == null) throw new IllegalArgumentException("Không đọc được hình ảnh chứng từ");
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Hình ảnh chứng từ bị lỗi", ex);
        }
    }

    private void validateRenderedSize(PDPage page) {
        double scale = Math.max(96, properties.getRenderDpi()) / 72d;
        int width = (int) Math.ceil(page.getCropBox().getWidth() * scale);
        int height = (int) Math.ceil(page.getCropBox().getHeight() * scale);
        validateImageSize(width, height);
    }

    private void validateImageSize(int width, int height) {
        long pixels = (long) width * height;
        if (width < 1 || height < 1 || width > properties.getMaxImageWidth()
                || height > properties.getMaxImageHeight() || pixels > properties.getMaxImagePixels()) {
            throw new IllegalArgumentException("Kích thước hình ảnh vượt quá giới hạn xử lý");
        }
    }

    private void validateSource(byte[] source) {
        if (source == null || source.length == 0) throw new IllegalArgumentException("Vui lòng chọn chứng từ");
        if (source.length > properties.getMaxUploadBytes()) {
            throw new IllegalArgumentException("Chứng từ vượt quá giới hạn dung lượng");
        }
    }

    private void requireOcrAvailable() {
        if (!properties.isEnabled()) throw new IllegalStateException("Chức năng nhận diện chứng từ đang tắt");
        if (!engine.isAvailable()) throw new IllegalStateException("Tesseract chưa sẵn sàng trên máy chủ");
    }

    private void ensureWithinDeadline(long started) {
        long elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started);
        if (elapsedSeconds >= Math.max(1, properties.getTimeoutSeconds())) {
            throw new IllegalStateException("Tác vụ OCR vượt quá thời gian cho phép");
        }
    }

    private String limit(String value) {
        String normalized = normalize(value);
        if (normalized.length() > properties.getMaxOutputCharacters()) {
            throw new IllegalArgumentException("Nội dung nhận diện vượt quá giới hạn xử lý");
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private int compactLength(String value) {
        return value == null ? 0 : value.replaceAll("\\s+", "").length();
    }

    private boolean isPdf(byte[] source) {
        return source.length >= 5 && source[0] == '%' && source[1] == 'P' && source[2] == 'D'
                && source[3] == 'F' && source[4] == '-';
    }

    private boolean isPng(byte[] source) {
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
        if (source.length < signature.length) return false;
        for (int index = 0; index < signature.length; index++) if (source[index] != signature[index]) return false;
        return true;
    }

    private boolean isJpeg(byte[] source) {
        return source.length >= 3 && (source[0] & 0xff) == 0xff && (source[1] & 0xff) == 0xd8
                && (source[2] & 0xff) == 0xff;
    }
}
