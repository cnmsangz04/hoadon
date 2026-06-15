package vn.hoadon.services;

import java.util.Map;

public interface MomoPaymentService {

    CreatePaymentResponse createPayment(CreatePaymentRequest request);

    boolean verifyCallbackSignature(Map<String, ?> payload);

    boolean isSuccessResult(Map<String, ?> payload);

    boolean isExpectedPartner(String partnerCode);

    String value(Map<String, ?> payload, String key);

    record CreatePaymentRequest(
            String orderId,
            String requestId,
            long amount,
            String orderInfo,
            String extraData,
            String redirectUrl,
            String ipnUrl
    ) {}

    record CreatePaymentResponse(
            String payUrl,
            String deeplink,
            String qrCodeUrl,
            String message,
            int resultCode,
            Map<String, Object> rawResponse
    ) {}
}
