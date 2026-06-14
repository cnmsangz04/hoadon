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

      <b-pagination
        v-if="list.total > list.per_page"
        v-model="list.current_page"
        :per-page="list.per_page"
        :total-rows="list.total"
        align="right"
        class="mt-2 mb-0"
        @change="onPageChange"
      />
    </b-card>

    <b-modal
      id="companyModal"
      ref="companyModal"
      :title="companyForm.id ? 'Cập nhật công ty' : 'Thêm công ty mới'"
      hide-footer
      size="lg"
      modal-class="company-modal"
      header-class="company-modal-header"
      body-class="company-modal-body"
    >
      <b-form @submit.prevent="saveCompany" class="company-form">
        <div class="form-section">
          <div class="section-header">
            <i class="fas fa-building text-primary"></i>
            <h6 class="mb-0">Thông tin công ty</h6>
          </div>

          <b-row>
            <b-col cols="12">
              <b-form-group label="Tên công ty" label-for="name" class="required-field">
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-building"></i>
                  </b-input-group-prepend>
                  <b-form-input id="name" v-model.trim="companyForm.name" required placeholder="Nhập tên công ty" />
                </b-input-group>
              </b-form-group>
            </b-col>

            <b-col cols="12" md="6">
              <b-form-group label="Mã số thuế" label-for="taxcode" class="required-field">
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-hashtag"></i>
                  </b-input-group-prepend>
                  <b-form-input id="taxcode" v-model.trim="companyForm.taxcode" required placeholder="Nhập mã số thuế" />
                </b-input-group>
              </b-form-group>
            </b-col>

            <b-col cols="12" md="6">
              <b-form-group label="Trạng thái" label-for="status">
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-toggle-on"></i>
                  </b-input-group-prepend>
                  <b-form-select id="status" v-model="companyForm.status" :options="statusOptions" />
                </b-input-group>
              </b-form-group>
            </b-col>

            <b-col cols="12">
              <b-form-group label="Địa chỉ" label-for="address">
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-map-marker-alt"></i>
                  </b-input-group-prepend>
                  <b-form-input id="address" v-model.trim="companyForm.address" placeholder="Nhập địa chỉ công ty" />
                </b-input-group>
              </b-form-group>
            </b-col>
          </b-row>
        </div>

        <div class="form-section">
          <div class="section-header">
            <i class="fas fa-address-book text-info"></i>
            <h6 class="mb-0">Thông tin liên hệ</h6>
          </div>

          <b-row>
            <b-col cols="12" md="6">
              <b-form-group label="Email" label-for="email">
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-envelope"></i>
                  </b-input-group-prepend>
                  <b-form-input id="email" v-model.trim="companyForm.email" type="email" placeholder="contact@example.com" />
                </b-input-group>
              </b-form-group>
            </b-col>

            <b-col cols="12" md="6">
              <b-form-group label="Hotline" label-for="hotline">
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-phone"></i>
                  </b-input-group-prepend>
                  <b-form-input id="hotline" v-model.trim="companyForm.hotline" placeholder="1900 xxxx" />
                </b-input-group>
              </b-form-group>
            </b-col>
          </b-row>
        </div>

        <div class="form-actions">
          <b-button type="button" variant="secondary" size="sm" class="btn-modal-cancel" @click="$refs.companyModal.hide()">
            <i class="fas fa-times mr-1"></i>
            Hủy
          </b-button>
          <b-button type="submit" variant="primary" size="sm" class="btn-modal-save">
            <i class="fas fa-save mr-1"></i>
            {{ companyForm.id ? 'Cập nhật' : 'Thêm mới' }}
          </b-button>
        </div>
      </b-form>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'CompanyList',
  data() {
    return {
      isBusy: false,
      filter: {
        keyword: null,
        status: null,
        companyId: null,
      },
      companyForm: this.emptyCompanyForm(),
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
        this.list.total = res.data.totalElements
        this.list.current_page = res.data.number + 1
        return res.data.content || []
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
      this.list.current_page = page
      this.$refs.tblCompany.refresh()
    },
    showModal() {
      this.companyForm = this.emptyCompanyForm()
      this.$refs.companyModal.show()
    },
    editCompany(item) {
      this.companyForm = { ...item }
      if (!this.companyForm.name && item.companyName) this.companyForm.name = item.companyName
      if (!this.companyForm.address && item.companyAddress) this.companyForm.address = item.companyAddress
      this.companyForm.status = this.normalizeStatus(this.companyForm.status, 1)
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

.company-modal .modal-dialog {
  max-width: 800px;
}

.company-modal-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom: none;
  padding: 1.25rem 1.5rem;
}

.company-modal-header .modal-title {
  font-weight: 600;
  font-size: 1.25rem;
}

.company-modal-header .close {
  color: white;
  opacity: 0.9;
  text-shadow: none;
}

.company-modal-body {
  padding: 1.5rem;
  background: #f8f9fa;
}

.company-form .form-section {
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
  margin-bottom: 1.25rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  border: 1px solid #e9ecef;
}

.company-form .section-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 2px solid #f0f0f0;
}

.company-form .section-header h6 {
  font-weight: 600;
  color: #2d3748;
}

.company-form label {
  font-weight: 500;
  color: #4a5568;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.company-form .required-field label::after {
  content: ' *';
  color: #e53e3e;
}

.company-form .input-group-text {
  background: #f7fafc;
  border-right: none;
  color: #718096;
  min-width: 40px;
  justify-content: center;
}

.company-form .input-group .form-control,
.company-form .input-group .custom-select {
  border-left: none;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding-top: 1rem;
  margin-top: 1.25rem;
  border-top: 1px solid #e2e8f0;
}

.form-actions .btn {
  font-weight: 500;
  border-radius: 0.35rem;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.btn-modal-cancel {
  background-color: #f8fafc;
  border-color: #e2e8f0;
  color: #4a5568;
}

@media (max-width: 768px) {
  .company-modal-body {
    padding: 1rem;
  }

  .form-actions {
    flex-direction: row-reverse;
  }

  .form-actions .btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
