# Hướng dẫn viết báo cáo chương trình

Cập nhật: 18/06/2026.

Tài liệu này giúp người viết báo cáo biết nên lấy nội dung ở đâu và sắp xếp báo cáo như thế nào để trình bày đầy đủ hệ thống hóa đơn điện tử.

## Cấu trúc báo cáo đề xuất

| Chương | Nội dung nên viết | Tài liệu tham khảo |
| --- | --- | --- |
| 1. Mở đầu | Lý do chọn đề tài, mục tiêu, phạm vi, đối tượng sử dụng. | `docs/TONG_QUAN.md`, `MO_TA_CHI_TIET_CHUONG_TRINH.md` |
| 2. Cơ sở lý thuyết | Hóa đơn điện tử, mô hình 03 lớp, Spring MVC REST, JWT, queue mail. | `docs/KIEN_TRUC.md`, `docs/CONG_NGHE.md` |
| 3. Khảo sát và yêu cầu | Tác nhân, yêu cầu chức năng, yêu cầu phi chức năng. | `docs/YEU_CAU_HE_THONG.md`, `docs/USE_CASE.md` |
| 4. Phân tích thiết kế | Kiến trúc, luồng xử lý, database, phân quyền, bảo mật. | `docs/KIEN_TRUC.md`, `docs/CO_SO_DU_LIEU.md`, `QUY_TAC_VAI_TRO_CHUONG_TRINH.md` |
| 5. Cài đặt chương trình | Công nghệ, cấu trúc thư mục, API, cấu hình chạy. | `docs/CONG_NGHE.md`, `docs/CAU_TRUC_THU_MUC.md`, `docs/API_TONG_QUAN.md`, `docs/CAU_HINH_VA_VAN_HANH.md` |
| 6. Chức năng chính | Mô tả màn hình và luồng nghiệp vụ. | `docs/LUONG_NGHIEP_VU.md`, `MO_TA_CHI_TIET_CHUONG_TRINH.md` |
| 7. Sơ đồ minh họa | Kiến trúc, use case, 03 lớp, sequence, ERD. | `docs/SO_DO_BAO_CAO.md`, `db.dbml`, `db.dbdiagram` |
| 8. Kiểm thử | Test case, kết quả mong đợi, lỗi cần chú ý. | `docs/KIEM_THU_HE_THONG.md`, `FORM_VALIDATION_SUMMARY.md` |
| 9. Kết luận | Kết quả đạt được, hạn chế, hướng phát triển. | Tổng hợp từ các tài liệu trên. |

## Cách mô tả mô hình chương trình

Có thể viết:

> Hệ thống được xây dựng theo mô hình ứng dụng web 3 tầng gồm tầng giao diện Vue, tầng xử lý nghiệp vụ Spring Boot và tầng dữ liệu SQL Server. Riêng backend Java áp dụng mô hình 03 lớp gồm Presentation layer, Business logic layer và Data access layer trên nền Spring MVC REST.

Khi cần giải thích 03 lớp:

- Presentation layer: giao diện Vue và Controller backend.
- Business logic layer: Service xử lý nghiệp vụ, quyền, vai trò và phạm vi công ty.
- Data access layer: Repository, Entity và SQL Server.

## Chức năng nên đưa vào báo cáo

- Đăng ký công ty và duyệt hồ sơ.
- Đăng nhập, JWT, phiên đăng nhập và phân quyền.
- Quản lý hồ sơ công ty, thành viên, bảo mật IP.
- Quản lý mẫu hóa đơn, tờ khai và hóa đơn GTGT.
- Import hóa đơn từ Excel.
- Ký hóa đơn, gửi cơ quan thuế, xem XML/PDF.
- Gửi email hóa đơn và hàng đợi mail.
- Báo cáo hóa đơn và báo cáo hóa đơn ngày qua Telegram/email.
- Mua gói hóa đơn và thanh toán.
- Tra cứu hóa đơn công khai.

## Nội dung cần có khi viết phần database

- Giới thiệu SQL Server là database chính.
- Nêu các nhóm bảng: công ty, người dùng, phân quyền, hóa đơn, chữ ký/XML, mail, báo cáo, thanh toán, danh mục nền.
- Nhấn mạnh nguyên tắc dữ liệu nhiều công ty theo `company_id`.
- Dẫn nguồn schema từ `db.dbml` và `db.dbdiagram`.

## Nội dung cần có khi viết phần bảo mật

- JWT và tách token user/admin.
- Kiểm tra phiên đăng nhập bằng `login_sessions`.
- Phân quyền theo `permissions`, `permission_categories`, `user_permissions`.
- Phạm vi công ty theo `company_id`.
- Bảo mật IP theo công ty.
- Mã hóa mật khẩu SMTP và Telegram bot token.
- Public API chỉ mở cho chức năng cần thiết như tra cứu hóa đơn và callback thanh toán.

## Hướng phát triển có thể ghi trong báo cáo

- Tách quyền riêng cho báo cáo hóa đơn ngày thay vì dùng chung `telegram-config-manage`.
- Chuẩn hóa migration database thay cho phụ thuộc `ddl-auto=update`.
- Bổ sung test tự động cho service quan trọng.
- Bổ sung audit log chi tiết hơn cho thao tác phát hành hóa đơn.
- Bổ sung dashboard quản trị sâu hơn theo công ty, gói hóa đơn và trạng thái gửi cơ quan thuế.
- Chuẩn hóa validate độ dài field theo database trên toàn bộ frontend.
