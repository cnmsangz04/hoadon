<template>
  <footer class="footer">
    <div class="container">
      &copy; {{ new Date().getFullYear() }} Invoice - Hóa đơn điện tử.
    </div>
  </footer>
</template>

<script>
export default {
  name: 'Footer'
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
