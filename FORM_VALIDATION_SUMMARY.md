# Tổng hợp validate form

Tài liệu này mô tả chi tiết validate client-side đang áp dụng cho các form chính. Mục tiêu là nắm rõ trường nào bắt buộc, trường nào tùy chọn, định dạng nào được chấp nhận, và trường nào hiện chưa giới hạn độ dài.

Lưu ý: validate client-side chỉ giúp chặn lỗi sớm trên UI. Backend vẫn cần tiếp tục validate lại các trường quan trọng trước khi lưu dữ liệu.

## Quy ước validate dùng chung

File helper: `src/utils/validators.js`

| Rule | Điều kiện hiện tại |
| --- | --- |
| Bắt buộc | Giá trị khác `null`, `undefined`, và sau khi `trim()` không được rỗng. |
| Email | Regex: `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$`. Ví dụ hợp lệ: `abc@example.com`. |
| Số điện thoại | Trước khi kiểm tra sẽ bỏ khoảng trắng, dấu chấm và dấu gạch. Regex sau chuẩn hóa: `^(\\+?84|0)[0-9]{8,10}$`. Chấp nhận dạng `0912345678`, `+84912345678`, `84...`. |
| Mã số thuế | Regex: `^\\d{10}(-?\\d{3})?$`. Chấp nhận MST 10 số, 13 số, hoặc dạng `0123456789-001`. |
| Mật khẩu tối thiểu | Đang áp dụng tại màn đặt lại mật khẩu, đổi mật khẩu tài khoản và quản lý thành viên: tối thiểu 8 ký tự. |
| Số trong khoảng | Giá trị phải là số hợp lệ và nằm trong khoảng khai báo. |
| Website | Có helper kiểm tra URL, nhưng hiện chưa gắn vào form nào trong đợt này. |

## Giới hạn ký tự hiện tại

Frontend hiện mới áp dụng giới hạn độ dài tối thiểu cho một số trường mật khẩu: `>= 8 ký tự`.

Chưa áp dụng giới hạn tối đa ký tự ở frontend cho các trường tên công ty, địa chỉ, email, mã hàng, tên hàng, ghi chú... Các giới hạn tối đa nếu có đang phụ thuộc vào backend/database. Nếu cần làm chặt hơn trước production, nên bổ sung max length theo schema database cho các trường này.

## Đăng ký tài khoản

File: `src/views/auth/register.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Tên công ty | Có | Không rỗng sau `trim()` | Chưa giới hạn min/max ký tự. |
| Mã số thuế | Có | Không rỗng, đúng regex MST `10 số` hoặc `10 số + 3 số chi nhánh` | Ví dụ: `0101234567`, `0101234567001`, `0101234567-001`. |
| Địa chỉ | Có | Không rỗng sau `trim()` | Chưa giới hạn min/max ký tự. |
| Email | Có | Không rỗng, đúng regex email | Ví dụ: `contact@company.vn`. |
| Điện thoại | Không | Nếu nhập thì phải đúng regex phone | Chấp nhận `0...`, `84...`, `+84...`. |
| Người liên hệ | Không | Chưa validate | Trường tùy chọn. |

Submit bị chặn nếu có bất kỳ lỗi nào trong các trường trên. Lỗi hiển thị trực tiếp dưới input.

## Đăng nhập người dùng và đăng nhập quản trị

File:

- `src/views/auth/login.vue`
- `src/views/auth/login_admin.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Tài khoản | Có | Không rỗng | Nút đăng nhập bị disabled khi rỗng. Chưa validate độ dài/ký tự tại frontend. |
| Mật khẩu | Có | Không rỗng | Nút đăng nhập bị disabled khi rỗng. Chưa validate độ dài tại màn đăng nhập. |
| Ghi nhớ đăng nhập | Không | Boolean | Mặc định đang bật. |

Màn quản trị còn kiểm tra token sau đăng nhập có role `0` hoặc `1`; nếu không phải quyền admin thì xóa token admin và báo lỗi.

## Quên mật khẩu

File: `src/views/auth/forgot_password.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Tài khoản | Có | Không rỗng sau `trim()` | Chưa validate độ dài tài khoản. |
| Email tài khoản | Có | Không rỗng, đúng regex email | Dùng để backend xác minh yêu cầu reset. |

Submit bị chặn nếu thiếu tài khoản/email hoặc email sai định dạng.

## Đặt lại mật khẩu

File: `src/views/auth/reset_password.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Token reset | Có | Phải tồn tại trong route params | Nếu thiếu token thì báo lỗi và không submit. |
| Mật khẩu mới | Có | Không rỗng, tối thiểu 8 ký tự | Chưa bắt buộc chữ hoa/chữ thường/số/ký tự đặc biệt. |
| Nhập lại mật khẩu | Có | Không rỗng, phải khớp với mật khẩu mới | Lỗi hiển thị tại field xác nhận. |

## Tài khoản cá nhân

File: `src/views/settings/account/list.vue`

| Nhóm/Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Tên | Có khi cập nhật | Input `required`; nếu backend trả lỗi thì hiển thị dưới field | Chưa validate độ dài/ký tự tại frontend. |
| Email | Có khi cập nhật | Input `required`; lỗi cụ thể hiển thị theo backend | Chưa gắn `type=email`, nên frontend chưa tự check regex email ở form này. |
| Điện thoại | Có khi cập nhật | Input `required`; lỗi cụ thể hiển thị theo backend | Chưa check regex phone ở frontend. |
| Mật khẩu hiện tại | Có khi đổi mật khẩu | Không rỗng sau `trim()` | Nếu rỗng thì state false. |
| Mật khẩu mới | Có khi đổi mật khẩu | Tối thiểu 8 ký tự | Chưa bắt buộc chữ hoa/chữ thường/số/ký tự đặc biệt. |
| Nhập lại mật khẩu mới | Có khi đổi mật khẩu | Phải khớp với mật khẩu mới | Nút đổi mật khẩu disabled nếu không hợp lệ. |
| Ảnh đại diện | Không | Chọn file `image/*`, crop về blob PNG | Chưa validate kích thước file/tối đa dung lượng tại frontend. |

## Hồ sơ công ty

File: `src/views/settings/profile/list.vue`

Trang này chia thành nhiều form nhỏ. Frontend chủ yếu gắn `required`, `state(field)` và hiển thị lỗi `errors` backend trả về. Nghĩa là các field bắt buộc được UI đánh dấu, nhưng độ dài tối đa và regex email/phone/website phần lớn đang do backend quyết định.

| Nhóm/Trường | Bắt buộc | Điều kiện validate hiện tại | Ghi chú |
| --- | --- | --- | --- |
| Mã số thuế | Readonly | Không cho sửa | Hiển thị MST công ty. |
| Ngành nghề | Có | Input `required`, lỗi từ backend | Chưa giới hạn độ dài frontend. |
| Tên đơn vị | Có | Input `required`, lỗi từ backend | Chưa giới hạn độ dài frontend. |
| Địa chỉ công ty | Có | Input `required`, lỗi từ backend | Chưa giới hạn độ dài frontend. |
| Logo | Không | File/ảnh, lỗi từ backend nếu có | Chưa validate kích thước/định dạng rõ trên frontend. |
| Favicon | Không | File/ảnh, lỗi từ backend nếu có | Chưa validate kích thước/định dạng rõ trên frontend. |
| Đại diện: Họ và tên | Có | Lỗi từ backend | Chưa giới hạn độ dài frontend. |
| Đại diện: Điện thoại | Có | Input `required`, lỗi từ backend | Chưa dùng regex phone dùng chung ở frontend. |
| Đại diện: CCCD | Không | Lỗi từ backend nếu có | Chưa check 9/12 số tại frontend. |
| Đại diện: Số hộ chiếu | Không | Lỗi từ backend nếu có | Chưa check format passport. |
| Đại diện: Ngày sinh | Có | Nhập tay phải đúng `dd/mm/yyyy`, năm `1900-2099` | Có parse và báo lỗi nếu sai ngày. |
| Đại diện: Giới tính | Có | Chọn từ danh sách | Lỗi từ backend nếu có. |
| Đại diện: Email | Có | Input `required`, lỗi từ backend | Chưa dùng regex email dùng chung ở frontend. |
| Thông tin hóa đơn: Email | Có | Lỗi từ backend | Chưa dùng regex email dùng chung ở frontend. |
| Thông tin hóa đơn: Điện thoại | Có | Input `required`, lỗi từ backend | Chưa dùng regex phone dùng chung ở frontend. |
| Thông tin hóa đơn: Fax | Có | Input `required`, lỗi từ backend | Chưa validate format fax. |
| Thông tin hóa đơn: Website | Có | Input `required`, lỗi từ backend | Chưa dùng helper URL tại frontend. |
| Liên hệ: Họ và tên | Có | Lỗi từ backend | Chưa giới hạn độ dài frontend. |
| Liên hệ: Email | Có | Input `required`, lỗi từ backend | Chưa dùng regex email dùng chung ở frontend. |
| Liên hệ: Điện thoại | Có | Input `required`, lỗi từ backend | Chưa dùng regex phone dùng chung ở frontend. |
| Liên hệ: Địa chỉ | Có | Input `required`, lỗi từ backend | Chưa giới hạn độ dài frontend. |
| Ngân hàng: Số tài khoản | Có | Input `required`, lỗi từ backend | Chưa validate chỉ gồm số/ký tự ngân hàng. |
| Ngân hàng: Tên ngân hàng | Có | Chọn từ `v-select` | Lỗi từ backend nếu bỏ trống/sai. |
| Ngân hàng: Chi nhánh | Không | Lỗi từ backend nếu có | Trường tùy chọn. |
| Cơ quan thuế: Cục thuế tỉnh/thành | Có | Chọn từ `v-select` | Lỗi từ backend nếu bỏ trống/sai. |
| Cơ quan thuế quản lý | Có | Chọn từ `v-select` | Lỗi từ backend nếu bỏ trống/sai. |

## Tra cứu hóa đơn public

File: `src/views/public/invoice_lookup.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Mã tra cứu | Có | Không rỗng sau `trim()` | Chưa validate độ dài/bộ ký tự của mã tra cứu ở frontend. |
| Mã số thuế bên bán | Có | Không rỗng, đúng regex MST | Cần khớp với công ty phát hành hóa đơn ở backend. |

Nếu validate frontend pass, API public vẫn tiếp tục kiểm tra cặp `mã tra cứu + MST bên bán`.

## Danh mục khách hàng

File: `src/views/customers/categories/customer/list.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Mã khách hàng | Có | Không rỗng sau `trim()` | Khi cập nhật đang bị disabled, không cho sửa mã. Chưa giới hạn độ dài. |
| Tên khách hàng / công ty đối tác | Có | Không rỗng sau `trim()` | Chưa giới hạn độ dài. |
| Mã số thuế | Không | Nếu nhập thì phải đúng regex MST | Chấp nhận cả MST cá nhân/tổ chức 10 số và chi nhánh 13 số. |
| Người mua hàng | Không | Chưa validate | Trường tùy chọn. |
| Số điện thoại | Không | Nếu nhập thì phải đúng regex phone | Bỏ khoảng trắng/dấu chấm/dấu gạch trước khi check. |
| Địa chỉ | Không | Chưa validate | Trường tùy chọn. |
| Email nhận hóa đơn | Không | Nếu nhập thì phải đúng regex email | Chưa bắt buộc vì có khách hàng không nhận mail. |
| Fax | Không | Chưa validate | Trường tùy chọn. |
| Số tài khoản ngân hàng | Không | Chưa validate | Trường tùy chọn. |
| Tại ngân hàng | Không | Chưa validate | Trường tùy chọn. |
| Ghi chú | Không | Chưa validate | Trường tùy chọn. |
| Trạng thái | Có giá trị mặc định | `1` hoặc `0` theo checkbox | UI gán mặc định `1`. |

Modal thêm/cập nhật sẽ reset lỗi mỗi lần mở form.

## Danh mục hàng hóa

File: `src/views/customers/categories/product/list.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Mã sản phẩm | Có | Không rỗng sau `trim()` | Khi cập nhật đang bị disabled, không cho sửa mã. Chưa giới hạn độ dài. |
| Tên sản phẩm | Có | Không rỗng sau `trim()` | Chưa giới hạn độ dài. |
| Thuộc công ty | Không cho nhập | Readonly | Lấy theo công ty đang đăng nhập. |
| Đơn vị tính | Không | Chưa validate | Trường tùy chọn. |
| Đơn giá | Có | Không rỗng, là số, `>= 0` | Chưa giới hạn số chữ số/tối đa. |
| Thuế suất | Có giá trị mặc định | Giá trị từ danh sách thuế suất | Mặc định `-1` nếu chưa chọn. |
| Mô tả | Không | Chưa validate | Trường tùy chọn. |
| Trạng thái | Có giá trị mặc định | `1` hoặc `0` theo checkbox | UI gán mặc định `1`. |

## Cấu hình máy chủ gửi mail

File: `src/views/customers/email/mail-server.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Máy chủ SMTP | Có | Không rỗng sau `trim()` | Ví dụ: `smtp.gmail.com`. Chưa validate domain/hostname chi tiết. |
| Cổng SMTP | Có | Là số trong khoảng `1-65535` | Ví dụ thường dùng: `587`, `465`. |
| Kiểu mã hóa | Có giá trị mặc định | Giá trị select `0`, `1`, hoặc `2` | `1 = STARTTLS`, `2 = SSL/TLS`, `0 = không mã hóa`. |
| Tên đăng nhập SMTP | Có | Không rỗng, đúng regex email | Thường là email SMTP. |
| Mật khẩu ứng dụng | Có khi tạo mới | Khi `hasSaved = false` thì bắt buộc. Khi đã có cấu hình lưu trước đó thì không bắt buộc nhập lại. |
| Tên người gửi | Có | Không rỗng sau `trim()` | Chưa giới hạn độ dài. |
| Email gửi đi | Có | Không rỗng, đúng regex email | Email hiển thị trong trường From. |
| Email gửi test | Có khi test | Không rỗng, đúng regex email | Chỉ validate khi bấm gửi mail kiểm tra. |

## Lập hóa đơn GTGT

File: `src/views/customers/invoices/vat-invoice/create.vue`

### Thông tin hóa đơn

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Ngày lập | Có | Không rỗng | Mặc định gán ngày hiện tại lúc tạo mới. |
| Mã đơn hàng | Không | Chưa validate | Trường tùy chọn. |
| Ký hiệu, số hóa đơn, thông tin mẫu | Readonly | Không validate client-side | Lấy từ backend/prepare. |

### Thông tin bên mua

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Mã khách hàng | Không | Chưa validate | Nếu khớp danh mục thì auto-fill thông tin. |
| Đơn vị mua | Bắt buộc có điều kiện | Phải nhập `Đơn vị mua` hoặc `Người mua`; ít nhất 1 trong 2 trường không rỗng | Cho phép hóa đơn cho cả tổ chức và cá nhân. |
| Người mua | Bắt buộc có điều kiện | Phải nhập `Đơn vị mua` hoặc `Người mua`; ít nhất 1 trong 2 trường không rỗng | Nếu `Đơn vị mua` đã có thì `Người mua` có thể rỗng. |
| Mã số thuế | Không | Nếu nhập thì phải đúng regex MST | Hóa đơn cá nhân có thể không có MST. |
| Email | Không | Nếu nhập thì phải đúng regex email | Dùng cho gửi mail hóa đơn. |
| Điện thoại | Không | Nếu nhập thì phải đúng regex phone | Trường tùy chọn. |
| Địa chỉ | Không | Chưa validate | Nên cân nhắc bắt buộc nếu nghiệp vụ yêu cầu. |
| Ngân hàng | Không | Chưa validate | Trường tùy chọn. |
| Số tài khoản | Không | Chưa validate | Trường tùy chọn. |
| Hình thức thanh toán | Có | Không rỗng | Giá trị select: tiền mặt, chuyển khoản, tiền mặt/chuyển khoản. |

### Chi tiết hàng hóa/dịch vụ

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Dòng hàng hóa | Có | Phải có ít nhất 1 dòng có `Tên hàng hóa/Dịch vụ` không rỗng | Dòng rỗng cuối bảng được bỏ qua. |
| Tên hàng hóa/Dịch vụ | Có theo dòng | Dòng được tính là hợp lệ khi có tên | Chưa giới hạn độ dài. |
| Mã hàng | Không | Chưa validate | Tùy chọn. |
| Đơn vị | Không | Chưa validate | Tùy chọn. |
| Số lượng | Có theo dòng có tên | Là số, `>= 0` | Chưa yêu cầu lớn hơn 0 vì có dòng ghi chú/khuyến mại/chiết khấu. |
| Đơn giá | Có theo dòng có tên | Là số, `>= 0` | Chưa giới hạn số chữ số/tối đa. |
| Thành tiền | Có theo dòng có tên | Là số, `>= 0` | Có thể được tính từ số lượng x đơn giá. |
| Thuế suất | Có giá trị | Theo danh sách thuế suất | Nếu là thuế suất khác thì check thêm field thuế suất khác. |
| Thuế suất khác | Có điều kiện | Khi `vatRate = -3`, giá trị phải là số trong khoảng `0-100` | Đơn vị tính là %. |
| Tiền thuế | Có theo dòng có tên | Là số, `>= 0` | Chưa giới hạn số chữ số/tối đa. |
| Tổng tiền | Có theo dòng có tên | Là số, `>= 0` | Chưa giới hạn số chữ số/tối đa. |
| Tính chất | Có giá trị | Select từ danh sách | Hiện chưa validate ngoài danh sách. |

Nếu có lỗi dòng hàng hóa, thông báo hiển thị ở cuối bảng hàng hóa/dịch vụ và chặn submit.

## Danh sách hóa đơn GTGT - modal gửi email

File: `src/views/customers/invoices/vat-invoice/list.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Tên người nhận | Có | Không rỗng sau `trim()` | Chưa giới hạn độ dài. Thường được nạp sẵn từ thông tin khách hàng nếu có. |
| Email người nhận | Có | Không rỗng, input `type=email` và hàm validate email riêng trong modal | Chưa giới hạn độ dài. |

Nếu hóa đơn không có MST bên bán trong app/profile thì các nút xem/tải public có thể báo lỗi không xác định MST.

## Mẫu hóa đơn phía khách hàng

File: `src/views/customers/form-invoice/create.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Tên mẫu | Có | Không rỗng sau `trim()` | Chưa giới hạn độ dài frontend. |
| Ký tự loại hóa đơn | Readonly | Từ `category`, hợp lệ khi là `1` hoặc `2` | Lấy từ mẫu/dữ liệu backend. |
| Hình thức có mã/không mã | Có giá trị mặc định | Chọn `C` hoặc `K` | Mặc định `C`. |
| Năm ký hiệu | Readonly | 2 chữ số cuối của năm hiện tại hoặc dữ liệu cũ | Ví dụ `26`. |
| Hậu tố ký hiệu | Có | Đúng chính xác 2 chữ cái `A-Z`; input có `maxlength=2` và tự động viết hoa | Regex: `^[A-Z]{2}$`. Ký tự khác chữ cái bị loại bỏ khi nhập. |
| Ký hiệu đầy đủ | Có theo kết quả ghép | Được ghép từ các thành phần trên | Ví dụ dạng `1C26THD`. |
| Trạng thái | Có giá trị mặc định | `1` hoặc `0` | Mặc định kích hoạt. |

## Import hóa đơn từ Excel

File: `src/views/customers/imports/invoice/index.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| File Excel import | Có khi import | Nút import disabled nếu chưa chọn file; hàm upload cũng chặn khi `selectedFile` rỗng | Input chấp nhận `.xlsx,.xls`. Chưa validate dung lượng file/tối đa số dòng tại frontend. |
| Import lại file cũ | Có điều kiện | Cần có `id` bản ghi import và user xác nhận confirm | Tạo thêm hóa đơn nháp mới từ file cũ. |

## Mua gói hóa đơn

File: `src/views/customers/invoice-packages/index.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Gói hóa đơn | Có khi thanh toán | Chỉ mở modal khi chọn 1 gói | Gói lấy từ backend và phải đang hiển thị. |
| Phương thức thanh toán | Có giá trị mặc định | `MOMO` hoặc `VNPAY` | Mặc định `MOMO`. |
| Lọc lịch sử: Từ ngày/Đến ngày | Không | Input `type=date`; có `min/max` ràng buộc từ ngày <= đến ngày trên UI | Nếu nhập bằng tay sai format thì browser quyết định. |
| Lọc lịch sử: Từ khóa/trạng thái/thanh toán | Không | Chưa validate | Dùng để filter danh sách giao dịch. |

## Tờ khai hóa đơn điện tử

File: `src/views/customers/registers/invoice/create.vue`

| Nhóm/Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Ngày lập tờ khai | Có | Không rỗng | Mặc định là ngày hiện tại. |
| Hình thức hóa đơn | Có | Chọn ít nhất 1 checkbox | Ví dụ: có mã, không có mã, mã từ máy tính tiền. |
| Hình thức gửi dữ liệu | Có | Chọn ít nhất 1 lựa chọn trong nhóm A, B hoặc C | Nếu tick nhóm A/B thì các lựa chọn con được chọn theo logic hiện có. |
| Phương thức chuyển dữ liệu | Có | Chọn ít nhất 1 checkbox | Chuyển đầy đủ hoặc chuyển theo bảng tổng hợp. |
| Loại hóa đơn sử dụng | Có | Chọn ít nhất 1 checkbox | Ví dụ: HĐGTGT, HĐBHàng... |
| Nơi lập | Có | Không rỗng | Chọn từ danh sách tỉnh/thành phố. |
| Danh sách chữ ký số | Không bắt buộc ở form chính | Chưa yêu cầu có ít nhất 1 chữ ký số | Chỉ validate khi thêm/cập nhật từng chữ ký trong modal. |

Lỗi tổng hợp hiển thị ở đầu form, đồng thời lỗi từng nhóm hiển thị gần nhóm input.

### Modal chữ ký số

File: `src/views/customers/registers/invoice/create.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Tên tổ chức chứng thực/cấp/công nhận chữ ký số | Có | Không rỗng sau `trim()` | Chưa giới hạn độ dài. |
| Số sê-ri chứng thư | Có | Không rỗng sau `trim()` | Chưa validate format hex/base64/serial riêng. |
| Thời gian từ ngày | Có | Không rỗng | Chọn bằng datepicker. |
| Thời gian đến ngày | Có | Không rỗng, phải sau hoặc bằng `từ ngày` | Nếu nhỏ hơn ngày bắt đầu thì báo lỗi. |
| Hình thức đăng ký | Có giá trị mặc định | Giá trị mặc định `1 = Thêm mới` | Có thể chọn gia hạn/ngừng sử dụng. |

## Quản lý thành viên và phân quyền

File: `src/views/settings/member/list.vue`

### Modal thêm/cập nhật thành viên

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Họ và tên | Có | Input `required`, `canSubmitForm` yêu cầu có giá trị | Chưa giới hạn độ dài frontend. |
| Điện thoại | Không | Chưa validate | Trường tùy chọn, chưa dùng regex phone. |
| Email | Không | Input `type=email` nên browser có thể check format nếu nhập | Chưa bắt buộc và chưa dùng regex email dùng chung. |
| Username | Có khi tạo mới theo logic submit | `canSubmitForm` yêu cầu có username khi `!form.id` | UI hiện tại không có input username trong modal, cần kiểm tra luồng tạo user/backend có tự sinh username hay không. |
| Mật khẩu | Có khi tạo mới; không bắt buộc khi cập nhật nếu không đổi | Nếu nhập thì tối thiểu 8 ký tự và phải khớp confirm | Nút Lưu disabled nếu không hợp lệ. |
| Nhập lại mật khẩu | Có khi cần mật khẩu | Phải khớp mật khẩu | Lỗi hiển thị `Mật khẩu không khớp`. |
| Đặt user này là Admin | Không | Chỉ hiển thị theo quyền root | Khi tick thì có thể nhập mật khẩu quản trị. |
| Mật khẩu quản trị | Không bắt buộc nếu không nhập | Nếu nhập và user là admin thì tối thiểu 8 ký tự, phải khớp confirm | Có nút tạo mật khẩu mạnh 14 ký tự. |

### Modal phân quyền

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Thành viên cần phân quyền | Có | `userId` phải là số hợp lệ | Nút Lưu disabled nếu không có userId. |
| Danh sách quyền | Không bắt buộc | Checkbox lưu danh sách permission override `allowed = 0/1` | Không yêu cầu chọn ít nhất 1 quyền. |

## Các form quản trị đang dùng HTML/backend validate

Những form dưới đây có bắt buộc/có giới hạn nhất định trên UI, nhưng chưa dùng chung `src/utils/validators.js`. Nếu cần chuẩn hóa production, nên chuyển dần sang helper validate chung để thông báo lỗi đồng nhất.

### Quản lý công ty

File: `src/views/administrators/company/list.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Tên công ty | Có | Input `required`, trim trước khi gửi | Chưa giới hạn độ dài frontend. |
| Mã số thuế | Có | Input `required`, trim trước khi gửi | Chưa dùng regex MST ở frontend trong form admin này. |
| Địa chỉ | Không | Trim trước khi gửi | Chưa giới hạn độ dài. |
| Email | Không | Input `type=email` | Browser check format nếu nhập; chưa dùng regex email chung. |
| Hotline | Không | Trim trước khi gửi | Chưa dùng regex phone. |
| Trạng thái | Có giá trị mặc định | `1`, `0`, hoặc `2` | Kích hoạt, ngừng hoạt động, chưa kích hoạt. |

### Mẫu email

File: `src/views/administrators/email-template/create.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Loại template | Có | Select `system` có `required` | Giá trị `1 = cá nhân`, `0 = hệ thống`. |
| Mã template | Có khi tạo mới | Input `required`; disabled khi cập nhật | Chưa validate pattern key, nên backend cần chặn trùng/sai format. |
| Tên template | Có | Input `required` | Chưa giới hạn độ dài frontend. |
| Nội dung email | Có theo label | Chưa có check client-side rõ ràng trong `submit()` | Editor có thể rỗng và backend cần validate. |
| Trạng thái | Có giá trị mặc định | `1` hoặc `0` | Checkbox. |

### Mẫu hóa đơn phía quản trị

File: `src/views/administrators/form-invoice/create.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Tên mẫu | Có | `required` và hàm submit check không rỗng sau `trim()` | Chưa giới hạn độ dài. |
| Loại hóa đơn | Có | Select `required`; mặc định `1` | `1 = GTGT`, `2 = bán hàng`. |
| Thuế suất | Có | Select `required`; mặc định `1` | `1 = một thuế suất`, `2 = nhiều thuế suất`. |
| Hình thức | Không | Giá trị `1` hoặc `0` | Mặc định có mã. |
| Trạng thái | Không | Giá trị `1` hoặc `0` | Mặc định chưa kích hoạt. |
| Tập tin mẫu XSLT/XML | Không | Input accept `.xslt,.xml,text/xml` | Chưa validate dung lượng/nội dung file frontend. |
| Ảnh mẫu | Không | Input accept `.png,.jpg,.jpeg` | Chưa validate dung lượng/kích thước frontend. |

### Gói hóa đơn

File: `src/views/administrators/invoice-package/list.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Tên gói | Có | Input `required` | Chưa giới hạn độ dài. |
| Thứ tự | Không | Input number `min=0` | Chưa validate trong hàm save. |
| Số hóa đơn | Có | Input number `min=1`, `required` | Chưa validate trong hàm save nếu browser bị bỏ qua. |
| Đơn giá | Có | Input number `min=0`, `required` | Thành tiền được tính lại từ số lượng x đơn giá khi input. |
| Thành tiền | Có | Input number `min=0`, `required` | Có thể bị sửa tay, backend cần validate lại. |
| Gói trải nghiệm | Không | Boolean | Checkbox. |
| Mô tả | Không | Chưa validate | Chưa giới hạn độ dài. |
| Trạng thái | Có giá trị mặc định | `1` hoặc `0` | Kích hoạt/ngừng. |

### Ngân hàng

File: `src/views/administrators/bank/list.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Mã ngân hàng/viết tắt | Có | Input `required`, submit check không rỗng | Chưa giới hạn độ dài/chỉ cho chữ in hoa. |
| Tên đầy đủ | Có | Input `required`, submit check không rỗng | Chưa giới hạn độ dài. |
| Trạng thái | Có giá trị mặc định | `1` hoặc `0` | Hoạt động/tạm khóa. |

### Thuế suất

File: `src/views/administrators/vat-rate/list.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Tên/nhãn thuế suất | Không | Nếu rỗng thì tự sinh theo giá trị code | Chưa giới hạn độ dài. |
| Giá trị | Có | Input number `required` | Cho phép cả giá trị đặc biệt như `-1`; chưa giới hạn min/max frontend. |
| Thứ tự hiển thị | Không | Input number | Khi kéo thả thì cập nhật order theo index. |
| Trạng thái | Có giá trị mặc định | `1` hoặc `0` | Hiển thị/ẩn. |

### Cơ quan thuế

File: `src/views/administrators/tax-authority/list.vue`

| Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Mã CQT | Có | Input `required` | Chưa giới hạn độ dài/format mã CQT frontend. |
| Tên Cơ quan thuế | Có | Input `required` | Chưa giới hạn độ dài. |
| Tỉnh/Thành phố | Không | Chưa validate | Trường tùy chọn. |
| Cơ quan quản lý cấp cha | Không | Chọn từ danh sách, không cho chọn chính mình khi cập nhật | Có thể để rỗng để làm cấp cao nhất. |
| Trạng thái | Có giá trị mặc định | `1` hoặc `0` | Hoạt động/ẩn. |

### Quyền và nhóm quyền

File:

- `src/views/administrators/access-control/permissions/list.vue`
- `src/views/administrators/access-control/permission-categories/list.vue`

| Form/Trường | Bắt buộc | Điều kiện validate | Ghi chú |
| --- | --- | --- | --- |
| Quyền: Tên quyền | Có | `canSubmit` yêu cầu không rỗng | Chưa giới hạn độ dài/pattern. |
| Quyền: Tên hiển thị | Có | `canSubmit` yêu cầu không rỗng | Chưa giới hạn độ dài. |
| Quyền: Level | Có giá trị | Số trong khoảng `0-2` | Input có `min=0`, `max=2` và computed `validLevel`. |
| Quyền: Nhóm quyền | Có | Phải chọn nhóm đang hiển thị | Hiển thị lỗi nếu chưa chọn. |
| Quyền: Mô tả | Không | Chưa validate | Chưa giới hạn độ dài. |
| Quyền: Trạng thái | Có giá trị mặc định | `1` hoặc `0` | Hiển thị/ẩn. |
| Nhóm quyền: Tên nhóm | Có | Input `required` | Hàm save chưa check riêng ngoài HTML/browser. |
| Nhóm quyền: Thứ tự hiển thị | Không | Input number | Khi kéo thả thì cập nhật order theo danh sách. |
| Nhóm quyền: Trạng thái | Có giá trị mặc định | `1` hoặc `0` | Hiển thị/ẩn. |

## Các form đã có validate sẵn từ trước

Một số form vẫn đang dựa vào `required`, `type=email`, `min/max` của HTML hoặc lỗi backend trả về, không phải validate client-side đồng nhất. Tài liệu đã ghi rõ các trường hợp này để khi review production có thể quyết định form nào cần siết tiếp.

## Các điểm nên bổ sung tiếp nếu cần chặt hơn

- Max length theo database cho tên công ty, địa chỉ, tên khách hàng, tên hàng hóa, ghi chú.
- Quy tắc mật khẩu mạnh hơn: chữ hoa, chữ thường, số, ký tự đặc biệt.
- Validate hostname SMTP chi tiết hơn thay vì chỉ bắt buộc không rỗng.
- Validate số tài khoản ngân hàng, fax, website nếu nghiệp vụ yêu cầu.
- Bắt buộc địa chỉ bên mua trên hóa đơn nếu mẫu hóa đơn/quy định nội bộ yêu cầu.
- Validate định dạng mã tra cứu hóa đơn nếu backend có quy tắc cố định về độ dài/ký tự.
