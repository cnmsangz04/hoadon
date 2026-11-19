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
        if (snapshot.datasets().stream().anyMatch(dataset -> dataset.parent() != null))
            return mapLegacyHierarchy(document, context, snapshot);

        Map<String, Object> result = new LinkedHashMap<>();
        addPresentationContext(result, context);
        int[] count = {0};
        var invoiceRoots = elements(document, "/HDon");
        if (invoiceRoots.size() != 1) {
            throw TemplateException.invalid(
                    "DATASET_ROOT_INVALID", "XML hóa đơn phải có đúng một phần tử /HDon");
        }
        for (var group : snapshot.groups()) {
            if ("document".equals(group.code()) || !group.active()) continue;
            var groupFields = snapshot.fields().stream()
                    .filter(field -> group.code().equals(field.group()))
                    .filter(InvoiceCatalogSnapshot.Field::active)
                    .toList();
            if (groupFields.isEmpty()) continue;
            var roots = elements(document, group.rootPath());
            if ("MANY".equals(group.cardinality())) {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (var root : roots) rows.add(mapGroupRow(root, group, groupFields, context, count));
                result.put(group.code(), rows);
                continue;
            }
            if (roots.size() > 1) {
                throw TemplateException.invalid(
                        "DATASET_CARDINALITY", "Bảng dữ liệu đơn có nhiều dòng: " + group.code());
            }
            result.putAll(mapGroupRow(roots.isEmpty() ? null : roots.getFirst(), group, groupFields, context, count));
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<String, Object> mapGroupRow(
            Element root,
            InvoiceCatalogSnapshot.Group group,
            List<InvoiceCatalogSnapshot.Field> fields,
            Map<String, String> context,
            int[] count) {
        if (++count[0] > 100000)
            throw TemplateException.invalid("DATASET_LIMIT", "Dữ liệu lặp vượt giới hạn");
        Map<String, Object> result = new LinkedHashMap<>();
        for (var field : fields) {
            String value = root == null ? "" : relativeText(root, field.sourcePath());
            result.put(field.key(), typed(field, value));
        }
        return result;
    }

    private Map<String, Object> mapLegacyHierarchy(
            Document document,
            Map<String, String> context,
            InvoiceCatalogSnapshot snapshot) {
        Map<String, Object> result = new LinkedHashMap<>();
        addPresentationContext(result, context);
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
            String value = relativeText(root, field.sourcePath());
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

    private static void addPresentationContext(Map<String, Object> result, Map<String, String> context) {
        if (context == null) return;
        for (String key : List.of("sampleMarker", "signatureSummary", "statusLabel", "lookupCode", "authorityCode")) {
            String value = context.get(key);
            if (value != null) result.put("presentation." + key, value);
        }
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
            PathSegment segment = PathSegment.parse(part);
            List<Element> next = new ArrayList<>();
            for (Element node : current) {
                for (Node child = node.getFirstChild();
                        child != null;
                        child = child.getNextSibling()) {
                    if (child instanceof Element element
                            && name(element).equals(segment.name())
                            && segment.matches(element))
                        next.add(element);
                }
            }
            current = next;
        }
        return current;
    }

    /** Supports a small XML path predicate for repeated TTin nodes. */
    private record PathSegment(String name, String filterName, String filterValue) {
        static PathSegment parse(String value) {
            int open = value.indexOf('[');
            if (open < 1 || !value.endsWith("]")) return new PathSegment(value, null, null);
            String name = value.substring(0, open);
            String expression = value.substring(open + 1, value.length() - 1);
            int equals = expression.indexOf('=');
            if (equals < 1 || equals == expression.length() - 1)
                return new PathSegment(value, null, null);
            String filterValue = expression.substring(equals + 1).trim();
            if (filterValue.length() < 2
                    || !((filterValue.startsWith("'") && filterValue.endsWith("'"))
                    || (filterValue.startsWith("\"") && filterValue.endsWith("\""))))
                return new PathSegment(value, null, null);
            return new PathSegment(
                    name,
                    expression.substring(0, equals).trim(),
                    filterValue.substring(1, filterValue.length() - 1));
        }

        boolean matches(Element element) {
            if (filterName == null) return true;
            for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child instanceof Element candidate
                        && InvoiceViewModelMapper.name(candidate).equals(filterName)
                        && text(candidate).equals(filterValue)) return true;
            }
            return false;
        }
    }

    private static String name(Element element) {
        return element.getLocalName() != null ? element.getLocalName() : element.getTagName();
    }

    private static String text(Element element) {
        return element == null ? "" : element.getTextContent();
    }
}
