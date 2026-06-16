<template>
  <div class="auth-shell">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'AuthPage'
}
</script>

<style scoped>
.auth-shell {
  min-height: 100vh;
  overflow: hidden;
}
</style>
