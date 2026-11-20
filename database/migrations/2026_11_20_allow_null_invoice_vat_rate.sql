/* Hóa đơn đã bao gồm thuế (form_invoices.type = 0) lưu vat_rate = NULL. */
IF COL_LENGTH(N'dbo.invoices', N'vat_rate') IS NOT NULL
BEGIN
    DECLARE @default_constraint sysname, @sql nvarchar(max);

    SELECT @default_constraint = dc.name
    FROM sys.default_constraints dc
    INNER JOIN sys.columns col
        ON col.default_object_id = dc.object_id
    INNER JOIN sys.tables tbl
        ON tbl.object_id = col.object_id
    INNER JOIN sys.schemas sch
        ON sch.schema_id = tbl.schema_id
    WHERE sch.name = N'dbo'
      AND tbl.name = N'invoices'
      AND col.name = N'vat_rate';

    IF @default_constraint IS NOT NULL
    BEGIN
        SET @sql = N'ALTER TABLE dbo.invoices DROP CONSTRAINT ' + QUOTENAME(@default_constraint);
        EXEC(@sql);
    END;

    ALTER TABLE dbo.invoices ALTER COLUMN vat_rate smallint NULL;
    ALTER TABLE dbo.invoices ADD CONSTRAINT DF_invoices_vat_rate DEFAULT (-1) FOR vat_rate;
END;
