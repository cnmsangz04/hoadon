package vn.hoadon.invoice.template;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.UUID;

@EnabledIfSystemProperty(named = "invoice.catalog.sqlserver-tests", matches = "true")
class InvoiceCatalogSqlServerMigrationTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void upgradesCoreSchemaAndExistingJsonSchemaWithoutReintroducingLegacyColumns(
            boolean existingJsonSchema) throws Exception {
        String url = System.getenv("INVOICE_CATALOG_TEST_JDBC_URL");
        String username = System.getenv("INVOICE_CATALOG_TEST_USERNAME");
        String password = System.getenv("INVOICE_CATALOG_TEST_PASSWORD");
        assertThat(url)
                .matches("(?i)jdbc:sqlserver://(?:localhost|127\\.0\\.0\\.1)(?::[0-9]+)?(?:;.*)?");
        String database = "invoice_catalog_test_" + UUID.randomUUID().toString().replace("-", "");
        String temporaryUrl =
                url.replaceAll("(?i);databaseName=[^;]*", "") + ";databaseName=" + database;
        try (var admin = DriverManager.getConnection(url, username, password);
                var statement = admin.createStatement()) {
            admin.setCatalog("master");
            statement.execute("CREATE DATABASE [" + database + "]");
            try {
                admin.setCatalog(database);
                statement.execute(
                        "CREATE TABLE dbo.form_invoices(id BIGINT PRIMARY KEY,status INT NULL)");
                statement.execute("CREATE TABLE dbo.invoices(id BIGINT PRIMARY KEY)");
                if (existingJsonSchema) {
                    configuration(temporaryUrl, username, password)
                            .target("20261109.1700")
                            .load()
                            .migrate();
                    try (var schema =
                            getClass()
                                    .getResourceAsStream(
                                            "/db/schema/invoice_template_schema.sql")) {
                        statement.execute(
                                new String(schema.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
                var flyway = configuration(temporaryUrl, username, password).load();
                assertThat(flyway.migrate().migrationsExecuted).isGreaterThan(0);
                flyway.validate();
                assertThat(flyway.migrate().migrationsExecuted).isZero();
                try (var rows =
                        statement.executeQuery(
                                "SELECT COUNT(*) FROM dbo.invoice_template_fields")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isEqualTo(98);
                }
                try (var rows =
                        statement.executeQuery(
                                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME"
                                    + " IN ('invoice_template_drafts','invoice_template_versions')"
                                    + " AND COLUMN_NAME='source_kind'")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isZero();
                }
            } finally {
                admin.setCatalog("master");
                statement.execute(
                        "ALTER DATABASE ["
                                + database
                                + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                statement.execute("DROP DATABASE [" + database + "]");
            }
        }
    }

    private org.flywaydb.core.api.configuration.FluentConfiguration configuration(
            String url, String username, String password) {
        return Flyway.configure()
                .dataSource(url, username, password)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .locations("classpath:db/migration/sqlserver");
    }
}
