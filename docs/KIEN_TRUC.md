# Kiến trúc chương trình

Cập nhật: 18/06/2026.

## Bức tranh tổng thể

Chương trình là ứng dụng web gồm ba phần chính:

| Thành phần | Vai trò |
| --- | --- |
| Frontend Vue 2 | Giao diện người dùng, gọi REST API, điều hướng theo token và quyền. |
| Backend Spring Boot | Xử lý nghiệp vụ, xác thực, phân quyền, kết nối database, gửi mail, sinh XML/PDF. |
| SQL Server | Lưu dữ liệu công ty, người dùng, hóa đơn, mẫu, mail, lịch sử, cấu hình. |

Khi phát triển, frontend thường chạy ở `http://localhost:8080`, backend chạy ở `http://localhost:8081`. Khi build production, frontend được build vào `src/main/resources/static` để backend có thể phục vụ file tĩnh.

## Mô hình kiến trúc

Chương trình có thể mô tả theo ba góc nhìn sau:

| Góc nhìn | Mô hình | Giải thích |
| --- | --- | --- |
| Triển khai tổng thể | 3 tầng | Frontend Vue là tầng trình bày, backend Spring Boot là tầng ứng dụng/nghiệp vụ, SQL Server là tầng dữ liệu. |
| Mô hình lập trình Java | 03 lớp | Controller là lớp giao diện/API, Service là lớp xử lý nghiệp vụ, Repository/Entity là lớp truy cập dữ liệu. |
| Web framework | MVC dạng REST | Spring MVC dùng Controller để nhận request và trả JSON. Phần View chính nằm ở frontend Vue, không phải view render server truyền thống. |
| Tổ chức backend | Nhiều lớp, thực tế là 5 nhóm lớp chính | Controller, Service, Repository, Entity/DTO và nhóm hạ tầng hỗ trợ gồm Security, Auth, Config, Util, Worker. |

Vì vậy khi mô tả ngắn gọn có thể nói: chương trình là ứng dụng web 3 tầng; riêng phần backend Java áp dụng mô hình 03 lớp trên nền Spring MVC REST.

Ánh xạ theo mô hình 03 lớp trong Java:

| Lớp | Thành phần trong dự án | Vai trò |
| --- | --- | --- |
| Presentation layer | Frontend `src/views`, `src/router`, `src/plugins/axios.js` và backend `controllers` | Hiển thị giao diện, nhận thao tác người dùng, lấy dữ liệu từ form và gửi request xuống server. Controller nhận request, kiểm tra dữ liệu đầu vào cơ bản và trả kết quả về giao diện. |
| Business logic layer | `services`, `services/impl` | Xử lý nghiệp vụ, kiểm tra quyền, kiểm tra phạm vi công ty, điều phối xử lý. |
| Data access layer | `repositories`, `entity`, SQL Server | Truy vấn và lưu dữ liệu. |

Diễn giải theo luồng xử lý:

- Presentation layer là lớp giao diện người dùng. Trong dự án này, phần hiển thị view nằm ở Vue, còn phần controller nhận request nằm ở Spring Boot. Lớp này lấy dữ liệu người dùng nhập, gửi xuống server và hiển thị kết quả trả về.
- Business logic layer là lớp xử lý logic nghiệp vụ. Lớp này quyết định dữ liệu có hợp lệ không, user có quyền không, công ty có đúng phạm vi không, trạng thái nghiệp vụ có cho phép thao tác không.
- Data access layer là lớp kết nối và query database. Repository lấy hoặc lưu dữ liệu ở SQL Server rồi chuyển kết quả cho lớp Business logic xử lý tiếp.

Ví dụ luồng đăng nhập:

1. Người dùng nhập tài khoản và mật khẩu trên màn hình đăng nhập Vue.
2. Frontend gửi request đăng nhập xuống controller backend.
3. Controller nhận request và gọi service xác thực.
4. Service xử lý logic đăng nhập: kiểm tra user tồn tại, mật khẩu đúng, trạng thái user, bảo mật IP, phiên đăng nhập và vai trò.
5. Repository query bảng `users`, `companies`, `login_sessions` hoặc bảng liên quan.
6. Service tạo kết quả đăng nhập và JWT nếu hợp lệ.
7. Controller trả response cho frontend.
8. Frontend lưu token và điều hướng về trang phù hợp, ví dụ trang quản trị hoặc trang doanh nghiệp.

Các thư mục như `security`, `auth`, `config`, `util`, `worker` là phần hạ tầng hỗ trợ. Chúng không làm mất mô hình 03 lớp, nhưng khiến backend thực tế được tổ chức chi tiết hơn để dễ bảo trì.

Nguyên tắc phụ thuộc chính:

1. Frontend chỉ gọi API, không truy cập trực tiếp database.
2. Controller chỉ nhận request, kiểm tra dữ liệu đầu vào cơ bản và gọi service.
3. Service chứa nghiệp vụ chính, kiểm tra quyền, phạm vi công ty và điều phối xử lý.
4. Repository chỉ phụ trách truy vấn dữ liệu.
5. Entity/DTO mô tả dữ liệu, không chứa nghiệp vụ phức tạp.
6. Security, Auth, Config, Util và Worker là nhóm hạ tầng dùng chung, không thay thế service nghiệp vụ.

## Luồng request thông thường

1. Người dùng thao tác trên màn hình Vue.
2. Frontend lấy token trong `localStorage` và gửi request bằng Axios.
3. Backend đi qua `JwtAuthenticationFilter` để xác thực JWT và phiên đăng nhập.
4. Controller lấy thông tin user hiện tại qua lớp auth/base controller.
5. Service kiểm tra quyền, phạm vi công ty và xử lý nghiệp vụ.
6. Repository đọc ghi dữ liệu qua Spring Data JPA hoặc câu query tùy chỉnh.
7. Backend trả JSON cho frontend hiển thị.

## Backend

Backend tổ chức theo các lớp chính:

| Lớp | Trách nhiệm |
| --- | --- |
| `controllers` | Nhận request, đọc tham số, gọi service, trả response. |
| `services` | Khai báo interface nghiệp vụ. |
| `services/impl` | Hiện thực nghiệp vụ, kiểm tra quyền, gom dữ liệu, gọi repository. |
| `repositories` | Truy vấn database. |
| `entity` | Ánh xạ bảng database. |
| `dto` | Dữ liệu trung gian trả ra hoặc nhận vào ở một số luồng. |
| `security` | JWT, filter xác thực, cấu hình bảo mật. |
| `util` | Hàm tiện ích như sinh XML, mã hóa, đường dẫn upload. |
| `worker` | Tác vụ nền như xử lý hàng đợi mail. |

## Frontend

Frontend dùng Vue 2, Vue Router và BootstrapVue. Route chính nằm tại:

- `src/router/index.js`: route người dùng, auth và public.
- `src/router/modules/administrator.js`: route khu quản trị.
- `src/router/modules/setting.js`: route cài đặt công ty và tài khoản.

Màn hình được chia theo ngữ cảnh:

- `src/views/customers`: nghiệp vụ công ty.
- `src/views/administrators`: nghiệp vụ quản trị hệ thống.
- `src/views/settings`: cấu hình tài khoản và công ty.
- `src/views/public`: trang công khai.

## Xác thực và phân quyền

Hệ thống dùng hai loại token lưu ở frontend:

| Token | Dùng cho |
| --- | --- |
| `token` | Khu người dùng doanh nghiệp. |
| `token-admin` | Khu quản trị. |

Backend kiểm tra quyền bằng bảng `permissions`, `permission_categories`, `user_permissions` và các trường `role`, `admin_scope` trong bảng `users`.

## Tác vụ nền

Các tác vụ nền quan trọng:

- Gửi mail từ hàng đợi trong bảng `mail_jobs`.
- Gửi báo cáo hóa đơn ngày theo lịch trong `daily_invoice_report_configs`.
- Kiểm tra và gửi thông báo Telegram qua cấu hình `telegram_configs`.

Khi sửa tác vụ nền cần chú ý log, điều kiện chạy lại, company id và khả năng gửi trùng.

## Sinh file và dữ liệu hóa đơn

Một số luồng không chỉ đọc ghi bảng mà còn sinh dữ liệu:

- XML hóa đơn từ `InvoiceXmlBuilder`.
- XML tờ khai từ `RegisterInvoiceXmlBuilder`.
- XML mẫu từ `SampleInvoiceXmlBuilder`.
- PDF từ HTML/XSLT qua OpenHTMLToPDF.
- Excel import và export qua Apache POI.

File upload runtime nằm dưới `uploads`; log runtime nằm dưới `logs`.
