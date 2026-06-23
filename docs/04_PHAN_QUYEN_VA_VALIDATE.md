# Phân Quyền Và Validate Form

Tài liệu gom quy tắc vai trò, phân quyền, phạm vi dữ liệu và chuẩn validate form.

## Nguồn đã hợp nhất

- `QUY_TAC_VAI_TRO_CHUONG_TRINH.md`
- `FORM_VALIDATION_SUMMARY.md`

## Nội dung

### Từ `QUY_TAC_VAI_TRO_CHUONG_TRINH.md`

Cập nhật: 19/06/2026

Tài liệu này mô tả các vai trò, phạm vi quyền, rule phân quyền và rule nghiệp vụ quan trọng của hệ thống hóa đơn điện tử. Khi sửa quyền, thêm màn hình mới hoặc thêm API mới, cần đối chiếu file này với code backend để tránh mở nhầm dữ liệu công ty khác hoặc chặn sai người dùng hợp lệ.

### 1. Bảng và trường liên quan

Các bảng chính:

| Bảng | Ý nghĩa |
| --- | --- |
| `users` | Tài khoản đăng nhập. Chứa `company_id`, `role`, `admin_password`, `status`. |
| `companies` | Công ty/đơn vị sử dụng hệ thống. Chứa trạng thái kích hoạt công ty. |
| `permissions` | Danh sách quyền chi tiết. `name` là key kiểm tra trong code, `display_name` là tên hiển thị khi báo thiếu quyền. |
| `permission_categories` | Nhóm quyền dùng để gom quyền trên màn hình phân quyền. |
| `user_permissions` | Quyền riêng được bật/tắt cho từng user. `allowed = 1` là cho phép, `allowed = 0` là từ chối. |
| `login_sessions` | Phiên đăng nhập hiện tại, dùng để thu hồi hoặc kiểm tra phiên. |
| `login_histories` | Lịch sử đăng nhập. |

Các trường quan trọng trong `users`:

| Trường | Rule |
| --- | --- |
| `company_id` | Xác định công ty sở hữu user. Hầu hết dữ liệu nghiệp vụ phải lọc theo company này. |
| `role` | Vai trò cấp hệ thống: `0 = Quản trị viên toàn quyền`, `1 = Quản trị viên hệ thống`, `2 = Quản lý doanh nghiệp`, `3 = Nhân viên doanh nghiệp`. |
| `admin_password` | Mật khẩu riêng cho khu Quản trị. Chỉ Quản trị viên toàn quyền hoặc Quản trị viên hệ thống dùng được. |
| `status` | `1 = hoạt động`, `0 = khóa/ngừng hoạt động`. User không hoạt động không được đăng nhập. |
| `deleted_at` | Nếu có giá trị thì user bị xóa mềm, không nên hiện trong danh sách thành viên. |

### 2. Vai trò hệ thống

#### 2.1 Quản trị viên toàn quyền

Giá trị:

- `users.role = 0`

Rule:

- Có toàn quyền trong hệ thống.
- `PermissionServiceImpl.hasPermission()` luôn cho phép Quản trị viên toàn quyền.
- Được vào khu Quản trị bằng `/auth/login-admin` nếu có `admin_password`.
- Được thao tác dữ liệu nhiều công ty khi API hỗ trợ lọc công ty.
- Không được tạo thêm user role Quản trị viên toàn quyền từ màn hình thành viên. Service chặn payload có `role = 0`.
- Tài khoản Quản trị viên toàn quyền không được thao tác/xóa/khóa bởi Quản lý doanh nghiệp.

#### 2.2 Quản trị viên hệ thống

Giá trị:

- `users.role = 1`
- `users.company_id = 1`

Rule:

- Là quản trị thuộc công ty root.
- Được hiện dropdown `Quản trị` ở header.
- Được đăng nhập khu Quản trị bằng `admin_password`.
- Dùng các permission admin được gán trong `user_permissions`.
- Khi phân quyền thành viên cho tài khoản này, chỉ hiển thị nhóm quyền admin; quyền user level 0 không áp dụng cho role này.
- Không mặc định được cho qua quyền user level 0 như Quản lý doanh nghiệp.

#### 2.3 Quản lý doanh nghiệp

Giá trị:

- `users.role = 2`
- `users.company_id` là công ty doanh nghiệp đang quản lý.

Rule:

- Là quản trị của một công ty khách hàng.
- Không hiện dropdown `Quản trị` ở header.
- Không đăng nhập khu Quản trị bằng `/auth/login-admin`.
- Không có `admin_password`; nếu chuyển khỏi role quản trị hệ thống thì `admin_password` phải bị xóa.
- Được cho qua các quyền user level 0.
- Không cần insert `user_permissions` cho quyền level 0.
- Được quản lý thành viên trong công ty mình theo các quyền `setting-member-*`.
- Không được thao tác Quản trị viên toàn quyền hoặc Quản trị viên hệ thống.

#### 2.4 Nhân viên doanh nghiệp

Giá trị:

- `users.role = 3`
- `users.admin_password = null`

Rule:

- Chỉ có quyền khi được gán trong `user_permissions`.
- Chỉ được dùng các quyền level 0.
- Không được vào khu Quản trị.
- Có thể vào menu Thành viên nếu được cấp quyền `setting-member-list`.
- Nếu thiếu quyền khi vào API, backend trả `403` kèm tên quyền thiếu.
- Không được thao tác tài khoản quản lý hoặc quản trị.
- Không được tự phân quyền cho chính mình.

#### 2.5 Người truy cập công khai

Rule:

- Chỉ dùng được API public hoặc auth.
- Có thể đăng ký công ty, đăng nhập, quên mật khẩu, đặt lại mật khẩu.
- Có thể tra cứu hóa đơn public theo rule của API public.
- Không có quyền truy cập API nghiệp vụ dưới `/v1/**` nếu không có JWT.

### 3. Bảng role chuẩn

Hệ thống chỉ dùng `users.role` để phân biệt vai trò chính.

| Role | Tên đọc | Rule chính |
| --- | --- | --- |
| `0` | Quản trị viên toàn quyền | Toàn quyền hệ thống, được vào khu Quản trị bằng `admin_password`. |
| `1` | Quản trị viên hệ thống | Thuộc công ty root, được vào khu Quản trị bằng `admin_password`, dùng permission admin được gán. |
| `2` | Quản lý doanh nghiệp | Quản lý trong phạm vi công ty, mặc định qua quyền user level 0, không có `admin_password`. |
| `3` | Nhân viên doanh nghiệp | Dùng quyền chi tiết được gán trong `user_permissions`, không có `admin_password`. |

Rule chuẩn hóa:

- Không cho tạo thêm `role = 0` từ màn hình thành viên.
- `role = 1` phải thuộc công ty root (`company_id = 1`).
- `role = 2` và `role = 3` luôn xóa `admin_password`.
- Quản trị viên hệ thống chỉ cập nhật `admin_password` khi có payload gửi lên.

### 4. JWT và đăng nhập

JWT có thời hạn theo cấu hình `jwt.expiration_ms`. Cấu hình hiện tại là `86400000` ms, tương đương 24 giờ.

Các claim quan trọng:

| Claim | Ý nghĩa |
| --- | --- |
| `userId` | ID user. |
| `role` | Role số của user. |
| `companyId` | Công ty của user. |
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
- User phải `canAccessAdminArea() = true`, tức Quản trị viên toàn quyền hoặc Quản trị viên hệ thống.
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

### 5. Route guard frontend

Rule token:

- Route user dùng `meta.requiresUser`, cần `localStorage.token`.
- Route admin dùng `meta.requiresAdmin`, cần `localStorage.token-admin`.
- Route guest user nếu đã có token user thì chuyển về `/`.
- Route guest admin nếu đã có token admin hợp lệ thì chuyển về `/administrator`.

Rule vào khu admin:

- Frontend chỉ xem token admin hợp lệ khi `adminAccess = true` hoặc `role = 0` hoặc `role = 1`.
- Quản lý doanh nghiệp không được vào `/administrator`.

Rule menu header:

- Dropdown `Quản trị` chỉ hiện với Quản trị viên toàn quyền hoặc Quản trị viên hệ thống.
- Công ty chờ kích hoạt thì ẩn phần lớn nút nhanh, chỉ giữ luồng cần thiết.

Rule màn Thành viên:

- Không dùng router `role<2` để chặn Nhân viên doanh nghiệp.
- Nhân viên doanh nghiệp có quyền `setting-member-list` vẫn được vào và xem danh sách theo API.
- Quyền thao tác chi tiết do backend kiểm tra bằng `setting-member-save` và `setting-member-manage`.

### 6. Permission level

`permissions.level` đang dùng để phân cấp quyền:

| Level | Ý nghĩa hiện hành |
| --- | --- |
| `0` | Quyền user/doanh nghiệp. Nhân viên doanh nghiệp có thể được cấp các quyền này. Quản lý doanh nghiệp được cho qua mặc định. |
| `1` | Quyền admin/quản trị hệ thống hoặc quyền không dành cho nhân viên thường. |
| `2` | Quyền cao hơn, dùng cho nhóm quyền hệ thống nếu có cấu hình. |

Rule kiểm tra:

- Quản trị viên toàn quyền (`role = 0`) qua tất cả.
- Quản trị viên hệ thống (`role = 1`) dùng permission admin được gán.
- Quản lý doanh nghiệp (`role = 2`) qua các quyền `level = 0`.
- Nhân viên doanh nghiệp (`role = 3`) phải có override `user_permissions.allowed = 1`.
- Nếu user có override `allowed = 0` cho một quyền thì quyền đó bị từ chối.
- Nếu key quyền không tồn tại trong bảng `permissions`, hệ thống từ chối và log cấu hình sai.
- `permission("key-a|key-b")` nghĩa là chỉ cần có một trong các quyền.
- Khi thiếu quyền, backend trả message rõ: `Thiếu quyền: <display_name>` hoặc `Thiếu một trong các quyền: ...`.

Rule gán quyền:

- Không được tự phân quyền cho tài khoản đang đăng nhập.
- Không gán role Quản trị viên toàn quyền từ màn thành viên.
- Nhân viên doanh nghiệp chỉ được nhận quyền level 0.
- Quản lý doanh nghiệp full quyền user level 0 nên không cần insert `user_permissions`.
- Quản trị viên hệ thống khi phân quyền chỉ dùng nhóm quyền admin; quyền level 0 được bỏ qua khi lưu.
- Khi tạo quyền mới trong màn quản trị quyền, hệ thống tự gán quyền đó cho toàn bộ Quản trị viên toàn quyền.

### 7. Danh sách permission key đang dùng

Danh sách dưới đây lấy theo các controller đang gọi `permission(...)`.

#### Hóa đơn

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

#### Tờ khai hóa đơn

| Key | Ý nghĩa |
| --- | --- |
| `register-invoice-list` | Xem danh sách/chi tiết tờ khai. |
| `register-invoice-save` | Thêm, sửa, sao chép, lưu tờ khai. |
| `register-invoice-send` | Gửi tờ khai đến cơ quan thuế. |

Rule riêng:

- Tờ khai chỉ được đọc/sửa trong công ty của user, trừ một số luồng Quản trị viên toàn quyền hoặc Quản trị viên hệ thống có xử lý riêng.
- Gửi tờ khai ghi nhận lịch sử tiếp nhận/chấp nhận/từ chối theo phản hồi mô phỏng hoặc tích hợp.

#### Mẫu hóa đơn

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

#### Danh mục khách hàng và hàng hóa

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

#### Báo cáo

| Key | Ý nghĩa |
| --- | --- |
| `report-invoice` | Xem báo cáo hóa đơn. |
| `report-invoice-export` | Xuất báo cáo hóa đơn ra Excel. |

Rule riêng:

- Báo cáo phía khách hàng lọc theo công ty của user.
- File export cũng phải dùng cùng phạm vi lọc với danh sách.

#### Cài đặt công ty và thành viên

| Key | Ý nghĩa |
| --- | --- |
| `setting-profile` | Cập nhật hồ sơ công ty, người đại diện, ngân hàng, cơ quan thuế. |
| `setting-member-list` | Xem danh sách thành viên. |
| `setting-member-save` | Thêm/sửa thành viên và mở catalog quyền để phân quyền. |
| `setting-member-manage` | Khóa/mở khóa, reset mật khẩu, gửi thông tin đăng nhập, xóa/rời công ty. |
| `setting-security-ip` | Cấu hình bảo mật IP công ty. |
| `setting-login-history` | Xem lịch sử đăng nhập. |

Rule riêng:

- Danh sách thành viên của user không phải Quản trị viên toàn quyền luôn ép `companyId = actor.companyId`.
- Quản trị viên toàn quyền có thể lọc công ty nếu API nhận companyId.
- Quản trị viên hệ thống không được thao tác Quản trị viên hệ thống khác, trừ thao tác chính mình khi rule cho phép.
- Nhân viên doanh nghiệp chỉ được thao tác Nhân viên doanh nghiệp cùng công ty khi API cho phép.
- Gửi thông tin đăng nhập thành viên dùng email của `user.email` và template `ACCOUNT_INFO_MAIL`.

#### Email

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

#### Gói hóa đơn và hạn mức

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

#### Quản trị doanh nghiệp và hệ thống

| Key | Ý nghĩa |
| --- | --- |
| `company-list` | Xem danh sách công ty và hồ sơ đăng ký. |
| `company-save` | Thêm/sửa công ty. |
| `company-manage` | Duyệt/từ chối đăng ký, đổi trạng thái công ty, gửi thông tin Quản lý doanh nghiệp. |

Rule riêng:

- Duyệt đăng ký tạo công ty và tài khoản Quản lý doanh nghiệp.
- Gửi thông tin đăng nhập lúc tạo/duyệt công ty dùng template đăng nhập công ty, khác với gửi tài khoản thành viên.

#### Danh mục nền admin

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

#### Mail template, Telegram, phân quyền

| Key | Ý nghĩa |
| --- | --- |
| `mail-template-list` | Xem danh sách mail template. |
| `mail-template-save` | Thêm/sửa mail template. |
| `mail-template-delete` | Xóa/ẩn mail template. |
| `telegram-config-manage` | Cấu hình Telegram và gửi báo cáo thử. |
| `permission-manage` | Quản lý danh sách quyền. |
| `permission-category-manage` | Quản lý nhóm quyền và thứ tự hiển thị. |

### 8. Rule dữ liệu theo công ty

Rule chung:

- Không truyền `companyId` từ giao diện cho các màn khách hàng nếu backend có thể suy ra từ JWT.
- Backend phải lấy `currentUser().getCompanyId()` làm nguồn tin cậy.
- User không phải Quản trị viên toàn quyền không được xem hoặc sửa dữ liệu công ty khác.
- Khi API nhận `companyId` để lọc, nếu actor không phải Quản trị viên toàn quyền thì phải ghi đè về `actor.companyId`.
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

### 9. Rule trạng thái công ty

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

### 10. Rule thành viên

Rule danh sách:

- `setting-member-list` là quyền xem danh sách thành viên.
- Không phải Quản trị viên toàn quyền thì danh sách luôn theo công ty đang đăng nhập.
- Công ty thường không được thấy Quản trị viên toàn quyền hoặc Quản trị viên hệ thống.

Rule thêm/sửa:

- `setting-member-save` là quyền thêm/sửa thành viên.
- User không phải Quản trị viên toàn quyền không được tự chọn công ty khác; backend ghi đè `companyId` theo actor.
- Khi tạo mới nếu không truyền username, hệ thống tạo username dạng `cp-<userId>`.
- Không cho gán `role = 0`.
- Nếu role không phải Quản trị viên toàn quyền hoặc Quản trị viên hệ thống thì xóa `admin_password`.

Rule quản lý:

- `setting-member-manage` là quyền khóa/mở khóa, reset mật khẩu, gửi thông tin đăng nhập, xóa/rời công ty.
- Không được thao tác tài khoản Quản trị viên toàn quyền.
- Quản trị viên hệ thống không được thao tác Quản trị viên toàn quyền hoặc Quản trị viên hệ thống khác.
- Quản lý doanh nghiệp chỉ reset mật khẩu chính mình hoặc Nhân viên doanh nghiệp trong công ty.
- Gửi thông tin đăng nhập thành viên luôn reset mật khẩu mới và gửi đến `user.email`.

Rule phân quyền:

- `setting-member-save` cần để mở danh sách quyền và lưu phân quyền.
- Không được tự phân quyền cho chính mình.
- Nhân viên doanh nghiệp chỉ nhận quyền level 0.
- Quản lý doanh nghiệp không cần phân quyền level 0.
- Quản trị viên hệ thống chỉ hiển thị/nhận nhóm quyền admin.

### 11. Rule khu Quản trị

Khu Quản trị gồm các route `/administrator/**` và API `/v1/administrator/**`.

Rule truy cập:

- Chỉ Quản trị viên toàn quyền hoặc Quản trị viên hệ thống có token admin hợp lệ được vào.
- Quản lý doanh nghiệp không được vào.
- Nhân viên doanh nghiệp không được vào.

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

### 12. Rule nghiệp vụ hóa đơn

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

### 13. Rule tờ khai hóa đơn

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

### 14. Rule mẫu hóa đơn

Rule phía khách hàng:

- Danh sách cần `form-invoice-list`.
- Chi tiết/sao chép để sửa cho phép `form-invoice-list` hoặc `form-invoice-save`.
- Tạo/sửa/xóa cần `form-invoice-save`.
- Mẫu active phải thuộc công ty hiện tại.
- Không được dùng mẫu công ty khác để lập hóa đơn.

Rule phía admin:

- Quản lý mẫu hệ thống cần `form-invoice-manage`.
- File XSLT/ảnh mẫu lưu theo company phát sinh.

### 15. Rule gói hóa đơn và thanh toán

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

### 16. Rule email và template

Rule template hệ thống:

- Các template sau dùng công ty root (`company_id = 1`) để lấy cấu hình/template:
  - `ACCOUNT_INFO_MAIL`
  - `LOGIN_INFO_MAIL`
  - `RESET_PASSWORD_MAIL`
  - `BUY_INVOICE_MAIL`
- Template khác dùng company fallback theo nghiệp vụ.

Rule gửi thông tin đăng nhập:

- Tạo/duyệt công ty dùng luồng gửi thông tin Quản lý doanh nghiệp.
- Gửi thông tin đăng nhập thành viên tại `/setting/member/list` dùng template `ACCOUNT_INFO_MAIL` và gửi đến `user.email`.

Rule mail job:

- Mail phía công ty lọc theo company.
- Mail admin toàn hệ thống cần quyền admin mail job.
- Gửi lại mail phải giữ đúng company/template ban đầu.

### 17. Rule thông báo lỗi quyền

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

### 18. Rule validate form liên quan quyền

Rule chung:

- Form tự validate phải dùng `novalidate`.
- Lỗi field hiển thị bằng BootstrapVue state/feedback.
- Nội dung lỗi tiếng Việt.
- Backend vẫn validate lại trước khi lưu.

Rule đặc biệt:

- Mật khẩu user tối thiểu 8 ký tự ở các form đổi/reset/tạo thành viên.
- Mật khẩu quản trị chỉ hiện và validate cho Quản trị viên toàn quyền hoặc Quản trị viên hệ thống theo rule.
- Email, số điện thoại, MST, website nên dùng helper trong `src/utils/validators.js`.

### 19. Checklist khi thêm API mới

1. API có cần đăng nhập không? Nếu không, phải nằm trong nhóm public rõ ràng.
2. API thuộc công ty nào? Nếu là nghiệp vụ khách hàng, lấy `companyId` từ `currentUser()`.
3. API cần permission key nào? Tạo key trong `permissions` và gán `display_name` tiếng Việt.
4. Permission key thuộc level nào? Quyền user thường là level 0.
5. Quản lý doanh nghiệp có được mặc định qua quyền này không?
6. Nhân viên doanh nghiệp có thể được cấp quyền này không?
7. API có dùng dữ liệu phụ không? Nếu là dữ liệu phụ, frontend cần xử lý thiếu quyền nhẹ nhàng.
8. Lỗi thiếu quyền có trả message rõ chưa?
9. Có nguy cơ công ty này thấy dữ liệu công ty khác không?
10. Có cần ghi lịch sử thao tác hoặc lịch sử gửi mail không?

### 20. Checklist khi thêm màn hình mới

1. Route có `requiresUser` hoặc `requiresAdmin` đúng chưa?
2. Menu có hiển thị đúng với công ty chờ kích hoạt không?
3. Màn hình có gọi API chính kiểm tra quyền rõ ràng không?
4. API phụ có `suppressGlobalErrorToast` nếu lỗi phụ không nên đá user khỏi màn hình không?
5. Form có `novalidate` và feedback BootstrapVue không?
6. Toast có dùng Toastr không?
7. Với màn công ty, có truyền `companyId` từ UI không? Nếu có, backend có ghi đè/kiểm tra actor không?
8. Nhân viên doanh nghiệp có quyền thì vào được, thiếu quyền thì nhận thông báo rõ và quay về trang chủ theo interceptor.

### 21. Nguồn code cần đối chiếu

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

### Từ `FORM_VALIDATION_SUMMARY.md`

Cập nhật: 19/06/2026

Tài liệu này ghi lại quy ước validate form đang áp dụng trong hệ thống. Mục tiêu là khi sửa hoặc thêm màn hình mới, giao diện không còn hiện tooltip validate mặc định của trình duyệt, thông báo lỗi đồng nhất bằng BootstrapVue trên form và Toastr ở góc màn hình.

Validate phía giao diện chỉ dùng để chặn lỗi sớm. Backend vẫn phải kiểm tra lại dữ liệu quan trọng trước khi lưu.

### Quy ước hiện hành

- Form có validate riêng phải dùng `novalidate` và bắt submit bằng `@submit.prevent`.
- Không dựa vào tooltip mặc định của trình duyệt cho `required`, `type=email`, `min`, `max`.
- Lỗi dưới input dùng `:state`, `b-form-invalid-feedback` hoặc vùng lỗi cùng phong cách BootstrapVue.
- Nội dung lỗi hiển thị cho người dùng phải viết bằng tiếng Việt.
- Khi component tự xử lý lỗi API, request phải truyền `meta: { suppressGlobalErrorToast: true }` để tránh hiện 2 toast.
- Lỗi quyền `403` không được thay bằng câu chung chung nếu backend đã trả thông báo chi tiết. Thông báo cần giữ dạng rõ quyền thiếu, ví dụ `Thiếu quyền: Xem danh mục sản phẩm`.
- Thông báo toàn hệ thống chỉ dùng Toastr. Không thêm mới `$bvToast`; nếu gặp code cũ thì chuyển sang `this.$toastr` hoặc helper trong `src/utils/toast.js`.
- Những API phụ như dropdown, autocomplete, danh sách gợi ý không được làm hỏng form chính nếu user thiếu quyền phụ. Các request này nên dùng `suppressGlobalErrorToast` và xử lý fallback tại component.

### Helper validate dùng chung

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

### Thông báo lỗi và toast

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

### Các nhóm form đã chuẩn hóa chính

#### Đăng ký, đăng nhập, quên mật khẩu

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

#### Tài khoản cá nhân

File: `src/views/settings/account/list.vue`

Quy tắc chính:

- Các form đổi tên, email, điện thoại dùng `b-form novalidate`.
- Tên bắt buộc.
- Email bắt buộc và đúng định dạng.
- Số điện thoại bắt buộc và đúng định dạng.
- Đổi mật khẩu bắt buộc mật khẩu hiện tại, mật khẩu mới tối thiểu 8 ký tự và nhập lại phải khớp.
- Cập nhật ảnh đại diện gửi file crop PNG lên backend. Giao diện chưa giới hạn dung lượng file.

#### Hồ sơ công ty

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

#### Thành viên và phân quyền

File: `src/views/settings/member/list.vue`

Quy tắc chính:

- Modal thêm/cập nhật thành viên dùng `b-form novalidate`.
- Họ tên bắt buộc.
- Điện thoại và email không bắt buộc, nhưng nếu nhập thì phải đúng định dạng.
- Khi tạo mới, mật khẩu bắt buộc, tối thiểu 8 ký tự và nhập lại phải khớp.
- Khi cập nhật, mật khẩu không bắt buộc; nếu nhập thì vẫn phải tối thiểu 8 ký tự và khớp xác nhận.
- Mật khẩu quản trị chỉ validate khi trường này được phép hiển thị cho Quản trị viên toàn quyền hoặc Quản trị viên hệ thống.
- Quản lý doanh nghiệp có sẵn quyền user level 0 nên không cần insert thêm `user_permissions` cho các quyền level 0.
- Modal phân quyền hiển thị nhóm quyền quản trị cho Quản trị viên hệ thống, không hiển thị quyền cho Quản lý doanh nghiệp và chỉ hiển thị quyền level 0 cho Nhân viên doanh nghiệp.
- Danh sách quyền chỉ load khi user có quyền thao tác phù hợp, tránh nhân viên được quyền xem thành viên nhưng bị màn hình trống do thiếu quyền sửa.

#### Danh mục khách hàng

File: `src/views/customers/categories/customer/list.vue`

Quy tắc chính:

- Mã khách hàng và tên khách hàng bắt buộc.
- Khi thêm mới hoặc cập nhật mã, backend không cho trùng mã khách hàng trong cùng công ty.
- Mã số thuế, số điện thoại, email nhận hóa đơn là tùy chọn nhưng nếu nhập phải đúng định dạng.
- Màn danh sách có thao tác xóa khách hàng và nút đi tới `Import khách hàng`.
- Khi component tự xử lý lỗi lưu, request dùng `suppressGlobalErrorToast` để không hiện double toast.
- Nếu thiếu quyền khi tải danh sách, dùng thông báo quyền chi tiết từ backend.

#### Danh mục hàng hóa

File: `src/views/customers/categories/product/list.vue`

Quy tắc chính:

- Mã sản phẩm và tên sản phẩm bắt buộc.
- Khi thêm mới hoặc cập nhật mã, backend không cho trùng mã sản phẩm trong cùng công ty.
- Đơn giá bắt buộc, là số và không âm.
- Thuế suất có giá trị mặc định theo danh sách thuế suất.
- Màn danh sách có thao tác xóa sản phẩm và nút đi tới `Import sản phẩm`.
- Lỗi quyền khi mở danh sách phải hiện rõ quyền thiếu, ví dụ `Thiếu quyền: Xem danh mục sản phẩm`.
- Các thao tác lưu, xóa, đổi trạng thái dùng Toastr và chống double toast.

#### Lập hóa đơn GTGT

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

#### Danh sách hóa đơn GTGT

File: `src/views/customers/invoices/vat-invoice/list.vue`

Quy tắc chính:

- Modal gửi email bắt buộc tên người nhận và email người nhận.
- Email người nhận phải đúng định dạng.
- Các hành động phát hành, gửi email, tải file cần giữ thông báo lỗi nghiệp vụ từ backend.

#### Import hóa đơn

File: `src/views/customers/imports/invoice/index.vue`

Quy tắc chính:

- Import bắt buộc chọn file Excel.
- Input nhận `.xlsx` và `.xls`.
- Import lại file cũ cần có ID bản ghi import và xác nhận từ người dùng.
- Mẫu Excel tải về là một mẫu dùng chung cho cả mẫu nhiều thuế suất và mẫu một thuế suất.
- Nội dung mẫu cần hướng dẫn rõ cách nhập thông tin hóa đơn, thông tin người mua và nhiều dòng hàng hóa/dịch vụ.
- Khi import, backend phải gom đúng nhiều dòng hàng thuộc cùng hóa đơn, không chỉ lấy một dòng đầu tiên.
- Với hóa đơn một thuế suất, file import vẫn phải có dữ liệu thuế suất, tiền thuế và tổng tiền hợp lệ theo mẫu đang dùng.
- Màn import hóa đơn có nút `Danh sách hóa đơn` để đi tới danh sách hóa đơn GTGT.
- Chưa validate dung lượng file và số dòng ở phía giao diện.

#### Import danh mục khách hàng/sản phẩm

File: `src/views/customers/imports/catalog/index.vue`

Quy tắc chính:

- Dùng chung component cho `/imports/customer` và `/imports/product`.
- Import bắt buộc chọn file Excel, input nhận `.xlsx` và `.xls`.
- Trang import khách hàng có nút `Danh sách khách hàng`; trang import sản phẩm có nút `Danh sách sản phẩm`.
- Mẫu Excel phải hướng dẫn mã là khóa import: mã đã tồn tại thì cập nhật bản ghi trong công ty, mã chưa có thì tạo mới.
- Backend không chấp nhận mã khách hàng hoặc mã sản phẩm bị lặp trong cùng file import.
- Lịch sử import phải lọc theo đúng loại `CUSTOMER` hoặc `PRODUCT`, không lẫn với import hóa đơn.

#### Tờ khai đăng ký hóa đơn điện tử

File: `src/views/customers/registers/invoice/create.vue`

Quy tắc chính:

- Ngày lập và nơi lập bắt buộc.
- Nhóm hình thức hóa đơn phải chọn ít nhất một lựa chọn.
- Nhóm hình thức gửi dữ liệu và phương thức chuyển dữ liệu phải có lựa chọn hợp lệ.
- Loại hóa đơn sử dụng phải chọn ít nhất một loại.
- Modal chữ ký số bắt buộc tên tổ chức chứng thực, số sê-ri, từ ngày, đến ngày.
- Đến ngày của chữ ký số phải lớn hơn hoặc bằng từ ngày.
- Thông báo gửi tờ khai đã chuyển về Toastr, không dùng kiểu thông báo BootstrapVue riêng.

#### Cấu hình máy chủ gửi mail

File: `src/views/customers/email/mail-server.vue`

Quy tắc chính:

- SMTP host, cổng, kiểu mã hóa, tên đăng nhập SMTP, tên người gửi, email gửi đi là các trường chính cần hợp lệ.
- Cổng SMTP phải nằm trong khoảng 1 đến 65535.
- Email SMTP, email gửi đi và email gửi test phải đúng định dạng.
- Mật khẩu ứng dụng bắt buộc khi tạo mới cấu hình; khi đã lưu thì không bắt buộc nhập lại.

#### Mẫu hóa đơn phía khách hàng

File: `src/views/customers/form-invoice/create.vue`

Quy tắc chính:

- Tên mẫu bắt buộc.
- Hậu tố ký hiệu bắt buộc đúng 2 chữ cái `A-Z`, tự viết hoa khi nhập.
- Ký hiệu đầy đủ được ghép từ loại hóa đơn, hình thức, năm ký hiệu và hậu tố.

### Nhóm form quản trị

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
| Cấu hình Telegram | `src/views/administrators/telegram/config.vue` | Bật/tắt Telegram, Bot Token và Group Chat ID. Khi gửi test cần giữ lỗi backend nếu token hoặc chat id không hợp lệ. |
| Báo cáo hóa đơn ngày | `src/views/administrators/daily-invoice-report/config.vue` | Bật/tắt lịch gửi, giờ gửi trong khoảng 0-23, phút gửi trong khoảng 0-59. Gửi thử dùng ngày báo cáo hợp lệ hoặc mặc định ngày hôm qua. |

### Những điểm còn cần siết nếu làm chặt hơn

- Chưa có giới hạn tối đa thống nhất theo database cho tên công ty, địa chỉ, tên khách hàng, tên hàng hóa, ghi chú.
- Chưa áp dụng chính sách mật khẩu mạnh gồm chữ hoa, chữ thường, số và ký tự đặc biệt; hiện chủ yếu là tối thiểu 8 ký tự.
- Chưa validate dung lượng file upload trên hầu hết màn hình.
- Một số form quản trị cũ vẫn còn dựa một phần vào backend validate. Khi sửa tiếp nên chuyển dần về helper chung.
- Các field như fax, website, số tài khoản ngân hàng đã có kiểm tra ở hồ sơ công ty, nhưng chưa chắc đã được áp dụng giống nhau ở mọi form khác.
- Các input `type=email`, `type=number`, `required` có thể vẫn tồn tại để hỗ trợ bàn phím và accessibility, nhưng form có validate riêng phải dùng `novalidate` để không hiện tooltip mặc định của trình duyệt.
- Một số màn cấu hình admin mới vẫn cần rà thêm giới hạn độ dài chuỗi theo database, ví dụ Bot Token, Group Chat ID và nội dung test.

### Checklist khi thêm hoặc sửa form

1. Thêm `novalidate` cho form nếu tự validate.
2. Dùng helper trong `src/utils/validators.js` trước khi tự viết regex mới.
3. Hiển thị lỗi ngay dưới input bằng BootstrapVue state/feedback.
4. Nội dung lỗi viết tiếng Việt, ngắn và chỉ rõ field sai.
5. Không dùng `$bvToast`; dùng `this.$toastr` hoặc helper trong `src/utils/toast.js`.
6. Request đã tự xử lý lỗi tại component phải truyền `meta: { suppressGlobalErrorToast: true }`.
7. Với lỗi quyền, ưu tiên hiển thị message từ backend để người dùng biết thiếu quyền nào.
8. Không tin dữ liệu từ frontend; backend vẫn validate lại trước khi ghi database.

### Lệnh kiểm tra nên chạy

Khi chỉ sửa tài liệu:

```bash
git diff --check -- 04_PHAN_QUYEN_VA_VALIDATE.md
```

Khi sửa giao diện Vue:

```bash
npx vue-cli-service build --dest /tmp/hoadon-validation-build
```

Khi sửa backend validate hoặc phân quyền:

```bash
JAVA_HOME=/home/nvpa/.local/share/jdks/jdk-21.0.11+10 ./mvnw -DskipTests clean compile
```
