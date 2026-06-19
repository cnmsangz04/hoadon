# Quy tắc và vai trò của chương trình

Cập nhật: 19/06/2026

Tài liệu này mô tả các vai trò, phạm vi quyền, rule phân quyền và rule nghiệp vụ quan trọng của hệ thống hóa đơn điện tử. Khi sửa quyền, thêm màn hình mới hoặc thêm API mới, cần đối chiếu file này với code backend để tránh mở nhầm dữ liệu công ty khác hoặc chặn sai người dùng hợp lệ.

## 1. Bảng và trường liên quan

Các bảng chính:

| Bảng | Ý nghĩa |
| --- | --- |
| `users` | Tài khoản đăng nhập. Chứa `company_id`, `role`, `admin_scope`, `admin_password`, `status`. |
| `companies` | Công ty/đơn vị sử dụng hệ thống. Chứa trạng thái kích hoạt công ty. |
| `permissions` | Danh sách quyền chi tiết. `name` là key kiểm tra trong code, `display_name` là tên hiển thị khi báo thiếu quyền. |
| `permission_categories` | Nhóm quyền dùng để gom quyền trên màn hình phân quyền. |
| `user_permissions` | Quyền riêng được bật/tắt cho từng user. `allowed = 1` là cho phép, `allowed = 0` là từ chối. |
| `login_sessions` | Phiên đăng nhập hiện tại, dùng để thu hồi hoặc kiểm tra phiên. |
| `login_history` | Lịch sử đăng nhập. |

Các trường quan trọng trong `users`:

| Trường | Rule |
| --- | --- |
| `company_id` | Xác định công ty sở hữu user. Hầu hết dữ liệu nghiệp vụ phải lọc theo company này. |
| `role` | Vai trò cấp hệ thống: `0 = Root`, `1 = Quản trị công ty`, `2 = Nhân viên`. |
| `admin_scope` | Phạm vi admin: `ROOT`, `ROOT_COMPANY`, `COMPANY`, hoặc `null` với nhân viên. |
| `admin_password` | Mật khẩu riêng cho khu Quản trị. Chỉ Root hoặc admin công ty root dùng được. |
| `status` | `1 = hoạt động`, `0 = khóa/ngừng hoạt động`. User không hoạt động không được đăng nhập. |
| `deleted_at` | Nếu có giá trị thì user bị xóa mềm, không nên hiện trong danh sách thành viên. |

## 2. Vai trò hệ thống

### 2.1 Root

Giá trị:

- `users.role = 0`
- `users.admin_scope = ROOT`

Rule:

- Có toàn quyền trong hệ thống.
- `PermissionServiceImpl.hasPermission()` luôn cho phép Root.
- Được vào khu Quản trị bằng `/auth/login-admin` nếu có `admin_password`.
- Được thao tác dữ liệu nhiều công ty khi API hỗ trợ lọc công ty.
- Không được tạo thêm user role Root từ màn hình thành viên. Service chặn payload có `role = 0`.
- Tài khoản Root không được thao tác/xóa/khóa bởi admin công ty.

### 2.2 Admin công ty root

Giá trị:

- `users.role = 1`
- `users.company_id = 1`
- `users.admin_scope = ROOT_COMPANY`

Rule:

- Là quản trị thuộc công ty root.
- Được hiện dropdown `Quản trị` ở header.
- Được đăng nhập khu Quản trị bằng `admin_password`.
- Được cho qua các quyền user level 0.
- Khi phân quyền thành viên cho tài khoản này, chỉ hiển thị nhóm quyền admin; không cần gán quyền user level 0 vì role admin đã mặc định qua.
- Có thể quản lý khu admin theo các permission admin tương ứng.

### 2.3 Admin công ty thường

Giá trị:

- `users.role = 1`
- `users.company_id != 1`
- `users.admin_scope = COMPANY`

Rule:

- Là quản trị của một công ty khách hàng.
- Không hiện dropdown `Quản trị` ở header.
- Không đăng nhập khu Quản trị bằng `/auth/login-admin`.
- Không có `admin_password`; nếu chuyển khỏi admin công ty root thì `admin_password` phải bị xóa.
- Được cho qua các quyền user level 0.
- Không cần insert `user_permissions` cho quyền level 0.
- Được quản lý thành viên trong công ty mình theo các quyền `setting-member-*`.
- Không được thao tác admin khác trong cùng công ty, trừ thao tác chính mình khi rule cho phép.

### 2.4 Nhân viên

Giá trị:

- `users.role = 2`
- `users.admin_scope = null`
- `users.admin_password = null`

Rule:

- Chỉ có quyền khi được gán trong `user_permissions`.
- Chỉ được dùng các quyền level 0.
- Không được vào khu Quản trị.
- Có thể vào menu Thành viên nếu được cấp quyền `setting-member-list`.
- Nếu thiếu quyền khi vào API, backend trả `403` kèm tên quyền thiếu.
- Không được thao tác tài khoản admin/root.
- Không được tự phân quyền cho chính mình.

### 2.5 Khách chưa đăng nhập

Rule:

- Chỉ dùng được API public hoặc auth.
- Có thể đăng ký công ty, đăng nhập, quên mật khẩu, đặt lại mật khẩu.
- Có thể tra cứu hóa đơn public theo rule của API public.
- Không có quyền truy cập API nghiệp vụ dưới `/v1/**` nếu không có JWT.

## 3. Admin scope

`admin_scope` dùng để phân biệt phạm vi admin trong cùng `role = 1`.

| Giá trị | Ý nghĩa | Rule |
| --- | --- | --- |
| `ROOT` | Tài khoản Root hệ thống | Chỉ dùng với `role = 0`. |
| `ROOT_COMPANY` | Admin thuộc công ty root | `role = 1`, `company_id = 1`, có thể vào khu Quản trị nếu có `admin_password`. |
| `COMPANY` | Admin công ty thường | `role = 1`, `company_id != 1`, không có quyền vào khu Quản trị. |
| `null` | Nhân viên | Không có quyền admin mặc định. |

Rule chuẩn hóa:

- Root giữ `admin_scope = ROOT`.
- Role không phải admin/root thì xóa `admin_scope` và `admin_password`.
- Admin công ty thường luôn xóa `admin_password`.
- Admin công ty root chỉ cập nhật `admin_password` khi có payload gửi lên.

## 4. JWT và đăng nhập

JWT có thời hạn 4 giờ.

Các claim quan trọng:

| Claim | Ý nghĩa |
| --- | --- |
| `userId` | ID user. |
| `role` | Role số của user. |
| `companyId` | Công ty của user. |
| `adminScope` | Phạm vi admin. |
| `rootCompanyAdmin` | `true` nếu là admin công ty root. |
| `adminAccess` | `true` nếu user có thể vào khu Quản trị. |
| `sid` | ID phiên đăng nhập. |

Rule đăng nhập user:

- Endpoint: `/v1/auth/login`.
- Kiểm tra username tồn tại.
- User phải `status = 1`.
- Mật khẩu thường phải đúng.
- IP phải hợp lệ nếu công ty bật bảo mật IP.
- Tạo phiên đăng nhập loại `USER`.
- Trả token lưu ở frontend bằng key `token`.

Rule đăng nhập admin:

- Endpoint: `/v1/auth/login-admin`.
- User phải `status = 1`.
- User phải `canAccessAdminArea() = true`, tức Root hoặc admin công ty root.
- Phải có `admin_password`.
- Mật khẩu nhập vào là mật khẩu quản trị, không phải mật khẩu user thường.
- IP phải hợp lệ nếu công ty bật bảo mật IP.
- Tạo phiên đăng nhập loại `ADMIN`.
- Trả token lưu ở frontend bằng key `token-admin`.

Rule phiên:

- Mỗi token có `sid`.
- Request có `sid` phải khớp bản ghi trong `login_sessions`.
- Phiên bị thu hồi, hết hạn hoặc không khớp user thì trả `401`.
- Mỗi request hợp lệ cập nhật `last_seen_at`.

## 5. Route guard frontend

Rule token:

- Route user dùng `meta.requiresUser`, cần `localStorage.token`.
- Route admin dùng `meta.requiresAdmin`, cần `localStorage.token-admin`.
- Route guest user nếu đã có token user thì chuyển về `/`.
- Route guest admin nếu đã có token admin hợp lệ thì chuyển về `/administrator`.

Rule vào khu admin:

- Frontend chỉ xem token admin hợp lệ khi:
  - `role = 0`, hoặc
  - `role = 1` và `rootCompanyAdmin = true`, hoặc
  - `role = 1` và `companyId = 1`.
- Admin công ty thường không được vào `/administrator`.

Rule menu header:

- Dropdown `Quản trị` chỉ hiện với Root hoặc admin công ty root.
- Công ty chờ kích hoạt thì ẩn phần lớn nút nhanh, chỉ giữ luồng cần thiết.

Rule màn Thành viên:

- Không dùng router `role<2` để chặn nhân viên.
- Nhân viên có quyền `setting-member-list` vẫn được vào và xem danh sách theo API.
- Quyền thao tác chi tiết do backend kiểm tra bằng `setting-member-save` và `setting-member-manage`.

## 6. Permission level

`permissions.level` đang dùng để phân cấp quyền:

| Level | Ý nghĩa hiện hành |
| --- | --- |
| `0` | Quyền user/khách hàng. Nhân viên có thể được cấp các quyền này. Admin công ty được cho qua mặc định. |
| `1` | Quyền admin/quản trị hệ thống hoặc quyền không dành cho nhân viên thường. |
| `2` | Quyền cao hơn, dùng cho nhóm quyền hệ thống nếu có cấu hình. |

Rule kiểm tra:

- Root (`role = 0`) qua tất cả.
- Admin công ty (`role = 1`) qua các quyền `level = 0`.
- Nhân viên (`role = 2`) phải có override `user_permissions.allowed = 1`.
- Nếu user có override `allowed = 0` cho một quyền thì quyền đó bị từ chối.
- Nếu key quyền không tồn tại trong bảng `permissions`, hệ thống từ chối và log cấu hình sai.
- `permission("key-a|key-b")` nghĩa là chỉ cần có một trong các quyền.
- Khi thiếu quyền, backend trả message rõ: `Thiếu quyền: <display_name>` hoặc `Thiếu một trong các quyền: ...`.

Rule gán quyền:

- Không được tự phân quyền cho tài khoản đang đăng nhập.
- Không gán role Root từ màn thành viên.
- Nhân viên chỉ được nhận quyền level 0.
- Admin công ty thường full quyền user level 0 nên không cần insert `user_permissions`.
- Admin công ty root khi phân quyền chỉ dùng nhóm quyền admin; quyền level 0 được bỏ qua khi lưu.
- Khi tạo quyền mới trong màn quản trị quyền, hệ thống tự gán quyền đó cho toàn bộ Root.

## 7. Danh sách permission key đang dùng

Danh sách dưới đây lấy theo các controller đang gọi `permission(...)`.

### Hóa đơn

| Key | Ý nghĩa |
| --- | --- |
| `invoice-list` | Xem danh sách/chi tiết liên quan hóa đơn. |
| `invoice-save` | Lập, lưu, cập nhật, ký, phát hành, gửi hóa đơn và import hóa đơn. |
| `invoice-delete` | Xóa hóa đơn. |

Rule riêng:

- Màn lập hóa đơn kiểm tra quyền chính bằng `/v1/invoices/prepare` với `invoice-save`.
- Import hóa đơn dùng `invoice-save`.
- Lịch sử import cho phép `invoice-list` hoặc `invoice-save`.
- API phụ để gợi ý khách hàng/sản phẩm không được chặn quyền lập hóa đơn nếu user đã có `invoice-save`.

### Tờ khai hóa đơn

| Key | Ý nghĩa |
| --- | --- |
| `register-invoice-list` | Xem danh sách/chi tiết tờ khai. |
| `register-invoice-save` | Thêm, sửa, sao chép, lưu tờ khai. |
| `register-invoice-send` | Gửi tờ khai đến cơ quan thuế. |

Rule riêng:

- Tờ khai chỉ được đọc/sửa trong công ty của user, trừ một số luồng Root/admin có xử lý riêng.
- Gửi tờ khai ghi nhận lịch sử tiếp nhận/chấp nhận/từ chối theo phản hồi mô phỏng hoặc tích hợp.

### Mẫu hóa đơn

| Key | Ý nghĩa |
| --- | --- |
| `form-invoice-list` | Xem danh sách, chi tiết, preview mẫu hóa đơn của công ty. |
| `form-invoice-save` | Tạo, cập nhật, đổi trạng thái mẫu hóa đơn phía khách hàng. |
| `form-invoice-manage` | Quản lý mẫu hóa đơn hệ thống ở khu admin. |

Rule riêng:

- API đọc chi tiết mẫu phía khách hàng cho phép `form-invoice-list` hoặc `form-invoice-save`.
- User có quyền thêm/sửa mẫu phải mở được form sửa mà không cần thêm quyền list riêng.
- Mẫu hóa đơn kích hoạt phải thuộc đúng công ty.
- Khi kích hoạt mẫu theo loại hóa đơn, hệ thống xử lý mẫu active/counter theo company.

### Danh mục khách hàng và hàng hóa

| Key | Ý nghĩa |
| --- | --- |
| `category-customer-list` | Xem danh mục khách hàng. |
| `category-customer-save` | Thêm/sửa/xóa khách hàng. |
| `category-product-list` | Xem danh mục hàng hóa. |
| `category-product-save` | Thêm/sửa/xóa hàng hóa. |
| `import-customer-list` | Xem lịch sử import khách hàng. |
| `import-customer-save` | Tải mẫu, upload và import lại khách hàng. |
| `import-product-list` | Xem lịch sử import sản phẩm. |
| `import-product-save` | Tải mẫu, upload và import lại sản phẩm. |

Rule riêng:

- Danh sách khách hàng/hàng hóa luôn lọc theo `currentUser.companyId`.
- Khi lưu, backend tự gán `companyId` theo user hiện tại; không tin companyId từ giao diện.
- Khi thêm mới hoặc đổi mã thủ công, mã khách hàng/mã sản phẩm không được trùng với bản ghi khác trong cùng công ty.
- API import danh mục cho phép quyền import riêng hoặc quyền danh mục tương ứng: customer dùng `category-customer-list|import-customer-list` và `category-customer-save|import-customer-save`; product dùng `category-product-list|import-product-list` và `category-product-save|import-product-save`.
- Import danh mục dùng mã để cập nhật bản ghi đã tồn tại trong công ty, nhưng không cho mã bị lặp trong cùng file import.

### Báo cáo

| Key | Ý nghĩa |
| --- | --- |
| `report-invoice` | Xem báo cáo hóa đơn. |
| `report-invoice-export` | Xuất báo cáo hóa đơn ra Excel. |

Rule riêng:

- Báo cáo phía khách hàng lọc theo công ty của user.
- File export cũng phải dùng cùng phạm vi lọc với danh sách.

### Cài đặt công ty và thành viên

| Key | Ý nghĩa |
| --- | --- |
| `setting-profile` | Cập nhật hồ sơ công ty, người đại diện, ngân hàng, cơ quan thuế. |
| `setting-member-list` | Xem danh sách thành viên. |
| `setting-member-save` | Thêm/sửa thành viên và mở catalog quyền để phân quyền. |
| `setting-member-manage` | Khóa/mở khóa, reset mật khẩu, gửi thông tin đăng nhập, xóa/rời công ty. |
| `setting-security-ip` | Cấu hình bảo mật IP công ty. |
| `setting-login-history` | Xem lịch sử đăng nhập. |

Rule riêng:

- Danh sách thành viên của user không phải Root luôn ép `companyId = actor.companyId`.
- Root có thể lọc công ty nếu API nhận companyId.
- Admin không được thao tác admin khác, trừ thao tác chính mình khi rule cho phép.
- Nhân viên chỉ được thao tác nhân viên cùng công ty khi API cho phép.
- Gửi thông tin đăng nhập thành viên dùng email của `user.email` và template `ACCOUNT_INFO_MAIL`.

### Email

| Key | Ý nghĩa |
| --- | --- |
| `mail-server-manage` | Quản lý máy chủ gửi mail của công ty. |
| `mail-job-list` | Xem lịch sử email phía công ty. |
| `mail-job-retry` | Gửi lại email phía công ty. |
| `admin-mail-job-list` | Xem lịch sử email toàn hệ thống ở admin. |
| `admin-mail-job-retry` | Gửi lại email toàn hệ thống ở admin. |

Rule riêng:

- Mail server phía khách hàng lấy theo công ty của user.
- Một số template hệ thống luôn dùng công ty root để lấy template gửi mail.

### Gói hóa đơn và hạn mức

| Key | Ý nghĩa |
| --- | --- |
| `invoice-package-purchase` | Xem gói hóa đơn, mua gói, xem lịch sử mua phía khách hàng. |
| `buy-invoice-list` | Xem gói/hạn mức/giao dịch mua ở khu admin. |
| `buy-invoice-save` | Thêm/sửa gói hoặc hạn mức mua hóa đơn ở admin. |
| `buy-invoice-delete` | Xóa/ẩn gói hoặc hạn mức mua hóa đơn ở admin. |

Rule riêng:

- Công ty chờ kích hoạt vẫn được gọi nhóm `/v1/invoice-packages` để mua gói.
- Sau thanh toán thành công, hệ thống tạo/cập nhật hạn mức hóa đơn và kích hoạt công ty nếu cần.
- Mỗi công ty chỉ nên có một hạn mức mua hóa đơn active.

### Quản trị công ty

| Key | Ý nghĩa |
| --- | --- |
| `company-list` | Xem danh sách công ty và hồ sơ đăng ký. |
| `company-save` | Thêm/sửa công ty. |
| `company-manage` | Duyệt/từ chối đăng ký, đổi trạng thái công ty, gửi thông tin admin công ty. |

Rule riêng:

- Duyệt đăng ký tạo công ty và tài khoản admin công ty.
- Gửi thông tin đăng nhập lúc tạo/duyệt công ty dùng template đăng nhập công ty, khác với gửi tài khoản thành viên.

### Danh mục nền admin

| Key | Ý nghĩa |
| --- | --- |
| `bank-list` | Xem danh sách ngân hàng. |
| `bank-save` | Thêm/sửa ngân hàng. |
| `tax-authority-list` | Xem danh sách cơ quan thuế. |
| `tax-authority-save` | Thêm/sửa cơ quan thuế. |
| `tax-authority-delete` | Xóa/ẩn cơ quan thuế. |
| `vat-rate-list` | Xem danh sách thuế suất. |
| `vat-rate-save` | Thêm/sửa, đổi trạng thái, sắp xếp thuế suất. |
| `vat-rate-delete` | Xóa/ẩn thuế suất. |

### Mail template, Telegram, phân quyền

| Key | Ý nghĩa |
| --- | --- |
| `mail-template-list` | Xem danh sách mail template. |
| `mail-template-save` | Thêm/sửa mail template. |
| `mail-template-delete` | Xóa/ẩn mail template. |
| `telegram-config-manage` | Cấu hình Telegram và gửi báo cáo thử. |
| `permission-manage` | Quản lý danh sách quyền. |
| `permission-category-manage` | Quản lý nhóm quyền và thứ tự hiển thị. |

## 8. Rule dữ liệu theo công ty

Rule chung:

- Không truyền `companyId` từ giao diện cho các màn khách hàng nếu backend có thể suy ra từ JWT.
- Backend phải lấy `currentUser().getCompanyId()` làm nguồn tin cậy.
- User không phải Root không được xem hoặc sửa dữ liệu công ty khác.
- Khi API nhận `companyId` để lọc, nếu actor không phải Root thì phải ghi đè về `actor.companyId`.
- Nếu bản ghi có `companyId` khác user hiện tại thì trả `403` hoặc lỗi nghiệp vụ phù hợp.

Các dữ liệu bắt buộc lọc theo công ty:

- Hóa đơn.
- Chi tiết hóa đơn và lịch sử hóa đơn.
- Tờ khai đăng ký hóa đơn.
- Mẫu hóa đơn phía khách hàng.
- Khách hàng.
- Hàng hóa.
- Thành viên.
- Hồ sơ công ty.
- Mail server.
- Lịch sử gửi mail phía khách hàng.
- Lịch sử import hóa đơn.
- Lịch sử import khách hàng/sản phẩm.
- Lịch sử mua gói phía khách hàng.
- Thông báo/lịch sử thao tác theo công ty.

Rule file:

- File public như hóa đơn view/download có endpoint public riêng.
- File upload theo công ty lưu dưới `uploads/<companyId>/...`.
- Khi đọc file nghiệp vụ không public, phải kiểm tra bản ghi thuộc đúng công ty.

## 9. Rule trạng thái công ty

Trạng thái công ty:

| Giá trị | Ý nghĩa | Rule |
| --- | --- | --- |
| `0` | Tạm ngưng/ngừng hoạt động | User có thể bị hạn chế theo API hoặc trạng thái tài khoản. |
| `1` | Đang hoạt động | Được dùng chức năng theo quyền. |
| `2` | Chờ kích hoạt | Chỉ cho phép nhóm mua gói hóa đơn. |

Rule công ty chờ kích hoạt:

- JWT filter chặn hầu hết API `/v1/**` khi công ty `status = 2`.
- Ngoại lệ: `/v1/invoice-packages/**` để người dùng mua gói kích hoạt.
- Frontend sidebar chỉ hiện menu gói hóa đơn khi công ty chờ kích hoạt.
- Header ẩn các nút thao tác không cần thiết.
- Dashboard hiển thị màn chờ kích hoạt và nút mua gói.

## 10. Rule thành viên

Rule danh sách:

- `setting-member-list` là quyền xem danh sách thành viên.
- Không phải Root thì danh sách luôn theo công ty đang đăng nhập.
- Công ty thường không được thấy Root hoặc admin công ty root.

Rule thêm/sửa:

- `setting-member-save` là quyền thêm/sửa thành viên.
- User không phải Root không được tự chọn công ty khác; backend ghi đè `companyId` theo actor.
- Khi tạo mới nếu không truyền username, hệ thống tạo username dạng `cp-<userId>`.
- Không cho gán `role = 0`.
- Nếu role không phải admin/root thì xóa `admin_scope` và `admin_password`.

Rule quản lý:

- `setting-member-manage` là quyền khóa/mở khóa, reset mật khẩu, gửi thông tin đăng nhập, xóa/rời công ty.
- Không được thao tác tài khoản Root.
- Admin không được thao tác admin khác.
- Admin chỉ reset mật khẩu admin cho chính mình; với user khác chỉ reset nhân viên.
- Gửi thông tin đăng nhập thành viên luôn reset mật khẩu mới và gửi đến `user.email`.

Rule phân quyền:

- `setting-member-save` cần để mở danh sách quyền và lưu phân quyền.
- Không được tự phân quyền cho chính mình.
- Nhân viên chỉ nhận quyền level 0.
- Admin công ty thường không cần phân quyền level 0.
- Admin công ty root chỉ hiển thị/nhận nhóm quyền admin.

## 11. Rule khu Quản trị

Khu Quản trị gồm các route `/administrator/**` và API `/v1/administrator/**`.

Rule truy cập:

- Chỉ Root hoặc admin công ty root có token admin hợp lệ được vào.
- Admin công ty thường không được vào.
- Nhân viên không được vào.

Các nhóm chức năng admin:

- Thống kê mua gói hóa đơn.
- Duyệt đăng ký công ty.
- Quản lý công ty.
- Quản lý gói/hạn mức hóa đơn.
- Quản lý mẫu hóa đơn hệ thống.
- Quản lý ngân hàng.
- Quản lý cơ quan thuế.
- Quản lý thuế suất.
- Quản lý mail template.
- Theo dõi mail job toàn hệ thống.
- Cấu hình Telegram.
- Quản lý quyền và nhóm quyền.

Rule màn phân quyền admin:

- Màn quyền có lọc theo từ khóa, nhóm, level, trạng thái.
- Màn nhóm quyền có kéo thả, nút lên/xuống, nhập STT nhanh và đổi trạng thái.
- Thứ tự nhóm quyền dùng `orderIndex`.

## 12. Rule nghiệp vụ hóa đơn

Rule lập hóa đơn:

- Cần quyền `invoice-save`.
- Cần công ty đang hoạt động.
- Cần tờ khai hợp lệ/được chấp nhận theo rule prepare.
- Cần mẫu hóa đơn GTGT active thuộc công ty.
- Khi lưu hóa đơn, backend gán company theo user.
- Không được sửa/xem hóa đơn của công ty khác.

Rule phát hành/gửi cơ quan thuế:

- Cần quyền `invoice-save`.
- Hóa đơn phải thuộc công ty của user.
- Khi gửi mô phỏng, hệ thống có thể ghi nhận các trạng thái phản hồi như tiếp nhận, chấp nhận hoặc từ chối.
- Lịch sử hóa đơn ghi theo company để chỉ công ty đó xem được.

Rule xóa:

- Cần quyền `invoice-delete`.
- Chỉ xóa hóa đơn thuộc công ty của user.

Rule public:

- Các endpoint view/download hóa đơn public được SecurityConfig cho phép theo URL.
- Tra cứu public vẫn phải kiểm tra mã tra cứu/MST theo nghiệp vụ, không dựa vào user đăng nhập.

## 13. Rule tờ khai hóa đơn

Rule danh sách/chi tiết:

- Cần `register-invoice-list`.
- Dữ liệu lọc theo company user.

Rule tạo/sửa:

- Cần `register-invoice-save`.
- Backend tự set `companyId` theo user khi tạo.
- Khi sửa, bản ghi phải thuộc đúng công ty.

Rule gửi:

- Cần `register-invoice-send`.
- Bản ghi phải thuộc đúng công ty.
- Sau khi gửi phải ghi lịch sử xử lý.

## 14. Rule mẫu hóa đơn

Rule phía khách hàng:

- Danh sách cần `form-invoice-list`.
- Chi tiết/sao chép để sửa cho phép `form-invoice-list` hoặc `form-invoice-save`.
- Tạo/sửa/xóa cần `form-invoice-save`.
- Mẫu active phải thuộc công ty hiện tại.
- Không được dùng mẫu công ty khác để lập hóa đơn.

Rule phía admin:

- Quản lý mẫu hệ thống cần `form-invoice-manage`.
- File XSLT/ảnh mẫu lưu theo company phát sinh.

## 15. Rule gói hóa đơn và thanh toán

Rule phía khách hàng:

- Cần `invoice-package-purchase`.
- Được xem gói active, tạo giao dịch mua gói, xem lịch sử mua, thanh toán lại giao dịch chưa hoàn tất.
- Công ty đang chờ kích hoạt vẫn được phép mua gói.

Rule sau thanh toán:

- Nếu thanh toán thành công, cập nhật trạng thái giao dịch.
- Tạo hoặc tăng hạn mức hóa đơn cho công ty.
- Có thể kích hoạt công ty từ `status = 2` sang `status = 1`.
- Gửi email thông báo mua gói bằng template hệ thống phù hợp.

Rule admin:

- Quản lý gói/hạn mức dùng nhóm quyền `buy-invoice-*`.
- Khi tạo hạn mức active, phải tránh nhiều hạn mức active trùng một công ty.

## 16. Rule email và template

Rule template hệ thống:

- Các template sau dùng công ty root (`company_id = 1`) để lấy cấu hình/template:
  - `ACCOUNT_INFO_MAIL`
  - `LOGIN_INFO_MAIL`
  - `RESET_PASSWORD_MAIL`
  - `BUY_INVOICE_MAIL`
- Template khác dùng company fallback theo nghiệp vụ.

Rule gửi thông tin đăng nhập:

- Tạo/duyệt công ty dùng luồng gửi thông tin admin công ty.
- Gửi thông tin đăng nhập thành viên tại `/setting/member/list` dùng template `ACCOUNT_INFO_MAIL` và gửi đến `user.email`.

Rule mail job:

- Mail phía công ty lọc theo company.
- Mail admin toàn hệ thống cần quyền admin mail job.
- Gửi lại mail phải giữ đúng company/template ban đầu.

## 17. Rule thông báo lỗi quyền

Rule backend:

- Controller dùng `permission("key")` hoặc `permission("key-a|key-b")`.
- Khi thiếu quyền, message trả về phải rõ tên quyền thiếu.
- Tên quyền lấy từ `permissions.display_name`; nếu không có thì dùng permission key.

Rule frontend:

- Chỉ dùng Toastr cho thông báo nổi.
- Không dùng `$bvToast` cho code mới.
- Nếu component tự hiện lỗi hoặc tự xử lý fallback, request phải dùng `meta: { suppressGlobalErrorToast: true }`.
- Không hiện double toast cho cùng một lỗi.
- Không che mất message thiếu quyền chi tiết của backend bằng câu chung chung.

## 18. Rule validate form liên quan quyền

Rule chung:

- Form tự validate phải dùng `novalidate`.
- Lỗi field hiển thị bằng BootstrapVue state/feedback.
- Nội dung lỗi tiếng Việt.
- Backend vẫn validate lại trước khi lưu.

Rule đặc biệt:

- Mật khẩu user tối thiểu 8 ký tự ở các form đổi/reset/tạo thành viên.
- Mật khẩu quản trị chỉ hiện và validate cho Root hoặc admin công ty root theo rule.
- Email, số điện thoại, MST, website nên dùng helper trong `src/utils/validators.js`.

## 19. Checklist khi thêm API mới

1. API có cần đăng nhập không? Nếu không, phải nằm trong nhóm public rõ ràng.
2. API thuộc công ty nào? Nếu là nghiệp vụ khách hàng, lấy `companyId` từ `currentUser()`.
3. API cần permission key nào? Tạo key trong `permissions` và gán `display_name` tiếng Việt.
4. Permission key thuộc level nào? Quyền user thường là level 0.
5. Admin công ty có được mặc định qua quyền này không?
6. Nhân viên có thể được cấp quyền này không?
7. API có dùng dữ liệu phụ không? Nếu là dữ liệu phụ, frontend cần xử lý thiếu quyền nhẹ nhàng.
8. Lỗi thiếu quyền có trả message rõ chưa?
9. Có nguy cơ công ty này thấy dữ liệu công ty khác không?
10. Có cần ghi lịch sử thao tác hoặc lịch sử gửi mail không?

## 20. Checklist khi thêm màn hình mới

1. Route có `requiresUser` hoặc `requiresAdmin` đúng chưa?
2. Menu có hiển thị đúng với công ty chờ kích hoạt không?
3. Màn hình có gọi API chính kiểm tra quyền rõ ràng không?
4. API phụ có `suppressGlobalErrorToast` nếu lỗi phụ không nên đá user khỏi màn hình không?
5. Form có `novalidate` và feedback BootstrapVue không?
6. Toast có dùng Toastr không?
7. Với màn công ty, có truyền `companyId` từ UI không? Nếu có, backend có ghi đè/kiểm tra actor không?
8. Nhân viên có quyền thì vào được, thiếu quyền thì nhận thông báo rõ và quay về trang chủ theo interceptor.

## 21. Nguồn code cần đối chiếu

Các file chính:

- `src/main/java/vn/hoadon/entity/UserEntity.java`
- `src/main/java/vn/hoadon/security/JwtUtil.java`
- `src/main/java/vn/hoadon/security/JwtAuthenticationFilter.java`
- `src/main/java/vn/hoadon/controllers/base/BaseController.java`
- `src/main/java/vn/hoadon/services/impl/PermissionServiceImpl.java`
- `src/main/java/vn/hoadon/services/impl/MemberServiceImpl.java`
- `src/main/java/vn/hoadon/controllers/setting/MemberController.java`
- `src/router/index.js`
- `src/router/modules/administrator.js`
- `src/router/modules/setting.js`
- `src/views/components/header.vue`
- `src/plugins/axios.js`
- `src/utils/toast.js`
