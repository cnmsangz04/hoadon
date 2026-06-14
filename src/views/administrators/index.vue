<template>
  <div class="administrators-index admin-page">
    <section class="admin-hero">
      <div class="hero-content">
        <h2>Quản trị</h2>
        <p>Chào mừng tới trang quản trị</p>
      </div>
    </section>
  </div>
</template>

<script>
export default {
  name: 'AdministratorsIndex'
};
</script>

<style scoped>
.administrators-index {
  padding: 20px;
  background: #f5f6fa;
  min-height: calc(100vh - 80px);
}

/* Hero */
.admin-hero {
  background: linear-gradient(90deg, #16a085 0%, #1abc9c 100%);
  color: #fff;
  padding: 22px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.admin-hero .hero-content h2 {
  margin: 0 0 6px 0;
  font-size: 1.6rem;
  font-weight: 600;
}
.admin-hero .hero-content p {
  margin: 0;
  opacity: 0.95;
}

/* Lưới thống kê */
.admin-stats {
  margin-top: 18px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.stat-card {
  background: #fff;
  padding: 18px;
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.04);
  text-align: center;
}
.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #2f3e46;
}
.stat-label {
  margin-top: 6px;
  color: #7a8b8e;
  font-size: 0.95rem;
}

/* Tương thích màn hình */
@media (max-width: 992px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 576px) {
  .administrators-index { padding: 12px; }
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .admin-hero .hero-content h2 { font-size: 1.3rem; }
}
</style>