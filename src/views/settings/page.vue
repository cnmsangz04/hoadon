<template>
  <div>
    <ComponentHeaderSetting />

    <div class="main-container">
      <ComponentSidebarSetting />

      <div class="content">
        <router-view ref="pageView" :key="routeRefreshKey + ':' + $route.fullPath" />
      </div>
    </div>

    <ComponentFooterSetting />
  </div>
</template>

<script>
import ComponentHeaderSetting from '../components/header.vue'
import ComponentSidebarSetting from '../components/sidebar_setting.vue'
import ComponentFooterSetting from '../components/footer.vue'

export default {
  name: 'SettingPage',
  components: {
    ComponentHeaderSetting,
    ComponentSidebarSetting,
    ComponentFooterSetting
  },
  data () {
    return {
      routeRefreshKey: 0
    }
  },
  mounted () {
    window.addEventListener('app-force-route-refresh', this.forceRouteRefresh)
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
  min-height: calc(100vh - 80px);
}

.content {
  flex: 1;
  padding: 20px;
  background-color: #f5f6fa;
}
</style>
