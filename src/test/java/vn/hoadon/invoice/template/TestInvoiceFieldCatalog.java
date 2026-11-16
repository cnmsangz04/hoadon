package vn.hoadon.invoice.template;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.hoadon.invoice.template.catalog.*;
import vn.hoadon.invoice.template.storage.DocumentStorage;

import java.sql.*;
import java.util.*;

import javax.sql.DataSource;

final class TestInvoiceFieldCatalog {
    private static final String VERSION = "invoice-xml-2.1.1/catalog-1";

    private record FieldFixture(
            String key,
            String label,
            String xmlPath,
            String bindingSource,
            String group,
            boolean repeated,
            String sourceLength,
            String sourceType,
            String requirementText,
            boolean requiredDisplay) {}

    private TestInvoiceFieldCatalog() {}

    static InvoiceFieldCatalog create(ObjectMapper mapper) {
        return new InvoiceFieldCatalog(snapshot(mapper));
    }

    static InvoiceCatalogSnapshot snapshot(ObjectMapper mapper) {
        try (var input =
                TestInvoiceFieldCatalog.class.getResourceAsStream(
                        "/invoice-template/field-catalog-fixture.json")) {
            var fixtures = mapper.readValue(input, FieldFixture[].class);
            List<InvoiceCatalogSnapshot.Group> groups = new ArrayList<>();
            List<InvoiceCatalogSnapshot.Dataset> datasets = new ArrayList<>();
            List<InvoiceCatalogSnapshot.Field> fields = new ArrayList<>();
            Map<String, String> roots = new LinkedHashMap<>();
            roots.put("document", "/HDon");
            for (var f : fixtures) {
                if (groups.stream().noneMatch(g -> g.code().equals(f.group())))
                    groups.add(
                            new InvoiceCatalogSnapshot.Group(
                                    (long) groups.size() + 1,
                                    f.group(),
                                    f.group(),
                                    groups.size() * 10,
                                    true,
                                    0L));
                if (f.repeated()) {
                    String root = f.xmlPath().substring(0, f.xmlPath().lastIndexOf('/'));
                    roots.merge(
                            f.group(),
                            root,
                            (a, b) -> {
                                while (!b.equals(a) && !b.startsWith(a + "/"))
                                    a = a.substring(0, a.lastIndexOf('/'));
                                return a;
                            });
                }
            }
            datasets.add(
                    new InvoiceCatalogSnapshot.Dataset(
                            1L, "document", "Hóa đơn", null, "ONE", "/HDon", true, 0L));
            for (var entry : roots.entrySet())
                if (!entry.getKey().equals("document")) {
                    String code = entry.getKey(),
                            parent =
                                    code.contains(".")
                                            ? code.substring(0, code.lastIndexOf('.'))
                                            : "document";
                    datasets.add(
                            new InvoiceCatalogSnapshot.Dataset(
                                    (long) datasets.size() + 1,
                                    code,
                                    code,
                                    parent,
                                    "MANY",
                                    entry.getValue().substring(roots.get(parent).length() + 1),
                                    true,
                                    0L));
                }
            for (var f : fixtures) {
                String dataset = f.repeated() ? f.group() : "document",
                        source = "INVOICE".equals(f.bindingSource()) ? "CONTEXT" : "XML";
                String path =
                        source.equals("XML")
                                ? f.xmlPath().substring(roots.get(dataset).length() + 1)
                                : "invoice." + f.xmlPath().substring("invoices.".length());
                String type =
                        f.sourceType() != null && f.sourceType().startsWith("Ngày")
                                ? "DATE"
                                : f.sourceLength() != null
                                                && f.sourceLength().matches("[0-9]+,[0-9]+")
                                        ? "DECIMAL"
                                        : "STRING";
                fields.add(
                        new InvoiceCatalogSnapshot.Field(
                                (long) fields.size() + 1,
                                f.key(),
                                f.label(),
                                f.group(),
                                dataset,
                                f.repeated(),
                                source,
                                path,
                                type,
                                null,
                                "plainText",
                                f.requirementText(),
                                f.requiredDisplay(),
                                true,
                                fields.size() * 10,
                                0L));
            }
            groups.add(
                    new InvoiceCatalogSnapshot.Group(
                            (long) groups.size() + 1,
                            "presentation",
                            "Thông tin bản in",
                            10000,
                            false,
                            0L));
            for (String key :
                    List.of(
                            "sourceStage",
                            "statusLabel",
                            "lookupCode",
                            "authorityCode",
                            "signatureSummary",
                            "sampleMarker"))
                fields.add(
                        new InvoiceCatalogSnapshot.Field(
                                (long) fields.size() + 1,
                                "presentation." + key,
                                key,
                                "presentation",
                                "document",
                                false,
                                "CONTEXT",
                                "document." + key,
                                "STRING",
                                null,
                                "plainText",
                                null,
                                false,
                                false,
                                10000,
                                0L));
            return new InvoiceCatalogSnapshot(VERSION, groups, datasets, fields);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    static void seed(DataSource dataSource, ObjectMapper mapper) {
        try (var c = dataSource.getConnection()) {
            try (var st = c.createStatement();
                    var rs = st.executeQuery("SELECT COUNT(*) FROM invoice_template_fields")) {
                rs.next();
                if (rs.getLong(1) > 0) return;
            }
            var source = snapshot(mapper);
            for (var g : source.groups())
                insert(
                        c,
                        "INSERT INTO"
                            + " invoice_template_field_groups(id,code,label,sort_order,active,lock_version)"
                            + " VALUES(?,?,?,?,?,0)",
                        g.id(),
                        g.code(),
                        g.label(),
                        g.sortOrder(),
                        g.active());
            for (var d : source.datasets())
                insert(
                        c,
                        "INSERT INTO"
                            + " invoice_template_datasets(id,code,label,parent_id,cardinality,root_path,active,lock_version)"
                            + " VALUES(?,?,?,?,?,?,?,0)",
                        d.id(),
                        d.code(),
                        d.label(),
                        d.parent() == null ? null : source.requireDataset(d.parent()).id(),
                        d.cardinality(),
                        d.rootPath(),
                        d.active());
            for (var f : source.fields())
                insert(
                        c,
                        "INSERT INTO"
                            + " invoice_template_fields(id,field_key,label,group_id,dataset_id,source_type,source_path,value_type,default_formatter,description,required_display,active,sort_order,created_at,updated_at,lock_version)"
                            + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)",
                        f.id(),
                        f.key(),
                        f.label(),
                        source.groups().stream()
                                .filter(g -> g.code().equals(f.group()))
                                .findFirst()
                                .orElseThrow()
                                .id(),
                        source.requireDataset(f.dataset()).id(),
                        f.sourceType(),
                        f.sourcePath(),
                        f.valueType(),
                        f.defaultFormatter(),
                        f.description(),
                        f.requiredDisplay(),
                        f.active(),
                        f.sortOrder());
            for (var entry :
                    Map.of(
                                    "invoice_template_fields",
                                    source.fields().size(),
                                    "invoice_template_field_groups",
                                    source.groups().size(),
                                    "invoice_template_datasets",
                                    source.datasets().size())
                            .entrySet())
                try (var st = c.createStatement()) {
                    st.execute(
                            "ALTER TABLE "
                                    + entry.getKey()
                                    + " ALTER COLUMN id RESTART WITH "
                                    + (entry.getValue() + 1));
                }
            String json = mapper.writeValueAsString(source);
            insert(
                    c,
                    "INSERT INTO"
                        + " invoice_template_catalog_releases(code,snapshot_json,content_hash,published_at)"
                        + " VALUES(?,?,?,CURRENT_TIMESTAMP)",
                    source.code(),
                    json,
                    DocumentStorage.sha256(json));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void insert(Connection c, String sql, Object... args) throws Exception {
        try (var s = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
            s.executeUpdate();
        }
    }
}
