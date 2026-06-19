-- Add catalog import metadata columns to invoice_imports
-- Target: SQL Server

IF COL_LENGTH('dbo.invoice_imports', 'import_type') IS NULL
BEGIN
  ALTER TABLE dbo.invoice_imports ADD import_type VARCHAR(30) NULL;
END

IF COL_LENGTH('dbo.invoice_imports', 'item_count') IS NULL
BEGIN
  ALTER TABLE dbo.invoice_imports ADD item_count INT NULL;
END

IF COL_LENGTH('dbo.invoice_imports', 'imported_item_ids') IS NULL
BEGIN
  ALTER TABLE dbo.invoice_imports ADD imported_item_ids NVARCHAR(MAX) NULL;
END

UPDATE dbo.invoice_imports
SET import_type = 'INVOICE',
    updated_at = SYSDATETIME()
WHERE import_type IS NULL;
