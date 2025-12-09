<template>
  <div class="dashboard">
    <h2>Thống kê hóa đơn</h2>

    <div class="stats-container">
      <div class="stat-card">
        <h3>{{ invoiceStats.total }}</h3>
        <p>Tổng số hóa đơn đã mua</p>
      </div>
      <div class="stat-card">
        <h3>{{ invoiceStats.used }}</h3>
        <p>Số hóa đơn đã sử dụng</p>
      </div>
      <div class="stat-card">
        <h3>{{ invoiceStats.remaining }}</h3>
        <p>Số hóa đơn còn lại</p>
      </div>
      <div class="stat-card">
        <h3>{{ invoiceStats.issuedThisYear }}</h3>
        <p>Số hóa đơn phát hành trong năm</p>
      </div>
      <div class="stat-card">
        <h3>{{ invoiceStats.valueThisYear | currency }}</h3>
        <p>Giá trị hóa đơn phát hành trong năm</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CustomerIndex',
  data() {
    return {
      invoiceStats: {
        total: 1500,
        used: 800,
        remaining: 700,
        issuedThisYear: 300,
        valueThisYear: 450000000 // giả lập giá trị VND
      }
    };
  },
  filters: {
    currency(value) {
      if (!value) return '0 VND';
      return value.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' });
    }
  }
}
</script>

<style scoped>
.dashboard {
  background: #fff;
  padding: 20px;
  border-radius: 6px;
}

.stats-container {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 20px;
}

.stat-card {
  flex: 1 1 200px;
  background: #00b894;
  color: #fff;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
}

.stat-card h3 {
  font-size: 2rem;
  margin-bottom: 8px;
}
</style>
