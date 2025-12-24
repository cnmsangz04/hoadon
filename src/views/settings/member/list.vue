<template>
  <div class="container-fluid py-3 members">
    <!-- Header and actions -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center">
        <h4 class="mb-0 font-weight-bold">Danh sách thành viên</h4>
      </div>
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

        <!-- Tài khoản -> users.username -->
        <template #cell(username)="{ item }">
          {{ item.username || item.user?.username || '—' }}
        </template>

        <!-- Họ và Tên -> users.name -->
        <template #cell(name)="{ item }">
          {{ item.name || item.user?.name || '—' }}
        </template>

        <!-- Email -> users.email -->
        <template #cell(email)="{ item }">
          {{ item.email || item.user?.email || '—' }}
        </template>

        <!-- Trạng thái -> users.status (1: Active, 0: Lock) -->
        <template #cell(status)="{ item }">
          <b-badge :variant="Number(item.status) === 1 ? 'success' : 'secondary'">
            {{ Number(item.status) === 1 ? 'Active' : 'Lock' }}
          </b-badge>
        </template>

        <!-- Cấp tài khoản -> users.role (chỉ xem) -->
        <template #cell(accountLevel)="{ item }">
          <b-badge variant="info" class="role-pill">{{ roleCodeToText(item) }}</b-badge>
        </template>

        <!-- Vai trò làm việc -> roles.display_name -->
        <template #cell(workRole)="{ item }">
          {{ workRoleDisplayName(item) }}
        </template>

        <template #cell(option)="{ item }">
          <b-dropdown v-if="canOperateOn(item)" size="sm" right variant="link" toggle-class="text-decoration-none" no-caret>
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="openEdit(item)">Cập nhật</b-dropdown-item>
            <b-dropdown-item v-if="canEditPermissions(item)" class="text-center" href="#" @click.prevent="openPermission(item)">Phân quyền</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="toggleLock(item)">
              <span :class="Number(item.status) !== 1 ? 'text-success' : 'text-warning'">
                {{ Number(item.status) !== 1 ? 'Mở khóa' : 'Khóa' }}
              </span>
            </b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="resetPassword(item)">Reset mật khẩu</b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <!-- Temporary password modal -->
      <b-modal ref="tempPwdModal" title="Mật khẩu tạm thời" hide-footer>
        <div class="mb-2">Đã reset mật khẩu. Vui lòng gửi mật khẩu tạm thời cho người dùng và yêu cầu đổi sau khi đăng nhập.</div>
        <b-input-group>
          <b-form-input :value="tempPassword" readonly></b-form-input>
          <b-input-group-append>
            <b-button size="sm" variant="outline-primary" @click="copyTempPassword">Sao chép</b-button>
          </b-input-group-append>
        </b-input-group>
        <div class="text-right mt-3">
          <b-button variant="primary" @click="$refs.tempPwdModal.hide()">Đóng</b-button>
        </div>
      </b-modal>

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
    <b-modal ref="memberModal" :title="form.id ? 'Cập nhật thành viên' : 'Thêm thành viên'" hide-footer>
      <b-form @submit.prevent="saveMember">
        <b-form-group label="Tài khoản">
          <b-form-input v-model.trim="form.username" :disabled="!!form.id" required />
        </b-form-group>
        <b-form-group label="Họ và tên">
          <b-form-input v-model.trim="form.fullName" required />
        </b-form-group>
        <b-form-group label="Điện thoại">
          <b-form-input v-model.trim="form.phone" />
        </b-form-group>
        <b-form-group label="Email">
          <b-form-input v-model.trim="form.email" type="email" />
        </b-form-group>
        <b-form-row>
          <b-col md="6">
            <b-form-group label="Mật khẩu" :state="passwordState">
              <b-input-group>
                <b-form-input v-model="form.password" type="password" :required="!form.id" />
                <b-input-group-append>
                  <b-button size="sm" variant="outline-secondary" @click="onGeneratePassword">Tạo</b-button>
                </b-input-group-append>
              </b-input-group>
            </b-form-group>
          </b-col>
          <b-col md="6">
            <b-form-group label="Nhập lại mật khẩu" :state="passwordState">
              <b-form-input v-model="form.passwordConfirm" type="password" :required="!form.id" />
              <small v-if="passwordState === false" class="text-danger">Mật khẩu không khớp</small>
            </b-form-group>
          </b-col>
        </b-form-row>
        <b-form-group v-if="!isEditingRootTarget" label="Vai trò làm việc">
          <b-form-select v-model="form.roleId" :options="roleOptionsLoad" @change="onWorkRoleChange">
            <template #first>
              <b-form-select-option :value="null" disabled>Chọn vai trò</b-form-select-option>
            </template>
          </b-form-select>
        </b-form-group>

        <!-- Removed override UI from member modal; manage in Phân quyền modal only -->

        <!-- Root-only: Set Admin -->
        <b-form-group v-if="isRoot && !isEditingRootTarget" label="Set user này là Admin">
          <b-form-checkbox v-model="form.isAdmin">Đặt làm Admin</b-form-checkbox>
        </b-form-group>
        <!-- Admin type selection required when Admin is checked -->
        <b-form-group v-if="isRoot && form.isAdmin && !isEditingRootTarget" label="Loại Admin">
          <div class="admin-type-cards">
            <div
              class="admin-type-card"
              :class="{ active: Number(form.adminType) === 1 }"
              role="button"
              tabindex="0"
              @click="form.adminType = 1"
              @keydown.enter.prevent="form.adminType = 1"
              @keydown.space.prevent="form.adminType = 1"
            >
              <div class="icon text-primary"><i class="fas fa-shield-alt"></i></div>
              <div>
                <div class="title">Admin hệ thống</div>
                <div class="desc">Yêu cầu mật khẩu quản trị</div>
              </div>
            </div>
            <div
              class="admin-type-card"
              :class="{ active: Number(form.adminType) === 2 }"
              role="button"
              tabindex="0"
              @click="form.adminType = 2"
              @keydown.enter.prevent="form.adminType = 2"
              @keydown.space.prevent="form.adminType = 2"
            >
              <div class="icon text-success"><i class="fas fa-building"></i></div>
              <div>
                <div class="title">Admin company</div>
                <div class="desc">Không cần mật khẩu quản trị</div>
              </div>
            </div>
          </div>
          <small v-if="form.isAdmin && !form.adminType" class="text-danger d-block mt-1">Vui lòng chọn loại Admin</small>
        </b-form-group>
        <!-- System admin requires admin password -->
        <b-form-group v-if="form.isAdmin && Number(form.adminType) === 1 && !isEditingRootTarget" label="Mật khẩu quản trị" :state="adminPasswordState">
          <b-input-group>
            <b-form-input v-model.trim="form.adminPassword" type="password" :required="form.isAdmin && Number(form.adminType) === 1" />
            <b-input-group-append>
              <b-button size="sm" variant="outline-secondary" @click="onGenerateAdminPassword">Tạo</b-button>
            </b-input-group-append>
          </b-input-group>
        </b-form-group>
        <b-form-group v-if="form.isAdmin && Number(form.adminType) === 1 && !isEditingRootTarget" label="Nhập lại mật khẩu quản trị" :state="adminPasswordState">
          <b-form-input v-model.trim="form.adminPasswordConfirm" type="password" :required="form.isAdmin && Number(form.adminType) === 1" />
          <small v-if="adminPasswordState === false" class="text-danger">Mật khẩu quản trị không khớp</small>
        </b-form-group>
        <!-- Note for editing root account -->
        <b-alert v-if="isEditingRootTarget" variant="info" class="mt-3">
          Bạn đang chỉnh sửa tài khoản <strong>Root</strong>. Vai trò và quyền hạn của tài khoản này không thể thay đổi.
        </b-alert>
        <div class="text-right">
          <b-button type="submit" variant="primary" :disabled="!canSubmitForm">Lưu</b-button>
          <b-button variant="secondary" @click="$refs.memberModal.hide()">Hủy</b-button>
        </div>
      </b-form>
    </b-modal>

    <!-- Permissions (Phân quyền) dedicated modal -->
    <b-modal
      ref="permissionModal"
      title="Phân quyền thành viên"
      hide-footer
      size="xl"
      content-class="role-modal-content"
      header-class="role-modal-header"
      body-class="role-modal-body"
    >
      <div>
        <b-form @submit.prevent="savePermissions">
          <b-card class="section-card mb-3">
            <b-row>
              <b-col cols="8" class="d-flex align-items-center">
                <div>
                  <div class="text-muted small">Vai trò làm việc</div>
                  <div class="font-weight-bold">{{ permRoleDisplayName }}</div>
                </div>
              </b-col>
              <b-col cols="4" class="d-flex align-items-end">
                <div class="ml-auto">
                  <b-button size="sm" class="mr-2" variant="outline-secondary" @click="permSelectAll(true)">Chọn tất cả</b-button>
                  <b-button size="sm" variant="outline-secondary" @click="permSelectAll(false)">Bỏ chọn</b-button>
                </div>
              </b-col>
            </b-row>
          </b-card>

          <b-card class="section-card">
            <div class="d-flex align-items-center mb-2">
              <h6 class="mb-0 mr-2"><i class="fas fa-key mr-1"></i> Phân quyền</h6>
              <b-badge variant="light" class="text-muted">Tích chọn để bật quyền, bỏ chọn để chặn quyền</b-badge>
            </div>
            <b-row>
              <b-col v-for="group in permGroupedPermissions" :key="group.categoryName" cols="12" md="6" lg="4" class="mb-3">
                <b-card class="perm-group h-100">
                  <div class="d-flex align-items-center mb-2 perm-group-header">
                    <strong>{{ group.categoryName }}</strong>
                    <b-button size="sm" class="ml-auto" variant="link" @click="togglePermGroup(group)">{{ group._allSelected ? 'Bỏ chọn nhóm' : 'Chọn nhóm' }}</b-button>
                  </div>
                  <div>
                    <div v-for="perm in group.items" :key="perm.id" class="perm-row mb-2">
                      <div class="perm-info">
                        <div class="perm-name">{{ perm.displayName || perm.name || ('#' + perm.id) }}</div>
                        <div class="perm-code">{{ perm.code || perm.key }}</div>
                        <b-badge variant="light">Level {{ perm.level }}</b-badge>
                      </div>
                      <div class="perm-actions d-flex align-items-center">
                        <b-form-checkbox
                          :checked="getPermEffectiveChecked(perm.id)"
                          switch
                          @change="onPermCheckboxChange(perm.id, $event)"
                        >
                          <!-- label hidden for compactness -->
                        </b-form-checkbox>
                      </div>
                    </div>
                  </div>
                </b-card>
              </b-col>
            </b-row>
          </b-card>

          <div class="text-right mt-3">
            <b-button type="submit" :disabled="!canSavePermissions" variant="primary">Lưu</b-button>
            <b-button variant="secondary" @click="$refs.permissionModal.hide()">Đóng</b-button>
          </div>
        </b-form>
      </div>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import { parseJwt } from '@/utils/jwt'

export default {
  name: 'SettingsMemberList',
  data() {
    return {
      isBusy: false,
      items: [],
      roles: [],
      rolePermissions: {},
      categories: [],
      page: { current: 1, size: 10, total: 0 },
      filters: { keyword: '', userRole: null, status: null },
      statusOptions: [
        { value: 1, text: 'Active' },
        { value: 0, text: 'Lock' }
      ],
      fields: [
        { key: 'index', label: '#', thStyle: { width: '50px' } },
        { key: 'username', label: 'Tài khoản', thStyle: { width: '180px' } },
        { key: 'name', label: 'Họ và Tên', thStyle: { width: '220px' } },
        { key: 'email', label: 'Email', thStyle: { width: '240px' } },
        { key: 'accountLevel', label: 'Cấp tài khoản', thStyle: { width: '140px' } },
        { key: 'workRole', label: 'Vai trò làm việc', thStyle: { width: '180px' } },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '110px' } },
        { key: 'option', label: 'Chức năng', thStyle: { width: '120px' } }
      ],
      form: {
        id: null,
        username: '',
        fullName: '',
        phone: '',
        email: '',
        password: '',
        passwordConfirm: '',
        roleId: null,
        isAdmin: false,
        adminType: null,
        adminPassword: '',
        adminPasswordConfirm: '',
        extraPermissionIds: []
      },
      adminTypeOptions: [
        { value: 1, text: 'Admin hệ thống' },
        { value: 2, text: 'Admin company' }
      ],
      userPermissionOverrides: {},
      // Dedicated permission modal state
      permForm: { userId: null, roleId: null, username: '', name: '', role: null, status: null, email: '', phone: '' },
      permOverrides: {},
      tempPassword: ''
    }
  },
  computed: {
    // không cho đổi users.role (root / admin)
    roleOptionsFiltered() {
      const blocked = new Set(['root', 'admin'])
      return this.roles
        .filter(r => !blocked.has(String(r.name).toLowerCase()))
        .map(r => ({ value: r.id, text: r.display_name || r.displayName || r.name }))
    },
    // Static filter options for users.role
    userRoleFilterOptions() {
      return [
        { value: 0, text: 'Root' },
        { value: 1, text: 'Quản trị' },
        { value: 2, text: 'Nhân viên' }
      ]
    },
    // Detect system company (id === 1) from JWT payload (companyId/company/cid)
    isSystemCompany() {
      const cid = this.companyIdFromToken
      return Number(cid) === 1
    },
    companyIdFromToken() {
      try {
        const token = localStorage.getItem('token-admin') || localStorage.getItem('token')
        const payload = parseJwt(token)
        let cid = (
          payload?.companyId ??
          payload?.company_id ??
          (typeof payload?.company === 'object' ? payload?.company?.id : payload?.company) ??
          payload?.cid ??
          payload?.tenantId ??
          payload?.tenant_id ??
          payload?.orgId ??
          payload?.org_id
        )
        if (cid == null || cid === '') {
          const lsCid = localStorage.getItem('companyId') || localStorage.getItem('cid')
          cid = lsCid
        }
        if (cid == null || cid === '') return undefined
        const n = Number(cid)
        return Number.isFinite(n) ? n : undefined
      } catch {
        const lsCid = localStorage.getItem('companyId') || localStorage.getItem('cid')
        if (lsCid == null || lsCid === '') return undefined
        const n = Number(lsCid)
        return Number.isFinite(n) ? n : undefined
      }
    },
    currentRole() {
      try {
        const token = localStorage.getItem('token') || localStorage.getItem('token-admin')
        const payload = parseJwt(token)
        const r = payload?.role
        return Number(r)
      } catch { return undefined }
    },
    isRoot() { return this.currentRole === 0 },
    isCompanySystem() { 
      const cid = this.companyIdFromToken
      if (cid === undefined || cid === null) return false
      return Number(cid) === 1 
    },
    passwordState() {
      const pwd = this.form.password || ''
      const confirm = this.form.passwordConfirm || ''
      const hasPwd = pwd.length > 0
      const minLenOk = pwd.length === 0 || pwd.length >= 8
      const matchOk = !hasPwd ? true : (pwd === confirm)
      // When creating: must have password, must meet min length, and must match
      if (!this.form.id) {
        if (!hasPwd) return null
        return minLenOk && matchOk
      }
      // When updating: if password provided, must meet min length and match; otherwise no validation
      if (hasPwd || confirm.length > 0) {
        return minLenOk && matchOk
      }
      return null
    },
    adminPasswordState() {
      // Only validate when admin_type == 1 (Admin hệ thống)
      if (!(this.form.isAdmin && Number(this.form.adminType) === 1)) return null
      if (!this.form.adminPassword && !this.form.adminPasswordConfirm) return false
      return this.form.adminPassword === this.form.adminPasswordConfirm
    },
    roleOptionsLoad() {
      const meRole = this.currentRole
      const meAdminType = this.currentAdminType
      if (meRole === 0) {
        // Root: all roles
        return this.roles.map(r => ({ value: r.id, text: r.display_name || r.displayName || r.name }))
      }
      if (meRole === 1 && meAdminType === 1) {
        // System admin: all roles
        return this.roles.map(r => ({ value: r.id, text: r.display_name || r.displayName || r.name }))
      }
      if (meRole === 1 && meAdminType === 2) {
        // Company admin: only roles where all permissions have level == 0
        const allowed = []
        for (const r of this.roles) {
          const perms = (r.permissions || [])
          const allLevel0 = perms.length === 0 || perms.every(p => Number(p.level) === 0 || p.level == null)
          if (allLevel0) allowed.push({ value: r.id, text: r.display_name || r.displayName || r.name })
        }
        return allowed
      }
      // Other roles/users: no assignment or safest minimal set (empty)
      return []
    },
    canSubmitForm() {
      const pwd = this.form.password || ''
      const confirm = this.form.passwordConfirm || ''
      const hasPwd = pwd.length > 0
      const minLenOk = !hasPwd ? true : pwd.length >= 8
      const matchOk = !hasPwd ? true : (pwd === confirm)
      // On create: require password provided + min length + match
      const passOkCreate = !this.form.id ? (hasPwd && minLenOk && matchOk) : true
      // On update: if password provided, require min length + match; else allow
      const passOkUpdate = this.form.id ? (!hasPwd ? true : (minLenOk && matchOk)) : true
      const passOk = passOkCreate && passOkUpdate

      const roleOk = this.form.roleId != null || this.form.isAdmin === true
      const adminTypeOk = this.form.isAdmin ? (this.form.adminType === 1 || this.form.adminType === 2) : true
      const adminPassOk = this.form.isAdmin && Number(this.form.adminType) === 1 ? (this.adminPasswordState === true) : true
      return !!this.form.username && !!this.form.fullName && passOk && roleOk && adminTypeOk && adminPassOk
    },
    currentAdminType() {
      try {
        const token = localStorage.getItem('token-admin') || localStorage.getItem('token')
        const p = parseJwt(token)
        const at = p?.adminType
        return at != null ? Number(at) : undefined
      } catch { return undefined }
    },
    isSystemAdminByType() { return this.currentAdminType === 1 },
    isEditingRootTarget() {
      if (!this.form.id) return false
      const target = this.items.find(x => x.id === this.form.id)
      const role = Number(target?.role ?? target?.user?.role)
      return role === 0
    },
    currentUserId() {
      try {
        const token = localStorage.getItem('token-admin') || localStorage.getItem('token')
        const p = parseJwt(token)
        const uid = p?.userId ?? p?.uid ?? p?.id
        return uid != null ? Number(uid) : undefined
      } catch { return undefined }
    },
    rolePermissionList() {
      const roleId = Number(this.form?.roleId)
      if (!Number.isFinite(roleId)) return []
      const role = this.roles.find(r => Number(r.id) === roleId)
      const perms = (role?.permissions || []).filter(p => Number(p.status) === 1)
      return Array.isArray(perms) ? perms : []
    },
    categoryById() {
      const map = {}
      for (const c of this.categories || []) map[c.id] = c
      return map
    },
    groupedPermissions() {
      const groups = {}
      for (const p of this.rolePermissionList) {
        const catName = this.getPermissionCategoryName(p)
        if (!groups[catName]) groups[catName] = []
        groups[catName].push(p)
      }
      return Object.entries(groups).map(([categoryName, items]) => ({ categoryName, items }))
    },
    // Permissions modal computed properties
    permRolePermissionList() {
      const roleId = Number(this.permForm?.roleId)
      if (!Number.isFinite(roleId)) return []
      const role = this.roles.find(r => Number(r.id) === roleId)
      let perms = (role?.permissions || []).filter(p => Number(p.status) === 1)
      // Company admin: only level 0 permissions are visible/editable
      const meRole = this.currentRole
      const meAdminType = this.currentAdminType
      if (meRole === 1 && meAdminType === 2) {
        perms = perms.filter(p => Number(p.level) === 0 || p.level == null)
      }
      return Array.isArray(perms) ? perms : []
    },
    permGroupedPermissions() {
      const groups = {}
      for (const p of this.permRolePermissionList) {
        const catName = this.getPermissionCategoryName(p)
        if (!groups[catName]) groups[catName] = { categoryName: catName, items: [] }
        groups[catName].items.push(p)
      }
      const res = Object.values(groups)
      for (const g of res) {
        g._allSelected = g.items.length > 0 && g.items.every(x => this.getPermEffectiveChecked(Number(x.id)))
      }
      return res
    },
    canSavePermissions() {
      return Number.isFinite(Number(this.permForm.userId)) && Number.isFinite(Number(this.permForm.roleId))
    },
    permRoleDisplayName() {
      const role = this.roles.find(r => Number(r.id) === Number(this.permForm?.roleId))
      return role?.display_name || role?.displayName || role?.name || '—'
    }
  },
  mounted: async function() {
    this.loadCategories()
    await this.loadRoles()
    await this.loadData()
  },
  methods: {
    roleCodeToText(item) {
      let code = item?.user?.role
      if (code === undefined) code = item?.role
      if (code && typeof code === 'object') code = code.role ?? code.code ?? code.id
      const n = Number(code)
      if (Number.isNaN(n)) return '—'
      switch (n) {
        case 0: return 'Root'
        case 1: return 'Quản trị'
        case 2: return 'Nhân viên'
        default: return '—'
      }
    },
    workRoleDisplayName(item) {
      // Strict mapping: users.work_role_id -> roles.id -> roles.display_name
      const workRoleId = Number(item?.work_role_id ?? item?.workRoleId ?? item?.workRole?.id)
      if (!Number.isFinite(workRoleId)) return '—'
      const roleObj = this.roles.find(r => Number(r.id) === workRoleId)
      return roleObj?.display_name || roleObj?.displayName || roleObj?.name || '—'
    },
    async loadRoles() {
      // Choose endpoint based on current operator privileges
      const meRole = this.currentRole
      const meAdminType = this.currentAdminType
      const primaryUrl = (meRole === 1 && meAdminType === 2) ? '/setting/roles/list' : '/administrator/roles/list'
      const fallbackUrl = (meRole === 1 && meAdminType === 2) ? '/administrator/roles/list' : '/setting/roles/list'
      const extractRoles = (data) => {
        // support both paged (data.content) and non-paged (array) responses
        if (Array.isArray(data)) return data
        if (Array.isArray(data?.content)) return data.content
        // some APIs return { items: [...] }
        if (Array.isArray(data?.items)) return data.items
        return []
      }
      try {
        const res = await axios.post(primaryUrl, null, { params: { page: 0, size: 1000 } })
        this.roles = extractRoles(res.data)
        // Fallback if empty (some environments may scope differently)
        if (!Array.isArray(this.roles) || this.roles.length === 0) {
          try {
            const res2 = await axios.post(fallbackUrl, null, { params: { page: 0, size: 1000 } })
            this.roles = extractRoles(res2.data)
          } catch (e2) { /* ignore fallback errors */ }
        }
      } catch (e) {
        // Attempt fallback on error
        try {
          const res2 = await axios.post(fallbackUrl, null, { params: { page: 0, size: 1000 } })
          this.roles = extractRoles(res2.data)
        } catch (e2) {
          this.roles = []
        }
      }
      // Map role permissions
      this.rolePermissions = {}
      for (const r of (this.roles || [])) {
        this.rolePermissions[r.id] = (r.permissions || []).filter(p => p.status === 1)
      }
    },
    async loadCategories() {
      try {
        const res = await axios.post('/administrator/permission-categories/list', null, { params: { page: 0, size: 200 } })
        this.categories = res.data?.content || []
      } catch { this.categories = [] }
    },
    async loadData() {
      this.isBusy = true
      try {
        const page = this.page.current - 1
        const params = {
          keyword: this.filters.keyword || undefined,
          // account-level role filter (0 Root / 1 Admin / 2 User)
          role: this.filters.userRole ?? undefined,
          status: this.filters.status,
          companyId: undefined, // scope if needed
          page,
          size: this.page.size
        }
        const res = await axios.post('/setting/members/list', null, { params })
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
      this.form = { id: null, username: '', fullName: '', phone: '', email: '', password: '', passwordConfirm: '', roleId: null, isAdmin: false, adminType: null, adminPassword: '', adminPasswordConfirm: '', extraPermissionIds: [] }
      this.userPermissionOverrides = {}
      // Ensure roles are loaded before showing modal
      Promise.resolve(this.roles && this.roles.length ? null : this.loadRoles()).then(() => {
        // Auto-select first allowed role for convenience (if any)
        const opts = this.roleOptionsLoad
        if (Array.isArray(opts) && opts.length > 0) this.form.roleId = opts[0].value
        this.$refs.memberModal.show()
      })
    },
    isRootItem(item) {
      const targetRole = Number(item?.role ?? item?.user?.role)
      return targetRole === 0
    },
    canOperateOn(item) {
      const targetRole = Number(item?.role ?? item?.user?.role)
      const meRole = this.currentRole
      // Root protections: only root can operate on root and only on itself
      if (targetRole === 0) {
        const meId = this.currentUserId
        const targetId = Number(item?.id ?? item?.user?.id)
        return meRole === 0 && meId != null && targetId != null && Number(meId) === Number(targetId)
      }
      // Admin users cannot operate on Admin accounts
      if (meRole === 1 && targetRole === 1) {
        return false
      }
      // Other cases allowed
      return true
    },
    onWorkRoleChange() {
      // When changing role, keep existing overrides only for permissions in new role
      const ids = new Set(this.rolePermissionList.map(p => Number(p.id)))
      const next = {}
      for (const [pid, val] of Object.entries(this.userPermissionOverrides)) {
        const nPid = Number(pid)
        if (ids.has(nPid)) next[nPid] = val
      }
      this.userPermissionOverrides = next
    },
    getOverrideState(permissionId) {
      const v = this.userPermissionOverrides[Number(permissionId)]
      return v === 1 ? 1 : (v === 0 ? 0 : null)
    },
    setOverride(permissionId, allowed) {
      const pid = Number(permissionId)
      if (allowed !== 0 && allowed !== 1) return
      this.$set(this.userPermissionOverrides, pid, allowed)
    },
    clearOverride(permissionId) {
      const pid = Number(permissionId)
      if (this.userPermissionOverrides.hasOwnProperty(pid)) {
        this.$delete(this.userPermissionOverrides, pid)
      }
    },
    async loadUserPermissionOverrides(userId) {
      try {
        const res = await axios.get(`/setting/members/${userId}/permissions`)
        const list = Array.isArray(res.data) ? res.data : (res.data?.content || [])
        const map = {}
        for (const it of list) {
          const pid = Number(it.permissionId ?? it.id ?? it.permission?.id)
          const allowed = Number(it.allowed ?? it.allow ?? it.value)
          if (Number.isFinite(pid) && (allowed === 0 || allowed === 1)) map[pid] = allowed
        }
        this.userPermissionOverrides = map
      } catch {
        this.userPermissionOverrides = {}
      }
    },
    openEdit(item) {
      // Ensure roles up-to-date for edit dropdown
      const ensureRoles = Promise.resolve(this.roles && this.roles.length ? null : this.loadRoles())
      ensureRoles.then(() => {
        this.form = {
          id: item.id,
          username: item.username || item.user?.username || item.email || '',
          fullName: item.name || item.fullName,
          phone: item.phone || '',
          email: item.email || '',
          password: '', passwordConfirm: '',
          // Use workRoleId/workRole.id for editable work role
          roleId: Number(item.workRoleId ?? item.workRole?.id),
          isAdmin: Number(item.role ?? item.user?.role) === 1,
          adminType: (item.adminType != null ? Number(item.adminType) : (Number(item.role ?? item.user?.role) === 1 ? 2 : null)),
          adminPassword: '',
          adminPasswordConfirm: '',
          extraPermissionIds: item.extraPermissionIds || []
        }
        const isTargetRoot = Number(item.role ?? item.user?.role) === 0
        if (isTargetRoot) {
          // When editing root, disable admin toggle and keep role/adminType intact
          this.form.isAdmin = false
          this.form.adminType = null
        }
        // If current roleId is not in allowed options (e.g., Admin company), clear it to force selection
        const allowedIds = new Set((this.roleOptionsLoad || []).map(o => Number(o.value)))
        if (!allowedIds.has(Number(this.form.roleId))) this.form.roleId = null
        this.$refs.memberModal.show()
        if (item?.id) this.loadUserPermissionOverrides(item.id)
      })
    },
    openPermission(item) {
      // Initialize modal state
      this.permForm = {
        userId: Number(item.id ?? item.user?.id),
        // Use work role id for permissions modal
        roleId: Number(item.workRoleId ?? item.workRole?.id),
        username: item.username || item.user?.username || '',
        name: item.name || item.user?.name || '',
        role: Number(item.role ?? item.user?.role),
        status: Number(item.status ?? item.user?.status),
        email: item.email || item.user?.email || '',
        phone: item.phone || item.user?.phone || ''
      }
      // Load overrides for this user into modal-specific state
      this.loadUserPermissionOverrides(this.permForm.userId).then(() => {
        // Mirror overrides into permOverrides scoped to modal
        this.permOverrides = { ...this.userPermissionOverrides }
        this.$refs.permissionModal.show()
        this.$nextTick(() => {
          const el = this.$el.querySelector('.perm-group') || this.$el.querySelector('.perm-row')
          if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
        })
      })
    },
    getPermOverrideState(permissionId) {
      const v = this.permOverrides[Number(permissionId)]
      return v === 1 ? 1 : (v === 0 ? 0 : null)
    },
    // Set override value (0 or 1) for permissions in the permissions modal
    setPermOverride(permissionId, allowed) {
      const pid = Number(permissionId)
      const val = Number(allowed)
      if (!Number.isFinite(pid)) return
      if (val !== 0 && val !== 1) return
      this.$set(this.permOverrides, pid, val)
    },
    // Effective checked: for listed permissions, role default is checked; override 0 makes it unchecked
    getPermEffectiveChecked(permissionId) {
      const st = this.getPermOverrideState(permissionId)
      if (st === 1) return true
      if (st === 0) return false
      // For modal we list only permissions included in role => default is checked
      return true
    },
    onPermCheckboxChange(permissionId, checked) {
      const isChecked = !!checked
      this.setPermOverride(permissionId, isChecked ? 1 : 0)
    },
    togglePermGroup(group) {
      const allSelected = group.items.length > 0 && group.items.every(p => this.getPermEffectiveChecked(Number(p.id)))
      if (allSelected) {
        for (const p of group.items) this.setPermOverride(Number(p.id), 0)
      } else {
        for (const p of group.items) this.setPermOverride(Number(p.id), 1)
      }
    },
    permSelectAll(flag) {
      if (flag) {
        for (const p of this.permRolePermissionList) this.setPermOverride(Number(p.id), 1)
      } else {
        for (const p of this.permRolePermissionList) this.setPermOverride(Number(p.id), 0)
      }
    },
    async savePermissions() {
      if (!this.canSavePermissions) return
      // Preserve non-nullable fields to avoid backend overwriting with nulls
      if (!this.permForm.username) {
        const src = this.items.find(x => Number(x.id ?? x.user?.id) === Number(this.permForm.userId))
        if (src) this.permForm.username = src.username || src.user?.username || ''
      }
      // Ensure we preserve email and phone as well
      const srcItem = (!this.permForm.email || !this.permForm.phone) ? this.items.find(x => Number(x.id ?? x.user?.id) === Number(this.permForm.userId)) : null
      if (!this.permForm.email && srcItem) {
        this.permForm.email = srcItem.email || srcItem.user?.email || ''
      }
      if (!this.permForm.phone && srcItem) {
        this.permForm.phone = srcItem.phone || srcItem.user?.phone || ''
      }
      if (!this.permForm.username) {
        window.alert('Không thể lưu: thiếu username của người dùng. Vui lòng tải lại danh sách và thử lại.')
        return
      }
      const safeRole = Number.isFinite(Number(this.permForm.role)) ? Number(this.permForm.role) : undefined
      const safeStatus = (this.permForm.status === 0 || this.permForm.status === 1) ? this.permForm.status : undefined
      // Only include overrides for permissions present in the filtered list
      const allowedPermIds = new Set((this.permRolePermissionList || []).map(p => Number(p.id)))
      const userPermissions = Object.entries(this.permOverrides)
        .map(([pid, allowed]) => ({ permissionId: Number(pid), allowed: Number(allowed) }))
        .filter(it => allowedPermIds.has(Number(it.permissionId)))
      const payload = {
        id: this.permForm.userId,
        username: this.permForm.username,
        name: this.permForm.name || undefined,
        email: this.permForm.email || undefined,
        phone: this.permForm.phone || undefined,
        role: safeRole,
        status: safeStatus,
        workRole: this.permForm.roleId ? { id: this.permForm.roleId } : undefined,
        userPermissions
      }
      await axios.post('/setting/members/saveOrUpdate', payload)
      this.$toastr.success('Đã lưu phân quyền thành viên')
      this.$refs.permissionModal.hide()
      this.loadData()
    },
    async saveMember() {
      if (!this.canSubmitForm) return
      // Privilege guard: Admin cannot edit Admin or Root
      if (this.form.id) {
        const target = this.items.find(x => x.id === this.form.id)
        const targetRole = Number(target?.role ?? target?.user?.role)
        const meRole = this.currentRole
        if (meRole === 1 && (targetRole === 0 || targetRole === 1)) {
          window.alert('Bạn không có quyền chỉnh sửa tài khoản Root/Admin')
          return
        }
      }
      // Company admin constraint: only assign roles where all permissions level == 0
      const meRole = this.currentRole
      const meAdminType = this.currentAdminType
      if (meRole === 1 && meAdminType === 2 && this.form.roleId != null) {
        const role = this.roles.find(r => Number(r.id) === Number(this.form.roleId))
        const perms = (role?.permissions || [])
        const allLevel0 = perms.length === 0 || perms.every(p => Number(p.level) === 0 || p.level == null)
        if (!allLevel0) {
          window.alert('Admin company chỉ được gán vai trò có tất cả quyền Level = 0')
          return
        }
      }
      // Prevent changing root's role when editing self
      const isEditingRoot = this.form.id && Number(this.items.find(x => x.id === this.form.id)?.role) === 0
      const rolePayload = isEditingRoot ? undefined : (this.form.isAdmin ? 1 : 2)
      const payload = {
        id: this.form.id || undefined,
        username: this.form.username,
        name: this.form.fullName,
        phone: this.form.phone || undefined,
        email: this.form.email || undefined,
        // Send password on create or when provided on update
        password: this.form.password ? this.form.password : (this.form.id ? undefined : this.form.password),
        role: rolePayload,
        workRole: this.form.roleId ? { id: this.form.roleId } : undefined,
        adminType: this.form.isAdmin ? (this.form.adminType || 2) : 0,
        adminPassword: (this.form.isAdmin && Number(this.form.adminType) === 1 && this.form.adminPassword) ? this.form.adminPassword : undefined,
        // Only send overrides that correspond to selected role permissions
        userPermissions: (() => {
          const allowedIds = new Set((this.rolePermissionList || []).map(p => Number(p.id)))
          return Object.entries(this.userPermissionOverrides)
            .map(([pid, allowed]) => ({ permissionId: Number(pid), allowed: Number(allowed) }))
            .filter(it => allowedIds.has(Number(it.permissionId)))
        })()
      }
      await axios.post('/setting/members/saveOrUpdate', payload)
      this.$toastr.success(this.form.id ? 'Cập nhật thành viên thành công' : 'Thêm thành viên thành công')
      this.$refs.memberModal.hide()
      this.loadData()
    },
    async toggleLock(item) {
      await axios.post(`/setting/members/${item.id}/lock`, null, { params: { lock: Number(item.status) === 1 ? 1 : 0 } })
      this.$toastr.success(Number(item.status) === 1 ? 'Đã khóa tài khoản' : 'Đã mở khóa tài khoản')
      this.loadData()
    },
    async resetPassword(item) {
      const res = await axios.post(`/setting/members/${item.id}/reset-password`)
      const pwd = res?.data?.temporaryPassword || res?.data?.password || ''
      if (pwd) {
        this.tempPassword = String(pwd)
        this.$refs.tempPwdModal.show()
      } else {
        this.$toastr.success('Đã reset mật khẩu thành công')
      }
    },
    async copyTempPassword() {
      try {
        await navigator.clipboard.writeText(this.tempPassword || '')
        this.$toastr.success('Đã sao chép mật khẩu')
      } catch {
        // Fallback: select text for manual copy
        const el = this.$el.querySelector('input[readonly]')
        if (el) { el.focus(); el.select() }
      }
    },
    onGeneratePassword() {
      const pwd = this.generateStrongPassword()
      this.form.password = pwd
      this.form.passwordConfirm = pwd
    },
    onGenerateAdminPassword() {
      const pwd = this.generateStrongPassword()
      this.form.adminPassword = pwd
      this.form.adminPasswordConfirm = pwd
    },
    generateStrongPassword(length = 14) {
      // Ensure at least one from each group
      const lowers = 'abcdefghijkmnopqrstuvwxyz' // exclude ambiguous characters
      const uppers = 'ABCDEFGHJKLMNPQRSTUVWXYZ'
      const digits = '23456789'
      const symbols = '!@#$%^&*()-_=+[]{}:;,./?'
      const groups = [lowers, uppers, digits, symbols]
      function randFrom(str) { return str[Math.floor(Math.random() * str.length)] }
      let result = [randFrom(lowers), randFrom(uppers), randFrom(digits), randFrom(symbols)]
      const all = lowers + uppers + digits + symbols
      while (result.length < length) {
        result.push(randFrom(all))
      }
      // shuffle
      for (let i = result.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1))
        const t = result[i]; result[i] = result[j]; result[j] = t
      }
      return result.join('')
    },
    canEditPermissions(item) {
      const meRole = Number(this.currentRole)
      const meAdminType = Number(this.currentAdminType)
      const targetRole = Number(item?.role ?? item?.user?.role)
      // never allow permissions editing for root accounts
      if (targetRole === 0) return false
      // Root can edit any
      if (meRole === 0) return true
      // System admin can edit any non-root
      if (meRole === 1 && meAdminType === 1) return true
      // Company admin can edit, but will be limited to level 0 permissions in modal
      if (meRole === 1 && meAdminType === 2) return true
      return false
    },
    getPermissionCategoryName(p) {
      try {
        // try nested category object
        const catObj = p?.permissionCategory || p?.category
        const nameFromObj = catObj?.displayName || catObj?.name
        if (nameFromObj && String(nameFromObj).trim()) return String(nameFromObj).trim()
        // try id-based lookup
        const catId = (catObj && catObj.id != null) ? catObj.id : (p?.categoryId ?? p?.category_id ?? p?.permissionCategoryId ?? p?.permission_category_id ?? p?.category)
        if (catId != null && this.categoryById[catId]?.name) return this.categoryById[catId].name
        return 'Khác'
      } catch { return 'Khác' }
    },
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
.admin-type-cards { display: flex; gap: 12px; }
.admin-type-card { flex: 1 1 0; display: flex; align-items: center; gap: 12px; padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; cursor: pointer; transition: all .15s ease; }
.admin-type-card .icon { font-size: 20px; width: 28px; display: flex; align-items: center; justify-content: center; }
.admin-type-card .title { font-weight: 700; color: #111827; }
.admin-type-card .desc { font-size: 12px; color: #64748b; margin-top: 2px; }
.admin-type-card:hover { border-color: #c7d2fe; background: #f8fafc; }
.admin-type-card.active { border-color: #3b82f6; background: #eff6ff; box-shadow: 0 0 0 2px rgba(59,130,246,.15); }
.perm-grid { display: grid; grid-template-columns: 1fr; grid-row-gap: 10px; }
.perm-row { display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; border: 1px solid #eef2f7; border-radius: 8px; }
.perm-info { display: flex; align-items: center; gap: 10px; }
.perm-name { font-weight: 600; }
.perm-code { font-size: 12px; color: #6b7280; }
.perm-group { border-top: 1px dashed #eee; padding-top: 8px; margin-top: 8px; }
.perm-group-header { display: flex; align-items: center; margin-bottom: 6px; }
.perm-group-items { display: flex; flex-direction: column; gap: 10px; }
/* Reuse role modal polish for permissions modal */
.members .role-modal-content { border-radius: 14px; overflow: hidden; border: 1px solid #eef0f6; box-shadow: 0 10px 30px rgba(18, 38, 63, 0.08); }
.members .role-modal-header { background: linear-gradient(180deg, #ffffff 0%, #f7f9fc 100%); border-bottom: 1px solid #ecf0f6; }
.members .role-modal-body { background: #ffffff; max-height: 72vh; overflow: auto; padding-bottom: 8px; }
.members .section-card { border: 1px solid #e8e8e8; border-radius: 12px; padding: 12px 14px; background: #fff; box-shadow: 0 1px 2px rgba(0,0,0,0.02); }
.members .perm-group { border: 1px solid #e8e8e8; border-radius: 12px; }
.members .perm-group-header { border-bottom: 1px dashed #ecf0f6; padding-bottom: 6px; margin-bottom: 8px; }
@media (max-width: 576px) { .admin-type-cards { flex-direction: column; } }
</style>
