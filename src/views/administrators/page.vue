<template>
  <div>
    <!-- Phần đầu trang -->
    <ComponentHeader />

    <div class="main-container">
      <!-- Thanh bên -->
      <ComponentSidebarAdmin />

      <!-- Nội dung chính -->
      <div class="content">
        <div class="page-inner">
          <router-view ref="pageView" :key="routeRefreshKey + ':' + $route.fullPath" />
        </div>
      </div>
    </div>

    <!-- Phần cuối trang -->
    <ComponentFooter />
  </div>
</template>


<script>
import ComponentHeader from '../components/header.vue';
import ComponentSidebarAdmin from '../components/sidebar_administrator.vue';
import ComponentFooter from '../components/footer.vue';

export default {
    name: "AdminPage",
    components: {
        ComponentHeader,
        ComponentSidebarAdmin,
		ComponentFooter,
    },
    data() {
        return {
            routeRefreshKey: 0
        }
    },
    mounted() {
        window.addEventListener('app-force-route-refresh', this.forceRouteRefresh)
    },
    beforeDestroy() {
        window.removeEventListener('app-force-route-refresh', this.forceRouteRefresh)
    },
    methods: {
        forceRouteRefresh() {
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
  padding: 20px;
  background-color: #f5f6fa;
}
</style>
