# Tổng hợp validate form

Cập nhật: 17/06/2026

Tài liệu này ghi lại quy ước validate form đang áp dụng trong hệ thống. Mục tiêu là khi sửa hoặc thêm màn hình mới, giao diện không còn hiện tooltip validate mặc định của trình duyệt, thông báo lỗi đồng nhất bằng BootstrapVue trên form và Toastr ở góc màn hình.

Validate phía giao diện chỉ dùng để chặn lỗi sớm. Backend vẫn phải kiểm tra lại dữ liệu quan trọng trước khi lưu.

## Quy ước hiện hành

- Form có validate riêng phải dùng `novalidate` và bắt submit bằng `@submit.prevent`.
- Không dựa vào tooltip mặc định của trình duyệt cho `required`, `type=email`, `min`, `max`.
- Lỗi dưới input dùng `:state`, `b-form-invalid-feedback` hoặc vùng lỗi cùng phong cách BootstrapVue.
- Nội dung lỗi hiển thị cho người dùng phải viết bằng tiếng Việt.
- Khi component tự xử lý lỗi API, request phải truyền `meta: { suppressGlobalErrorToast: true }` để tránh hiện 2 toast.
- Lỗi quyền `403` không được thay bằng câu chung chung nếu backend đã trả thông báo chi tiết. Thông báo cần giữ dạng rõ quyền thiếu, ví dụ `Thiếu quyền: Xem danh mục sản phẩm`.
- Thông báo toàn hệ thống chỉ dùng Toastr. Không thêm mới `$bvToast`; nếu gặp code cũ thì chuyển sang `this.$toastr` hoặc helper trong `src/utils/toast.js`.
- Những API phụ như dropdown, autocomplete, danh sách gợi ý không được làm hỏng form chính nếu user thiếu quyền phụ. Các request này nên dùng `suppressGlobalErrorToast` và xử lý fallback tại component.

## Helper validate dùng chung

File: `src/utils/validators.js`

| Helper | Mục đích | Ghi chú |
| --- | --- | --- |
| `required(value, message)` | Kiểm tra bắt buộc | Rỗng nếu `null`, `undefined` hoặc `trim()` không còn nội dung. |
| `email(value, message)` | Kiểm tra định dạng email | Trường rỗng được xem là hợp lệ, nên kết hợp `required` nếu bắt buộc. |
| `phone(value, message)` | Kiểm tra số điện thoại | Bỏ khoảng trắng, dấu chấm, dấu gạch trước khi kiểm tra. Chấp nhận `0...`, `84...`, `+84...`. |
| `taxCode(value, message)` | Kiểm tra mã số thuế | Chấp nhận MST 10 số, 13 số hoặc dạng `0123456789-001`. |
| `minLength(value, min, message)` | Kiểm tra độ dài tối thiểu | Đang dùng chủ yếu cho mật khẩu. |
| `numberRange(value, min, max, message)` | Kiểm tra số trong khoảng | Cho phép bỏ qua `min` hoặc `max` bằng `null`. |
| `url(value, message)` | Kiểm tra website | Tự thêm `https://` khi người dùng nhập thiếu giao thức. |
| `firstError(rules)` | Lấy lỗi đầu tiên | Dùng để gom nhiều rule cho một field. |
| `hasErrors(errors)` | Kiểm tra object lỗi | Dùng trước khi submit. |

## Thông báo lỗi và toast

File liên quan:

- `src/main.js`
- `src/plugins/axios.js`
- `src/utils/toast.js`

Trạng thái hiện tại:

- Toastr được cấu hình toàn cục với nút đóng, progress bar, vị trí góc phải trên và chống trùng thông báo.
- `src/utils/toast.js` có `toastError`, `toastWarning`, `toastSuccess`, `clearToastKeys` để chống double toast theo khóa lỗi.
- `src/plugins/axios.js` xử lý lỗi `401`, `403` và lỗi API chung.
- `meta.suppressGlobalErrorToast === true` dùng cho những request mà component đã tự hiện lỗi tại form hoặc tự quyết định không cần toast.
- `main.js` có lớp chuyển tiếp để các lời gọi toast kiểu BootstrapVue cũ không tạo giao diện thông báo riêng. Code mới vẫn phải dùng Toastr trực tiếp.

## Các nhóm form đã chuẩn hóa chính

### Đăng ký, đăng nhập, quên mật khẩu

File:

- `src/views/auth/register.vue`
- `src/views/auth/login.vue`
- `src/views/auth/login_admin.vue`
- `src/views/auth/forgot_password.vue`
- `src/views/auth/reset_password.vue`

Quy tắc chính:

- Đăng ký bắt buộc tên công ty, mã số thuế, địa chỉ, email.
- Mã số thuế dùng rule MST, email dùng rule email, điện thoại nếu nhập phải đúng định dạng.
- Đăng nhập bắt buộc tài khoản và mật khẩu.
- Quên mật khẩu bắt buộc tài khoản và email.
- Đặt lại mật khẩu bắt buộc token, mật khẩu mới tối thiểu 8 ký tự và nhập lại phải khớp.

### Tài khoản cá nhân

File: `src/views/settings/account/list.vue`

Quy tắc chính:

- Các form đổi tên, email, điện thoại dùng `b-form novalidate`.
- Tên bắt buộc.
- Email bắt buộc và đúng định dạng.
- Số điện thoại bắt buộc và đúng định dạng.
- Đổi mật khẩu bắt buộc mật khẩu hiện tại, mật khẩu mới tối thiểu 8 ký tự và nhập lại phải khớp.
- Cập nhật ảnh đại diện gửi file crop PNG lên backend. Giao diện chưa giới hạn dung lượng file.

### Hồ sơ công ty

File: `src/views/settings/profile/list.vue`

Trang này đã chuyển các form con sang `b-form novalidate` và lỗi BootstrapVue.

| Nhóm | Validate chính |
| --- | --- |
| Thông tin đơn vị | Bắt buộc ngành nghề, tên đơn vị, địa chỉ. Logo và favicon nếu chọn file thì chỉ hỗ trợ PNG, JPG, JPEG. |
| Người đại diện pháp luật | Bắt buộc họ tên, điện thoại, ngày sinh, giới tính, email. Điện thoại và email kiểm tra định dạng. CCCD nếu nhập phải gồm 9 hoặc 12 chữ số. Hộ chiếu nếu nhập chỉ gồm chữ và số, từ 6 đến 20 ký tự. Ngày sinh dạng `dd/mm/yyyy`, năm hợp lệ và không được lớn hơn ngày hiện tại. |
| Thông tin hiển thị trên hóa đơn | Bắt buộc email, điện thoại, fax, website. Email, điện thoại, fax, website đều có kiểm tra định dạng. |
| Thông tin liên hệ | Bắt buộc họ tên, email, điện thoại, địa chỉ. Email và điện thoại kiểm tra định dạng. |
| Ngân hàng | Bắt buộc số tài khoản và tên ngân hàng. Số tài khoản kiểm tra định dạng. Chi nhánh nếu nhập không được vượt quá 255 ký tự. |
| Cơ quan thuế | Bắt buộc chọn cục thuế tỉnh/thành và cơ quan thuế quản lý. |

### Thành viên và phân quyền

File: `src/views/settings/member/list.vue`

Quy tắc chính:

- Modal thêm/cập nhật thành viên dùng `b-form novalidate`.
- Họ tên bắt buộc.
- Điện thoại và email không bắt buộc, nhưng nếu nhập thì phải đúng định dạng.
- Khi tạo mới, mật khẩu bắt buộc, tối thiểu 8 ký tự và nhập lại phải khớp.
- Khi cập nhật, mật khẩu không bắt buộc; nếu nhập thì vẫn phải tối thiểu 8 ký tự và khớp xác nhận.
- Mật khẩu quản trị chỉ validate khi trường này được phép hiển thị cho tài khoản quản trị công ty root.
- Tài khoản quản trị full quyền user nên không cần insert thêm `user_permissions` cho các quyền level 0.
- Modal phân quyền chỉ phục vụ nhân viên. Với tài khoản quản trị công ty root chỉ hiển thị nhóm quyền admin; tài khoản quản trị công ty thường không hiện phân quyền thành viên.
- Danh sách quyền chỉ load khi user có quyền thao tác phù hợp, tránh nhân viên được quyền xem thành viên nhưng bị màn hình trống do thiếu quyền sửa.

### Danh mục khách hàng

File: `src/views/customers/categories/customer/list.vue`

Quy tắc chính:

- Mã khách hàng và tên khách hàng bắt buộc.
- Mã số thuế, số điện thoại, email nhận hóa đơn là tùy chọn nhưng nếu nhập phải đúng định dạng.
- Khi component tự xử lý lỗi lưu, request dùng `suppressGlobalErrorToast` để không hiện double toast.
- Nếu thiếu quyền khi tải danh sách, dùng thông báo quyền chi tiết từ backend.

### Danh mục hàng hóa

File: `src/views/customers/categories/product/list.vue`

Quy tắc chính:

- Mã sản phẩm và tên sản phẩm bắt buộc.
- Đơn giá bắt buộc, là số và không âm.
- Thuế suất có giá trị mặc định theo danh sách thuế suất.
- Lỗi quyền khi mở danh sách phải hiện rõ quyền thiếu, ví dụ `Thiếu quyền: Xem danh mục sản phẩm`.
- Các thao tác lưu, xóa, đổi trạng thái dùng Toastr và chống double toast.

### Lập hóa đơn GTGT

File: `src/views/customers/invoices/vat-invoice/create.vue`

Quy tắc chính:

- Trước khi lập hóa đơn, màn hình gọi `/invoices/prepare`; endpoint này kiểm tra quyền `invoice-save` và trả thông tin mẫu hóa đơn đang dùng.
- Không còn phụ thuộc vào API danh sách mẫu hóa đơn để quyết định quyền lập hóa đơn.
- API phụ như khách hàng, hàng hóa, thuế suất nếu thiếu quyền sẽ không làm hỏng form chính.
- Ngày lập hóa đơn bắt buộc.
- Bên mua bắt buộc nhập ít nhất một trong hai trường `Đơn vị mua` hoặc `Người mua`.
- MST, email, điện thoại bên mua nếu nhập phải đúng định dạng.
- Hình thức thanh toán bắt buộc.
- Chi tiết hóa đơn phải có ít nhất một dòng hàng hóa/dịch vụ có tên.
- Số lượng, đơn giá, thành tiền, tiền thuế, tổng tiền không được âm.
- Thuế suất khác chỉ hợp lệ trong khoảng 0 đến 100.

### Danh sách hóa đơn GTGT

File: `src/views/customers/invoices/vat-invoice/list.vue`

Quy tắc chính:

- Modal gửi email bắt buộc tên người nhận và email người nhận.
- Email người nhận phải đúng định dạng.
- Các hành động phát hành, gửi email, tải file cần giữ thông báo lỗi nghiệp vụ từ backend.

### Import hóa đơn

File: `src/views/customers/imports/invoice/index.vue`

Quy tắc chính:

- Import bắt buộc chọn file Excel.
- Input nhận `.xlsx` và `.xls`.
- Import lại file cũ cần có ID bản ghi import và xác nhận từ người dùng.
- Chưa validate dung lượng file và số dòng ở phía giao diện.

### Tờ khai đăng ký hóa đơn điện tử

File: `src/views/customers/registers/invoice/create.vue`

Quy tắc chính:

- Ngày lập và nơi lập bắt buộc.
- Nhóm hình thức hóa đơn phải chọn ít nhất một lựa chọn.
- Nhóm hình thức gửi dữ liệu và phương thức chuyển dữ liệu phải có lựa chọn hợp lệ.
- Loại hóa đơn sử dụng phải chọn ít nhất một loại.
- Modal chữ ký số bắt buộc tên tổ chức chứng thực, số sê-ri, từ ngày, đến ngày.
- Đến ngày của chữ ký số phải lớn hơn hoặc bằng từ ngày.
- Thông báo gửi tờ khai đã chuyển về Toastr, không dùng kiểu thông báo BootstrapVue riêng.

### Cấu hình máy chủ gửi mail

File: `src/views/customers/email/mail-server.vue`

Quy tắc chính:

- SMTP host, cổng, kiểu mã hóa, tên đăng nhập SMTP, tên người gửi, email gửi đi là các trường chính cần hợp lệ.
- Cổng SMTP phải nằm trong khoảng 1 đến 65535.
- Email SMTP, email gửi đi và email gửi test phải đúng định dạng.
- Mật khẩu ứng dụng bắt buộc khi tạo mới cấu hình; khi đã lưu thì không bắt buộc nhập lại.

### Mẫu hóa đơn phía khách hàng

File: `src/views/customers/form-invoice/create.vue`

Quy tắc chính:

- Tên mẫu bắt buộc.
- Hậu tố ký hiệu bắt buộc đúng 2 chữ cái `A-Z`, tự viết hoa khi nhập.
- Ký hiệu đầy đủ được ghép từ loại hóa đơn, hình thức, năm ký hiệu và hậu tố.

## Nhóm form quản trị

Các màn quản trị đã được rà soát lại giao diện modal, spacing form và trạng thái nút thao tác. Những form này vẫn cần backend validate lại vì một số trường chưa có giới hạn tối đa ở frontend.

| Màn hình | File | Validate chính |
| --- | --- | --- |
| Công ty | `src/views/administrators/company/list.vue` | Tên công ty và mã số thuế bắt buộc. Email nếu nhập phải đúng định dạng. |
| Đăng ký công ty | `src/views/administrators/company-registration/list.vue` | Dữ liệu đăng ký hiển thị theo công ty, thao tác duyệt cần giữ lỗi backend. |
| Gói hóa đơn | `src/views/administrators/invoice-package/list.vue` | Tên gói, số hóa đơn, đơn giá, thành tiền là các trường chính. Số hóa đơn không nhỏ hơn 1, tiền không âm. |
| Hạn mức hóa đơn mua | `src/views/administrators/buy-invoice/list.vue` | Chọn công ty, số lượng và trạng thái. Dropdown công ty là dữ liệu phụ, lỗi phụ không được làm vỡ giao diện. |
| Ngân hàng | `src/views/administrators/bank/list.vue` | Mã ngân hàng và tên đầy đủ bắt buộc. |
| Cơ quan thuế | `src/views/administrators/tax-authority/list.vue` | Mã CQT và tên cơ quan thuế bắt buộc. Cơ quan cha không được là chính nó khi cập nhật. |
| Thuế suất | `src/views/administrators/vat-rate/list.vue` | Giá trị thuế suất bắt buộc là số. Thứ tự hiển thị là số. Giao diện đã đồng bộ theo danh sách nhóm quyền. |
| Quyền | `src/views/administrators/access-control/permissions/list.vue` | Tên quyền, tên hiển thị, level và nhóm quyền bắt buộc. Level nằm trong khoảng 0 đến 2. Có bộ lọc danh sách quyền. |
| Nhóm quyền | `src/views/administrators/access-control/permission-categories/list.vue` | Tên nhóm bắt buộc. Có sắp xếp nhanh bằng nhập STT/nút lên xuống ngoài kéo thả. |
| Mẫu email | `src/views/administrators/email-template/create.vue` | Loại template, mã template khi tạo mới, tên template và trạng thái là các trường chính. |
| Mẫu hóa đơn hệ thống | `src/views/administrators/form-invoice/create.vue` | Tên mẫu, loại hóa đơn, thuế suất, hình thức, trạng thái và file mẫu. |

## Những điểm còn cần siết nếu làm chặt hơn

- Chưa có giới hạn tối đa thống nhất theo database cho tên công ty, địa chỉ, tên khách hàng, tên hàng hóa, ghi chú.
- Chưa áp dụng chính sách mật khẩu mạnh gồm chữ hoa, chữ thường, số và ký tự đặc biệt; hiện chủ yếu là tối thiểu 8 ký tự.
- Chưa validate dung lượng file upload trên hầu hết màn hình.
- Một số form quản trị cũ vẫn còn dựa một phần vào backend validate. Khi sửa tiếp nên chuyển dần về helper chung.
- Các field như fax, website, số tài khoản ngân hàng đã có kiểm tra ở hồ sơ công ty, nhưng chưa chắc đã được áp dụng giống nhau ở mọi form khác.
- Các input `type=email`, `type=number`, `required` có thể vẫn tồn tại để hỗ trợ bàn phím và accessibility, nhưng form có validate riêng phải dùng `novalidate` để không hiện tooltip mặc định của trình duyệt.

## Checklist khi thêm hoặc sửa form

1. Thêm `novalidate` cho form nếu tự validate.
2. Dùng helper trong `src/utils/validators.js` trước khi tự viết regex mới.
3. Hiển thị lỗi ngay dưới input bằng BootstrapVue state/feedback.
4. Nội dung lỗi viết tiếng Việt, ngắn và chỉ rõ field sai.
5. Không dùng `$bvToast`; dùng `this.$toastr` hoặc helper trong `src/utils/toast.js`.
6. Request đã tự xử lý lỗi tại component phải truyền `meta: { suppressGlobalErrorToast: true }`.
7. Với lỗi quyền, ưu tiên hiển thị message từ backend để người dùng biết thiếu quyền nào.
8. Không tin dữ liệu từ frontend; backend vẫn validate lại trước khi ghi database.

## Lệnh kiểm tra nên chạy

Khi chỉ sửa tài liệu:

```bash
git diff --check -- FORM_VALIDATION_SUMMARY.md
```

Khi sửa giao diện Vue:

```bash
npx vue-cli-service build --dest /tmp/hoadon-validation-build
```

Khi sửa backend validate hoặc phân quyền:

```bash
JAVA_HOME=/home/nvpa/.local/share/jdks/jdk-21.0.11+10 ./mvnw -DskipTests clean compile
```
