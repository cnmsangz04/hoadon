<template>
  <div class="container-fluid py-3 database-admin">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Dữ liệu database</h4>
      <div>
        <b-button size="sm" variant="outline-primary" class="refresh-button" @click="refreshCurrentTable">
          <i class="fas fa-sync-alt mr-1"></i>Làm mới
        </b-button>
      </div>
    </div>

    <b-alert show variant="warning" class="database-warning small mb-3">
      <i class="fas fa-shield-alt mr-2"></i>
      Kiểm tra kỹ dữ liệu trước khi lưu hoặc xóa. Một số bảng hệ thống chỉ cho phép xem.
    </b-alert>

    <b-row>
      <b-col lg="3" xl="3" class="mb-3">
        <b-card class="shadow-sm table-list-card" body-class="p-0">
          <div class="table-list-header d-flex align-items-center justify-content-between px-3 py-3">
            <div>
              <strong>Các bảng</strong>
              <div class="text-muted small">{{ filteredTables.length }} bảng</div>
            </div>
            <b-spinner v-if="isLoadingTables" small></b-spinner>
          </div>
          <div class="px-3 pb-2">
            <b-input-group size="sm">
              <b-input-group-prepend is-text><i class="fas fa-search text-muted"></i></b-input-group-prepend>
              <b-form-input v-model.trim="tableKeyword" placeholder="Lọc tên bảng"></b-form-input>
            </b-input-group>
          </div>
          <div class="table-list px-2 pb-2">
            <button
              v-for="item in filteredTables"
              :key="item.name"
              type="button"
              class="table-list-item"
              :class="{ active: selectedTable === item.name }"
              :title="item.label"
              @click="selectTable(item.name)">
              <span class="table-list-label text-left mr-2">
                <i class="fas fa-table mr-2"></i>{{ item.label }}
              </span>
              <b-badge pill :variant="selectedTable === item.name ? 'primary' : 'light'">{{ item.count }}</b-badge>
            </button>
            <div v-if="filteredTables.length === 0" class="empty-table-list text-muted small text-center py-4">
              Không tìm thấy bảng
            </div>
          </div>
        </b-card>
      </b-col>

      <b-col lg="9" xl="9">
        <b-card class="mb-3 shadow-sm filter-card">
          <div class="d-flex align-items-center justify-content-between flex-wrap mb-3">
            <div>
              <h5 class="mb-1 font-weight-bold">{{ selectedTableLabel || 'Chưa chọn bảng' }}</h5>
              <code v-if="selectedTable" class="table-name">{{ selectedTable }}</code>
              <b-badge v-if="selectedTableReadOnly" variant="secondary">Chỉ xem</b-badge>
              <b-badge v-else variant="success">Có thể thêm, sửa, xóa</b-badge>
            </div>
            <b-button v-if="!selectedTableReadOnly && selectedTable" size="sm" variant="success" @click="openCreate">
              <i class="fas fa-plus mr-1"></i>Thêm dữ liệu
            </b-button>
          </div>

          <b-row>
            <b-col md="8" lg="8" class="mb-2 mb-md-0">
              <b-input-group>
                <b-input-group-prepend is-text><i class="fas fa-search text-muted"></i></b-input-group-prepend>
                <b-form-input
                  v-model.trim="keyword"
                  placeholder="Tìm theo nội dung trong bảng"
                  @keyup.enter="applyFilter"></b-form-input>
              </b-input-group>
            </b-col>
            <b-col md="4" lg="4" class="text-md-right">
              <b-button size="sm" variant="primary" class="filter-button" @click="applyFilter">
                <i class="fas fa-filter mr-1"></i>Lọc dữ liệu
              </b-button>
            </b-col>
          </b-row>
        </b-card>

        <b-card class="shadow-sm database-card">
          <div class="table-responsive">
            <b-table
              bordered hover small show-empty responsive="false"
              class="database-table"
              :items="rows"
              :fields="tableFields"
              :busy="isLoadingRows"
              empty-text="Không có dữ liệu">
              <template #cell(index)="{ index }">
                {{ index + 1 + page * pageSize }}
              </template>
              <template #cell(option)="{ item }">
                <b-dropdown v-if="!selectedTableReadOnly" size="sm" right variant="link" no-caret>
                  <template #button-content><i class="fas fa-ellipsis-h text-muted"></i></template>
                  <b-dropdown-item @click="openEdit(item)">
                    <i class="fas fa-edit mr-2"></i>Sửa
                  </b-dropdown-item>
                  <b-dropdown-item @click="confirmDelete(item)" class="text-danger">
                    <i class="fas fa-trash mr-2"></i>Xóa
                  </b-dropdown-item>
                </b-dropdown>
                <span v-else class="text-muted">-</span>
              </template>
            </b-table>
          </div>

          <pagination-bar
            :current.sync="pageDisplay"
            :size.sync="pageSize"
            :total="total"
            :sizes="pageSizes"
            @page-change="changePage"
            @size-change="changePageSize"
          />
        </b-card>
      </b-col>
    </b-row>

    <b-modal
      ref="dataModal"
      size="xl"
      :title="editId === null ? 'Thêm dữ liệu' : 'Cập nhật dữ liệu'"
      ok-title="Lưu lại"
      cancel-title="Hủy"
      :busy="isSaving"
      @ok.prevent="saveRow">
      <b-alert v-if="formError" show variant="danger" class="small">{{ formError }}</b-alert>
      <b-row>
        <b-col v-for="column in formColumns" :key="column.name" md="6" class="mb-2">
          <b-form-group :label="column.name + (column.nullable ? '' : ' *')">
            <b-form-textarea
              v-if="isLongText(column)"
              v-model="form[column.name]"
              rows="3"
              :placeholder="column.sqlType"></b-form-textarea>
            <b-form-checkbox
              v-else-if="column.type === 'boolean'"
              v-model="form[column.name]"
              switch>
              {{ form[column.name] ? 'Bật' : 'Tắt' }}
            </b-form-checkbox>
            <b-form-input
              v-else
              v-model="form[column.name]"
              :type="inputType(column)"
              :placeholder="column.sqlType"></b-form-input>
          </b-form-group>
        </b-col>
      </b-row>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import PaginationBar from '@/views/components/pagination_bar.vue'

export default {
  name: 'DatabaseAdmin',
  components: { PaginationBar },
  data() {
    return {
      tables: [],
      selectedTable: '',
      tableKeyword: '',
      schema: [],
      rows: [],
      tableFields: [],
      keyword: '',
      page: 0,
      pageSize: 20,
      pageSizes: [10, 20, 50, 100],
      total: 0,
      isLoadingTables: false,
      isLoadingRows: false,
      isSaving: false,
      editId: null,
      form: {},
      formError: ''
    }
  },
  computed: {
    filteredTables() {
      const text = (this.tableKeyword || '').toLowerCase()
      return this.tables.filter(item => !text || `${item.label} ${item.name}`.toLowerCase().includes(text))
    },
    selectedTableMeta() {
      return this.tables.find(item => item.name === this.selectedTable) || null
    },
    selectedTableLabel() {
      return this.selectedTableMeta ? this.selectedTableMeta.label : ''
    },
    selectedTableReadOnly() {
      return !!(this.selectedTableMeta && this.selectedTableMeta.readOnly)
    },
    pageDisplay: {
      get() { return this.page + 1 },
      set(value) { this.page = Math.max(Number(value || 1) - 1, 0) }
    },
    formColumns() {
      return this.schema.filter(column => column.writable && !column.identity && !column.primaryKey)
    }
  },
  mounted() {
    this.loadTables()
  },
  methods: {
    async loadTables() {
      this.isLoadingTables = true
      try {
        const response = await axios.get('/administrator/database/tables')
        this.tables = response.data.tables || []
        if (!this.selectedTable && this.tables.length) this.selectTable(this.tables[0].name)
      } finally {
        this.isLoadingTables = false
      }
    },
    async selectTable(table) {
      this.selectedTable = table
      this.page = 0
      this.keyword = ''
      this.schema = []
      this.rows = []
      await this.loadSchema()
      await this.loadRows()
    },
    async loadSchema() {
      if (!this.selectedTable) return
      const response = await axios.get(`/administrator/database/${this.selectedTable}/schema`)
      this.schema = response.data.columns || []
      const visible = this.schema.filter(column => !['password', 'admin_password', 'api_token', 'remember_token', 'forget_token', 'bot_token'].includes(column.name))
      this.tableFields = [
        { key: 'index', label: '#', thStyle: { width: '50px' }, class: 'text-center' },
        ...visible.map(column => ({ key: column.name, label: column.name, sortable: false })),
        { key: 'option', label: 'Thao tác', thStyle: { width: '90px' }, class: 'text-center' }
      ]
    },
    async loadRows() {
      if (!this.selectedTable) return
      this.isLoadingRows = true
      try {
        const response = await axios.get(`/administrator/database/${this.selectedTable}/rows`, {
          params: { page: this.page, size: this.pageSize, keyword: this.keyword || undefined }
        })
        this.rows = response.data.data || []
        this.total = Number(response.data.total || 0)
      } finally {
        this.isLoadingRows = false
      }
    },
    refreshCurrentTable() {
      if (this.selectedTable) this.loadRows()
      else this.loadTables()
    },
    applyFilter() {
      this.page = 0
      this.loadRows()
    },
    changePage(page) {
      this.page = page - 1
      this.loadRows()
    },
    changePageSize(size) {
      this.pageSize = Number(size) || 20
      this.page = 0
      this.loadRows()
    },
    openCreate() {
      this.editId = null
      this.formError = ''
      this.form = {}
      this.formColumns.forEach(column => {
        this.$set(this.form, column.name, column.type === 'boolean' ? false : '')
      })
      this.$refs.dataModal.show()
    },
    async openEdit(row) {
      this.formError = ''
      this.editId = row.id
      try {
        const response = await axios.get(`/administrator/database/${this.selectedTable}/rows/${encodeURIComponent(row.id)}`)
        this.form = { ...response.data }
        this.formColumns.forEach(column => {
          if (typeof this.form[column.name] === 'undefined' || this.form[column.name] === null) {
            this.$set(this.form, column.name, column.type === 'boolean' ? false : '')
          }
        })
        this.$refs.dataModal.show()
      } catch (error) {
        this.editId = null
      }
    },
    async saveRow() {
      if (this.isSaving) return
      this.isSaving = true
      this.formError = ''
      try {
        const url = this.editId === null
          ? `/administrator/database/${this.selectedTable}/rows`
          : `/administrator/database/${this.selectedTable}/rows/${encodeURIComponent(this.editId)}`
        const method = this.editId === null ? 'post' : 'put'
        await axios[method](url, this.form, { successMessage: this.editId === null ? 'Đã thêm dữ liệu' : 'Đã cập nhật dữ liệu' })
        this.$refs.dataModal.hide()
        await this.loadTables()
        await this.loadRows()
      } catch (error) {
        this.formError = error?.response?.data?.message || 'Không thể lưu dữ liệu'
      } finally {
        this.isSaving = false
      }
    },
    confirmDelete(row) {
      this.$bvModal.msgBoxConfirm('Bạn có chắc muốn xóa dòng dữ liệu này không?', {
        title: 'Xác nhận xóa',
        okTitle: 'Xóa',
        cancelTitle: 'Hủy',
        okVariant: 'danger'
      }).then(confirmed => {
        if (confirmed) this.deleteRow(row)
      })
    },
    async deleteRow(row) {
      try {
        await axios.delete(`/administrator/database/${this.selectedTable}/rows/${encodeURIComponent(row.id)}`, {
          successMessage: 'Đã xóa dữ liệu'
        })
        if (this.rows.length === 1 && this.page > 0) this.page -= 1
        await this.loadTables()
        await this.loadRows()
      } catch (error) {
        // axios interceptor hiển thị thông báo lỗi
      }
    },
    isLongText(column) {
      return ['text', 'ntext', 'nvarchar', 'varchar'].includes(column.sqlType) &&
        (column.sqlType === 'text' || column.sqlType === 'ntext' || column.name.includes('xml') || column.name.includes('json') || column.name.includes('info') || column.name.includes('description') || column.name.includes('content'))
    },
    inputType(column) {
      if (column.type === 'number' || column.type === 'decimal') return 'number'
      if (column.type === 'date') return 'date'
      if (column.type === 'datetime') return 'datetime-local'
      return 'text'
    }
  }
}
</script>

<style scoped>
.database-admin {
  color: #1f2937;
}

.refresh-button,
.filter-button {
  border-radius: 8px;
  font-weight: 600;
}

.database-warning {
  border: 0;
  border-left: 4px solid #f59e0b;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, .04);
}

.database-admin ::v-deep .card {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

.table-list-header {
  border-bottom: 1px solid #eef0f3;
}

.table-list-card {
  position: sticky;
  top: 16px;
}

.table-list {
  max-height: calc(100vh - 285px);
  overflow-y: auto;
}

.table-list-item {
  width: 100%;
  border: 0;
  background: transparent;
  color: #344054;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 7px;
  cursor: pointer;
  font-size: 13px;
  transition: background-color .15s ease, color .15s ease;
}

.table-list-item:hover,
.table-list-item.active {
  background: #eaf2ff;
  color: #1456a0;
}

.table-list-item.active .text-muted {
  color: #2563eb !important;
}

.table-list-label {
  flex: 1;
  min-width: 0;
  line-height: 1.35;
  white-space: normal;
}

.empty-table-list {
  border: 1px dashed #d1d5db;
  border-radius: 7px;
}

.table-name {
  color: #64748b;
  display: inline-block;
  font-size: 11px;
  margin-right: 6px;
  margin-bottom: 4px;
}

.database-admin .table-responsive {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  max-height: 60vh;
  overflow: auto;
}

.database-admin table {
  margin-bottom: 0;
  white-space: nowrap;
}

.database-admin ::v-deep .database-table thead th {
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  position: sticky;
  top: 0;
  z-index: 2;
}

.database-admin ::v-deep .database-table td,
.database-admin ::v-deep .database-table th {
  vertical-align: middle;
}

@media (max-width: 991.98px) {
  .table-list-card {
    position: static;
  }

  .table-list {
    max-height: 260px;
  }
}
</style>
