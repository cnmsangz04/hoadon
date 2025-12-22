<template>
  <div class="access-control">
    <h2>Quản lý phân quyền</h2>
    <ul>
      <li><router-link :to="{ name: 'admin-permissions-list' }">Quyền</router-link></li>
      <li><router-link :to="{ name: 'admin-permission-categories-list' }">Nhóm quyền</router-link></li>
      <li><router-link :to="{ name: 'admin-roles-list' }">Vai trò</router-link></li>
    </ul>
    <router-view />
  </div>
</template>

<script>
export default { name: 'AccessControlIndex' }
</script>

<style scoped>
.access-control ul { list-style: none; padding: 0; display: flex; gap: 12px; }
.access-control a { color: #1976d2; }
</style>
