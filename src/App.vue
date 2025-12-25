<template>
  <div id="app" class="bg-light" style="min-height:100vh;">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
/* Import shared admin styles */
@import '@/assets/css/admin.css';
</style>