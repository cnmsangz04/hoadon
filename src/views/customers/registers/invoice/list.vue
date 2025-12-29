<template>
  <div class="container-fluid py-3 register-invoices">
    <!-- Header and actions -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center">
        <h4 class="mb-0 font-weight-bold">Danh sách tờ khai hóa đơn điện tử</h4>
      </div>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
        <b-button size="sm" variant="success" @click="openCreate">
          <i class="fas fa-file-alt"></i>
          Tạo tờ khai
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
            <b-form-input v-model.trim="filters.keyword" placeholder="Tìm theo mã hồ sơ / số thông báo / tax code / tên DN" @keyup.enter="applyFilters" />
          </b-input-group>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-select v-model="filters.declarationType" :options="declarationTypeOptions">
            <template #first>
              <b-form-select-option :value="null">Tất cả hình thức tờ khai</b-form-select-option>
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
      <b-row class="mt-2">
        <b-col md="3" class="mb-2">
          <b-form-datepicker v-model="filters.dateFrom" :max="filters.dateTo || undefined" placeholder="Từ ngày" size="sm" />
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-datepicker v-model="filters.dateTo" :min="filters.dateFrom || undefined" placeholder="Đến ngày" size="sm" />
        </b-col>
        <b-col md="6" class="mb-2 d-flex align-items-center justify-content-end">
          <b-button size="sm" variant="outline-secondary" class="mr-2" @click="resetFilters">Xóa lọc</b-button>
          <b-button size="sm" variant="outline-primary" @click="applyFilters">
            <i class="fas fa-filter"></i>
            Áp dụng
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <!-- Register invoices table -->
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

        <template #cell(form_pattern)="{ item }">
          <code>{{ item.form_pattern || '—' }}</code>
        </template>

        <template #cell(declaration_date)="{ item }">
          {{ formatDate(item.declaration_date) }}
        </template>

        <template #cell(declaration_type)="{ item }">
          <b-badge :variant="item.declaration_type == 2 ? 'warning' : 'info'">
            {{ item.declaration_type == 2 ? 'Thay đổi' : 'ĐK mới' }}
          </b-badge>
        </template>

        <template #cell(invoice_forms)="{ item }">
          <span class="text-muted" v-if="!item.invoice_forms">—</span>
          <span v-else>{{ summarizeInvoiceForms(item.invoice_forms) }}</span>
        </template>

        <template #cell(response_receive_file)="{ item }">
          <span>{{ item.response_receive_file || '—' }}</span>
        </template>

        <template #cell(response_accept_file)="{ item }">
          <span>{{ item.response_accept_file || '—' }}</span>
        </template>

        <template #cell(status)="{ item }">
          <b-badge :variant="statusVariant(item.status)">{{ statusText(item.status) }}</b-badge>
        </template>

        <template #cell(option)="{ item }">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret>
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="openEdit(item)">Cập nhật</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="sendToTaxAuthority(item)">Gửi CQT</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="downloadXml(item)">Download XML</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="openHistory(item)">Lịch sử truyền nhận</b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <!-- Loading skeleton when changing page -->
      <div v-if="isBusy" class="mt-2">
        <b-skeleton width="100%" height="20px" animated class="mb-2" />
        <b-skeleton width="96%" height="20px" animated class="mb-2" />
        <b-skeleton width="92%" height="20px" animated class="mb-2" />
      </div>

      <b-row class="mt-2">
        <b-col cols="6">
          <b-form inline>
            <b-form-select
              size="sm"
              class="d-inline-block mb-2 mr-2 pl-2 pr-4"
              v-model.number="list.per_page"
              :options="pageSizes"
              @input="onPageSizeChange"
            />
            <div class="pt-1 text-muted">
              <i class="fas fa-globe mr-1"></i> Hiển thị từ
              <b class="pl-1 pr-2">{{ list.from || 0 }}</b>
              đến
              <b class="pl-1 pr-2">{{ list.to || 0 }}</b>
              trong tổng số
              <b class="pl-1 pr-2">{{ list.total || 0 }}</b>
              bản ghi.
            </div>
          </b-form>
        </b-col>
        <b-col cols="6">
          <b-pagination
            align="right"
            v-model.number="list.current_page"
            :per-page="list.per_page"
            :total-rows="list.total"
            :hide-goto-end-buttons="true"
            v-if="list.last_page > 1"
            size="sm"
            pills
            @input="onPageChange"
          />
        </b-col>
      </b-row>
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'RegisterInvoiceList',
  data() {
    return {
      isBusy: false,
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
      pageSizes: [10, 20, 50, 100],
      filters: {
        keyword: '',
        declarationType: null,
        status: null,
        dateFrom: null,
        dateTo: null
      },
      fields: [
        { key: 'index', label: '#', thStyle: { width: '50px' } },
        { key: 'form_pattern', label: 'Mẫu số', thStyle: { width: '140px' } },
        { key: 'declaration_date', label: 'Ngày lập', thStyle: { width: '130px' } },
        { key: 'declaration_type', label: 'Hình thức tờ khai', thStyle: { width: '140px' } },
        { key: 'invoice_forms', label: 'Hình thức hóa đơn' },
        { key: 'response_receive_file', label: 'Thông báo tiếp nhận', thStyle: { width: '200px' } },
        { key: 'response_accept_file', label: 'Thông báo chấp nhận', thStyle: { width: '200px' } },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '120px' } },
        { key: 'option', label: 'Chức năng', thStyle: { width: '140px' } }
      ],
      declarationTypeOptions: [
        { value: 1, text: 'Đăng ký mới' },
        { value: 2, text: 'Thay đổi' }
      ],
      statusOptions: [
        { value: 0, text: 'Khởi tạo' },
        { value: 1, text: 'Đã tạo XML' },
        { value: 2, text: 'Đã ký' },
        { value: 3, text: 'Đã gửi' },
        { value: 4, text: 'Đã tiếp nhận' },
        { value: 5, text: 'Đã chấp nhận' },
        { value: 6, text: 'Từ chối / Lỗi' }
      ]
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    formatDate(d) {
      if (!d) return '—'
      try {
        const dt = new Date(d)
        const yyyy = dt.getFullYear()
        const mm = String(dt.getMonth() + 1).padStart(2, '0')
        const dd = String(dt.getDate()).padStart(2, '0')
        return `${dd}/${mm}/${yyyy}`
      } catch {
        return d
      }
    },
    summarizeInvoiceForms(json) {
      // json may be string or object/array
      try {
        const data = typeof json === 'string' ? JSON.parse(json) : json
        if (Array.isArray(data)) {
          return data.map(x => x.name || x.code || x).join(', ')
        }
        if (data && typeof data === 'object') {
          const keys = Object.keys(data)
          return keys.map(k => data[k]?.name || data[k]?.code || k).join(', ')
        }
        return String(data)
      } catch {
        return String(json)
      }
    },
    statusText(s) {
      const m = {
        0: 'Khởi tạo',
        1: 'Đã tạo XML',
        2: 'Đã ký',
        3: 'Đã gửi',
        4: 'Đã tiếp nhận',
        5: 'Đã chấp nhận',
        6: 'Từ chối / Lỗi'
      }
      return m[Number(s)] || '—'
    },
    statusVariant(s) {
      const n = Number(s)
      if (n === 5) return 'success'
      if (n === 6) return 'danger'
      if (n === 4) return 'primary'
      if (n === 3) return 'info'
      if (n === 2) return 'warning'
      if (n === 1) return 'light'
      return 'secondary'
    },
    async fetchList() {
      this.isBusy = true
      try {
        // Placeholder API; replace with backend endpoint
        // const { data } = await axios.get('/api/register-invoices', { params: this.buildQuery() })
        // this.list = { ...this.list, ...data }
        // For now, fill with mock data to visualize UI
        const mock = this.mockData()
        this.list.data = mock.items
        this.list.total = mock.total
        this.list.last_page = 1
        this.list.from = 1
        this.list.to = mock.items.length
      } catch (e) {
        // handle error toast later
        // console.error(e)
      } finally {
        this.isBusy = false
      }
    },
    buildQuery() {
      const q = {
        page: this.list.current_page,
        per_page: this.list.per_page
      }
      if (this.filters.keyword) q.keyword = this.filters.keyword
      if (this.filters.declarationType != null) q.declaration_type = this.filters.declarationType
      if (this.filters.status != null) q.status = this.filters.status
      if (this.filters.dateFrom) q.date_from = this.filters.dateFrom
      if (this.filters.dateTo) q.date_to = this.filters.dateTo
      return q
    },
    applyFilters() {
      this.list.current_page = 1
      this.fetchList()
    },
    resetFilters() {
      this.filters = { keyword: '', declarationType: null, status: null, dateFrom: null, dateTo: null }
      this.applyFilters()
    },
    onPageSizeChange() {
      this.list.current_page = 1
      this.fetchList()
    },
    onPageChange() {
      this.fetchList()
    },
    reload() {
      this.fetchList()
    },
    openCreate() {
      // TODO: navigate to create page or open modal
      this.$router.push({ name: 'CustomerRegisterInvoiceCreate' }).catch(() => {})
    },
    openEdit(item) {
      this.$router.push({ name: 'CustomerRegisterInvoiceEdit', params: { id: item.id } }).catch(() => {})
    },
    sendToTaxAuthority(item) {
      // TODO: implement send action
      // Placeholder: show notification
      // this.$bvToast.toast('Đã gửi yêu cầu tới Cơ quan thuế', { title: 'Gửi CQT', variant: 'info' })
    },
    downloadXml(item) {
      // TODO: call backend to download
      // window.open(`/api/register-invoices/${item.id}/xml`, '_blank')
    },
    openHistory(item) {
      // TODO: open history modal or navigate
      this.$router.push({ name: 'CustomerRegisterInvoiceHistory', params: { id: item.id } }).catch(() => {})
    },
    mockData() {
      // Simple mock dataset
      const items = [
        {
          id: 1,
          form_pattern: '01/ĐKTĐ-HĐĐT',
          declaration_date: new Date().toISOString(),
          declaration_type: 1,
          invoice_forms: JSON.stringify([{ code: 'T', name: 'Có mã' }, { code: 'K', name: 'Không mã' }]),
          response_receive_file: null,
          response_accept_file: null,
          status: 1
        },
        {
          id: 2,
          form_pattern: '01/ĐKTĐ-HĐĐT',
          declaration_date: new Date(Date.now() - 86400000).toISOString(),
          declaration_type: 2,
          invoice_forms: JSON.stringify([{ code: 'T', name: 'Có mã' }]),
          response_receive_file: 'TB_TiepNhan_2025_12_26.pdf',
          response_accept_file: 'TB_ChapNhan_2025_12_27.pdf',
          status: 5
        }
      ]
      return { items, total: items.length }
    }
  }
}
</script>

<style scoped>
.register-invoices .role-pill { font-size: 12px; }
</style>
