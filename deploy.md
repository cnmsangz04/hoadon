# `/usr/local/sbin/hoadon-deploy`

File này ghi lại nội dung script deploy đang dùng trên VPS.

Khi có bổ sung hoặc thay đổi script, sửa nội dung bên dưới rồi copy lại vào VPS:

```bash
#!/usr/bin/env bash
set -Eeuo pipefail

source_dir="/opt/apps/hoadon"
app_jar="/opt/hoadon-prod/app.jar"
timestamp="$(date +%Y%m%d-%H%M%S)"
new_jar="/opt/hoadon-prod/app.jar.new"
backup_jar="/opt/hoadon-prod/app.jar.bak-${timestamp}"

cd "$source_dir"

restore_frontend_build_output() {
    git restore --source=HEAD --staged --worktree -- \
        src/main/resources/static/index.html \
        src/main/resources/static/js
    git clean -fd -- src/main/resources/static/js
}

restore_frontend_build_output
trap restore_frontend_build_output EXIT

git fetch origin
git pull --ff-only origin main

npm ci
npm run build

chmod +x mvnw
./mvnw clean package -DskipTests

built_jar="$(find target -maxdepth 1 -type f -name 'hoadon-*.jar' ! -name '*.original' | sort | tail -n 1)"

if [[ -z "$built_jar" ]]; then
    echo "Không tìm thấy file JAR sau khi build"
    exit 1
fi

cp "$app_jar" "$backup_jar"
cp "$built_jar" "$new_jar"
chmod 644 "$new_jar"
mv "$new_jar" "$app_jar"
```

## Copy script lên VPS

```bash
sudo nano /usr/local/sbin/hoadon-deploy
```

Dán lại phần script bên trên, lưu bằng `Ctrl+O`, nhấn `Enter`, thoát bằng `Ctrl+X`.

Sau đó cấp quyền chạy:

```bash
sudo chmod +x /usr/local/sbin/hoadon-deploy
```

## Dọn trạng thái build cũ lần đầu trên VPS

Nếu lần deploy trước đã tạo lỗi `index.html` hoặc các bundle untracked, chạy một lần:

```bash
cd /opt/apps/hoadon
git restore --source=HEAD --staged --worktree -- \
    src/main/resources/static/index.html \
    src/main/resources/static/js
git clean -fd -- src/main/resources/static/js
```

Chạy deploy:

```bash
sudo /usr/local/sbin/hoadon-deploy
```
