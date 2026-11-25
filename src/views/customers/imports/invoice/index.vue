<template>
  <div class="container-fluid py-3 import-page">
    <div class="page-head">
      <div>
        <h4>Import {{ pageTitle }}</h4>
      </div>
      <div class="page-filter">
        <label for="invoice-import-kind" class="sr-only">Loại dữ liệu import</label>
        <b-form-select
          id="invoice-import-kind"
          v-model="importKind"
          :options="importKindOptions"
          size="sm"
          @change="changeImportKind"
        />
      </div>
      <div class="page-actions">
        <b-button size="sm" variant="outline-secondary" :to="invoiceListRoute">
          <i class="fas fa-file-invoice mr-1"></i>
          {{ invoiceListLabel }}
        </b-button>
        <b-button size="sm" variant="outline-primary" :disabled="downloadingTemplate" @click="downloadTemplate">
          <i class="fas fa-download mr-1"></i>
          Tải mẫu Excel
        </b-button>
      </div>
    </div>

    <b-card class="mb-3 import-card shadow-sm">
      <div class="upload-row">
        <div class="upload-icon">
          <i class="fas fa-file-excel"></i>
        </div>
        <div class="upload-main">
          <label class="upload-label">File Excel import</label>
          <b-form-file
            v-model="selectedFile"
            accept=".xlsx,.xls"
            browse-text="Chọn file"
            placeholder="Chưa chọn file Excel"
            drop-placeholder="Thả file Excel vào đây"
          />
        </div>
        <div class="upload-actions">
          <b-button variant="success" :disabled="uploading || !selectedFile" @click="uploadFile">
            <b-spinner v-if="uploading" small class="mr-1"></b-spinner>
            <i v-else class="fas fa-file-upload mr-1"></i>
            Import Excel
          </b-button>
        </div>
      </div>
    </b-card>

    <b-card class="history-card shadow-sm">
      <div class="section-head">
        <div>
          <h5>Lịch sử import</h5>
        </div>
        <b-button size="sm" variant="outline-secondary" :disabled="loading" @click="loadHistory">
          <i class="fas fa-sync-alt mr-1"></i>
          Làm mới
        </b-button>
      </div>

      <b-table
        class="import-history-table"
        bordered
        hover
        responsive
        small
        show-empty
        :items="list.data"
        :fields="fields"
        :busy="loading"
        empty-text="Chưa có lịch sử import"
      >
        <template #cell(index)="{ index }">
          {{ index + 1 + (list.current_page - 1) * list.per_page }}
        </template>

        <template #cell(originalFilename)="{ item }">
          <div class="font-weight-bold text-wrap-anywhere">{{ item.originalFilename || '—' }}</div>
        </template>

        <template #cell(stockIssueType)="{ item }">
          {{ Number(item.stockIssueType) === 1 ? 'Vận chuyển nội bộ' : Number(item.stockIssueType) === 2 ? 'Gửi bán đại lý' : '—' }}
        </template>

        <template #cell(status)="{ item }">
          <b-badge :variant="statusVariant(item.status)">{{ statusText(item.status) }}</b-badge>
          <div v-if="item.sourceImportId" class="small text-muted mt-1">
            Import lại từ #{{ item.sourceImportId }}
          </div>
        </template>

        <template #cell(summary)="{ item }">
          <div>{{ item.successCount || 0 }} hóa đơn / {{ item.totalRows || 0 }} dòng hàng</div>
          <small v-if="item.errorCount" class="text-danger">{{ item.errorCount }} lỗi</small>
        </template>

        <template #cell(importedInvoiceIds)="{ item }">
          <span class="text-wrap-anywhere">{{ shortIds(item.importedInvoiceIds) }}</span>
        </template>

        <template #cell(createdAt)="{ item }">
          {{ formatDateTime(item.createdAt) }}
        </template>

        <template #cell(actions)="{ item }">
          <b-dropdown size="sm" right variant="link" no-caret toggle-class="text-decoration-none" boundary="window">
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item v-if="item.fileUrl" :href="item.fileUrl" target="_blank">
              Tải file Excel
            </b-dropdown-item>
            <b-dropdown-item href="#" @click.prevent="reimport(item)">
              Import lại file này
            </b-dropdown-item>
            <b-dropdown-item v-if="item.importedInvoiceIds" :to="invoiceListRoute">
              Xem danh sách hóa đơn
            </b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <pagination-bar
        :current.sync="list.current_page"
        :size.sync="list.per_page"
        :total="list.total"
        :sizes="pageSizes"
        @page-change="loadHistory"
        @size-change="onPageSizeChange"
      />
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import PaginationBar from '@/views/components/pagination_bar.vue'

export default {
  name: 'CustomerInvoiceImport',
  components: { PaginationBar },
  data () {
    return {
      selectedFile: null,
      selectedCategory: null,
      selectedStockIssueType: null,
      uploading: false,
      downloadingTemplate: false,
      loading: false,
      lastResult: null,
      list: {
        data: [],
        current_page: 1,
        per_page: 10,
        total: 0,
        last_page: 1
      },
      pageSizes: [10, 20, 50, 100],
      fields: [
        { key: 'index', label: '#', class: 'text-center', thStyle: { width: '5%' } },
        { key: 'originalFilename', label: 'File import', thStyle: { width: '30%' } },
        { key: 'stockIssueType', label: 'Loại phiếu', thStyle: { width: '18%' } },
        { key: 'status', label: 'Trạng thái', class: 'text-center', thStyle: { width: '14%' } },
        { key: 'summary', label: 'Kết quả', class: 'text-center', thStyle: { width: '14%' } },
        { key: 'importedInvoiceIds', label: 'Mã tra cứu', thStyle: { width: '20%' } },
        { key: 'createdAt', label: 'Ngày import', thStyle: { width: '12%' } },
        { key: 'actions', label: '', class: 'text-center', thStyle: { width: '5%' } }
      ]
    }
  },
  mounted () {
    this.initializeImportKind()
    this.loadHistory()
  },
  computed: {
    invoiceCategory () {
      const routeCategory = Number(this.$route?.meta?.importCategory)
      if (routeCategory === 6) return 6
      const category = Number(this.$route?.query?.category || 1)
      return this.selectedCategory || (category === 2 || category === 6 ? category : 1)
    },
    stockIssueType () {
      return this.selectedStockIssueType || (Number(this.$route?.query?.stockIssueType || 1) === 2 ? 2 : 1)
    },
    importKind: {
      get () {
        if (this.invoiceCategory === 6) return `6:${this.stockIssueType}`
        return String(this.invoiceCategory)
      },
      set () {}
    },
    importKindOptions () {
      if (this.invoiceCategory === 6) {
        return [
          { value: '6:1', text: 'Phiếu xuất kho kiêm vận chuyển nội bộ' },
          { value: '6:2', text: 'Phiếu xuất kho hàng gửi bán đại lý' }
        ]
      }
      return [
        { value: '1', text: 'Hóa đơn GTGT' },
        { value: '2', text: 'Hóa đơn bán hàng' }
      ]
    },
    pageTitle () {
      return this.invoiceCategory === 6 ? 'phiếu xuất kho' : 'hóa đơn'
    },
    templateFilename () {
      if (this.invoiceCategory === 2) return 'mau-import-hoa-don-ban-hang.xlsx'
      if (this.invoiceCategory === 6) return this.stockIssueType === 2 ? 'mau-import-phieu-xuat-kho-hang-gui-ban-dai-ly.xlsx' : 'mau-import-phieu-xuat-kho-van-chuyen-noi-bo.xlsx'
      return 'mau-import-hoa-don-gtgt.xlsx'
    },
    invoiceListRoute () {
      if (this.invoiceCategory === 2) return '/invoice/sales-invoice/list'
      if (this.invoiceCategory === 6) return this.stockIssueType === 2 ? '/stock-issue/consignment/list' : '/stock-issue/internal-transport/list'
      return '/invoice/vat-invoice/list'
    },
    invoiceListLabel () {
      return this.invoiceCategory === 6 ? 'Danh sách phiếu xuất kho' : 'Danh sách hóa đơn'
    }
  },
  watch: {
    '$route': {
      deep: true,
      handler () {
        this.initializeImportKind()
        this.selectedFile = null
        this.lastResult = null
        this.list.current_page = 1
        this.loadHistory()
      }
    }
  },
  methods: {
    initializeImportKind () {
      const routeCategory = Number(this.$route?.meta?.importCategory)
      if (routeCategory === 6) {
        this.selectedCategory = 6
        this.selectedStockIssueType = 1
        return
      }
      const category = Number(this.$route?.query?.category || 1)
      this.selectedCategory = category === 2 || category === 6 ? category : 1
      const stockIssueType = Number(this.$route?.query?.stockIssueType || 1)
      this.selectedStockIssueType = stockIssueType === 2 ? 2 : 1
    },
    changeImportKind (value) {
      const [category, stockIssueType] = String(value).split(':')
      this.selectedCategory = Number(category) === 2 ? 2 : Number(category) === 6 ? 6 : 1
      this.selectedStockIssueType = Number(category) === 6 && Number(stockIssueType) === 2 ? 2 : 1
      this.selectedFile = null
      this.list.current_page = 1
      this.loadHistory()
    },
    async downloadTemplate () {
      this.downloadingTemplate = true
      try {
        const res = await axios.get('/invoice-imports/template', { params: { category: this.invoiceCategory, stockIssueType: this.invoiceCategory === 6 ? this.stockIssueType : undefined }, responseType: 'blob' })
        const blob = new Blob([res.data], { type: res.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = this.templateFilename
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (e) {
        this.toastError(e?.response?.data?.message || 'Không tải được mẫu Excel import')
      } finally {
        this.downloadingTemplate = false
      }
    },
    async uploadFile () {
      if (!this.selectedFile) {
        this.toastError('Vui lòng chọn file Excel')
        return
      }
      this.uploading = true
      this.lastResult = null
      try {
        const form = new FormData()
        form.append('file', this.selectedFile)
        const { data } = await axios.post('/invoice-imports/upload', form, {
          params: { category: this.invoiceCategory, stockIssueType: this.invoiceCategory === 6 ? this.stockIssueType : undefined },
          meta: { suppressGlobalErrorToast: true }
        })
        this.lastResult = data
        this.selectedFile = null
        this.notifyResult(data)
        await this.loadHistory()
      } catch (e) {
        this.toastError(e?.response?.data?.message || 'Không import được file Excel')
      } finally {
        this.uploading = false
      }
    },
    async reimport (item) {
      if (!item || !item.id) return
      const ok = window.confirm(`Import lại file "${item.originalFilename || item.id}"? Hệ thống sẽ tạo thêm hóa đơn nháp mới.`)
      if (!ok) return
      this.loading = true
      this.lastResult = null
      try {
        const { data } = await axios.post(`/invoice-imports/${item.id}/reimport`, null, {
          params: { category: this.invoiceCategory, stockIssueType: this.invoiceCategory === 6 ? this.stockIssueType : undefined },
          meta: { suppressGlobalErrorToast: true }
        })
        this.lastResult = data
        this.notifyResult(data)
        await this.loadHistory()
      } catch (e) {
        this.toastError(e?.response?.data?.message || 'Không import lại được file Excel')
      } finally {
        this.loading = false
      }
    },
    async loadHistory () {
      this.loading = true
      try {
        const { data } = await axios.get('/invoice-imports', {
          params: {
            page: this.list.current_page,
            size: this.list.per_page,
            category: this.invoiceCategory,
            stockIssueType: this.invoiceCategory === 6 ? this.stockIssueType : undefined
          },
          meta: { suppressGlobalErrorToast: true }
        })
        const items = data?.items || []
        this.list.data = items
        this.list.current_page = data?.current_page || 1
        this.list.per_page = data?.per_page || this.list.per_page
        this.list.total = data?.total || 0
        this.list.last_page = data?.last_page || 1
      } catch (e) {
        this.toastError(e?.response?.data?.message || 'Không tải được lịch sử import')
      } finally {
        this.loading = false
      }
    },
    onPageSizeChange () {
      this.list.current_page = 1
      this.loadHistory()
    },
    notifyResult (data) {
      if (data?.status === 'SUCCESS') {
        this.toastSuccess(`Đã tạo ${data.successCount || 0} hóa đơn nháp từ ${data.totalRows || 0} dòng hàng`)
      } else {
        this.toastError(data?.errorMessage || 'Import hóa đơn thất bại')
      }
    },
    statusText (status) {
      if (status === 'SUCCESS') return 'Thành công'
      if (status === 'ERROR') return 'Lỗi'
      return 'Đang xử lý'
    },
    statusVariant (status) {
      if (status === 'SUCCESS') return 'success'
      if (status === 'ERROR') return 'danger'
      return 'warning'
    },
    shortIds (value) {
      if (!value) return '—'
      const arr = String(value).split(',').filter(Boolean)
      if (arr.length <= 4) return arr.join(', ')
      return `${arr.slice(0, 4).join(', ')}... (+${arr.length - 4})`
    },
    formatDateTime (value) {
      if (!value) return '—'
      const d = new Date(value)
      if (Number.isNaN(d.getTime())) return value
      return d.toLocaleString('vi-VN')
    },
    toastSuccess (message) {
      if (this.$toastr && typeof this.$toastr.success === 'function') {
        this.$toastr.success(message, 'Thành công')
      }
    },
    toastError (message) {
      if (this.$toastr && typeof this.$toastr.error === 'function') {
        this.$toastr.error(message, 'Lỗi')
      }
    }
  }
}
</script>

<style scoped>
.import-page {
  font-size: 13px;
}

.page-head,
.section-head,
.upload-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-head {
  margin-bottom: 16px;
}

.page-filter {
  margin-left: auto;
  min-width: 260px;
}

.page-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.page-head h4,
.section-head h5 {
  margin: 0 0 4px;
  color: #1f2937;
  font-weight: 700;
}

.page-head p,
.section-head span {
  margin: 0;
  color: #667085;
}

.import-card,
.history-card {
  border-radius: 8px;
  border: 1px solid #e6ebf2;
}

.upload-icon {
  align-items: center;
  background: #ecfdf5;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  color: #15803d;
  display: inline-flex;
  flex: 0 0 44px;
  font-size: 20px;
  height: 44px;
  justify-content: center;
  width: 44px;
}

.history-card::v-deep .table-responsive {
  overflow-x: hidden;
}

.history-card::v-deep .import-history-table {
  width: 100%;
  table-layout: fixed;
}

.history-card::v-deep .import-history-table th,
.history-card::v-deep .import-history-table td {
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
  vertical-align: middle;
}

.history-card::v-deep .dropdown-menu {
  min-width: 180px;
  z-index: 1050;
}

.upload-main {
  flex: 1;
  min-width: 0;
}

.text-wrap-anywhere {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.upload-label {
  display: block;
  margin-bottom: var(--ui-label-gap);
  color: #344054;
  font-weight: 700;
}

.upload-hint {
  color: #64748b;
  display: block;
  margin-top: 6px;
}

.upload-actions {
  align-self: flex-end;
}

.section-head {
  margin-bottom: 14px;
}

.error-message {
  margin: 0;
  white-space: pre-wrap;
  color: inherit;
  font-family: inherit;
}

@media (max-width: 768px) {
  .page-head,
  .section-head,
  .upload-row {
    align-items: stretch;
    flex-direction: column;
  }

  .page-filter {
    margin-left: 0;
    width: 100%;
  }

  .upload-actions {
    align-self: stretch;
  }

  .page-actions {
    justify-content: stretch;
  }

  .page-actions .btn {
    width: 100%;
  }

  .upload-actions .btn {
    width: 100%;
  }
}
</style>
