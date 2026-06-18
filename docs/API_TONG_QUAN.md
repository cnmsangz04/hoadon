# Tổng quan API

Cập nhật: 18/06/2026.

Tài liệu này tóm tắt nhóm API chính để người đọc hiểu cách frontend giao tiếp với backend. Danh sách này không thay thế code controller, nhưng đủ dùng cho báo cáo và định hướng đọc mã nguồn.

## Nguyên tắc chung

- API nghiệp vụ dùng prefix `/v1`.
- Frontend gọi API qua Axios.
- API người dùng cần token `token`.
- API admin cần token `token-admin`.
- Backend kiểm tra JWT, phiên đăng nhập, quyền và phạm vi công ty.
- Response chủ yếu là JSON.

## Public/Auth

| Nhóm API | Chức năng |
| --- | --- |
| `/v1/auth/**` | Đăng nhập, đăng ký, quên mật khẩu, đặt lại mật khẩu, thông tin user. |
| `/v1/public/**` | API công khai. |
| `/lookup-invoice` | Trang tra cứu hóa đơn công khai. |

## API doanh nghiệp

| API | Chức năng |
| --- | --- |
| `/v1/dashboard/stats` | Thống kê dashboard công ty. |
| `/v1/register-invoices` | Tờ khai đăng ký hóa đơn điện tử. |
| `/v1/form-invoices` | Mẫu hóa đơn của công ty. |
| `/v1/invoices` | Hóa đơn GTGT, ký, phát hành, gửi cơ quan thuế, tải XML/PDF. |
| `/v1/invoice-imports` | Import hóa đơn từ Excel và lịch sử import. |
| `/v1/categories/product` | Danh mục sản phẩm. |
| `/v1/categories/customer` | Danh mục khách hàng. |
| `/v1/reports/invoices` | Báo cáo hóa đơn. |
| `/v1/mail-servers` | Cấu hình SMTP theo công ty. |
| `/v1/mail-jobs` | Lịch sử gửi mail của công ty. |
| `/v1/invoice-packages` | Gói hóa đơn, mua gói, thanh toán, lịch sử mua. |

Các endpoint thanh toán của nhóm gói hóa đơn:

- `/v1/invoice-packages/purchase`: tạo giao dịch mua gói.
- `/v1/invoice-packages/purchases/{id}/retry-payment`: tạo lại giao dịch cho đơn đang chờ hoặc thất bại.
- `/v1/invoice-packages/momo/ipn`, `/v1/invoice-packages/momo/return`: nhận kết quả MoMo.
- `/v1/invoice-packages/vnpay/ipn`, `/v1/invoice-packages/vnpay/return`: nhận kết quả VNPAY.
- `/v1/invoice-packages/zalopay/callback`, `/v1/invoice-packages/zalopay/return`: nhận kết quả ZaloPay.
- `/v1/invoice-packages/zalopay/banks`: lấy danh sách ngân hàng sandbox ZaloPay đang hỗ trợ.

## API cài đặt

| API | Chức năng |
| --- | --- |
| `/v1/setting/account` | Tài khoản cá nhân. |
| `/v1/setting/profile` | Hồ sơ công ty. |
| `/v1/setting/members` | Thành viên và phân quyền. |
| `/v1/setting/sessions` | Phiên đăng nhập. |
| `/v1/setting/login-history` | Lịch sử đăng nhập. |
| `/v1/setting/security/ip` | Bảo mật IP theo công ty. |

## API quản trị

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

## Ví dụ luồng gọi API đăng nhập

1. Frontend gửi tài khoản/mật khẩu đến API auth.
2. Backend kiểm tra user, mật khẩu, trạng thái và bảo mật IP.
3. Backend tạo phiên đăng nhập và JWT.
4. Frontend lưu token và điều hướng theo vai trò.

## Ví dụ luồng gọi API hóa đơn

1. Frontend gọi API chuẩn bị lập hóa đơn.
2. Backend kiểm tra quyền, mẫu hóa đơn, tờ khai và công ty.
3. Frontend gửi dữ liệu hóa đơn.
4. Backend lưu hóa đơn, sinh XML/PDF khi cần.
5. Khi phát hành, backend cập nhật trạng thái và có thể tạo mail job.
