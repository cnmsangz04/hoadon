# Luồng nghiệp vụ chính

Cập nhật: 18/06/2026.

## Đăng ký công ty

1. Khách đăng ký công ty ở màn hình auth.
2. Backend kiểm tra mã số thuế, email và dữ liệu bắt buộc.
3. Hồ sơ chờ duyệt được lưu vào `company_registration_requests`.
4. Admin duyệt hồ sơ trong khu quản trị.
5. Hệ thống tạo công ty, tài khoản admin công ty và dữ liệu nền cần thiết.

## Đăng nhập và phân quyền

1. Người dùng đăng nhập bằng tài khoản thường hoặc admin.
2. Backend xác thực mật khẩu, trạng thái user và bảo mật IP nếu có.
3. Hệ thống tạo phiên trong `login_sessions`.
4. JWT trả về frontend và được lưu vào `token` hoặc `token-admin`.
5. Mỗi API nghiệp vụ kiểm tra token, phiên, quyền và phạm vi công ty.

## Mẫu hóa đơn

1. Công ty tạo hoặc chọn mẫu hóa đơn.
2. Mẫu lưu trong `form_invoices`, gồm thông tin loại mẫu, ký hiệu, file XSLT và ảnh xem trước nếu có.
3. Khi lập hóa đơn, hệ thống lấy mẫu đang dùng để sinh XML/PDF và hiển thị.
4. Với hóa đơn GTGT mẫu một thuế suất, cần đảm bảo dữ liệu dòng hàng và tổng thuế phù hợp `form_invoices.type = 1`.

## Lập hóa đơn GTGT

1. Người dùng nhập thông tin người mua, hình thức thanh toán và dòng hàng hóa.
2. Backend kiểm tra dải số, mẫu hóa đơn, dữ liệu dòng hàng, thuế suất và quyền thao tác.
3. Hóa đơn lưu trong `invoices`, dòng hàng hoặc dữ liệu chi tiết lưu theo cấu trúc hiện tại của entity.
4. Khi phát hành, hệ thống sinh XML, ký số và cập nhật trạng thái hóa đơn.
5. XML đã ký lưu trong `signature_vats` theo hóa đơn và công ty.

## Gửi cơ quan thuế

1. Người dùng gửi hóa đơn hoặc tờ khai sang cơ quan thuế.
2. Backend sinh XML theo định dạng tương ứng.
3. Dữ liệu gửi/nhận lưu trong `signature_authorities_tax` hoặc bảng liên quan.
4. Trạng thái hóa đơn cập nhật theo phản hồi.
5. Khi xem lại XML hoặc PDF, hệ thống phải lấy đúng dữ liệu theo hóa đơn và công ty.

## Import hóa đơn từ Excel

1. Người dùng tải một mẫu Excel import dùng chung.
2. Người dùng nhập thông tin hóa đơn và nhiều dòng hàng.
3. Backend đọc file bằng Apache POI.
4. Hệ thống gom dòng theo hóa đơn, kiểm tra dữ liệu bắt buộc, thuế suất, tổng tiền và loại mẫu.
5. Hóa đơn hợp lệ được tạo trong hệ thống; dòng lỗi được trả về để người dùng sửa.

Mẫu import cần đủ hướng dẫn để người dùng hiểu cách nhập dù công ty đang dùng mẫu nhiều thuế suất hay một thuế suất.

## Gửi email hóa đơn

1. Người dùng hoặc hệ thống tạo yêu cầu gửi email.
2. Nội dung lấy từ `mail_templates` và dữ liệu nghiệp vụ.
3. Job lưu vào `mail_jobs` theo công ty phát sinh nội dung.
4. Worker gửi mail qua SMTP của công ty hoặc cấu hình phù hợp.
5. Trạng thái gửi, số lần gửi và lỗi được cập nhật trong `mail_jobs`.

## Báo cáo hóa đơn ngày

1. Admin cấu hình lịch gửi trong màn báo cáo hóa đơn ngày.
2. Lịch lưu ở `daily_invoice_report_configs`.
3. Scheduler kiểm tra đến giờ gửi và ngày báo cáo chưa được gửi.
4. Hệ thống gom dữ liệu hóa đơn cần kiểm tra theo từng công ty.
5. Telegram gửi thông báo vào group phù hợp, kể cả trường hợp không có dữ liệu.
6. Email chỉ tạo khi có dữ liệu báo cáo.
7. Mail job và lịch sử gửi phải nằm theo công ty nhận nội dung báo cáo.

## Mua gói hóa đơn

1. Người dùng chọn gói hóa đơn.
2. Hệ thống tạo giao dịch mua trong bảng mua gói.
3. Người dùng thanh toán qua MoMo, VNPAY hoặc ZaloPay nếu chọn cổng thanh toán.
4. Sau khi thanh toán thành công, hạn mức hóa đơn được cộng vào công ty.
5. Lịch sử mua và thanh toán được lưu để đối soát.

## Tra cứu hóa đơn công khai

1. Người dùng ngoài hệ thống nhập thông tin tra cứu.
2. Backend tìm hóa đơn theo mã tra cứu hoặc điều kiện public được hỗ trợ.
3. Nếu hợp lệ, hệ thống trả thông tin hóa đơn hoặc file xem/tải.
4. API public không được làm lộ dữ liệu công ty khác ngoài hóa đơn được tra cứu hợp lệ.
