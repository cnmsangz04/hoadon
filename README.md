# Hệ thống hóa đơn điện tử

Tài liệu này là điểm bắt đầu cho người mới đọc mã nguồn chương trình. Dự án gồm backend Spring Boot, frontend Vue 2 và cơ sở dữ liệu SQL Server, phục vụ quản lý công ty, người dùng, mẫu hóa đơn, hóa đơn GTGT, ký hóa đơn, gửi cơ quan thuế, gửi email, báo cáo và các cấu hình quản trị.

Cập nhật: 18/06/2026.

## Đọc tài liệu theo thứ tự

| Tài liệu | Nội dung chính |
| --- | --- |
| [Tổng quan chương trình](docs/TONG_QUAN.md) | Mục tiêu, nhóm người dùng, phạm vi chức năng. |
| [Kiến trúc chương trình](docs/KIEN_TRUC.md) | Mô hình 3 tầng, Spring MVC REST, backend nhiều lớp và cách các thành phần phối hợp. |
| [Cấu trúc thư mục](docs/CAU_TRUC_THU_MUC.md) | Vai trò các thư mục và nhóm file quan trọng. |
| [Cơ sở dữ liệu](docs/CO_SO_DU_LIEU.md) | Nhóm bảng, quan hệ chính và nguyên tắc dữ liệu nhiều công ty. |
| [Công nghệ áp dụng](docs/CONG_NGHE.md) | Framework, thư viện và lý do sử dụng. |
| [Cấu hình và vận hành](docs/CAU_HINH_VA_VAN_HANH.md) | Cổng chạy, cấu hình database, mail, thanh toán, log và upload. |
| [Luồng nghiệp vụ chính](docs/LUONG_NGHIEP_VU.md) | Các luồng đăng ký, hóa đơn, import, email, báo cáo ngày, thanh toán. |
| [Yêu cầu hệ thống](docs/YEU_CAU_HE_THONG.md) | Yêu cầu chức năng, phi chức năng và ràng buộc nghiệp vụ. |
| [Use case hệ thống](docs/USE_CASE.md) | Tác nhân, danh sách use case và mô tả luồng chính. |
| [Tổng quan API](docs/API_TONG_QUAN.md) | Nhóm API public, doanh nghiệp, cài đặt và quản trị. |
| [Kiểm thử hệ thống](docs/KIEM_THU_HE_THONG.md) | Nhóm kiểm thử và test case tiêu biểu. |
| [Sơ đồ nên dùng trong báo cáo](docs/SO_DO_BAO_CAO.md) | Gợi ý sơ đồ và Mermaid mẫu để đưa vào báo cáo. |
| [Hướng dẫn viết báo cáo](docs/HUONG_DAN_VIET_BAO_CAO.md) | Cách sắp xếp nội dung để viết báo cáo đầy đủ. |
| [Bảo trì và kiểm tra](docs/BAO_TRI_VA_KIEM_TRA.md) | Lệnh thường dùng, checklist kiểm tra và lưu ý khi sửa hệ thống. |

## Tài liệu chi tiết đang có

| Tài liệu | Ghi chú |
| --- | --- |
| [MO_TA_CHI_TIET_CHUONG_TRINH.md](MO_TA_CHI_TIET_CHUONG_TRINH.md) | Mô tả nghiệp vụ chi tiết theo nhiều nhóm chức năng. |
| [QUY_TAC_VAI_TRO_CHUONG_TRINH.md](QUY_TAC_VAI_TRO_CHUONG_TRINH.md) | Quy tắc vai trò, phân quyền, phạm vi dữ liệu và đăng nhập. |
| [dinh_dang_du_lieu_hoa_don_dien_tu.md](dinh_dang_du_lieu_hoa_don_dien_tu.md) | Định dạng dữ liệu hóa đơn điện tử. |
| [cau_truc_tkhai_theo_xml_mau.md](cau_truc_tkhai_theo_xml_mau.md) | Cấu trúc tờ khai theo XML mẫu. |
| [FORM_VALIDATION_SUMMARY.md](FORM_VALIDATION_SUMMARY.md) | Ghi chú liên quan kiểm tra dữ liệu form. |

## Chạy nhanh môi trường phát triển

Backend chạy trên cổng `8081`:

```bash
./mvnw spring-boot:run
```

Frontend chạy trên cổng `8080`:

```bash
npm install
npm run serve
```

Build frontend vào thư mục static của backend:

```bash
npm run build
```

## Nguồn cấu hình quan trọng

- Backend: `src/main/resources/application.properties`.
- Frontend: `package.json`, `vue.config.js`, `src/router`.
- Database: `db.dbml`, `db.dbdiagram`.
- Script dữ liệu hoặc chỉnh schema thủ công: `tools/sql`.
- File upload runtime: `uploads`.
- Log runtime: `logs/hoadon.log`.
