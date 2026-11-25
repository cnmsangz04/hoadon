package vn.hoadon.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Component
public class InvoiceStockIssueSchemaInitializer {
    private final JdbcTemplate jdbc;

    public InvoiceStockIssueSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void initialize() {
        try {
            for (String table : List.of("invoice_imports", "form_invoices", "invoices")) {
                if (!tableExists(table)) continue;
                if ("invoice_imports".equals(table)) addColumn(table, "category", "int");
                addColumn(table, "stock_issue_type", "int");
            }
            if (tableExists("error_messages")) {
                addColumn("error_messages", "category", "int");
                jdbc.update("update error_messages set category = 1 where category is null");
            }
            if (tableExists("invoice_imports") && columnExists("invoice_imports", "category")) {
                jdbc.update("update invoice_imports set category = 1 where category is null");
            }
            if (tableExists("form_invoices") && columnExists("form_invoices", "category") && columnExists("form_invoices", "type")) {
                ensureFormTypeColumn();
                jdbc.update("update form_invoices set type = -1 where category = 2 and type = 0");
                jdbc.update("update form_invoices set type = 0 where category = 6 and type = -1");
            }
            if (tableExists("invoices") && tableExists("form_invoices")
                    && columnExists("invoices", "stock_issue_type") && columnExists("form_invoices", "stock_issue_type")) {
                if (isSqlServer()) {
                    jdbc.update("update i set stock_issue_type = f.stock_issue_type from invoices i join form_invoices f on f.id = i.form_id where i.stock_issue_type is null and f.category = 6");
                } else {
                    jdbc.update("update invoices set stock_issue_type = (select f.stock_issue_type from form_invoices f where f.id = invoices.form_id and f.category = 6) where stock_issue_type is null");
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể cập nhật cấu trúc dữ liệu phiếu xuất kho", ex);
        }
    }

    private void addColumn(String table, String column, String type) {
        if (!columnExists(table, column)) {
            jdbc.execute("alter table " + table + " add " + column + " " + type + " null");
        }
    }

    private boolean tableExists(String table) {
        return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet result = metadata.getTables(null, null, null, new String[]{"TABLE"})) {
                while (result.next()) {
                    if (table.equalsIgnoreCase(result.getString("TABLE_NAME"))) return true;
                }
            }
            return false;
        }));
    }

    private boolean columnExists(String table, String column) {
        return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet result = metadata.getColumns(null, null, null, null)) {
                while (result.next()) {
                    if (table.equalsIgnoreCase(result.getString("TABLE_NAME"))
                            && column.equalsIgnoreCase(result.getString("COLUMN_NAME"))) return true;
                }
            }
            return false;
        }));
    }

    private void ensureFormTypeColumn() {
        Integer dataType = jdbc.execute((ConnectionCallback<Integer>) connection -> {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet result = metadata.getColumns(null, null, "form_invoices", "type")) {
                return result.next() ? result.getInt("DATA_TYPE") : null;
            }
        });
        if (dataType == null || dataType != Types.TINYINT) return;
        if (isSqlServer()) {
            Map<String, Object> defaultConstraint = jdbc.query(
                    "select dc.name, dc.definition "
                            + "from sys.default_constraints dc "
                            + "join sys.columns c on c.object_id = dc.parent_object_id "
                            + "and c.column_id = dc.parent_column_id "
                            + "where dc.parent_object_id = object_id('form_invoices') "
                            + "and c.name = 'type'",
                    result -> result.next()
                            ? Map.of("name", result.getString("name"), "definition", result.getString("definition"))
                            : null);
            if (defaultConstraint != null) {
                String constraintName = String.valueOf(defaultConstraint.get("name")).replace("]", "]]");
                jdbc.execute("alter table form_invoices drop constraint [" + constraintName + "]");
            }
            jdbc.execute("alter table form_invoices alter column type smallint null");
            if (defaultConstraint != null) {
                String constraintName = String.valueOf(defaultConstraint.get("name")).replace("]", "]]");
                String definition = String.valueOf(defaultConstraint.get("definition"));
                jdbc.execute("alter table form_invoices add constraint [" + constraintName
                        + "] default " + definition + " for type");
            }
        } else {
            jdbc.execute("alter table form_invoices alter column type smallint");
        }
    }

    private boolean isSqlServer() {
        return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection ->
                connection.getMetaData().getDatabaseProductName().toLowerCase().contains("sql server")));
    }
}
