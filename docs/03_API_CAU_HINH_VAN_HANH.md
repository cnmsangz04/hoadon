# API, Cấu Hình Và Vận Hành

Tài liệu gom tổng quan API, cấu hình môi trường, vận hành, bảo trì và kiểm tra thường dùng.

## Nguồn đã hợp nhất

- `API_TONG_QUAN.md`
- `CAU_HINH_VA_VAN_HANH.md`
- `BAO_TRI_VA_KIEM_TRA.md`

## Nội dung

### Từ `API_TONG_QUAN.md`

Cập nhật: 19/06/2026.

Tài liệu này tóm tắt nhóm API chính để người đọc hiểu cách frontend giao tiếp với backend. Danh sách này không thay thế code controller, nhưng đủ dùng cho báo cáo và định hướng đọc mã nguồn.

### Nguyên tắc chung

- API nghiệp vụ dùng prefix `/v1`.
- Frontend gọi API qua Axios.
- API người dùng cần token `token`.
- API admin cần token `token-admin`.
- Backend kiểm tra JWT, phiên đăng nhập, quyền và phạm vi công ty.
- Response chủ yếu là JSON.

### Public/Auth

| Nhóm API | Chức năng |
| --- | --- |
| `/v1/auth/**` | Đăng nhập, đăng ký, quên mật khẩu, đặt lại mật khẩu, thông tin user. |
| `/v1/public/**` | API công khai. |
| `/lookup-invoice` | Trang tra cứu hóa đơn công khai. |

### API doanh nghiệp

| API | Chức năng |
| --- | --- |
| `/v1/dashboard/stats` | Thống kê dashboard công ty. |
| `/v1/register-invoices` | Tờ khai đăng ký hóa đơn điện tử. |
| `/v1/form-invoices` | Mẫu hóa đơn của công ty. |
| `/v1/invoices` | Hóa đơn GTGT, ký, phát hành, gửi cơ quan thuế, tải XML/PDF. |
| `/v1/invoice-imports` | Import hóa đơn từ Excel và lịch sử import. |
| `/v1/catalog-imports/{customer|product}` | Import danh mục khách hàng/sản phẩm từ Excel, tải mẫu, lịch sử import và import lại. |
| `/v1/categories/product` | Danh mục sản phẩm. |
| `/v1/categories/customer` | Danh mục khách hàng. |
| `/v1/reports/invoices` | Báo cáo hóa đơn. |
| `/v1/mail-servers` | Cấu hình SMTP theo công ty. |
| `/v1/mail-jobs` | Lịch sử gửi mail của công ty. |
| `/v1/invoice-packages` | Gói hóa đơn, mua gói, thanh toán, lịch sử mua. |
| `/v1/history/notifications` | Thông báo header theo lịch sử nghiệp vụ của công ty. |
| `/v1/file` | Xem/tải file preview mẫu hóa đơn công khai hoặc được phép xem. |
| `/v1/provinces` | Danh sách tỉnh/thành đang hoạt động dùng cho form hồ sơ. |

Các endpoint thanh toán của nhóm gói hóa đơn:

- `/v1/invoice-packages/purchase`: tạo giao dịch mua gói.
- `/v1/invoice-packages/purchases/{id}/retry-payment`: tạo lại giao dịch cho đơn đang chờ hoặc thất bại.
- `/v1/invoice-packages/momo/ipn`, `/v1/invoice-packages/momo/return`: nhận kết quả MoMo.
- `/v1/invoice-packages/vnpay/ipn`, `/v1/invoice-packages/vnpay/return`: nhận kết quả VNPAY.
- `/v1/invoice-packages/zalopay/callback`, `/v1/invoice-packages/zalopay/return`: nhận kết quả ZaloPay.
- `/v1/invoice-packages/zalopay/banks`: lấy danh sách ngân hàng sandbox ZaloPay đang hỗ trợ.
- `/v1/invoice-packages/my-purchases`: lịch sử mua gói của công ty hiện tại.
- `/v1/invoice-packages/purchases/{id}`: xem chi tiết giao dịch mua gói của công ty hiện tại.

### API cài đặt

| API | Chức năng |
| --- | --- |
| `/v1/setting/account` | Tài khoản cá nhân. |
| `/v1/setting/profile` | Hồ sơ công ty. |
| `/v1/setting/members` | Thành viên và phân quyền. |
| `/v1/setting/sessions` | Phiên đăng nhập. |
| `/v1/setting/login-history` | Lịch sử đăng nhập. |
| `/v1/setting/security/ip` | Bảo mật IP theo công ty. |

### API quản trị

| API | Chức năng |
| --- | --- |
| `/v1/administrator/company` | Quản lý công ty. |
| `/v1/administrator/company-registration` | Duyệt đăng ký công ty. |
| `/v1/administrator/invoice-packages` | Quản lý gói hóa đơn. |
| `/v1/administrator/buy-invoice` | Quản lý hạn mức hóa đơn đã mua. |
| `/v1/administrator/form-invoices` | Quản lý mẫu hóa đơn hệ thống. |
| `/v1/administrator/bank` | Quản lý ngân hàng. |
| `/v1/tax-authorities` | Quản lý cơ quan thuế. |
| `/v1/administrator/vat-rate` | Quản lý thuế suất. |
| `/v1/administrator/mail-template` | Quản lý mẫu email. |
| `/v1/administrator/mail-jobs` | Lịch sử gửi mail toàn hệ thống. |
| `/v1/administrator/telegram-config` | Cấu hình Telegram. |
| `/v1/administrator/daily-invoice-report` | Cấu hình và gửi báo cáo hóa đơn ngày. |
| `/v1/administrator/permissions` | Quản lý quyền. |
| `/v1/administrator/permission-categories` | Quản lý nhóm quyền. |

### Ví dụ luồng gọi API đăng nhập

1. Frontend gửi tài khoản/mật khẩu đến API auth.
2. Backend kiểm tra user, mật khẩu, trạng thái và bảo mật IP.
3. Backend tạo phiên đăng nhập và JWT.
4. Frontend lưu token và điều hướng theo vai trò.

### Ví dụ luồng gọi API hóa đơn

1. Frontend gọi API chuẩn bị lập hóa đơn.
2. Backend kiểm tra quyền, mẫu hóa đơn, tờ khai và công ty.
3. Frontend gửi dữ liệu hóa đơn.
4. Backend lưu hóa đơn, sinh XML/PDF khi cần.
5. Khi phát hành, backend cập nhật trạng thái và có thể tạo mail job.

### Ví dụ luồng gọi API import danh mục

1. Frontend tải mẫu bằng `/v1/catalog-imports/customer/template` hoặc `/v1/catalog-imports/product/template`.
2. Người dùng upload Excel qua `/v1/catalog-imports/{type}/upload`.
3. Backend kiểm tra quyền import hoặc quyền danh mục tương ứng, đọc file, kiểm tra trùng mã trong file và lưu lịch sử import.
4. Nếu mã đã tồn tại trong công ty, import cập nhật bản ghi đó; nếu chưa tồn tại thì tạo mới.

### Ghi chú khi viết tài liệu API chi tiết

File này chỉ tóm tắt nhóm API. Nếu cần bàn giao vận hành hoặc tích hợp, nên bổ sung bảng chi tiết cho từng endpoint gồm:

- Method HTTP.
- Quyền cần có.
- Tham số query/path/body.
- Response thành công.
- Response lỗi phổ biến.
- API public nào không cần JWT, ví dụ callback thanh toán hoặc tra cứu công khai.

### Từ `CAU_HINH_VA_VAN_HANH.md`

Cập nhật: 19/06/2026.

### Cổng chạy

| Thành phần | Cổng mặc định |
| --- | --- |
| Backend Spring Boot | `8081` |
| Frontend Vue dev server | `8080` |

Frontend dev server gọi backend qua cấu hình API hiện có trong dự án. Khi build production, frontend được build vào static resource của backend.

### Cấu hình backend

File chính: `src/main/resources/application.properties`.

Nhóm cấu hình quan trọng:

| Nhóm | Khóa tiêu biểu | Ghi chú |
| --- | --- | --- |
| Server | `server.port` | Cổng backend. |
| Frontend | `app.frontend-url` | URL frontend dùng trong link hoặc mail. |
| Database | `spring.datasource.*` | Kết nối SQL Server. |
| JPA | `spring.jpa.*` | Dialect, log SQL, cập nhật schema. |
| JWT | `jwt.secret`, `jwt.expiration_ms` | Bí mật ký token và thời hạn token theo mili-giây. |
| Log | `logging.file.name` | File log runtime. |
| Mail | `spring.mail.*`, `app.encryption.key`, `mail.queue.max-retries` | Cấu hình mail dự phòng, khóa mã hóa SMTP lưu trong DB và số lần retry hàng đợi mail. SMTP chính của từng công ty lưu trong bảng `mail_servers`. |
| MoMo | `momo.*` | Cấu hình thanh toán MoMo sandbox/demo. |
| VNPAY | `vnpay.*` | Cấu hình thanh toán VNPAY. |
| ZaloPay | `zalopay.*` | Cấu hình thanh toán ZaloPay sandbox/demo. |

### Cấu hình frontend

Các file cần xem:

- `package.json`: script và dependency.
- `vue.config.js`: cấu hình build/dev server nếu có.
- `src/router`: route và rule điều hướng.
- `src/plugins`: cấu hình Axios, thông báo và plugin dùng chung.

### Lệnh thường dùng

Chạy backend:

```bash
./mvnw spring-boot:run
```

Chạy frontend:

```bash
npm run serve
```

Build frontend:

```bash
npm run build
```

Build backend:

```bash
./mvnw package
```

Chạy test backend:

```bash
./mvnw test
```

### File runtime

| Đường dẫn | Nội dung |
| --- | --- |
| `uploads` | Logo, avatar, file mẫu hóa đơn, file import hóa đơn, file import danh mục và file upload khác. |
| `logs/hoadon.log` | Log ứng dụng. |
| `target` | Kết quả build Maven. |
| `node_modules` | Dependency frontend sau khi `npm install`. |

Không nên commit file log runtime, file build tạm hoặc file upload phát sinh ngoài dữ liệu mẫu cần thiết.

### Lưu ý secret và môi trường thật

File `application.properties` hiện có giá trị mặc định để chạy/dev nhanh. Khi triển khai thật cần thay hoặc override bằng biến môi trường:

- `jwt.secret`: khóa ký JWT, không dùng khóa mẫu.
- `app.encryption.key`: khóa AES mã hóa mật khẩu SMTP/token lưu DB.
- `spring.datasource.*`: thông tin SQL Server.
- `momo.*`, `vnpay.*`, `zalopay.*`: thông tin merchant/cổng thanh toán.
- `app.frontend-url`, `app.backend-url`: domain thật để link email và callback thanh toán đúng.

Các giá trị mặc định của MoMo và ZaloPay trong file cấu hình là bộ sandbox/demo public do nhà cung cấp công khai trong tài liệu hoặc mã nguồn mẫu để kiểm thử tích hợp. Đây không phải key riêng của dự án và không dùng cho production.

Không đưa mật khẩu DB, token Telegram, khóa thanh toán thật hoặc khóa mã hóa thật vào source public.

### Vận hành mail

Hệ thống có hai lớp mail:

- Mail server của công ty trong bảng `mail_servers`.
- Hàng đợi và lịch sử gửi trong bảng `mail_jobs`.

Khi gửi email, cần kiểm tra đúng công ty sở hữu nội dung. Với báo cáo hóa đơn ngày, nội dung và người nhận thuộc công ty thường thì job/lịch sử cũng phải nằm ở công ty đó.

### Vận hành báo cáo hóa đơn ngày

Báo cáo hóa đơn ngày dùng bảng `daily_invoice_report_configs` để lưu lịch gửi. Cấu hình Telegram vẫn dùng `telegram_configs`. Hai phần này tách riêng vì báo cáo ngày có thể gửi cả Telegram và email.

Nguyên tắc gửi:

- Telegram có thể gửi thông báo không có hóa đơn cần kiểm tra.
- Email chỉ gửi khi có dữ liệu báo cáo.
- Lần gửi gần nhất phải được lưu để tránh gửi lặp cùng ngày báo cáo.
- Khi đổi giờ/phút hoặc bật lại lịch, cần kiểm tra lại trạng thái lần gửi gần nhất.

### Thanh toán

Cấu hình MoMo, VNPAY và ZaloPay đang ở chế độ cấu hình qua property. Khi đổi môi trường thật cần kiểm tra:

- Endpoint thanh toán.
- Partner code, terminal code hoặc `app_id`.
- Secret key.
- Return URL và notify URL.
- Log giao dịch trong bảng mua gói hóa đơn.

Tài liệu chi tiết về luồng tạo giao dịch, callback, chữ ký và checklist triển khai nằm trong [06_THANH_TOAN.md](06_THANH_TOAN.md).

Nguồn key sandbox/demo hiện tại:

- MoMo dùng bộ `partnerCode`, `accessKey`, `secretKey` demo public trong repo mẫu `momo-wallet/payment` của MoMo, tương ứng endpoint test `https://test-payment.momo.vn`.
- ZaloPay dùng bộ sandbox `app_id = 2553`, `key1` và `key2` trong tài liệu ZaloPay Developer, tương ứng endpoint sandbox `https://sb-openapi.zalopay.vn`.
- Các bộ key này được nhà cung cấp đặt sẵn để developer, cá nhân hoặc tổ chức test luồng tạo đơn, redirect, IPN/callback và xác thực chữ ký.
- Khi triển khai thật, cần override bằng biến môi trường `MOMO_*`, `VNPAY_*`, `ZALOPAY_*` được cấp cho merchant thật; không sửa trực tiếp key thật vào source.
- `momo.redirect-url`, `momo.ipn-url`, `zalopay.redirect-url` và `zalopay.callback-url` là URL của hệ thống này, không phải key do cổng thanh toán cấp. Nếu để trống, backend dùng `app.backend-url` và endpoint callback/return tương ứng.

Nguồn tham khảo: repo mẫu MoMo `https://github.com/momo-wallet/payment/blob/master/php/config.json`, tài liệu MoMo `https://developers.momo.vn/v3/vi/docs/payment/onboarding/integration-process/`, tài liệu ZaloPay `https://developers.zalopay.vn/v2/general/overview.html`.

Với ZaloPay sandbox, các khóa chính là:

- `zalopay.app-id`, `zalopay.key1`, `zalopay.key2`.
- `zalopay.create-order-url`, `zalopay.query-order-url`.
- `zalopay.bank-list-url`, `zalopay.bank-list-app-id`, `zalopay.bank-list-key1`.
- `zalopay.redirect-url`, `zalopay.callback-url`.
- `zalopay.bank-code`, `zalopay.preferred-payment-methods`.

Nếu `zalopay.redirect-url` hoặc `zalopay.callback-url` để trống, backend tự dùng `app.backend-url` và các endpoint `/v1/invoice-packages/zalopay/return`, `/v1/invoice-packages/zalopay/callback`. Khi test callback thật, URL backend phải truy cập được từ ZaloPay sandbox.

### Từ `BAO_TRI_VA_KIEM_TRA.md`

Cập nhật: 19/06/2026.

### Checklist trước khi sửa nghiệp vụ

- Xác định màn hình thuộc khu người dùng, setting hay quản trị.
- Xác định dữ liệu có cần lọc theo `company_id` không.
- Kiểm tra quyền cần dùng trong bảng `permissions`.
- Kiểm tra route frontend có guard phù hợp không.
- Kiểm tra service có đang dùng đúng repository và đúng phạm vi công ty không.
- Nếu sửa mail, kiểm tra `mail_jobs.company_id`, người gửi, người nhận, tiêu đề và nội dung.
- Nếu sửa hóa đơn, kiểm tra XML, PDF, trạng thái hóa đơn, chữ ký và dữ liệu gửi cơ quan thuế.

### Lệnh kiểm tra thường dùng

Chạy test backend:

```bash
./mvnw test
```

Build backend:

```bash
./mvnw package
```

Chạy frontend dev:

```bash
npm run serve
```

Build frontend:

```bash
npm run build
```

Kiểm tra file markdown hiện có:

```bash
rg --files -g '*.md' -g '*.MD'
```

Kiểm tra khác biệt code:

```bash
git diff --check
```

### Khi sửa database

- Cập nhật `db.dbml` hoặc `db.dbdiagram` nếu thay đổi schema.
- Tạo script SQL trong `tools/sql` nếu cần chạy dữ liệu hoặc migration thủ công.
- Đặt tên script có ngày và mục đích, ví dụ `2026-06-18_ten_thay_doi.sql`.
- Khi thêm quyền mới, tạo script idempotent để insert/update `permission_categories` và `permissions`.
- Không dựa hoàn toàn vào `ddl-auto=update` cho môi trường thật.
- Kiểm tra dữ liệu nhiều công ty, đặc biệt các bảng hóa đơn, chữ ký, mail và báo cáo.

### Khi sửa mail

- Kiểm tra mẫu trong `mail_templates`.
- Kiểm tra tiêu đề đã thay placeholder như `[REPORT_DATE]` trước khi lưu job.
- Kiểm tra `mail_jobs.company_id` là công ty phát sinh nội dung.
- Kiểm tra người gửi và người nhận không bị lấy nhầm giữa công ty root và công ty thường.
- Kiểm tra retry không tạo trùng nội dung hoặc gửi sai công ty.

### Khi sửa báo cáo hóa đơn ngày

- Cấu hình lịch gửi nằm ở `daily_invoice_report_configs`.
- Cấu hình Telegram nằm ở `telegram_configs`.
- Gửi thử và gửi tự động cần dùng cùng logic gom dữ liệu.
- Email chỉ gửi khi có dữ liệu.
- Telegram vẫn có thể thông báo khi không có hóa đơn cần kiểm tra.
- Cần lưu ngày báo cáo gần nhất và thời điểm gửi để tránh gửi lặp.

### Khi sửa import Excel

- Mẫu tải về phải đủ cột cho các loại mẫu hóa đơn đang hỗ trợ.
- File import phải đọc được nhiều hóa đơn và nhiều dòng hàng.
- Với import danh mục, file phải kiểm tra trùng mã trong cùng file và dùng mã để cập nhật bản ghi đã tồn tại trong công ty.
- Lịch sử import hóa đơn và import danh mục cùng nằm trong `invoice_imports`, cần lọc đúng `import_type`.
- Cần kiểm tra trường hợp một thuế suất và nhiều thuế suất.
- Lỗi import nên chỉ rõ dòng dữ liệu để người dùng sửa được.
- Không nên thay đổi thứ tự cột mà không cập nhật service đọc file và hướng dẫn trong mẫu.

### Khi sửa danh mục khách hàng/sản phẩm

- Kiểm tra thêm mới không cho trùng mã trong cùng công ty.
- Kiểm tra cập nhật vẫn cho đổi mã, nhưng không được đổi sang mã đang thuộc bản ghi khác.
- Kiểm tra xóa chỉ thao tác bản ghi thuộc công ty hiện tại.
- Kiểm tra màn lập hóa đơn nạp đúng bản ghi được chọn, kể cả dữ liệu cũ từng có mã trùng.
- Kiểm tra nút đi tới import và nút đi tới danh sách tương ứng vẫn đúng route.

### Khi sửa XML hóa đơn

- Kiểm tra dữ liệu đầu vào theo loại mẫu hóa đơn.
- Kiểm tra XML sinh ra có đúng thông tin người bán, người mua, dòng hàng, thuế và tổng tiền.
- Khi lấy XML đã ký, phải lọc đúng hóa đơn và công ty.
- Khi trả file cho người dùng, cần tránh dùng XML của hóa đơn khác hoặc công ty khác.

### Khi sửa giao diện

- Giữ style đồng bộ với các màn hình hiện có.
- Kiểm tra trên màn hình nhỏ và màn hình desktop.
- Kiểm tra route, menu, quyền truy cập và thông báo lỗi.
- Nếu thêm màn hình mới, cập nhật tài liệu route hoặc cấu trúc thư mục khi cần.
