<template>
  <div class="container-fluid py-3 import-page">
    <div class="page-head">
      <div>
        <h4>{{ config.title }}</h4>
      </div>
      <div class="page-actions">
        <b-button size="sm" variant="outline-secondary" :to="config.listRoute">
          <i :class="[config.icon, 'mr-1']"></i>
          {{ config.listTitle }}
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
          <i :class="config.icon"></i>
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

        <template #cell(status)="{ item }">
          <b-badge :variant="statusVariant(item.status)">{{ statusText(item.status) }}</b-badge>
          <div v-if="item.sourceImportId" class="small text-muted mt-1">
            Import lại từ #{{ item.sourceImportId }}
          </div>
        </template>

        <template #cell(summary)="{ item }">
          <div>{{ item.successCount || 0 }} {{ config.itemLabelLower }} / {{ item.totalRows || 0 }} dòng</div>
          <small v-if="item.errorCount" class="text-danger">{{ item.errorCount }} lỗi</small>
        </template>

        <template #cell(importedItemIds)="{ item }">
          <span class="text-wrap-anywhere">{{ shortIds(item.importedItemIds || item.importedInvoiceIds) }}</span>
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
            <b-dropdown-item v-if="item.importedItemIds || item.importedInvoiceIds" :to="config.listRoute">
              Xem danh sách {{ config.itemLabelLower }}
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

const CONFIGS = {
  customer: {
    title: 'Import khách hàng',
    itemLabelLower: 'khách hàng',
    icon: 'fas fa-users',
    templateFile: 'mau-import-khach-hang.xlsx',
    listRoute: '/categories/customer/list',
    listTitle: 'Danh sách khách hàng'
  },
  product: {
    title: 'Import sản phẩm',
    itemLabelLower: 'sản phẩm',
    icon: 'fas fa-box',
    templateFile: 'mau-import-san-pham.xlsx',
    listRoute: '/categories/product/list',
    listTitle: 'Danh sách sản phẩm'
  }
}

export default {
  name: 'CustomerCatalogImport',
  components: { PaginationBar },
  data () {
    return {
      selectedFile: null,
      uploading: false,
      downloadingTemplate: false,
      loading: false,
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
        { key: 'status', label: 'Trạng thái', class: 'text-center', thStyle: { width: '14%' } },
        { key: 'summary', label: 'Kết quả', class: 'text-center', thStyle: { width: '16%' } },
        { key: 'importedItemIds', label: 'Mã đã import', thStyle: { width: '18%' } },
        { key: 'createdAt', label: 'Ngày import', thStyle: { width: '12%' } },
        { key: 'actions', label: '', class: 'text-center', thStyle: { width: '5%' } }
      ]
    }
  },
  computed: {
    importType () {
      return this.$route?.meta?.importType || 'customer'
    },
    config () {
      return CONFIGS[this.importType] || CONFIGS.customer
    }
  },
  watch: {
    importType () {
      this.selectedFile = null
      this.list.current_page = 1
      this.loadHistory()
    }
  },
  mounted () {
    this.loadHistory()
  },
  methods: {
    endpoint (suffix = '') {
      return `/catalog-imports/${this.importType}${suffix}`
    },
    async downloadTemplate () {
      this.downloadingTemplate = true
      try {
        const res = await axios.get(this.endpoint('/template'), { responseType: 'blob' })
        const blob = new Blob([res.data], { type: res.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = this.config.templateFile
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
      try {
        const form = new FormData()
        form.append('file', this.selectedFile)
        const { data } = await axios.post(this.endpoint('/upload'), form, {
          meta: { suppressGlobalErrorToast: true }
        })
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
      const ok = window.confirm(`Import lại file "${item.originalFilename || item.id}"? Hệ thống sẽ cập nhật hoặc tạo mới ${this.config.itemLabelLower}.`)
      if (!ok) return
      this.loading = true
      try {
        const { data } = await axios.post(this.endpoint(`/${item.id}/reimport`), null, {
          meta: { suppressGlobalErrorToast: true }
        })
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
        const { data } = await axios.get(this.endpoint(), {
          params: {
            page: this.list.current_page,
            size: this.list.per_page
          },
          meta: { suppressGlobalErrorToast: true }
        })
        this.list.data = data?.items || []
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
        this.toastSuccess(`Đã import ${data.successCount || 0} ${this.config.itemLabelLower}`)
      } else {
        this.toastError(data?.errorMessage || `Import ${this.config.itemLabelLower} thất bại`)
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

.upload-actions {
  align-self: flex-end;
}

.section-head {
  margin-bottom: 14px;
}

@media (max-width: 768px) {
  .page-head,
  .section-head,
  .upload-row {
    align-items: stretch;
    flex-direction: column;
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
