# Cấu hình và vận hành

Cập nhật: 19/06/2026.

## Cổng chạy

| Thành phần | Cổng mặc định |
| --- | --- |
| Backend Spring Boot | `8081` |
| Frontend Vue dev server | `8080` |

Frontend dev server gọi backend qua cấu hình API hiện có trong dự án. Khi build production, frontend được build vào static resource của backend.

## Cấu hình backend

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
| MoMo | `momo.*` | Cấu hình thanh toán MoMo. |
| VNPAY | `vnpay.*` | Cấu hình thanh toán VNPAY. |
| ZaloPay | `zalopay.*` | Cấu hình thanh toán ZaloPay sandbox. |

## Cấu hình frontend

Các file cần xem:

- `package.json`: script và dependency.
- `vue.config.js`: cấu hình build/dev server nếu có.
- `src/router`: route và rule điều hướng.
- `src/plugins`: cấu hình Axios, thông báo và plugin dùng chung.

## Lệnh thường dùng

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

## File runtime

| Đường dẫn | Nội dung |
| --- | --- |
| `uploads` | Logo, avatar, file mẫu hóa đơn, file import hóa đơn, file import danh mục và file upload khác. |
| `logs/hoadon.log` | Log ứng dụng. |
| `target` | Kết quả build Maven. |
| `node_modules` | Dependency frontend sau khi `npm install`. |

Không nên commit file log runtime, file build tạm hoặc file upload phát sinh ngoài dữ liệu mẫu cần thiết.

## Lưu ý secret và môi trường thật

File `application.properties` hiện có giá trị mặc định để chạy/dev nhanh. Khi triển khai thật cần thay hoặc override bằng biến môi trường:

- `jwt.secret`: khóa ký JWT, không dùng khóa mẫu.
- `app.encryption.key`: khóa AES mã hóa mật khẩu SMTP/token lưu DB.
- `spring.datasource.*`: thông tin SQL Server.
- `momo.*`, `vnpay.*`, `zalopay.*`: thông tin merchant/cổng thanh toán.
- `app.frontend-url`, `app.backend-url`: domain thật để link email và callback thanh toán đúng.

Không đưa mật khẩu DB, token Telegram, khóa thanh toán thật hoặc khóa mã hóa thật vào source public.

## Vận hành mail

Hệ thống có hai lớp mail:

- Mail server của công ty trong bảng `mail_servers`.
- Hàng đợi và lịch sử gửi trong bảng `mail_jobs`.

Khi gửi email, cần kiểm tra đúng công ty sở hữu nội dung. Với báo cáo hóa đơn ngày, nội dung và người nhận thuộc công ty thường thì job/lịch sử cũng phải nằm ở công ty đó.

## Vận hành báo cáo hóa đơn ngày

Báo cáo hóa đơn ngày dùng bảng `daily_invoice_report_configs` để lưu lịch gửi. Cấu hình Telegram vẫn dùng `telegram_configs`. Hai phần này tách riêng vì báo cáo ngày có thể gửi cả Telegram và email.

Nguyên tắc gửi:

- Telegram có thể gửi thông báo không có hóa đơn cần kiểm tra.
- Email chỉ gửi khi có dữ liệu báo cáo.
- Lần gửi gần nhất phải được lưu để tránh gửi lặp cùng ngày báo cáo.
- Khi đổi giờ/phút hoặc bật lại lịch, cần kiểm tra lại trạng thái lần gửi gần nhất.

## Thanh toán

Cấu hình MoMo, VNPAY và ZaloPay đang ở chế độ cấu hình qua property. Khi đổi môi trường thật cần kiểm tra:

- Endpoint thanh toán.
- Partner code, terminal code hoặc `app_id`.
- Secret key.
- Return URL và notify URL.
- Log giao dịch trong bảng mua gói hóa đơn.

Với ZaloPay sandbox, các khóa chính là:

- `zalopay.app-id`, `zalopay.key1`, `zalopay.key2`.
- `zalopay.create-order-url`, `zalopay.query-order-url`.
- `zalopay.bank-list-url`, `zalopay.bank-list-app-id`, `zalopay.bank-list-key1`.
- `zalopay.redirect-url`, `zalopay.callback-url`.
- `zalopay.bank-code`, `zalopay.preferred-payment-methods`.

Nếu `zalopay.redirect-url` hoặc `zalopay.callback-url` để trống, backend tự dùng `app.backend-url` và các endpoint `/v1/invoice-packages/zalopay/return`, `/v1/invoice-packages/zalopay/callback`. Khi test callback thật, URL backend phải truy cập được từ ZaloPay sandbox.
