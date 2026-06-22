# Định dạng dữ liệu hóa đơn điện tử

## Hóa đơn giá trị gia tăng

> Tài liệu này tổng hợp các thẻ dữ liệu chính trong cấu trúc hóa đơn điện tử, bao gồm thông tin chung, người bán, người mua, hàng hóa dịch vụ, thanh toán, QR Code và chữ ký số.

## Cấu trúc tổng quát

- `HDon`: Chứa thông tin dữ liệu hóa đơn, dữ liệu QR Code và thông tin chữ ký số.
- `HDon\DLHDon`: Chứa thông tin chung, nội dung chi tiết hóa đơn và thông tin khác do người bán tự định nghĩa.
- `HDon\DLHDon\TTChung`: Chứa thông tin chung của hóa đơn.
- `HDon\DLHDon\NDHDon`: Chứa nội dung hóa đơn, bao gồm thông tin người bán, người mua, danh sách hàng hóa/dịch vụ và thông tin thanh toán.
- `HDon\DSCKS`: Chứa thông tin chữ ký số của người bán/đơn vị nhận ủy nhiệm, người mua, cơ quan thuế và các chữ ký số khác nếu có.

## HDon\DLHDon\TTChung - Thông tin chung của hóa đơn

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

## HDon\DLHDon\TTChung\TTHDLQuan - Thông tin hóa đơn liên quan

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

## HDon\DLHDon\NDHDon\NBan - Thông tin người bán

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

## HDon\DLHDon\NDHDon\NMua - Thông tin người mua

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

## HDon\DLHDon\NDHDon\DSHHDVu\HHDVu - Chi tiết hàng hóa, dịch vụ

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

## HDon\DLHDon\NDHDon\DSHHDVu\HHDVu\TTHHDTrung\TTin - Thông tin hàng hóa đặc trưng

Nhóm này bắt buộc nếu thẻ `TChat` có giá trị là `5`.

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Loại hàng hóa đặc trưng | LHHDTrung | 1 | Số (Chi tiết Phụ lục XV, Quy định này) | Bắt buộc (Nếu có) | Điểm b khoản 7 điều 1 Nghị định 70 |
| Tên trường | TTruong | 20 | Chuỗi ký tự (Chi tiết Cột “Giá trị bắt buộc” tại Phụ lục XV Quy định này) | Bắt buộc |  |
| Dữ liệu | DLieu | 200 |  | Bắt buộc |  |

## HDon\DLHDon\NDHDon\TToan\THTTLTSuat\LTSuat - Tổng hợp theo từng loại thuế suất

Thẻ này có thể lặp lại nhiều lần tương ứng với số lượng mức thuế suất khác nhau.

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Thuế suất (Thuế suất thuế GTGT) | TSuat | 11 | Chuỗi ký tự (Chi tiết tại Phụ lục V Quy định này) | Bắt buộc (Nếu có) | Khoản 6, khoản 14, Điều 10 Nghị định 123 |
| Thành tiền (Thành tiền chưa có thuế GTGT) | ThTien | 21,6 | Số | Bắt buộc |  |
| Tiền thuế (Tiền thuế GTGT) | TThue | 21,6 | Số | Bắt buộc (Nếu có) | Khoản 6, khoản 14, Điều 10 Nghị định 123 |

## HDon\DLHDon\NDHDon\TToan - Thông tin thanh toán

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Tổng tiền chưa thuế (Tổng cộng thành tiền chưa có thuế GTGT) | TgTCThue | 21,6 | Số | Bắt buộc |  |
| Tổng giảm trừ không chịu thuế | TGTKCThue | 21,6 | Số | Không bắt buộc |  |
| Tổng tiền thuế (Tổng cộng tiền thuế GTGT) | TgTThue | 21,6 | Số | Bắt buộc |  |

## HDon\DLHDon\NDHDon\TToan\DSLPhi\LPhi - Chi tiết tiền phí, lệ phí

Thẻ này có thể lặp lại nhiều lần tương ứng với số loại phí, lệ phí.

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Tên loại phí | TLPhi | 100 | Chuỗi ký tự | Bắt buộc (Nếu có) | Khoản 11, Điều 10, Nghị định 123 |
| Tiền phí | TPhi | 21,6 | Số | Bắt buộc (Nếu có) | Khoản 11, Điều 10, Nghị định 123 |

## Các chỉ tiêu đặt trong HDon\DLHDon\NDHDon\TToan

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Tổng tiền chiết khấu thương mại | TTCKTMai | 21,6 | Số | Bắt buộc (Nếu có) | Khoản 6, Điều 10 Nghị định 123 |
| Tổng giảm trừ khác | TGTKhac | 21,6 | Số | Không bắt buộc |  |
| Tổng tiền thanh toán bằng số | TgTTTBSo | 21,6 | Số | Bắt buộc |  |
| Tổng tiền thanh toán bằng chữ | TgTTTBChu | 255 | Chuỗi ký tự | Bắt buộc |  |

## HDon\DLQRCode - Dữ liệu QR Code

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Dữ liệu QR Code (Chi tiết tại Khoản 7, Mục IV, Phần I, Quyết định số này) |  | 512 | Chuỗi ký tự | Không bắt buộc |  |

## HDon\MCCQT - Mã của cơ quan thuế

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Mã của cơ quan thuế (Mã của cơ quan thuế trên hóa đơn điện tử) | MCCQT | 34 | Chuỗi ký tự | Bắt buộc |  |

## HDon\DSCKS - Thông tin chữ ký số

Với hóa đơn điện tử đủ điều kiện cấp mã, hệ thống cơ quan thuế bổ sung chữ ký số cơ quan thuế trong thẻ `HDon\DSCKS\CQT`.

| Tên chỉ tiêu | Tên thẻ | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc | Tham khảo |
| --- | --- | --- | --- | --- | --- |
| Chữ ký số người bán | Signature |  |  | Bắt buộc (Nếu có) | Khoản 7, khoản 14, Điều 10, Nghị định 123 |
| Chữ ký số người mua | Signature |  |  | Không bắt buộc |  |
| Chữ ký số cơ quan thuế | Signature |  |  | Bắt buộc |  |

## Các thẻ thông tin khác

- `HDon\DLHDon\TTChung\TTKhac`: Chứa thông tin khác theo mô tả tại Khoản 1, Mục II, Phần II Quy định này.
- `HDon\DLHDon\NDHDon\NBan\TTKhac`: Chứa thông tin khác của người bán.
- `HDon\DLHDon\NDHDon\NMua\TTKhac`: Chứa thông tin khác của người mua.
- `HDon\DLHDon\NDHDon\DSHHDVu\HHDVu\TTKhac`: Chứa thông tin khác của từng hàng hóa/dịch vụ.
- `HDon\DLHDon\NDHDon\TToan\TTKhac`: Chứa thông tin khác trong phần thanh toán.
- `HDon\DLHDon\TTKhac`: Chứa thông tin khác ở cấp dữ liệu hóa đơn.
- `HDon\DSCKS\CCKSKhac`: Chứa các chữ ký số khác nếu có.

## Ghi chú sử dụng

- Cột **Độ dài tối đa** để trống nghĩa là tài liệu gốc không nêu rõ độ dài.
- Các trường có ràng buộc **Bắt buộc (Nếu có)** chỉ cần khai báo khi phát sinh dữ liệu tương ứng.
- Khi triển khai sinh XML hóa đơn, cần đảm bảo đúng tên thẻ, kiểu dữ liệu, độ dài và điều kiện bắt buộc theo từng trường hợp nghiệp vụ.
