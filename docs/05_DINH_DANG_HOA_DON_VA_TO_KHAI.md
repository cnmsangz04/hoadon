# Định Dạng Hóa Đơn Và Tờ Khai

Tài liệu gom link tài liệu nguồn, định dạng dữ liệu hóa đơn điện tử và cấu trúc tờ khai theo XML mẫu.

## Nguồn đã hợp nhất

- `tai_lieu_lien_quan_hoa_don_dien_tu.md`
- `dinh_dang_du_lieu_hoa_don_dien_tu.md`
- `cau_truc_tkhai_theo_xml_mau.md`

## Nội dung

### Từ `tai_lieu_lien_quan_hoa_don_dien_tu.md`

Ngày tổng hợp: 2026-06-22

### Link tài liệu nguồn

| Nhóm tài liệu | Tài liệu | Link | Ghi chú |
| --- | --- | --- | --- |
| Thông báo chấp nhận/không chấp nhận | Định dạng dữ liệu thông báo về việc chấp nhận/không chấp nhận | [Google Docs](https://docs.google.com/document/d/1_i7-stCGdYm_wxkjwsJXviICaq5v1Z1_/edit?usp=sharing&ouid=112406957879396621294&rtpof=true&sd=true) | Dùng để tham chiếu cấu trúc dữ liệu thông báo kết quả chấp nhận hoặc không chấp nhận. |
| Tờ khai đăng ký/thay đổi | Định dạng dữ liệu tờ khai đăng ký/thay đổi thông tin sử dụng hóa đơn điện tử | [Google Docs](https://docs.google.com/document/d/1mhniM9SVumJ8_m5D33n_ZgXl3j247ipu/edit?usp=sharing&ouid=112406957879396621294&rtpof=true&sd=true) | Dùng khi sinh, kiểm tra hoặc đối chiếu XML tờ khai đăng ký/thay đổi thông tin sử dụng hóa đơn điện tử. |
| Thông báo tiếp nhận/không tiếp nhận tờ khai | Định dạng dữ liệu thông báo về tiếp nhận/không tiếp nhận tờ khai đăng ký/thay đổi thông tin sử dụng hóa đơn, chứng từ điện tử | [Google Docs](https://docs.google.com/document/d/1ztmrI4cuPnCNhtMDCND05_j9R4AVoyFcxzrLgd31oKk/edit?usp=sharing) | Dùng để tham chiếu thông điệp phản hồi tiếp nhận hoặc không tiếp nhận tờ khai. |
| Hóa đơn điện tử | Định dạng dữ liệu hóa đơn điện tử | [Google Docs](https://docs.google.com/document/d/1ziOIEue_-gH8QlnON4mAoov5N245W1_WsbPGTMTuric/edit?usp=sharing) | Tài liệu chính để đối chiếu cấu trúc, tên thẻ, ràng buộc và kiểu dữ liệu hóa đơn điện tử. |

### Tài liệu nội bộ trong repo

| Tài liệu | Đường dẫn | Mục đích |
| --- | --- | --- |
| Định dạng dữ liệu hóa đơn điện tử | [05_DINH_DANG_HOA_DON_VA_TO_KHAI.md](05_DINH_DANG_HOA_DON_VA_TO_KHAI.md) | Bản tổng hợp cấu trúc hóa đơn điện tử đang có trong repo. |
| Cấu trúc dữ liệu tờ khai theo XML mẫu | [05_DINH_DANG_HOA_DON_VA_TO_KHAI.md](05_DINH_DANG_HOA_DON_VA_TO_KHAI.md) | Bản rút gọn theo XML mẫu hiện có, hữu ích khi đối chiếu phần sinh XML tờ khai. |

### Ghi chú cập nhật

- Khi có thêm tài liệu quy chuẩn mới, bổ sung vào mục "Link tài liệu nguồn".
- Khi có thêm file tổng hợp nội bộ trong repo, bổ sung vào mục "Tài liệu nội bộ trong repo".
- Nếu tài liệu nguồn thay đổi phiên bản, ghi rõ ngày cập nhật và phiên bản nếu có.

### Từ `dinh_dang_du_lieu_hoa_don_dien_tu.md`

### Hóa đơn giá trị gia tăng

> Tài liệu này tổng hợp các thẻ dữ liệu chính trong cấu trúc hóa đơn điện tử, bao gồm thông tin chung, người bán, người mua, hàng hóa dịch vụ, thanh toán, QR Code và chữ ký số.

### Cấu trúc tổng quát

- `HDon`: Chứa thông tin dữ liệu hóa đơn, dữ liệu QR Code và thông tin chữ ký số.
- `HDon\DLHDon`: Chứa thông tin chung, nội dung chi tiết hóa đơn và thông tin khác do người bán tự định nghĩa.
- `HDon\DLHDon\TTChung`: Chứa thông tin chung của hóa đơn.
- `HDon\DLHDon\NDHDon`: Chứa nội dung hóa đơn, bao gồm thông tin người bán, người mua, danh sách hàng hóa/dịch vụ và thông tin thanh toán.
- `HDon\DSCKS`: Chứa thông tin chữ ký số của người bán/đơn vị nhận ủy nhiệm, người mua, cơ quan thuế và các chữ ký số khác nếu có.

### HDon\DLHDon\TTChung - Thông tin chung của hóa đơn

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Phiên bản của thông điệp (trong Quy định này có giá trị là 2.1.0) | PBan | 6 | Chuỗi ký tự | Bắt buộc |  |
| Tên hóa đơn | THDon | 100 | Chuỗi ký tự | Bắt buộc | Khoản 1 và khoản 14, Điều 10 Nghị định 123. |
| Ký hiệu mẫu số hóa đơn | KHMSHDon | 1 | Chuỗi ký tự (Chi tiết tại Phụ lục II kèm theo Quy định này) | Bắt buộc | Khoản 1 và khoản 14, Điều 10 Nghị định 123; Khoản 1 Điều 4 Thông tư số 78/2021/TT-BTC |
| Ký hiệu hóa đơn | KHHDon | 6 | Chuỗi ký tự | Bắt buộc | Khoản 1 và khoản 14 Điều 10 Nghị định 123; Khoản 1 Điều 4 Thông tư số 78/2021/TT-BTC |
| Số hóa đơn | SHDon | 8 | Số | Bắt buộc (Nếu có) | Khoản 3 và khoản 14 Điều 10 Nghị định 123 |
| Mã hồ sơ | MHSo | 20 | Chuỗi ký tự | Bắt buộc (Đối với trường hợp là hóa đơn đề nghị cấp mã của cơ quan thuế theo từng lần phát sinh) |  |
| Ngày lập | NLap |  | Ngày | Bắt buộc |  |
| Hóa đơn cho thuê tài chính (Hóa đơn đối với hoạt động cho thuê tài chính) | HDCTTChinh | 1 | Số (1: Hóa đơn dành cho hoạt động thuê tài chính, 0: Hóa đơn không dành cho hoạt động cho thuê tài chính) | Bắt buộc |  |
| Số bảng kê (Số của bảng kê các loại hàng hóa, dịch vụ đã bán kèm theo hóa đơn hoặc hóa đơn chiết khấu thương mại) | SBKe | 50 | Chuỗi ký tự | Bắt buộc (Nếu có) | Điểm a, khoản 6, Điều 10 Nghị định 123 |
| Ngày bảng kê (Ngày của bảng kê các loại hàng hóa, dịch vụ đã bán kèm theo hóa đơn hoặc hóa đơn chiết khấu thương mại) | NBKe |  | Ngày | Bắt buộc (Nếu có) | Điểm a, khoản 6, Điều 10 Nghị định 123 |
| Đơn vị tiền tệ | DVTTe | 3 | Chuỗi ký tự (Chi tiết tại Khoản 2, Mục IV, Phần I Quy định này) | Bắt buộc |  |
| Tỷ giá | TGia | 7,2 | Số | Bắt buộc (Trừ trường hợp Đơn vị tiền tệ là VND) |  |
| Hình thức thanh toán | HTTToan | 50 | Chuỗi ký tự | Bắt buộc |  |
| Mã số thuế tổ chức cung cấp giải pháp hóa đơn điện tử | MSTTCGP | 14 | Chuỗi ký tự | Bắt buộc |  |
| Mã số thuế đơn vị nhận ủy nhiệm lập hóa đơn | MSTDVNUNLHDon | 14 | Chuỗi ký tự | Bắt buộc (Đối với trường hợp ủy nhiệm lập hóa đơn) |  |
| Tên đơn vị nhận ủy nhiệm lập hóa đơn | TDVNUNLHDon | 400 | Chuỗi ký tự | Bắt buộc (Đối với trường hợp ủy nhiệm lập hóa đơn) |  |
| Địa chỉ đơn vị nhận ủy nhiệm lập hóa đơn | DCDVNUNLHDon | 400 | Chuỗi ký tự | Bắt buộc (Đối với trường hợp ủy nhiệm lập hóa đơn) |  |

### HDon\DLHDon\TTChung\TTHDLQuan - Thông tin hóa đơn liên quan

Áp dụng trong trường hợp hóa đơn điều chỉnh, thay thế hoặc chiết khấu thương mại.

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Tính chất hóa đơn | TCHDon | 1 | Số (1: Thay thế, 2: Điều chỉnh) | Bắt buộc |  |
| Loại hóa đơn có liên quan (Loại hóa đơn bị thay thế/điều chỉnh) | LHDCLQuan | 1 | Số (Chi tiết tại Phụ lục VI kèm theo Quy định này) | Bắt buộc (Đối với trường hợp điều chỉnh, thay thế cho 1 hóa đơn) |  |
| Ký hiệu mẫu số hóa đơn có liên quan (Ký hiệu mẫu số hóa đơn bị thay thế/điều chỉnh) | KHMSHDCLQuan | 11 | Chuỗi ký tự (Chi tiết tại Phụ lục II kèm theo Quy định này) | Bắt buộc (Đối với trường hợp điều chỉnh, thay thế cho 1 hóa đơn có Ký hiệu mẫu số hóa đơn, Ký hiệu hóa đơn, Số hóa đơn) |  |
| Ký hiệu hóa đơn có liên quan (Ký hiệu hóa đơn bị thay thế/điều chỉnh) | KHHDCLQuan | 8 | Chuỗi ký tự | Bắt buộc (Đối với trường hợp điều chỉnh, thay thế cho 1 hóa đơn có Ký hiệu mẫu số hóa đơn, Ký hiệu hóa đơn, Số hóa đơn) |  |
| Số hóa đơn có liên quan (Số hóa đơn bị thay thế/điều chỉnh) | SHDCLQuan | 8 | Chuỗi ký tự | Bắt buộc (Đối với trường hợp điều chỉnh, thay thế cho 1 hóa đơn có Ký hiệu mẫu số hóa đơn, Ký hiệu hóa đơn, Số hóa đơn) |  |
| Ngày lập hóa đơn có liên quan (Ngày lập hóa đơn bị thay thế/điều chỉnh) | NLHDCLQuan |  | Ngày | Bắt buộc (Đối với trường hợp điều chỉnh, thay thế cho 1 hóa đơn) |  |
| Ghi chú | GChu | 255 | Chuỗi ký tự | Không bắt buộc |  |
| Số bảng kê có liên quan (Số bảng kê thay thế/điều chỉnh) | SBKCLQuan | 50 | Chuỗi ký tự | Bắt buộc (Đối với trường hợp điều chỉnh, thay thế cho nhiều hóa đơn) | Khoản 13 điều 1 Nghị định 70 |
| Ngày bảng kê có liên quan (Ngày lập bảng kê thay thế/điều chỉnh) | NBKCLQuan |  | Ngày | Bắt buộc (Đối với trường hợp điều chỉnh, thay thế cho nhiều hóa đơn) | Khoản 13 điều 1 Nghị định 70 |

### HDon\DLHDon\NDHDon\NBan - Thông tin người bán

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Tên | Ten | 400 | Chuỗi ký tự | Bắt buộc |  |
| Mã số thuế | MST | 14 | Chuỗi ký tự | Bắt buộc |  |
| Địa chỉ | DChi | 400 | Chuỗi ký tự | Bắt buộc |  |
| Mã cửa hàng | MCHang | 50 | Chuỗi ký tự | Không bắt buộc |  |
| Tên cửa hàng | TCHang | 400 | Chuỗi ký tự | Không bắt buộc |  |
| Số điện thoại | SDThoai | 20 | Chuỗi ký tự | Không bắt buộc |  |
| Địa chỉ thư điện tử | DCTDTu | 50 | Chuỗi ký tự | Không bắt buộc |  |
| Số tài khoản ngân hàng | STKNHang | 30 | Chuỗi ký tự | Không bắt buộc |  |
| Tên ngân hàng | TNHang | 400 | Chuỗi ký tự | Không bắt buộc |  |
| Fax | Fax | 20 | Chuỗi ký tự | Không bắt buộc |  |
| Website | Website | 100 | Chuỗi ký tự | Không bắt buộc |  |

### HDon\DLHDon\NDHDon\NMua - Thông tin người mua

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Tên | Ten | 400 | Chuỗi ký tự | Bắt buộc (Nếu có) | Khoản 5, Điều 10, Nghị định 123 |
| Mã số thuế | MST | 14 | Chuỗi ký tự | Bắt buộc (Nếu có) | Khoản 5, Điều 10, Nghị định 123 |
| Mã đơn vị quan hệ ngân sách (Mã số đơn vị có quan hệ với ngân sách) | MDVQHNSach | 7 | Chuỗi ký tự | Bắt buộc (nếu có) | Khoản 7 điều 1 Nghị định 70 |
| Địa chỉ | DChi | 400 | Chuỗi ký tự | Bắt buộc (Nếu có) | Khoản 5, Điều 10, Nghị định 123 |
| Mã khách hàng | MKHang | 50 | Chuỗi ký tự | Không bắt buộc |  |
| Số điện thoại | SDThoai | 20 | Chuỗi ký tự | Không bắt buộc |  |
| Căn cước công dân (Số CC/CCCD/số định danh) | CCCDan | 12 | Chuỗi ký tự | Bắt buộc (Nếu có) | Khoản 7, Điều 1, Nghị định 70 |
| Số hộ chiếu (Số hộ chiếu/Giấy tờ nhập xuất cảnh) | SHChieu | 20 | Chuỗi ký tự | Bắt buộc (Nếu có) | Khoản 7, Điều 1, Nghị định 70 |
| Địa chỉ thư điện tử | DCTDTu | 50 | Chuỗi ký tự | Không bắt buộc |  |
| Họ và tên người mua hàng | HVTNMHang | 100 | Chuỗi ký tự | Không bắt buộc |  |
| Số tài khoản ngân hàng | STKNHang | 30 | Chuỗi ký tự | Không bắt buộc |  |
| Tên ngân hàng | TNHang | 400 | Chuỗi ký tự | Không bắt buộc |  |

### HDon\DLHDon\NDHDon\DSHHDVu\HHDVu - Chi tiết hàng hóa, dịch vụ

Thẻ `HHDVu` có thể lặp lại nhiều lần tương ứng với số lượng hàng hóa, dịch vụ trên hóa đơn.

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Tính chất | TChat | 1 | Số (Chi tiết tại Phụ lục IV Quy định này) | Bắt buộc |  |
| Số thứ tự | STT | 4 | Số | Không bắt buộc |  |
| Mã hàng hóa, dịch vụ | MHHDVu | 50 | Chuỗi ký tự | Bắt buộc (Nếu có) | Điểm a, khoản 6, Điều 10 Nghị định 123 |
| Tên hàng hóa, dịch vụ | THHDVu | 500 | Chuỗi ký tự | Bắt buộc |  |
| Đơn vị tính | DVTinh | 50 | Chuỗi ký tự | Bắt buộc (Nếu có) | Khoản 6, khoản 14, Điều 10 Nghị định 123 |
| Số lượng | SLuong | 21,6 | Số | Bắt buộc (Nếu có) | Khoản 6, khoản 14, Điều 10 Nghị định 123 |
| Đơn giá | DGia | 21,6 | Số | Bắt buộc (Nếu có) | Khoản 6, khoản 14, Điều 10 Nghị định 123 |
| Tỷ lệ % chiết khấu (Trong trường hợp thể hiện thông tin chiết khấu cho từng hàng hóa, dịch vụ) | TLCKhau | 6,4 | Số | Không bắt buộc |  |
| Số tiền chiết khấu (Trong trường hợp thể hiện thông tin chiết khấu cho từng hàng hóa, dịch vụ) | STCKhau | 21,6 | Số | Không bắt buộc |  |
| Thành tiền (Thành tiền chưa có thuế GTGT) | ThTien | 21,6 | Số | Bắt buộc (Trừ trường hợp TChat có giá trị là “4-Ghi chú/diễn giải”) |  |
| Thuế suất (Thuế suất thuế GTGT) | TSuat | 11 | Chuỗi ký tự (Chi tiết tại Phụ lục V Quy định này) | Bắt buộc (Nếu có) | Khoản 6, khoản 14, Điều 10 Nghị định 123 |

### HDon\DLHDon\NDHDon\DSHHDVu\HHDVu\TTHHDTrung\TTin - Thông tin hàng hóa đặc trưng

Nhóm này bắt buộc nếu thẻ `TChat` có giá trị là `5`.

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Loại hàng hóa đặc trưng | LHHDTrung | 1 | Số (Chi tiết Phụ lục XV, Quy định này) | Bắt buộc (Nếu có) | Điểm b khoản 7 điều 1 Nghị định 70 |
| Tên trường | TTruong | 20 | Chuỗi ký tự (Chi tiết Cột “Giá trị bắt buộc” tại Phụ lục XV Quy định này) | Bắt buộc |  |
| Dữ liệu | DLieu | 200 |  | Bắt buộc |  |

### HDon\DLHDon\NDHDon\TToan\THTTLTSuat\LTSuat - Tổng hợp theo từng loại thuế suất

Thẻ này có thể lặp lại nhiều lần tương ứng với số lượng mức thuế suất khác nhau.

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Thuế suất (Thuế suất thuế GTGT) | TSuat | 11 | Chuỗi ký tự (Chi tiết tại Phụ lục V Quy định này) | Bắt buộc (Nếu có) | Khoản 6, khoản 14, Điều 10 Nghị định 123 |
| Thành tiền (Thành tiền chưa có thuế GTGT) | ThTien | 21,6 | Số | Bắt buộc |  |
| Tiền thuế (Tiền thuế GTGT) | TThue | 21,6 | Số | Bắt buộc (Nếu có) | Khoản 6, khoản 14, Điều 10 Nghị định 123 |

### HDon\DLHDon\NDHDon\TToan - Thông tin thanh toán

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Tổng tiền chưa thuế (Tổng cộng thành tiền chưa có thuế GTGT) | TgTCThue | 21,6 | Số | Bắt buộc |  |
| Tổng giảm trừ không chịu thuế | TGTKCThue | 21,6 | Số | Không bắt buộc |  |
| Tổng tiền thuế (Tổng cộng tiền thuế GTGT) | TgTThue | 21,6 | Số | Bắt buộc |  |

### HDon\DLHDon\NDHDon\TToan\DSLPhi\LPhi - Chi tiết tiền phí, lệ phí

Thẻ này có thể lặp lại nhiều lần tương ứng với số loại phí, lệ phí.

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Tên loại phí | TLPhi | 100 | Chuỗi ký tự | Bắt buộc (Nếu có) | Khoản 11, Điều 10, Nghị định 123 |
| Tiền phí | TPhi | 21,6 | Số | Bắt buộc (Nếu có) | Khoản 11, Điều 10, Nghị định 123 |

### Các chỉ tiêu đặt trong HDon\DLHDon\NDHDon\TToan

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Tổng tiền chiết khấu thương mại | TTCKTMai | 21,6 | Số | Bắt buộc (Nếu có) | Khoản 6, Điều 10 Nghị định 123 |
| Tổng giảm trừ khác | TGTKhac | 21,6 | Số | Không bắt buộc |  |
| Tổng tiền thanh toán bằng số | TgTTTBSo | 21,6 | Số | Bắt buộc |  |
| Tổng tiền thanh toán bằng chữ | TgTTTBChu | 255 | Chuỗi ký tự | Bắt buộc |  |

### HDon\DLQRCode - Dữ liệu QR Code

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Dữ liệu QR Code (Chi tiết tại Khoản 7, Mục IV, Phần I, Quyết định số này) |  | 512 | Chuỗi ký tự | Không bắt buộc |  |

### HDon\MCCQT - Mã của cơ quan thuế

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Mã của cơ quan thuế (Mã của cơ quan thuế trên hóa đơn điện tử) | MCCQT | 34 | Chuỗi ký tự | Bắt buộc |  |

### HDon\DSCKS - Thông tin chữ ký số

Với hóa đơn điện tử đủ điều kiện cấp mã, hệ thống cơ quan thuế bổ sung chữ ký số cơ quan thuế trong thẻ `HDon\DSCKS\CQT`.

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Chữ ký số người bán | Signature |  |  | Bắt buộc (Nếu có) | Khoản 7, khoản 14, Điều 10, Nghị định 123 |
| Chữ ký số người mua | Signature |  |  | Không bắt buộc |  |
| Chữ ký số cơ quan thuế | Signature |  |  | Bắt buộc |  |

### Các thẻ thông tin khác

- `HDon\DLHDon\TTChung\TTKhac`: Chứa thông tin khác theo mô tả tại Khoản 1, Mục II, Phần II Quy định này.
- `HDon\DLHDon\NDHDon\NBan\TTKhac`: Chứa thông tin khác của người bán.
- `HDon\DLHDon\NDHDon\NMua\TTKhac`: Chứa thông tin khác của người mua.
- `HDon\DLHDon\NDHDon\DSHHDVu\HHDVu\TTKhac`: Chứa thông tin khác của từng hàng hóa/dịch vụ.
- `HDon\DLHDon\NDHDon\TToan\TTKhac`: Chứa thông tin khác trong phần thanh toán.
- `HDon\DLHDon\TTKhac`: Chứa thông tin khác ở cấp dữ liệu hóa đơn.
- `HDon\DSCKS\CCKSKhac`: Chứa các chữ ký số khác nếu có.

### Ghi chú sử dụng

- Cột **Độ dài tối đa** để trống nghĩa là tài liệu gốc không nêu rõ độ dài.
- Các trường có ràng buộc **Bắt buộc (Nếu có)** chỉ cần khai báo khi phát sinh dữ liệu tương ứng.
- Khi triển khai sinh XML hóa đơn, cần đảm bảo đúng tên thẻ, kiểu dữ liệu, độ dài và điều kiện bắt buộc theo từng trường hợp nghiệp vụ.

### Từ `cau_truc_tkhai_theo_xml_mau.md`

> Tài liệu này chỉ giữ lại các thẻ đang xuất hiện trong XML mẫu. Các cụm thẻ không có trong XML mẫu như `TTDVHTPT`, `TTTNSDung`, `TTDKTH`, `CCKSKhac` đã được loại bỏ.

### 1. Tổng quan cấu trúc XML

XML có thẻ gốc là `TKhai`, bên trong gồm 2 nhóm chính:

- `DLTKhai`: chứa dữ liệu tờ khai.
- `DSCKS`: chứa thông tin chữ ký số.

Cấu trúc thực tế theo XML mẫu:

```text
TKhai
├── DLTKhai @Id
│   ├── TTChung
│   └── NDTKhai
│       ├── HTHDon
│       ├── HTGDLHDDT
│       ├── PThuc
│       ├── LHDSDung
│       ├── DSCTSSDung
│       │   └── CTS
│       ├── TTTCGP
│       │   └── TCGP
│       └── TTTCTN
│           └── TCTN
└── DSCKS
    └── NNT
        └── Signature
```

### 2. Ghi chú chung

- Các trường có giá trị `0/1` thường được hiểu là:
  - `0`: không áp dụng hoặc không sử dụng.
  - `1`: áp dụng hoặc sử dụng.
- Các thẻ tự đóng như `<TTCGP/>`, `<MSTTCGP/>`, `<TNgay/>` vẫn được giữ lại vì có xuất hiện trong XML mẫu.
- Một số tên thẻ trong XML mẫu khác với tên trong bảng quy chuẩn ban đầu. Tài liệu này ưu tiên giữ đúng tên thẻ đang có trong XML mẫu, ví dụ: `HDGTGT_BienLai`, `HDBHang_BienLai`, `HDThuongMai`.

### 3. Thuộc tính của thẻ `DLTKhai`

| Thuộc tính | Giá trị trong XML mẫu | Ý nghĩa |
|---|---|---|
| `Id` | `6213B5FD47054009894D7C7635CD7691` | Mã định danh của phần dữ liệu tờ khai. |

### 4. `TKhai\DLTKhai\TTChung` — Thông tin chung của tờ khai

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| Phiên bản của thông điệp | `PBan` | `2.1.0` | 6 | Chuỗi ký tự | Bắt buộc |
| Mẫu số tờ khai | `MSo` | `01/ĐKTĐ-HĐĐT` | 15 | Chuỗi ký tự | Bắt buộc |
| Tên tờ khai | `Ten` | `Tờ khai đăng ký/thay đổi thông tin sử dụng hóa đơn điện tử` | 100 | Chuỗi ký tự | Bắt buộc |
| Hình thức đăng ký/thay đổi | `HThuc` | `1` | 1 | Số: `1` đăng ký mới, `2` thay đổi thông tin | Bắt buộc |
| Tên người nộp thuế | `TNNT` | `Công ty ABC` | 400 | Chuỗi ký tự | Bắt buộc |
| Mã số thuế | `MST` | `0102030405` | 14 | Chuỗi ký tự | Bắt buộc |
| Cơ quan thuế quản lý | `CQTQLy` | `Chi cục Thuế Quận 1` | 100 | Chuỗi ký tự | Bắt buộc |
| Mã cơ quan thuế quản lý | `MCQTQLy` | `202` | 5 | Chuỗi ký tự | Bắt buộc |
| Tên người đại diện pháp luật | `TNDDPLuat` | `A` | 50 | Chuỗi ký tự | Bắt buộc |
| Điện thoại người đại diện pháp luật | `DTDDPLuat` | `1` | 20 | Chuỗi ký tự | Bắt buộc |
| Căn cước công dân/số định danh | `CCCDan` | `1` | 12 | Chuỗi ký tự | Bắt buộc |
| Số hộ chiếu | `SHChieu` | `1` | 20 | Chuỗi ký tự | Bắt buộc nếu có |
| Ngày sinh đại diện pháp luật | `NSDDPLuat` | `1978-12-07` |  | Ngày | Bắt buộc |
| Giới tính | `GTinh` | `1` | 1 | Số: `0` nữ, `1` nam | Bắt buộc |
| Địa chỉ liên hệ | `DCLHe` | `135 Nam Kỳ Khởi Nghĩa, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh` | 400 | Chuỗi ký tự | Bắt buộc |
| Địa chỉ thư điện tử | `DCTDTu` | `root@hoadon.com` | 50 | Chuỗi ký tự | Bắt buộc |
| Người liên hệ | `NLHe` | `A` | 50 | Chuỗi ký tự | Không bắt buộc |
| Điện thoại liên hệ | `DTLHe` | `1` | 20 | Chuỗi ký tự | Không bắt buộc |
| Địa danh | `DDanh` | `2` | 50 | Chuỗi ký tự | Bắt buộc |
| Ngày lập | `NLap` | `2026-06-14` |  | Ngày | Bắt buộc |

### 5. `TKhai\DLTKhai\NDTKhai\HTHDon` — Hình thức hóa đơn áp dụng

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| Hóa đơn có mã | `CMa` | `1` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| Hóa đơn không có mã | `KCMa` | `1` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| Hóa đơn có mã khởi tạo từ máy tính tiền | `CMTMTTien` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |

### 6. `TKhai\DLTKhai\NDTKhai\HTGDLHDDT` — Hình thức gửi dữ liệu hóa đơn điện tử

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| NNT địa bàn khó khăn | `NNTDBKKhan` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| NNT khác theo đề nghị UBND | `NNTKTDNUBND` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| Cơ quan thuế hoặc cơ quan được giao nhiệm vụ xử lý tài sản công | `CQXLTSCong` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| Chuyển dữ liệu trực tiếp đến cơ quan thuế | `CDLTTDCQT` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| Chuyển dữ liệu qua tổ chức truyền nhận | `CDLQTCTN` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |

### 7. `TKhai\DLTKhai\NDTKhai\PThuc` — Phương thức chuyển dữ liệu hóa đơn điện tử

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| Chuyển đầy đủ nội dung từng hóa đơn | `CDDu` | `1` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| Chuyển theo bảng tổng hợp dữ liệu hóa đơn điện tử | `CBTHop` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |

### 8. `TKhai\DLTKhai\NDTKhai\LHDSDung` — Loại hóa đơn sử dụng

| Tên chỉ tiêu | Tên thẻ trong XML mẫu | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| Hóa đơn GTGT | `HDGTGT` | `1` | 1 | Số: `0` không sử dụng, `1` sử dụng | Bắt buộc |
| Hóa đơn GTGT tích hợp biên lai | `HDGTGT_BienLai` | `1` | 1 | Số: `0` không sử dụng, `1` sử dụng | Bắt buộc |
| Hóa đơn bán hàng | `HDBHang` | `1` | 1 | Số: `0` không sử dụng, `1` sử dụng | Bắt buộc |
| Hóa đơn bán hàng tích hợp biên lai | `HDBHang_BienLai` | `1` | 1 | Số: `0` không sử dụng, `1` sử dụng | Bắt buộc |
| Hóa đơn thương mại | `HDThuongMai` | `0` | 1 | Số: `0` không sử dụng, `1` sử dụng | Bắt buộc |
| Hóa đơn bán tài sản công | `HDBTSCong` | `0` | 1 | Số: `0` không sử dụng, `1` sử dụng | Bắt buộc |
| Hóa đơn bán hàng dự trữ quốc gia | `HDBHDTQGia` | `0` | 1 | Số: `0` không sử dụng, `1` sử dụng | Bắt buộc |
| Hóa đơn khác | `HDKhac` | `0` | 1 | Số: `0` không sử dụng, `1` sử dụng | Bắt buộc |
| Chứng từ | `CTu` | `0` | 1 | Số: `0` không sử dụng, `1` sử dụng | Bắt buộc |

### 9. `TKhai\DLTKhai\NDTKhai\DSCTSSDung\CTS` — Thông tin chứng thư số sử dụng

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| Số thứ tự | `STT` | `1` | 3 | Số | Không bắt buộc |
| Tên tổ chức chứng thực/cấp/công nhận chữ ký số | `TTChuc` | `54011258ee156590ac5a77b88545237e` | 400 | Chuỗi ký tự | Bắt buộc |
| Số sê-ri chứng thư số | `Seri` | `icacorp` | 40 | Chuỗi ký tự | Bắt buộc |
| Thời hạn sử dụng chứng thư số từ ngày | `TNgay` | `2026-06-14` |  | Ngày giờ | Bắt buộc |
| Thời hạn sử dụng chứng thư số đến ngày | `DNgay` | `2029-06-14` |  | Ngày giờ | Bắt buộc |
| Hình thức đăng ký chứng thư số | `HThuc` | `1` | 1 | Số: `1` thêm mới, `2` gia hạn, `3` ngừng sử dụng | Bắt buộc |

### 10. `TKhai\DLTKhai\NDTKhai\TTTCGP\TCGP` — Thông tin tổ chức cung cấp giải pháp hóa đơn điện tử

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| Số thứ tự | `STT` | `1` | 3 | Số | Không bắt buộc |
| Tên tổ chức cung cấp giải pháp hóa đơn điện tử | `TTCGP` | Rỗng | 400 | Chuỗi ký tự | Bắt buộc |
| Mã số thuế tổ chức cung cấp giải pháp hóa đơn điện tử | `MSTTCGP` | Rỗng | 14 | Chuỗi ký tự | Bắt buộc |
| Ngày bắt đầu sử dụng dịch vụ | `TNgay` | Rỗng |  | Ngày | Bắt buộc |
| Ngày kết thúc sử dụng dịch vụ | `DNgay` | Rỗng |  | Ngày | Không bắt buộc |
| Ghi chú | `GChu` | Rỗng | 255 | Chuỗi ký tự | Không bắt buộc |

### 11. `TKhai\DLTKhai\NDTKhai\TTTCTN\TCTN` — Thông tin tổ chức truyền nhận

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| Số thứ tự | `STT` | `1` | 3 | Số | Không bắt buộc |
| Tên tổ chức truyền nhận | `TTCTN` | Rỗng | 400 | Chuỗi ký tự | Bắt buộc |
| Mã số thuế tổ chức truyền nhận | `MSTTCTN` | Rỗng | 14 | Chuỗi ký tự | Bắt buộc |
| Ngày bắt đầu sử dụng dịch vụ | `TNgay` | Rỗng |  | Ngày | Bắt buộc |
| Ngày kết thúc sử dụng dịch vụ | `DNgay` | Rỗng |  | Ngày | Không bắt buộc |
| Ghi chú | `GChu` | Rỗng | 255 | Chuỗi ký tự | Không bắt buộc |

### 12. `TKhai\DSCKS\NNT\Signature` — Chữ ký số của người nộp thuế

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---|---|
| Tên người ký/người nộp thuế | `SignerName` | `Công ty ABC` | Chuỗi ký tự | Có trong XML mẫu |
| Mã số thuế người ký/người nộp thuế | `SignerTaxCode` | `0102030405` | Chuỗi ký tự | Có trong XML mẫu |
| Thời điểm ký | `SignedAt` | `2026-06-14T16:29:33` | Ngày giờ | Có trong XML mẫu |
| Giá trị chữ ký | `SignatureValue` | `SIMULATED-54E7DE452AB64CBF93EBE218105966C2` | Chuỗi ký tự | Có trong XML mẫu |

### 13. Các thẻ đã loại bỏ vì không có trong XML mẫu

Các cụm thẻ sau có trong tài liệu quy chuẩn ban đầu nhưng không xuất hiện trong XML mẫu nên đã được bỏ khỏi tài liệu này:

| Cụm thẻ bị loại bỏ | Lý do |
|---|---|
| `TKhai\DLTKhai\NDTKhai\TTDVHTPT\DVHTPT` | Không có trong XML mẫu. |
| `TKhai\DLTKhai\NDTKhai\TTTNSDung\TNSDung` | Không có trong XML mẫu. |
| `TKhai\DLTKhai\NDTKhai\TTDKTH\DKTH` | Không có trong XML mẫu. |
| `TKhai\DSCKS\CCKSKhac` | Không có trong XML mẫu. |
| `TKhai\DSCKS\NNT\Signature\Object` | Không có trong XML mẫu. |

### 14. Tóm tắt các thẻ có trong XML mẫu

```text
TKhai
DLTKhai
TTChung
PBan, MSo, Ten, HThuc, TNNT, MST, CQTQLy, MCQTQLy,
TNDDPLuat, DTDDPLuat, CCCDan, SHChieu, NSDDPLuat, GTinh,
DCLHe, DCTDTu, NLHe, DTLHe, DDanh, NLap

NDTKhai
HTHDon: CMa, KCMa, CMTMTTien
HTGDLHDDT: NNTDBKKhan, NNTKTDNUBND, CQXLTSCong, CDLTTDCQT, CDLQTCTN
PThuc: CDDu, CBTHop
LHDSDung: HDGTGT, HDGTGT_BienLai, HDBHang, HDBHang_BienLai, HDThuongMai,
          HDBTSCong, HDBHDTQGia, HDKhac, CTu
DSCTSSDung/CTS: STT, TTChuc, Seri, TNgay, DNgay, HThuc
TTTCGP/TCGP: STT, TTCGP, MSTTCGP, TNgay, DNgay, GChu
TTTCTN/TCTN: STT, TTCTN, MSTTCTN, TNgay, DNgay, GChu

DSCKS
NNT
Signature: SignerName, SignerTaxCode, SignedAt, SignatureValue
```
