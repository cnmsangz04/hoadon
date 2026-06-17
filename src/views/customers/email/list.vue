<template>
  <div class="container-fluid py-3 email-home">
    <div class="page-head">
      <div>
        <h4>Email</h4>
        <p>Cấu hình máy chủ gửi email và xem lịch sử gửi hóa đơn.</p>
      </div>
    </div>

    <div class="email-grid">
      <router-link class="email-tile" to="/email/mail-server">
        <i class="fas fa-server"></i>
        <span>Máy chủ mail</span>
        <small>Thiết lập SMTP dùng để gửi hóa đơn cho khách hàng.</small>
      </router-link>
      <router-link class="email-tile" to="/email/mail-history">
        <i class="fas fa-envelope-open-text"></i>
        <span>Lịch sử gửi mail</span>
        <small>Theo dõi trạng thái các email đã gửi từ hệ thống.</small>
      </router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CustomerEmailIndex'
}
</script>

<style scoped>
.email-home {
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

.email-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.email-tile {
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

.email-tile:hover {
  border-color: #bfdbfe;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  text-decoration: none;
  transform: translateY(-2px);
}

.email-tile i {
  color: #2563eb;
  font-size: 22px;
}

.email-tile span {
  font-weight: 700;
}

.email-tile small {
  color: #64748b;
  line-height: 1.4;
}
</style>
