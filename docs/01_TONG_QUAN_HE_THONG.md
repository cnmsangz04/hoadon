# Tổng Quan Hệ Thống

Tài liệu gom các phần tổng quan, yêu cầu, use case, luồng nghiệp vụ và mô tả chi tiết chương trình.

## Nguồn đã hợp nhất

- `TONG_QUAN.md`
- `YEU_CAU_HE_THONG.md`
- `USE_CASE.md`
- `LUONG_NGHIEP_VU.md`
- `MO_TA_CHI_TIET_CHUONG_TRINH.md`

## Nội dung

### Từ `TONG_QUAN.md`

Cập nhật: 19/06/2026.

### Mục tiêu

Chương trình hỗ trợ doanh nghiệp quản lý vòng đời hóa đơn điện tử từ lúc đăng ký sử dụng, tạo mẫu hóa đơn, lập hóa đơn GTGT, ký số, gửi cơ quan thuế, gửi email cho khách hàng, tra cứu hóa đơn, đến báo cáo và theo dõi lịch sử xử lý.

Phần quản trị hệ thống hỗ trợ duyệt đăng ký công ty, quản lý danh mục nền, gói hóa đơn, email, Telegram, báo cáo hóa đơn ngày và phân quyền.

### Nhóm người dùng

| Nhóm | Vai trò chính |
| --- | --- |
| Khách chưa đăng nhập | Đăng ký công ty, đăng nhập, quên mật khẩu, tra cứu hóa đơn công khai. |
| Nhân viên công ty | Lập hóa đơn, import hóa đơn, import danh mục khách hàng/sản phẩm, quản lý khách hàng, sản phẩm, báo cáo theo quyền được cấp. |
| Admin công ty | Quản lý hồ sơ công ty, thành viên, phân quyền nội bộ, cấu hình bảo mật và email. |
| Admin công ty root | Vào khu quản trị, xử lý danh mục và cấu hình cấp hệ thống theo quyền. |
| Root | Toàn quyền hệ thống, quản lý dữ liệu nhiều công ty và cấu hình lõi. |

Chi tiết quyền và phạm vi dữ liệu nằm trong [04_PHAN_QUYEN_VA_VALIDATE.md](04_PHAN_QUYEN_VA_VALIDATE.md).

### Nhóm chức năng

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
| Thanh toán | Gói hóa đơn, mua hóa đơn, lịch sử mua, thanh toán MoMo, VNPAY và ZaloPay sandbox. Chi tiết tích hợp nằm trong [06_THANH_TOAN.md](06_THANH_TOAN.md). |
| Công khai | Tra cứu hóa đơn bằng mã tra cứu hoặc thông tin hóa đơn. |

### Nguyên tắc quan trọng

- Hầu hết dữ liệu nghiệp vụ phải gắn với `company_id`.
- Mã khách hàng và mã sản phẩm không được trùng trong cùng công ty khi thêm mới hoặc đổi mã thủ công; riêng import danh mục dùng mã để cập nhật bản ghi đã tồn tại.
- Root có thể xem hoặc thao tác nhiều công ty khi API cho phép, nhưng dữ liệu công ty thường không được ghi nhầm về công ty root.
- Lịch sử gửi mail và job mail phải nằm theo công ty phát sinh nội dung.
- Báo cáo hóa đơn ngày gửi Telegram có thể thông báo cả khi không có dữ liệu, nhưng email chỉ gửi khi có dữ liệu cần báo cáo.
- XML hóa đơn đã ký phải lấy đúng theo hóa đơn và công ty, tránh dùng nhầm XML của công ty khác.
- Cấu hình thanh toán MoMo và ZaloPay hiện dùng bộ key sandbox/demo public của nhà cung cấp để cá nhân/tổ chức kiểm thử tích hợp; khi triển khai thật phải thay bằng key merchant riêng qua biến môi trường và domain callback public.

### Từ `YEU_CAU_HE_THONG.md`

Cập nhật: 19/06/2026.

Tài liệu này mô tả yêu cầu chức năng và phi chức năng của hệ thống hóa đơn điện tử, phục vụ phần khảo sát và phân tích yêu cầu trong báo cáo.

### Tác nhân

| Tác nhân | Mô tả |
| --- | --- |
| Khách chưa đăng nhập | Đăng ký công ty, đăng nhập, quên mật khẩu, tra cứu hóa đơn công khai. |
| Nhân viên công ty | Thao tác nghiệp vụ theo quyền được cấp. |
| Admin công ty | Quản lý hồ sơ công ty, thành viên, phân quyền và cấu hình trong phạm vi công ty. |
| Admin công ty root | Truy cập khu quản trị hệ thống theo quyền. |
| Root | Toàn quyền hệ thống. |
| Hệ thống nền | Scheduler, worker mail, callback thanh toán. |

### Yêu cầu chức năng

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

### Yêu cầu phi chức năng

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

### Ràng buộc nghiệp vụ quan trọng

- Công ty chưa kích hoạt bị hạn chế chức năng nghiệp vụ.
- Hóa đơn phải thuộc công ty hiện tại, không được thao tác hóa đơn công ty khác.
- Khách hàng và sản phẩm phải thuộc công ty hiện tại; mã khách hàng/mã sản phẩm không được trùng khi thêm mới hoặc đổi mã thủ công.
- Import khách hàng/sản phẩm dùng mã để cập nhật bản ghi đã có trong công ty, nhưng không chấp nhận mã bị lặp trong cùng file import.
- Hóa đơn đã phát hành không được sửa như hóa đơn nháp.
- Hóa đơn thay thế/điều chỉnh phải tham chiếu hóa đơn gốc.
- Email báo cáo ngày chỉ gửi khi có dữ liệu.
- Telegram báo cáo ngày có thể gửi thông báo không có hóa đơn cần kiểm tra.
- XML ký và XML gửi cơ quan thuế phải lấy đúng theo hóa đơn và công ty.

### Từ `USE_CASE.md`

Cập nhật: 19/06/2026.

Tài liệu này tóm tắt các use case chính để dùng trong phần phân tích thiết kế và báo cáo.

### Danh sách use case

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

### UC01 - Đăng ký công ty

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Khách chưa đăng nhập. |
| Điều kiện trước | Người đăng ký có thông tin công ty, mã số thuế, email và thông tin tài khoản admin công ty. |
| Luồng chính | Khách mở màn đăng ký, nhập thông tin công ty và tài khoản quản trị ban đầu; backend kiểm tra dữ liệu bắt buộc, mã số thuế, email và hồ sơ đang chờ; hệ thống lưu hồ sơ vào `company_registration_requests`. |
| Kết quả | Hồ sơ đăng ký ở trạng thái chờ duyệt để admin hệ thống xử lý. |
| Ngoại lệ | Thiếu dữ liệu bắt buộc, mã số thuế đã tồn tại, đã có hồ sơ đang chờ duyệt, email sai định dạng hoặc lỗi gửi thông báo. |

### UC02 - Duyệt hồ sơ đăng ký công ty

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Admin hệ thống. |
| Điều kiện trước | Có hồ sơ đăng ký công ty đang chờ duyệt và admin có quyền quản lý công ty. |
| Luồng chính | Admin xem danh sách hồ sơ, kiểm tra thông tin, duyệt hồ sơ; hệ thống tạo công ty, tài khoản admin công ty, cập nhật trạng thái hồ sơ và có thể gửi thông tin đăng nhập qua email. |
| Kết quả | Công ty và tài khoản admin công ty được tạo, hồ sơ chuyển sang trạng thái đã duyệt. |
| Ngoại lệ | Hồ sơ không tồn tại, hồ sơ đã được xử lý, dữ liệu đăng ký trùng công ty hiện có, thiếu quyền hoặc gửi email thất bại. |

### UC03 - Đăng nhập hệ thống

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Người dùng hoặc admin. |
| Điều kiện trước | Tài khoản tồn tại, đang hoạt động. |
| Luồng chính | Người dùng nhập tài khoản/mật khẩu, frontend gửi request, controller gọi service, service kiểm tra user, mật khẩu, trạng thái, IP, phiên đăng nhập, sau đó trả JWT. |
| Kết quả | Người dùng vào trang phù hợp với vai trò. |
| Ngoại lệ | Sai mật khẩu, user bị khóa, công ty chưa hợp lệ, IP không được phép, phiên lỗi. |

### UC04 - Quản lý thành viên và phân quyền

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Admin công ty. |
| Điều kiện trước | Admin công ty đã đăng nhập, công ty hợp lệ, có quyền `setting-member-list`, `setting-member-save` hoặc `setting-member-manage` tùy thao tác. |
| Luồng chính | Admin xem danh sách thành viên, thêm hoặc cập nhật thông tin thành viên, gán quyền chi tiết, khóa/mở khóa, reset mật khẩu hoặc gửi thông tin đăng nhập. |
| Kết quả | Thành viên và quyền trong công ty được cập nhật đúng phạm vi công ty. |
| Ngoại lệ | Thiếu quyền, username/email không hợp lệ, thao tác vượt phạm vi công ty, admin thường thao tác admin khác không được phép hoặc gửi email thất bại. |

### UC05 - Quản lý hồ sơ công ty

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Admin công ty. |
| Điều kiện trước | Có quyền `setting-profile` và công ty hiện tại xác định được từ JWT. |
| Luồng chính | Admin cập nhật thông tin công ty, người đại diện, thông tin hóa đơn, liên hệ, ngân hàng, cơ quan thuế, logo và favicon. |
| Kết quả | Hồ sơ công ty được lưu và dùng cho hóa đơn, email, mẫu hiển thị và tích hợp cơ quan thuế. |
| Ngoại lệ | Thiếu quyền, thiếu dữ liệu bắt buộc, mã số thuế/email/số điện thoại sai định dạng, file ảnh không hợp lệ hoặc không xác định được công ty. |

### UC06 - Quản lý khách hàng

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền xem hoặc lưu danh mục khách hàng. |
| Luồng chính | Người dùng xem danh sách, tìm kiếm, thêm, cập nhật, đổi mã khách hàng hoặc xóa khách hàng trong công ty hiện tại. |
| Kết quả | Danh mục khách hàng được cập nhật và dùng để nạp nhanh thông tin bên mua khi lập hóa đơn. |
| Ngoại lệ | Mã khách hàng bị trùng trong cùng công ty, thiếu dữ liệu bắt buộc, thiếu quyền hoặc truy cập dữ liệu công ty khác. |

### UC07 - Quản lý sản phẩm

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền xem hoặc lưu danh mục sản phẩm. |
| Luồng chính | Người dùng xem danh sách, tìm kiếm, thêm, cập nhật, đổi mã sản phẩm hoặc xóa sản phẩm trong công ty hiện tại. |
| Kết quả | Danh mục sản phẩm được cập nhật và dùng để nạp nhanh dòng hàng khi lập hóa đơn. |
| Ngoại lệ | Mã sản phẩm bị trùng trong cùng công ty, thiếu dữ liệu bắt buộc, thiếu quyền hoặc truy cập dữ liệu công ty khác. |

### UC08 - Tạo và gửi tờ khai

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền `register-invoice-save` khi tạo/sửa và `register-invoice-send` khi gửi. Công ty có thông tin hồ sơ cần thiết. |
| Luồng chính | Người dùng tạo tờ khai đăng ký sử dụng hóa đơn điện tử, hệ thống lấy dữ liệu công ty để điền sẵn, lưu tờ khai, ký/gửi tờ khai và ghi nhận lịch sử phản hồi. |
| Kết quả | Tờ khai được lưu hoặc gửi sang cơ quan thuế theo trạng thái xử lý. |
| Ngoại lệ | Thiếu quyền, thiếu thông tin công ty, XML tờ khai không hợp lệ, tờ khai không thuộc công ty hiện tại hoặc phản hồi cơ quan thuế thất bại. |

### UC09 - Quản lý mẫu hóa đơn

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền `form-invoice-list` để xem hoặc `form-invoice-save` để tạo/sửa/xóa. |
| Luồng chính | Người dùng xem mẫu hệ thống, chọn mẫu để sao chép về công ty, cập nhật ký hiệu, loại mẫu, ảnh xem trước, file XSLT, trạng thái và kích hoạt mẫu cần dùng. |
| Kết quả | Mẫu hóa đơn thuộc công ty được tạo/cập nhật và có thể dùng khi lập hóa đơn. |
| Ngoại lệ | Thiếu quyền, thiếu file mẫu, mẫu không thuộc công ty, loại mẫu không phù hợp, file XSLT/ảnh không hợp lệ hoặc không thể kích hoạt mẫu. |

### UC10 - Lập hóa đơn GTGT

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền lập hóa đơn, công ty có mẫu hóa đơn hợp lệ, có dải số hoặc điều kiện phát hành phù hợp. |
| Luồng chính | Người dùng nhập thông tin người mua và dòng hàng, hệ thống kiểm tra dữ liệu, tính tiền, lưu hóa đơn ở trạng thái mới khởi tạo. |
| Kết quả | Hóa đơn nháp được tạo. |
| Ngoại lệ | Thiếu quyền, thiếu mẫu hóa đơn, dữ liệu dòng hàng sai, thuế suất không hợp lệ. |

### UC11 - Ký và phát hành hóa đơn

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Hóa đơn tồn tại, thuộc công ty hiện tại, trạng thái cho phép ký/phát hành. |
| Luồng chính | Người dùng ký hóa đơn, hệ thống sinh XML, lưu chữ ký, gửi cơ quan thuế nếu cần, cập nhật trạng thái. |
| Kết quả | Hóa đơn được ký hoặc phát hành theo trạng thái phản hồi. |
| Ngoại lệ | XML không hợp lệ, chữ ký lỗi, phản hồi cơ quan thuế không thành công, hóa đơn không thuộc công ty. |

### UC12 - Gửi hóa đơn qua email

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Hóa đơn tồn tại, thuộc công ty hiện tại, người nhận có email hợp lệ, công ty có cấu hình SMTP hoặc có cấu hình mail phù hợp. |
| Luồng chính | Người dùng chọn gửi email hóa đơn; hệ thống lấy mẫu mail, thay biến dữ liệu, tạo job trong `mail_jobs`; worker gửi mail và cập nhật trạng thái. |
| Kết quả | Email hóa đơn được đưa vào hàng đợi và gửi thành công hoặc ghi nhận lỗi để retry. |
| Ngoại lệ | Thiếu email người nhận, thiếu template, SMTP sai, file đính kèm không sinh được, thiếu quyền hoặc job gửi quá số lần retry. |

### UC13 - Import hóa đơn từ Excel

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền `invoice-save`. |
| Luồng chính | Người dùng tải mẫu Excel, nhập dữ liệu, upload file, hệ thống đọc file, gom dòng theo hóa đơn, kiểm tra dữ liệu và tạo hóa đơn nháp. |
| Kết quả | Hóa đơn được import hoặc danh sách lỗi được trả về. |
| Ngoại lệ | File sai định dạng, thiếu thông tin bắt buộc, dữ liệu tiền/thuế sai, không gom được dòng hàng. |

### UC14 - Xem báo cáo hóa đơn

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền `report-invoice` để xem và `report-invoice-export` để xuất Excel. |
| Luồng chính | Người dùng chọn bộ lọc báo cáo, hệ thống truy vấn hóa đơn trong công ty hiện tại, trả danh sách/tổng hợp và cho phép xuất file Excel khi có quyền. |
| Kết quả | Người dùng xem hoặc tải được báo cáo hóa đơn đúng phạm vi công ty. |
| Ngoại lệ | Thiếu quyền, bộ lọc ngày không hợp lệ, không có dữ liệu, lỗi xuất Excel hoặc truy cập dữ liệu công ty khác. |

### UC15 - Mua gói hóa đơn

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền `invoice-package-purchase`; công ty có thể đang hoạt động hoặc đang chờ kích hoạt nhưng được phép mua gói. |
| Luồng chính | Người dùng xem gói active, chọn gói và phương thức thanh toán; hệ thống tạo giao dịch mua, chuyển người dùng sang cổng thanh toán hoặc xử lý thanh toán giả lập; callback/return cập nhật trạng thái và cộng hạn mức. |
| Kết quả | Giao dịch mua được lưu, hạn mức hóa đơn được cộng khi thanh toán thành công, lịch sử mua có thể xem lại. |
| Ngoại lệ | Gói không tồn tại hoặc không active, cổng thanh toán lỗi, callback sai chữ ký, số tiền không khớp, giao dịch đã xử lý hoặc thiếu quyền. |

### UC16 - Quản lý danh mục nền

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Admin hệ thống. |
| Điều kiện trước | Admin có quyền tương ứng: `bank-*`, `tax-authority-*`, `vat-rate-*`. |
| Luồng chính | Admin quản lý ngân hàng, cơ quan thuế và thuế suất; hệ thống lưu trạng thái, thứ tự ưu tiên và dữ liệu nền để các form nghiệp vụ sử dụng. |
| Kết quả | Danh mục nền được cập nhật và hiển thị ở các màn hình liên quan. |
| Ngoại lệ | Thiếu quyền, mã/tên dữ liệu nền không hợp lệ, dữ liệu đang được tham chiếu nên không thể xóa hoặc thao tác vượt phạm vi quản trị. |

### UC17 - Cấu hình báo cáo hóa đơn ngày

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Admin hệ thống. |
| Điều kiện trước | Có quyền quản lý cấu hình tương ứng. |
| Luồng chính | Admin bật lịch gửi, chọn giờ/phút, hệ thống lưu cấu hình, scheduler tự gửi báo cáo ngày hôm qua khi đến giờ. |
| Kết quả | Báo cáo được gửi qua Telegram và email nếu có dữ liệu. |
| Ngoại lệ | Chưa cấu hình Telegram, không có email công ty nhận, gửi mail lỗi, đã gửi ngày báo cáo đó. |

### UC18 - Tra cứu hóa đơn công khai

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Khách chưa đăng nhập. |
| Điều kiện trước | Có thông tin tra cứu hợp lệ. |
| Luồng chính | Người dùng nhập thông tin tra cứu, hệ thống tìm hóa đơn public phù hợp và trả dữ liệu xem/tải. |
| Kết quả | Người dùng xem được hóa đơn hợp lệ. |
| Ngoại lệ | Không tìm thấy hóa đơn, thông tin tra cứu sai, hóa đơn không được phép public. |

### UC19 - Import danh mục khách hàng/sản phẩm từ Excel

| Mục | Nội dung |
| --- | --- |
| Tác nhân | Nhân viên công ty. |
| Điều kiện trước | Có quyền import hoặc quyền lưu danh mục tương ứng. |
| Luồng chính | Người dùng tải mẫu Excel, nhập danh mục, upload file; hệ thống kiểm tra trùng mã trong file, tạo mới hoặc cập nhật bản ghi theo mã trong cùng công ty. |
| Kết quả | Khách hàng hoặc sản phẩm được import, lịch sử import được lưu. |
| Ngoại lệ | File sai định dạng, thiếu mã bắt buộc, mã bị lặp trong file, dữ liệu sản phẩm sai đơn giá/thuế suất hoặc thiếu quyền. |

### Từ `LUONG_NGHIEP_VU.md`

Cập nhật: 19/06/2026.

### Đăng ký công ty

1. Khách đăng ký công ty ở màn hình auth.
2. Backend kiểm tra mã số thuế, email và dữ liệu bắt buộc.
3. Hồ sơ chờ duyệt được lưu vào `company_registration_requests`.
4. Admin duyệt hồ sơ trong khu quản trị.
5. Hệ thống tạo công ty, tài khoản admin công ty và dữ liệu nền cần thiết.

### Đăng nhập và phân quyền

1. Người dùng đăng nhập bằng tài khoản thường hoặc admin.
2. Backend xác thực mật khẩu, trạng thái user và bảo mật IP nếu có.
3. Hệ thống tạo phiên trong `login_sessions`.
4. JWT trả về frontend và được lưu vào `token` hoặc `token-admin`.
5. Mỗi API nghiệp vụ kiểm tra token, phiên, quyền và phạm vi công ty.

### Mẫu hóa đơn

1. Công ty tạo hoặc chọn mẫu hóa đơn.
2. Mẫu lưu trong `form_invoices`, gồm thông tin loại mẫu, ký hiệu, file XSLT và ảnh xem trước nếu có.
3. Khi lập hóa đơn, hệ thống lấy mẫu đang dùng để sinh XML/PDF và hiển thị.
4. Với hóa đơn GTGT mẫu một thuế suất, cần đảm bảo dữ liệu dòng hàng và tổng thuế phù hợp `form_invoices.type = 1`.

### Lập hóa đơn GTGT

1. Người dùng nhập thông tin người mua, hình thức thanh toán và dòng hàng hóa.
2. Backend kiểm tra dải số, mẫu hóa đơn, dữ liệu dòng hàng, thuế suất và quyền thao tác.
3. Hóa đơn lưu trong `invoices`, dòng hàng hoặc dữ liệu chi tiết lưu theo cấu trúc hiện tại của entity.
4. Khi phát hành, hệ thống sinh XML, ký số và cập nhật trạng thái hóa đơn.
5. XML đã ký lưu trong `signature_vats` theo hóa đơn và công ty.

### Gửi cơ quan thuế

1. Người dùng gửi hóa đơn hoặc tờ khai sang cơ quan thuế.
2. Backend sinh XML theo định dạng tương ứng.
3. Dữ liệu gửi/nhận lưu trong `signature_authorities_tax` hoặc bảng liên quan.
4. Trạng thái hóa đơn cập nhật theo phản hồi.
5. Khi xem lại XML hoặc PDF, hệ thống phải lấy đúng dữ liệu theo hóa đơn và công ty.

### Import hóa đơn từ Excel

1. Người dùng tải một mẫu Excel import dùng chung.
2. Người dùng nhập thông tin hóa đơn và nhiều dòng hàng.
3. Backend đọc file bằng Apache POI.
4. Hệ thống gom dòng theo hóa đơn, kiểm tra dữ liệu bắt buộc, thuế suất, tổng tiền và loại mẫu.
5. Hóa đơn hợp lệ được tạo trong hệ thống; dòng lỗi được trả về để người dùng sửa.

Mẫu import cần đủ hướng dẫn để người dùng hiểu cách nhập dù công ty đang dùng mẫu nhiều thuế suất hay một thuế suất.

### Import danh mục khách hàng/sản phẩm

1. Người dùng vào `/imports/customer` hoặc `/imports/product`.
2. Người dùng tải mẫu Excel tương ứng, nhập dữ liệu và upload file.
3. Backend đọc file bằng Apache POI, kiểm tra dữ liệu bắt buộc và kiểm tra mã bị lặp trong cùng file.
4. Với khách hàng, mã khách hàng là khóa nhận diện để tạo mới hoặc cập nhật bản ghi đã tồn tại trong cùng công ty.
5. Với sản phẩm, mã sản phẩm là khóa nhận diện để tạo mới hoặc cập nhật bản ghi đã tồn tại trong cùng công ty.
6. Hệ thống lưu lịch sử import và cho phép import lại file cũ khi cần.

Danh mục khách hàng/sản phẩm có nút đi tới import tương ứng, và trang import có nút đi tới danh sách cùng loại để thao tác nhanh.

### Gửi email hóa đơn

1. Người dùng hoặc hệ thống tạo yêu cầu gửi email.
2. Nội dung lấy từ `mail_templates` và dữ liệu nghiệp vụ.
3. Job lưu vào `mail_jobs` theo công ty phát sinh nội dung.
4. Worker gửi mail qua SMTP của công ty hoặc cấu hình phù hợp.
5. Trạng thái gửi, số lần gửi và lỗi được cập nhật trong `mail_jobs`.

### Báo cáo hóa đơn ngày

1. Admin cấu hình lịch gửi trong màn báo cáo hóa đơn ngày.
2. Lịch lưu ở `daily_invoice_report_configs`.
3. Scheduler kiểm tra đến giờ gửi và ngày báo cáo chưa được gửi.
4. Hệ thống gom dữ liệu hóa đơn cần kiểm tra theo từng công ty.
5. Telegram gửi thông báo vào group phù hợp, kể cả trường hợp không có dữ liệu.
6. Email chỉ tạo khi có dữ liệu báo cáo.
7. Mail job và lịch sử gửi phải nằm theo công ty nhận nội dung báo cáo.

### Mua gói hóa đơn

1. Người dùng chọn gói hóa đơn.
2. Hệ thống tạo giao dịch mua trong bảng mua gói.
3. Người dùng thanh toán qua MoMo, VNPAY hoặc ZaloPay nếu chọn cổng thanh toán.
4. Sau khi thanh toán thành công, hạn mức hóa đơn được cộng vào công ty.
5. Lịch sử mua và thanh toán được lưu để đối soát.

### Tra cứu hóa đơn công khai

1. Người dùng ngoài hệ thống nhập thông tin tra cứu.
2. Backend tìm hóa đơn theo mã tra cứu hoặc điều kiện public được hỗ trợ.
3. Nếu hợp lệ, hệ thống trả thông tin hóa đơn hoặc file xem/tải.
4. API public không được làm lộ dữ liệu công ty khác ngoài hóa đơn được tra cứu hợp lệ.

### Từ `MO_TA_CHI_TIET_CHUONG_TRINH.md`

Tài liệu này mô tả tổng quan hệ thống, các nhóm người dùng, chức năng nghiệp vụ, luồng xử lý chính, dữ liệu quan trọng và các thành phần kỹ thuật của chương trình hóa đơn điện tử.

Cập nhật gần nhất: 19/06/2026.

### 1. Mục Đích Chương Trình

Chương trình là hệ thống quản lý hóa đơn điện tử dành cho doanh nghiệp và bộ phận quản trị hệ thống. Hệ thống hỗ trợ doanh nghiệp đăng ký sử dụng, quản lý hồ sơ công ty, quản lý mẫu hóa đơn, lập hóa đơn GTGT, ký và gửi hóa đơn đến cơ quan thuế, gửi email hóa đơn cho khách mua, mua thêm gói hóa đơn, báo cáo thống kê và tra cứu hóa đơn công khai.

Ở phía quản trị, hệ thống hỗ trợ duyệt đăng ký công ty, quản lý công ty, quản lý gói hóa đơn, quản lý hạn mức hóa đơn, quản lý danh mục nền, quản lý phân quyền, theo dõi trạng thái gửi email, cấu hình Telegram, cấu hình báo cáo hóa đơn ngày và gửi báo cáo tự động.

Tài liệu nền bổ sung:

- [README.md](README.md): điểm vào tài liệu dự án.
- [02_KIEN_TRUC_KY_THUAT.md](02_KIEN_TRUC_KY_THUAT.md): mô hình kiến trúc 3 tầng, Spring MVC REST và backend nhiều lớp.
- [02_KIEN_TRUC_KY_THUAT.md](02_KIEN_TRUC_KY_THUAT.md): nhóm bảng và nguyên tắc dữ liệu nhiều công ty.
- [02_KIEN_TRUC_KY_THUAT.md](02_KIEN_TRUC_KY_THUAT.md): cấu trúc thư mục.
- [01_TONG_QUAN_HE_THONG.md](01_TONG_QUAN_HE_THONG.md): các luồng nghiệp vụ chính.

### 2. Công Nghệ Sử Dụng

#### 2.1 Mô Hình Kiến Trúc

Chương trình là ứng dụng web 3 tầng:

- Tầng trình bày: frontend Vue 2.
- Tầng ứng dụng/nghiệp vụ: backend Spring Boot.
- Tầng dữ liệu: SQL Server.

Backend dùng Spring MVC theo kiểu REST API. Controller nhận request và trả JSON; phần view chính nằm ở frontend Vue, không phải render view server truyền thống.

Tổ chức backend theo mô hình nhiều lớp:

- Controller: nhận request, đọc tham số, gọi service.
- Service: xử lý nghiệp vụ, kiểm tra quyền, kiểm tra phạm vi công ty.
- Repository: truy vấn database.
- Entity/DTO: mô tả dữ liệu lưu trữ và dữ liệu trao đổi.
- Nhóm hạ tầng: Security, Auth, Config, Util, Worker.

Nếu mô tả theo mô hình 03 lớp trong Java thì backend đang áp dụng:

- Presentation layer: frontend Vue hiển thị giao diện và gửi request; backend `controllers` nhận request, kiểm tra dữ liệu đầu vào cơ bản và trả response.
- Business logic layer: `services`, `services/impl` xử lý nghiệp vụ, kiểm tra quyền, vai trò, phạm vi công ty và trạng thái dữ liệu.
- Data access layer: `repositories`, `entity` và SQL Server kết nối, query, lưu dữ liệu rồi chuyển kết quả cho lớp xử lý nghiệp vụ.

Ví dụ đăng nhập: người dùng nhập tài khoản/mật khẩu ở Vue, frontend gửi request xuống controller, controller gọi service xác thực, service dùng repository query database để kiểm tra user, mật khẩu, trạng thái và vai trò. Nếu hợp lệ, hệ thống tạo phiên đăng nhập, trả JWT và frontend điều hướng vào trang phù hợp.

Nếu mô tả theo triển khai tổng thể thì hệ thống là 3 tầng: frontend Vue, backend Spring Boot và SQL Server.

Khi mô tả ngắn gọn có thể nói: hệ thống là ứng dụng web 3 tầng; riêng backend Java áp dụng mô hình 03 lớp trên nền Spring MVC REST.

#### 2.2 Backend

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

#### 2.3 Frontend

- Vue 2.
- Vue Router.
- BootstrapVue.
- Axios.
- Toastr.
- vue-select.
- TinyMCE.
- Font Awesome.
- Vue Advanced Cropper cho avatar/logo.
- vuedraggable cho các màn hình cần sắp xếp thứ tự bằng kéo thả.

#### 2.4 Lưu Trữ File

Hệ thống lưu file upload trong thư mục `uploads`, gồm:

- Logo công ty.
- Favicon công ty.
- Avatar người dùng.
- Ảnh đại diện mẫu hóa đơn.
- File XSLT mẫu hóa đơn.
- File import hóa đơn và import danh mục khách hàng/sản phẩm.

Các file XML/PDF hóa đơn, tờ khai và mẫu xem trước thường được sinh tại thời điểm người dùng xem, tải hoặc gửi mail; không phải mọi file XML/PDF đều được lưu cố định trong `uploads`.

### 3. Nhóm Người Dùng Và Vai Trò

#### 3.1 Khách Chưa Có Tài Khoản

Khách có thể:

- Đăng ký công ty mới.
- Đăng nhập tài khoản người dùng.
- Đăng nhập tài khoản admin.
- Quên mật khẩu và đặt lại mật khẩu.
- Tra cứu hóa đơn công khai bằng mã tra cứu/MST.

#### 3.2 Người Dùng Doanh Nghiệp

Người dùng doanh nghiệp có thể:

- Xem dashboard công ty.
- Quản lý tờ khai đăng ký sử dụng hóa đơn điện tử.
- Quản lý mẫu hóa đơn.
- Lập, sửa, ký, gửi và theo dõi hóa đơn GTGT.
- Import hóa đơn, khách hàng và sản phẩm từ file Excel.
- Quản lý sản phẩm.
- Quản lý khách hàng.
- Mua gói hóa đơn.
- Xem lịch sử mua gói và thanh toán lại giao dịch chưa thành công.
- Cấu hình máy chủ gửi mail.
- Xem lịch sử gửi mail.
- Xuất báo cáo hóa đơn.
- Cập nhật tài khoản cá nhân.
- Xem phiên đăng nhập.

#### 3.3 Admin Công Ty

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

Tài khoản admin công ty có `role = 1`. Hệ thống dùng thêm cột `admin_scope` trong bảng `users` để phân biệt phạm vi admin:

- `ROOT`: tài khoản Root hệ thống.
- `ROOT_COMPANY`: tài khoản admin thuộc công ty root, tức `company_id = 1`.
- `COMPANY`: tài khoản admin công ty thường.

Chỉ admin của công ty root (`company_id = 1`, `admin_scope = ROOT_COMPANY`) mới có mật khẩu quản trị riêng và được hiển thị dropdown `Quản trị` ở header. Admin công ty thường không dùng mật khẩu quản trị riêng.

#### 3.4 Admin Hệ Thống Và Root

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
- Cấu hình báo cáo hóa đơn ngày.
- Gửi báo cáo hóa đơn ngày thử.
- Quản lý quyền và nhóm quyền.
- Xem phiên đăng nhập theo ngữ cảnh admin.

### 4. Xác Thực Và Điều Hướng

#### 4.1 Trang Xác Thực

Các trang xác thực chính:

- `/auth/login`: đăng nhập người dùng doanh nghiệp.
- `/auth/login-admin`: đăng nhập khu vực admin.
- `/auth/register`: đăng ký công ty mới.
- `/auth/forgot-password`: quên mật khẩu.
- `/auth/reset-password/:token`: đặt lại mật khẩu.

Frontend lưu riêng:

- `token`: token đăng nhập người dùng.
- `token-admin`: token đăng nhập admin.

#### 4.2 Điều Hướng Người Dùng

Các route người dùng yêu cầu `requiresUser`. Nếu chưa có token người dùng thì chuyển về `/auth/login`.

Các route admin yêu cầu `requiresAdmin`. Nếu chưa có token admin hợp lệ thì chuyển về `/auth/login-admin`.

Một số route cấu hình yêu cầu `rolePolicy: role<2`, nghĩa là chỉ Root hoặc Admin được truy cập, ví dụ quản lý thành viên, lịch sử đăng nhập và bảo mật IP.

#### 4.3 Trạng Thái Công Ty

Trạng thái công ty được dùng để kiểm soát quyền sử dụng hệ thống:

- `0`: tạm ngưng.
- `1`: đang hoạt động.
- `2`: chờ kích hoạt.

Khi công ty đang chờ kích hoạt, người dùng vẫn đăng nhập được nhưng các chức năng nghiệp vụ bị hạn chế. Backend chặn hầu hết API nghiệp vụ dưới `/v1/**`; hiện chỉ cho phép nhóm `/v1/invoice-packages` để người dùng có thể mua gói hóa đơn. Màn hình hiển thị thông báo chờ kích hoạt thay vì cho thao tác như bình thường.

### 5. Phân Quyền

Hệ thống dùng bảng:

- `permission_categories`: nhóm quyền.
- `permissions`: danh sách quyền, trong đó `name` là permission key.
- `user_permissions`: quyền được gán cho từng user.
- `users.admin_scope`: phân biệt tài khoản Root, admin công ty root và admin công ty thường.

Controller backend gọi `permission("permission-key")` để kiểm tra quyền. Có thể truyền nhiều quyền dạng `permission("key-a|key-b")`; khi đó user chỉ cần có một trong các quyền được liệt kê. Nếu user không có quyền phù hợp thì hệ thống trả lỗi rõ tên quyền thiếu, ví dụ `Thiếu quyền: Xem danh mục sản phẩm`, dựa theo `display_name` trong bảng `permissions`.

Root có `role = 0` được phép toàn bộ. Nhân viên có `role = 2` cần được gán quyền trong `user_permissions`.

Admin công ty có `role = 1` được mặc định cho qua các quyền level 0. Vì vậy tài khoản quản trị công ty root và tài khoản quản trị công ty thường không cần insert `user_permissions` cho các quyền user level 0. Nhân viên có `role = 2` vẫn kiểm tra theo `user_permissions`.

Các API dữ liệu phụ như dropdown, autocomplete hoặc danh sách gợi ý không được làm sai quyền chính của màn hình. Khi dữ liệu phụ bị thiếu quyền, frontend phải xử lý nhẹ nhàng hoặc dùng `suppressGlobalErrorToast`, còn API chính vẫn trả thông báo thiếu quyền rõ ràng.

Một số nhóm quyền quan trọng:

- Quyền hóa đơn: `invoice-list`, `invoice-save`, `invoice-delete`.
- Quyền tờ khai: `register-invoice-list`, `register-invoice-save`, `register-invoice-send`.
- Quyền mẫu hóa đơn: `form-invoice-list`, `form-invoice-save`, `form-invoice-manage`.
- Quyền báo cáo: `report-invoice`, `report-invoice-export`.
- Quyền danh mục: `category-product-list`, `category-product-save`, `category-customer-list`, `category-customer-save`.
- Quyền import danh mục: `import-customer-list`, `import-customer-save`, `import-product-list`, `import-product-save`.
- Quyền cài đặt: `setting-profile`, `setting-member-list`, `setting-member-save`, `setting-member-manage`, `setting-security-ip`, `setting-login-history`.
- Quyền email: `mail-server-manage`, `mail-job-list`, `mail-job-retry`, `admin-mail-job-list`, `admin-mail-job-retry`.
- Quyền mua gói: `invoice-package-purchase`, `buy-invoice-list`, `buy-invoice-save`, `buy-invoice-delete`.
- Quyền quản lý công ty: `company-list`, `company-save`, `company-manage`.
- Quyền mail template: `mail-template-list`, `mail-template-save`, `mail-template-delete`.
- Quyền Telegram và báo cáo hóa đơn ngày: `telegram-config-manage`.
- Quyền quản trị danh mục nền: `bank-list`, `bank-save`, `tax-authority-list`, `tax-authority-save`, `tax-authority-delete`, `vat-rate-list`, `vat-rate-save`, `vat-rate-delete`.
- Quyền quản lý phân quyền: `permission-manage`, `permission-category-manage`.

Nhóm quyền được sắp xếp theo `orderIndex` để điều khiển thứ tự hiển thị trong màn phân quyền thành viên và các màn quản trị liên quan. Màn quản lý nhóm quyền hỗ trợ kéo thả, chọn 2 nhóm để hoán đổi nhanh, bật/tắt trạng thái ngay trên dòng và lưu lại thứ tự bằng API reorder.

### 6. Luồng Đăng Ký Công Ty

#### 6.1 Người Dùng Đăng Ký

Người dùng nhập thông tin đăng ký công ty trên trang `/auth/register`.

Hệ thống kiểm tra:

- Mã số thuế đã tồn tại trong danh sách công ty hay chưa.
- Mã số thuế đã có hồ sơ đăng ký đang chờ duyệt hay chưa.
- Dữ liệu bắt buộc như tên công ty, mã số thuế, email, số điện thoại.

Nếu hợp lệ, hồ sơ được lưu vào bảng `company_registration_requests`.

#### 6.2 Admin Duyệt Hồ Sơ

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

### 7. Chức Năng Phía Doanh Nghiệp

#### 7.1 Dashboard Công Ty

Đường dẫn: `/`

Dashboard hiển thị:

- Tổng số hóa đơn công ty đang có.
- Số hóa đơn đã sử dụng.
- Số hóa đơn còn lại.
- Số hóa đơn đã phát hành trong năm.
- Giá trị hóa đơn trong năm.
- Nút đi nhanh đến mua gói hóa đơn khi gần hết hạn mức.

API chính: `/v1/dashboard/stats`.

#### 7.2 Quản Lý Gói Hóa Đơn Và Mua Gói

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
- `ZALOPAY`: cổng ZaloPay sandbox.

Trạng thái thanh toán:

- `PENDING`: chờ thanh toán.
- `SUCCESS`: thanh toán thành công.
- `FAILED`: thanh toán thất bại.

Luồng xử lý:

1. Người dùng chọn gói và phương thức thanh toán.
2. Hệ thống tạo bản ghi `invoice_package_purchases`.
3. Nếu là MoMo, VNPAY hoặc ZaloPay, hệ thống tạo URL thanh toán và mở cổng thanh toán.
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
- `/v1/invoice-packages/zalopay/callback`
- `/v1/invoice-packages/zalopay/return`
- `/v1/invoice-packages/zalopay/banks`

#### 7.3 Tờ Khai Đăng Ký Hóa Đơn Điện Tử

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

#### 7.4 Mẫu Hóa Đơn

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

Ghi chú phân quyền:

- Danh sách và xem trước mẫu dùng quyền `form-invoice-list`.
- Tạo/cập nhật/xóa mẫu dùng quyền `form-invoice-save`.
- Khi người dùng đang tạo hoặc sửa mẫu, API đọc chi tiết mẫu hoặc đọc mẫu hệ thống để sao chép được phép dùng `form-invoice-list` hoặc `form-invoice-save`, tránh trường hợp có quyền thêm/sửa mẫu nhưng không mở được form tạo/sửa.

#### 7.5 Lập Và Quản Lý Hóa Đơn GTGT

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
- Hỗ trợ luồng phát hành cho hóa đơn GTGT mẫu một thuế suất với `form_invoices.type = 1`.

Trạng thái hóa đơn phổ biến:

- `0`: mới khởi tạo.
- `1`: đã ký.
- `2`: đã gửi thuế.
- `3`: đã phát hành.
- `4`: bị thay thế.
- `5`: bị điều chỉnh.
- `6`: đã hủy.
- `7`: không đủ điều kiện cấp mã.

Luồng hóa đơn thông thường:

1. Người dùng lập hóa đơn.
2. Hệ thống gọi `/v1/invoices/prepare` để kiểm tra quyền `invoice-save`, mẫu hóa đơn GTGT đang kích hoạt, tờ khai đã được chấp nhận và hình thức hóa đơn đã đăng ký.
3. Hóa đơn được lưu ở trạng thái mới khởi tạo.
4. Người dùng ký hóa đơn.
5. Hệ thống tạo XML hóa đơn và lưu thông tin ký.
6. Người dùng gửi cơ quan thuế.
7. Hệ thống ghi lịch sử gửi và cập nhật trạng thái.
8. Khi phát hành thành công, hệ thống có thể đưa email hóa đơn vào hàng đợi gửi cho khách mua.

API chính: `/v1/invoices`.

Ghi chú phân quyền:

- Màn lập hóa đơn dùng quyền chính `invoice-save`.
- Dữ liệu khách hàng và sản phẩm trên màn lập hóa đơn chỉ là dữ liệu gợi ý để tự điền nhanh. Nếu user không có `category-customer-list` hoặc `category-product-list`, form lập hóa đơn vẫn được mở; người dùng vẫn có thể nhập tay thông tin người mua và hàng hóa.
- Khi thiếu quyền chính, hệ thống báo rõ quyền thiếu thay vì báo chung chung.

Ghi chú dữ liệu và XML:

- Hóa đơn một thuế suất vẫn phải có dòng hàng hợp lệ, thuế suất và tổng tiền đúng theo mẫu.
- Khi lấy XML đã ký hoặc XML gửi cơ quan thuế, hệ thống cần lọc đúng hóa đơn, công ty và mã tra cứu để tránh dùng nhầm XML của công ty khác.

#### 7.6 Import Hóa Đơn

Đường dẫn: `/imports/invoice`

Chức năng:

- Upload file import hóa đơn.
- Đọc dữ liệu theo cấu trúc mẫu.
- Kiểm tra lỗi dữ liệu.
- Tạo hóa đơn nháp hoặc nhóm hóa đơn.
- Trả file lỗi hoặc danh sách lỗi nếu dữ liệu không hợp lệ.
- Xem lịch sử import hóa đơn.
- Import lại file đã upload để tạo hóa đơn nháp mới khi cần.

API chính: `/v1/invoice-imports` hoặc controller import hóa đơn tương ứng.

Giao diện:

- Trang danh sách hóa đơn có nút `Import hóa đơn`.
- Trang import hóa đơn có nút `Danh sách hóa đơn`.

Ghi chú mẫu Excel:

- Hệ thống dùng một mẫu Excel import chung để người dùng có thể nhập dữ liệu dù công ty đang dùng mẫu nhiều thuế suất hay mẫu một thuế suất.
- Mẫu cần có hướng dẫn rõ cách nhập thông tin người mua, thông tin hóa đơn và nhiều dòng hàng hóa/dịch vụ.
- Import phải gom đúng nhiều dòng hàng thuộc cùng hóa đơn, không chỉ lấy một dòng đầu tiên.
- Với mẫu một thuế suất, dữ liệu import vẫn phải kiểm tra thuế suất, tiền thuế và tổng tiền theo rule của mẫu.

Ghi chú phân quyền:

- Tải mẫu Excel, upload file và import lại dùng quyền `invoice-save`.
- Danh sách lịch sử import cho phép `invoice-list` hoặc `invoice-save`, để user có quyền import vẫn xem được kết quả import của chính công ty.

#### 7.6.1 Import Danh Mục Khách Hàng/Sản Phẩm

Đường dẫn:

- `/imports/customer`: import khách hàng.
- `/imports/product`: import sản phẩm.

Chức năng:

- Tải mẫu Excel theo từng loại danh mục.
- Upload file import khách hàng hoặc sản phẩm.
- Kiểm tra dữ liệu bắt buộc và trùng mã trong cùng file.
- Tạo mới bản ghi khi mã chưa tồn tại trong công ty.
- Cập nhật bản ghi hiện có khi mã đã tồn tại trong công ty.
- Xem lịch sử import và import lại file cũ.

API chính: `/v1/catalog-imports/{customer|product}`.

Ghi chú giao diện:

- Trang danh sách khách hàng có nút `Import khách hàng`.
- Trang danh sách sản phẩm có nút `Import sản phẩm`.
- Trang import khách hàng có nút `Danh sách khách hàng`.
- Trang import sản phẩm có nút `Danh sách sản phẩm`.

Ghi chú phân quyền:

- Import khách hàng cho phép quyền import riêng hoặc quyền danh mục khách hàng: `import-customer-list|category-customer-list` và `import-customer-save|category-customer-save`.
- Import sản phẩm cho phép quyền import riêng hoặc quyền danh mục sản phẩm: `import-product-list|category-product-list` và `import-product-save|category-product-save`.

#### 7.7 Danh Mục Sản Phẩm

Đường dẫn: `/categories/product/list`

Chức năng:

- Danh sách sản phẩm theo công ty.
- Tìm kiếm sản phẩm.
- Thêm sản phẩm.
- Cập nhật sản phẩm.
- Xóa sản phẩm.
- Quản lý mã, tên, đơn vị tính, đơn giá, thuế suất, mô tả và trạng thái.
- Không cho thêm mới hoặc đổi mã sang mã sản phẩm đã thuộc bản ghi khác trong cùng công ty.
- Dùng sản phẩm để nạp nhanh dòng hàng hóa khi lập hóa đơn.

API chính: `/v1/categories/product`.

#### 7.8 Danh Mục Khách Hàng

Đường dẫn: `/categories/customer/list`

Chức năng:

- Danh sách khách hàng theo công ty.
- Tìm kiếm khách hàng.
- Thêm khách hàng.
- Cập nhật khách hàng.
- Xóa khách hàng.
- Quản lý tên khách hàng, mã số thuế, người mua, địa chỉ, email, điện thoại, fax, tài khoản ngân hàng và tên ngân hàng.
- Không cho thêm mới hoặc đổi mã sang mã khách hàng đã thuộc bản ghi khác trong cùng công ty.
- Dùng khách hàng để nạp nhanh thông tin người mua khi lập hóa đơn.

API chính: `/v1/categories/customer`.

#### 7.9 Báo Cáo Hóa Đơn

Đường dẫn: `/reports/invoice/list`

Chức năng:

- Xem báo cáo hóa đơn theo khoảng ngày.
- Lọc theo danh mục hóa đơn, trạng thái và kỳ báo cáo.
- Chọn kỳ theo tháng, theo quý hoặc không theo kỳ.
- Khi không theo kỳ, người dùng nhập `fromDate` và `toDate` để lọc theo khoảng ngày.
- Tổng hợp số tiền, tiền thuế, tổng thanh toán.
- Xuất báo cáo Excel.

API chính:

- `/v1/reports/invoices`
- `/v1/reports/invoices/export`

#### 7.10 Cấu Hình Máy Chủ Gửi Mail

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

#### 7.11 Lịch sử gửi mail

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

### 8. Chức Năng Cài Đặt Doanh Nghiệp

#### 8.1 Tài Khoản Cá Nhân

Đường dẫn: `/setting/account/list`

Chức năng:

- Xem thông tin tài khoản hiện tại.
- Cập nhật họ tên.
- Cập nhật email.
- Cập nhật số điện thoại.
- Đổi mật khẩu.
- Upload avatar.

API chính: `/v1/setting/account`.

#### 8.2 Hồ Sơ Công Ty

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

#### 8.3 Quản Lý Thành Viên

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

#### 8.4 Phiên Đăng Nhập

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

#### 8.5 Lịch Sử Đăng Nhập

Đường dẫn: `/setting/login-history/list`

Chức năng:

- Xem lịch sử đăng nhập.
- Lọc theo từ khóa.
- Hiển thị username, IP, user-agent, loại đăng nhập, thời gian đăng nhập.
- Root có thể xem rộng hơn, admin công ty xem trong phạm vi công ty.

API chính: `/v1/setting/login-history`.

#### 8.6 Bảo Mật IP

Đường dẫn: `/setting/security/ip`

Chức năng:

- Bật hoặc tắt bảo mật IP cho công ty.
- Xem IP hiện tại.
- Thêm IP được phép đăng nhập.
- Xóa IP được phép.
- Ghi chú IP.
- Chặn đăng nhập nếu công ty bật bảo mật IP và request không thuộc danh sách cho phép.

API chính: `/v1/setting/security/ip`.

### 9. Chức Năng Quản Trị Hệ Thống

#### 9.1 Dashboard Admin

Đường dẫn: `/administrator`

Chức năng:

- Xem thống kê mua gói hóa đơn.
- Lọc theo thời gian, phương thức thanh toán, trạng thái.
- Xem biểu đồ/thống kê theo tháng.
- Xuất Excel thống kê.

#### 9.2 Quản Lý Công Ty

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

#### 9.3 Duyệt Đăng Ký Công Ty

Đường dẫn: `/administrator/company-registration/list`

Chức năng:

- Xem hồ sơ đăng ký công ty.
- Lọc hồ sơ theo trạng thái.
- Tìm kiếm theo tên công ty, mã số thuế, email.
- Duyệt hồ sơ.
- Từ chối hồ sơ.
- Xem thời gian và người xử lý.
- Thao tác trên từng hồ sơ qua menu chức năng dạng dropdown để đồng bộ với các bảng quản trị khác.

API chính: `/v1/administrator/company-registration`.

#### 9.4 Quản Lý Gói Hóa Đơn

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

#### 9.5 Hạn Mức Hóa Đơn

Đường dẫn: `/administrator/buy-invoice/list`

Chức năng:

- Xem danh sách hạn mức hóa đơn của các công ty.
- Thêm/cập nhật số hóa đơn mua.
- Theo dõi số hóa đơn đã dùng.
- Lọc theo công ty và trạng thái.
- Xem lịch sử thay đổi hạn mức.
- Lọc lịch sử theo công ty, nguồn, loại thay đổi và khoảng ngày.
- Không cho xóa bản ghi đã phát sinh sử dụng hoặc đang có ràng buộc nghiệp vụ.
- Thao tác cập nhật/xóa qua menu chức năng dạng dropdown.

API chính: `/v1/administrator/buy-invoice`.

#### 9.6 Mẫu Hóa Đơn Hệ Thống

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

#### 9.7 Ngân Hàng

Đường dẫn: `/administrator/bank/list`

Chức năng:

- Xem danh sách ngân hàng.
- Tìm kiếm và lọc trạng thái.
- Thêm/cập nhật ngân hàng.
- Bật/tắt trạng thái.
- Dữ liệu ngân hàng được dùng trong hồ sơ công ty và thông tin thanh toán.

API chính: `/v1/administrator/bank`.

#### 9.8 Cơ Quan Thuế

Đường dẫn: `/administrator/tax-authority/list`

Chức năng:

- Xem danh sách cơ quan thuế.
- Tìm kiếm, lọc.
- Thêm/cập nhật cơ quan thuế.
- Khai báo cấp cha/cấp con.
- Xóa theo điều kiện.
- Dữ liệu được dùng khi công ty chọn cơ quan thuế quản lý.

API chính: `/v1/tax-authorities`.

#### 9.9 Thuế Suất

Đường dẫn: `/administrator/vat-rate/list`

Chức năng:

- Xem danh sách thuế suất.
- Thêm/cập nhật thuế suất.
- Bật/tắt trạng thái.
- Xóa theo điều kiện.
- Dùng cho sản phẩm và dòng hóa đơn.

API chính: `/v1/administrator/vat-rate`.

#### 9.10 Mail Template

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

#### 9.11 Lịch Sử Gửi Mail Toàn Hệ Thống

Đường dẫn: `/administrator/email/mail-history`

Chức năng:

- Xem lịch sử gửi mail của tất cả công ty.
- Lọc theo từ khóa và trạng thái.
- Xem công ty, email nhận, loại email, tiêu đề, trạng thái, số lần gửi và lỗi.
- Gửi lại email nếu job có payload hợp lệ.

API chính: `/v1/administrator/mail-jobs`.

#### 9.12 Cấu Hình Telegram

Đường dẫn: `/administrator/telegram/config`

Chức năng:

- Bật/tắt gửi Telegram.
- Lưu Bot Token ở database, token được mã hóa.
- Lưu Group Chat ID.
- Gửi tin nhắn kiểm tra để xác nhận Bot Token và Group Chat ID đúng.

API chính:

- `/v1/administrator/telegram-config`
- `/v1/administrator/telegram-config/test`

Ghi chú:

- Bảng `telegram_configs` chỉ lưu cấu hình Telegram như trạng thái, bot token và chat id.
- Lịch gửi báo cáo hóa đơn ngày không lưu trong bảng này.

#### 9.13 Cấu Hình Báo Cáo Hóa Đơn Ngày

Đường dẫn: `/administrator/daily-invoice-report/config`

Chức năng:

- Bật/tắt lịch gửi báo cáo tự động.
- Cấu hình giờ và phút gửi hằng ngày.
- Xem ngày báo cáo đã gửi gần nhất.
- Xem thời điểm gửi gần nhất.
- Gửi báo cáo thử theo ngày báo cáo được chọn hoặc mặc định ngày hôm qua.

Báo cáo hóa đơn ngày gồm số lượng hóa đơn của ngày báo cáo theo từng công ty:

- Đã ký.
- Đã gửi thuế.
- Không đủ điều kiện cấp mã.
- Tổng số hóa đơn cần kiểm tra.

Nếu không có hóa đơn, Telegram vẫn gửi thông báo: `Không có hóa đơn cần kiểm tra.`

Email báo cáo ngày chỉ gửi khi có dữ liệu hóa đơn cần báo cáo. Email gửi từ cấu hình công ty root nhưng nội dung, người nhận và lịch sử mail nằm theo công ty thường. Tiêu đề lưu vào `mail_jobs` phải thay `[REPORT_DATE]` bằng ngày báo cáo thực tế.

Scheduler chạy mỗi phút, sau đó kiểm tra cấu hình giờ/phút trong bảng `daily_invoice_report_configs`. Nếu đã đến thời điểm gửi và chưa gửi cho ngày báo cáo đó, hệ thống gửi báo cáo và cập nhật `last_report_date`, `last_sent_at`.

API chính:

- `/v1/administrator/daily-invoice-report/config`
- `/v1/administrator/daily-invoice-report/send`

Ghi chú quyền:

- Hiện controller đang dùng quyền `telegram-config-manage` cho màn này.
- Nếu sau này tách quyền chi tiết hơn, cần bổ sung permission key và cập nhật route/menu tương ứng.

#### 9.14 Quản Lý Phân Quyền

Đường dẫn:

- `/administrator/access-control/permissions/list`
- `/administrator/access-control/permission-categories/list`

Chức năng:

- Quản lý danh sách quyền.
- Quản lý nhóm quyền.
- Cập nhật tên hiển thị, level, mô tả, trạng thái.
- Sắp xếp nhóm quyền bằng kéo thả.
- Hoán đổi nhanh vị trí của 2 nhóm quyền bằng bộ chọn.
- Bật/tắt trạng thái nhóm quyền ngay trên danh sách.
- Lưu thứ tự nhóm quyền bằng API reorder sau khi thay đổi.
- Quyền mới tạo có thể được tự gán cho Root.
- Một số quyền level thấp có thể dùng cho admin công ty/member tùy cấu hình.

API chính:

- `/v1/administrator/permissions`
- `/v1/administrator/permission-categories`

### 10. Tra Cứu Hóa Đơn Công Khai

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

### 11. Thông Báo Và Lịch Sử

Hệ thống thống nhất dùng Toastr cho thông báo giao diện. Các lời gọi cũ qua `$bvToast.toast(...)` được chuyển tiếp sang Toastr để không còn đồng thời hiển thị hai kiểu thông báo. BootstrapVue vẫn được dùng cho component giao diện như modal, form, table, sidebar, nhưng không dùng BootstrapVue Toast làm kênh thông báo chính.

Quy tắc xử lý lỗi quyền trên frontend:

- Request chính của trang để interceptor Axios xử lý lỗi 403, hiển thị Toastr và điều hướng về trang phù hợp.
- Request phụ như dropdown, dữ liệu gợi ý, thông tin header hoặc dữ liệu autocomplete dùng `meta.suppressGlobalErrorToast = true` khi cần tự xử lý, để tránh double thông báo.
- Thông báo lỗi quyền ưu tiên nội dung từ backend, ví dụ `Thiếu quyền: Xem danh mục khách hàng`.

#### 11.1 Thông Báo Header

Hệ thống có API thông báo gần đây theo công ty:

- Lấy các lịch sử nghiệp vụ có `show_notify`.
- Hiển thị số lượng chưa đọc trên icon chuông.
- Lưu trạng thái đã đọc/chưa đọc theo user vào bảng `notification_reads`.
- Khi user mở thông báo, hệ thống đánh dấu đã đọc theo từng history id.

API chính:

- `/v1/history/notifications`
- `/v1/history/notifications/read`

#### 11.2 Lịch Sử Nghiệp Vụ

Bảng `histories` lưu các sự kiện quan trọng như:

- Tạo/cập nhật hóa đơn.
- Ký hóa đơn.
- Gửi cơ quan thuế.
- Phản hồi cơ quan thuế.
- Mua gói hóa đơn.
- Thay đổi trạng thái quan trọng.

Lịch sử được dùng cho hiển thị thông báo và truy vết nghiệp vụ.

### 12. Hàng Đợi Email

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

### 13. Thanh Toán MoMo, VNPAY Và ZaloPay

Các cấu hình thanh toán nằm trong `src/main/resources/application.properties` và có thể override bằng biến môi trường. Bộ key MoMo và ZaloPay mặc định trong project là key sandbox/demo public do nhà cung cấp công khai trong repo mẫu hoặc tài liệu developer để cá nhân/tổ chức kiểm thử tích hợp. Đây không phải key riêng của dự án; khi chạy production phải thay bằng thông tin merchant thật và domain callback/return public.

#### 13.1 MoMo

Hệ thống hỗ trợ nhiều loại thanh toán MoMo:

- Ví MoMo.
- ATM nội địa.
- Thẻ quốc tế.
- Trả sau.

Mỗi loại được map sang `requestType` tương ứng khi gọi MoMo.

Mặc định MoMo dùng endpoint test `https://test-payment.momo.vn` và bộ demo public gồm `MOMOBKUN20180529`, `klm05TvNBzhg7h7j`, `at67qH6mk8w5Y1nAyMoYKMWACiEi2bsa`. Bộ này phục vụ kiểm thử luồng thanh toán sandbox, không dùng để nhận tiền thật.

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

#### 13.2 VNPAY

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

#### 13.3 ZaloPay

ZaloPay được tích hợp theo môi trường sandbox và dùng API v2.

Mặc định ZaloPay dùng `app_id = 2553`, `key1 = PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL`, `key2 = kLtgPl8HHhfvMuDHPwKfgfsY4Ydm9eIz` và các endpoint `sb-openapi.zalopay.vn`. Đây là bộ sandbox public trong tài liệu ZaloPay Developer để test tích hợp.

Khi tạo giao dịch:

- Hệ thống tạo `app_trans_id` theo định dạng bắt đầu bằng `yyMMdd` theo giờ Việt Nam.
- Hệ thống tạo `embed_data` gồm `redirecturl`, `preferred_payment_method` và thông tin giao dịch nội bộ.
- Hệ thống tạo `item` mô tả gói hóa đơn.
- Hệ thống ký dữ liệu bằng HMAC-SHA256 với `zalopay.key1`.
- Gọi API tạo đơn ZaloPay và nhận `order_url`.
- Lưu `app_trans_id` vào `payment_code` để đối soát callback/redirect.

Khi ZaloPay callback:

- Hệ thống kiểm tra `mac` bằng `zalopay.key2`.
- Đọc trường `data` do ZaloPay gửi.
- Kiểm tra `app_id`, `app_trans_id` và số tiền.
- Nếu hợp lệ thì cộng hạn mức hóa đơn và trả `return_code = 1`.

Khi ZaloPay redirect:

- Hệ thống kiểm tra `checksum` bằng `zalopay.key2`.
- Kiểm tra `app_trans_id`, `app_id` và số tiền.
- Nếu redirect báo thành công, hệ thống truy vấn lại trạng thái đơn hàng qua API query.
- Nếu query thành công thì cộng hạn mức hóa đơn; nếu còn xử lý thì giữ trạng thái `PENDING`.
- Nếu redirect hoặc query thất bại thì cập nhật trạng thái `FAILED`.

Hệ thống có API `/v1/invoice-packages/zalopay/banks` để lấy danh sách ngân hàng ZaloPay sandbox hỗ trợ. API này dùng `zalopay.bank-list-app-id`, `zalopay.bank-list-key1` và ký theo công thức ZaloPay yêu cầu.

#### 13.4 Thanh Toán Lại

Người dùng có thể thanh toán lại khi:

- Giao dịch đang `PENDING`.
- Giao dịch đã `FAILED`.
- Giao dịch chưa được cộng hạn mức.
- Phương thức thanh toán thuộc MoMo, VNPAY hoặc ZaloPay.

Hệ thống tạo giao dịch mới dựa trên giao dịch cũ để tránh dùng lại URL hoặc chữ ký cũ gây lỗi.

### 14. Xuất File Và Xem Hóa Đơn

Hệ thống hỗ trợ:

- Render HTML hóa đơn từ XML và XSLT.
- Tải PDF hóa đơn.
- Tải XML hóa đơn.
- Tải XML tờ khai.
- Tải file preview mẫu hóa đơn.
- Xuất Excel báo cáo hóa đơn.
- Xuất Excel thống kê mua gói.

Các file có thể được sinh tại thời điểm người dùng xem/tải hoặc được đính kèm khi gửi email hóa đơn.

### 15. Dữ Liệu Chính Trong Hệ Thống

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
- `signature_vats`: XML hóa đơn đã ký.
- `signature_authorities_tax`: XML gửi/nhận cơ quan thuế.
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
- `daily_invoice_report_configs`: cấu hình lịch gửi báo cáo hóa đơn ngày.
- `banks`: danh mục ngân hàng.
- `tax_authorities`: cơ quan thuế.
- `vat_rates`: thuế suất.
- `permissions`: quyền.
- `permission_categories`: nhóm quyền.
- `user_permissions`: quyền gán cho user.

### 16. API Tổng Quan Theo Nhóm

#### 16.1 Public/Auth

- `/v1/auth/**`: đăng nhập, đăng ký, quên mật khẩu, thông tin user.
- `/v1/public/**`: API public.
- `/lookup-invoice`: trang tra cứu hóa đơn công khai.

#### 16.2 Doanh Nghiệp

- `/v1/dashboard/stats`: thống kê dashboard.
- `/v1/register-invoices`: tờ khai.
- `/v1/form-invoices`: mẫu hóa đơn.
- `/v1/invoices`: hóa đơn.
- `/v1/invoice-packages`: gói hóa đơn và mua gói.
- `/v1/catalog-imports/{customer|product}`: import danh mục khách hàng/sản phẩm.
- `/v1/categories/product`: sản phẩm.
- `/v1/categories/customer`: khách hàng.
- `/v1/reports/invoices`: báo cáo hóa đơn.
- `/v1/mail-servers`: cấu hình SMTP.
- `/v1/mail-jobs`: lịch sử gửi email của công ty.

#### 16.3 Cài Đặt

- `/v1/setting/account`: tài khoản cá nhân.
- `/v1/setting/profile`: hồ sơ công ty.
- `/v1/setting/members`: thành viên.
- `/v1/setting/sessions`: phiên đăng nhập.
- `/v1/setting/login-history`: lịch sử đăng nhập.
- `/v1/setting/security/ip`: bảo mật IP.

#### 16.4 Admin

- `/v1/administrator/company`: công ty.
- `/v1/administrator/company-registration`: duyệt đăng ký.
- `/v1/administrator/invoice-packages`: gói hóa đơn.
- `/v1/administrator/buy-invoice`: hạn mức hóa đơn.
- `/v1/administrator/form-invoices`: mẫu hóa đơn hệ thống.
- `/v1/administrator/bank`: ngân hàng.
- `/v1/tax-authorities`: cơ quan thuế.
- `/v1/administrator/vat-rate`: thuế suất.
- `/v1/administrator/mail-template`: mail template.
- `/v1/administrator/mail-jobs`: lịch sử gửi mail toàn hệ thống.
- `/v1/administrator/telegram-config`: cấu hình Telegram.
- `/v1/administrator/daily-invoice-report`: cấu hình và gửi báo cáo hóa đơn ngày.
- `/v1/administrator/permissions`: quyền.
- `/v1/administrator/permission-categories`: nhóm quyền.

### 17. Quy Tắc Nghiệp Vụ Quan Trọng

- Công ty chưa kích hoạt không được sử dụng đầy đủ chức năng nghiệp vụ.
- Người dùng chỉ thao tác dữ liệu thuộc công ty của mình.
- Admin hệ thống thao tác dữ liệu toàn hệ thống theo quyền được cấp.
- Root có quyền toàn bộ.
- Admin công ty (`role = 1`) được cho qua quyền user level 0; nhân viên (`role = 2`) phải có quyền được gán trong `user_permissions`.
- Admin công ty root (`company_id = 1`, `admin_scope = ROOT_COMPANY`) mới có mật khẩu quản trị riêng và menu quản trị ở header.
- Mẫu hóa đơn cần hợp lệ trước khi lập hóa đơn.
- Hóa đơn cần đủ dữ liệu người bán, người mua, hàng hóa, thuế suất và mẫu hóa đơn.
- Màn lập hóa đơn chỉ bắt buộc quyền `invoice-save`; dữ liệu gợi ý khách hàng/sản phẩm không được chặn người dùng lập hóa đơn nếu thiếu quyền danh mục.
- Dữ liệu gợi ý khách hàng/sản phẩm trên màn lập hóa đơn cần nạp đúng bản ghi người dùng chọn, kể cả khi còn dữ liệu cũ từng bị trùng mã.
- Hóa đơn đã phát hành không được sửa như hóa đơn nháp.
- Hóa đơn thay thế/điều chỉnh phải tham chiếu hóa đơn gốc.
- Import hóa đơn dùng quyền `invoice-save`; lịch sử import cho phép `invoice-list` hoặc `invoice-save`.
- Import khách hàng/sản phẩm dùng `import_type` trong `invoice_imports`; quyền import danh mục có thể dùng quyền import riêng hoặc quyền danh mục tương ứng.
- Giao dịch mua gói thành công mới được cộng hạn mức hóa đơn.
- Không dùng lại URL/chữ ký thanh toán cũ khi thanh toán lại.
- Email gửi hóa đơn xử lý qua hàng đợi để tránh chậm request người dùng.
- Job/lịch sử mail phải lưu theo công ty phát sinh nội dung, không dồn hết về công ty root.
- Báo cáo hóa đơn ngày dùng `daily_invoice_report_configs` để lưu lịch gửi; `telegram_configs` chỉ lưu thông tin Telegram.
- Báo cáo hóa đơn ngày gửi Telegram cả khi không có dữ liệu, nhưng email chỉ gửi khi có dữ liệu hóa đơn cần báo cáo.
- Bot token Telegram không lưu plain text mà được mã hóa.
- SMTP password không trả ngược ra frontend, chỉ trả dạng che `••••••••`.

### 18. Bảo Mật

Các cơ chế bảo mật hiện có:

- JWT authentication.
- Tách token user và token admin.
- Route guard frontend.
- Security filter backend.
- Permission key ở controller.
- Phân quyền theo user.
- Thông báo thiếu quyền hiển thị rõ tên quyền dựa trên `permissions.display_name`.
- Bảo mật IP theo công ty.
- Ghi nhận phiên đăng nhập.
- Cho phép đăng xuất phiên khác.
- Ghi lịch sử đăng nhập.
- Mã hóa mật khẩu SMTP.
- Mã hóa Telegram bot token.
- Không đưa token/bí mật vào file cấu hình public.
- Public endpoint chỉ mở cho các luồng cần thiết như tra cứu hóa đơn, tải hóa đơn public, callback thanh toán.

### 19. Tác Vụ Nền

#### 19.1 Mail Queue Worker

Worker quét bảng `mail_jobs` định kỳ để gửi email.

Chức năng:

- Lấy job đang chờ.
- Gửi email theo template.
- Cập nhật trạng thái.
- Retry khi lỗi.
- Ghi lỗi cuối cùng khi thất bại vĩnh viễn.

#### 19.2 Daily Invoice Report Scheduler

Scheduler chạy mỗi phút, kiểm tra cấu hình báo cáo hóa đơn ngày trong bảng `daily_invoice_report_configs`.

Nếu:

- Lịch gửi báo cáo đang bật.
- Đã đến hoặc vượt qua giờ/phút cấu hình trong ngày.
- Chưa gửi báo cáo cho ngày hôm qua.

Thì hệ thống gửi báo cáo hóa đơn ngày hôm qua.

Quy tắc gửi:

- Telegram dùng cấu hình trong `telegram_configs`; nếu không có dữ liệu vẫn gửi thông báo không có hóa đơn cần kiểm tra.
- Email dùng template `DAILY_INVOICE_REPORT_MAIL`; chỉ gửi khi công ty có dữ liệu hóa đơn cần báo cáo.
- Mail job của công ty nào phải lưu theo `company_id` của công ty đó.

### 20. Lệnh Chạy Và Build

#### 20.1 Chạy Frontend Dev

```bash
npm run serve
```

Frontend chạy mặc định ở:

```text
http://localhost:8080
```

#### 20.2 Build Frontend Vào Spring Static

```bash
npm run build
```

Kết quả build nằm ở:

```text
src/main/resources/static
```

#### 20.3 Compile Backend

```bash
mvn -q -DskipTests compile
```

#### 20.4 Chạy Backend

```bash
./mvnw spring-boot:run
```

Tùy cấu hình server port, backend phục vụ API dưới prefix `/v1`.

### 21. Các Màn Hình Chính

#### 21.1 Public/Auth

- `/auth/login`: đăng nhập người dùng.
- `/auth/login-admin`: đăng nhập admin.
- `/auth/register`: đăng ký công ty.
- `/auth/forgot-password`: quên mật khẩu.
- `/auth/reset-password/:token`: đặt lại mật khẩu.
- `/lookup-invoice`: tra cứu hóa đơn.

#### 21.2 Doanh Nghiệp

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
- `/imports/customer`: import khách hàng.
- `/imports/product`: import sản phẩm.
- `/categories/product/list`: sản phẩm.
- `/categories/customer/list`: khách hàng.
- `/reports/invoice/list`: báo cáo hóa đơn.
- `/email/mail-server`: máy chủ gửi mail.
- `/email/mail-history`: Lịch sử gửi mail.

#### 21.3 Cài Đặt

- `/setting/account/list`: tài khoản.
- `/setting/profile/list`: hồ sơ công ty.
- `/setting/sessions/list`: phiên đăng nhập.
- `/setting/member/list`: thành viên.
- `/setting/login-history/list`: lịch sử đăng nhập.
- `/setting/security/ip`: bảo mật IP.

#### 21.4 Admin

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
- `/administrator/email-template/create`: tạo/cập nhật mail template.
- `/administrator/email/mail-history`: lịch sử gửi mail toàn hệ thống.
- `/administrator/telegram/config`: cấu hình Telegram.
- `/administrator/daily-invoice-report/config`: cấu hình báo cáo hóa đơn ngày.
- `/administrator/sessions/list`: phiên đăng nhập admin.
- `/administrator/form-invoice/create`: tạo mẫu hóa đơn hệ thống.
- `/administrator/form-invoice/:id/edit`: cập nhật mẫu hóa đơn hệ thống.
- `/administrator/access-control/permissions/list`: quyền.
- `/administrator/access-control/permission-categories/list`: nhóm quyền.

### 22. Chuẩn Giao Diện Hiện Tại

Các màn hình giao diện đang được chuẩn hóa theo các quy tắc sau:

- Sidebar admin, sidebar doanh nghiệp và sidebar setting giữ chiều rộng cố định khi mở, có trạng thái thu gọn qua nút menu trên header và không bị bóp khi nội dung bảng rộng.
- Bảng danh sách dùng cột `Chức năng` dạng dropdown ba chấm để đồng bộ thao tác cập nhật, xóa, duyệt, gửi lại hoặc các hành động phụ.
- Dropdown trong bảng dùng kích thước nhỏ, có vùng hiển thị đủ rộng và tránh bị cắt bởi bảng responsive trên desktop.
- Phân trang dùng thanh chung `pagination_bar.vue`, hiển thị số dòng/trang, khoảng bản ghi hiện tại, tổng bản ghi và cụm nút trang ở bên phải.
- Modal dùng header/footer thống nhất, nút hành động đặt ở footer hoặc cụm hành động cuối form; tránh để nút dính sát input.
- Khoảng cách label và input được rút gọn bằng biến CSS chung `--ui-label-gap`.
- Các form trong setting/profile/account dùng cụm nút hành động tách riêng để không dính vào ô nhập.
- Màn phân quyền thành viên dùng modal dạng lưới nhóm quyền, có tổng số quyền đã chọn và thao tác chọn/bỏ chọn theo nhóm.
- Màn nhóm quyền dùng danh sách sắp xếp gọn, hỗ trợ kéo thả và hoán đổi nhanh 2 nhóm, cột chức năng vẫn là dropdown ba chấm.
- Toàn bộ thông báo thành công, cảnh báo và lỗi dùng Toastr. Không dùng BootstrapVue Toast để tránh giao diện thông báo bị chồng kiểu.
- Các request phụ phục vụ dropdown, autocomplete hoặc dữ liệu nền phải tránh làm chặn màn hình chính nếu thiếu quyền không liên quan trực tiếp.

### 23. Ghi Chú Vận Hành

- Khi thêm chức năng quản trị mới, cần kiểm tra và bổ sung permission key ở controller.
- Khi thêm menu frontend, cần kiểm tra route guard và quyền backend tương ứng.
- Khi thêm API phụ cho màn hình, cần xác định rõ quyền chính của màn và quyền của API phụ. Nếu API phụ chỉ phục vụ gợi ý hoặc dropdown, frontend cần dùng `suppressGlobalErrorToast` hoặc backend cần cho phép nhiều key bằng `permission("key-a|key-b")` để tránh lỗi quyền sai ngữ cảnh.
- Khi thêm thông báo mới, dùng `this.$toastr` hoặc utility Toastr; không gọi BootstrapVue Toast trực tiếp.
- Khi thêm callback public như cổng thanh toán, không gắn permission theo user vì cổng thanh toán gọi từ bên ngoài.
- Khi thêm cấu hình nhạy cảm, ưu tiên lưu database và mã hóa, không commit token/mật khẩu thật vào source.
- Khi build frontend, Vue CLI tạo file static có hash mới trong `src/main/resources/static`.
- Cảnh báo bundle lớn khi build hiện tại chủ yếu do vendor bundle và logo lớn, không phải lỗi compile.
