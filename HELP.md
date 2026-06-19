# Trợ giúp nhanh cho dự án hóa đơn điện tử

File này thay cho nội dung mẫu của Spring Boot để người mới vào dự án có thể tìm đúng tài liệu cần đọc.

## Nên bắt đầu từ đâu

1. Đọc [README.md](README.md) để biết cách chạy chương trình và danh sách tài liệu.
2. Đọc [docs/TONG_QUAN.md](docs/TONG_QUAN.md) để nắm phạm vi chức năng.
3. Đọc [docs/KIEN_TRUC.md](docs/KIEN_TRUC.md) để hiểu cách frontend, backend và database phối hợp.
4. Đọc [docs/CO_SO_DU_LIEU.md](docs/CO_SO_DU_LIEU.md) trước khi sửa dữ liệu hoặc query.
5. Đọc [docs/YEU_CAU_HE_THONG.md](docs/YEU_CAU_HE_THONG.md), [docs/USE_CASE.md](docs/USE_CASE.md) và [docs/HUONG_DAN_VIET_BAO_CAO.md](docs/HUONG_DAN_VIET_BAO_CAO.md) nếu cần viết báo cáo.
6. Đọc [QUY_TAC_VAI_TRO_CHUONG_TRINH.md](QUY_TAC_VAI_TRO_CHUONG_TRINH.md) trước khi sửa quyền, route hoặc API theo công ty.

## Lệnh nhanh

Chạy backend:

```bash
./mvnw spring-boot:run
```

Chạy frontend:

```bash
npm run serve
```

Build frontend:

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
| Sai quyền hoặc sai menu | `QUY_TAC_VAI_TRO_CHUONG_TRINH.md`, `src/router`, `PermissionServiceImpl`. |
| Sai dữ liệu công ty | `docs/CO_SO_DU_LIEU.md`, repository/service liên quan. |
| Sai mail | `mail_templates`, `mail_jobs`, `MailQueueServiceImpl`, `DbMailQueueWorker`. |
| Sai báo cáo ngày | `DailyInvoiceReportServiceImpl`, `DailyInvoiceReportConfigRepository`, `daily_invoice_report_configs`. |
| Sai XML hóa đơn | `InvoiceXmlBuilder`, `SignatureVatRepository`, `SignatureAuthoritiesTaxRepository`. |
| Sai import Excel | `InvoiceImportServiceImpl`, `CatalogImportServiceImpl`. |
