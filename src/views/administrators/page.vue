<template>
  <div>
    <!-- Header -->
    <ComponentHeader />

    <div class="main-container">
      <!-- Sidebar -->
      <ComponentSidebarAdmin />

      <!-- Main Content -->
      <div class="content">
        <div class="page-inner">
          <router-view />
        </div>
      </div>
    </div>

    <!-- Footer -->
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