# Yêu cầu hệ thống

Cập nhật: 19/06/2026.

Tài liệu này mô tả yêu cầu chức năng và phi chức năng của hệ thống hóa đơn điện tử, phục vụ phần khảo sát và phân tích yêu cầu trong báo cáo.

## Tác nhân

| Tác nhân | Mô tả |
| --- | --- |
| Khách chưa đăng nhập | Đăng ký công ty, đăng nhập, quên mật khẩu, tra cứu hóa đơn công khai. |
| Nhân viên công ty | Thao tác nghiệp vụ theo quyền được cấp. |
| Admin công ty | Quản lý hồ sơ công ty, thành viên, phân quyền và cấu hình trong phạm vi công ty. |
| Admin công ty root | Truy cập khu quản trị hệ thống theo quyền. |
| Root | Toàn quyền hệ thống. |
| Hệ thống nền | Scheduler, worker mail, callback thanh toán. |

## Yêu cầu chức năng

| Mã | Yêu cầu | Tác nhân chính |
| --- | --- | --- |
| F01 | Đăng ký công ty mới và chờ admin duyệt. | Khách chưa đăng nhập |
| F02 | Đăng nhập người dùng, đăng nhập admin, quên mật khẩu và đặt lại mật khẩu. | Khách chưa đăng nhập |
| F03 | Quản lý hồ sơ công ty, người đại diện, ngân hàng, cơ quan thuế, thông tin hiển thị hóa đơn. | Admin công ty |
| F04 | Quản lý thành viên, khóa/mở khóa, reset mật khẩu và phân quyền. | Admin công ty |
| F05 | Quản lý khách hàng và sản phẩm theo công ty, gồm thêm, sửa, xóa, tìm kiếm và nạp nhanh khi lập hóa đơn. | Nhân viên công ty |
| F06 | Quản lý tờ khai đăng ký sử dụng hóa đơn điện tử. | Nhân viên công ty |
| F07 | Quản lý mẫu hóa đơn và xem trước XML/PDF mẫu. | Nhân viên công ty |
| F08 | Lập, sửa, ký, phát hành, gửi cơ quan thuế, thay thế, điều chỉnh hóa đơn GTGT. | Nhân viên công ty |
| F09 | Import hóa đơn, khách hàng và sản phẩm từ Excel; xem lịch sử import theo công ty. | Nhân viên công ty |
| F10 | Gửi email hóa đơn và xem lịch sử gửi mail theo công ty. | Nhân viên công ty |
| F11 | Xem và xuất báo cáo hóa đơn. | Nhân viên công ty |
| F12 | Mua gói hóa đơn, thanh toán và xem lịch sử mua. | Nhân viên công ty |
| F13 | Quản lý công ty, duyệt đăng ký, gói hóa đơn, hạn mức hóa đơn. | Admin hệ thống |
| F14 | Quản lý danh mục nền: ngân hàng, cơ quan thuế, thuế suất. | Admin hệ thống |
| F15 | Quản lý mail template và lịch sử gửi mail toàn hệ thống. | Admin hệ thống |
| F16 | Cấu hình Telegram và báo cáo hóa đơn ngày. | Admin hệ thống |
| F17 | Tra cứu hóa đơn công khai. | Khách chưa đăng nhập |

## Yêu cầu phi chức năng

| Nhóm | Yêu cầu |
| --- | --- |
| Bảo mật | API nghiệp vụ yêu cầu JWT, kiểm tra phiên, kiểm tra quyền và phạm vi công ty. |
| Phân quyền | Root toàn quyền; admin công ty có quyền quản lý phạm vi công ty; nhân viên dùng quyền gán chi tiết. |
| Toàn vẹn dữ liệu | Dữ liệu nghiệp vụ phải gắn đúng `company_id`, tránh lộ hoặc ghi nhầm dữ liệu công ty khác. |
| Hiệu năng | Các danh sách nghiệp vụ cần phân trang, lọc và tìm kiếm. |
| Tin cậy | Mail gửi qua hàng đợi để có thể retry và xem lỗi. |
| Dễ bảo trì | Backend chia controller, service, repository, entity; frontend chia route và màn hình theo nhóm chức năng. |
| Khả dụng | Scheduler báo cáo ngày không phụ thuộc người dùng thao tác thủ công. |
| Khả năng kiểm tra | Có thể kiểm thử theo API, theo màn hình và theo dữ liệu database. |

## Ràng buộc nghiệp vụ quan trọng

- Công ty chưa kích hoạt bị hạn chế chức năng nghiệp vụ.
- Hóa đơn phải thuộc công ty hiện tại, không được thao tác hóa đơn công ty khác.
- Khách hàng và sản phẩm phải thuộc công ty hiện tại; mã khách hàng/mã sản phẩm không được trùng khi thêm mới hoặc đổi mã thủ công.
- Import khách hàng/sản phẩm dùng mã để cập nhật bản ghi đã có trong công ty, nhưng không chấp nhận mã bị lặp trong cùng file import.
- Hóa đơn đã phát hành không được sửa như hóa đơn nháp.
- Hóa đơn thay thế/điều chỉnh phải tham chiếu hóa đơn gốc.
- Email báo cáo ngày chỉ gửi khi có dữ liệu.
- Telegram báo cáo ngày có thể gửi thông báo không có hóa đơn cần kiểm tra.
- XML ký và XML gửi cơ quan thuế phải lấy đúng theo hóa đơn và công ty.
