# Bảo trì và kiểm tra

Cập nhật: 19/06/2026.

## Checklist trước khi sửa nghiệp vụ

- Xác định màn hình thuộc khu người dùng, setting hay quản trị.
- Xác định dữ liệu có cần lọc theo `company_id` không.
- Kiểm tra quyền cần dùng trong bảng `permissions`.
- Kiểm tra route frontend có guard phù hợp không.
- Kiểm tra service có đang dùng đúng repository và đúng phạm vi công ty không.
- Nếu sửa mail, kiểm tra `mail_jobs.company_id`, người gửi, người nhận, tiêu đề và nội dung.
- Nếu sửa hóa đơn, kiểm tra XML, PDF, trạng thái hóa đơn, chữ ký và dữ liệu gửi cơ quan thuế.

## Lệnh kiểm tra thường dùng

Chạy test backend:

```bash
./mvnw test
```

Build backend:

```bash
./mvnw package
```

Chạy frontend dev:

```bash
npm run serve
```

Build frontend:

```bash
npm run build
```

Kiểm tra file markdown hiện có:

```bash
rg --files -g '*.md' -g '*.MD'
```

Kiểm tra khác biệt code:

```bash
git diff --check
```

## Khi sửa database

- Cập nhật `db.dbml` hoặc `db.dbdiagram` nếu thay đổi schema.
- Tạo script SQL trong `tools/sql` nếu cần chạy dữ liệu hoặc migration thủ công.
- Đặt tên script có ngày và mục đích, ví dụ `2026-06-18_ten_thay_doi.sql`.
- Khi thêm quyền mới, tạo script idempotent để insert/update `permission_categories` và `permissions`.
- Không dựa hoàn toàn vào `ddl-auto=update` cho môi trường thật.
- Kiểm tra dữ liệu nhiều công ty, đặc biệt các bảng hóa đơn, chữ ký, mail và báo cáo.

## Khi sửa mail

- Kiểm tra mẫu trong `mail_templates`.
- Kiểm tra tiêu đề đã thay placeholder như `[REPORT_DATE]` trước khi lưu job.
- Kiểm tra `mail_jobs.company_id` là công ty phát sinh nội dung.
- Kiểm tra người gửi và người nhận không bị lấy nhầm giữa công ty root và công ty thường.
- Kiểm tra retry không tạo trùng nội dung hoặc gửi sai công ty.

## Khi sửa báo cáo hóa đơn ngày

- Cấu hình lịch gửi nằm ở `daily_invoice_report_configs`.
- Cấu hình Telegram nằm ở `telegram_configs`.
- Gửi thử và gửi tự động cần dùng cùng logic gom dữ liệu.
- Email chỉ gửi khi có dữ liệu.
- Telegram vẫn có thể thông báo khi không có hóa đơn cần kiểm tra.
- Cần lưu ngày báo cáo gần nhất và thời điểm gửi để tránh gửi lặp.

## Khi sửa import Excel

- Mẫu tải về phải đủ cột cho các loại mẫu hóa đơn đang hỗ trợ.
- File import phải đọc được nhiều hóa đơn và nhiều dòng hàng.
- Với import danh mục, file phải kiểm tra trùng mã trong cùng file và dùng mã để cập nhật bản ghi đã tồn tại trong công ty.
- Lịch sử import hóa đơn và import danh mục cùng nằm trong `invoice_imports`, cần lọc đúng `import_type`.
- Cần kiểm tra trường hợp một thuế suất và nhiều thuế suất.
- Lỗi import nên chỉ rõ dòng dữ liệu để người dùng sửa được.
- Không nên thay đổi thứ tự cột mà không cập nhật service đọc file và hướng dẫn trong mẫu.

## Khi sửa danh mục khách hàng/sản phẩm

- Kiểm tra thêm mới không cho trùng mã trong cùng công ty.
- Kiểm tra cập nhật vẫn cho đổi mã, nhưng không được đổi sang mã đang thuộc bản ghi khác.
- Kiểm tra xóa chỉ thao tác bản ghi thuộc công ty hiện tại.
- Kiểm tra màn lập hóa đơn nạp đúng bản ghi được chọn, kể cả dữ liệu cũ từng có mã trùng.
- Kiểm tra nút đi tới import và nút đi tới danh sách tương ứng vẫn đúng route.

## Khi sửa XML hóa đơn

- Kiểm tra dữ liệu đầu vào theo loại mẫu hóa đơn.
- Kiểm tra XML sinh ra có đúng thông tin người bán, người mua, dòng hàng, thuế và tổng tiền.
- Khi lấy XML đã ký, phải lọc đúng hóa đơn và công ty.
- Khi trả file cho người dùng, cần tránh dùng XML của hóa đơn khác hoặc công ty khác.

## Khi sửa giao diện

- Giữ style đồng bộ với các màn hình hiện có.
- Kiểm tra trên màn hình nhỏ và màn hình desktop.
- Kiểm tra route, menu, quyền truy cập và thông báo lỗi.
- Nếu thêm màn hình mới, cập nhật tài liệu route hoặc cấu trúc thư mục khi cần.
