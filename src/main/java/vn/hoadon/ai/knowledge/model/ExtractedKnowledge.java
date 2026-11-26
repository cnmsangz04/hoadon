package vn.hoadon.ai.knowledge.model;

public record ExtractedKnowledge(String content, boolean requiresOcr, Integer pageCount,
                                 boolean ocrUsed, Double ocrAverageConfidence) {
}
