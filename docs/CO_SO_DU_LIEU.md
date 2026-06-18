# Cơ sở dữ liệu

Cập nhật: 18/06/2026.

## Nguồn mô tả schema

Database được mô tả chính trong:

- `db.dbml`: định nghĩa bảng, cột và quan hệ bằng DBML.
- `db.dbdiagram`: file đang dùng để xem hoặc chỉnh schema theo dbdiagram.
- `tools/sql`: script SQL bổ sung dữ liệu hoặc chỉnh schema thủ công.

Cấu hình hiện tại trong `application.properties` kết nối SQL Server database `hoadon_database` ở `localhost:1433`.

## Nhóm bảng chính

| Nhóm | Bảng tiêu biểu | Ý nghĩa |
| --- | --- | --- |
| Danh mục nền | `banks`, `tax_authorities`, `provinces`, `vat_rates` | Dữ liệu dùng chung toàn hệ thống. |
| Công ty | `companies`, `company_banks`, `company_allowed_ips`, `legal_representatives` | Hồ sơ công ty, tài khoản ngân hàng, bảo mật IP, người đại diện. |
| Đăng ký | `company_registration_requests`, `register_invoices` | Đăng ký công ty và tờ khai đăng ký sử dụng hóa đơn. |
| Người dùng | `users`, `login_sessions`, `login_history` | Tài khoản, phiên đăng nhập, lịch sử đăng nhập. |
| Phân quyền | `permission_categories`, `permissions`, `user_permissions` | Nhóm quyền, quyền chi tiết, quyền từng user. |
| Danh mục công ty | `customers`, `products` | Khách hàng và sản phẩm của từng công ty. |
| Hóa đơn | `form_invoices`, `invoice_numbers`, `invoices`, `invoice_imports` | Mẫu hóa đơn, dải số, hóa đơn, dữ liệu import. |
| Chữ ký và XML | `signature_vats`, `signature_authorities_tax` | XML hóa đơn đã ký và XML gửi/nhận cơ quan thuế. |
| Mail | `mail_servers`, `mail_templates`, `mail_jobs` | Cấu hình SMTP, mẫu mail, hàng đợi và lịch sử gửi mail. |
| Báo cáo và thông báo | `telegram_configs`, `daily_invoice_report_configs`, `notification_reads`, `history` | Cấu hình Telegram, lịch báo cáo ngày, thông báo đã đọc, lịch sử thao tác. |
| Gói hóa đơn | `invoice_packages`, `invoice_package_purchases`, `buy_invoices`, `buy_invoice_histories` | Gói bán, giao dịch mua, hạn mức hóa đơn và lịch sử. |

## Quan hệ dữ liệu quan trọng

| Quan hệ | Ghi chú |
| --- | --- |
| `users.company_id -> companies.id` | Mỗi user thuộc một công ty. |
| `customers.company_id -> companies.id` | Khách hàng phải nằm trong công ty sở hữu. |
| `products.company_id -> companies.id` | Sản phẩm phải nằm trong công ty sở hữu. |
| `form_invoices.company_id -> companies.id` | Mẫu hóa đơn thuộc công ty. |
| `invoice_numbers.company_id -> companies.id` | Dải số thuộc công ty. |
| `invoices.company_id -> companies.id` | Hóa đơn thuộc công ty phát hành. |
| `invoices.form_invoice_id -> form_invoices.id` | Hóa đơn dùng một mẫu hóa đơn cụ thể. |
| `signature_vats.invoice_id -> invoices.id` | XML ký của hóa đơn. Cần lọc thêm theo công ty khi truy vấn nghiệp vụ. |
| `signature_authorities_tax.invoice_id -> invoices.id` | XML gửi cơ quan thuế của hóa đơn. Cần lọc thêm theo công ty khi truy vấn nghiệp vụ. |
| `mail_jobs.company_id -> companies.id` | Lịch sử/job mail phải nằm ở công ty phát sinh nội dung. |
| `daily_invoice_report_configs.company_id -> companies.id` | Lịch gửi báo cáo ngày theo từng công ty cấu hình. |

## Nguyên tắc nhiều công ty

- Mọi dữ liệu nghiệp vụ của công ty thường phải truy vấn theo `company_id`.
- Root chỉ được bỏ lọc công ty ở các màn hình quản trị có chủ đích.
- Khi tạo mail job cho báo cáo ngày, `company_id` phải là công ty nhận nội dung báo cáo để lịch sử không dồn về root.
- Khi lấy XML ký hoặc XML gửi cơ quan thuế, cần kiểm tra cả hóa đơn, công ty và mã tra cứu để tránh trả nhầm XML.
- Cấu hình Telegram và cấu hình lịch báo cáo hóa đơn ngày là hai nghiệp vụ khác nhau, nên lưu ở hai bảng khác nhau: `telegram_configs` và `daily_invoice_report_configs`.

## Ghi chú về `ddl-auto`

Hiện cấu hình có `spring.jpa.hibernate.ddl-auto=update`. Cấu hình này tiện cho phát triển nhưng khi triển khai thật vẫn nên quản lý thay đổi schema bằng script SQL rõ ràng trong `tools/sql` để tránh thay đổi khó kiểm soát.

