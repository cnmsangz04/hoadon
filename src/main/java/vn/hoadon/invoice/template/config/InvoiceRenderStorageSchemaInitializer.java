package vn.hoadon.invoice.template.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component
public class InvoiceRenderStorageSchemaInitializer {
    private final JdbcTemplate jdbc;

    public InvoiceRenderStorageSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void initialize() {
        try {
            try (var connection = jdbc.getDataSource().getConnection()) {
                if (!"Microsoft SQL Server".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) return;
            }
            replaceConstraint("invoice_render_jobs", "CK_invoice_render_jobs_state",
                    "(([state]='CANCELLED' OR [state]='FAILED' OR [state]='SUCCEEDED' OR [state]='RUNNING' OR [state]='QUEUED') AND [attempt_count]>=(0) AND isjson([manifest_json])=(1) AND isjson([parameters_json])=(1) AND ([scope]='SYSTEM' OR [scope]='COMPANY' AND [company_id] IS NOT NULL))");
            replaceConstraint("invoice_render_artifacts", "CK_invoice_render_artifacts_state",
                    "([page_count]>(0) AND [byte_size]>(0) AND ([scope]='SYSTEM' OR [scope]='COMPANY' AND [company_id] IS NOT NULL))");
            allowRenderArtifactDeletion();
            allowAssetDeletion();
        } catch (Exception ignored) {
        }
    }

    private void allowRenderArtifactDeletion() {
        String trigger = jdbc.query("select object_definition(object_id) from sys.triggers where name = ?",
                rs -> rs.next() ? rs.getString(1) : null, "TR_invoice_render_artifacts_immutable");
        if (trigger == null || trigger.toUpperCase(java.util.Locale.ROOT).contains("AFTER UPDATE, DELETE")) {
            jdbc.execute("if object_id('dbo.TR_invoice_render_artifacts_immutable', 'TR') is not null " +
                    "drop trigger dbo.TR_invoice_render_artifacts_immutable");
            jdbc.execute("create trigger dbo.TR_invoice_render_artifacts_immutable on dbo.invoice_render_artifacts after update as " +
                    "begin set nocount on; if exists (select id, job_id, scope, company_id, storage_key, sha256, byte_size, page_count, input_hash, profile_version, created_at from deleted except select id, job_id, scope, company_id, storage_key, sha256, byte_size, page_count, input_hash, profile_version, created_at from inserted) throw 51001, 'Published invoice document data is immutable.', 1; end");
        }
    }

    private void allowAssetDeletion() {
        String trigger = jdbc.query("select object_definition(object_id) from sys.triggers where name = ?",
                rs -> rs.next() ? rs.getString(1) : null, "TR_invoice_assets_immutable");
        if (trigger != null && trigger.toUpperCase(java.util.Locale.ROOT).contains("AFTER UPDATE, DELETE")) {
            jdbc.execute("drop trigger dbo.TR_invoice_assets_immutable");
            jdbc.execute("create trigger dbo.TR_invoice_assets_immutable on dbo.invoice_assets after update as " +
                    "begin set nocount on; if exists (select id, scope, company_id, kind, storage_key, mime_type, sha256, byte_size, metadata_json, created_by, created_at from deleted except select id, scope, company_id, kind, storage_key, mime_type, sha256, byte_size, metadata_json, created_by, created_at from inserted) throw 51001, 'Published invoice document data is immutable.', 1; end");
        }
    }

    private void replaceConstraint(String table, String constraint, String definition) throws SQLException {
        String current = jdbc.query("select definition from sys.check_constraints where name = ?",
                rs -> rs.next() ? rs.getString(1) : null, constraint);
        if (current != null && !current.contains("[company_id] IS NULL")) return;
        if (current != null) {
            jdbc.execute("alter table " + table + " drop constraint " + constraint);
        }
        jdbc.execute("alter table " + table + " add constraint " + constraint + " check " + definition);
    }
}
