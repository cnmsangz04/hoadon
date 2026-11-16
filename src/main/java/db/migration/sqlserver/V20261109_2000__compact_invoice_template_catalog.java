package db.migration.sqlserver;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import vn.hoadon.invoice.template.migration.CompactCatalogMigration;

public class V20261109_2000__compact_invoice_template_catalog extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        CompactCatalogMigration.migrate(context.getConnection());
    }
}
