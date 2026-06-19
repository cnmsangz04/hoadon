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

## UC01 - Đăng ký công ty

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Khách chưa đăng nhập. |
| Điều kiện trước | Người đăng ký có thông tin công ty, mã số thuế, email và thông tin tài khoản admin công ty. |
| Luồng chính | Khách mở màn đăng ký, nhập thông tin công ty và tài khoản quản trị ban đầu; backend kiểm tra dữ liệu bắt buộc, mã số thuế, email và hồ sơ đang chờ; hệ thống lưu hồ sơ vào `company_registration_requests`. |
| Kết quả | Hồ sơ đăng ký ở trạng thái chờ duyệt để admin hệ thống xử lý. |
| Ngoại lệ | Thiếu dữ liệu bắt buộc, mã số thuế đã tồn tại, đã có hồ sơ đang chờ duyệt, email sai định dạng hoặc lỗi gửi thông báo. |

## UC02 - Duyệt hồ sơ đăng ký công ty

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Admin hệ thống. |
| Điều kiện trước | Có hồ sơ đăng ký công ty đang chờ duyệt và admin có quyền quản lý công ty. |
| Luồng chính | Admin xem danh sách hồ sơ, kiểm tra thông tin, duyệt hồ sơ; hệ thống tạo công ty, tài khoản admin công ty, cập nhật trạng thái hồ sơ và có thể gửi thông tin đăng nhập qua email. |
| Kết quả | Công ty và tài khoản admin công ty được tạo, hồ sơ chuyển sang trạng thái đã duyệt. |
| Ngoại lệ | Hồ sơ không tồn tại, hồ sơ đã được xử lý, dữ liệu đăng ký trùng công ty hiện có, thiếu quyền hoặc gửi email thất bại. |

## UC03 - Đăng nhập hệ thống

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Người dùng hoặc admin. |
| Điều kiện trước | Tài khoản tồn tại, đang hoạt động. |
| Luồng chính | Người dùng nhập tài khoản/mật khẩu, frontend gửi request, controller gọi service, service kiểm tra user, mật khẩu, trạng thái, IP, phiên đăng nhập, sau đó trả JWT. |
| Kết quả | Người dùng vào trang phù hợp với vai trò. |
| Ngoại lệ | Sai mật khẩu, user bị khóa, công ty chưa hợp lệ, IP không được phép, phiên lỗi. |

## UC04 - Quản lý thành viên và phân quyền

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Admin công ty. |
| Điều kiện trước | Admin công ty đã đăng nhập, công ty hợp lệ, có quyền `setting-member-list`, `setting-member-save` hoặc `setting-member-manage` tùy thao tác. |
| Luồng chính | Admin xem danh sách thành viên, thêm hoặc cập nhật thông tin thành viên, gán quyền chi tiết, khóa/mở khóa, reset mật khẩu hoặc gửi thông tin đăng nhập. |
| Kết quả | Thành viên và quyền trong công ty được cập nhật đúng phạm vi công ty. |
| Ngoại lệ | Thiếu quyền, username/email không hợp lệ, thao tác vượt phạm vi công ty, admin thường thao tác admin khác không được phép hoặc gửi email thất bại. |

## UC05 - Quản lý hồ sơ công ty

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Admin công ty. |
| Điều kiện trước | Có quyền `setting-profile` và công ty hiện tại xác định được từ JWT. |
| Luồng chính | Admin cập nhật thông tin công ty, người đại diện, thông tin hóa đơn, liên hệ, ngân hàng, cơ quan thuế, logo và favicon. |
| Kết quả | Hồ sơ công ty được lưu và dùng cho hóa đơn, email, mẫu hiển thị và tích hợp cơ quan thuế. |
| Ngoại lệ | Thiếu quyền, thiếu dữ liệu bắt buộc, mã số thuế/email/số điện thoại sai định dạng, file ảnh không hợp lệ hoặc không xác định được công ty. |

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

## UC08 - Tạo và gửi tờ khai

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền `register-invoice-save` khi tạo/sửa và `register-invoice-send` khi gửi. Công ty có thông tin hồ sơ cần thiết. |
| Luồng chính | Người dùng tạo tờ khai đăng ký sử dụng hóa đơn điện tử, hệ thống lấy dữ liệu công ty để điền sẵn, lưu tờ khai, ký/gửi tờ khai và ghi nhận lịch sử phản hồi. |
| Kết quả | Tờ khai được lưu hoặc gửi sang cơ quan thuế theo trạng thái xử lý. |
| Ngoại lệ | Thiếu quyền, thiếu thông tin công ty, XML tờ khai không hợp lệ, tờ khai không thuộc công ty hiện tại hoặc phản hồi cơ quan thuế thất bại. |

## UC09 - Quản lý mẫu hóa đơn

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền `form-invoice-list` để xem hoặc `form-invoice-save` để tạo/sửa/xóa. |
| Luồng chính | Người dùng xem mẫu hệ thống, chọn mẫu để sao chép về công ty, cập nhật ký hiệu, loại mẫu, ảnh xem trước, file XSLT, trạng thái và kích hoạt mẫu cần dùng. |
| Kết quả | Mẫu hóa đơn thuộc công ty được tạo/cập nhật và có thể dùng khi lập hóa đơn. |
| Ngoại lệ | Thiếu quyền, thiếu file mẫu, mẫu không thuộc công ty, loại mẫu không phù hợp, file XSLT/ảnh không hợp lệ hoặc không thể kích hoạt mẫu. |

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

## UC12 - Gửi hóa đơn qua email

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Hóa đơn tồn tại, thuộc công ty hiện tại, người nhận có email hợp lệ, công ty có cấu hình SMTP hoặc có cấu hình mail phù hợp. |
| Luồng chính | Người dùng chọn gửi email hóa đơn; hệ thống lấy mẫu mail, thay biến dữ liệu, tạo job trong `mail_jobs`; worker gửi mail và cập nhật trạng thái. |
| Kết quả | Email hóa đơn được đưa vào hàng đợi và gửi thành công hoặc ghi nhận lỗi để retry. |
| Ngoại lệ | Thiếu email người nhận, thiếu template, SMTP sai, file đính kèm không sinh được, thiếu quyền hoặc job gửi quá số lần retry. |

## UC13 - Import hóa đơn từ Excel

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền `invoice-save`. |
| Luồng chính | Người dùng tải mẫu Excel, nhập dữ liệu, upload file, hệ thống đọc file, gom dòng theo hóa đơn, kiểm tra dữ liệu và tạo hóa đơn nháp. |
| Kết quả | Hóa đơn được import hoặc danh sách lỗi được trả về. |
| Ngoại lệ | File sai định dạng, thiếu thông tin bắt buộc, dữ liệu tiền/thuế sai, không gom được dòng hàng. |

## UC14 - Xem báo cáo hóa đơn

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền `report-invoice` để xem và `report-invoice-export` để xuất Excel. |
| Luồng chính | Người dùng chọn bộ lọc báo cáo, hệ thống truy vấn hóa đơn trong công ty hiện tại, trả danh sách/tổng hợp và cho phép xuất file Excel khi có quyền. |
| Kết quả | Người dùng xem hoặc tải được báo cáo hóa đơn đúng phạm vi công ty. |
| Ngoại lệ | Thiếu quyền, bộ lọc ngày không hợp lệ, không có dữ liệu, lỗi xuất Excel hoặc truy cập dữ liệu công ty khác. |

## UC15 - Mua gói hóa đơn

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền `invoice-package-purchase`; công ty có thể đang hoạt động hoặc đang chờ kích hoạt nhưng được phép mua gói. |
| Luồng chính | Người dùng xem gói active, chọn gói và phương thức thanh toán; hệ thống tạo giao dịch mua, chuyển người dùng sang cổng thanh toán hoặc xử lý thanh toán giả lập; callback/return cập nhật trạng thái và cộng hạn mức. |
| Kết quả | Giao dịch mua được lưu, hạn mức hóa đơn được cộng khi thanh toán thành công, lịch sử mua có thể xem lại. |
| Ngoại lệ | Gói không tồn tại hoặc không active, cổng thanh toán lỗi, callback sai chữ ký, số tiền không khớp, giao dịch đã xử lý hoặc thiếu quyền. |

## UC16 - Quản lý danh mục nền

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Admin hệ thống. |
| Điều kiện trước | Admin có quyền tương ứng: `bank-*`, `tax-authority-*`, `vat-rate-*`. |
| Luồng chính | Admin quản lý ngân hàng, cơ quan thuế và thuế suất; hệ thống lưu trạng thái, thứ tự ưu tiên và dữ liệu nền để các form nghiệp vụ sử dụng. |
| Kết quả | Danh mục nền được cập nhật và hiển thị ở các màn hình liên quan. |
| Ngoại lệ | Thiếu quyền, mã/tên dữ liệu nền không hợp lệ, dữ liệu đang được tham chiếu nên không thể xóa hoặc thao tác vượt phạm vi quản trị. |

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

## UC19 - Import danh mục khách hàng/sản phẩm từ Excel

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền import hoặc quyền lưu danh mục tương ứng. |
| Luồng chính | Người dùng tải mẫu Excel, nhập danh mục, upload file; hệ thống kiểm tra trùng mã trong file, tạo mới hoặc cập nhật bản ghi theo mã trong cùng công ty. |
| Kết quả | Khách hàng hoặc sản phẩm được import, lịch sử import được lưu. |
| Ngoại lệ | File sai định dạng, thiếu mã bắt buộc, mã bị lặp trong file, dữ liệu sản phẩm sai đơn giá/thuế suất hoặc thiếu quyền. |
