<template>
  <div class="container-fluid py-3 members">
    <!-- Tiêu đề và thao tác -->
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

    <!-- Bộ lọc -->
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
          <b-button size="sm" variant="primary" @click="applyFilters">Tìm kiếm</b-button>
        </b-col>
      </b-row>
    </b-card>

    <!-- Bảng thành viên -->
    <b-card class="shadow-sm">
      <b-table
        bordered
        hover
        responsive
        small
        show-empty
        :items="list.data"
        :fields="fields"
        :busy="isBusy"
        empty-text="Không có dữ liệu"
      >
        <template #cell(index)="{ index }">
          {{ index + 1 + (list.current_page - 1) * list.per_page }}
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

        <template #cell(accountLevel)="{ item }">
          <b-badge variant="info" class="role-pill">{{ roleCodeToText(item) }}</b-badge>
        </template>

        <template #cell(option)="{ item }">
          <b-dropdown v-if="canOperateOn(item)" size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="openEdit(item)">Cập nhật</b-dropdown-item>
            <b-dropdown-item v-if="canEditPermissions(item)" class="text-center" href="#" @click.prevent="openPermission(item)">Phân quyền</b-dropdown-item>
            <b-dropdown-item v-if="canSendInfo(item)" class="text-center" href="#" @click.prevent="sendInfo(item)">Gửi thông tin</b-dropdown-item>
            <b-dropdown-item v-if="canToggleLock(item)" class="text-center" href="#" @click.prevent="toggleLock(item)">
              <span :class="Number(item.status) !== 1 ? 'text-success' : 'text-warning'">
                {{ Number(item.status) !== 1 ? 'Mở khóa' : 'Khóa' }}
              </span>
            </b-dropdown-item>
            <b-dropdown-item v-if="canResetPassword(item)" class="text-center" href="#" @click.prevent="resetPassword(item)">Reset mật khẩu</b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <!-- Khung tải khi chuyển trang -->
      <div v-if="isBusy" class="mt-2">
        <b-skeleton width="100%" height="20px" animated class="mb-2" />
        <b-skeleton width="96%" height="20px" animated class="mb-2" />
        <b-skeleton width="92%" height="20px" animated class="mb-2" />
      </div>

      <pagination-bar
        :current.sync="list.current_page"
        :size.sync="list.per_page"
        :total="list.total"
        :sizes="pageSizes"
        @page-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </b-card>

    <!-- Hộp thoại tạo/cập nhật thành viên -->
    <b-modal ref="memberModal" :title="form.id ? 'Cập nhật thành viên' : 'Thêm thành viên'" hide-footer>
      <b-form @submit.prevent="saveMember">
        <!-- Hiển thị thông tin cho thành viên mới -->
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

        <!-- Ch? root: d?t quy?n Admin -->
        <b-form-group v-if="isRoot && !isEditingRootTarget" label="Đặt user này là Admin">
          <b-form-checkbox v-model="form.isAdmin">Đặt làm Admin</b-form-checkbox>
        </b-form-group>

        <b-form-row v-if="form.isAdmin">
          <b-col md="6">
            <b-form-group label="Mật khẩu quản trị" :state="adminPasswordState">
              <b-input-group>
                <b-form-input v-model.trim="form.adminPassword" type="password" />
                <b-input-group-append>
                  <b-button size="sm" variant="outline-secondary" @click="onGenerateAdminPassword">Tạo</b-button>
                </b-input-group-append>
              </b-input-group>
              <small v-if="adminPasswordState === false" class="text-danger">Mật khẩu quản trị tối thiểu 8 ký tự</small>
            </b-form-group>
          </b-col>
          <b-col md="6">
            <b-form-group label="Nhập lại mật khẩu quản trị" :state="adminPasswordState">
              <b-form-input v-model.trim="form.adminPasswordConfirm" type="password" />
              <small v-if="adminPasswordState === false" class="text-danger">Mật khẩu quản trị không khớp</small>
            </b-form-group>
          </b-col>
        </b-form-row>

        <!-- Ghi chú khi sửa tài khoản root -->
        <b-alert v-if="isEditingRootTarget" variant="info" class="mt-3">
          Bạn đang chỉnh cập nhật tài khoản <strong>Root</strong>. Vai trò và quyền hạn của tài khoản này không thể thay đổi.
        </b-alert>
        <div class="text-right member-modal-actions">
          <b-button type="submit" variant="primary" :disabled="!canSubmitForm">Lưu</b-button>
          <b-button variant="secondary" @click="$refs.memberModal.hide()">Hủy</b-button>
        </div>
      </b-form>
    </b-modal>

    <!-- Hộp thoại phân quyền -->
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
                  <div class="text-muted small">Thành viên</div>
                  <div class="font-weight-bold">{{ permForm.name || permForm.username }}</div>
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
              <b-badge variant="light" class="text-muted">Tích chọn để bật quyền</b-badge>
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
                        />
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
import PaginationBar from '@/views/components/pagination_bar.vue'
import { parseJwt } from '@/utils/jwt'

export default {
  name: 'SettingsMemberList',
  components: { PaginationBar },
  data() {
    return {
      isBusy: false,
      // Đối tượng phân trang mới theo cấu trúc yêu cầu
      list: {
        current_page: 1,
        data: [],
        last_page: 1,
        prev_page_url: null,
        next_page_url: null,
        per_page: 10,
        total: 0,
        from: 0,
        to: 0
      },
      categories: [],
      allPermissions: [],
      pageSizes: [10, 20, 50, 100],
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
        { key: 'accountLevel', label: 'Vai trò', thStyle: { width: '140px' } },
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
        isAdmin: false,
        adminPassword: '',
        adminPasswordConfirm: ''
      },
      permForm: { userId: null, username: '', name: '', role: null, status: null, email: '', phone: '' },
      permOverrides: {}
    }
  },
  computed: {
    userRoleFilterOptions() {
      return [
        { value: 0, text: 'Root' },
        { value: 1, text: 'Quản trị' },
        { value: 2, text: 'Nhân viên' }
      ]
    },
    companyIdFromToken() {
      try {
        const token = localStorage.getItem('token-admin') || localStorage.getItem('token')
        const payload = parseJwt(token)
        let cid = (
          payload?.companyId ??
          payload?.company_id ??
          (typeof payload?.company === 'object' ? payload?.company?.id : payload?.company) ??
          payload?.cid ?? payload?.tenantId ?? payload?.tenant_id ?? payload?.orgId ?? payload?.org_id
        )
        if (cid == null || cid === '') return undefined
        const n = Number(cid)
        return Number.isFinite(n) ? n : undefined
      } catch { return undefined }
    },
    currentCompanyId() {
      const id = this.$app?.info?.company?.id
      if (Number.isFinite(Number(id))) return Number(id)
      return this.companyIdFromToken
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
    passwordState() {
      const pwd = this.form.password || ''
      const confirm = this.form.passwordConfirm || ''
      const hasPwd = pwd.length > 0
      const minLenOk = pwd.length === 0 || pwd.length >= 8
      const matchOk = !hasPwd ? true : (pwd === confirm)
      if (!this.form.id) {
        if (!hasPwd) return null
        return minLenOk && matchOk
      }
      if (hasPwd || confirm.length > 0) {
        return minLenOk && matchOk
      }
      return null
    },
    adminPasswordState() {
      // Chỉ validate khi isAdmin được chọn và có trường được nhập
      if (!this.form.isAdmin) return null
      const ap = this.form.adminPassword || ''
      const apc = this.form.adminPasswordConfirm || ''
      if (ap.length === 0 && apc.length === 0) return null
      return ap.length >= 8 && ap === apc
    },
    canSubmitForm() {
      const pwd = this.form.password || ''
      const confirm = this.form.passwordConfirm || ''
      const hasPwd = pwd.length > 0
      const minLenOk = !hasPwd ? true : pwd.length >= 8
      const matchOk = !hasPwd ? true : (pwd === confirm)
      const passOkCreate = !this.form.id ? (hasPwd && minLenOk && matchOk) : true
      const passOkUpdate = this.form.id ? (!hasPwd ? true : (minLenOk && matchOk)) : true
      const passOk = passOkCreate && passOkUpdate
      const adminPwdProvided = (this.form.adminPassword?.length || 0) > 0 || (this.form.adminPasswordConfirm?.length || 0) > 0
      const adminPwdOk = !this.form.isAdmin ? true : (!adminPwdProvided ? true : (this.adminPasswordState === true))
      const hasUsername = !this.form.id || (!!this.form.username && this.form.username.trim().length > 0)
      return hasUsername && !!this.form.fullName && passOk && adminPwdOk
    },
    isEditingRootTarget() {
      if (!this.form.id) return false
      const target = this.list.data.find(x => x.id === this.form.id)
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
    categoryById() {
      const map = {}
      for (const c of this.categories || []) map[c.id] = c
      return map
    },
    permVisiblePermissions() {
      // Cơ bản: chỉ lấy trạng thái active hoặc không chỉ định
      let perms = (this.allPermissions || []).filter(p => Number(p.status) === 1 || p.status == null)
      // Quy tắc nghiệp vụ: khi sửa nhân viên role === 2, chỉ hiển thị quyền level === 0
      const targetRole = Number(this.permForm?.role)
      if (targetRole === 2) {
        perms = perms.filter(p => Number(p.level) === 0)
      }
      return perms
    },
    permGroupedPermissions() {
      const groups = {}
      for (const p of this.permVisiblePermissions) {
        const catName = this.getPermissionCategoryName(p)
        if (!groups[catName]) {
          // Lấy orderIndex của danh mục để sắp xếp
          const catObj = p?.permissionCategory || p?.category
          const catId = (catObj && catObj.id != null) ? catObj.id : (p?.categoryId ?? p?.category_id ?? p?.permissionCategoryId ?? p?.permission_category_id ?? p?.category)
          const category = catId != null ? this.categoryById[catId] : null
          const orderIndex = category ? (Number(category.orderIndex ?? category.sothutu ?? 999)) : 999
          
          groups[catName] = { 
            categoryName: catName, 
            items: [],
            orderIndex: orderIndex,
            categoryId: catId
          }
        }
        groups[catName].items.push(p)
      }
      const res = Object.values(groups)
      // Sắp xếp nhóm theo category orderIndex (sothutu)
      res.sort((a, b) => {
        const aOrder = Number(a.orderIndex ?? 999)
        const bOrder = Number(b.orderIndex ?? 999)
        return aOrder - bOrder
      })
      for (const g of res) {
        g._allSelected = g.items.length > 0 && g.items.every(x => this.getPermEffectiveChecked(Number(x.id)))
      }
      return res
    },
    canSavePermissions() {
      return Number.isFinite(Number(this.permForm.userId))
    }
  },
  mounted: async function() {
    // Đồng bộ từ query string khi mount
    const q = this.$route?.query || {}
    const qp = Number(q.page)
    const qs = Number(q.size)
    if (Number.isFinite(qp) && qp > 0) this.list.current_page = qp
    if (Number.isFinite(qs) && qs > 0) this.list.per_page = qs

    this.loadCategories()
    await this.loadAllPermissions()
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
    async loadAllPermissions() {
      try {
        const res = await axios.post('/administrator/permissions/list', null, { params: { page: 0, size: 2000 } })
        const data = res?.data
        this.allPermissions = Array.isArray(data?.content) ? data.content : (Array.isArray(data) ? data : [])
      } catch {
        this.allPermissions = []
      }
    },
    async loadCategories() {
      try {
        const res = await axios.post('/administrator/permission-categories/list', null, { params: { page: 0, size: 200 } })
        // Phía backend đã sắp xếp theo orderIndex (sothutu), nhưng vẫn thêm lớp bảo vệ
        const cats = res.data?.content || []
        this.categories = cats.sort((a, b) => {
          const aOrder = Number(a.orderIndex ?? a.sothutu ?? 999)
          const bOrder = Number(b.orderIndex ?? b.sothutu ?? 999)
          return aOrder - bOrder
        })
      } catch { this.categories = [] }
    },
    async loadData() {
      this.isBusy = true
      try {
        const pageZero = Math.max(0, Number(this.list.current_page || 1) - 1)
        const params = {
          keyword: this.filters.keyword || undefined,
          role: this.filters.userRole ?? undefined,
          status: this.filters.status,
          // Bỏ companyId; backend tự detect từ user đã xác thực
          page: pageZero,
          size: this.list.per_page
        }
        const res = await axios.post('/setting/members/list', null, { params })
        const d = res?.data || {}
        // Chuẩn hóa key dữ liệu để phòng trường hợp lệch
        this.list.data = Array.isArray(d.data) ? d.data : (Array.isArray(d.content) ? d.content : [])
        this.list.total = Number(d.total ?? d.totalElements ?? 0)
        this.list.per_page = Number(d.per_page ?? this.list.per_page)
        this.list.current_page = Number(d.current_page ?? (Number(params.page) + 1))
        this.list.last_page = Number(d.last_page ?? Math.max(1, Math.ceil(this.list.total / this.list.per_page)))
        this.list.from = Number(d.from ?? ((this.list.total === 0) ? 0 : ((this.list.current_page - 1) * this.list.per_page + 1)))
        const numberOfElements = Array.isArray(this.list.data) ? this.list.data.length : 0
        this.list.to = Number(d.to ?? ((this.list.total === 0) ? 0 : (this.list.from + numberOfElements - 1)))

        // Cập nhật query string
        if (this.$route) {
          this.$router.replace({ query: { ...this.$route.query, page: String(this.list.current_page), size: String(this.list.per_page) } }).catch(() => {})
        }
      } finally {
        this.isBusy = false
      }
    },
    applyFilters() { this.list.current_page = 1; this.loadData() },
    onPageChange(p) { this.list.current_page = Number(p); this.loadData() },
    onPageSizeChange(s) { this.list.per_page = Number(s || this.list.per_page); this.list.current_page = 1; this.loadData() },
    reload() { this.applyFilters() },
    openCreate() {
      this.form = { id: null, username: '', fullName: '', phone: '', email: '', password: '', passwordConfirm: '', isAdmin: false, adminPassword: '', adminPasswordConfirm: '' }
      this.$refs.memberModal.show()
    },
    canOperateOn(item) {
      const targetRole = Number(item?.role ?? item?.user?.role)
      const meRole = this.currentRole
      const meId = this.currentUserId
      const targetId = Number(item?.id ?? item?.user?.id)
      if (targetRole === 0) {
        return meRole === 0 && meId != null && targetId != null && Number(meId) === Number(targetId)
      }
      if (meRole === 1 && targetRole === 1) {
        // Admin chỉ thao tác trên chính mình, không thao tác admin khác
        return meId != null && targetId != null && Number(meId) === Number(targetId)
      }
      return true
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
        this.permOverrides = map
      } catch { this.permOverrides = {} }
    },
    openEdit(item) {
      this.form = {
        id: item.id,
        username: item.username || item.user?.username || '',
        fullName: item.name || item.fullName,
        phone: item.phone || '',
        email: item.email || '',
        password: '', passwordConfirm: '',
        isAdmin: Number(item.role ?? item.user?.role) === 1,
        adminPassword: '',
        adminPasswordConfirm: ''
      }
      const isTargetRoot = Number(item.role ?? item.user?.role) === 0
      if (isTargetRoot) { this.form.isAdmin = false }
      this.$refs.memberModal.show()
    },
    openPermission(item) {
      this.permForm = {
        userId: Number(item.id ?? item.user?.id),
        username: item.username || item.user?.username || '',
        name: item.name || item.user?.name || '',
        role: Number(item.role ?? item.user?.role),
        status: Number(item.status ?? item.user?.status),
        email: item.email || item.user?.email || '',
        phone: item.phone || item.user?.phone || ''
      }
      this.loadUserPermissionOverrides(this.permForm.userId).then(() => {
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
    getPermEffectiveChecked(permissionId) { return this.getPermOverrideState(permissionId) === 1 },
    onPermCheckboxChange(permissionId, checked) { 
      this.$set(this.permOverrides, Number(permissionId), checked ? 1 : 0)
    },
    togglePermGroup(group) {
      const allSelected = group.items.length > 0 && group.items.every(p => this.getPermEffectiveChecked(Number(p.id)))
      for (const p of group.items) {
        this.$set(this.permOverrides, Number(p.id), allSelected ? 0 : 1)
      }
    },
    permSelectAll(flag) { 
      for (const p of this.permVisiblePermissions) {
        this.$set(this.permOverrides, Number(p.id), flag ? 1 : 0)
      }
    },
    async savePermissions() {
      if (!this.canSavePermissions) return
      if (!this.permForm.username) {
        const src = this.list.data.find(x => Number(x.id ?? x.user?.id) === Number(this.permForm.userId))
        if (src) this.permForm.username = src.username || src.user?.username || ''
      }
      const allowedPermIds = new Set((this.permVisiblePermissions || []).map(p => Number(p.id)))
      const userPermissions = Object.entries(this.permOverrides)
        .map(([pid, allowed]) => ({ permissionId: Number(pid), allowed: Number(allowed) }))
        .filter(it => allowedPermIds.has(Number(it.permissionId)))
      const payload = {
        id: this.permForm.userId,
        username: this.permForm.username || undefined,
        role: Number.isFinite(Number(this.permForm.role)) ? Number(this.permForm.role) : undefined,
        status: (this.permForm.status === 0 || this.permForm.status === 1) ? this.permForm.status : undefined,
        userPermissions
      }
      await axios.post('/setting/members/saveOrUpdate', payload)
      this.$toastr.success('Đã lưu phân quyền thành viên')
      this.$refs.permissionModal.hide()
      this.loadData()
    },
    async saveMember() {
      if (!this.canSubmitForm) return
      if (this.form.id) {
        const target = this.list.data.find(x => x.id === this.form.id)
        const targetRole = Number(target?.role ?? target?.user?.role)
        const meRole = this.currentRole
        if (meRole === 1 && (targetRole === 0 || targetRole === 1)) {
          window.alert('Bạn không có quyền chỉnh cập nhật tài khoản Root/Admin')
          return
        }
      }
      const isEditingRoot = this.form.id && Number(this.list.data.find(x => x.id === this.form.id)?.role) === 0
      const rolePayload = isEditingRoot ? undefined : (this.form.isAdmin ? 1 : 2)
      const payload = {
        id: this.form.id || undefined,
        username: this.form.username || undefined,
        name: this.form.fullName,
        phone: this.form.phone || undefined,
        email: this.form.email || undefined,
        password: this.form.password ? this.form.password : (this.form.id ? undefined : this.form.password),
        role: rolePayload,
        adminPassword: (this.form.isAdmin && this.form.adminPassword) ? this.form.adminPassword : undefined
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
      await axios.post(`/setting/members/${item.id}/reset-password`)
      this.$toastr.success('Đã reset mật khẩu thành công. Mật khẩu mới đã được gửi qua email.')
      this.loadData()
    },
    onGeneratePassword() { const pwd = this.generateStrongPassword(); this.form.password = pwd; this.form.passwordConfirm = pwd },
    onGenerateAdminPassword() {
      const pwd = this.generateStrongPassword()
      this.form.adminPassword = pwd
      this.form.adminPasswordConfirm = pwd
    },
    generateStrongPassword(length = 14) {
      const lowers = 'abcdefghijkmnopqrstuvwxyz'
      const uppers = 'ABCDEFGHJKLMNPQRSTUVWXYZ'
      const digits = '23456789'
      const symbols = '!@#$%^&*()-_=+[]{}:;,./?'
      const groups = [lowers, uppers, digits, symbols]
      function randFrom(str) { return str[Math.floor(Math.random() * str.length)] }
      let result = [randFrom(lowers), randFrom(uppers), randFrom(digits), randFrom(symbols)]
      const all = lowers + uppers + digits + symbols
      while (result.length < length) result.push(randFrom(all))
      for (let i = result.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); const t = result[i]; result[i] = result[j]; result[j] = t }
      return result.join('')
    },
    canEditPermissions(item) {
      const meRole = Number(this.currentRole)
      const targetRole = Number(item?.role ?? item?.user?.role)
      const meId = this.currentUserId
      const targetId = Number(item?.id ?? item?.user?.id)
      if (targetRole === 0) return false
      if (meRole === 0) return true
      if (meRole === 1) {
        // Admin không thể sửa quyền của chính mình và admin khác
        if (meId != null && targetId != null && Number(meId) === Number(targetId)) return false
        return targetRole !== 1
      }
      return false
    },
    canToggleLock(item) {
      const meRole = Number(this.currentRole)
      const meId = this.currentUserId
      const targetId = Number(item?.id ?? item?.user?.id)
      // Admin không thể tự khóa/mở khóa chính mình
      if (meRole === 1 && meId != null && targetId != null && Number(meId) === Number(targetId)) return false
      return true
    },
    canResetPassword(item) {
      const meRole = Number(this.currentRole)
      const meId = this.currentUserId
      const targetId = Number(item?.id ?? item?.user?.id)
      const targetRole = Number(item?.role ?? item?.user?.role)
      // Admin có thể reset mật khẩu cho chính mình và nhân viên (đối tượng không phải admin)
      if (meRole === 1) {
        const isSelf = meId != null && targetId != null && Number(meId) === Number(targetId)
        const isTargetAdmin = targetRole === 1
        return isSelf || !isTargetAdmin
      }
      return true
    },
    getPermissionCategoryName(p) {
      try {
        const catObj = p?.permissionCategory || p?.category
        const nameFromObj = catObj?.displayName || catObj?.name
        if (nameFromObj && String(nameFromObj).trim()) return String(nameFromObj).trim()
        const catId = (catObj && catObj.id != null) ? catObj.id : (p?.categoryId ?? p?.category_id ?? p?.permissionCategoryId ?? p?.permission_category_id ?? p?.category)
        if (catId != null && this.categoryById[catId]?.name) return this.categoryById[catId].name
        return 'Khác'
      } catch { return 'Khác' }
    },
    async sendInfo(item) {
      try {
        await axios.post(`/setting/members/${item.id}/send-credentials`)
        this.$toastr.success('Đã gửi thông tin tài khoản tới email thành viên')
      } catch (e) {
        this.$toastr.error('Không thể gửi thông tin tài khoản')
      }
    },
    canSendInfo(item) {
      // Chỉ gửi khi thành viên có email
      const email = item.email || item.user?.email
      return !!email
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
/* Tái dùng kiểu hộp thoại role cho hộp thoại phân quyền */
.members .role-modal-content { border-radius: 14px; overflow: hidden; border: 1px solid #eef0f6; box-shadow: 0 10px 30px rgba(18, 38, 63, 0.08); }
.members .role-modal-header { background: linear-gradient(180deg, #ffffff 0%, #f7f9fc 100%); border-bottom: 1px solid #ecf0f6; }
.members .role-modal-body { background: #ffffff; max-height: 72vh; overflow: auto; padding-bottom: 8px; }
.members .section-card { border: 1px solid #e8e8e8; border-radius: 12px; padding: 12px 14px; background: #fff; box-shadow: 0 1px 2px rgba(0,0,0,0.02); }
.members .perm-group { border: 1px solid #e8e8e8; border-radius: 12px; }
.members .perm-group-header { border-bottom: 1px dashed #ecf0f6; padding-bottom: 6px; margin-bottom: 8px; }
.member-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>
