<template>
  <div class="container-fluid py-3 members">
    <!-- Header and actions -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Danh sách thành viên</h4>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
        <b-button size="sm" variant="success" @click="openCreate">
          <i class="fas fa-user-plus"></i>
          Thêm thành viên
        </b-button>
      </div>
    </div>

    <!-- Filters -->
    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="4" class="mb-2">
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input v-model.trim="filters.keyword" placeholder="Tìm theo tên / email / username" @keyup.enter="applyFilters" />
          </b-input-group>
        </b-col>
        <b-col md="3" class="mb-2">
          <!-- Role filter now based on users.role -->
          <b-form-select v-model="filters.userRole" :options="userRoleFilterOptions">
            <template #first>
              <b-form-select-option :value="null">Tất cả vai trò</b-form-select-option>
            </template>
          </b-form-select>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-select v-model="filters.status" :options="statusOptions">
            <template #first>
              <b-form-select-option :value="null">Tất cả trạng thái</b-form-select-option>
            </template>
          </b-form-select>
        </b-col>
        <b-col md="2" class="text-right">
          <b-button size="sm" variant="primary" @click="applyFilters">Lọc</b-button>
        </b-col>
      </b-row>
    </b-card>

    <!-- Members table -->
    <b-card class="shadow-sm">
      <b-table
        bordered
        hover
        responsive
        small
        show-empty
        :items="items"
        :fields="fields"
        :busy="isBusy"
        empty-text="Không có dữ liệu"
      >
        <template #cell(index)="{ index }">
          {{ index + 1 + (page.current - 1) * page.size }}
        </template>

        <template #cell(user)="{ item }">
          <div class="d-flex align-items-center">
            <div class="avatar mr-2" v-if="item.avatarUrl">
              <img :src="item.avatarUrl" alt="avatar" />
            </div>
            <div>
              <div class="font-weight-bold">{{ item.fullName || item.name || item.username }}</div>
              <div class="text-muted small">{{ item.email || item.username }}</div>
            </div>
          </div>
        </template>

        <!-- New column: Họ và Tên from users.name -->
        <template #cell(fullName)="{ item }">
          {{ (item.user && item.user.name) || item.name || item.fullName || '—' }}
        </template>

        <!-- Vai trò now mapped from user.role (0: Root; 1: Quản trị; 2: Nhân viên) -->
        <template #cell(role)="{ item }">
          <b-badge variant="info" class="role-pill">{{ mainRoleText(item) }}</b-badge>
        </template>

        <template #cell(status)="{ item }">
          <b-badge :variant="item.locked ? 'secondary' : 'success'">{{ item.locked ? 'Lock' : 'Active' }}</b-badge>
        </template>

        <!-- Removed Quyền chính column -->

        <template #cell(option)="{ item }">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret>
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="openEdit(item)">Chỉnh sửa</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="toggleLock(item)">
              <span :class="item.locked ? 'text-success' : 'text-warning'">{{ item.locked ? 'Mở khóa' : 'Khóa' }}</span>
            </b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="resetPassword(item)">Reset mật khẩu</b-dropdown-item>
            <b-dropdown-divider />
            <b-dropdown-item class="text-center" href="#" @click.prevent="removeFromCompany(item)">
              <span class="text-danger">Xóa / rời công ty</span>
            </b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <b-pagination
        v-if="page.total > page.size"
        v-model="page.current"
        :per-page="page.size"
        :total-rows="page.total"
        align="right"
        class="mt-2"
        @change="onPageChange"
      />
    </b-card>

    <!-- Create/Edit member modal -->
    <b-modal ref="memberModal" :title="form.id ? 'Chỉnh sửa thành viên' : 'Thêm thành viên'" hide-footer>
      <b-form @submit.prevent="saveMember">
        <b-form-group label="Email / Username">
          <b-form-input v-model.trim="form.username" :disabled="!!form.id" required />
        </b-form-group>
        <b-form-group label="Tên">
          <b-form-input v-model.trim="form.fullName" required />
        </b-form-group>
        <b-form-group label="Vai trò">
          <b-form-select v-model="form.roleId" :options="roleOptionsFiltered">
            <template #first>
              <b-form-select-option :value="null" disabled>Chọn vai trò</b-form-select-option>
            </template>
          </b-form-select>
          <small class="text-muted">Mặc định chỉ cần chọn vai trò</small>
        </b-form-group>
        <b-form-group label="(Nâng cao) Quyền riêng">
          <b-form-tags
            v-model="form.extraPermissionIds"
            :input-attrs="{ placeholder: 'Nhập/tìm tên quyền để cấp riêng' }"
            size="sm"
          />
          <small class="text-muted">Chỉ dùng khi cần—quyền riêng sẽ lưu vào user_permissions</small>
        </b-form-group>
        <div class="text-right">
          <b-button type="submit" variant="primary">Lưu</b-button>
          <b-button variant="secondary" @click="$refs.memberModal.hide()">Hủy</b-button>
        </div>
      </b-form>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'SettingsMemberList',
  data() {
    return {
      isBusy: false,
      items: [],
      roles: [],
      rolePermissions: {}, // map roleId -> permissions[]
      page: { current: 1, size: 10, total: 0 },
      // Replace roleId filter with userRole (0: Root; 1: Quản trị; 2: Nhân viên)
      filters: { keyword: '', userRole: null, status: null },
      statusOptions: [
        { value: 1, text: 'Active' },
        { value: 0, text: 'Lock' }
      ],
      fields: [
        { key: 'index', label: '#', thStyle: { width: '50px' } },
        { key: 'user', label: 'Thành viên' },
        { key: 'fullName', label: 'Họ và Tên', thStyle: { width: '220px' } },
        { key: 'role', label: 'Vai trò', thStyle: { width: '160px' } },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '100px' } },
        { key: 'option', label: 'Chức năng', thStyle: { width: '120px' } }
      ],
      form: {
        id: null,
        username: '',
        fullName: '',
        roleId: null,
        extraPermissionIds: []
      }
    }
  },
  computed: {
    roleOptions() {
      return this.roles.map(r => ({ value: r.id, text: r.displayName || r.name }))
    },
    // không cho đổi users.role (root / admin)
    roleOptionsFiltered() {
      const blocked = new Set(['root', 'admin'])
      return this.roles
        .filter(r => !blocked.has(String(r.name).toLowerCase()))
        .map(r => ({ value: r.id, text: r.displayName || r.name }))
    },
    // Static filter options for users.role
    userRoleFilterOptions() {
      return [
        { value: 0, text: 'Root' },
        { value: 1, text: 'Quản trị' },
        { value: 2, text: 'Nhân viên' }
      ]
    }
  },
  mounted() {
    this.loadRoles()
    this.loadData()
  },
  methods: {
    roleName(item) {
      const id = item.roleId || item.role?.id || item.role
      const r = this.roles.find(x => x.id === id) || item.role
      return r?.displayName || r?.name || '—'
    },
    // Map role display text from user.role (0: Root; 1: Quản trị; 2: Nhân viên)
    mainRoleText(item) {
      // Prefer nested user.role when available
      let code = item?.user?.role
      if (code === undefined) {
        code = item?.role
      }
      if (code && typeof code === 'object') {
        code = code.role ?? code.code ?? code.id
      }
      const n = Number(code)
      if (Number.isNaN(n)) return '—'
      switch (n) {
        case 0: return 'Root'
        case 1: return 'Quản trị'
        case 2: return 'Nhân viên'
        default: return '—'
      }
    },
    mainPerms(item) {
      const id = item.roleId || item.role?.id || item.role
      const perms = this.rolePermissions[id] || []
      // hiển thị tối đa 3 quyền chính
      return perms.slice(0, 3)
    },
    async loadRoles() {
      // Assumption: admin roles endpoint
      const res = await axios.post('/administrator/roles/list', null, { params: { page: 0, size: 100 } })
      this.roles = (res.data?.content) || []
      // Preload role permissions summary
      for (const r of this.roles) {
        this.rolePermissions[r.id] = (r.permissions || []).filter(p => p.status === 1)
      }
    },
    async loadData() {
      this.isBusy = true
      try {
        const page = this.page.current - 1
        const res = await axios.post('/administrator/members/list', null, {
          params: {
            keyword: this.filters.keyword || undefined,
            // pass users.role filter only when selected
            userRole: this.filters.userRole ?? undefined,
            status: this.filters.status,
            page,
            size: this.page.size
          }
        })
        this.items = res.data?.content || []
        this.page.total = res.data?.totalElements || 0
      } finally {
        this.isBusy = false
      }
    },
    applyFilters() {
      this.page.current = 1
      this.loadData()
    },
    onPageChange(p) {
      this.page.current = p
      this.loadData()
    },
    reload() {
      this.applyFilters()
    },
    openCreate() {
      this.form = { id: null, username: '', fullName: '', roleId: null, extraPermissionIds: [] }
      this.$refs.memberModal.show()
    },
    openEdit(item) {
      this.form = {
        id: item.id,
        username: item.username || item.email,
        fullName: item.fullName || item.name,
        roleId: item.roleId || item.role?.id || item.role,
        extraPermissionIds: item.extraPermissionIds || []
      }
      this.$refs.memberModal.show()
    },
    async saveMember() {
      const payload = { ...this.form }
      // Assumption: saveOrUpdate endpoint
      await axios.post('/administrator/members/saveOrUpdate', payload)
      this.$refs.memberModal.hide()
      this.loadData()
    },
    async toggleLock(item) {
      // Assumption: lock/unlock endpoint
      await axios.post(`/administrator/members/${item.id}/lock`, null, { params: { lock: item.locked ? 0 : 1 } })
      this.loadData()
    },
    async resetPassword(item) {
      // Assumption: reset password endpoint
      await axios.post(`/administrator/members/${item.id}/reset-password`)
    },
    async removeFromCompany(item) {
      // Assumption: remove from company endpoint
      await axios.delete(`/administrator/members/${item.id}`)
      this.loadData()
    }
  }
}
</script>

<style scoped>
.members .card.shadow-sm { border-radius: 10px; }
.members .table-hover tbody tr:hover { background-color: #fafbfd; }
.members .btn-outline-primary { border-color: #dfe7ff; }
.members .btn-outline-primary:hover { background: #eef3ff; }
.members .table thead th { background: #f7f9fc; border-bottom: 1px solid #ecf0f6; color: #4a5568; font-weight: 700; }
.members .avatar img { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; }
.members .role-pill { font-weight: 600; }
.members .perm-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.members .chip { background: #f0f4ff; border: 1px solid #dfe7ff; color: #334155; border-radius: 999px; padding: 3px 8px; font-size: 12px; }
</style>