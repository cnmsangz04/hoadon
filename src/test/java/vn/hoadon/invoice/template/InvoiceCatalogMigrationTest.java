package vn.hoadon.invoice.template;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import vn.hoadon.invoice.template.catalog.*;
import vn.hoadon.invoice.template.storage.DocumentStorage;

import java.sql.DriverManager;
import java.util.*;

class InvoiceCatalogMigrationTest {
    @Test
    void keepsFieldKeysAndPinsTheExistingCatalog() throws Exception {
        String url =
                "jdbc:h2:mem:catalog_migration_"
                        + UUID.randomUUID()
                        + ";MODE=MSSQLServer;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration/h2")
                .load()
                .migrate();
        try (var c = DriverManager.getConnection(url, "sa", "");
                var st = c.createStatement()) {
            try (var rs = st.executeQuery("SELECT COUNT(*) FROM invoice_template_fields")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(98);
            }
            try (var rs =
                    st.executeQuery(
                            "SELECT snapshot_json,content_hash FROM"
                                + " invoice_template_catalog_releases")) {
                rs.next();
                String json = rs.getString(1);
                assertThat(DocumentStorage.sha256(json)).isEqualTo(rs.getString(2));
                var snapshot = new ObjectMapper().readValue(json, InvoiceCatalogSnapshot.class);
                assertThat(snapshot.requireField("invoice.lookupCode").sourcePath())
                        .isEqualTo("invoice.lookup_code");
                var data =
                        new InvoiceViewModelMapper(new InvoiceFieldCatalog(snapshot))
                                .map(
                                        "<HDon><DLHDon><TTChung><SHDon>0001</SHDon></TTChung></DLHDon></HDon>",
                                        Map.of("lookup_code", "TEST"),
                                        snapshot);
                assertThat(data.get("common.SHDon")).isEqualTo("0001");
                assertThat(data.get("invoice.lookupCode")).isEqualTo("TEST");
                assertThat((List<?>) data.get("items")).isEmpty();
            }
            try (var rs =
                    c.getMetaData().getColumns(null, null, "INVOICE_TEMPLATE_FIELDS", "XML_PATH")) {
                assertThat(rs.next()).isFalse();
            }
        }
    }
}
