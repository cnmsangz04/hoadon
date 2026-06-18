# Cấu trúc thư mục chương trình

Cập nhật: 18/06/2026.

## Thư mục gốc

| Đường dẫn | Nội dung |
| --- | --- |
| `src/main/java/vn/hoadon` | Mã nguồn backend Spring Boot. |
| `src/main/resources` | Cấu hình backend và static sau khi build frontend. |
| `src/views` | Màn hình Vue. |
| `src/router` | Cấu hình route frontend. |
| `src/assets` | Tài nguyên giao diện. |
| `src/plugins` | Plugin frontend dùng chung. |
| `src/utils` | Hàm tiện ích frontend. |
| `public` | File public của Vue. |
| `uploads` | File upload trong lúc chạy chương trình. |
| `logs` | File log runtime. |
| `tools/sql` | Script SQL thủ công hoặc dữ liệu nền. |
| `docs` | Tài liệu kỹ thuật bổ sung cho dự án. |
| `db.dbml`, `db.dbdiagram` | Mô tả database bằng DBML/dbdiagram. |
| `pom.xml` | Cấu hình Maven và dependency backend. |
| `package.json` | Cấu hình npm, script và dependency frontend. |

## Backend

| Đường dẫn | Nội dung |
| --- | --- |
| `auth` | Lớp hỗ trợ lấy thông tin user hiện tại và quyền truy cập. |
| `config` | Cấu hình web, Jackson, thanh toán, phân trang. |
| `controllers` | Controller chung, public, admin, customer và setting. |
| `controllers/admin` | API khu quản trị hệ thống. |
| `controllers/customers` | API nghiệp vụ của công ty. |
| `controllers/setting` | API cấu hình công ty, tài khoản, thành viên. |
| `dto` | DTO dùng ở các luồng cần dữ liệu trung gian. |
| `entity` | Entity ánh xạ bảng database. |
| `exception` | Xử lý lỗi toàn cục. |
| `messaging` | Message nội bộ cho hàng đợi mail. |
| `repositories` | Repository truy vấn database. |
| `security` | JWT, filter và cấu hình Spring Security. |
| `services` | Interface service nghiệp vụ. |
| `services/impl` | Xử lý nghiệp vụ cụ thể. |
| `util` | Tiện ích sinh XML, mã hóa, upload, IP, mail hệ thống. |
| `worker` | Worker xử lý tác vụ nền. |

## Frontend

| Đường dẫn | Nội dung |
| --- | --- |
| `src/views/auth` | Đăng nhập, đăng ký, quên mật khẩu. |
| `src/views/customers` | Dashboard, hóa đơn, báo cáo, danh mục, mua gói. |
| `src/views/administrators` | Quản trị công ty, gói hóa đơn, danh mục nền, mail, Telegram, quyền. |
| `src/views/settings` | Hồ sơ công ty, thành viên, bảo mật IP, lịch sử đăng nhập. |
| `src/views/public` | Trang công khai như tra cứu hóa đơn. |
| `src/router/index.js` | Route chính và guard người dùng. |
| `src/router/modules/administrator.js` | Route khu quản trị. |
| `src/router/modules/setting.js` | Route phần cài đặt. |
| `src/plugins` | Axios, thông báo, xác thực hoặc plugin giao diện dùng chung. |
| `src/utils` | Hàm tiện ích frontend. |

## Quy ước khi thêm file

- Màn hình khách hàng đặt dưới `src/views/customers`.
- Màn hình quản trị hệ thống đặt dưới `src/views/administrators`.
- API nghiệp vụ công ty đặt dưới `controllers/customers`.
- API cấu hình tài khoản/công ty đặt dưới `controllers/setting`.
- API quản trị hệ thống đặt dưới `controllers/admin`.
- Entity, repository, service và service impl nên đặt tên cùng nghiệp vụ để dễ tra cứu.
- Script SQL bổ sung nên đặt trong `tools/sql` với ngày và mục đích rõ ràng.

