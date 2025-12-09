<template>
  <div class="auth-page container mt-5">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'AuthPage'
}
</script>

<style scoped>
.auth-page { max-width: 960px; }
</style>