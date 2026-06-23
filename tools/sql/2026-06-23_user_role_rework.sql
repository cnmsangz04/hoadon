-- Re-map users.role after replacing admin_scope-based role splitting.
-- New mapping:
--   0 = Quản trị viên toàn quyền
--   1 = Quản trị viên hệ thống
--   2 = Quản lý doanh nghiệp
--   3 = Nhân viên doanh nghiệp

BEGIN TRANSACTION;

IF COL_LENGTH('dbo.users', 'admin_scope') IS NOT NULL
BEGIN
    UPDATE [dbo].[users]
    SET [role] = CASE
        WHEN [role] = 0 THEN 0
        WHEN [role] = 1 AND ([admin_scope] = 'ROOT_COMPANY' OR [company_id] = 1) THEN 1
        WHEN [role] = 1 THEN 2
        WHEN [role] = 2 THEN 3
        ELSE [role]
    END;

    UPDATE [dbo].[users]
    SET [admin_password] = NULL
    WHERE [role] NOT IN (0, 1);

    ALTER TABLE [dbo].[users] DROP COLUMN [admin_scope];
END;

-- Quản trị viên hệ thống kế thừa nhóm quyền admin từ admin công ty root cũ.
UPDATE up
SET up.[allowed] = 1
FROM [dbo].[user_permissions] up
JOIN [dbo].[users] u ON u.[id] = up.[user_id]
JOIN [dbo].[permissions] p ON p.[id] = up.[permission_id]
WHERE u.[role] = 1
  AND ISNULL(p.[level], 0) > 0;

INSERT INTO [dbo].[user_permissions] ([user_id], [permission_id], [allowed])
SELECT u.[id], p.[id], 1
FROM [dbo].[users] u
CROSS JOIN [dbo].[permissions] p
WHERE u.[role] = 1
  AND ISNULL(p.[level], 0) > 0
  AND NOT EXISTS (
      SELECT 1
      FROM [dbo].[user_permissions] up
      WHERE up.[user_id] = u.[id]
        AND up.[permission_id] = p.[id]
  );

COMMIT TRANSACTION;
