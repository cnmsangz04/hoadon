package vn.hoadon.ai.ocr.model;

import java.util.List;

public record OcrDocument(String text, int pageCount, boolean ocrUsed, boolean complete,
                          double averageConfidence, long durationMs, List<OcrPage> pages) {
}
