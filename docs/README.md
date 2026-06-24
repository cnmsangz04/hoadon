# Hệ thống hóa đơn điện tử

Tài liệu dự án đã được gom lại để dễ đọc và tránh quá nhiều file rời. Dự án gồm backend Spring Boot, frontend Vue 2 và SQL Server, phục vụ quản lý công ty, người dùng, mẫu hóa đơn, hóa đơn GTGT, ký hóa đơn, gửi cơ quan thuế, gửi email, báo cáo, thanh toán và cấu hình quản trị.

Cập nhật: 2026-06-22.

## Danh sách tài liệu

| Tài liệu | Nội dung chính |
| --- | --- |
| [01_TONG_QUAN_HE_THONG.md](01_TONG_QUAN_HE_THONG.md) | Tổng quan, yêu cầu hệ thống, use case, luồng nghiệp vụ và mô tả chi tiết chương trình. |
| [02_KIEN_TRUC_KY_THUAT.md](02_KIEN_TRUC_KY_THUAT.md) | Kiến trúc, công nghệ, cấu trúc thư mục và cơ sở dữ liệu. |
| [03_API_CAU_HINH_VAN_HANH.md](03_API_CAU_HINH_VAN_HANH.md) | Tổng quan API, cấu hình, vận hành, bảo trì và lệnh kiểm tra. |
| [04_PHAN_QUYEN_VA_VALIDATE.md](04_PHAN_QUYEN_VA_VALIDATE.md) | Quy tắc vai trò, phân quyền, phạm vi dữ liệu và validate form. |
| [05_DINH_DANG_HOA_DON_VA_TO_KHAI.md](05_DINH_DANG_HOA_DON_VA_TO_KHAI.md) | Link tài liệu nguồn, định dạng hóa đơn điện tử và cấu trúc tờ khai XML. |
| [06_THANH_TOAN.md](06_THANH_TOAN.md) | Tích hợp MoMo, VNPAY và ZaloPay. |
| [07_BAO_CAO_VA_KIEM_THU.md](07_BAO_CAO_VA_KIEM_THU.md) | Hướng dẫn viết báo cáo, sơ đồ nên dùng và kiểm thử hệ thống. |
| [08_TAI_LIEU_THAM_KHAO.md](08_TAI_LIEU_THAM_KHAO.md) | Danh mục tài liệu tham khảo có link: pháp lý hóa đơn điện tử, backend, frontend, database và tích hợp ngoài. |

## Chạy nhanh môi trường phát triển

Điều kiện cần có:

- Java 21.
- Node.js/npm tương thích Vue CLI 5.
- SQL Server đang chạy và có database `hoadon_database`.
- Cấu hình kết nối database nằm trong `src/main/resources/application.properties`.

Chạy backend:

```bash
./mvnw spring-boot:run
```

Chạy frontend:

```bash
npm install
npm run serve
```

Build frontend vào thư mục static của backend:

```bash
npm run build
```

Chạy test backend:

```bash
./mvnw test
```

## File cần xem khi điều tra lỗi

| Lỗi cần kiểm tra | File/thư mục nên xem |
| --- | --- |
| Backend không chạy | `src/main/resources/application.properties`, `logs/hoadon.log`, `pom.xml`. |
| Frontend không chạy | `package.json`, `vue.config.js`, `src/router`. |
| Sai quyền hoặc sai menu | `docs/04_PHAN_QUYEN_VA_VALIDATE.md`, `src/router`, `PermissionServiceImpl`. |
| Sai dữ liệu công ty | `docs/02_KIEN_TRUC_KY_THUAT.md`, repository/service liên quan. |
| Sai mail | `mail_templates`, `mail_jobs`, `MailQueueServiceImpl`, `DbMailQueueWorker`. |
| Sai báo cáo ngày | `DailyInvoiceReportServiceImpl`, `DailyInvoiceReportConfigRepository`, `daily_invoice_report_configs`. |
| Sai XML hóa đơn | `docs/05_DINH_DANG_HOA_DON_VA_TO_KHAI.md`, `InvoiceXmlBuilder`, `SignatureVatRepository`, `SignatureAuthoritiesTaxRepository`. |
| Sai import Excel | `InvoiceImportServiceImpl`, `CatalogImportServiceImpl`. |

## Nguồn cấu hình quan trọng

- Backend: `src/main/resources/application.properties`.
- Frontend: `package.json`, `vue.config.js`, `src/router`.
- Database: `db.dbml`, `db.dbdiagram`.
- Script dữ liệu hoặc chỉnh schema thủ công: `tools/sql`.
- File upload runtime: `uploads`.
- Log runtime: `logs/hoadon.log`.
