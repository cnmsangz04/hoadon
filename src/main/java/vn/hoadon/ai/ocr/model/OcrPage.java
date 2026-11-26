package vn.hoadon.ai.ocr.model;

public record OcrPage(int pageNumber, String text, double confidence, String method) {
}
