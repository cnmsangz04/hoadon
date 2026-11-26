package vn.hoadon.ai.ocr.service;

import vn.hoadon.ai.ocr.model.OcrText;

import java.awt.image.BufferedImage;

public interface OcrEngine {
    OcrText recognize(BufferedImage image);
    boolean isAvailable();
}
