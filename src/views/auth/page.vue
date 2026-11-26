<template>
  <div class="auth-shell">
    <div class="language-control">
      <LanguageSwitcher />
    </div>
    <router-view />
  </div>
</template>

<script>
import LanguageSwitcher from '@/views/components/language_switcher.vue'

export default {
  name: 'AuthPage',
  components: { LanguageSwitcher },
}
</script>

<style scoped>
.auth-shell {
  min-height: 100vh;
  overflow: hidden;
}

.language-control {
  position: fixed;
  z-index: 1050;
  top: 16px;
  right: 16px;
}
</style>
