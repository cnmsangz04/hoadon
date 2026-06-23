# Kiến Trúc Kỹ Thuật

Tài liệu gom kiến trúc chương trình, công nghệ sử dụng, cấu trúc thư mục và cơ sở dữ liệu.

## Nguồn đã hợp nhất

- `KIEN_TRUC.md`
- `CONG_NGHE.md`
- `CAU_TRUC_THU_MUC.md`
- `CO_SO_DU_LIEU.md`

## Nội dung

### Từ `KIEN_TRUC.md`

Cập nhật: 18/06/2026.

### Bức tranh tổng thể

Chương trình là ứng dụng web gồm ba phần chính:

| Thành phần | Vai trò |
| --- | --- |
| Frontend Vue 2 | Giao diện người dùng, gọi REST API, điều hướng theo token và quyền. |
| Backend Spring Boot | Xử lý nghiệp vụ, xác thực, phân quyền, kết nối database, gửi mail, sinh XML/PDF. |
| SQL Server | Lưu dữ liệu công ty, người dùng, hóa đơn, mẫu, mail, lịch sử, cấu hình. |

Khi phát triển, frontend thường chạy ở `http://localhost:8080`, backend chạy ở `http://localhost:8081`. Khi build production, frontend được build vào `src/main/resources/static` để backend có thể phục vụ file tĩnh.

### Mô hình kiến trúc

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

### Luồng request thông thường

1. Người dùng thao tác trên màn hình Vue.
2. Frontend lấy token trong `localStorage` và gửi request bằng Axios.
3. Backend đi qua `JwtAuthenticationFilter` để xác thực JWT và phiên đăng nhập.
4. Controller lấy thông tin user hiện tại qua lớp auth/base controller.
5. Service kiểm tra quyền, phạm vi công ty và xử lý nghiệp vụ.
6. Repository đọc ghi dữ liệu qua Spring Data JPA hoặc câu query tùy chỉnh.
7. Backend trả JSON cho frontend hiển thị.

### Backend

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

### Frontend

Frontend dùng Vue 2, Vue Router và BootstrapVue. Route chính nằm tại:

- `src/router/index.js`: route người dùng, auth và public.
- `src/router/modules/administrator.js`: route khu quản trị.
- `src/router/modules/setting.js`: route cài đặt công ty và tài khoản.

Màn hình được chia theo ngữ cảnh:

- `src/views/customers`: nghiệp vụ công ty.
- `src/views/administrators`: nghiệp vụ quản trị hệ thống.
- `src/views/settings`: cấu hình tài khoản và công ty.
- `src/views/public`: trang công khai.

### Xác thực và phân quyền

Hệ thống dùng hai loại token lưu ở frontend:

| Token | Dùng cho |
| --- | --- |
| `token` | Khu người dùng doanh nghiệp. |
| `token-admin` | Khu quản trị. |

Backend kiểm tra quyền bằng bảng `permissions`, `permission_categories`, `user_permissions` và trường `role` trong bảng `users`.

### Tác vụ nền

Các tác vụ nền quan trọng:

- Gửi mail từ hàng đợi trong bảng `mail_jobs`.
- Gửi báo cáo hóa đơn ngày theo lịch trong `daily_invoice_report_configs`.
- Kiểm tra và gửi thông báo Telegram qua cấu hình `telegram_configs`.

Khi sửa tác vụ nền cần chú ý log, điều kiện chạy lại, company id và khả năng gửi trùng.

### Sinh file và dữ liệu hóa đơn

Một số luồng không chỉ đọc ghi bảng mà còn sinh dữ liệu:

- XML hóa đơn từ `InvoiceXmlBuilder`.
- XML tờ khai từ `RegisterInvoiceXmlBuilder`.
- XML mẫu từ `SampleInvoiceXmlBuilder`.
- PDF từ HTML/XSLT qua OpenHTMLToPDF.
- Excel import và export qua Apache POI.

File upload runtime nằm dưới `uploads`; log runtime nằm dưới `logs`.

### Từ `CONG_NGHE.md`

Cập nhật: 18/06/2026.

### Backend

| Công nghệ | Vai trò |
| --- | --- |
| Java 21 | Ngôn ngữ chạy backend. |
| Spring Boot 3.5.7 | Khung ứng dụng chính. |
| Spring Web | Xây dựng REST API. |
| Spring Security | Bảo vệ API và cấu hình bảo mật. |
| JWT | Xác thực request bằng token. |
| Spring Data JPA | Ánh xạ entity và truy vấn database. |
| Spring Data JDBC | Hỗ trợ truy cập dữ liệu SQL khi cần. |
| SQL Server JDBC | Kết nối SQL Server theo cấu hình hiện tại. |
| Spring Mail | Gửi email qua SMTP. |
| Spring Scheduler | Chạy tác vụ nền theo lịch. |
| Thymeleaf | Hỗ trợ template phía backend khi cần render nội dung. |
| OpenHTMLToPDF | Xuất PDF từ HTML/XSLT. |
| Apache POI | Đọc và ghi file Excel. |
| Jackson | Xử lý JSON. |
| Validation | Kiểm tra dữ liệu đầu vào. |

### Frontend

| Công nghệ | Vai trò |
| --- | --- |
| Vue 2.7 | Khung giao diện chính. |
| Vue Router 3 | Điều hướng trang và route guard. |
| BootstrapVue | Component giao diện. |
| Bootstrap 4 | Nền CSS giao diện. |
| Axios | Gọi API backend. |
| Toastr | Hiển thị thông báo. |
| vue-select | Dropdown tìm kiếm. |
| TinyMCE | Soạn nội dung mẫu hoặc nội dung giàu định dạng. |
| Font Awesome | Icon giao diện. |
| vue-advanced-cropper | Cắt ảnh logo/avatar. |
| vuedraggable | Kéo thả sắp xếp ở một số màn hình. |

### Database và file

| Thành phần | Vai trò |
| --- | --- |
| SQL Server | Database chính theo cấu hình hiện tại. |
| DBML/dbdiagram | Mô tả schema và quan hệ database. |
| `uploads` | Lưu file upload runtime. |
| `logs` | Lưu log ứng dụng. |

### Tích hợp bên ngoài

| Tích hợp | Vai trò |
| --- | --- |
| SMTP | Gửi email hóa đơn, báo cáo, thông báo tài khoản. |
| Telegram Bot | Gửi thông báo hoặc báo cáo vào nhóm Telegram. |
| MoMo | Thanh toán mua gói hóa đơn. |
| VNPAY | Thanh toán mua gói hóa đơn. |
| ZaloPay | Thanh toán mua gói hóa đơn trong môi trường sandbox. |
| Cơ quan thuế | Luồng XML hóa đơn/tờ khai và trạng thái gửi cơ quan thuế. |

### Ghi chú lựa chọn kỹ thuật

- Backend và frontend cùng nằm trong một repository để dễ build, triển khai và đồng bộ nghiệp vụ.
- Frontend khi build sẽ được đưa vào `src/main/resources/static`, phù hợp kiểu triển khai một ứng dụng Spring Boot phục vụ cả API và giao diện.
- Hàng đợi mail nằm trong database, giúp xem lại lịch sử, gửi lại và xử lý lỗi gửi mail.
- Các dữ liệu nhạy cảm như mật khẩu SMTP hoặc token Telegram cần được mã hóa trước khi lưu.

### Từ `CAU_TRUC_THU_MUC.md`

Cập nhật: 19/06/2026.

### Thư mục gốc

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

### Backend

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

### Frontend

| Đường dẫn | Nội dung |
| --- | --- |
| `src/views/auth` | Đăng nhập, đăng ký, quên mật khẩu. |
| `src/views/customers` | Dashboard, hóa đơn, báo cáo, danh mục, mua gói. |
| `src/views/customers/imports` | Màn hình import hóa đơn, import khách hàng và import sản phẩm. |
| `src/views/administrators` | Quản trị hệ thống, gói hóa đơn, danh mục nền, mail, Telegram, quyền. |
| `src/views/settings` | Hồ sơ công ty, thành viên, bảo mật IP, lịch sử đăng nhập. |
| `src/views/public` | Trang công khai như tra cứu hóa đơn. |
| `src/router/index.js` | Route chính và guard người dùng. |
| `src/router/modules/administrator.js` | Route khu quản trị. |
| `src/router/modules/setting.js` | Route phần cài đặt. |
| `src/plugins` | Axios, thông báo, xác thực hoặc plugin giao diện dùng chung. |
| `src/utils` | Hàm tiện ích frontend. |

### Quy ước khi thêm file

- Màn hình khách hàng đặt dưới `src/views/customers`.
- Màn hình quản trị hệ thống đặt dưới `src/views/administrators`.
- API nghiệp vụ công ty đặt dưới `controllers/customers`.
- API cấu hình tài khoản/công ty đặt dưới `controllers/setting`.
- API quản trị hệ thống đặt dưới `controllers/admin`.
- Entity, repository, service và service impl nên đặt tên cùng nghiệp vụ để dễ tra cứu.
- Script SQL bổ sung nên đặt trong `tools/sql` với ngày và mục đích rõ ràng.

### Từ `CO_SO_DU_LIEU.md`

Cập nhật: 19/06/2026.

### Nguồn mô tả schema

Database được mô tả chính trong:

- `db.dbml`: định nghĩa bảng, cột và quan hệ bằng DBML.
- `db.dbdiagram`: file đang dùng để xem hoặc chỉnh schema theo dbdiagram.
- `tools/sql`: script SQL bổ sung dữ liệu hoặc chỉnh schema thủ công.

Cấu hình hiện tại trong `application.properties` kết nối SQL Server database `hoadon_database` ở `localhost:1433`.

### Nhóm bảng chính

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

### Quan hệ dữ liệu quan trọng

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

### Ghi chú về lịch sử import

`invoice_imports` lưu lịch sử import Excel. Cột `import_type` phân loại dữ liệu:

- `INVOICE` hoặc `NULL` với dữ liệu cũ: import hóa đơn.
- `CUSTOMER`: import danh mục khách hàng.
- `PRODUCT`: import danh mục sản phẩm.

Với import danh mục, `item_count` và `imported_item_ids` lưu số lượng và danh sách ID khách hàng/sản phẩm đã tạo hoặc cập nhật. Với import hóa đơn, `invoice_count` và `imported_invoice_ids` tiếp tục lưu số lượng và danh sách hóa đơn đã tạo.

### Nguyên tắc nhiều công ty

- Mọi dữ liệu nghiệp vụ của công ty thường phải truy vấn theo `company_id`.
- Quản trị viên toàn quyền chỉ được bỏ lọc công ty ở các màn hình quản trị có chủ đích.
- Khi tạo mail job cho báo cáo ngày, `company_id` phải là công ty nhận nội dung báo cáo để lịch sử không dồn về root.
- Khi lấy XML ký hoặc XML gửi cơ quan thuế, cần kiểm tra cả hóa đơn, công ty và mã tra cứu để tránh trả nhầm XML.
- Cấu hình Telegram và cấu hình lịch báo cáo hóa đơn ngày là hai nghiệp vụ khác nhau, nên lưu ở hai bảng khác nhau: `telegram_configs` và `daily_invoice_report_configs`.
- `daily_invoice_report_configs` hiện là cấu hình lịch gửi chung. Khi gửi báo cáo, hệ thống gom dữ liệu theo từng công ty và tạo `mail_jobs.company_id` theo công ty nhận nội dung.

### Ghi chú về `ddl-auto`

Hiện cấu hình có `spring.jpa.hibernate.ddl-auto=update`. Cấu hình này tiện cho phát triển nhưng khi triển khai thật vẫn nên quản lý thay đổi schema bằng script SQL rõ ràng trong `tools/sql` để tránh thay đổi khó kiểm soát.
