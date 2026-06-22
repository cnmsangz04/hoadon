# Cấu trúc dữ liệu tờ khai theo XML mẫu

> Tài liệu này chỉ giữ lại các thẻ đang xuất hiện trong XML mẫu. Các cụm thẻ không có trong XML mẫu như `TTDVHTPT`, `TTTNSDung`, `TTDKTH`, `CCKSKhac` đã được loại bỏ.

## 1. Tổng quan cấu trúc XML

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

## 2. Ghi chú chung

- Các trường có giá trị `0/1` thường được hiểu là:
  - `0`: không áp dụng hoặc không sử dụng.
  - `1`: áp dụng hoặc sử dụng.
- Các thẻ tự đóng như `<TTCGP/>`, `<MSTTCGP/>`, `<TNgay/>` vẫn được giữ lại vì có xuất hiện trong XML mẫu.
- Một số tên thẻ trong XML mẫu khác với tên trong bảng quy chuẩn ban đầu. Tài liệu này ưu tiên giữ đúng tên thẻ đang có trong XML mẫu, ví dụ: `HDGTGT_BienLai`, `HDBHang_BienLai`, `HDThuongMai`.

## 3. Thuộc tính của thẻ `DLTKhai`

| Thuộc tính | Giá trị trong XML mẫu | Ý nghĩa |
|---|---|---|
| `Id` | `6213B5FD47054009894D7C7635CD7691` | Mã định danh của phần dữ liệu tờ khai. |

## 4. `TKhai\DLTKhai\TTChung` — Thông tin chung của tờ khai

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

## 5. `TKhai\DLTKhai\NDTKhai\HTHDon` — Hình thức hóa đơn áp dụng

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| Hóa đơn có mã | `CMa` | `1` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| Hóa đơn không có mã | `KCMa` | `1` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| Hóa đơn có mã khởi tạo từ máy tính tiền | `CMTMTTien` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |

## 6. `TKhai\DLTKhai\NDTKhai\HTGDLHDDT` — Hình thức gửi dữ liệu hóa đơn điện tử

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| NNT địa bàn khó khăn | `NNTDBKKhan` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| NNT khác theo đề nghị UBND | `NNTKTDNUBND` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| Cơ quan thuế hoặc cơ quan được giao nhiệm vụ xử lý tài sản công | `CQXLTSCong` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| Chuyển dữ liệu trực tiếp đến cơ quan thuế | `CDLTTDCQT` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| Chuyển dữ liệu qua tổ chức truyền nhận | `CDLQTCTN` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |

## 7. `TKhai\DLTKhai\NDTKhai\PThuc` — Phương thức chuyển dữ liệu hóa đơn điện tử

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| Chuyển đầy đủ nội dung từng hóa đơn | `CDDu` | `1` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |
| Chuyển theo bảng tổng hợp dữ liệu hóa đơn điện tử | `CBTHop` | `0` | 1 | Số: `0` không áp dụng, `1` áp dụng | Bắt buộc |

## 8. `TKhai\DLTKhai\NDTKhai\LHDSDung` — Loại hóa đơn sử dụng

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

## 9. `TKhai\DLTKhai\NDTKhai\DSCTSSDung\CTS` — Thông tin chứng thư số sử dụng

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| Số thứ tự | `STT` | `1` | 3 | Số | Không bắt buộc |
| Tên tổ chức chứng thực/cấp/công nhận chữ ký số | `TTChuc` | `54011258ee156590ac5a77b88545237e` | 400 | Chuỗi ký tự | Bắt buộc |
| Số sê-ri chứng thư số | `Seri` | `icacorp` | 40 | Chuỗi ký tự | Bắt buộc |
| Thời hạn sử dụng chứng thư số từ ngày | `TNgay` | `2026-06-14` |  | Ngày giờ | Bắt buộc |
| Thời hạn sử dụng chứng thư số đến ngày | `DNgay` | `2029-06-14` |  | Ngày giờ | Bắt buộc |
| Hình thức đăng ký chứng thư số | `HThuc` | `1` | 1 | Số: `1` thêm mới, `2` gia hạn, `3` ngừng sử dụng | Bắt buộc |

## 10. `TKhai\DLTKhai\NDTKhai\TTTCGP\TCGP` — Thông tin tổ chức cung cấp giải pháp hóa đơn điện tử

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| Số thứ tự | `STT` | `1` | 3 | Số | Không bắt buộc |
| Tên tổ chức cung cấp giải pháp hóa đơn điện tử | `TTCGP` | Rỗng | 400 | Chuỗi ký tự | Bắt buộc |
| Mã số thuế tổ chức cung cấp giải pháp hóa đơn điện tử | `MSTTCGP` | Rỗng | 14 | Chuỗi ký tự | Bắt buộc |
| Ngày bắt đầu sử dụng dịch vụ | `TNgay` | Rỗng |  | Ngày | Bắt buộc |
| Ngày kết thúc sử dụng dịch vụ | `DNgay` | Rỗng |  | Ngày | Không bắt buộc |
| Ghi chú | `GChu` | Rỗng | 255 | Chuỗi ký tự | Không bắt buộc |

## 11. `TKhai\DLTKhai\NDTKhai\TTTCTN\TCTN` — Thông tin tổ chức truyền nhận

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Độ dài tối đa | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---:|---|---|
| Số thứ tự | `STT` | `1` | 3 | Số | Không bắt buộc |
| Tên tổ chức truyền nhận | `TTCTN` | Rỗng | 400 | Chuỗi ký tự | Bắt buộc |
| Mã số thuế tổ chức truyền nhận | `MSTTCTN` | Rỗng | 14 | Chuỗi ký tự | Bắt buộc |
| Ngày bắt đầu sử dụng dịch vụ | `TNgay` | Rỗng |  | Ngày | Bắt buộc |
| Ngày kết thúc sử dụng dịch vụ | `DNgay` | Rỗng |  | Ngày | Không bắt buộc |
| Ghi chú | `GChu` | Rỗng | 255 | Chuỗi ký tự | Không bắt buộc |

## 12. `TKhai\DSCKS\NNT\Signature` — Chữ ký số của người nộp thuế

| Tên chỉ tiêu | Tên thẻ | Giá trị trong XML mẫu | Kiểu dữ liệu | Ràng buộc |
|---|---|---|---|---|
| Tên người ký/người nộp thuế | `SignerName` | `Công ty ABC` | Chuỗi ký tự | Có trong XML mẫu |
| Mã số thuế người ký/người nộp thuế | `SignerTaxCode` | `0102030405` | Chuỗi ký tự | Có trong XML mẫu |
| Thời điểm ký | `SignedAt` | `2026-06-14T16:29:33` | Ngày giờ | Có trong XML mẫu |
| Giá trị chữ ký | `SignatureValue` | `SIMULATED-54E7DE452AB64CBF93EBE218105966C2` | Chuỗi ký tự | Có trong XML mẫu |

## 13. Các thẻ đã loại bỏ vì không có trong XML mẫu

Các cụm thẻ sau có trong tài liệu quy chuẩn ban đầu nhưng không xuất hiện trong XML mẫu nên đã được bỏ khỏi tài liệu này:

| Cụm thẻ bị loại bỏ | Lý do |
|---|---|
| `TKhai\DLTKhai\NDTKhai\TTDVHTPT\DVHTPT` | Không có trong XML mẫu. |
| `TKhai\DLTKhai\NDTKhai\TTTNSDung\TNSDung` | Không có trong XML mẫu. |
| `TKhai\DLTKhai\NDTKhai\TTDKTH\DKTH` | Không có trong XML mẫu. |
| `TKhai\DSCKS\CCKSKhac` | Không có trong XML mẫu. |
| `TKhai\DSCKS\NNT\Signature\Object` | Không có trong XML mẫu. |

## 14. Tóm tắt các thẻ có trong XML mẫu

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
