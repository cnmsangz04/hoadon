package db.migration.h2;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import vn.hoadon.invoice.template.migration.CatalogSchemaMigration;

public class V20261109_1900__normalize_invoice_template_catalog extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        CatalogSchemaMigration.migrate(context.getConnection());
    }
}
