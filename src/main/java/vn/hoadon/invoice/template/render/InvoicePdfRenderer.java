package vn.hoadon.invoice.template.render;

import java.util.Map;

public interface InvoicePdfRenderer {
    record Resource(byte[] bytes, String mimeType) { }
    record Result(byte[] bytes, int pageCount) { }
    Result render(String html, Map<Long, Resource> resources);
}
