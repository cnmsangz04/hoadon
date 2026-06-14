# Mô Tả Chức Năng Chương Trình Hóa Đơn Điện Tử

## 1. Tổng Quan

Chương trình là hệ thống quản lý hóa đơn điện tử gồm frontend Vue 2 và backend Spring Boot. Hệ thống hỗ trợ doanh nghiệp đăng ký tài khoản, quản lý hồ sơ công ty, khách hàng, sản phẩm, mẫu hóa đơn, tờ khai đăng ký sử dụng hóa đơn điện tử, lập hóa đơn GTGT, ký/gửi hóa đơn, gửi email hóa đơn, xuất báo cáo và quản trị dữ liệu nền.

Các nhóm người dùng chính:

- **Khách đăng ký mới**: đăng ký thông tin công ty để chờ admin hệ thống duyệt.
- **Người dùng doanh nghiệp**: lập và quản lý hóa đơn, tờ khai, danh mục khách hàng/sản phẩm, báo cáo, hồ sơ công ty.
- **Admin công ty**: quản lý thành viên, phân quyền và gửi thông tin đăng nhập trong phạm vi công ty.
- **Admin hệ thống/Root**: quản lý công ty, duyệt đăng ký, mẫu hệ thống, email template, ngân hàng, cơ quan thuế, thuế suất, phân quyền và lịch sử gửi mail toàn hệ thống.

## 2. Kiến Trúc Và Công Nghệ

- **Frontend**: Vue 2, Vue Router, BootstrapVue, Axios, Toastr, vue-select, vuedraggable, TinyMCE.
- **Backend**: Spring Boot 3, Java 21, Spring MVC, Spring Security, JWT, Spring Data JPA/JDBC.
- **Cơ sở dữ liệu**: SQL Server theo cấu hình hiện tại trong `application.properties`.
- **Xử lý tài liệu**: sinh XML hóa đơn/tờ khai, render XSLT, xuất PDF bằng OpenHTMLToPDF, xuất Excel bằng Apache POI.
- **Lưu file**: logo, favicon, avatar, ảnh mẫu hóa đơn và file XSLT được lưu dưới thư mục `uploads`.
- **Gửi email**: dùng hàng đợi database qua bảng `mail_jobs`, không phụ thuộc RabbitMQ.

## 3. Xác Thực, Đăng Ký Và Phân Quyền

Hệ thống dùng JWT để xác thực:

- Đăng nhập người dùng tại `/auth/login`.
- Đăng nhập admin tại `/auth/login-admin`.
- Đăng ký công ty mới tại `/auth/register`.
- Quên mật khẩu, gửi email đặt lại mật khẩu và cập nhật mật khẩu mới.
- Tải thông tin user/công ty hiện tại qua `/v1/auth/info`.
- Frontend tách token người dùng (`token`) và token admin (`token-admin`).
- Route người dùng yêu cầu `requiresUser`; route admin yêu cầu `requiresAdmin`.

Vai trò chính:

- **Role 0**: Root/Super admin.
- **Role 1**: Admin.
- **Role 2 hoặc thấp quyền hơn**: nhân viên/người dùng thường.

Trạng thái công ty:

- `0`: tạm ngưng.
- `1`: đang hoạt động.
- `2`: chưa kích hoạt/chờ kích hoạt.

Với công ty trạng thái `2`, người dùng vẫn đăng nhập được nhưng hệ thống ẩn các chức năng nghiệp vụ, chỉ giữ lại thông báo chờ kích hoạt và chức năng đăng xuất.

## 4. Luồng Đăng Ký Công Ty

Trang đăng ký cho phép khách nhập thông tin công ty. Khi gửi đăng ký:

- Hệ thống chưa tạo ngay bản ghi công ty chính thức.
- Thông tin được lưu vào bảng `company_registration_requests`.
- Nếu mã số thuế đã tồn tại ở bảng công ty hoặc đang có hồ sơ chờ duyệt, hệ thống báo lỗi.
- Admin hệ thống xử lý tại menu **Duyệt đăng ký**.

Khi admin duyệt:

- Hệ thống tạo công ty mới từ thông tin đăng ký.
- Công ty được tạo với trạng thái `2` là chưa kích hoạt.
- Hệ thống tạo user admin công ty.
- Hệ thống gửi email thông tin đăng nhập bằng template `LOGIN_INFO_MAIL`.
- Hồ sơ đăng ký được giữ lại, cập nhật trạng thái đã duyệt, người duyệt và thời gian duyệt.

Khi admin từ chối:

- Hồ sơ đăng ký không bị xóa.
- Hệ thống cập nhật trạng thái từ chối, ghi nhận người duyệt, thời gian duyệt và ghi chú nếu có.

## 5. Khu Vực Người Dùng Doanh Nghiệp

### 5.1 Trang Chủ Khách Hàng

Trang chủ khách hàng hiển thị tổng quan nghiệp vụ và thống kê hóa đơn. Backend có API thống kê tại `/v1/dashboard/stats`.

Nếu công ty đang chờ kích hoạt, trang chủ chỉ hiển thị thông báo tài khoản chờ kích hoạt, không hiển thị thống kê và không cho sử dụng menu nghiệp vụ.

### 5.2 Tờ Khai Hóa Đơn Điện Tử

Module tờ khai hỗ trợ:

- Danh sách tờ khai theo công ty, trạng thái, loại tờ khai, từ khóa và khoảng ngày.
- Tạo tờ khai đăng ký mới hoặc tờ khai thay đổi.
- Nạp dữ liệu gợi ý từ hồ sơ công ty và người đại diện pháp luật.
- Cập nhật tờ khai trước khi gửi.
- Ký tờ khai, lưu XML đã ký và thông tin chữ ký.
- Gửi tờ khai đến cơ quan thuế theo luồng giả lập/ghi nhận lịch sử.
- Tải XML tờ khai.
- Xem lịch sử xử lý tờ khai.
- Xóa tờ khai khi còn ở trạng thái cho phép.

API chính nằm dưới `/v1/register-invoices`.

### 5.3 Mẫu Hóa Đơn

Module mẫu hóa đơn hỗ trợ:

- Xem danh sách mẫu hóa đơn của doanh nghiệp.
- Chọn mẫu hệ thống để sao chép/tạo mẫu riêng.
- Tạo, cập nhật, xem và xóa mẫu hóa đơn.
- Upload file XSLT và ảnh đại diện mẫu.
- Xem trước mẫu hóa đơn bằng dữ liệu XML mẫu.
- Tải XML/PDF xem trước của mẫu.

Điều kiện nghiệp vụ:

- Chưa có tờ khai được chấp nhận thì không được thêm/kích hoạt mẫu hóa đơn.
- Chưa có mẫu hóa đơn được kích hoạt thì không được lập hóa đơn.

API chính nằm dưới `/v1/form-invoices` và `/v1/file`.

### 5.4 Hóa Đơn GTGT

Module hóa đơn GTGT hỗ trợ:

- Danh sách hóa đơn theo bộ lọc.
- Chuẩn bị dữ liệu lập hóa đơn: công ty, mẫu hóa đơn, khách hàng, sản phẩm, thuế suất.
- Lập hóa đơn mới.
- Sửa hóa đơn khi còn trạng thái cho phép.
- Xóa hóa đơn theo điều kiện nghiệp vụ.
- Nhân bản hóa đơn.
- Xem hóa đơn HTML theo mã tra cứu.
- Tải XML/PDF hóa đơn.
- Ký hóa đơn.
- Gửi hóa đơn đến cơ quan thuế và cập nhật phản hồi/lịch sử.
- Gửi email thông báo hóa đơn cho khách hàng.
- Theo dõi lịch sử xử lý hóa đơn.

Khi phát hành hóa đơn thành công, hệ thống tự đưa job gửi email vào hàng đợi nếu hóa đơn có email người nhận. Email phát hành hóa đơn có thể gửi kèm file `.zip` chứa XML/PDF của hóa đơn.

API chính nằm dưới `/v1/invoices`.

### 5.5 Danh Mục Sản Phẩm

Module sản phẩm hỗ trợ:

- Danh sách sản phẩm theo công ty.
- Thêm/cập nhật sản phẩm.
- Quản lý mã, tên, đơn giá, đơn vị tính, thuế suất, mô tả và trạng thái.
- Lấy danh sách thuế suất để chọn khi khai báo sản phẩm.

API chính nằm dưới `/v1/categories/product`.

### 5.6 Danh Mục Khách Hàng

Module khách hàng hỗ trợ:

- Danh sách khách hàng theo công ty.
- Thêm/cập nhật thông tin khách hàng.
- Lưu mã khách hàng, mã số thuế, tên công ty, người mua, địa chỉ, email, điện thoại, fax, tài khoản ngân hàng, ngân hàng và mô tả.
- Lấy chi tiết khách hàng để phục vụ lập hóa đơn.

API chính nằm dưới `/v1/categories/customer`.

### 5.7 Email Gửi Hóa Đơn

Khu vực email của doanh nghiệp gồm:

- Cấu hình máy chủ gửi mail.
- Kiểm tra gửi mail thử.
- Gửi email hóa đơn thủ công từ màn hình danh sách hóa đơn.
- Xem lịch sử gửi mail của chính công ty tại `/email/mail-history`.
- Gửi lại email từ lịch sử gửi mail nếu cần.

Lịch sử gửi mail hiển thị loại email, tiêu đề, người gửi, người nhận, email nhận, số lần gửi, trạng thái và thời gian cập nhật.

### 5.8 Báo Cáo Hóa Đơn

Module báo cáo hỗ trợ:

- Xem danh sách hóa đơn theo kỳ, khoảng ngày, trạng thái và các bộ lọc liên quan.
- Tổng hợp thông tin hóa đơn, khách hàng, tiền hàng, tiền thuế và tiền thanh toán.
- Xuất báo cáo Excel.

API chính nằm dưới `/v1/reports/invoices` và `/v1/reports/invoices/export`.

## 6. Khu Vực Cài Đặt Doanh Nghiệp

### 6.1 Tài Khoản Cá Nhân

Người dùng có thể:

- Xem thông tin tài khoản.
- Cập nhật họ tên, email, điện thoại.
- Đổi mật khẩu.
- Upload/cắt avatar.
- Đồng bộ avatar và thông tin user lên header/sidebar.

API chính nằm dưới `/v1/setting/account`.

### 6.2 Hồ Sơ Công Ty

Module hồ sơ hỗ trợ cập nhật:

- Thông tin đơn vị: tên, mã số thuế, địa chỉ, email, hotline, lĩnh vực kinh doanh, prefix, logo, favicon.
- Người đại diện pháp luật: họ tên, ngày sinh, giới tính, CCCD/CMND, hộ chiếu, điện thoại, email.
- Thông tin hiển thị trên hóa đơn: website, email, fax, điện thoại.
- Thông tin hồ sơ gửi cơ quan thuế: người liên hệ, email, điện thoại, địa chỉ.
- Thông tin thanh toán: ngân hàng, số tài khoản, chi nhánh/địa chỉ ngân hàng.
- Cơ quan thuế quản lý theo tỉnh/thành và cơ quan cấp dưới.

Các trường `companies.domain` và `companies.domain_lookup` không còn dùng trong giao diện hiện tại.

API chính nằm dưới `/v1/setting/profile`.

### 6.3 Thành Viên Và Phân Quyền Công Ty

Admin công ty có thể:

- Xem danh sách thành viên.
- Tạo/cập nhật tài khoản thành viên.
- Phân quyền theo danh sách quyền và nhóm quyền.
- Khóa/mở khóa tài khoản.
- Reset mật khẩu.
- Gửi thông tin đăng nhập.
- Xóa mềm thành viên.
- Áp dụng giới hạn quyền để admin không thao tác vượt quyền với admin khác hoặc chính mình ở một số chức năng.

Email gửi thông tin đăng nhập dùng template `LOGIN_INFO_MAIL` và được đưa vào hàng đợi gửi mail.

API chính nằm dưới `/v1/setting/members`.

## 7. Khu Vực Quản Trị Hệ Thống

### 7.1 Quản Lý Công Ty

Admin hệ thống có thể:

- Xem danh sách công ty.
- Tạo/cập nhật thông tin công ty.
- Kích hoạt, chuyển sang chưa kích hoạt hoặc tạm ngưng công ty.
- Tự động tạo admin mặc định cho công ty mới.
- Gửi thông tin đăng nhập cho công ty.

API chính nằm dưới `/v1/administrator/company`.

### 7.2 Duyệt Đăng Ký

Admin hệ thống có thể:

- Xem danh sách hồ sơ đăng ký công ty.
- Tìm kiếm theo tên công ty, mã số thuế, email.
- Lọc theo trạng thái hồ sơ.
- Duyệt hồ sơ để tạo công ty và user admin.
- Từ chối hồ sơ và ghi nhận lý do.
- Xem người duyệt, thời gian duyệt và trạng thái xử lý.

API chính nằm dưới `/v1/administrator/company-registration`.

### 7.3 Quản Lý Số Lượng Hóa Đơn Đã Mua

Module mua hóa đơn hỗ trợ:

- Xem danh sách gói/số lượng hóa đơn theo công ty.
- Tạo/cập nhật số lượng hóa đơn mua.
- Theo dõi số lượng đã sử dụng.
- Không cho xóa khi bản ghi đang hoạt động hoặc đã phát sinh sử dụng.

API chính nằm dưới `/v1/administrator/buy-invoice`.

### 7.4 Quản Lý Ngân Hàng

Module ngân hàng hỗ trợ:

- Danh sách ngân hàng có phân trang/tìm kiếm.
- Lấy toàn bộ ngân hàng để dùng trong combobox.
- Lấy ngân hàng theo mã viết tắt.
- Thêm/cập nhật ngân hàng.

API chính nằm dưới `/v1/administrator/bank`.

### 7.5 Quản Lý Cơ Quan Thuế

Module cơ quan thuế hỗ trợ:

- Danh sách cơ quan thuế theo từ khóa, cấp cha và trạng thái.
- Tạo/cập nhật/xóa cơ quan thuế.
- Quản lý quan hệ cha con giữa cơ quan thuế cấp tỉnh/thành và cơ quan quản lý.

API chính nằm dưới `/v1/tax-authorities`.

### 7.6 Quản Lý Thuế Suất

Module thuế suất hỗ trợ:

- Danh sách thuế suất có phân trang.
- Tạo/cập nhật/xóa thuế suất.
- Bật/tắt trạng thái thuế suất.
- Kéo thả sắp xếp thứ tự ưu tiên.

API chính nằm dưới `/v1/administrator/vat-rate`.

### 7.7 Quản Lý Mẫu Hóa Đơn Hệ Thống

Admin hệ thống có thể:

- Xem danh sách mẫu hóa đơn hệ thống.
- Tạo/cập nhật mẫu hóa đơn hệ thống.
- Upload file XSLT và ảnh mẫu.
- Bật/tắt trạng thái mẫu.
- Xóa mẫu và file liên quan.

API chính nằm dưới `/v1/administrator/form-invoices`.

### 7.8 Quản Lý Email Template

Module email template hỗ trợ:

- Danh sách template theo công ty.
- Xem chi tiết template.
- Tạo/cập nhật/xóa template.
- Soạn nội dung bằng trình soạn thảo TinyMCE.
- Quản lý key, tiêu đề, nội dung, trạng thái và loại template hệ thống/cá nhân.

Các template quan trọng:

- `ISSUE_INVOICE_MAIL`: email phát hành hóa đơn.
- `LOGIN_INFO_MAIL`: email gửi thông tin đăng nhập.
- `RESET_PASSWORD_MAIL`: email đặt lại mật khẩu.

API chính nằm dưới `/v1/administrator/mail-template`.

### 7.9 Lịch Sử Gửi Mail Toàn Hệ Thống

Admin hệ thống có thể:

- Xem toàn bộ lịch sử gửi mail của mọi công ty.
- Lọc theo trạng thái gửi.
- Tìm kiếm theo công ty, người nhận, email nhận hoặc tiêu đề.
- Xem tên công ty thay vì chỉ xem ID công ty.
- Gửi lại email nếu cần.

Trang admin: `/administrator/email/mail-history`.

### 7.10 Quản Lý Quyền Và Nhóm Quyền

Module phân quyền hệ thống hỗ trợ:

- Quản lý nhóm quyền: danh sách, tạo/cập nhật, xóa, kéo thả sắp xếp.
- Quản lý quyền: danh sách, tạo/cập nhật, xóa, bật/tắt trạng thái.
- Gắn quyền vào nhóm quyền.
- Sử dụng cấp quyền `level` để phân biệt quyền hệ thống/quyền hiển thị cho nhân viên.

API chính nằm dưới:

- `/v1/administrator/permission-categories`
- `/v1/administrator/permissions`

## 8. Hàng Đợi Gửi Mail Và Lịch Sử

Hệ thống gửi mail qua hàng đợi database:

- Bảng chính: `mail_jobs`.
- Service đưa job vào hàng đợi: `MailQueueService`.
- Worker xử lý định kỳ: `DbMailQueueWorker`.
- Nội dung job được lưu dạng JSON trong cột `payload`.
- Tiêu đề, người nhận, công ty, invoice_id, trạng thái, số lần gửi, lỗi cuối cùng và thời gian xử lý được lưu để phục vụ lịch sử gửi mail.

Trạng thái job chính:

- `queued`: chờ gửi.
- `processing`: đang xử lý.
- `retry`: chờ gửi lại.
- `sent`: đã gửi.
- `failed`: gửi lỗi.

Các luồng đang dùng hàng đợi:

- Tự động gửi email sau khi phát hành hóa đơn.
- Gửi lại/gửi thủ công email hóa đơn.
- Gửi thông tin đăng nhập.
- Gửi email đặt lại mật khẩu.

## 9. Thông Báo Và Lịch Sử Nghiệp Vụ

Hệ thống lưu lịch sử thao tác/luồng xử lý trong bảng `history` và cung cấp:

- API lấy thông báo gần đây theo công ty.
- Cờ `showNotify` và `status` để lọc thông báo hiển thị.
- Frontend polling định kỳ để tải thông báo mới.
- Lịch sử riêng cho hóa đơn và tờ khai.

API chính: `/v1/history/notifications`.

## 10. File, XML, PDF Và Xem Trước

Hệ thống có các chức năng xử lý file:

- Render mẫu hóa đơn từ XSLT và XML mẫu.
- Sinh XML hóa đơn và XML tờ khai.
- Ký và lưu XML đã ký.
- Chuyển HTML sang PDF.
- Tải XML/PDF.
- Xem trước HTML dùng cho PDF để kiểm tra hiển thị.
- Chuẩn hóa font, HTML entity, void tag và CSS dự phòng để PDF hiển thị tiếng Việt tốt hơn.

API chính nằm dưới:

- `/v1/file/{id}/view`
- `/v1/file/{id}/download-xml`
- `/v1/file/{id}/download-pdf`
- `/v1/file/{id}/view-pdf-html`

## 11. Dữ Liệu Chính

Các nhóm dữ liệu/entity chính:

- `users`: tài khoản, role, trạng thái, avatar, token quên mật khẩu.
- `companies`: hồ sơ công ty, mã số thuế, logo, favicon, trạng thái, thông tin liên hệ/hóa đơn.
- `company_registration_requests`: hồ sơ đăng ký công ty chờ admin duyệt.
- `company_banks`: tài khoản ngân hàng của công ty.
- `legal_representatives`: người đại diện pháp luật.
- `customers`: khách hàng của doanh nghiệp.
- `products`: sản phẩm/dịch vụ.
- `form_invoices`: mẫu hóa đơn.
- `invoices`: hóa đơn.
- `invoice_numbers`: số lượng/số hóa đơn theo mẫu/công ty.
- `buy_invoices`: số lượng hóa đơn đã mua và đã dùng.
- `register_invoices`: tờ khai đăng ký/thay đổi sử dụng hóa đơn điện tử.
- `history`: lịch sử và thông báo.
- `mail_jobs`: hàng đợi gửi mail và lịch sử gửi mail.
- `mail_servers`: cấu hình máy chủ gửi mail theo công ty.
- `mail_templates`: mẫu email.
- `banks`: danh mục ngân hàng.
- `tax_authorities`: cơ quan thuế.
- `vat_rates`: thuế suất.
- `permissions`, `permission_categories`, `user_permissions`: quyền, nhóm quyền và quyền tùy chỉnh theo user.
- `signature_vats`, `signature_authorities_tax`: XML chữ ký hóa đơn/tờ khai.

## 12. Luồng Nghiệp Vụ Tiêu Biểu

### 12.1 Luồng Đăng Ký Và Kích Hoạt Công Ty

1. Khách vào trang đăng ký từ màn hình đăng nhập.
2. Khách nhập thông tin công ty và gửi đăng ký.
3. Hệ thống lưu vào `company_registration_requests`.
4. Admin hệ thống vào menu **Duyệt đăng ký**.
5. Admin duyệt hồ sơ.
6. Hệ thống tạo công ty trạng thái `2`, tạo user admin công ty và đưa email `LOGIN_INFO_MAIL` vào hàng đợi.
7. Công ty đăng nhập được nhưng chưa dùng chức năng cho đến khi admin hệ thống kích hoạt trạng thái `1`.

### 12.2 Luồng Thiết Lập Ban Đầu

1. Admin hệ thống tạo hoặc duyệt công ty.
2. Hệ thống tạo/gửi thông tin tài khoản admin công ty.
3. Admin công ty đăng nhập và cập nhật hồ sơ công ty.
4. Admin công ty khai báo người đại diện, tài khoản ngân hàng, cơ quan thuế, thông tin hiển thị hóa đơn.
5. Admin công ty tạo thành viên và phân quyền nếu cần.

### 12.3 Luồng Tạo Mẫu Hóa Đơn

1. Người dùng chọn mẫu hệ thống hoặc tạo mẫu mới.
2. Upload file XSLT và ảnh mẫu.
3. Xem trước mẫu bằng XML mẫu.
4. Lưu mẫu để dùng khi lập hóa đơn.
5. Kích hoạt mẫu khi đáp ứng điều kiện có tờ khai được chấp nhận.

### 12.4 Luồng Đăng Ký Hóa Đơn Điện Tử

1. Người dùng tạo tờ khai.
2. Hệ thống nạp dữ liệu công ty/người đại diện.
3. Người dùng kiểm tra và lưu tờ khai.
4. Ký tờ khai.
5. Gửi cơ quan thuế.
6. Theo dõi lịch sử nhận/chấp nhận và tải XML nếu cần.

### 12.5 Luồng Lập Và Phát Hành Hóa Đơn GTGT

1. Người dùng vào màn hình lập hóa đơn.
2. Backend kiểm tra công ty đã có mẫu hóa đơn GTGT được kích hoạt.
3. Người dùng chọn khách hàng, sản phẩm và nhập thông tin hóa đơn.
4. Hệ thống tính tiền hàng, thuế, tổng tiền và số tiền bằng chữ.
5. Người dùng lưu hóa đơn.
6. Người dùng ký hóa đơn.
7. Người dùng gửi cơ quan thuế để cấp mã/ghi nhận phản hồi.
8. Khi hóa đơn phát hành thành công, hệ thống đưa job gửi email phát hành hóa đơn vào `mail_jobs`.
9. Worker gửi email, cập nhật trạng thái và lưu lịch sử gửi mail.

## 13. Ghi Chú Vận Hành

- Frontend dev server chạy port `8080`.
- Backend/proxy API mặc định trỏ `localhost:8081`.
- Build frontend xuất vào `src/main/resources/static`.
- Các file trong `src/main/resources/static` là output sinh ra từ `npm run build`, không nên chỉnh tay khi cập nhật chức năng.
- Các route SPA được xử lý bằng Vue Router history mode.
- Các endpoint public auth không gửi kèm token.
- Khi cần kiểm tra lỗi gửi mail, xem bảng `mail_jobs`, cột `status`, `attempts`, `error`, `payload`.
