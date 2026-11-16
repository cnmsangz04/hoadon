package db.migration.sqlserver;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.nio.charset.StandardCharsets;

public class V20261109_1850__ensure_invoice_template_schema extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (var check = context.getConnection().createStatement();
                var rows = check.executeQuery("SELECT OBJECT_ID('dbo.invoice_templates')")) {
            rows.next();
            if (rows.getObject(1) != null) return;
        }
        try (var input = getClass().getResourceAsStream("/db/schema/invoice_template_schema.sql")) {
            if (input == null) throw new IllegalStateException("Không tìm thấy schema mẫu hóa đơn");
            try (var statement = context.getConnection().createStatement()) {
                statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }
}
