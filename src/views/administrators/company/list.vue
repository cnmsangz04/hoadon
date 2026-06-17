<template>
  <div class="container-fluid py-3 permissions company-list-page">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Danh sách công ty</h4>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="onFilter">
          <i class="fas fa-sync-alt mr-1"></i>
          Làm mới
        </b-button>
        <b-button v-if="canCreateCompany" size="sm" variant="success" @click="showModal">
          <i class="fas fa-plus mr-1"></i>
          Thêm công ty
        </b-button>
      </div>
    </div>

    <b-card class="mb-3 shadow-sm filter-card">
      <b-row>
        <b-col md="4" class="mb-2 mb-md-0">
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input
              v-model.trim="filter.keyword"
              placeholder="Tìm theo MST / Tên / Địa chỉ"
              @keyup.enter="onFilter"
            />
          </b-input-group>
        </b-col>
        <b-col md="4" class="mb-2 mb-md-0">
          <b-form-select v-model="filter.status" :options="statusOptionsWithAll" />
        </b-col>
        <b-col md="4" class="text-right">
          <b-button size="sm" variant="primary" @click="onFilter">Tìm kiếm</b-button>
        </b-col>
      </b-row>
    </b-card>

    <b-card class="shadow-sm">
      <b-table
        ref="tblCompany"
        bordered
        hover
        responsive
        small
        show-empty
        :fields="fields"
        :items="dataProvider"
        :busy.sync="isBusy"
        :per-page="list.per_page"
        :current-page="list.current_page"
        empty-text="Không có dữ liệu"
        class="mb-0 table-modern table-compact"
      >
        <template #cell(id)="data">
          {{ safeRowNumber(data.index) }}
        </template>

        <template #cell(company)="data">
          <div class="font-weight-bold">{{ data.item.name || data.item.companyName }} - {{ data.item.taxcode }}</div>
          <div class="text-muted small">{{ data.item.address || data.item.companyAddress }}</div>
        </template>

        <template #cell(email)="data">
          <div>{{ data.item.email }}</div>
          <div class="text-muted small" v-if="data.item.hotline">Hotline: {{ data.item.hotline }}</div>
        </template>

        <template #cell(status)="data">
          <b-badge :variant="companyStatusVariant(data.item)">
            {{ companyStatusText(data.item) }}
          </b-badge>
        </template>

        <template #cell(option)="data">
          <b-dropdown
            size="sm"
            right
            variant="link"
            toggle-class="text-decoration-none"
            no-caret
            boundary="window"
          >
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item v-if="canUpdateCompany" class="text-center" href="#" @click.prevent="editCompany(data.item)">Cập nhật</b-dropdown-item>
            <b-dropdown-item v-if="canUpdateCompany" class="text-center" href="#" @click.prevent="sendAdminInfo(data.item)">Gửi thông tin</b-dropdown-item>
            <b-dropdown-item v-if="canUpdateCompany" class="text-center text-success" href="#" @click.prevent="setCompanyStatus(data.item.id, 1)">Kích hoạt</b-dropdown-item>
            <b-dropdown-item v-if="canUpdateCompany" class="text-center text-info" href="#" @click.prevent="setCompanyStatus(data.item.id, 2)">Chưa kích hoạt</b-dropdown-item>
            <b-dropdown-item v-if="canUpdateCompany" class="text-center text-warning" href="#" @click.prevent="setCompanyStatus(data.item.id, 0)">Tạm ngưng</b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <pagination-bar
        :current.sync="list.current_page"
        :size.sync="list.per_page"
        :total="list.total"
        :sizes="pageSizes"
        @page-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </b-card>

    <b-modal
      id="companyModal"
      ref="companyModal"
      :title="companyForm.id ? 'Cập nhật công ty' : 'Thêm công ty mới'"
      size="lg"
      modal-class="company-modal"
      body-class="company-modal-body"
      footer-class="company-modal-footer"
    >
      <b-form id="companyForm" novalidate @submit.prevent="saveCompany" class="company-form">
        <div class="form-section">
          <div class="section-header">
            <span class="section-icon">
              <i class="fas fa-building"></i>
            </span>
            <div>
              <h6>Thông tin công ty</h6>
              <p>Thông tin định danh và trạng thái hoạt động.</p>
            </div>
          </div>

          <b-row>
            <b-col cols="12">
              <b-form-group label="Tên công ty" label-for="name" class="required-field company-field" :state="companyState('name')">
                <b-form-input id="name" v-model.trim="companyForm.name" required placeholder="Nhập tên công ty" :state="companyState('name')" />
                <b-form-invalid-feedback :state="companyState('name')">
                  {{ companyFeedback('name') }}
                </b-form-invalid-feedback>
              </b-form-group>
            </b-col>

            <b-col cols="12" md="6">
              <b-form-group label="Mã số thuế" label-for="taxcode" class="required-field company-field" :state="companyState('taxcode')">
                <b-form-input id="taxcode" v-model.trim="companyForm.taxcode" required placeholder="Nhập mã số thuế" :state="companyState('taxcode')" />
                <b-form-invalid-feedback :state="companyState('taxcode')">
                  {{ companyFeedback('taxcode') }}
                </b-form-invalid-feedback>
              </b-form-group>
            </b-col>

            <b-col cols="12" md="6">
              <b-form-group label="Trạng thái" label-for="status" class="company-field">
                <b-form-select id="status" v-model="companyForm.status" :options="statusOptions" />
              </b-form-group>
            </b-col>

            <b-col cols="12">
              <b-form-group label="Địa chỉ" label-for="address" class="company-field">
                <b-form-input id="address" v-model.trim="companyForm.address" placeholder="Nhập địa chỉ công ty" />
              </b-form-group>
            </b-col>
          </b-row>
        </div>

        <div class="form-section">
          <div class="section-header">
            <span class="section-icon section-icon-info">
              <i class="fas fa-address-book"></i>
            </span>
            <div>
              <h6>Thông tin liên hệ</h6>
              <p>Email và số điện thoại dùng để liên hệ công ty.</p>
            </div>
          </div>

          <b-row>
            <b-col cols="12" md="6">
              <b-form-group label="Email" label-for="email" class="company-field" :state="companyState('email')">
                <b-form-input id="email" v-model.trim="companyForm.email" type="email" placeholder="contact@example.com" :state="companyState('email')" />
                <b-form-invalid-feedback :state="companyState('email')">
                  {{ companyFeedback('email') }}
                </b-form-invalid-feedback>
              </b-form-group>
            </b-col>

            <b-col cols="12" md="6">
              <b-form-group label="Hotline" label-for="hotline" class="company-field" :state="companyState('hotline')">
                <b-form-input id="hotline" v-model.trim="companyForm.hotline" type="tel" placeholder="1900 xxxx" :state="companyState('hotline')" />
                <b-form-invalid-feedback :state="companyState('hotline')">
                  {{ companyFeedback('hotline') }}
                </b-form-invalid-feedback>
              </b-form-group>
            </b-col>
          </b-row>
        </div>
      </b-form>
      <template #modal-footer>
        <div class="company-modal-actions">
          <b-button type="button" variant="light" size="sm" @click="$refs.companyModal.hide()">
            Hủy
          </b-button>
          <b-button type="submit" form="companyForm" variant="primary" size="sm">
            <i class="fas fa-save mr-1"></i>
            {{ companyForm.id ? 'Cập nhật' : 'Thêm mới' }}
          </b-button>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import { pageItems, pageNumber, pageTotal } from '@/utils/pagination'
import PaginationBar from '@/views/components/pagination_bar.vue'
import { email, required, taxCode } from '@/utils/validators'

export default {
  name: 'CompanyList',
  components: { PaginationBar },
  data() {
    return {
      isBusy: false,
      filter: {
        keyword: null,
        status: null,
        companyId: null,
      },
      companyForm: this.emptyCompanyForm(),
      companyErrors: {},
      statusOptions: [
        { value: 1, text: 'Kích hoạt' },
        { value: 0, text: 'Ngưng hoạt động' },
        { value: 2, text: 'Chưa kích hoạt' },
      ],
      list: {
        current_page: 1,
        per_page: 10,
        total: 0,
      },
      pageSizes: [10, 20, 50, 100],
      fields: [
        { key: 'id', label: '#', thStyle: { width: '50px' }, class: 'text-center' },
        { key: 'company', label: 'Tên công ty / Địa chỉ' },
        { key: 'email', label: 'Email' },
        { key: 'status', label: 'Trạng thái', class: 'text-center' },
        { key: 'option', label: 'Thao tác', thStyle: { width: '80px' }, class: 'text-center' },
      ],
      canCreateCompany: true,
      canUpdateCompany: true,
      canDeleteCompany: true,
    }
  },
  computed: {
    statusOptionsWithAll() {
      return [
        { value: null, text: 'Tất cả trạng thái' },
        ...this.statusOptions,
      ]
    },
  },
  methods: {
    emptyCompanyForm() {
      return {
        id: null,
        name: '',
        address: '',
        companyName: '',
        companyAddress: '',
        email: '',
        hotline: '',
        taxcode: '',
        status: 1,
      }
    },
    dataProvider(ctx) {
      this.isBusy = true
      return axios.post('/administrator/company/list', this.filter, {
        params: { page: ctx.currentPage - 1, size: this.list.per_page },
      }).then(res => {
        this.isBusy = false
        this.list.total = pageTotal(res.data)
        this.list.current_page = pageNumber(res.data, ctx.currentPage)
        return pageItems(res.data)
      }).catch(err => {
        this.isBusy = false
        const message = err.response?.data?.message || 'Không thể tải danh sách công ty'
        this.$toastr && this.$toastr.error(message)
        return []
      })
    },
    onFilter() {
      this.list.current_page = 1
      this.$refs.tblCompany.refresh()
    },
    onPageChange(page) {
      this.list.current_page = Number(page) || 1
      this.$refs.tblCompany.refresh()
    },
    onPageSizeChange(size) {
      this.list.per_page = Number(size) || this.list.per_page
      this.list.current_page = 1
      this.$refs.tblCompany.refresh()
    },
    showModal() {
      this.companyForm = this.emptyCompanyForm()
      this.companyErrors = {}
      this.$refs.companyModal.show()
    },
    editCompany(item) {
      this.companyForm = { ...item }
      if (!this.companyForm.name && item.companyName) this.companyForm.name = item.companyName
      if (!this.companyForm.address && item.companyAddress) this.companyForm.address = item.companyAddress
      this.companyForm.status = this.normalizeStatus(this.companyForm.status, 1)
      this.companyErrors = {}
      this.$refs.companyModal.show()
    },
    normalizeStatus(status, fallback = 0) {
      if (status === 'active') return 1
      if (status === 'inactive') return 0
      const n = Number(status)
      return [0, 1, 2].includes(n) ? n : fallback
    },
    companyStatusValue(item) {
      return this.normalizeStatus(item && item.status, 0)
    },
    companyStatusText(item) {
      const s = this.companyStatusValue(item)
      if (s === 1) return 'Kích hoạt'
      if (s === 2) return 'Chưa kích hoạt'
      return 'Ngưng hoạt động'
    },
    companyStatusVariant(item) {
      const s = this.companyStatusValue(item)
      if (s === 1) return 'success'
      if (s === 2) return 'warning'
      return 'secondary'
    },
    saveCompany() {
      if (!this.validateCompanyForm()) return
      const payload = { ...this.companyForm }
      if (payload.companyName && !payload.name) payload.name = payload.companyName
      if (payload.companyAddress && !payload.address) payload.address = payload.companyAddress
      payload.name = (payload.name || '').trim()
      payload.address = (payload.address || '').trim()
      payload.email = (payload.email || '').trim()
      payload.taxcode = (payload.taxcode || '').trim()
      payload.hotline = payload.hotline != null && String(payload.hotline).trim()
        ? String(payload.hotline).trim()
        : undefined
      payload.status = this.normalizeStatus(payload.status, 1)

      axios.post('/administrator/company/saveOrUpdate', payload)
        .then(() => {
          this.$toastr && this.$toastr.success(payload.id ? 'Cập nhật công ty thành công' : 'Thêm công ty thành công')
          this.$refs.companyModal.hide()
          this.$refs.tblCompany.refresh()
        })
        .catch(err => {
          const message = err.response?.data?.message || 'Không thể lưu thông tin công ty'
          this.$toastr && this.$toastr.error(message)
        })
    },
    validateCompanyForm() {
      const errors = {}
      const nameError = required(this.companyForm.name, 'Vui lòng nhập tên công ty')
      if (nameError) errors.name = [nameError]
      const taxcodeError = required(this.companyForm.taxcode, 'Vui lòng nhập mã số thuế') || taxCode(this.companyForm.taxcode)
      if (taxcodeError) errors.taxcode = [taxcodeError]
      const emailError = email(this.companyForm.email)
      if (emailError) errors.email = [emailError]
      const hotline = String(this.companyForm.hotline || '').trim()
      if (hotline && !/^[0-9+().\-\s]{6,20}$/.test(hotline)) {
        errors.hotline = ['Hotline không đúng định dạng']
      }
      this.companyErrors = errors
      return Object.keys(errors).length === 0
    },
    companyState(field) {
      return Object.prototype.hasOwnProperty.call(this.companyErrors, field) ? false : null
    },
    companyFeedback(field) {
      const value = this.companyErrors[field]
      return Array.isArray(value) ? value.join(' ') : (value || '')
    },
    setCompanyStatus(id, status) {
      axios.post(`/administrator/company/${id}/status`, { status })
        .then(() => {
          const message = status === 1 ? 'Đã kích hoạt công ty' : (status === 2 ? 'Đã chuyển công ty sang chưa kích hoạt' : 'Đã tạm ngưng công ty')
          this.$toastr && this.$toastr.success(message)
          this.$refs.tblCompany.refresh()
        })
        .catch(err => {
          const message = err.response?.data?.message || 'Không thể cập nhật trạng thái công ty'
          this.$toastr && this.$toastr.error(message)
        })
    },
    sendAdminInfo(item) {
      axios.post(`/administrator/company/${item.id}/send-credentials`)
        .then(res => {
          const message = res?.data?.message || 'Đã gửi thông tin tài khoản quản trị tới email'
          this.$toastr && this.$toastr.success(message)
        })
        .catch(err => {
          const message = err.response?.data?.message || 'Không thể gửi thông tin tài khoản quản trị'
          this.$toastr && this.$toastr.error(message)
        })
    },
    safeRowNumber(index) {
      const page = Number(this.list && this.list.current_page) || 1
      const size = Number(this.list && this.list.per_page) || 10
      const idx = Number(index)
      return (Number.isNaN(idx) ? 0 : idx) + 1 + (page - 1) * size
    },
  },
}
</script>

<style scoped>
.company-list-page {
  color: #2d3748;
}

.permissions .card.shadow-sm {
  border-radius: 10px;
  border: 1px solid #e9ecef;
}

.permissions .card-body {
  padding: 0.75rem 1rem;
}

.permissions .table-hover tbody tr:hover {
  background-color: #fafbfd;
}

.permissions .btn-outline-primary {
  border-color: #dfe7ff;
}

.permissions .btn-outline-primary:hover {
  background: #eef3ff;
}

.filter-card :deep(.input-group-text) {
  background: #f7fafc;
  border-color: #e2e8f0;
}

.filter-card :deep(.form-control),
.filter-card :deep(.custom-select) {
  border-color: #e2e8f0;
  min-height: 36px;
}

.table-modern thead th {
  background-color: #f9fafb;
  border-bottom: 2px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 1;
}

.table-compact td,
.table-compact th {
  padding: 0.5rem 0.75rem;
}

.table td {
  vertical-align: middle;
}

.badge {
  font-size: 13px;
}

.company-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.company-form .form-section {
  background: #fff;
  border: 1px solid #e6ebf2;
  border-radius: 10px;
  padding: 16px;
}

.company-form .section-header {
  align-items: flex-start;
  border-bottom: 1px solid #eef2f7;
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  padding-bottom: 12px;
}

.company-form .section-icon {
  align-items: center;
  background: #e8f0ff;
  border-radius: 10px;
  color: #2563eb;
  display: inline-flex;
  flex: 0 0 auto;
  height: 36px;
  justify-content: center;
  width: 36px;
}

.company-form .section-icon-info {
  background: #e6f7fb;
  color: #0891b2;
}

.company-form .section-header h6 {
  color: #1f2937;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.25;
  margin: 0;
}

.company-form .section-header p {
  color: #64748b;
  font-size: 12px;
  margin: 3px 0 0;
}

.company-form label {
  color: #334155;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: var(--ui-label-gap);
}

.company-form .required-field label::after {
  color: #dc2626;
  content: ' *';
}

.company-form :deep(.form-control),
.company-form :deep(.custom-select) {
  border-color: #d8dee8;
  border-radius: 8px;
  min-height: 36px;
}

.company-form :deep(.form-control:focus),
.company-form :deep(.custom-select:focus) {
  border-color: #7aa7f7;
  box-shadow: 0 0 0 3px rgba(122, 167, 247, 0.2);
}

.company-modal-actions {
  align-items: center;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  width: 100%;
}

.company-modal-actions .btn {
  min-width: 92px;
}

@media (max-width: 768px) {
  .company-form .form-section {
    padding: 14px;
  }
}

@media (max-width: 576px) {
  .company-modal-actions {
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .company-modal-actions .btn {
    width: 100%;
  }
}
</style>
