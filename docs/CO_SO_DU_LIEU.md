# Cơ sở dữ liệu

Cập nhật: 19/06/2026.

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
| Người dùng | `users`, `login_sessions`, `login_histories` | Tài khoản, phiên đăng nhập, lịch sử đăng nhập. |
| Phân quyền | `permission_categories`, `permissions`, `user_permissions` | Nhóm quyền, quyền chi tiết, quyền từng user. |
| Danh mục công ty | `customers`, `products` | Khách hàng và sản phẩm của từng công ty. |
| Hóa đơn và import | `form_invoices`, `invoice_numbers`, `invoices`, `invoice_imports` | Mẫu hóa đơn, dải số, hóa đơn, lịch sử import hóa đơn và import danh mục. |
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
| `daily_invoice_report_configs` | Lưu cấu hình lịch gửi báo cáo ngày ở cấp hệ thống, gồm giờ/phút gửi và lần gửi gần nhất. Bảng hiện không có `company_id`; dữ liệu báo cáo và mail job được tách theo công ty khi gửi. |

## Ghi chú về lịch sử import

`invoice_imports` lưu lịch sử import Excel. Cột `import_type` phân loại dữ liệu:

- `INVOICE` hoặc `NULL` với dữ liệu cũ: import hóa đơn.
- `CUSTOMER`: import danh mục khách hàng.
- `PRODUCT`: import danh mục sản phẩm.

Với import danh mục, `item_count` và `imported_item_ids` lưu số lượng và danh sách ID khách hàng/sản phẩm đã tạo hoặc cập nhật. Với import hóa đơn, `invoice_count` và `imported_invoice_ids` tiếp tục lưu số lượng và danh sách hóa đơn đã tạo.

## Nguyên tắc nhiều công ty

- Mọi dữ liệu nghiệp vụ của công ty thường phải truy vấn theo `company_id`.
- Root chỉ được bỏ lọc công ty ở các màn hình quản trị có chủ đích.
- Khi tạo mail job cho báo cáo ngày, `company_id` phải là công ty nhận nội dung báo cáo để lịch sử không dồn về root.
- Khi lấy XML ký hoặc XML gửi cơ quan thuế, cần kiểm tra cả hóa đơn, công ty và mã tra cứu để tránh trả nhầm XML.
- Cấu hình Telegram và cấu hình lịch báo cáo hóa đơn ngày là hai nghiệp vụ khác nhau, nên lưu ở hai bảng khác nhau: `telegram_configs` và `daily_invoice_report_configs`.
- `daily_invoice_report_configs` hiện là cấu hình lịch gửi chung. Khi gửi báo cáo, hệ thống gom dữ liệu theo từng công ty và tạo `mail_jobs.company_id` theo công ty nhận nội dung.

## Ghi chú về `ddl-auto`

Hiện cấu hình có `spring.jpa.hibernate.ddl-auto=update`. Cấu hình này tiện cho phát triển nhưng khi triển khai thật vẫn nên quản lý thay đổi schema bằng script SQL rõ ràng trong `tools/sql` để tránh thay đổi khó kiểm soát.
