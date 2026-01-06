-- Add prioritize column to vat_rates table
-- Date: 2026-01-06
-- Description: Add prioritize field for display ordering of VAT rates

ALTER TABLE [hoadon_database].[dbo].[vat_rates]
ADD [prioritize] INT NOT NULL DEFAULT 0;

-- Update existing records with sequential prioritize values
-- This will set prioritize based on current code order
WITH OrderedVatRates AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY code ASC) - 1 AS row_num
    FROM [hoadon_database].[dbo].[vat_rates]
)
UPDATE vr
SET vr.prioritize = ovr.row_num
FROM [hoadon_database].[dbo].[vat_rates] vr
INNER JOIN OrderedVatRates ovr ON vr.id = ovr.id;
