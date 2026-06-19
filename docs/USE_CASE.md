# Use case hệ thống

Cập nhật: 19/06/2026.

Tài liệu này tóm tắt các use case chính để dùng trong phần phân tích thiết kế và báo cáo.

## Danh sách use case

| Mã | Use case | Tác nhân |
| --- | --- | --- |
| UC01 | Đăng ký công ty | Khách chưa đăng nhập |
| UC02 | Duyệt hồ sơ đăng ký công ty | Admin hệ thống |
| UC03 | Đăng nhập hệ thống | Người dùng, admin |
| UC04 | Quản lý thành viên và phân quyền | Admin công ty |
| UC05 | Quản lý hồ sơ công ty | Admin công ty |
| UC06 | Quản lý khách hàng | Nhân viên công ty |
| UC07 | Quản lý sản phẩm | Nhân viên công ty |
| UC08 | Tạo và gửi tờ khai | Nhân viên công ty |
| UC09 | Quản lý mẫu hóa đơn | Nhân viên công ty |
| UC10 | Lập hóa đơn GTGT | Nhân viên công ty |
| UC11 | Ký và phát hành hóa đơn | Nhân viên công ty |
| UC12 | Gửi hóa đơn qua email | Nhân viên công ty |
| UC13 | Import hóa đơn từ Excel | Nhân viên công ty |
| UC14 | Xem báo cáo hóa đơn | Nhân viên công ty |
| UC15 | Mua gói hóa đơn | Nhân viên công ty |
| UC16 | Quản lý danh mục nền | Admin hệ thống |
| UC17 | Cấu hình báo cáo hóa đơn ngày | Admin hệ thống |
| UC18 | Tra cứu hóa đơn công khai | Khách chưa đăng nhập |
| UC19 | Import danh mục khách hàng/sản phẩm từ Excel | Nhân viên công ty |

## UC03 - Đăng nhập hệ thống

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Người dùng hoặc admin. |
| Điều kiện trước | Tài khoản tồn tại, đang hoạt động. |
| Luồng chính | Người dùng nhập tài khoản/mật khẩu, frontend gửi request, controller gọi service, service kiểm tra user, mật khẩu, trạng thái, IP, phiên đăng nhập, sau đó trả JWT. |
| Kết quả | Người dùng vào trang phù hợp với vai trò. |
| Ngoại lệ | Sai mật khẩu, user bị khóa, công ty chưa hợp lệ, IP không được phép, phiên lỗi. |

## UC06 - Quản lý khách hàng

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền xem hoặc lưu danh mục khách hàng. |
| Luồng chính | Người dùng xem danh sách, tìm kiếm, thêm, cập nhật, đổi mã khách hàng hoặc xóa khách hàng trong công ty hiện tại. |
| Kết quả | Danh mục khách hàng được cập nhật và dùng để nạp nhanh thông tin bên mua khi lập hóa đơn. |
| Ngoại lệ | Mã khách hàng bị trùng trong cùng công ty, thiếu dữ liệu bắt buộc, thiếu quyền hoặc truy cập dữ liệu công ty khác. |

## UC07 - Quản lý sản phẩm

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền xem hoặc lưu danh mục sản phẩm. |
| Luồng chính | Người dùng xem danh sách, tìm kiếm, thêm, cập nhật, đổi mã sản phẩm hoặc xóa sản phẩm trong công ty hiện tại. |
| Kết quả | Danh mục sản phẩm được cập nhật và dùng để nạp nhanh dòng hàng khi lập hóa đơn. |
| Ngoại lệ | Mã sản phẩm bị trùng trong cùng công ty, thiếu dữ liệu bắt buộc, thiếu quyền hoặc truy cập dữ liệu công ty khác. |

## UC10 - Lập hóa đơn GTGT

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền lập hóa đơn, công ty có mẫu hóa đơn hợp lệ, có dải số hoặc điều kiện phát hành phù hợp. |
| Luồng chính | Người dùng nhập thông tin người mua và dòng hàng, hệ thống kiểm tra dữ liệu, tính tiền, lưu hóa đơn ở trạng thái mới khởi tạo. |
| Kết quả | Hóa đơn nháp được tạo. |
| Ngoại lệ | Thiếu quyền, thiếu mẫu hóa đơn, dữ liệu dòng hàng sai, thuế suất không hợp lệ. |

## UC11 - Ký và phát hành hóa đơn

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Hóa đơn tồn tại, thuộc công ty hiện tại, trạng thái cho phép ký/phát hành. |
| Luồng chính | Người dùng ký hóa đơn, hệ thống sinh XML, lưu chữ ký, gửi cơ quan thuế nếu cần, cập nhật trạng thái. |
| Kết quả | Hóa đơn được ký hoặc phát hành theo trạng thái phản hồi. |
| Ngoại lệ | XML không hợp lệ, chữ ký lỗi, phản hồi cơ quan thuế không thành công, hóa đơn không thuộc công ty. |

## UC13 - Import hóa đơn từ Excel

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền `invoice-save`. |
| Luồng chính | Người dùng tải mẫu Excel, nhập dữ liệu, upload file, hệ thống đọc file, gom dòng theo hóa đơn, kiểm tra dữ liệu và tạo hóa đơn nháp. |
| Kết quả | Hóa đơn được import hoặc danh sách lỗi được trả về. |
| Ngoại lệ | File sai định dạng, thiếu thông tin bắt buộc, dữ liệu tiền/thuế sai, không gom được dòng hàng. |

## UC19 - Import danh mục khách hàng/sản phẩm từ Excel

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền import hoặc quyền lưu danh mục tương ứng. |
| Luồng chính | Người dùng tải mẫu Excel, nhập danh mục, upload file; hệ thống kiểm tra trùng mã trong file, tạo mới hoặc cập nhật bản ghi theo mã trong cùng công ty. |
| Kết quả | Khách hàng hoặc sản phẩm được import, lịch sử import được lưu. |
| Ngoại lệ | File sai định dạng, thiếu mã bắt buộc, mã bị lặp trong file, dữ liệu sản phẩm sai đơn giá/thuế suất hoặc thiếu quyền. |

## UC17 - Cấu hình báo cáo hóa đơn ngày

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Admin hệ thống. |
| Điều kiện trước | Có quyền quản lý cấu hình tương ứng. |
| Luồng chính | Admin bật lịch gửi, chọn giờ/phút, hệ thống lưu cấu hình, scheduler tự gửi báo cáo ngày hôm qua khi đến giờ. |
| Kết quả | Báo cáo được gửi qua Telegram và email nếu có dữ liệu. |
| Ngoại lệ | Chưa cấu hình Telegram, không có email công ty nhận, gửi mail lỗi, đã gửi ngày báo cáo đó. |

## UC18 - Tra cứu hóa đơn công khai

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Khách chưa đăng nhập. |
| Điều kiện trước | Có thông tin tra cứu hợp lệ. |
| Luồng chính | Người dùng nhập thông tin tra cứu, hệ thống tìm hóa đơn public phù hợp và trả dữ liệu xem/tải. |
| Kết quả | Người dùng xem được hóa đơn hợp lệ. |
| Ngoại lệ | Không tìm thấy hóa đơn, thông tin tra cứu sai, hóa đơn không được phép public. |
