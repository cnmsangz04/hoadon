package vn.hoadon.util;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

public final class ErrorMessageXsltRenderer {
    private static final String XSLT_RESOURCE = "/xslt/thong_bao_hoa_don_sai_sot.xslt";
    private static volatile Templates templates;

    private ErrorMessageXsltRenderer() {}

    public static String render(String xml, boolean signed) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("Không có dữ liệu XML thông báo sai sót");
        }
        try {
            Transformer transformer = templates().newTransformer();
            transformer.setParameter("status", signed ? 1 : 0);
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            StringWriter output = new StringWriter();
            transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(output));
            return output.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể hiển thị thông báo sai sót bằng XSLT", ex);
        }
    }

    private static Templates templates() {
        Templates value = templates;
        if (value != null) return value;
        synchronized (ErrorMessageXsltRenderer.class) {
            if (templates != null) return templates;
            TransformerFactory factory = TransformerFactory.newInstance();
            try { factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); } catch (Exception ignored) {}
            try { factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); } catch (Exception ignored) {}
            try { factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, ""); } catch (Exception ignored) {}
            try (InputStream input = ErrorMessageXsltRenderer.class.getResourceAsStream(XSLT_RESOURCE)) {
                if (input == null) throw new IllegalStateException("Không tìm thấy XSLT " + XSLT_RESOURCE);
                StreamSource source = new StreamSource(input);
                source.setSystemId(XSLT_RESOURCE);
                templates = factory.newTemplates(source);
                return templates;
            } catch (Exception ex) {
                throw new IllegalStateException("Không thể nạp XSLT thông báo sai sót", ex);
            }
        }
    }
}
