<template>
  <div class="container-fluid py-3 permissions company-registration-page">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Duyệt đăng ký</h4>
      <b-button size="sm" variant="outline-primary" @click="load">
        <i class="fas fa-sync-alt mr-1"></i>
        Làm mới
      </b-button>
    </div>

    <b-card class="mb-3 shadow-sm filter-card">
      <b-row>
        <b-col md="5" class="mb-2 mb-md-0">
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input
              v-model.trim="filter.keyword"
              placeholder="Tìm tên công ty, mã số thuế, email..."
              @keyup.enter="load"
            />
          </b-input-group>
        </b-col>
        <b-col md="4" class="mb-2 mb-md-0">
          <b-form-select v-model="filter.status" :options="statusOptions" />
        </b-col>
        <b-col md="3" class="text-right">
          <b-button size="sm" variant="primary" @click="load">
            <i class="fas fa-filter mr-1"></i>
            Tìm kiếm
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <b-card class="shadow-sm">
      <b-table
        :items="items"
        :fields="fields"
        :busy="loading"
        responsive
        bordered
        hover
        small
        show-empty
        empty-text="Không có hồ sơ đăng ký"
        class="mb-0 table-modern table-compact"
      >
        <template #table-busy>
          <div class="text-center py-4">
            <b-spinner small class="mr-2" />
            Đang tải...
          </div>
        </template>

        <template #cell(id)="data">
          {{ rowNumber(data.index) }}
        </template>

        <template #cell(company)="data">
          <div class="font-weight-bold">{{ data.item.companyName }}</div>
          <div class="text-muted small">MST: {{ data.item.taxcode }}</div>
          <div class="text-muted small">{{ data.item.address }}</div>
        </template>

        <template #cell(contact)="data">
          <div>{{ data.item.contactName || '-' }}</div>
          <div class="text-muted small">{{ data.item.phone || '-' }}</div>
        </template>

        <template #cell(email)="data">
          <span class="badge badge-success mr-1">To</span>{{ data.item.email }}
        </template>

        <template #cell(status)="data">
          <b-badge :variant="statusVariant(data.item.status)">
            {{ statusText(data.item.status) }}
          </b-badge>
        </template>

        <template #cell(createdAt)="data">
          {{ formatDate(data.item.createdAt) }}
        </template>

        <template #cell(reviewed)="data">
          <div>{{ data.item.reviewedByName || '-' }}</div>
          <div class="text-muted small">{{ formatDate(data.item.reviewedAt) }}</div>
        </template>

        <template #cell(option)="data">
          <b-dropdown
            right
            size="sm"
            variant="link"
            toggle-class="text-decoration-none"
            no-caret
            boundary="window"
          >
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item
              v-if="Number(data.item.status) === 0"
              class="text-center text-success"
              href="#"
              @click.prevent="approve(data.item)"
            >
              Duyệt đăng ký
            </b-dropdown-item>
            <b-dropdown-item
              v-if="Number(data.item.status) === 0"
              class="text-center text-danger"
              href="#"
              @click.prevent="reject(data.item)"
            >
              Từ chối
            </b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <pagination-bar
        :current.sync="page"
        :size.sync="size"
        :total="totalRows"
        :sizes="pageSizes"
        @page-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import { pageItems, pageTotal } from '@/utils/pagination'
import PaginationBar from '@/views/components/pagination_bar.vue'

export default {
  name: 'AdminCompanyRegistrationList',
  components: { PaginationBar },
  data() {
    return {
      loading: false,
      items: [],
      totalRows: 0,
      page: 1,
      size: 10,
      pageSizes: [10, 20, 50, 100],
      filter: {
        keyword: '',
        status: null,
      },
      statusOptions: [
        { value: null, text: 'Tất cả trạng thái' },
        { value: 0, text: 'Chờ duyệt' },
        { value: 1, text: 'Đã duyệt' },
        { value: 2, text: 'Từ chối' },
      ],
      fields: [
        { key: 'id', label: '#', class: 'text-center', thStyle: { width: '70px' } },
        { key: 'company', label: 'Thông tin công ty' },
        { key: 'contact', label: 'Người liên hệ', thStyle: { width: '180px' } },
        { key: 'email', label: 'Email nhận' },
        { key: 'status', label: 'Trạng thái', class: 'text-center', thStyle: { width: '120px' } },
        { key: 'createdAt', label: 'Ngày đăng ký', thStyle: { width: '160px' } },
        { key: 'reviewed', label: 'Duyệt bởi', thStyle: { width: '170px' } },
        { key: 'option', label: '', class: 'text-center', thStyle: { width: '70px' } },
      ],
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const { data } = await axios.post('/administrator/company-registration/list', this.filter, {
          params: { page: this.page - 1, size: this.size },
          meta: { suppressGlobalErrorToast: true },
        })
        this.items = pageItems(data)
        this.totalRows = pageTotal(data)
      } catch (e) {
        const msg = e?.response?.data?.message || 'Không thể tải danh sách đăng ký'
        this.$toastr && this.$toastr.error(msg)
      } finally {
        this.loading = false
      }
    },
    onPageChange(page) {
      this.page = Number(page) || 1
      this.load()
    },
    onPageSizeChange(size) {
      this.size = Number(size) || this.size
      this.page = 1
      this.load()
    },
    async approve(item) {
      if (!confirm(`Duyệt đăng ký cho ${item.companyName}?`)) return
      try {
        const { data } = await axios.post(`/administrator/company-registration/${item.id}/approve`, null, {
          meta: { suppressGlobalErrorToast: true },
        })
        this.$toastr && this.$toastr.success(data?.message || 'Đã duyệt đăng ký')
        this.load()
      } catch (e) {
        const msg = e?.response?.data?.message || 'Không thể duyệt đăng ký'
        this.$toastr && this.$toastr.error(msg)
      }
    },
    async reject(item) {
      if (!confirm(`Từ chối đăng ký cho ${item.companyName}?`)) return
      try {
        const { data } = await axios.post(`/administrator/company-registration/${item.id}/reject`, {}, {
          meta: { suppressGlobalErrorToast: true },
        })
        this.$toastr && this.$toastr.success(data?.message || 'Đã từ chối đăng ký')
        this.load()
      } catch (e) {
        const msg = e?.response?.data?.message || 'Không thể từ chối đăng ký'
        this.$toastr && this.$toastr.error(msg)
      }
    },
    rowNumber(index) {
      return Number(index || 0) + 1 + (this.page - 1) * this.size
    },
    statusText(status) {
      const s = Number(status)
      if (s === 1) return 'Đã duyệt'
      if (s === 2) return 'Từ chối'
      return 'Chờ duyệt'
    },
    statusVariant(status) {
      const s = Number(status)
      if (s === 1) return 'success'
      if (s === 2) return 'secondary'
      return 'warning'
    },
    formatDate(value) {
      if (!value) return ''
      try {
        return new Intl.DateTimeFormat('vi-VN', {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
        }).format(new Date(value))
      } catch {
        return value
      }
    },
  },
}
</script>

<style scoped>
.company-registration-page {
  color: #2d3748;
}

.permissions .card.shadow-sm {
  border-radius: 10px;
  border: 1px solid #e9ecef;
}

.permissions .card-body {
  padding: 0.75rem 1rem;
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

.filter-card :deep(.form-control:focus),
.filter-card :deep(.custom-select:focus) {
  border-color: #cbd5e0;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.1);
}

:deep(.table th) {
  background: #f7f9fc;
  border-bottom: 1px solid #ecf0f6;
  color: #2d3748;
  font-weight: 700;
  vertical-align: middle;
}

:deep(.table td) {
  vertical-align: middle;
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

.table-modern tbody tr:hover {
  background-color: #f6f8fa;
}

.badge {
  font-size: 13px;
}

:deep(.dropdown-menu) {
  border-radius: 8px;
  border-color: #e5e7eb;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.12);
}
</style>
