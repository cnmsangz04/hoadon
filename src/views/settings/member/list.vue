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
        <b-button v-if="canSaveMembers" size="sm" variant="success" @click="openCreate">
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
        :fields="tableFields"
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
          <b-dropdown v-if="canShowActionMenu(item)" size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item v-if="canEditMember(item)" class="text-center" href="#" @click.prevent="openEdit(item)">Cập nhật</b-dropdown-item>
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
    <b-modal ref="memberModal" :title="form.id ? 'Cập nhật thành viên' : 'Thêm thành viên'" hide-footer @hidden="clearMemberErrors">
      <b-form novalidate @submit.prevent="saveMember">
        <!-- Hiển thị thông tin cho thành viên mới -->
        <b-form-group label="Họ và tên" :state="memberState('fullName')">
          <b-form-input v-model.trim="form.fullName" required :state="memberState('fullName')" />
          <b-form-invalid-feedback :state="memberState('fullName')">
            {{ memberFeedback('fullName') }}
          </b-form-invalid-feedback>
        </b-form-group>
        <b-form-group label="Điện thoại" :state="memberState('phone')">
          <b-form-input v-model.trim="form.phone" type="tel" :state="memberState('phone')" />
          <b-form-invalid-feedback :state="memberState('phone')">
            {{ memberFeedback('phone') }}
          </b-form-invalid-feedback>
        </b-form-group>
        <b-form-group label="Email" :state="memberState('email')">
          <b-form-input v-model.trim="form.email" type="email" :state="memberState('email')" />
          <b-form-invalid-feedback :state="memberState('email')">
            {{ memberFeedback('email') }}
          </b-form-invalid-feedback>
        </b-form-group>
        <b-form-row>
          <b-col md="6">
            <b-form-group label="Mật khẩu" :state="memberState('password') !== null ? memberState('password') : passwordState">
              <b-input-group>
                <b-form-input
                  v-model="form.password"
                  type="password"
                  :required="!form.id"
                  :state="memberState('password') !== null ? memberState('password') : passwordState"
                />
                <b-input-group-append>
                  <b-button size="sm" variant="outline-secondary" @click="onGeneratePassword">Tạo</b-button>
                </b-input-group-append>
              </b-input-group>
              <b-form-invalid-feedback :state="memberState('password') !== null ? memberState('password') : passwordState">
                {{ memberFeedback('password') || 'Mật khẩu tối thiểu 8 ký tự' }}
              </b-form-invalid-feedback>
            </b-form-group>
          </b-col>
          <b-col md="6">
            <b-form-group label="Nhập lại mật khẩu" :state="memberState('passwordConfirm') !== null ? memberState('passwordConfirm') : passwordState">
              <b-form-input
                v-model="form.passwordConfirm"
                type="password"
                :required="!form.id"
                :state="memberState('passwordConfirm') !== null ? memberState('passwordConfirm') : passwordState"
              />
              <b-form-invalid-feedback :state="memberState('passwordConfirm') !== null ? memberState('passwordConfirm') : passwordState">
                {{ memberFeedback('passwordConfirm') || 'Mật khẩu không khớp' }}
              </b-form-invalid-feedback>
            </b-form-group>
          </b-col>
        </b-form-row>

        <!-- Chỉ root được đặt vai trò Admin -->
        <b-form-group v-if="canAssignAdminRole && !isEditingRootTarget" label="Đặt user này là Admin">
          <b-form-checkbox v-model="form.isAdmin">Đặt làm Admin</b-form-checkbox>
        </b-form-group>

        <b-form-row v-if="showAdminPasswordFields">
          <b-col md="6">
            <b-form-group label="Mật khẩu quản trị" :state="memberState('adminPassword') !== null ? memberState('adminPassword') : adminPasswordState">
              <b-input-group>
                <b-form-input
                  v-model.trim="form.adminPassword"
                  type="password"
                  :state="memberState('adminPassword') !== null ? memberState('adminPassword') : adminPasswordState"
                />
                <b-input-group-append>
                  <b-button size="sm" variant="outline-secondary" @click="onGenerateAdminPassword">Tạo</b-button>
                </b-input-group-append>
              </b-input-group>
              <b-form-invalid-feedback :state="memberState('adminPassword') !== null ? memberState('adminPassword') : adminPasswordState">
                {{ memberFeedback('adminPassword') || 'Mật khẩu quản trị tối thiểu 8 ký tự' }}
              </b-form-invalid-feedback>
            </b-form-group>
          </b-col>
          <b-col md="6">
            <b-form-group label="Nhập lại mật khẩu quản trị" :state="memberState('adminPasswordConfirm') !== null ? memberState('adminPasswordConfirm') : adminPasswordState">
              <b-form-input
                v-model.trim="form.adminPasswordConfirm"
                type="password"
                :state="memberState('adminPasswordConfirm') !== null ? memberState('adminPasswordConfirm') : adminPasswordState"
              />
              <b-form-invalid-feedback :state="memberState('adminPasswordConfirm') !== null ? memberState('adminPasswordConfirm') : adminPasswordState">
                {{ memberFeedback('adminPasswordConfirm') || 'Mật khẩu quản trị không khớp' }}
              </b-form-invalid-feedback>
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
      size="xl"
      content-class="role-modal-content"
      header-class="role-modal-header"
      body-class="role-modal-body"
      footer-class="role-modal-footer"
    >
      <div class="role-modal-shell">
        <div class="role-user-panel">
          <div class="role-user-avatar">
            <i class="fas fa-user-shield"></i>
          </div>
          <div class="role-user-copy">
            <div class="role-user-label">Thành viên</div>
            <div class="role-user-name">{{ permForm.name || permForm.username || '—' }}</div>
            <div class="role-user-meta">
              <span><i class="fas fa-user mr-1"></i>{{ permForm.username || '—' }}</span>
              <span><i class="fas fa-id-badge mr-1"></i>{{ roleCodeToText({ role: permForm.role }) }}</span>
              <span>
                <i class="fas fa-circle mr-1"></i>
                {{ Number(permForm.status) === 1 ? 'Đang hoạt động' : 'Đang khóa' }}
              </span>
            </div>
          </div>
          <div class="role-user-summary">
            <strong>{{ permSelectedCount }}</strong>
            <span>/ {{ permVisiblePermissions.length }} quyền bật</span>
          </div>
        </div>

        <div class="role-toolbar">
          <div>
            <h6><i class="fas fa-key mr-1"></i> Danh sách quyền</h6>
            <p>{{ permHelpText }}</p>
          </div>
          <div class="role-toolbar-actions">
            <b-button size="sm" variant="outline-primary" @click="permSelectAll(true)">
              <i class="fas fa-check-double mr-1"></i>
              Chọn tất cả
            </b-button>
            <b-button size="sm" variant="outline-secondary" @click="permSelectAll(false)">
              Bỏ chọn
            </b-button>
          </div>
        </div>

        <div v-if="!permGroupedPermissions.length" class="role-empty">
          <i class="fas fa-key"></i>
          <span>Không có quyền phù hợp để hiển thị</span>
        </div>

        <div v-else class="role-permission-grid">
          <section v-for="group in permGroupedPermissions" :key="group.categoryName" class="perm-group">
            <div class="perm-group-header">
              <div>
                <strong>{{ group.categoryName }}</strong>
                <span>{{ permGroupSelectedCount(group) }}/{{ group.items.length }} quyền bật</span>
              </div>
              <b-button size="sm" variant="link" @click="togglePermGroup(group)">
                {{ group._allSelected ? 'Bỏ chọn nhóm' : 'Chọn nhóm' }}
              </b-button>
            </div>
            <div class="perm-group-items">
              <div
                v-for="perm in group.items"
                :key="perm.id"
                class="perm-row"
                :class="{ active: getPermEffectiveChecked(perm.id) }"
              >
                <div class="perm-info">
                  <div class="perm-name">{{ perm.displayName || perm.name || ('#' + perm.id) }}</div>
                  <div class="perm-code">{{ perm.code || perm.key || '—' }}</div>
                  <b-badge variant="light">Cấp {{ perm.level }}</b-badge>
                </div>
                <div class="perm-actions">
                  <b-form-checkbox
                    :checked="getPermEffectiveChecked(perm.id)"
                    switch
                    @change="onPermCheckboxChange(perm.id, $event)"
                  />
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>

      <template #modal-footer>
        <div class="role-modal-footer-inner">
          <b-button variant="light" @click="$refs.permissionModal.hide()">Đóng</b-button>
          <b-button :disabled="!canSavePermissions" variant="primary" @click="savePermissions">
            <i class="fas fa-save mr-1"></i>
            Lưu phân quyền
          </b-button>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import { pageFrom, pageItems, pageLast, pageNumber, pageSize, pageTo, pageTotal } from '@/utils/pagination'
import PaginationBar from '@/views/components/pagination_bar.vue'
import { parseJwt } from '@/utils/jwt'
import { email, phone, required } from '@/utils/validators'

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
      abilities: {
        canList: false,
        canSave: false,
        canManage: false
      },
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
        companyId: null,
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
      memberErrors: {},
      permForm: { userId: null, companyId: null, username: '', name: '', role: null, status: null, email: '', phone: '', adminScope: '' },
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
    canListMembers() {
      return this.abilities.canList || this.isRoot
    },
    canSaveMembers() {
      return this.abilities.canSave || this.isRoot
    },
    canManageMembers() {
      return this.abilities.canManage || this.isRoot
    },
    tableFields() {
      if (this.canSaveMembers || this.canManageMembers) return this.fields
      return this.fields.filter(field => field.key !== 'option')
    },
    formCompanyId() {
      const cid = this.form.companyId ?? this.currentCompanyId
      const n = Number(cid)
      return Number.isFinite(n) ? n : undefined
    },
    canAssignAdminRole() {
      return this.isRoot
    },
    isEditingSelfTarget() {
      if (!this.form.id) return false
      const meId = this.currentUserId
      return meId != null && Number(meId) === Number(this.form.id)
    },
    showAdminPasswordFields() {
      return this.form.isAdmin && this.formCompanyId === 1 && !this.isEditingRootTarget && (this.canAssignAdminRole || this.isEditingSelfTarget)
    },
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
      // Chỉ validate khi trường mật khẩu quản trị đang hiển thị.
      if (!this.showAdminPasswordFields) return null
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
      const adminPwdOk = !this.showAdminPasswordFields ? true : (!adminPwdProvided ? true : (this.adminPasswordState === true))
      return !!this.form.fullName && passOk && adminPwdOk
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
      const targetRole = Number(this.permForm?.role)
      const targetCompanyId = Number(this.permForm?.companyId)
      if (targetRole === 1 && targetCompanyId === 1) {
        return perms.filter(p => Number(p.level) !== 0)
      }
      if (targetRole === 1 && targetCompanyId !== 1) {
        return []
      }
      if (targetRole === 2) {
        perms = perms.filter(p => Number(p.level) === 0)
      }
      return perms
    },
    permHelpText() {
      const targetRole = Number(this.permForm?.role)
      const targetCompanyId = Number(this.permForm?.companyId)
      if (targetRole === 1 && targetCompanyId === 1) {
        return 'Tài khoản quản trị đã có toàn bộ quyền user cấp 0; chỉ cấu hình quyền quản trị.'
      }
      if (targetRole === 2) {
        return 'Tích chọn những quyền cấp 0 mà nhân viên được sử dụng trong hệ thống.'
      }
      return 'Không có quyền phù hợp để cấu hình cho tài khoản này.'
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
    permSelectedCount() {
      return this.permVisiblePermissions.filter(p => this.getPermEffectiveChecked(Number(p.id))).length
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

    await this.loadAbilities()
    if (this.canSaveMembers) {
      await Promise.all([this.loadCategories(), this.loadAllPermissions()])
    }
    await this.loadData()
  },
  methods: {
    async loadAbilities() {
      try {
        const { data } = await axios.get('/setting/members/abilities', {
          meta: { suppressGlobalErrorToast: true }
        })
        this.abilities = {
          canList: data?.canList === true,
          canSave: data?.canSave === true,
          canManage: data?.canManage === true
        }
      } catch {
        this.abilities = { canList: false, canSave: false, canManage: false }
      }
    },
    resolveItemCompanyId(item) {
      const cid = item?.companyId ?? item?.company_id ?? item?.user?.companyId ?? item?.user?.company_id ?? this.currentCompanyId
      const n = Number(cid)
      return Number.isFinite(n) ? n : undefined
    },
    isRootCompanyAdminAccount(item) {
      const role = Number(item?.role ?? item?.user?.role)
      const companyId = this.resolveItemCompanyId(item)
      return role === 1 && companyId === 1
    },
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
        const res = await axios.get('/setting/members/permission-catalog')
        const data = res?.data
        this.allPermissions = Array.isArray(data?.content) ? data.content : (Array.isArray(data) ? data : [])
      } catch {
        this.allPermissions = []
      }
    },
    async loadCategories() {
      try {
        const res = await axios.get('/setting/members/permission-categories')
        // Phía backend đã sắp xếp theo orderIndex (sothutu), nhưng vẫn thêm lớp bảo vệ
        const cats = Array.isArray(res.data) ? res.data : (res.data?.content || [])
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
          page: pageZero,
          size: this.list.per_page
        }
        const res = await axios.post('/setting/members/list', null, { params })
        const d = res?.data || {}
        // Chuẩn hóa key dữ liệu để phòng trường hợp lệch
        this.list.data = pageItems(d)
        this.list.total = pageTotal(d)
        this.list.per_page = pageSize(d, this.list.per_page)
        this.list.current_page = pageNumber(d, Number(params.page) + 1)
        this.list.last_page = pageLast(d, this.list.per_page)
        this.list.from = pageFrom(d, this.list.current_page, this.list.per_page)
        const numberOfElements = Array.isArray(this.list.data) ? this.list.data.length : 0
        this.list.to = pageTo(d, numberOfElements, this.list.current_page, this.list.per_page)

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
      if (!this.canSaveMembers) return
      this.clearMemberErrors()
      this.form = { id: null, companyId: this.currentCompanyId || 1, username: '', fullName: '', phone: '', email: '', password: '', passwordConfirm: '', isAdmin: false, adminPassword: '', adminPasswordConfirm: '' }
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
      if (meRole === 2 && targetRole !== 2) {
        // Nhân viên được cấp quyền thành viên chỉ thao tác trên nhóm nhân viên cùng công ty.
        return false
      }
      return true
    },
    canShowActionMenu(item) {
      return this.canEditMember(item) ||
        this.canEditPermissions(item) ||
        this.canSendInfo(item) ||
        this.canToggleLock(item) ||
        this.canResetPassword(item)
    },
    canEditMember(item) {
      return this.canSaveMembers && this.canOperateOn(item)
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
      if (!this.canEditMember(item)) return
      this.clearMemberErrors()
      this.form = {
        id: item.id,
        companyId: this.resolveItemCompanyId(item),
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
    clearMemberErrors() {
      this.memberErrors = {}
    },
    memberState(field) {
      return Object.prototype.hasOwnProperty.call(this.memberErrors, field) ? false : null
    },
    memberFeedback(field) {
      const value = this.memberErrors[field]
      return Array.isArray(value) ? value.join(' ') : (value || '')
    },
    validateMemberForm() {
      const errors = {}
      const fullNameError = required(this.form.fullName, 'Vui lòng nhập họ và tên')
      if (fullNameError) errors.fullName = [fullNameError]

      const phoneError = phone(this.form.phone)
      if (phoneError) errors.phone = [phoneError]

      const emailError = email(this.form.email)
      if (emailError) errors.email = [emailError]

      const password = this.form.password || ''
      const passwordConfirm = this.form.passwordConfirm || ''
      if (!this.form.id && !password) {
        errors.password = ['Vui lòng nhập mật khẩu']
      } else if (password && password.length < 8) {
        errors.password = ['Mật khẩu tối thiểu 8 ký tự']
      }
      if ((!this.form.id || password || passwordConfirm) && password !== passwordConfirm) {
        errors.passwordConfirm = ['Mật khẩu không khớp']
      }

      if (this.showAdminPasswordFields) {
        const adminPassword = this.form.adminPassword || ''
        const adminPasswordConfirm = this.form.adminPasswordConfirm || ''
        if ((adminPassword || adminPasswordConfirm) && adminPassword.length < 8) {
          errors.adminPassword = ['Mật khẩu quản trị tối thiểu 8 ký tự']
        }
        if ((adminPassword || adminPasswordConfirm) && adminPassword !== adminPasswordConfirm) {
          errors.adminPasswordConfirm = ['Mật khẩu quản trị không khớp']
        }
      }

      this.memberErrors = errors
      return Object.keys(errors).length === 0
    },
    openPermission(item) {
      if (!this.canEditPermissions(item)) return
      this.permForm = {
        userId: Number(item.id ?? item.user?.id),
        companyId: this.resolveItemCompanyId(item),
        username: item.username || item.user?.username || '',
        name: item.name || item.user?.name || '',
        role: Number(item.role ?? item.user?.role),
        status: Number(item.status ?? item.user?.status),
        email: item.email || item.user?.email || '',
        phone: item.phone || item.user?.phone || '',
        adminScope: item.adminScope || item.admin_scope || item.user?.adminScope || item.user?.admin_scope || ''
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
    permGroupSelectedCount(group) {
      return (group.items || []).filter(p => this.getPermEffectiveChecked(Number(p.id))).length
    },
    permSelectAll(flag) { 
      for (const p of this.permVisiblePermissions) {
        this.$set(this.permOverrides, Number(p.id), flag ? 1 : 0)
      }
    },
    async savePermissions() {
      if (!this.canSavePermissions || !this.canSaveMembers) return
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
      if (!this.canSaveMembers) return
      if (!this.validateMemberForm()) return
      if (this.form.id) {
        const target = this.list.data.find(x => x.id === this.form.id)
        const targetRole = Number(target?.role ?? target?.user?.role)
        const meRole = this.currentRole
        const isSelf = this.currentUserId != null && Number(this.currentUserId) === Number(this.form.id)
        if (meRole === 1 && (targetRole === 0 || targetRole === 1) && !isSelf) {
          window.alert('Bạn không có quyền chỉnh cập nhật tài khoản Root/Admin')
          return
        }
      }
      const isEditingRoot = this.form.id && Number(this.list.data.find(x => x.id === this.form.id)?.role) === 0
      const rolePayload = isEditingRoot ? undefined : (this.form.isAdmin ? 1 : 2)
      const payload = {
        id: this.form.id || undefined,
        username: this.form.username || undefined,
        name: (this.form.fullName || '').trim(),
        phone: (this.form.phone || '').trim() || undefined,
        email: (this.form.email || '').trim() || undefined,
        password: this.form.password ? this.form.password : (this.form.id ? undefined : this.form.password),
        role: rolePayload,
        adminPassword: (this.showAdminPasswordFields && this.form.adminPassword) ? this.form.adminPassword : undefined
      }
      await axios.post('/setting/members/saveOrUpdate', payload)
      this.$toastr.success(this.form.id ? 'Cập nhật thành viên thành công' : 'Thêm thành viên thành công')
      this.$refs.memberModal.hide()
      this.loadData()
    },
    async toggleLock(item) {
      if (!this.canToggleLock(item)) return
      await axios.post(`/setting/members/${item.id}/lock`, null, { params: { lock: Number(item.status) === 1 ? 1 : 0 } })
      this.$toastr.success(Number(item.status) === 1 ? 'Đã khóa tài khoản' : 'Đã mở khóa tài khoản')
      this.loadData()
    },
    async resetPassword(item) {
      if (!this.canResetPassword(item)) return
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
      if (!this.canSaveMembers) return false
      const targetRole = Number(item?.role ?? item?.user?.role)
      const meId = this.currentUserId
      const targetId = Number(item?.id ?? item?.user?.id)
      if (targetRole === 0) return false
      if (targetRole === 1 && !this.isRootCompanyAdminAccount(item)) return false
      if (meId != null && targetId != null && Number(meId) === Number(targetId)) return false
      return this.canOperateOn(item)
    },
    canToggleLock(item) {
      if (!this.canManageMembers || !this.canOperateOn(item)) return false
      const meRole = Number(this.currentRole)
      const meId = this.currentUserId
      const targetId = Number(item?.id ?? item?.user?.id)
      // Admin không thể tự khóa/mở khóa chính mình
      if (meRole === 1 && meId != null && targetId != null && Number(meId) === Number(targetId)) return false
      return true
    },
    canResetPassword(item) {
      if (!this.canManageMembers || !this.canOperateOn(item)) return false
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
      if (!this.canSendInfo(item)) return
      try {
        await axios.post(`/setting/members/${item.id}/send-credentials`)
        this.$toastr.success('Đã gửi thông tin tài khoản tới email thành viên')
      } catch (e) {
        this.$toastr.error('Không thể gửi thông tin tài khoản')
      }
    },
    canSendInfo(item) {
      if (!this.canManageMembers || !this.canOperateOn(item)) return false
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
.member-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>
