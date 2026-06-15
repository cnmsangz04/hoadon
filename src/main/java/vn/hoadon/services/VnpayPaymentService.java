package vn.hoadon.services;

import java.util.Map;

public interface VnpayPaymentService {

    CreatePaymentResponse createPaymentUrl(CreatePaymentRequest request);

    boolean verifyCallbackSignature(Map<String, ?> payload);

    boolean isSuccessResult(Map<String, ?> payload);

    boolean isExpectedTmnCode(String tmnCode);

    String value(Map<String, ?> payload, String key);

    record CreatePaymentRequest(
            String txnRef,
            long amount,
            String orderInfo,
            String ipAddress,
            String returnUrl
    ) {}

    record CreatePaymentResponse(
            String payUrl,
            String message
    ) {}
}
