package vn.hoadon.controllers;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

public class FileControllerSanitizeTest {

    private String invokeSanitize(String html) throws Exception {
        FileController controller = new FileController(null);
        Method m = FileController.class.getDeclaredMethod("sanitizeImgSrcAttributes", String.class);
        m.setAccessible(true);
        return (String) m.invoke(controller, html);
    }

    @Test
    void svgDataUriWithRawLtIsConvertedToBase64() throws Exception {
        String input = "<html><body><img src=\"data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg'><rect/></svg>\"/></body></html>";
        String out = invokeSanitize(input);
        assertTrue(out.contains("data:image/svg+xml;base64,"), "SVG data URI should be converted to base64");
        assertFalse(out.contains("data:image/svg+xml,<svg"), "Raw '<' sequence should not remain in src");
    }

    @Test
    void normalHttpUrlRemainsUnchanged() throws Exception {
        String input = "<img src=\"https://example.com/logo.png\" alt=\"x\"/>";
        String out = invokeSanitize(input);
        assertEquals(input, out, "Safe URLs should remain unchanged");
    }

    @Test
    void genericSrcWithAngleBracketsIsPercentEncoded() throws Exception {
        String input = "<img src=\"/images/<unsafe>.png\"/>";
        String out = invokeSanitize(input);
        assertTrue(out.contains("%3Cunsafe%3E"), "Angle brackets should be percent-encoded in src");
        assertFalse(out.contains("<unsafe>"), "Raw '<' should not remain in src");
    }
}
