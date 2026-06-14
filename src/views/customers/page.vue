<template>
  <div>
    <!-- Tiêu đề -->
    <Header />

    <div class="main-container">
      <!-- Thanh bên -->
      <Sidebar />

      <!-- Nội dung chính -->
      <div class="content">
        <router-view />
      </div>
    </div>

    <!-- Chân trang -->
    <Footer />
  </div>
</template>

<script>
import Header from '../components/header.vue';
import Sidebar from '../components/sidebar.vue';
import Footer from '../components/footer.vue';

export default {
  name: 'CustomerPage',
  components: {
    Header,
    Sidebar,
    Footer
  }
}
</script>

<style scoped>
.main-container {
  display: flex;
  min-height: calc(100vh - 80px); /* Giả sử header + footer cao ~80px */
}

.content {
  flex: 1;
  padding: 20px;
  background-color: #f5f6fa;
}
</style>
