package vn.hoadon.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpUtilTest {

    @Test
    void ignoresUntrustedForwardedHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.12");
        request.addHeader("X-Forwarded-For", "203.0.113.50");
        request.addHeader("X-Real-IP", "203.0.113.51");

        assertThat(ClientIpUtil.resolve(request)).isEqualTo("10.0.0.12");
    }

    @Test
    void normalizesIpv6Loopback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::1");

        assertThat(ClientIpUtil.resolve(request)).isEqualTo("127.0.0.1");
    }
}
