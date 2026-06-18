# Công nghệ áp dụng

Cập nhật: 18/06/2026.

## Backend

| Công nghệ | Vai trò |
| --- | --- |
| Java 21 | Ngôn ngữ chạy backend. |
| Spring Boot 3.5.7 | Khung ứng dụng chính. |
| Spring Web | Xây dựng REST API. |
| Spring Security | Bảo vệ API và cấu hình bảo mật. |
| JWT | Xác thực request bằng token. |
| Spring Data JPA | Ánh xạ entity và truy vấn database. |
| Spring Data JDBC | Hỗ trợ truy cập dữ liệu SQL khi cần. |
| SQL Server JDBC | Kết nối SQL Server theo cấu hình hiện tại. |
| Spring Mail | Gửi email qua SMTP. |
| Spring Scheduler | Chạy tác vụ nền theo lịch. |
| Thymeleaf | Hỗ trợ template phía backend khi cần render nội dung. |
| OpenHTMLToPDF | Xuất PDF từ HTML/XSLT. |
| Apache POI | Đọc và ghi file Excel. |
| Jackson | Xử lý JSON. |
| Validation | Kiểm tra dữ liệu đầu vào. |

## Frontend

| Công nghệ | Vai trò |
| --- | --- |
| Vue 2.7 | Khung giao diện chính. |
| Vue Router 3 | Điều hướng trang và route guard. |
| BootstrapVue | Component giao diện. |
| Bootstrap 4 | Nền CSS giao diện. |
| Axios | Gọi API backend. |
| Toastr | Hiển thị thông báo. |
| vue-select | Dropdown tìm kiếm. |
| TinyMCE | Soạn nội dung mẫu hoặc nội dung giàu định dạng. |
| Font Awesome | Icon giao diện. |
| vue-advanced-cropper | Cắt ảnh logo/avatar. |
| vuedraggable | Kéo thả sắp xếp ở một số màn hình. |

## Database và file

| Thành phần | Vai trò |
| --- | --- |
| SQL Server | Database chính theo cấu hình hiện tại. |
| DBML/dbdiagram | Mô tả schema và quan hệ database. |
| `uploads` | Lưu file upload runtime. |
| `logs` | Lưu log ứng dụng. |

## Tích hợp bên ngoài

| Tích hợp | Vai trò |
| --- | --- |
| SMTP | Gửi email hóa đơn, báo cáo, thông báo tài khoản. |
| Telegram Bot | Gửi thông báo hoặc báo cáo vào nhóm Telegram. |
| MoMo | Thanh toán mua gói hóa đơn. |
| VNPAY | Thanh toán mua gói hóa đơn. |
| Cơ quan thuế | Luồng XML hóa đơn/tờ khai và trạng thái gửi cơ quan thuế. |

## Ghi chú lựa chọn kỹ thuật

- Backend và frontend cùng nằm trong một repository để dễ build, triển khai và đồng bộ nghiệp vụ.
- Frontend khi build sẽ được đưa vào `src/main/resources/static`, phù hợp kiểu triển khai một ứng dụng Spring Boot phục vụ cả API và giao diện.
- Hàng đợi mail nằm trong database, giúp xem lại lịch sử, gửi lại và xử lý lỗi gửi mail.
- Các dữ liệu nhạy cảm như mật khẩu SMTP hoặc token Telegram cần được mã hóa trước khi lưu.

