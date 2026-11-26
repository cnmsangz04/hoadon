<template>
  <footer class="footer">
    <div class="container">
      &copy; {{ new Date().getFullYear() }} {{ $t('footer.copyright') }}
    </div>
  </footer>
</template>

<script>
export default {
  name: 'AppFooter'
}
</script>

<style scoped>
.footer {
  height: 40px;
  background: #2f3e46;
  color: #fff;
  text-align: center;
  line-height: 40px;
  font-size: 0.9rem;
}
</style>
