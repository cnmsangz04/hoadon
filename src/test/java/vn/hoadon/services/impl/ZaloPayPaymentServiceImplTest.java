package vn.hoadon.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.hoadon.config.ZaloPayProperties;
import vn.hoadon.services.ZaloPayPaymentService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZaloPayPaymentServiceImplTest {

    @Test
    void acceptsValidCallbackSignature() {
        ZaloPayPaymentServiceImpl service = serviceWithKey2("callback-secret");
        String data = "{\"app_id\":2553,\"app_trans_id\":\"260618_HD1\",\"amount\":120000,\"zp_trans_id\":123456}";
        String mac = hmacSha256("callback-secret", data);

        ZaloPayPaymentService.CallbackPayment result = service.verifyCallback(Map.of(
                "data", data,
                "mac", mac
        ));

        assertThat(result.appId()).isEqualTo("2553");
        assertThat(result.appTransId()).isEqualTo("260618_HD1");
        assertThat(result.amount()).isEqualTo(120000L);
        assertThat(result.zpTransId()).isEqualTo("123456");
    }

    @Test
    void rejectsInvalidCallbackSignature() {
        ZaloPayPaymentServiceImpl service = serviceWithKey2("callback-secret");
        String data = "{\"app_id\":2553,\"app_trans_id\":\"260618_HD1\",\"amount\":120000}";

        assertThatThrownBy(() -> service.verifyCallback(Map.of(
                "data", data,
                "mac", "sai-chu-ky"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chữ ký callback ZaloPay không hợp lệ");
    }

    @Test
    void acceptsValidRedirectChecksum() {
        ZaloPayPaymentServiceImpl service = serviceWithKey2("redirect-secret");
        String checksumData = "2553|260618_HD1|38|zalopayapp|120000|0|1";
        String checksum = hmacSha256("redirect-secret", checksumData);

        boolean valid = service.verifyRedirectChecksum(Map.of(
                "app_id", "2553",
                "app_trans_id", "260618_HD1",
                "pmc_id", "38",
                "bank_code", "zalopayapp",
                "amount", "120000",
                "discount_amount", "0",
                "status", "1",
                "checksum", checksum
        ));

        assertThat(valid).isTrue();
    }

    private ZaloPayPaymentServiceImpl serviceWithKey2(String key2) {
        ZaloPayProperties properties = new ZaloPayProperties();
        properties.setKey2(key2);
        properties.setKey1("key1");
        properties.setAppId("2553");
        return new ZaloPayPaymentServiceImpl(properties, new ObjectMapper());
    }

    private static String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
