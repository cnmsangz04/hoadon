package vn.hoadon.ai.document.controller;

import java.util.List;

public final class DocumentRecognitionDtos {
    private DocumentRecognitionDtos() { }

    public record FieldValue(String value, double confidence, boolean lowConfidence, String evidence) { }

    public record PartyView(FieldValue name, FieldValue taxCode, FieldValue address,
                            Long matchedCustomerId, String matchedCustomerCode) { }

    public record ItemView(FieldValue name, FieldValue unit, FieldValue quantity, FieldValue unitPrice,
                           FieldValue vatRate, FieldValue lineTotal, Long matchedProductId,
                           String matchedProductCode) { }

    public record RecognitionResponse(String filename, String sourceType, int pageCount, boolean ocrUsed,
                                      double averageConfidence, long durationMs, PartyView seller, PartyView buyer,
                                      FieldValue documentDate, FieldValue currency, List<ItemView> items,
                                      FieldValue subtotal, FieldValue vatAmount, FieldValue total,
                                      List<String> warnings) { }
}
