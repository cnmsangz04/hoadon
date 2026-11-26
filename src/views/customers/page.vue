<template>
  <div>
    <!-- Tiêu đề -->
    <AppHeader />

    <div class="main-container">
      <!-- Thanh bên -->
      <Sidebar />

      <!-- Nội dung chính -->
      <div class="content">
        <router-view ref="pageView" :key="routeRefreshKey + ':' + $route.fullPath" />
      </div>
    </div>

    <!-- Chân trang -->
    <AppFooter />
    <AiChatbot />
  </div>
</template>

<script>
import AppHeader from '../components/header.vue';
import Sidebar from '../components/sidebar.vue';
import AppFooter from '../components/footer.vue';
import AiChatbot from '../components/ai_chatbot.vue';

export default {
  name: 'CustomerPage',
  components: {
    AppHeader,
    Sidebar,
    AppFooter,
    AiChatbot
  },
  data () {
    return {
      routeRefreshKey: 0
    }
  },
  mounted () {
    window.addEventListener('app-force-route-refresh', this.forceRouteRefresh)
    window.$crisp = [];
    window.CRISP_WEBSITE_ID = 'b7a71c3f-2527-40e3-a16d-6b1913b4dedc';
    if (!document.getElementById('crisp-sdk')) {
      const script = document.createElement('script');
      script.id = 'crisp-sdk';
      script.src = 'https://client.crisp.chat/l.js';
      script.async = true;
      document.head.appendChild(script);
    }
  },
  beforeDestroy () {
    window.removeEventListener('app-force-route-refresh', this.forceRouteRefresh)
  },
  methods: {
    forceRouteRefresh () {
      this.routeRefreshKey += 1
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
  min-width: 0;
  padding: 20px;
  background-color: #f5f6fa;
  overflow-x: hidden;
}
</style>
