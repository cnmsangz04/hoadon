<template>
  <div class="container-fluid py-3 settings-home">
    <div class="page-head">
      <div>
        <h4>Cấu hình</h4>
        <p>Quản lý thông tin tài khoản, hồ sơ doanh nghiệp và bảo mật đăng nhập.</p>
      </div>
    </div>

    <div class="settings-grid">
      <router-link class="settings-tile" to="/setting/account/list">
        <i class="fas fa-user-cog"></i>
        <span>Tài khoản</span>
        <small>Cập nhật thông tin cá nhân và mật khẩu.</small>
      </router-link>
      <router-link class="settings-tile" to="/setting/profile/list">
        <i class="fas fa-id-card"></i>
        <span>Hồ sơ</span>
        <small>Thông tin doanh nghiệp và dữ liệu xuất hóa đơn.</small>
      </router-link>
      <router-link class="settings-tile" to="/setting/sessions/list">
        <i class="fas fa-desktop"></i>
        <span>Phiên đăng nhập</span>
        <small>Theo dõi các thiết bị đang đăng nhập.</small>
      </router-link>
      <router-link class="settings-tile" to="/setting/security/ip">
        <i class="fas fa-shield-alt"></i>
        <span>Bảo mật IP</span>
        <small>Giới hạn truy cập theo địa chỉ IP tin cậy.</small>
      </router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SettingsIndex'
}
</script>

<style scoped>
.settings-home {
  font-size: 13px;
}

.page-head {
  margin-bottom: 16px;
}

.page-head h4 {
  color: #1f2937;
  font-weight: 700;
  margin: 0 0 4px;
}

.page-head p {
  color: #64748b;
  margin: 0;
}

.settings-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.settings-tile {
  background: #fff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  color: #1f2937;
  display: grid;
  gap: 6px;
  padding: 16px;
  text-decoration: none;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.settings-tile:hover {
  border-color: #bfdbfe;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  text-decoration: none;
  transform: translateY(-2px);
}

.settings-tile i {
  color: #2563eb;
  font-size: 22px;
}

.settings-tile span {
  font-weight: 700;
}

.settings-tile small {
  color: #64748b;
  line-height: 1.4;
}
</style>
