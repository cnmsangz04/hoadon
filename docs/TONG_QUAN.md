# Tổng quan chương trình

Cập nhật: 19/06/2026.

## Mục tiêu

Chương trình hỗ trợ doanh nghiệp quản lý vòng đời hóa đơn điện tử từ lúc đăng ký sử dụng, tạo mẫu hóa đơn, lập hóa đơn GTGT, ký số, gửi cơ quan thuế, gửi email cho khách hàng, tra cứu hóa đơn, đến báo cáo và theo dõi lịch sử xử lý.

Phần quản trị hệ thống hỗ trợ duyệt đăng ký công ty, quản lý danh mục nền, gói hóa đơn, email, Telegram, báo cáo hóa đơn ngày và phân quyền.

## Nhóm người dùng

| Nhóm | Vai trò chính |
| --- | --- |
| Khách chưa đăng nhập | Đăng ký công ty, đăng nhập, quên mật khẩu, tra cứu hóa đơn công khai. |
| Nhân viên công ty | Lập hóa đơn, import hóa đơn, import danh mục khách hàng/sản phẩm, quản lý khách hàng, sản phẩm, báo cáo theo quyền được cấp. |
| Admin công ty | Quản lý hồ sơ công ty, thành viên, phân quyền nội bộ, cấu hình bảo mật và email. |
| Admin công ty root | Vào khu quản trị, xử lý danh mục và cấu hình cấp hệ thống theo quyền. |
| Root | Toàn quyền hệ thống, quản lý dữ liệu nhiều công ty và cấu hình lõi. |

Chi tiết quyền và phạm vi dữ liệu nằm trong [QUY_TAC_VAI_TRO_CHUONG_TRINH.md](../QUY_TAC_VAI_TRO_CHUONG_TRINH.md).

## Nhóm chức năng

| Nhóm | Chức năng |
| --- | --- |
| Xác thực | Đăng nhập người dùng, đăng nhập admin, JWT, phiên đăng nhập, kiểm tra IP. |
| Công ty | Hồ sơ công ty, ngân hàng, người đại diện, thông tin gửi cơ quan thuế. |
| Người dùng | Thành viên, quyền, lịch sử đăng nhập, phiên đăng nhập. |
| Danh mục | Khách hàng, sản phẩm, ngân hàng, cơ quan thuế, thuế suất. |
| Hóa đơn | Mẫu hóa đơn, dải số, hóa đơn GTGT, XML, PDF, ký số, gửi cơ quan thuế. |
| Import | Tải mẫu Excel, import nhiều hóa đơn và nhiều dòng hàng hóa; import/cập nhật danh mục khách hàng và sản phẩm theo mã. |
| Email | Máy chủ mail, mẫu mail, hàng đợi gửi mail, lịch sử gửi mail. |
| Báo cáo | Báo cáo hóa đơn, xuất dữ liệu, báo cáo hóa đơn ngày qua Telegram và email. |
| Thanh toán | Gói hóa đơn, mua hóa đơn, lịch sử mua, thanh toán MoMo, VNPAY và ZaloPay sandbox. Chi tiết tích hợp nằm trong [TICH_HOP_THANH_TOAN.md](TICH_HOP_THANH_TOAN.md). |
| Công khai | Tra cứu hóa đơn bằng mã tra cứu hoặc thông tin hóa đơn. |

## Nguyên tắc quan trọng

- Hầu hết dữ liệu nghiệp vụ phải gắn với `company_id`.
- Mã khách hàng và mã sản phẩm không được trùng trong cùng công ty khi thêm mới hoặc đổi mã thủ công; riêng import danh mục dùng mã để cập nhật bản ghi đã tồn tại.
- Root có thể xem hoặc thao tác nhiều công ty khi API cho phép, nhưng dữ liệu công ty thường không được ghi nhầm về công ty root.
- Lịch sử gửi mail và job mail phải nằm theo công ty phát sinh nội dung.
- Báo cáo hóa đơn ngày gửi Telegram có thể thông báo cả khi không có dữ liệu, nhưng email chỉ gửi khi có dữ liệu cần báo cáo.
- XML hóa đơn đã ký phải lấy đúng theo hóa đơn và công ty, tránh dùng nhầm XML của công ty khác.
- Cấu hình thanh toán MoMo và ZaloPay hiện dùng bộ key sandbox/demo public của nhà cung cấp để cá nhân/tổ chức kiểm thử tích hợp; khi triển khai thật phải thay bằng key merchant riêng qua biến môi trường và domain callback public.
