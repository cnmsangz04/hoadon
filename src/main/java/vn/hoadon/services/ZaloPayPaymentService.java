package vn.hoadon.services;

import java.util.Map;

public interface ZaloPayPaymentService {

    CreatePaymentResponse createPayment(CreatePaymentRequest request);

    CallbackPayment verifyCallback(Map<String, ?> payload);

    boolean verifyRedirectChecksum(Map<String, ?> params);

    boolean isSuccessRedirect(Map<String, ?> params);

    boolean isExpectedAppId(String appId);

    QueryStatusResponse queryStatus(String appTransId);

    BankListResponse getSupportedBanks();

    String value(Map<String, ?> payload, String key);

    record CreatePaymentRequest(
            String appTransId,
            long amount,
            String appUser,
            String description,
            String embedData,
            String item,
            String bankCode,
            String redirectUrl,
            String callbackUrl
    ) {}

    record CreatePaymentResponse(
            String payUrl,
            String zpTransToken,
            String message,
            int returnCode,
            Map<String, Object> rawResponse
    ) {}

    record CallbackPayment(
            String appId,
            String appTransId,
            long amount,
            String zpTransId,
            Map<String, Object> data
    ) {}

    record QueryStatusResponse(
            int returnCode,
            String returnMessage,
            boolean processing,
            long amount,
            long discountAmount,
            String zpTransId,
            Map<String, Object> rawResponse
    ) {}

    record BankListResponse(
            int returnCode,
            String returnMessage,
            Map<String, Object> banks,
            Map<String, Object> rawResponse
    ) {}
}
