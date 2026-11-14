package vn.hoadon.invoice.template.catalog;

import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import vn.hoadon.invoice.template.service.TemplateException;

import java.io.StringReader;
import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

@Component
public class InvoiceViewModelMapper {
    private final InvoiceFieldCatalog catalog;

    public InvoiceViewModelMapper(InvoiceFieldCatalog catalog) {
        this.catalog = catalog;
    }

    public Map<String, Object> map(String xml, Map<String, String> context) {
        return map(xml, context, catalog.latest());
    }

    public Map<String, Object> map(
            String xml, Map<String, String> context, InvoiceCatalogSnapshot snapshot) {
        Document document = parse(xml);
        Map<String, Object> result = new LinkedHashMap<>();
        int[] count = {0};
        for (var dataset : snapshot.datasets()) {
            if (dataset.parent() != null) continue;
            var roots = elements(document, dataset.rootPath());
            if (!"ONE".equals(dataset.cardinality()) || roots.size() != 1) {
                throw TemplateException.invalid(
                        "DATASET_ROOT_INVALID",
                        "Root hóa đơn phải chứa đúng một phần tử: " + dataset.code());
            }
            result.putAll(mapRow(roots.getFirst(), dataset, snapshot, context, count, 0));
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<String, Object> mapRow(
            Element root,
            InvoiceCatalogSnapshot.Dataset dataset,
            InvoiceCatalogSnapshot snapshot,
            Map<String, String> context,
            int[] count,
            int depth) {
        if (depth > 16 || ++count[0] > 100000)
            throw TemplateException.invalid("DATASET_LIMIT", "Dữ liệu lặp vượt giới hạn");
        Map<String, Object> result = new LinkedHashMap<>();
        for (var field : snapshot.fields()) {
            if (!dataset.code().equals(field.dataset())) continue;
            String value =
                    "CONTEXT".equals(field.sourceType())
                            ? InvoiceExternalFieldRegistry.read(context, field.sourcePath())
                            : relativeText(root, field.sourcePath());
            result.put(field.key(), typed(field, value));
        }
        for (var child : snapshot.datasets()) {
            if (!dataset.code().equals(child.parent())) continue;
            var nodes = descendants(root, Arrays.asList(child.rootPath().split("/")));
            if ("ONE".equals(child.cardinality())) {
                if (nodes.size() > 1)
                    throw TemplateException.invalid(
                            "DATASET_CARDINALITY", "Dữ liệu đơn có nhiều dòng: " + child.code());
                if (!nodes.isEmpty())
                    result.putAll(
                            mapRow(nodes.getFirst(), child, snapshot, context, count, depth + 1));
            } else {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (var node : nodes)
                    rows.add(mapRow(node, child, snapshot, context, count, depth + 1));
                result.put(child.code(), rows);
            }
        }
        return result;
    }

    private static String relativeText(Element root, String selector) {
        var values = descendants(root, Arrays.asList(selector.split("/")));
        if (values.size() > 1)
            throw TemplateException.invalid(
                    "FIELD_CARDINALITY", "Trường đơn có nhiều giá trị: " + selector);
        return values.isEmpty() ? "" : text(values.getFirst());
    }

    private static Object typed(InvoiceCatalogSnapshot.Field field, String value) {
        if (value == null || value.isBlank()) return "";
        try {
            return switch (field.valueType()) {
                case "DECIMAL" -> new java.math.BigDecimal(value);
                case "INTEGER" -> {
                    new java.math.BigInteger(value);
                    yield value;
                }
                case "DATE" -> java.time.LocalDate.parse(value).toString();
                case "BOOLEAN" -> {
                    if (!Set.of("0", "1", "true", "false").contains(value))
                        throw new IllegalArgumentException();
                    yield value.equals("1") || value.equals("true");
                }
                default -> value;
            };
        } catch (IllegalArgumentException | java.time.DateTimeException ex) {
            throw TemplateException.invalid(
                    "INVALID_FIELD_VALUE",
                    "Dữ liệu không đúng kiểu " + field.valueType() + ": " + field.key());
        }
    }

    public static Document parse(String xml) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception ex) {
            throw TemplateException.invalid("INVALID_XML", "Dữ liệu XML hóa đơn không hợp lệ");
        }
    }

    private static List<Element> elements(Document document, String path) {
        List<String> parts = Arrays.stream(path.split("/")).filter(p -> !p.isBlank()).toList();
        Element root = document.getDocumentElement();
        if (parts.isEmpty() || !name(root).equals(parts.getFirst())) return List.of();
        return descendants(root, parts.subList(1, parts.size()));
    }

    private static List<Element> descendants(Element parent, List<String> parts) {
        List<Element> current = List.of(parent);
        for (String part : parts) {
            List<Element> next = new ArrayList<>();
            for (Element node : current) {
                for (Node child = node.getFirstChild();
                        child != null;
                        child = child.getNextSibling()) {
                    if (child instanceof Element element && name(element).equals(part))
                        next.add(element);
                }
            }
            current = next;
        }
        return current;
    }

    private static String name(Element element) {
        return element.getLocalName() != null ? element.getLocalName() : element.getTagName();
    }

    private static String text(Element element) {
        return element == null ? "" : element.getTextContent();
    }
}
