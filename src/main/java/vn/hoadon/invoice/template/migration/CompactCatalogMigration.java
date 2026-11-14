package vn.hoadon.invoice.template.migration;

import static vn.hoadon.invoice.template.migration.CatalogSchemaMigration.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import vn.hoadon.invoice.template.catalog.InvoiceCatalogSnapshot;
import vn.hoadon.invoice.template.catalog.InvoiceCatalogValidator;
import vn.hoadon.invoice.template.storage.DocumentStorage;

import java.sql.Connection;
import java.util.*;

/** Converts the intermediate catalog schema without changing field keys or template references. */
public final class CompactCatalogMigration {
    private CompactCatalogMigration() {}

    public static void migrate(Connection connection) throws Exception {
        if (!column(connection, "invoice_template_fields", "status")) return;
        var mapper = new ObjectMapper();
        Map<Long, String> snapshots = new LinkedHashMap<>();
        try (var statement = connection.createStatement();
                var rows =
                        statement.executeQuery(
                                "SELECT id,snapshot_json FROM invoice_template_catalog_releases")) {
            while (rows.next())
                snapshots.put(rows.getLong(1), convertSnapshot(rows.getString(2), mapper));
        }
        for (String column :
                List.of(
                        "source_type VARCHAR(20)",
                        "source_path NVARCHAR(700)",
                        "active BIT",
                        "required_display BIT")) {
            String name = column.substring(0, column.indexOf(' '));
            if (!column(connection, "invoice_template_fields", name))
                execute(connection, "ALTER TABLE invoice_template_fields ADD " + column + " NULL");
        }
        try (var statement = connection.createStatement();
                var rows =
                        statement.executeQuery(
                                "SELECT field_id,COUNT(*) FROM invoice_template_field_bindings"
                                    + " GROUP BY field_id HAVING COUNT(*)<>1")) {
            if (rows.next())
                throw new IllegalStateException(
                        "Phần tử có nhiều nguồn dữ liệu; cần xác định nguồn trước khi chuyển đổi");
        }
        execute(
                connection,
                "UPDATE f SET f.source_type=b.source_type,f.source_path=b.selector,f.active=CASE"
                    + " WHEN f.status='ACTIVE' THEN 1 ELSE 0 END,f.required_display=CASE WHEN"
                    + " EXISTS(SELECT 1 FROM invoice_template_field_rules r WHERE r.field_id=f.id"
                    + " AND r.active=1 AND r.rule_type='DISPLAY_REQUIRED') THEN 1 ELSE 0 END FROM"
                    + " invoice_template_fields f JOIN invoice_template_field_bindings b ON"
                    + " b.field_id=f.id");
        for (String column :
                List.of(
                        "source_type VARCHAR(20)",
                        "source_path NVARCHAR(700)",
                        "active BIT",
                        "required_display BIT"))
            execute(
                    connection,
                    "ALTER TABLE invoice_template_fields ALTER COLUMN " + column + " NOT NULL");

        // Only the representation changes here. Existing catalog codes and field values stay
        // intact.
        execute(
                connection,
                "DISABLE TRIGGER TR_invoice_template_catalog_immutable ON"
                    + " invoice_template_catalog_releases");
        for (var entry : snapshots.entrySet())
            update(
                    connection,
                    "UPDATE invoice_template_catalog_releases SET snapshot_json=?,content_hash=?"
                        + " WHERE id=?",
                    entry.getValue(),
                    DocumentStorage.sha256(entry.getValue()),
                    entry.getKey());
        execute(
                connection,
                "ENABLE TRIGGER TR_invoice_template_catalog_immutable ON"
                    + " invoice_template_catalog_releases");

        for (String table :
                List.of(
                        "invoice_template_field_usages",
                        "invoice_template_field_bindings",
                        "invoice_template_field_rules"))
            if (table(connection, table)) execute(connection, "DROP TABLE " + table);
        execute(
                connection,
                "ALTER TABLE invoice_template_fields DROP CONSTRAINT CK_template_field_type");
        execute(connection, "DROP INDEX IX_template_field_list ON invoice_template_fields");
        try (var statement = connection.createStatement();
                var rows =
                        statement.executeQuery(
                                "SELECT dc.name FROM sys.default_constraints dc JOIN sys.columns"
                                    + " col ON col.object_id=dc.parent_object_id AND"
                                    + " col.column_id=dc.parent_column_id WHERE"
                                    + " dc.parent_object_id=OBJECT_ID('invoice_template_fields')"
                                    + " AND col.name IN"
                                    + " ('status','numeric_precision','numeric_scale')")) {
            List<String> constraints = new ArrayList<>();
            while (rows.next()) constraints.add(rows.getString(1));
            for (String constraint : constraints)
                execute(
                        connection,
                        "ALTER TABLE invoice_template_fields DROP CONSTRAINT ["
                                + constraint.replace("]", "]]")
                                + "]");
        }
        for (String column : List.of("status", "numeric_precision", "numeric_scale"))
            if (column(connection, "invoice_template_fields", column))
                execute(connection, "ALTER TABLE invoice_template_fields DROP COLUMN " + column);
        execute(
                connection,
                "ALTER TABLE invoice_template_fields ADD CONSTRAINT CK_compact_field_type"
                    + " CHECK(source_type IN ('XML','CONTEXT') AND value_type IN"
                    + " ('STRING','INTEGER','DECIMAL','DATE','BOOLEAN') AND (max_length IS NULL OR"
                    + " max_length>0))");
        execute(
                connection,
                "CREATE INDEX IX_compact_field_list ON"
                    + " invoice_template_fields(active,group_id,sort_order,id)");
    }

    public static String convertSnapshot(String json, ObjectMapper mapper) throws Exception {
        ObjectNode source = (ObjectNode) mapper.readTree(json);
        if (!source.has("bindings")) return json;
        Map<String, JsonNode> bindings = new HashMap<>();
        Set<String> required = new HashSet<>();
        for (var binding : source.path("bindings")) {
            String key = binding.path("fieldKey").asText();
            if (!"ANY".equals(binding.path("sourceStage").asText())
                    || bindings.put(key, binding) != null)
                throw new IllegalStateException(
                        "Không thể tự thu gọn nguồn dữ liệu có điều kiện: " + key);
        }
        for (var rule : source.path("rules")) {
            if (!rule.path("active").asBoolean()) continue;
            if (!"DISPLAY_REQUIRED".equals(rule.path("ruleType").asText())
                    || !rule.path("invoiceCategory").isNull()
                    || !rule.path("taxMode").isNull()
                    || !rule.path("sourceStage").isNull()
                    || !rule.path("condition").isEmpty()
                    || !rule.path("params").isEmpty())
                throw new IllegalStateException("Không thể tự thu gọn quy tắc có điều kiện");
            required.add(rule.path("fieldKey").asText());
        }
        for (var node : source.path("fields")) {
            var field = (ObjectNode) node;
            String key = field.path("key").asText();
            var binding = bindings.get(key);
            if (binding == null)
                throw new IllegalStateException("Phần tử thiếu nguồn dữ liệu: " + key);
            field.put("sourceType", binding.path("sourceType").asText());
            field.put("sourcePath", binding.path("selector").asText());
            field.put("active", "ACTIVE".equals(field.path("status").asText()));
            field.put("requiredDisplay", required.contains(key));
            String dataset = field.path("dataset").asText();
            boolean repeated = false;
            for (var candidate : source.path("datasets"))
                if (dataset.equals(candidate.path("code").asText()))
                    repeated = "MANY".equals(candidate.path("cardinality").asText());
            field.put("repeated", repeated);
            field.remove(List.of("status", "numericPrecision", "numericScale"));
        }
        source.remove(List.of("bindings", "rules"));
        InvoiceCatalogValidator.validate(mapper.treeToValue(source, InvoiceCatalogSnapshot.class));
        return mapper.writeValueAsString(source);
    }
}
