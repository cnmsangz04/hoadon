# Deploy production

Script deploy được lưu tại [`scripts/hoadon-deploy`](scripts/hoadon-deploy) và được cài trên VPS ở:

```text
/usr/local/sbin/hoadon-deploy
```

Các đường dẫn production hiện tại:

| Thành phần | Đường dẫn / tên |
| --- | --- |
| Source | `/opt/apps/hoadon` |
| JAR đang chạy | `/opt/hoadon-prod/app.jar` |
| Service | `hoadon` |
| Health-check nội bộ | `http://127.0.0.1:8081/` |
| JAR backup | `/var/backups/hoadon/jars` |
| Clone database backup | `/opt/apps/hoadon_db` |

## Cài hoặc cập nhật script trên VPS

Từ thư mục repository trên máy local:

```bash
scp scripts/hoadon-deploy root@112.213.88.134:/usr/local/sbin/hoadon-deploy
ssh root@112.213.88.134 'chmod 755 /usr/local/sbin/hoadon-deploy'
```

Script deploy database được cài riêng:

```bash
scp scripts/hoadon-db-deploy root@112.213.88.134:/usr/local/sbin/hoadon-db-deploy
ssh root@112.213.88.134 'chmod 755 /usr/local/sbin/hoadon-db-deploy'
```

## Chạy deploy source

```bash
sudo /usr/local/sbin/hoadon-deploy
```

Thành công khi lệnh trả về `Deploy thành công commit ...` và health-check
không báo lỗi. Nếu lệnh thất bại, xem log bằng:

```bash
sudo systemctl status hoadon --no-pager
sudo journalctl -u hoadon -n 100 --no-pager
```

Script thực hiện các bước sau:

1. Khóa chống hai tiến trình deploy chạy đồng thời.
2. Kiểm tra source không có thay đổi tracked ngoài output build.
3. Fetch và fast-forward nhánh `main` qua deploy key SSH chỉ đọc.
4. Build frontend, Spring Boot và cài Chromium đúng phiên bản Playwright.
5. Thay JAR theo kiểu atomic và restart dịch vụ `hoadon`.
6. Chờ health-check HTTP thành công; tự rollback JAR nếu bản mới không khởi động.
7. Chỉ giữ ba JAR backup gần nhất trong `/var/backups/hoadon/jars`.
8. Khôi phục output frontend trong source và xóa thư mục `target` sau deploy.

## Git remote trên VPS

Source production sử dụng host alias và deploy key chỉ đọc:

```text
git@github-hoadon:cnmsangz04/hoadon.git
```

Repository database sử dụng key riêng:

```text
git@github-hoadon-db:cnmsangz04/hoadon_db.git
```

Clone database được giữ tại `/opt/apps/hoadon_db` để có thể fetch/pull bản
backup mới khi chạy quy trình restore riêng.

Không bật quyền ghi cho deploy key và không lưu GitHub PAT trong remote URL.

## Database

Deploy source thông thường **không restore hoặc thay đổi database trực tiếp**.

Để chủ động thay toàn bộ database production bằng `hoadon_database.bak` mới
nhất từ repository private, chạy:

```bash
sudo /usr/local/sbin/hoadon-db-deploy --check
```

Lệnh `--check` chỉ cập nhật clone Git, kiểm tra kết nối SQL Server và xác minh
file backup; nó không backup, dừng ứng dụng hoặc thay database. Sau khi kiểm tra
thành công, thực hiện restore thật bằng:

```bash
sudo /usr/local/sbin/hoadon-db-deploy
```

Đây là thao tác phá hủy dữ liệu production hiện tại. Script thực hiện:

1. Fetch và fast-forward repository `/opt/apps/hoadon_db` qua deploy key chỉ đọc.
2. Chạy `RESTORE VERIFYONLY` và kiểm tra logical file của backup từ Git.
3. Backup database hiện tại với `CHECKSUM` và kiểm tra backup rollback.
4. Dừng ứng dụng rồi restore database với đúng data/log file hiện tại.
5. Khởi động ứng dụng và chạy health-check HTTP.
6. Nếu restore hoặc health-check lỗi, tự thử phục hồi database cũ.
7. Giữ ba backup rollback gần nhất tại `/var/opt/mssql/backup/hoadon/rollback`.

Không đưa lệnh restore database vào script deploy hằng ngày.
