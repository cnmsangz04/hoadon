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
  },
  mounted () {
    window.$crisp = [];
    window.CRISP_WEBSITE_ID = 'b7a71c3f-2527-40e3-a16d-6b1913b4dedc';
    if (!document.getElementById('crisp-sdk')) {
      const script = document.createElement('script');
      script.id = 'crisp-sdk';
      script.src = 'https://client.crisp.chat/l.js';
      script.async = true;
      document.head.appendChild(script);
    }
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
