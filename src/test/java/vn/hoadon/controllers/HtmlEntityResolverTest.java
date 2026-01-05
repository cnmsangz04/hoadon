package vn.hoadon.controllers;

import org.junit.jupiter.api.Test;

import vn.hoadon.controllers.customers.FormInvoiceController;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

public class HtmlEntityResolverTest {

    @Test
    void testResolveNamedHtmlEntities_FileController() throws Exception {
        FileController c = new FileController(null);
        Method m = FileController.class.getDeclaredMethod("resolveNamedHtmlEntities", String.class);
        m.setAccessible(true);
        String input = "H&oacute;a &nbsp;đơn &amp; thuế";
        String out = (String) m.invoke(c, input);
        assertTrue(out.contains("Hóa"), "Should decode &oacute; to ó");
        assertTrue(out.contains("\u00A0") || out.contains(" "), "Should decode &nbsp; to non-breaking space");
        assertTrue(out.contains("&amp;"), "Should preserve &amp; entity");
    }

    @Test
    void testResolveNamedHtmlEntities_FormInvoiceController() throws Exception {
        FormInvoiceController c = new FormInvoiceController(null, null);
        Method m = FormInvoiceController.class.getDeclaredMethod("resolveNamedHtmlEntities", String.class);
        m.setAccessible(true);
        String input = "C&oacute; &oacute; &eacute; &lt;xml&gt;";
        String out = (String) m.invoke(c, input);
        assertTrue(out.contains("Có ó é"), "Should decode multiple named entities");
        assertTrue(out.contains("&lt;xml&gt;"), "Should preserve XML core entities");
    }

    @Test
    void testNormalizeToXhtml_VoidTags_FileController() throws Exception {
        FileController c = new FileController(null);
        Method m = FileController.class.getDeclaredMethod("normalizeToXhtml", String.class);
        m.setAccessible(true);
        String input = "<head><meta charset=\"UTF-8\"><link rel=\"stylesheet\" href=\"/a.css\"></head>"
                + "<img src=\"x.png\"><br><hr><input type=\"text\">";
        String out = (String) m.invoke(c, input);
        assertTrue(out.contains("<meta charset=\"UTF-8\"/>"), "meta should be self-closed");
        assertTrue(out.contains("<link rel=\"stylesheet\" href=\"/a.css\"/>"), "link should be self-closed");
        assertTrue(out.contains("<img src=\"x.png\"/>"), "img should be self-closed");
        assertTrue(out.contains("<br/>"), "br should be self-closed");
        assertTrue(out.contains("<hr/>"), "hr should be self-closed");
        assertTrue(out.contains("<input type=\"text\"/>"), "input should be self-closed");
        // Existing self-closed should remain valid
        String already = "<meta charset=\"UTF-8\"/>";
        String out2 = (String) m.invoke(c, already);
        assertEquals(already, out2, "Already self-closed tags should remain unchanged");
    }

    @Test
    void testNormalizeToXhtml_VoidTags_FormInvoiceController() throws Exception {
        FormInvoiceController c = new FormInvoiceController(null, null);
        Method m = FormInvoiceController.class.getDeclaredMethod("normalizeToXhtml", String.class);
        m.setAccessible(true);
        String input = "<head><META charset=\"UTF-8\"></head><IMG src=\"x.png\">";
        String out = (String) m.invoke(c, input);
        assertTrue(out.toLowerCase().contains("<meta charset=\"utf-8\"/>"), "meta (case-insensitive) should be self-closed");
        assertTrue(out.toLowerCase().contains("<img src=\"x.png\"/>"), "img (case-insensitive) should be self-closed");
    }
}