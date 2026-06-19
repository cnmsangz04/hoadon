# Kiểm thử hệ thống

Cập nhật: 19/06/2026.

Tài liệu này mô tả các nhóm kiểm thử và test case tiêu biểu để dùng trong báo cáo.

## Nhóm kiểm thử

| Nhóm | Mục tiêu |
| --- | --- |
| Kiểm thử chức năng | Đảm bảo các chức năng chính chạy đúng nghiệp vụ. |
| Kiểm thử phân quyền | Đảm bảo user chỉ thao tác đúng quyền và đúng công ty. |
| Kiểm thử validate | Đảm bảo dữ liệu nhập được kiểm tra ở frontend và backend. |
| Kiểm thử tích hợp | Đảm bảo các phần như mail, Telegram, thanh toán, XML phối hợp đúng. |
| Kiểm thử hồi quy | Đảm bảo sửa chức năng mới không làm hỏng luồng cũ. |

## Test case tiêu biểu

| Mã | Chức năng | Dữ liệu/Điều kiện | Kết quả mong đợi |
| --- | --- | --- | --- |
| TC01 | Đăng nhập user | Tài khoản/mật khẩu đúng | Đăng nhập thành công, nhận JWT, vào dashboard công ty. |
| TC02 | Đăng nhập sai mật khẩu | Mật khẩu sai | Hệ thống báo lỗi, không tạo phiên đăng nhập hợp lệ. |
| TC03 | Phân quyền nhân viên | User thiếu quyền `invoice-save` | Không được lập hóa đơn, thông báo rõ quyền thiếu. |
| TC04 | Tạo hóa đơn GTGT | Dữ liệu người mua và dòng hàng hợp lệ | Hóa đơn nháp được tạo đúng công ty. |
| TC05 | Tạo hóa đơn thiếu dòng hàng | Không có dòng hàng hợp lệ | Hệ thống báo lỗi và không lưu hóa đơn. |
| TC06 | Phát hành hóa đơn một thuế suất | Mẫu `form_invoices.type = 1`, dữ liệu thuế hợp lệ | XML và trạng thái hóa đơn xử lý đúng. |
| TC07 | Import Excel nhiều dòng hàng | Một hóa đơn có nhiều dòng hàng | Hệ thống gom đủ dòng hàng, không chỉ lấy dòng đầu tiên. |
| TC08 | Gửi email hóa đơn | Cấu hình SMTP hợp lệ | Tạo mail job, worker gửi thành công, trạng thái `sent`. |
| TC09 | Báo cáo ngày không có dữ liệu | Không có hóa đơn cần kiểm tra | Telegram gửi thông báo không có hóa đơn; email không gửi. |
| TC10 | Báo cáo ngày có dữ liệu | Có hóa đơn trạng thái cần kiểm tra | Telegram gửi báo cáo; email tạo theo từng công ty có dữ liệu. |
| TC11 | Tra cứu hóa đơn public | Mã tra cứu hợp lệ | Hiển thị đúng hóa đơn được phép tra cứu. |
| TC12 | Chặn dữ liệu công ty khác | User công ty A truy cập hóa đơn công ty B | Hệ thống từ chối hoặc không trả dữ liệu. |
| TC13 | Mua gói qua ZaloPay sandbox | Chọn gói hóa đơn và phương thức `ZALOPAY` | Hệ thống tạo `order_url`, lưu `app_trans_id`, trạng thái giao dịch là `PENDING`. |
| TC14 | Callback ZaloPay hợp lệ | Payload có `data`, `mac` đúng `key2`, số tiền khớp | Hệ thống cộng hạn mức hóa đơn và trả `return_code = 1`. |
| TC15 | Callback ZaloPay sai chữ ký | Payload có `mac` không khớp | Hệ thống từ chối callback, không cộng hạn mức hóa đơn. |
| TC16 | Thêm khách hàng trùng mã | Công ty đã có khách hàng mã `KH001`, tạo khách hàng khác cùng mã | Hệ thống báo trùng mã và không lưu bản ghi mới. |
| TC17 | Thêm sản phẩm trùng mã | Công ty đã có sản phẩm mã `SP001`, tạo sản phẩm khác cùng mã | Hệ thống báo trùng mã và không lưu bản ghi mới. |
| TC18 | Import danh mục cập nhật theo mã | File import có mã khách hàng/sản phẩm đã tồn tại trong công ty | Hệ thống cập nhật bản ghi hiện có và lưu lịch sử import đúng loại. |
| TC19 | Import danh mục trùng mã trong file | File import có hai dòng cùng mã khách hàng hoặc mã sản phẩm | Hệ thống báo lỗi dòng dữ liệu, không import file lỗi. |

## Kiểm thử validate form

Tham khảo chi tiết tại `FORM_VALIDATION_SUMMARY.md`. Các điểm cần kiểm:

- Form có `novalidate` nếu tự validate.
- Lỗi hiển thị bằng tiếng Việt.
- Email, số điện thoại, mã số thuế, ngày tháng đúng định dạng.
- API phụ không làm hỏng form chính nếu thiếu quyền phụ.
- Backend vẫn kiểm tra lại dữ liệu quan trọng trước khi lưu.

## Kiểm thử báo cáo hóa đơn ngày

Các trường hợp quan trọng:

- Lịch gửi tắt thì scheduler không gửi.
- Đến giờ gửi và chưa gửi ngày báo cáo thì scheduler gửi.
- Đã gửi ngày báo cáo thì không gửi lặp.
- Đổi giờ/phút hoặc bật lại lịch thì trạng thái lần gửi gần nhất được xử lý phù hợp.
- Email chỉ gửi khi có dữ liệu.
- Mail job lưu đúng công ty nhận nội dung.

## Kiểm thử import Excel

Các trường hợp quan trọng:

- File đúng mẫu và có một hóa đơn một dòng hàng.
- File đúng mẫu và có một hóa đơn nhiều dòng hàng.
- File có nhiều hóa đơn.
- File dùng mẫu một thuế suất.
- File thiếu thông tin người mua hoặc dòng hàng.
- File sai định dạng số tiền, thuế suất hoặc ngày.
- File import khách hàng đúng mẫu, tạo mới và cập nhật theo mã.
- File import sản phẩm đúng mẫu, tạo mới và cập nhật theo mã.
- File import danh mục có mã bị lặp trong cùng file.

## Lệnh hỗ trợ kiểm tra

Kiểm tra backend:

```bash
./mvnw test
```

Build backend:

```bash
./mvnw package
```

Build frontend:

```bash
npm run build
```

Kiểm tra markdown:

```bash
git diff --check
```
