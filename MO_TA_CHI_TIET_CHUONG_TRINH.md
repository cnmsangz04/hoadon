# Mô Tả Chi Tiết Chương Trình Hóa Đơn Điện Tử

Tài liệu này mô tả tổng quan hệ thống, các nhóm người dùng, chức năng nghiệp vụ, luồng xử lý chính, dữ liệu quan trọng và các thành phần kỹ thuật của chương trình hóa đơn điện tử.

## 1. Mục Đích Chương Trình

Chương trình là hệ thống quản lý hóa đơn điện tử dành cho doanh nghiệp và bộ phận quản trị hệ thống. Hệ thống hỗ trợ doanh nghiệp đăng ký sử dụng, quản lý hồ sơ công ty, quản lý mẫu hóa đơn, lập hóa đơn GTGT, ký và gửi hóa đơn đến cơ quan thuế, gửi email hóa đơn cho khách mua, mua thêm gói hóa đơn, báo cáo thống kê và tra cứu hóa đơn công khai.

Ở phía quản trị, hệ thống hỗ trợ duyệt đăng ký công ty, quản lý công ty, quản lý gói hóa đơn, quản lý hạn mức hóa đơn, quản lý danh mục nền, quản lý phân quyền, theo dõi trạng thái gửi email, cấu hình Telegram và gửi báo cáo tự động.

## 2. Công Nghệ Sử Dụng

### 2.1 Backend

- Spring Boot 3.
- Java 21.
- Spring MVC để xây dựng REST API.
- Spring Security và JWT để xác thực request.
- Spring Data JPA/JDBC để thao tác cơ sở dữ liệu.
- SQL Server là cơ sở dữ liệu chính theo cấu hình hiện tại.
- Spring Mail để gửi email.
- Spring Scheduler để chạy tác vụ nền.
- OpenHTMLToPDF để xuất PDF từ HTML/XSLT.
- Apache POI để xuất Excel.
- Jackson để xử lý JSON.
- AES encryptor để mã hóa dữ liệu nhạy cảm như mật khẩu SMTP và bot token Telegram.

### 2.2 Frontend

- Vue 2.
- Vue Router.
- BootstrapVue.
- Axios.
- Toastr.
- vue-select.
- TinyMCE.
- Font Awesome.
- Vue Advanced Cropper cho avatar/logo.

### 2.3 Lưu Trữ File

Hệ thống lưu file upload trong thư mục `uploads`, gồm:

- Logo công ty.
- Favicon công ty.
- Avatar người dùng.
- Ảnh đại diện mẫu hóa đơn.
- File XSLT mẫu hóa đơn.
- File XML/PDF sinh ra trong các luồng xem, tải hoặc gửi mail.

## 3. Nhóm Người Dùng Và Vai Trò

### 3.1 Khách Chưa Có Tài Khoản

Khách có thể:

- Đăng ký công ty mới.
- Đăng nhập tài khoản người dùng.
- Đăng nhập tài khoản admin.
- Quên mật khẩu và đặt lại mật khẩu.
- Tra cứu hóa đơn công khai bằng mã tra cứu/MST.

### 3.2 Người Dùng Doanh Nghiệp

Người dùng doanh nghiệp có thể:

- Xem dashboard công ty.
- Quản lý tờ khai đăng ký sử dụng hóa đơn điện tử.
- Quản lý mẫu hóa đơn.
- Lập, sửa, ký, gửi và theo dõi hóa đơn GTGT.
- Import hóa đơn từ file.
- Quản lý sản phẩm.
- Quản lý khách hàng.
- Mua gói hóa đơn.
- Xem lịch sử mua gói và thanh toán lại giao dịch chưa thành công.
- Cấu hình máy chủ gửi mail.
- Xem trạng thái email gửi hóa đơn.
- Xuất báo cáo hóa đơn.
- Cập nhật tài khoản cá nhân.
- Xem phiên đăng nhập.

### 3.3 Admin Công Ty

Admin công ty có thêm quyền:

- Cập nhật hồ sơ công ty.
- Quản lý người đại diện pháp luật.
- Quản lý thông tin hóa đơn và thông tin gửi cơ quan thuế.
- Quản lý thông tin ngân hàng của công ty.
- Quản lý thành viên công ty.
- Gán quyền cho thành viên.
- Khóa, mở khóa, reset mật khẩu, gửi thông tin đăng nhập cho thành viên.
- Cấu hình bảo mật IP.
- Xem lịch sử đăng nhập trong phạm vi công ty.

### 3.4 Admin Hệ Thống Và Root

Admin hệ thống hoặc Root có thể:

- Xem thống kê mua gói hóa đơn.
- Duyệt hoặc từ chối hồ sơ đăng ký công ty.
- Quản lý danh sách công ty.
- Quản lý gói hóa đơn.
- Quản lý hạn mức hóa đơn đã mua.
- Quản lý mẫu hóa đơn hệ thống.
- Quản lý ngân hàng.
- Quản lý cơ quan thuế.
- Quản lý thuế suất.
- Quản lý mail template.
- Theo dõi và gửi lại email toàn hệ thống.
- Cấu hình Telegram.
- Gửi báo cáo Telegram thử.
- Quản lý quyền và nhóm quyền.
- Xem phiên đăng nhập theo ngữ cảnh admin.

## 4. Xác Thực Và Điều Hướng

### 4.1 Trang Xác Thực

Các trang xác thực chính:

- `/auth/login`: đăng nhập người dùng doanh nghiệp.
- `/auth/login-admin`: đăng nhập khu vực admin.
- `/auth/register`: đăng ký công ty mới.
- `/auth/forgot-password`: quên mật khẩu.
- `/auth/reset-password/:token`: đặt lại mật khẩu.

Frontend lưu riêng:

- `token`: token đăng nhập người dùng.
- `token-admin`: token đăng nhập admin.

### 4.2 Điều Hướng Người Dùng

Các route người dùng yêu cầu `requiresUser`. Nếu chưa có token người dùng thì chuyển về `/auth/login`.

Các route admin yêu cầu `requiresAdmin`. Nếu chưa có token admin hợp lệ thì chuyển về `/auth/login-admin`.

Một số route cấu hình yêu cầu `rolePolicy: role<2`, nghĩa là chỉ Root hoặc Admin được truy cập, ví dụ quản lý thành viên, lịch sử đăng nhập và bảo mật IP.

### 4.3 Trạng Thái Công Ty

Trạng thái công ty được dùng để kiểm soát quyền sử dụng hệ thống:

- `0`: tạm ngưng.
- `1`: đang hoạt động.
- `2`: chờ kích hoạt.

Khi công ty đang chờ kích hoạt, người dùng vẫn đăng nhập được nhưng các chức năng nghiệp vụ bị hạn chế. Màn hình hiển thị thông báo chờ kích hoạt thay vì cho thao tác như bình thường.

## 5. Phân Quyền

Hệ thống dùng bảng:

- `permission_categories`: nhóm quyền.
- `permissions`: danh sách quyền, trong đó `name` là permission key.
- `user_permissions`: quyền được gán cho từng user.

Controller backend gọi `permission("permission-key")` để kiểm tra quyền. Nếu user không có quyền phù hợp thì hệ thống trả lỗi không có quyền thao tác.

Root có `role = 0` được phép toàn bộ. Các user khác cần được gán quyền trong `user_permissions`.

Một số nhóm quyền quan trọng:

- Quyền hóa đơn: `invoice-list`, `invoice-save`, `invoice-delete`.
- Quyền tờ khai: `register-invoice-list`, `register-invoice-save`, `register-invoice-send`.
- Quyền mẫu hóa đơn: `form-invoice-list`, `form-invoice-save`, `form-invoice-manage`.
- Quyền báo cáo: `report-invoice`, `report-invoice-export`.
- Quyền danh mục: `category-product-list`, `category-product-save`, `category-customer-list`, `category-customer-save`.
- Quyền cài đặt: `setting-profile`, `setting-member-list`, `setting-member-save`, `setting-member-manage`, `setting-security-ip`, `setting-login-history`.
- Quyền email: `mail-server-manage`, `mail-job-list`, `mail-job-retry`, `admin-mail-job-list`, `admin-mail-job-retry`.
- Quyền mua gói: `invoice-package-purchase`, `buy-invoice-list`, `buy-invoice-save`, `buy-invoice-delete`.
- Quyền Telegram: `telegram-config-manage`.
- Quyền quản trị danh mục nền: `bank-list`, `bank-save`, `tax-authority-list`, `tax-authority-save`, `tax-authority-delete`, `vat-rate-list`, `vat-rate-save`, `vat-rate-delete`.
- Quyền quản lý phân quyền: `permission-manage`, `permission-category-manage`.

## 6. Luồng Đăng Ký Công Ty

### 6.1 Người Dùng Đăng Ký

Người dùng nhập thông tin đăng ký công ty trên trang `/auth/register`.

Hệ thống kiểm tra:

- Mã số thuế đã tồn tại trong danh sách công ty hay chưa.
- Mã số thuế đã có hồ sơ đăng ký đang chờ duyệt hay chưa.
- Dữ liệu bắt buộc như tên công ty, mã số thuế, email, số điện thoại.

Nếu hợp lệ, hồ sơ được lưu vào bảng `company_registration_requests`.

### 6.2 Admin Duyệt Hồ Sơ

Admin xử lý tại `/administrator/company-registration/list`.

Khi duyệt:

- Tạo công ty mới.
- Tạo tài khoản admin công ty.
- Cập nhật hồ sơ đăng ký sang trạng thái đã duyệt.
- Gửi email thông tin đăng nhập bằng template `LOGIN_INFO_MAIL`.
- Công ty có thể được tạo ở trạng thái chờ kích hoạt tùy luồng cấu hình.

Khi từ chối:

- Hồ sơ được đánh dấu từ chối.
- Lưu người duyệt, thời gian duyệt và ghi chú.
- Không tạo công ty và tài khoản.

## 7. Chức Năng Phía Doanh Nghiệp

### 7.1 Dashboard Công Ty

Đường dẫn: `/`

Dashboard hiển thị:

- Tổng số hóa đơn công ty đang có.
- Số hóa đơn đã sử dụng.
- Số hóa đơn còn lại.
- Số hóa đơn đã phát hành trong năm.
- Giá trị hóa đơn trong năm.
- Nút đi nhanh đến mua gói hóa đơn khi gần hết hạn mức.

API chính: `/v1/dashboard/stats`.

### 7.2 Quản Lý Gói Hóa Đơn Và Mua Gói

Đường dẫn: `/invoice-packages`

Chức năng:

- Xem danh sách gói hóa đơn đang kích hoạt.
- Xem số lượng hóa đơn, đơn giá, thành tiền và mô tả gói.
- Chọn phương thức thanh toán.
- Tạo giao dịch mua gói.
- Theo dõi lịch sử mua gói.
- Lọc lịch sử mua gói.
- Thanh toán lại giao dịch đang chờ hoặc thất bại.

Phương thức thanh toán:

- `MOMO`: MoMo mặc định.
- `MOMO_WALLET`: MoMo ví điện tử.
- `MOMO_ATM`: MoMo ATM nội địa.
- `MOMO_CREDIT`: MoMo thẻ quốc tế.
- `MOMO_PAY_LATER`: MoMo trả sau.
- `VNPAY`: cổng VNPAY.

Trạng thái thanh toán:

- `PENDING`: chờ thanh toán.
- `SUCCESS`: thanh toán thành công.
- `FAILED`: thanh toán thất bại.

Luồng xử lý:

1. Người dùng chọn gói và phương thức thanh toán.
2. Hệ thống tạo bản ghi `invoice_package_purchases`.
3. Nếu là MoMo hoặc VNPAY, hệ thống tạo URL thanh toán và mở cổng thanh toán.
4. Cổng thanh toán gọi IPN/return về hệ thống.
5. Hệ thống xác thực chữ ký, số tiền, mã giao dịch.
6. Nếu thành công, hệ thống cộng hạn mức hóa đơn vào `buy_invoices`.
7. Hệ thống ghi lịch sử mua và có thể gửi email thông báo mua gói.

API chính:

- `/v1/invoice-packages`
- `/v1/invoice-packages/purchase`
- `/v1/invoice-packages/my-purchases`
- `/v1/invoice-packages/purchases/{id}/retry-payment`
- `/v1/invoice-packages/momo/ipn`
- `/v1/invoice-packages/momo/return`
- `/v1/invoice-packages/vnpay/ipn`
- `/v1/invoice-packages/vnpay/return`

### 7.3 Tờ Khai Đăng Ký Hóa Đơn Điện Tử

Đường dẫn:

- `/register/invoice/list`
- `/register/invoice/create`
- `/register/invoice/:id/edit`

Chức năng:

- Danh sách tờ khai theo công ty.
- Lọc theo trạng thái, loại tờ khai, từ khóa, khoảng ngày.
- Tạo tờ khai đăng ký mới.
- Tạo tờ khai thay đổi.
- Tự lấy dữ liệu từ hồ sơ công ty, người đại diện và cơ quan thuế.
- Cập nhật tờ khai trước khi gửi.
- Khai báo chữ ký số trong tờ khai.
- Ký tờ khai.
- Gửi tờ khai đến cơ quan thuế.
- Xem lịch sử truyền nhận.
- Tải XML tờ khai.
- Xóa tờ khai khi còn trạng thái cho phép.

API chính: `/v1/register-invoices`.

### 7.4 Mẫu Hóa Đơn

Đường dẫn:

- `/form-invoice/list`
- `/form-invoice/template`
- `/form-invoice/create`
- `/form-invoice/:id/edit`
- `/form-invoice/:id/view`

Chức năng:

- Danh sách mẫu hóa đơn của công ty.
- Chọn mẫu hệ thống để sao chép thành mẫu riêng.
- Tạo mẫu hóa đơn mới.
- Cập nhật mẫu hóa đơn.
- Upload file XSLT.
- Upload ảnh đại diện mẫu.
- Xem trước mẫu bằng XML mẫu.
- Tải XML/PDF xem trước.
- Bật/tắt trạng thái mẫu.
- Xóa mẫu theo điều kiện nghiệp vụ.

Điều kiện:

- Công ty cần có tờ khai hợp lệ trước khi kích hoạt/lập mẫu.
- Khi lập hóa đơn, hệ thống cần có mẫu hóa đơn đang kích hoạt.

API chính:

- `/v1/form-invoices`
- `/v1/file`

### 7.5 Lập Và Quản Lý Hóa Đơn GTGT

Đường dẫn:

- `/invoice/vat-invoice/list`
- `/invoice/create`
- `/invoice/:id/edit`
- `/invoice/replace`
- `/invoice/adjust`

Chức năng:

- Danh sách hóa đơn GTGT.
- Lọc hóa đơn theo mã tra cứu, khách hàng, ký hiệu, ngày, trạng thái.
- Lập hóa đơn mới.
- Cập nhật hóa đơn khi còn trạng thái cho phép.
- Xóa hóa đơn khi chưa phát hành và chưa khóa nghiệp vụ.
- Nhân bản hóa đơn.
- Ký số hóa đơn.
- Gửi hóa đơn đến cơ quan thuế.
- Nhận và hiển thị trạng thái phản hồi.
- Xem hóa đơn HTML.
- Tải PDF hóa đơn.
- Tải XML hóa đơn.
- Gửi email hóa đơn cho khách hàng.
- Xem lịch sử truyền nhận.
- Lập hóa đơn thay thế.
- Lập hóa đơn điều chỉnh.

Trạng thái hóa đơn phổ biến:

- `0`: mới khởi tạo.
- `1`: đã ký.
- `2`: đã gửi thuế.
- `3`: đã phát hành.
- `4`: bị thay thế hoặc đã hủy tùy ngữ cảnh hiển thị.
- `5`: bị điều chỉnh hoặc đã thay thế tùy ngữ cảnh hiển thị.
- `6`: đã hủy hoặc đã điều chỉnh tùy ngữ cảnh hiển thị.
- `7`: không đủ điều kiện cấp mã.

Luồng hóa đơn thông thường:

1. Người dùng lập hóa đơn.
2. Hệ thống kiểm tra mẫu hóa đơn, hạn mức hóa đơn và dữ liệu bắt buộc.
3. Hóa đơn được lưu ở trạng thái mới khởi tạo.
4. Người dùng ký hóa đơn.
5. Hệ thống tạo XML hóa đơn và lưu thông tin ký.
6. Người dùng gửi cơ quan thuế.
7. Hệ thống ghi lịch sử gửi và cập nhật trạng thái.
8. Khi phát hành thành công, hệ thống có thể đưa email hóa đơn vào hàng đợi gửi cho khách mua.

API chính: `/v1/invoices`.

### 7.6 Import Hóa Đơn

Đường dẫn: `/imports/invoice`

Chức năng:

- Upload file import hóa đơn.
- Đọc dữ liệu theo cấu trúc mẫu.
- Kiểm tra lỗi dữ liệu.
- Tạo hóa đơn nháp hoặc nhóm hóa đơn.
- Trả file lỗi hoặc danh sách lỗi nếu dữ liệu không hợp lệ.

API chính: `/v1/invoice-imports` hoặc controller import hóa đơn tương ứng.

### 7.7 Danh Mục Sản Phẩm

Đường dẫn: `/categories/product/list`

Chức năng:

- Danh sách sản phẩm theo công ty.
- Tìm kiếm sản phẩm.
- Thêm sản phẩm.
- Cập nhật sản phẩm.
- Quản lý mã, tên, đơn vị tính, đơn giá, thuế suất, mô tả và trạng thái.
- Dùng sản phẩm để nạp nhanh dòng hàng hóa khi lập hóa đơn.

API chính: `/v1/categories/product`.

### 7.8 Danh Mục Khách Hàng

Đường dẫn: `/categories/customer/list`

Chức năng:

- Danh sách khách hàng theo công ty.
- Tìm kiếm khách hàng.
- Thêm khách hàng.
- Cập nhật khách hàng.
- Quản lý tên khách hàng, mã số thuế, người mua, địa chỉ, email, điện thoại, fax, tài khoản ngân hàng và tên ngân hàng.
- Dùng khách hàng để nạp nhanh thông tin người mua khi lập hóa đơn.

API chính: `/v1/categories/customer`.

### 7.9 Báo Cáo Hóa Đơn

Đường dẫn: `/reports/invoice/list`

Chức năng:

- Xem báo cáo hóa đơn theo khoảng ngày.
- Lọc theo trạng thái hoặc thông tin liên quan.
- Tổng hợp số tiền, tiền thuế, tổng thanh toán.
- Xuất báo cáo Excel.

API chính:

- `/v1/reports/invoices`
- `/v1/reports/invoices/export`

### 7.10 Cấu Hình Máy Chủ Gửi Mail

Đường dẫn: `/email/mail-server`

Chức năng:

- Cấu hình host SMTP.
- Cấu hình port.
- Cấu hình tài khoản gửi.
- Cấu hình mật khẩu SMTP, được mã hóa trước khi lưu.
- Cấu hình tên người gửi và email người gửi.
- Chọn kiểu mã hóa STARTTLS hoặc SSL/TLS.
- Gửi email kiểm tra.

API chính: `/v1/mail-servers`.

### 7.11 Trạng Thái Email Gửi Hóa Đơn

Đường dẫn: `/email/mail-history`

Chức năng:

- Xem lịch sử email của công ty.
- Lọc theo từ khóa và trạng thái.
- Xem chi tiết email.
- Xem số lần gửi.
- Xem lỗi gửi mail nếu có.
- Gửi lại email từ lịch sử.

Trạng thái mail job:

- `queued`: chờ gửi.
- `processing`: đang gửi.
- `retry`: chờ gửi lại sau lỗi tạm thời.
- `sent`: đã gửi.
- `failed`: lỗi vĩnh viễn.

API chính: `/v1/mail-jobs`.

## 8. Chức Năng Cài Đặt Doanh Nghiệp

### 8.1 Tài Khoản Cá Nhân

Đường dẫn: `/setting/account/list`

Chức năng:

- Xem thông tin tài khoản hiện tại.
- Cập nhật họ tên.
- Cập nhật email.
- Cập nhật số điện thoại.
- Đổi mật khẩu.
- Upload avatar.

API chính: `/v1/setting/account`.

### 8.2 Hồ Sơ Công Ty

Đường dẫn: `/setting/profile/list`

Chức năng:

- Cập nhật thông tin công ty.
- Cập nhật logo, favicon.
- Cập nhật người đại diện pháp luật.
- Cập nhật thông tin liên hệ.
- Cập nhật thông tin hiển thị trên hóa đơn.
- Cập nhật thông tin ngân hàng.
- Cập nhật cơ quan thuế quản lý.
- Cập nhật thông tin phục vụ tờ khai gửi cơ quan thuế.

API chính: `/v1/setting/profile`.

### 8.3 Quản Lý Thành Viên

Đường dẫn: `/setting/member/list`

Chức năng:

- Xem danh sách thành viên.
- Lọc theo vai trò và trạng thái.
- Tạo thành viên mới.
- Cập nhật thành viên.
- Tạo mật khẩu tự động.
- Đặt user làm admin nếu user hiện tại đủ quyền.
- Phân quyền chi tiết theo nhóm quyền.
- Khóa hoặc mở khóa tài khoản.
- Reset mật khẩu.
- Gửi thông tin đăng nhập.

API chính: `/v1/setting/members`.

### 8.4 Phiên Đăng Nhập

Đường dẫn:

- `/setting/sessions/list`
- `/administrator/sessions/list`

Chức năng:

- Xem danh sách phiên đăng nhập của chính user.
- Hiển thị thiết bị, IP, user-agent, thời gian tạo, hết hạn và lần truy cập gần nhất.
- Đánh dấu phiên hiện tại.
- Đăng xuất một phiên cụ thể.
- Đăng xuất các thiết bị khác.
- Tách ngữ cảnh user/admin để không hiển thị lẫn phiên không cùng loại.

API chính: `/v1/setting/sessions`.

### 8.5 Lịch Sử Đăng Nhập

Đường dẫn: `/setting/login-history/list`

Chức năng:

- Xem lịch sử đăng nhập.
- Lọc theo từ khóa.
- Hiển thị username, IP, user-agent, loại đăng nhập, thời gian đăng nhập.
- Root có thể xem rộng hơn, admin công ty xem trong phạm vi công ty.

API chính: `/v1/setting/login-history`.

### 8.6 Bảo Mật IP

Đường dẫn: `/setting/security/ip`

Chức năng:

- Bật hoặc tắt bảo mật IP cho công ty.
- Xem IP hiện tại.
- Thêm IP được phép đăng nhập.
- Xóa IP được phép.
- Ghi chú IP.
- Chặn đăng nhập nếu công ty bật bảo mật IP và request không thuộc danh sách cho phép.

API chính: `/v1/setting/security/ip`.

## 9. Chức Năng Quản Trị Hệ Thống

### 9.1 Dashboard Admin

Đường dẫn: `/administrator`

Chức năng:

- Xem thống kê mua gói hóa đơn.
- Lọc theo thời gian, phương thức thanh toán, trạng thái.
- Xem biểu đồ/thống kê theo tháng.
- Xuất Excel thống kê.

### 9.2 Quản Lý Công Ty

Đường dẫn: `/administrator/company/list`

Chức năng:

- Danh sách công ty.
- Tìm kiếm, lọc trạng thái.
- Tạo công ty.
- Cập nhật công ty.
- Kích hoạt, tạm ngưng hoặc chuyển về chờ kích hoạt.
- Tạo admin công ty.
- Gửi thông tin đăng nhập cho admin công ty.

API chính: `/v1/administrator/company`.

### 9.3 Duyệt Đăng Ký Công Ty

Đường dẫn: `/administrator/company-registration/list`

Chức năng:

- Xem hồ sơ đăng ký công ty.
- Lọc hồ sơ theo trạng thái.
- Tìm kiếm theo tên công ty, mã số thuế, email.
- Duyệt hồ sơ.
- Từ chối hồ sơ.
- Xem thời gian và người xử lý.

API chính: `/v1/administrator/company-registration`.

### 9.4 Quản Lý Gói Hóa Đơn

Đường dẫn: `/administrator/invoice-package/list`

Chức năng:

- Danh sách gói hóa đơn.
- Thêm gói mới.
- Cập nhật gói.
- Xóa mềm hoặc tắt gói.
- Quản lý tên gói, số lượng hóa đơn, đơn giá, tổng tiền, mô tả, thứ tự hiển thị và trạng thái.
- Xem thống kê giao dịch mua gói.
- Xuất Excel.

API chính: `/v1/administrator/invoice-packages`.

### 9.5 Hạn Mức Hóa Đơn

Đường dẫn: `/administrator/buy-invoice/list`

Chức năng:

- Xem danh sách hạn mức hóa đơn của các công ty.
- Thêm/cập nhật số hóa đơn mua.
- Theo dõi số hóa đơn đã dùng.
- Xem lịch sử mua gói.
- Không cho xóa bản ghi đã phát sinh sử dụng hoặc đang có ràng buộc nghiệp vụ.

API chính: `/v1/administrator/buy-invoice`.

### 9.6 Mẫu Hóa Đơn Hệ Thống

Đường dẫn:

- `/administrator/form-invoice/list`
- `/administrator/form-invoice/create`
- `/administrator/form-invoice/:id/edit`

Chức năng:

- Quản lý mẫu hóa đơn hệ thống.
- Upload XSLT và ảnh mẫu.
- Tạo mẫu để doanh nghiệp sao chép.
- Xem trước mẫu.
- Bật/tắt trạng thái mẫu.

API chính: `/v1/administrator/form-invoices`.

### 9.7 Ngân Hàng

Đường dẫn: `/administrator/bank/list`

Chức năng:

- Xem danh sách ngân hàng.
- Tìm kiếm và lọc trạng thái.
- Thêm/cập nhật ngân hàng.
- Bật/tắt trạng thái.
- Dữ liệu ngân hàng được dùng trong hồ sơ công ty và thông tin thanh toán.

API chính: `/v1/administrator/bank`.

### 9.8 Cơ Quan Thuế

Đường dẫn: `/administrator/tax-authority/list`

Chức năng:

- Xem danh sách cơ quan thuế.
- Tìm kiếm, lọc.
- Thêm/cập nhật cơ quan thuế.
- Khai báo cấp cha/cấp con.
- Xóa theo điều kiện.
- Dữ liệu được dùng khi công ty chọn cơ quan thuế quản lý.

API chính: `/v1/administrator/tax-authority`.

### 9.9 Thuế Suất

Đường dẫn: `/administrator/vat-rate/list`

Chức năng:

- Xem danh sách thuế suất.
- Thêm/cập nhật thuế suất.
- Bật/tắt trạng thái.
- Xóa theo điều kiện.
- Dùng cho sản phẩm và dòng hóa đơn.

API chính: `/v1/administrator/vat-rates`.

### 9.10 Mail Template

Đường dẫn:

- `/administrator/email-template/list`
- `/administrator/email-template/create`

Chức năng:

- Quản lý template email hệ thống.
- Cấu hình tiêu đề email.
- Cấu hình nội dung email.
- Bật/tắt template.
- Dùng cho các luồng gửi mail như thông tin đăng nhập, reset mật khẩu, phát hành hóa đơn, mua gói hóa đơn.

API chính: `/v1/administrator/mail-template`.

### 9.11 Trạng Thái Email Toàn Hệ Thống

Đường dẫn: `/administrator/email/mail-history`

Chức năng:

- Xem lịch sử gửi mail của tất cả công ty.
- Lọc theo từ khóa và trạng thái.
- Xem công ty, email nhận, loại email, tiêu đề, trạng thái, số lần gửi và lỗi.
- Gửi lại email nếu job có payload hợp lệ.

API chính: `/v1/administrator/mail-jobs`.

### 9.12 Cấu Hình Telegram

Đường dẫn: `/administrator/telegram/config`

Chức năng:

- Bật/tắt gửi báo cáo Telegram.
- Lưu Bot Token ở database, token được mã hóa.
- Lưu Group Chat ID.
- Cấu hình giờ và phút gửi báo cáo hằng ngày.
- Xem ngày báo cáo đã gửi gần nhất.
- Xem thời điểm gửi gần nhất.
- Gửi báo cáo thử.

Báo cáo Telegram gồm số lượng hóa đơn của ngày hôm qua theo từng công ty:

- Đã ký.
- Đã gửi thuế.
- Không đủ điều kiện cấp mã.
- Tổng số hóa đơn cần kiểm tra.

Nếu không có hóa đơn, Telegram vẫn gửi thông báo: `Không có hóa đơn cần kiểm tra.`

Scheduler chạy mỗi phút, sau đó kiểm tra cấu hình giờ/phút trong bảng `telegram_configs`. Nếu đến đúng giờ và chưa gửi cho ngày báo cáo đó, hệ thống gửi báo cáo và cập nhật `last_report_date`, `last_sent_at`.

API chính:

- `/v1/administrator/telegram-config`
- `/v1/administrator/telegram-config/test`
- `/v1/administrator/telegram-report/send`

### 9.13 Quản Lý Phân Quyền

Đường dẫn:

- `/administrator/access-control/permissions/list`
- `/administrator/access-control/permission-categories/list`

Chức năng:

- Quản lý danh sách quyền.
- Quản lý nhóm quyền.
- Cập nhật tên hiển thị, level, mô tả, trạng thái.
- Sắp xếp nhóm quyền.
- Quyền mới tạo có thể được tự gán cho Root.
- Một số quyền level thấp có thể dùng cho admin công ty/member tùy cấu hình.

API chính:

- `/v1/administrator/permissions`
- `/v1/administrator/permission-categories`

## 10. Tra Cứu Hóa Đơn Công Khai

Đường dẫn: `/lookup-invoice`

Chức năng:

- Khách mua có thể tra cứu hóa đơn mà không cần đăng nhập.
- Nhập mã tra cứu hoặc thông tin liên quan.
- Xem trạng thái hóa đơn.
- Xem thông tin người bán, người mua, ngày lập, tổng tiền.
- Mở bản xem hóa đơn.
- Tải PDF.
- Tải XML.

API chính: `/v1/public/invoices/lookup` hoặc controller public lookup tương ứng.

## 11. Thông Báo Và Lịch Sử

### 11.1 Thông Báo Header

Hệ thống có API thông báo gần đây theo công ty:

- Lấy các lịch sử nghiệp vụ có `show_notify`.
- Hiển thị số lượng chưa đọc trên icon chuông.
- Lưu trạng thái đã đọc/chưa đọc theo user vào bảng `notification_reads`.
- Khi user mở thông báo, hệ thống đánh dấu đã đọc theo từng history id.

API chính:

- `/v1/history/notifications`
- `/v1/history/notifications/read`

### 11.2 Lịch Sử Nghiệp Vụ

Bảng `histories` lưu các sự kiện quan trọng như:

- Tạo/cập nhật hóa đơn.
- Ký hóa đơn.
- Gửi cơ quan thuế.
- Phản hồi cơ quan thuế.
- Mua gói hóa đơn.
- Thay đổi trạng thái quan trọng.

Lịch sử được dùng cho hiển thị thông báo và truy vết nghiệp vụ.

## 12. Hàng Đợi Email

Hệ thống dùng hàng đợi email lưu trong database, tương tự queue job.

Bảng chính: `mail_jobs`.

Luồng xử lý:

1. Chức năng nghiệp vụ gọi `MailQueueService.enqueue`.
2. Hệ thống lưu payload JSON vào `mail_jobs`.
3. Job có trạng thái ban đầu là `queued`.
4. Worker nền quét job sẵn sàng xử lý.
5. Khi xử lý, trạng thái chuyển sang `processing`.
6. Nếu gửi thành công, trạng thái là `sent`.
7. Nếu lỗi tạm thời, trạng thái là `retry`.
8. Nếu vượt số lần thử lại, trạng thái là `failed`.

Các email đăng nhập và reset mật khẩu có thể không hiển thị ở lịch sử phía khách hàng để tránh lộ thông tin nhạy cảm.

## 13. Thanh Toán MoMo Và VNPAY

### 13.1 MoMo

Hệ thống hỗ trợ nhiều loại thanh toán MoMo:

- Ví MoMo.
- ATM nội địa.
- Thẻ quốc tế.
- Trả sau.

Mỗi loại được map sang `requestType` tương ứng khi gọi MoMo.

Khi tạo giao dịch:

- Hệ thống tạo `orderId`.
- Hệ thống tạo chữ ký.
- Gọi API MoMo.
- Lưu URL thanh toán.
- Cập nhật giao dịch chờ thanh toán.

Khi MoMo callback:

- Hệ thống kiểm tra chữ ký.
- Kiểm tra order id.
- Kiểm tra số tiền.
- Cập nhật thành công hoặc thất bại.
- Cộng hạn mức hóa đơn nếu thành công.

### 13.2 VNPAY

Khi tạo giao dịch:

- Hệ thống tạo `vnp_TxnRef`.
- Tạo tham số thanh toán.
- Tạo chữ ký VNPAY.
- Trả URL sang frontend để mở cổng thanh toán.

Khi VNPAY IPN/return:

- Hệ thống xác thực chữ ký.
- Kiểm tra mã giao dịch.
- Kiểm tra số tiền và website code.
- Nếu thành công thì cộng hạn mức hóa đơn.
- Nếu thất bại thì cập nhật trạng thái thất bại.

### 13.3 Thanh Toán Lại

Người dùng có thể thanh toán lại khi:

- Giao dịch đang `PENDING`.
- Giao dịch đã `FAILED`.
- Giao dịch chưa được cộng hạn mức.
- Phương thức thanh toán thuộc MoMo hoặc VNPAY.

Hệ thống tạo giao dịch mới dựa trên giao dịch cũ để tránh dùng lại URL hoặc chữ ký cũ gây lỗi.

## 14. Xuất File Và Xem Hóa Đơn

Hệ thống hỗ trợ:

- Render HTML hóa đơn từ XML và XSLT.
- Tải PDF hóa đơn.
- Tải XML hóa đơn.
- Tải XML tờ khai.
- Tải file preview mẫu hóa đơn.
- Xuất Excel báo cáo hóa đơn.
- Xuất Excel thống kê mua gói.

Các file có thể được sinh động khi người dùng xem/tải hoặc được đính kèm khi gửi email hóa đơn.

## 15. Dữ Liệu Chính Trong Hệ Thống

Các bảng/entity quan trọng:

- `companies`: thông tin công ty.
- `users`: tài khoản người dùng.
- `company_registration_requests`: hồ sơ đăng ký công ty.
- `legal_representatives`: người đại diện pháp luật.
- `company_banks`: thông tin ngân hàng công ty.
- `company_allowed_ips`: danh sách IP được phép.
- `login_histories`: lịch sử đăng nhập.
- `login_sessions`: phiên đăng nhập.
- `products`: danh mục sản phẩm.
- `customers`: danh mục khách hàng.
- `form_invoices`: mẫu hóa đơn.
- `register_invoices`: tờ khai đăng ký hóa đơn.
- `invoices`: hóa đơn GTGT.
- `invoice_numbers`: quản lý số hóa đơn.
- `invoice_imports`: dữ liệu import hóa đơn.
- `histories`: lịch sử nghiệp vụ.
- `notification_reads`: trạng thái đọc thông báo theo user.
- `invoice_packages`: gói hóa đơn.
- `invoice_package_purchases`: giao dịch mua gói hóa đơn.
- `buy_invoices`: hạn mức hóa đơn đã mua.
- `buy_invoice_histories`: lịch sử cộng/trừ hạn mức hóa đơn.
- `mail_templates`: template email.
- `mail_servers`: cấu hình SMTP theo công ty.
- `mail_jobs`: hàng đợi gửi email.
- `telegram_configs`: cấu hình Telegram.
- `banks`: danh mục ngân hàng.
- `tax_authorities`: cơ quan thuế.
- `vat_rates`: thuế suất.
- `permissions`: quyền.
- `permission_categories`: nhóm quyền.
- `user_permissions`: quyền gán cho user.

## 16. API Tổng Quan Theo Nhóm

### 16.1 Public/Auth

- `/v1/auth/**`: đăng nhập, đăng ký, quên mật khẩu, thông tin user.
- `/v1/public/**`: API public.
- `/lookup-invoice`: trang tra cứu hóa đơn công khai.

### 16.2 Doanh Nghiệp

- `/v1/dashboard/stats`: thống kê dashboard.
- `/v1/register-invoices`: tờ khai.
- `/v1/form-invoices`: mẫu hóa đơn.
- `/v1/invoices`: hóa đơn.
- `/v1/invoice-packages`: gói hóa đơn và mua gói.
- `/v1/categories/product`: sản phẩm.
- `/v1/categories/customer`: khách hàng.
- `/v1/reports/invoices`: báo cáo hóa đơn.
- `/v1/mail-servers`: cấu hình SMTP.
- `/v1/mail-jobs`: lịch sử gửi email của công ty.

### 16.3 Cài Đặt

- `/v1/setting/account`: tài khoản cá nhân.
- `/v1/setting/profile`: hồ sơ công ty.
- `/v1/setting/members`: thành viên.
- `/v1/setting/sessions`: phiên đăng nhập.
- `/v1/setting/login-history`: lịch sử đăng nhập.
- `/v1/setting/security/ip`: bảo mật IP.

### 16.4 Admin

- `/v1/administrator/company`: công ty.
- `/v1/administrator/company-registration`: duyệt đăng ký.
- `/v1/administrator/invoice-packages`: gói hóa đơn.
- `/v1/administrator/buy-invoice`: hạn mức hóa đơn.
- `/v1/administrator/form-invoices`: mẫu hóa đơn hệ thống.
- `/v1/administrator/bank`: ngân hàng.
- `/v1/administrator/tax-authority`: cơ quan thuế.
- `/v1/administrator/vat-rates`: thuế suất.
- `/v1/administrator/mail-template`: mail template.
- `/v1/administrator/mail-jobs`: trạng thái email toàn hệ thống.
- `/v1/administrator/telegram-config`: cấu hình Telegram.
- `/v1/administrator/telegram-report`: gửi báo cáo Telegram.
- `/v1/administrator/permissions`: quyền.
- `/v1/administrator/permission-categories`: nhóm quyền.

## 17. Quy Tắc Nghiệp Vụ Quan Trọng

- Công ty chưa kích hoạt không được sử dụng đầy đủ chức năng nghiệp vụ.
- Người dùng chỉ thao tác dữ liệu thuộc công ty của mình.
- Admin hệ thống thao tác dữ liệu toàn hệ thống theo quyền được cấp.
- Root có quyền toàn bộ.
- Mẫu hóa đơn cần hợp lệ trước khi lập hóa đơn.
- Hóa đơn cần đủ dữ liệu người bán, người mua, hàng hóa, thuế suất và mẫu hóa đơn.
- Hóa đơn đã phát hành không được sửa như hóa đơn nháp.
- Hóa đơn thay thế/điều chỉnh phải tham chiếu hóa đơn gốc.
- Giao dịch mua gói thành công mới được cộng hạn mức hóa đơn.
- Không dùng lại URL/chữ ký thanh toán cũ khi thanh toán lại.
- Email gửi hóa đơn xử lý qua hàng đợi để tránh chậm request người dùng.
- Bot token Telegram không lưu plain text mà được mã hóa.
- SMTP password không trả ngược ra frontend, chỉ trả dạng che `••••••••`.

## 18. Bảo Mật

Các cơ chế bảo mật hiện có:

- JWT authentication.
- Tách token user và token admin.
- Route guard frontend.
- Security filter backend.
- Permission key ở controller.
- Phân quyền theo user.
- Bảo mật IP theo công ty.
- Ghi nhận phiên đăng nhập.
- Cho phép đăng xuất phiên khác.
- Ghi lịch sử đăng nhập.
- Mã hóa mật khẩu SMTP.
- Mã hóa Telegram bot token.
- Không đưa token/bí mật vào file cấu hình public.
- Public endpoint chỉ mở cho các luồng cần thiết như tra cứu hóa đơn, tải hóa đơn public, callback thanh toán.

## 19. Tác Vụ Nền

### 19.1 Mail Queue Worker

Worker quét bảng `mail_jobs` định kỳ để gửi email.

Chức năng:

- Lấy job đang chờ.
- Gửi email theo template.
- Cập nhật trạng thái.
- Retry khi lỗi.
- Ghi lỗi cuối cùng khi thất bại vĩnh viễn.

### 19.2 Telegram Daily Report Scheduler

Scheduler chạy mỗi phút, kiểm tra cấu hình Telegram trong database.

Nếu:

- Telegram đang bật.
- Có bot token và chat id.
- Đến đúng giờ/phút cấu hình.
- Chưa gửi báo cáo cho ngày hôm qua.

Thì hệ thống gửi báo cáo hóa đơn ngày hôm qua vào group Telegram.

## 20. Lệnh Chạy Và Build

### 20.1 Chạy Frontend Dev

```bash
npm run serve
```

Frontend chạy mặc định ở:

```text
http://localhost:8080
```

### 20.2 Build Frontend Vào Spring Static

```bash
npm run build
```

Kết quả build nằm ở:

```text
src/main/resources/static
```

### 20.3 Compile Backend

```bash
mvn -q -DskipTests compile
```

### 20.4 Chạy Backend

```bash
./mvnw spring-boot:run
```

Tùy cấu hình server port, backend phục vụ API dưới prefix `/v1`.

## 21. Các Màn Hình Chính

### 21.1 Public/Auth

- `/auth/login`: đăng nhập người dùng.
- `/auth/login-admin`: đăng nhập admin.
- `/auth/register`: đăng ký công ty.
- `/auth/forgot-password`: quên mật khẩu.
- `/auth/reset-password/:token`: đặt lại mật khẩu.
- `/lookup-invoice`: tra cứu hóa đơn.

### 21.2 Doanh Nghiệp

- `/`: dashboard.
- `/invoice-packages`: mua gói hóa đơn.
- `/register/invoice/list`: danh sách tờ khai.
- `/register/invoice/create`: tạo tờ khai.
- `/form-invoice/list`: mẫu hóa đơn.
- `/form-invoice/template`: chọn mẫu hóa đơn.
- `/invoice/vat-invoice/list`: danh sách hóa đơn.
- `/invoice/create`: lập hóa đơn.
- `/invoice/replace`: hóa đơn thay thế.
- `/invoice/adjust`: hóa đơn điều chỉnh.
- `/imports/invoice`: import hóa đơn.
- `/categories/product/list`: sản phẩm.
- `/categories/customer/list`: khách hàng.
- `/reports/invoice/list`: báo cáo hóa đơn.
- `/email/mail-server`: máy chủ gửi mail.
- `/email/mail-history`: trạng thái email.

### 21.3 Cài Đặt

- `/setting/account/list`: tài khoản.
- `/setting/profile/list`: hồ sơ công ty.
- `/setting/sessions/list`: phiên đăng nhập.
- `/setting/member/list`: thành viên.
- `/setting/login-history/list`: lịch sử đăng nhập.
- `/setting/security/ip`: bảo mật IP.

### 21.4 Admin

- `/administrator`: dashboard admin.
- `/administrator/company/list`: công ty.
- `/administrator/company-registration/list`: duyệt đăng ký.
- `/administrator/invoice-package/list`: gói hóa đơn.
- `/administrator/buy-invoice/list`: hạn mức hóa đơn.
- `/administrator/form-invoice/list`: mẫu hóa đơn hệ thống.
- `/administrator/bank/list`: ngân hàng.
- `/administrator/tax-authority/list`: cơ quan thuế.
- `/administrator/vat-rate/list`: thuế suất.
- `/administrator/email-template/list`: mail template.
- `/administrator/email/mail-history`: trạng thái email toàn hệ thống.
- `/administrator/telegram/config`: cấu hình Telegram.
- `/administrator/sessions/list`: phiên đăng nhập admin.
- `/administrator/access-control/permissions/list`: quyền.
- `/administrator/access-control/permission-categories/list`: nhóm quyền.

## 22. Ghi Chú Vận Hành

- Khi thêm chức năng quản trị mới, cần kiểm tra và bổ sung permission key ở controller.
- Khi thêm menu frontend, cần kiểm tra route guard và quyền backend tương ứng.
- Khi thêm callback public như cổng thanh toán, không gắn permission theo user vì cổng thanh toán gọi từ bên ngoài.
- Khi thêm cấu hình nhạy cảm, ưu tiên lưu database và mã hóa, không commit token/mật khẩu thật vào source.
- Khi build frontend, Vue CLI tạo file static có hash mới trong `src/main/resources/static`.
- Cảnh báo bundle lớn khi build hiện tại chủ yếu do vendor bundle và logo lớn, không phải lỗi compile.

