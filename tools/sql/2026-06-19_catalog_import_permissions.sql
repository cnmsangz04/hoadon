-- Add customer/product catalog import permissions
-- Target: SQL Server

DECLARE @CategoryId BIGINT;
DECLARE @OrderIndex INT;

SELECT @OrderIndex = ISNULL(MAX(ISNULL(sothutu, 0)), 0) + 1
FROM dbo.permission_categories;

IF NOT EXISTS (
  SELECT 1 FROM dbo.permission_categories WHERE name = N'Import dữ liệu'
)
BEGIN
  INSERT INTO dbo.permission_categories (name, sothutu, status, created_at, updated_at)
  VALUES (N'Import dữ liệu', @OrderIndex, 1, SYSDATETIME(), SYSDATETIME());
END
ELSE
BEGIN
  UPDATE dbo.permission_categories
  SET status = 1,
      updated_at = SYSDATETIME()
  WHERE name = N'Import dữ liệu'
    AND (status IS NULL OR status <> 1);
END

SELECT @CategoryId = id
FROM dbo.permission_categories
WHERE name = N'Import dữ liệu';

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE name = 'import-customer-list')
BEGIN
  INSERT INTO dbo.permissions (name, display_name, level, category, description, status, created_at, updated_at)
  VALUES ('import-customer-list', N'Xem import khách hàng', 0, @CategoryId, N'Xem lịch sử import danh mục khách hàng', 1, SYSDATETIME(), SYSDATETIME());
END
ELSE
BEGIN
  UPDATE dbo.permissions
  SET display_name = N'Xem import khách hàng',
      level = 0,
      category = @CategoryId,
      description = N'Xem lịch sử import danh mục khách hàng',
      status = 1,
      updated_at = SYSDATETIME()
  WHERE name = 'import-customer-list';
END

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE name = 'import-customer-save')
BEGIN
  INSERT INTO dbo.permissions (name, display_name, level, category, description, status, created_at, updated_at)
  VALUES ('import-customer-save', N'Import khách hàng', 0, @CategoryId, N'Tải mẫu, upload và import lại danh mục khách hàng', 1, SYSDATETIME(), SYSDATETIME());
END
ELSE
BEGIN
  UPDATE dbo.permissions
  SET display_name = N'Import khách hàng',
      level = 0,
      category = @CategoryId,
      description = N'Tải mẫu, upload và import lại danh mục khách hàng',
      status = 1,
      updated_at = SYSDATETIME()
  WHERE name = 'import-customer-save';
END

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE name = 'import-product-list')
BEGIN
  INSERT INTO dbo.permissions (name, display_name, level, category, description, status, created_at, updated_at)
  VALUES ('import-product-list', N'Xem import sản phẩm', 0, @CategoryId, N'Xem lịch sử import danh mục sản phẩm', 1, SYSDATETIME(), SYSDATETIME());
END
ELSE
BEGIN
  UPDATE dbo.permissions
  SET display_name = N'Xem import sản phẩm',
      level = 0,
      category = @CategoryId,
      description = N'Xem lịch sử import danh mục sản phẩm',
      status = 1,
      updated_at = SYSDATETIME()
  WHERE name = 'import-product-list';
END

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE name = 'import-product-save')
BEGIN
  INSERT INTO dbo.permissions (name, display_name, level, category, description, status, created_at, updated_at)
  VALUES ('import-product-save', N'Import sản phẩm', 0, @CategoryId, N'Tải mẫu, upload và import lại danh mục sản phẩm', 1, SYSDATETIME(), SYSDATETIME());
END
ELSE
BEGIN
  UPDATE dbo.permissions
  SET display_name = N'Import sản phẩm',
      level = 0,
      category = @CategoryId,
      description = N'Tải mẫu, upload và import lại danh mục sản phẩm',
      status = 1,
      updated_at = SYSDATETIME()
  WHERE name = 'import-product-save';
END
