<template>
  <div class="container-fluid py-3 system-logs">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div>
        <h4 class="mb-1 font-weight-bold">Log hệ thống</h4>
      </div>
      <b-button size="sm" variant="outline-primary" :disabled="isBusy" @click="fetchLogs">
        <b-spinner v-if="isBusy" small class="mr-1"></b-spinner>
        <i v-else class="fas fa-sync-alt mr-1"></i>
        Làm mới
      </b-button>
    </div>

    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="6" lg="4" class="mb-2">
          <b-form-group label="Từ khóa" label-for="log-keyword" class="mb-0">
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input
              id="log-keyword"
              v-model.trim="filters.keyword"
              placeholder="Tìm requestId, PATVAN, mã tham chiếu..."
              @keyup.enter="applyFilters"
            />
          </b-input-group>
          </b-form-group>
        </b-col>
        <b-col md="3" lg="2" class="mb-2">
          <b-form-group label="Mức log" label-for="log-level" class="mb-0">
            <b-form-select id="log-level" v-model="filters.level" :options="levelOptions" />
          </b-form-group>
        </b-col>
        <b-col md="3" lg="2" class="mb-2">
          <b-form-group label="Từ ngày" label-for="log-date-from" class="mb-0">
            <b-form-input id="log-date-from" v-model="filters.dateFrom" type="date" />
          </b-form-group>
        </b-col>
        <b-col md="3" lg="2" class="mb-2">
          <b-form-group label="Đến ngày" label-for="log-date-to" class="mb-0">
            <b-form-input id="log-date-to" v-model="filters.dateTo" type="date" />
          </b-form-group>
        </b-col>
        <b-col md="6" lg="2" class="mb-2 d-flex align-items-end justify-content-md-end filter-actions">
          <b-button size="sm" variant="primary" class="mr-2" @click="applyFilters">
            <i class="fas fa-filter mr-1"></i>
            Lọc
          </b-button>
          <b-button size="sm" variant="outline-secondary" @click="resetFilters">
            Xóa lọc
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <b-card class="shadow-sm log-table-card">
      <div class="d-flex align-items-center justify-content-between flex-wrap mb-2">
        <div class="text-muted small">
          <i class="fas fa-file-alt mr-1"></i>
          {{ meta.fileName || 'logs/hoadon.log' }}
          <span class="mx-1">·</span>
          {{ list.total }} bản ghi
          <span v-if="meta.lastModified" class="ml-2">
            Cập nhật: {{ formatDateTime(meta.lastModified) }}
          </span>
        </div>
        <b-badge :variant="meta.exists ? 'success' : 'warning'">
          {{ meta.exists ? 'Đang ghi log' : 'Chưa có file log' }}
        </b-badge>
      </div>

      <b-table
        bordered
        hover
        responsive
        small
        show-empty
        class="mb-0 table-modern table-compact log-table"
        :busy="isBusy"
        :items="list.data"
        :fields="fields"
        empty-text="Không có bản ghi log"
      >
        <template #cell(index)="{ index }">
          {{ index + 1 + (list.current_page - 1) * list.per_page }}
        </template>

        <template #cell(timestamp)="{ item }">
          <span class="text-nowrap">{{ formatDateTime(item.timestamp) }}</span>
        </template>

        <template #cell(level)="{ item }">
          <b-badge :variant="levelVariant(item.level)">{{ item.level || '-' }}</b-badge>
        </template>

        <template #cell(message)="{ item }">
          <div class="log-message" :title="item.message">{{ item.message || '-' }}</div>
        </template>

        <template #cell(option)="{ item }">
          <b-button size="sm" variant="link" class="text-primary" @click="showDetail(item)">
            <i class="fas fa-search mr-1"></i>Chi tiết
          </b-button>
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

    <b-modal ref="detailModal" title="Chi tiết log" ok-only ok-title="Đóng" size="xl">
      <b-table-simple small bordered class="mb-3">
        <b-tbody>
          <b-tr><b-th>Mức log</b-th><b-td><b-badge :variant="levelVariant(selected.level)">{{ selected.level || '-' }}</b-badge></b-td></b-tr>
          <b-tr><b-th>Thời gian</b-th><b-td>{{ formatDateTime(selected.timestamp) }}</b-td></b-tr>
          <b-tr><b-th>Thread</b-th><b-td>{{ selected.thread || '-' }}</b-td></b-tr>
          <b-tr><b-th>Logger</b-th><b-td>{{ selected.logger || '-' }}</b-td></b-tr>
          <b-tr><b-th>Nội dung</b-th><b-td><pre class="log-detail">{{ selected.raw || selected.message || '-' }}</pre></b-td></b-tr>
        </b-tbody>
      </b-table-simple>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import PaginationBar from '@/views/components/pagination_bar.vue'

export default {
  name: 'AdminSystemLogs',
  components: { PaginationBar },
  data() {
    return {
      isBusy: false,
      selected: {},
      filters: { keyword: '', level: '', dateFrom: '', dateTo: '' },
      levelOptions: [
        { value: '', text: 'Tất cả mức log' },
        { value: 'ERROR', text: 'ERROR' },
        { value: 'WARN', text: 'WARN' },
        { value: 'INFO', text: 'INFO' },
        { value: 'DEBUG', text: 'DEBUG' },
      ],
      pageSizes: [10, 20, 50, 100],
      meta: { fileName: '', exists: false, lastModified: null },
      list: { current_page: 1, data: [], per_page: 20, total: 0 },
      fields: [
        { key: 'index', label: '#', thStyle: { width: '48px' }, tdClass: 'text-center' },
        { key: 'timestamp', label: 'Thời gian', thStyle: { width: '165px' } },
        { key: 'level', label: 'Mức', thStyle: { width: '80px' }, tdClass: 'text-center' },
        { key: 'logger', label: 'Logger', thStyle: { width: '230px' } },
        { key: 'message', label: 'Nội dung' },
        { key: 'option', label: '', thStyle: { width: '90px' }, tdClass: 'text-center' },
      ],
    }
  },
  mounted() {
    this.fetchLogs()
  },
  methods: {
    async fetchLogs() {
      this.isBusy = true
      try {
        const { data } = await axios.get('/administrator/logs', {
          params: {
            page: this.list.current_page,
            size: this.list.per_page,
            q: this.filters.keyword || undefined,
            level: this.filters.level || undefined,
            dateFrom: this.filters.dateFrom || undefined,
            dateTo: this.filters.dateTo || undefined,
          },
        })
        this.list.data = data.items || []
        this.list.total = data.total || 0
        this.list.current_page = data.current_page || this.list.current_page
        this.list.per_page = data.per_page || this.list.per_page
        this.meta = {
          fileName: data.fileName || '',
          exists: !!data.exists,
          lastModified: data.lastModified || null,
        }
      } catch {
        this.list.data = []
        this.list.total = 0
      } finally {
        this.isBusy = false
      }
    },
    applyFilters() {
      this.list.current_page = 1
      this.fetchLogs()
    },
    resetFilters() {
      this.filters = { keyword: '', level: '', dateFrom: '', dateTo: '' }
      this.applyFilters()
    },
    onPageChange(page) {
      this.list.current_page = Number(page) || 1
      this.fetchLogs()
    },
    onPageSizeChange(size) {
      this.list.per_page = Number(size) || 20
      this.list.current_page = 1
      this.fetchLogs()
    },
    showDetail(item) {
      this.selected = item || {}
      this.$refs.detailModal.show()
    },
    levelVariant(level) {
      if (level === 'ERROR') return 'danger'
      if (level === 'WARN') return 'warning'
      if (level === 'DEBUG') return 'secondary'
      return 'info'
    },
    formatDateTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
      const pad = value => String(value).padStart(2, '0')
      return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    },
  },
}
</script>

<style scoped>
.system-logs {
  max-width: 100%;
  overflow-x: hidden;
}

.log-table-card {
  max-width: 100%;
  overflow: hidden;
}

.log-table-card ::v-deep .table-responsive {
  max-width: 100%;
  overflow-x: auto;
}

.log-table {
  min-width: 0;
  table-layout: fixed;
}

.log-table ::v-deep th {
  background: #eef4fa;
  vertical-align: middle;
  white-space: normal;
}

.log-table ::v-deep td {
  vertical-align: middle;
}

.log-message {
  max-width: 520px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-detail {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  color: #334155;
  margin: 0;
  max-height: 55vh;
  overflow: auto;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
