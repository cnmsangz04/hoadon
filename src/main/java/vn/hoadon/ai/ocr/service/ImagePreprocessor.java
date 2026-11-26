package vn.hoadon.ai.ocr.service;

import org.springframework.stereotype.Component;
import vn.hoadon.ai.ocr.config.OcrProperties;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

@Component
public class ImagePreprocessor {
    private final OcrProperties properties;

    public ImagePreprocessor(OcrProperties properties) {
        this.properties = properties;
    }

    public BufferedImage prepare(BufferedImage source) {
        if (source == null) throw new IllegalArgumentException("Không đọc được hình ảnh chứng từ");
        validateDimensions(source);
        BufferedImage scaled = scale(source);
        BufferedImage gray = grayscale(scaled);
        stretchContrast(gray);
        BufferedImage denoised = medianFilter(gray);
        double angle = estimateSkew(denoised);
        return Math.abs(angle) >= 0.8d ? rotate(denoised, -angle) : denoised;
    }

    private void validateDimensions(BufferedImage image) {
        long pixels = (long) image.getWidth() * image.getHeight();
        if (image.getWidth() < 1 || image.getHeight() < 1
                || image.getWidth() > properties.getMaxImageWidth()
                || image.getHeight() > properties.getMaxImageHeight()
                || pixels > properties.getMaxImagePixels()) {
            throw new IllegalArgumentException("Kích thước hình ảnh vượt quá giới hạn xử lý");
        }
    }

    private BufferedImage scale(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longest = Math.max(width, height);
        double factor = longest < 1_400 ? Math.min(2d, 1_800d / longest) : 1d;
        int maximum = Math.max(1_000, properties.getMaxProcessedDimension());
        if (longest * factor > maximum) factor = (double) maximum / longest;
        if (Math.abs(factor - 1d) < 0.01d) return source;
        int targetWidth = Math.max(1, (int) Math.round(width * factor));
        int targetHeight = Math.max(1, (int) Math.round(height * factor));
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private BufferedImage grayscale(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private void stretchContrast(BufferedImage image) {
        WritableRaster raster = image.getRaster();
        int[] histogram = new int[256];
        for (int y = 0; y < image.getHeight(); y += 2) {
            for (int x = 0; x < image.getWidth(); x += 2) histogram[raster.getSample(x, y, 0)]++;
        }
        int total = 0;
        for (int value : histogram) total += value;
        int lowTarget = Math.max(1, total / 100);
        int highTarget = Math.max(1, total - total / 100);
        int cumulative = 0;
        int low = 0;
        int high = 255;
        for (int index = 0; index < histogram.length; index++) {
            cumulative += histogram[index];
            if (cumulative >= lowTarget) { low = index; break; }
        }
        cumulative = 0;
        for (int index = 0; index < histogram.length; index++) {
            cumulative += histogram[index];
            if (cumulative >= highTarget) { high = index; break; }
        }
        if (high - low < 30) return;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int value = raster.getSample(x, y, 0);
                int adjusted = Math.max(0, Math.min(255, (value - low) * 255 / (high - low)));
                raster.setSample(x, y, 0, adjusted);
            }
        }
    }

    private BufferedImage medianFilter(BufferedImage source) {
        if ((long) source.getWidth() * source.getHeight() > 10_000_000L) return source;
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster input = source.getRaster();
        WritableRaster output = result.getRaster();
        int[] values = new int[9];
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int count = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    int yy = Math.max(0, Math.min(source.getHeight() - 1, y + dy));
                    for (int dx = -1; dx <= 1; dx++) {
                        int xx = Math.max(0, Math.min(source.getWidth() - 1, x + dx));
                        values[count++] = input.getSample(xx, yy, 0);
                    }
                }
                java.util.Arrays.sort(values);
                output.setSample(x, y, 0, values[4]);
            }
        }
        return result;
    }

    private double estimateSkew(BufferedImage image) {
        int step = Math.max(2, Math.max(image.getWidth(), image.getHeight()) / 900);
        WritableRaster raster = image.getRaster();
        double bestAngle = 0d;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int degree = -5; degree <= 5; degree++) {
            double tangent = Math.tan(Math.toRadians(degree));
            int[] rows = new int[Math.max(1, image.getHeight() / step + 4)];
            for (int y = 0; y < image.getHeight(); y += step) {
                for (int x = 0; x < image.getWidth(); x += step) {
                    if (raster.getSample(x, y, 0) > 145) continue;
                    int shifted = (int) Math.round(y + tangent * (x - image.getWidth() / 2d));
                    int index = shifted / step + 2;
                    if (index >= 0 && index < rows.length) rows[index]++;
                }
            }
            double score = 0d;
            for (int index = 1; index < rows.length; index++) {
                double difference = rows[index] - rows[index - 1];
                score += difference * difference;
            }
            if (score > bestScore) {
                bestScore = score;
                bestAngle = degree;
            }
        }
        return bestAngle;
    }

    private BufferedImage rotate(BufferedImage source, double degrees) {
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int width = (int) Math.floor(source.getWidth() * cos + source.getHeight() * sin);
        int height = (int) Math.floor(source.getHeight() * cos + source.getWidth() * sin);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            AffineTransform transform = new AffineTransform();
            transform.translate((width - source.getWidth()) / 2d, (height - source.getHeight()) / 2d);
            transform.rotate(radians, source.getWidth() / 2d, source.getHeight() / 2d);
            graphics.drawRenderedImage(source, transform);
        } finally {
            graphics.dispose();
        }
        return result;
    }
}
