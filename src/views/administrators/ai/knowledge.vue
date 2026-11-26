<template>
  <div class="container-fluid py-3 knowledge-page">
    <div class="d-flex align-items-center justify-content-between flex-wrap mb-3">
      <div>
        <h4 class="mb-1 font-weight-bold">Thư viện tài liệu AI</h4>
        <div class="text-muted small">Quản lý tài liệu RAG, phiên bản và phạm vi được phép gửi tới Gemini.</div>
      </div>
      <div class="mt-2 mt-md-0">
        <b-button size="sm" variant="outline-secondary" class="mr-2" :disabled="busy" @click="rebuildIndex">
          <i class="fas fa-database mr-1"></i>Xây dựng lại chỉ mục
        </b-button>
        <b-button size="sm" variant="primary" @click="openCreate">
          <i class="fas fa-plus mr-1"></i>Thêm tài liệu
        </b-button>
      </div>
    </div>

    <b-tabs content-class="pt-3">
      <b-tab title="Tài liệu" active>
        <b-card class="mb-3 shadow-sm">
          <b-row>
            <b-col md="4" class="mb-2">
              <b-form-group label="Tìm kiếm" class="mb-0">
                <b-form-input v-model.trim="filters.q" placeholder="Tên tài liệu..." @keyup.enter="applyFilters" />
              </b-form-group>
            </b-col>
            <b-col md="2" class="mb-2">
              <b-form-group label="Danh mục" class="mb-0">
                <b-form-select v-model="filters.categoryId" :options="categoryOptions" />
              </b-form-group>
            </b-col>
            <b-col md="2" class="mb-2">
              <b-form-group label="Định dạng" class="mb-0">
                <b-form-select v-model="filters.type" :options="typeFilterOptions" />
              </b-form-group>
            </b-col>
            <b-col md="2" class="mb-2">
              <b-form-group label="Trạng thái" class="mb-0">
                <b-form-select v-model="filters.status" :options="statusFilterOptions" />
              </b-form-group>
            </b-col>
            <b-col md="2" class="mb-2 d-flex align-items-end">
              <b-button size="sm" variant="primary" class="mr-2" @click="applyFilters">Lọc</b-button>
              <b-button size="sm" variant="outline-secondary" @click="resetFilters">Xóa</b-button>
            </b-col>
          </b-row>
        </b-card>

        <b-card class="shadow-sm">
          <div class="d-flex justify-content-between align-items-center mb-2">
            <span class="text-muted small">{{ list.total }} tài liệu</span>
            <b-button size="sm" variant="link" @click="openCategories">
              <i class="fas fa-tags mr-1"></i>Quản lý danh mục
            </b-button>
          </div>
          <b-table bordered hover responsive small show-empty :busy="busy" :items="list.items" :fields="fields"
                   empty-text="Chưa có tài liệu kiến thức">
            <template #cell(index)="{ index }">
              {{ index + 1 + (list.current_page - 1) * list.per_page }}
            </template>
            <template #cell(title)="{ item }">
              <div class="font-weight-bold">{{ item.title }}</div>
              <div class="small text-muted">{{ item.originalFilename || '-' }} · v{{ item.currentVersion || '-' }}</div>
              <b-badge v-if="item.promptInjectionDetected" variant="warning" class="mt-1">Có chỉ dẫn đáng ngờ</b-badge>
            </template>
            <template #cell(scope)="{ item }">
              <span>{{ item.scope === 'GLOBAL' ? 'Toàn hệ thống' : `Công ty #${item.companyId}` }}</span>
            </template>
            <template #cell(status)="{ item }">
              <b-badge :variant="statusVariant(item.status)">{{ statusLabel(item.status) }}</b-badge>
              <div class="small mt-1" :class="item.active ? 'text-success' : 'text-muted'">
                {{ item.active ? 'Đang áp dụng' : 'Chưa áp dụng' }}
              </div>
            </template>
            <template #cell(externalShareAllowed)="{ item }">
              <i :class="item.externalShareAllowed ? 'fas fa-check text-success' : 'fas fa-lock text-muted'"></i>
            </template>
            <template #cell(option)="{ item }">
              <b-dropdown size="sm" right variant="outline-primary" text="Thao tác">
                <b-dropdown-item @click="openEdit(item)"><i class="fas fa-edit mr-2"></i>Chỉnh sửa</b-dropdown-item>
                <b-dropdown-item @click="showVersions(item)"><i class="fas fa-history mr-2"></i>Phiên bản</b-dropdown-item>
                <b-dropdown-item v-if="!item.active && item.status === 'READY'" @click="toggleActive(item, true)">
                  <i class="fas fa-toggle-on mr-2"></i>Kích hoạt
                </b-dropdown-item>
                <b-dropdown-item v-if="item.active" @click="toggleActive(item, false)">
                  <i class="fas fa-toggle-off mr-2"></i>Vô hiệu hóa
                </b-dropdown-item>
                <b-dropdown-item @click="reprocess(item)"><i class="fas fa-redo mr-2"></i>Xử lý lại</b-dropdown-item>
                <b-dropdown-divider />
                <b-dropdown-item class="text-danger" @click="remove(item)"><i class="fas fa-trash mr-2"></i>Xóa</b-dropdown-item>
              </b-dropdown>
            </template>
          </b-table>
          <pagination-bar :current.sync="list.current_page" :size.sync="list.per_page" :total="list.total"
                          :sizes="[10, 20, 50, 100]" @page-change="pageChanged" @size-change="sizeChanged" />
        </b-card>
      </b-tab>

      <b-tab title="Kiểm thử truy xuất & AI">
        <b-row>
          <b-col lg="5">
            <b-card class="shadow-sm mb-3">
              <b-form-group label="Câu hỏi hoặc từ khóa">
                <b-form-textarea v-model.trim="testForm.query" rows="5"
                                 placeholder="Ví dụ: Cách xử lý hóa đơn điều chỉnh?" />
              </b-form-group>
              <b-row>
                <b-col md="6">
                  <b-form-group label="Công ty (tùy chọn)">
                    <b-form-input v-model.number="testForm.companyId" type="number" min="1" placeholder="ID công ty" />
                  </b-form-group>
                </b-col>
                <b-col md="6">
                  <b-form-group label="Số đoạn">
                    <b-form-input v-model.number="testForm.limit" type="number" min="1" max="8" />
                  </b-form-group>
                </b-col>
              </b-row>
              <b-form-checkbox v-model="testForm.externalOnly" class="mb-3">
                Chỉ tài liệu được phép gửi ra AI Provider
              </b-form-checkbox>
              <b-button variant="outline-primary" class="mr-2" :disabled="testing" @click="testRetrieval">
                <b-spinner v-if="testing" small class="mr-1" />Truy xuất
              </b-button>
              <b-button variant="primary" :disabled="testing" @click="askAi">
                <i class="fas fa-robot mr-1"></i>Hỏi Gemini
              </b-button>
              <div class="small text-muted mt-3">
                Q&A chỉ gửi các đoạn liên quan đã được đánh dấu cho phép chia sẻ. Không gửi toàn bộ tài liệu.
              </div>
            </b-card>
            <b-card v-if="answer" title="Câu trả lời AI" class="shadow-sm mb-3">
              <div class="answer-text">{{ answer }}</div>
              <div v-if="requestId" class="small text-muted mt-2">Request ID: {{ requestId }}</div>
            </b-card>
          </b-col>
          <b-col lg="7">
            <b-card class="shadow-sm" title="Nguồn được truy xuất">
              <div v-if="!sources.length" class="text-muted">Chưa có kết quả truy xuất.</div>
              <b-card v-for="(source, index) in sources" :key="source.chunkId" class="mb-2 source-card">
                <div class="d-flex justify-content-between">
                  <strong>[S{{ index + 1 }}] {{ source.title }}</strong>
                  <b-badge variant="light">{{ Number(source.score || 0).toFixed(3) }}</b-badge>
                </div>
                <div class="small text-muted mb-2">
                  {{ source.heading || 'Không có heading' }} · {{ source.sourceLocation }}
                </div>
                <pre class="source-content">{{ source.content }}</pre>
              </b-card>
            </b-card>
          </b-col>
        </b-row>
      </b-tab>
    </b-tabs>

    <b-modal ref="documentModal" :title="editingId ? 'Cập nhật tài liệu' : 'Thêm tài liệu kiến thức'"
             size="xl" hide-footer @hidden="resetEditor">
      <b-form @submit.prevent="saveDocument">
        <b-row>
          <b-col md="8">
            <b-form-group label="Tiêu đề *">
              <b-form-input v-model.trim="editor.title" maxlength="255" required />
            </b-form-group>
          </b-col>
          <b-col md="4">
            <b-form-group label="Danh mục">
              <b-form-select v-model="editor.categoryId" :options="categoryOptions" />
            </b-form-group>
          </b-col>
        </b-row>
        <b-row>
          <b-col md="3">
            <b-form-group label="Phạm vi">
              <b-form-select v-model="editor.scope" :options="scopeOptions" />
            </b-form-group>
          </b-col>
          <b-col md="3">
            <b-form-group label="ID công ty" :description="editor.scope === 'GLOBAL' ? 'Không áp dụng với phạm vi toàn hệ thống' : ''">
              <b-form-input v-model.number="editor.companyId" type="number" min="1" :disabled="editor.scope === 'GLOBAL'" />
            </b-form-group>
          </b-col>
          <b-col md="3">
            <b-form-group label="Hiệu lực từ">
              <b-form-input v-model="editor.effectiveFrom" type="datetime-local" />
            </b-form-group>
          </b-col>
          <b-col md="3">
            <b-form-group label="Hiệu lực đến">
              <b-form-input v-model="editor.effectiveTo" type="datetime-local" />
            </b-form-group>
          </b-col>
        </b-row>
        <b-form-checkbox v-model="editor.externalShareAllowed" class="mb-3">
          Cho phép gửi các đoạn liên quan của tài liệu này đến Gemini
        </b-form-checkbox>
        <b-alert show variant="warning" v-if="editor.externalShareAllowed">
          Chỉ bật khi tài liệu không chứa dữ liệu khách hàng, bí mật, token hoặc thông tin nội bộ không được phép chia sẻ.
        </b-alert>

        <template v-if="!editingId">
          <b-form-radio-group v-model="editor.mode" class="mb-3">
            <b-form-radio value="markdown">Soạn Markdown</b-form-radio>
            <b-form-radio value="upload">Tải tài liệu</b-form-radio>
          </b-form-radio-group>
        </template>

        <template v-if="editor.mode === 'markdown'">
          <b-row>
            <b-col lg="6">
              <b-form-group label="Nội dung Markdown *">
                <b-form-textarea v-model="editor.content" rows="18" class="markdown-editor"
                                 placeholder="# Tiêu đề&#10;&#10;Nội dung kiến thức..." />
              </b-form-group>
              <b-button size="sm" variant="outline-secondary" type="button" @click="previewMarkdown">
                <i class="fas fa-eye mr-1"></i>Cập nhật xem trước
              </b-button>
            </b-col>
            <b-col lg="6">
              <label class="font-weight-bold">Xem trước an toàn</label>
              <div class="markdown-preview" v-html="previewHtml"></div>
            </b-col>
          </b-row>
        </template>
        <template v-else-if="!editingId">
          <b-form-group label="Tài liệu *" description="Hỗ trợ MD, TXT, PDF, DOCX, XLSX, XML, XSD; tối đa 10 MB.">
            <b-form-file v-model="editor.file" accept=".md,.txt,.pdf,.docx,.xlsx,.xml,.xsd"
                         browse-text="Chọn file" placeholder="Chưa chọn tài liệu" />
          </b-form-group>
        </template>

        <div class="d-flex justify-content-end mt-4">
          <b-button variant="outline-secondary" class="mr-2" type="button" @click="$refs.documentModal.hide()">Hủy</b-button>
          <b-button variant="primary" type="submit" :disabled="saving">
            <b-spinner v-if="saving" small class="mr-1" />Lưu tài liệu
          </b-button>
        </div>
      </b-form>
    </b-modal>

    <b-modal ref="versionsModal" title="Lịch sử phiên bản" size="lg" ok-only ok-title="Đóng">
      <b-table small bordered responsive show-empty :items="versions" :fields="versionFields" empty-text="Chưa có phiên bản">
        <template #cell(status)="{ item }"><b-badge :variant="statusVariant(item.status)">{{ statusLabel(item.status) }}</b-badge></template>
        <template #cell(fileSize)="{ item }">{{ formatBytes(item.fileSize) }}</template>
        <template #cell(createdAt)="{ item }">{{ formatDateTime(item.createdAt) }}</template>
        <template #cell(promptInjectionDetected)="{ item }">
          <b-badge :variant="item.promptInjectionDetected ? 'warning' : 'success'">
            {{ item.promptInjectionDetected ? 'Cảnh báo' : 'An toàn' }}
          </b-badge>
        </template>
      </b-table>
    </b-modal>

    <b-modal ref="categoriesModal" title="Danh mục kiến thức" size="lg" hide-footer>
      <b-form inline class="mb-3" @submit.prevent="saveCategory">
        <b-form-input v-model.trim="categoryForm.code" class="mr-2 mb-2" placeholder="Mã danh mục" required />
        <b-form-input v-model.trim="categoryForm.name" class="mr-2 mb-2 flex-grow-1" placeholder="Tên danh mục" required />
        <b-button type="submit" variant="primary" class="mb-2">Thêm</b-button>
      </b-form>
      <b-table small bordered :items="categories" :fields="categoryFields">
        <template #cell(active)="{ item }"><b-badge :variant="item.active ? 'success' : 'secondary'">{{ item.active ? 'Bật' : 'Tắt' }}</b-badge></template>
        <template #cell(option)="{ item }">
          <b-button size="sm" variant="outline-danger" @click="deleteCategory(item)"><i class="fas fa-trash"></i></b-button>
        </template>
      </b-table>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import PaginationBar from '@/views/components/pagination_bar.vue'
import { toastSuccess } from '@/utils/toast'

const emptyEditor = () => ({
  title: '', categoryId: null, scope: 'GLOBAL', companyId: null,
  externalShareAllowed: false, effectiveFrom: '', effectiveTo: '',
  mode: 'markdown', content: '', file: null,
})

export default {
  name: 'AdminAiKnowledge',
  components: { PaginationBar },
  data() {
    return {
      busy: false, saving: false, testing: false, editingId: null,
      categories: [], versions: [], previewHtml: '', originalContent: '',
      filters: { q: '', categoryId: null, type: '', status: '' },
      list: { items: [], current_page: 1, per_page: 20, total: 0 },
      editor: emptyEditor(),
      categoryForm: { code: '', name: '' },
      testForm: { query: '', companyId: null, limit: 5, externalOnly: true },
      sources: [], answer: '', requestId: '',
      fields: [
        { key: 'index', label: '#', thStyle: { width: '45px' } },
        { key: 'title', label: 'Tài liệu' },
        { key: 'documentType', label: 'Loại', thStyle: { width: '75px' } },
        { key: 'scope', label: 'Phạm vi', thStyle: { width: '135px' } },
        { key: 'chunkCount', label: 'Đoạn', thStyle: { width: '65px' }, tdClass: 'text-center' },
        { key: 'externalShareAllowed', label: 'Gemini', thStyle: { width: '70px' }, tdClass: 'text-center' },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '125px' } },
        { key: 'option', label: '', thStyle: { width: '115px' } },
      ],
      versionFields: [
        { key: 'versionNumber', label: 'Phiên bản' }, { key: 'originalFilename', label: 'File' },
        { key: 'fileSize', label: 'Dung lượng' }, { key: 'status', label: 'Trạng thái' },
        { key: 'promptInjectionDetected', label: 'Nội dung' }, { key: 'createdAt', label: 'Ngày tạo' },
      ],
      categoryFields: [
        { key: 'code', label: 'Mã' }, { key: 'name', label: 'Tên' },
        { key: 'active', label: 'Trạng thái' }, { key: 'option', label: '', thStyle: { width: '60px' } },
      ],
      typeFilterOptions: [{ value: '', text: 'Tất cả' }, ...['MD', 'TXT', 'PDF', 'DOCX', 'XLSX', 'XML', 'XSD'].map(v => ({ value: v, text: v }))],
      statusFilterOptions: [
        { value: '', text: 'Tất cả' }, { value: 'READY', text: 'Sẵn sàng' },
        { value: 'PROCESSING', text: 'Đang xử lý' }, { value: 'OCR_REQUIRED', text: 'Cần OCR' },
        { value: 'FAILED', text: 'Lỗi' },
      ],
      scopeOptions: [{ value: 'GLOBAL', text: 'Toàn hệ thống' }, { value: 'COMPANY', text: 'Theo doanh nghiệp' }],
    }
  },
  computed: {
    categoryOptions() {
      return [{ value: null, text: 'Không phân loại' }, ...this.categories.map(item => ({ value: item.id, text: item.name }))]
    },
  },
  mounted() {
    this.loadCategories()
    this.fetchDocuments()
  },
  methods: {
    async fetchDocuments() {
      this.busy = true
      try {
        const { data } = await axios.get('/administrator/ai/knowledge/documents', {
          params: {
            q: this.filters.q || undefined, categoryId: this.filters.categoryId || undefined,
            type: this.filters.type || undefined, status: this.filters.status || undefined,
            page: this.list.current_page, size: this.list.per_page,
          },
        })
        this.list.items = data.items || []
        this.list.total = data.total || 0
        this.list.current_page = data.current_page || 1
        this.list.per_page = data.per_page || this.list.per_page
      } finally { this.busy = false }
    },
    async loadCategories() {
      const { data } = await axios.get('/administrator/ai/knowledge/categories')
      this.categories = data || []
    },
    applyFilters() { this.list.current_page = 1; this.fetchDocuments() },
    resetFilters() { this.filters = { q: '', categoryId: null, type: '', status: '' }; this.applyFilters() },
    pageChanged(page) { this.list.current_page = Number(page) || 1; this.fetchDocuments() },
    sizeChanged(size) { this.list.per_page = Number(size) || 20; this.list.current_page = 1; this.fetchDocuments() },
    openCreate() { this.resetEditor(); this.$refs.documentModal.show() },
    async openEdit(item) {
      const { data } = await axios.get(`/administrator/ai/knowledge/documents/${item.id}`)
      this.editingId = item.id
      this.editor = {
        title: data.title, categoryId: data.categoryId, scope: data.scope, companyId: data.companyId,
        externalShareAllowed: !!data.externalShareAllowed,
        effectiveFrom: this.toLocalInput(data.effectiveFrom), effectiveTo: this.toLocalInput(data.effectiveTo),
        mode: data.documentType === 'MD' ? 'markdown' : 'metadata', content: data.content || '', file: null,
      }
      this.originalContent = this.editor.content
      this.previewHtml = ''
      if (this.editor.mode === 'markdown') this.previewMarkdown()
      this.$refs.documentModal.show()
    },
    resetEditor() { this.editingId = null; this.editor = emptyEditor(); this.previewHtml = ''; this.originalContent = '' },
    metadataPayload() {
      return {
        title: this.editor.title, categoryId: this.editor.categoryId || null, scope: this.editor.scope,
        companyId: this.editor.scope === 'COMPANY' ? this.editor.companyId : null,
        externalShareAllowed: !!this.editor.externalShareAllowed,
        effectiveFrom: this.editor.effectiveFrom || null, effectiveTo: this.editor.effectiveTo || null,
      }
    },
    markdownPayload() {
      return { ...this.metadataPayload(), content: this.editor.content,
        effectiveFrom: this.editor.effectiveFrom || null, effectiveTo: this.editor.effectiveTo || null }
    },
    async saveDocument() {
      if (this.editor.scope === 'COMPANY' && !this.editor.companyId) return
      this.saving = true
      try {
        if (this.editingId) {
          await axios.put(`/administrator/ai/knowledge/documents/${this.editingId}/metadata`, this.metadataPayload())
          if (this.editor.mode === 'markdown' && this.editor.content !== this.originalContent) {
            await axios.put(`/administrator/ai/knowledge/documents/${this.editingId}/markdown`, this.markdownPayload())
          }
        } else if (this.editor.mode === 'markdown') {
          await axios.post('/administrator/ai/knowledge/documents/markdown', this.markdownPayload())
        } else {
          if (!this.editor.file) return
          const form = new FormData()
          Object.entries(this.metadataPayload()).forEach(([key, value]) => { if (value !== null) form.append(key, value) })
          form.append('file', this.editor.file)
          await axios.post('/administrator/ai/knowledge/documents/upload', form)
        }
        toastSuccess('Đã lưu tài liệu kiến thức')
        this.$refs.documentModal.hide()
        this.fetchDocuments()
      } finally { this.saving = false }
    },
    async previewMarkdown() {
      const { data } = await axios.post('/administrator/ai/knowledge/preview', { content: this.editor.content || '' })
      this.previewHtml = data.html || ''
    },
    async toggleActive(item, active) {
      await axios.post(`/administrator/ai/knowledge/documents/${item.id}/${active ? 'activate' : 'deactivate'}`)
      toastSuccess(active ? 'Đã kích hoạt tài liệu' : 'Đã vô hiệu hóa tài liệu')
      this.fetchDocuments()
    },
    async reprocess(item) {
      await axios.post(`/administrator/ai/knowledge/documents/${item.id}/reprocess`)
      toastSuccess('Đã xử lý lại tài liệu')
      this.fetchDocuments()
    },
    async remove(item) {
      const ok = await this.$bvModal.msgBoxConfirm(`Xóa tài liệu “${item.title}”?`, { title: 'Xác nhận xóa', okVariant: 'danger', okTitle: 'Xóa', cancelTitle: 'Hủy' })
      if (!ok) return
      await axios.delete(`/administrator/ai/knowledge/documents/${item.id}`)
      toastSuccess('Đã xóa tài liệu')
      this.fetchDocuments()
    },
    async showVersions(item) {
      const { data } = await axios.get(`/administrator/ai/knowledge/documents/${item.id}/versions`)
      this.versions = data || []
      this.$refs.versionsModal.show()
    },
    async rebuildIndex() {
      this.busy = true
      try {
        const { data } = await axios.post('/administrator/ai/knowledge/index/rebuild')
        toastSuccess(`${data.message}: ${data.chunks} đoạn`)
      } finally { this.busy = false }
    },
    openCategories() { this.$refs.categoriesModal.show() },
    async saveCategory() {
      await axios.post('/administrator/ai/knowledge/categories', { ...this.categoryForm, active: true })
      this.categoryForm = { code: '', name: '' }
      await this.loadCategories()
      toastSuccess('Đã thêm danh mục')
    },
    async deleteCategory(item) {
      await axios.delete(`/administrator/ai/knowledge/categories/${item.id}`)
      await this.loadCategories()
      toastSuccess('Đã xóa danh mục')
    },
    async testRetrieval() {
      this.testing = true; this.answer = ''; this.requestId = ''
      try {
        const { data } = await axios.post('/administrator/ai/knowledge/retrieve', {
          query: this.testForm.query, companyId: this.testForm.companyId || null,
          limit: this.testForm.limit, externalOnly: this.testForm.externalOnly,
        })
        this.sources = data.sources || []
      } finally { this.testing = false }
    },
    async askAi() {
      this.testing = true; this.answer = ''; this.requestId = ''
      try {
        const { data } = await axios.post('/administrator/ai/knowledge/ask', {
          question: this.testForm.query, companyId: this.testForm.companyId || null,
        })
        this.answer = data.answer || ''
        this.requestId = data.requestId || ''
        this.sources = data.sources || []
      } finally { this.testing = false }
    },
    statusVariant(status) { return ({ READY: 'success', PROCESSING: 'info', OCR_REQUIRED: 'warning', FAILED: 'danger' })[status] || 'secondary' },
    statusLabel(status) { return ({ READY: 'Sẵn sàng', PROCESSING: 'Đang xử lý', OCR_REQUIRED: 'Cần OCR', FAILED: 'Lỗi' })[status] || status },
    formatBytes(value) { if (!value) return '0 B'; if (value < 1024) return `${value} B`; return `${(value / 1024).toFixed(1)} KB` },
    formatDateTime(value) { return value ? new Date(value).toLocaleString('vi-VN') : '-' },
    toLocalInput(value) { return value ? String(value).slice(0, 16) : '' },
  },
}
</script>

<style scoped>
.knowledge-page { max-width: 100%; }
.knowledge-page ::v-deep th { background: #eef4fa; vertical-align: middle; }
.knowledge-page ::v-deep td { vertical-align: middle; }
.markdown-editor { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
.markdown-preview { border: 1px solid #dfe7ef; border-radius: 6px; min-height: 430px; max-height: 60vh; overflow: auto; padding: 16px; background: #fff; }
.markdown-preview ::v-deep table { width: 100%; border-collapse: collapse; margin-bottom: 1rem; }
.markdown-preview ::v-deep th, .markdown-preview ::v-deep td { border: 1px solid #dee2e6; padding: 6px 8px; }
.markdown-preview ::v-deep pre { background: #f4f6f8; border-radius: 4px; padding: 12px; overflow: auto; }
.source-card { border-left: 4px solid #3b82f6; }
.source-content { background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 5px; max-height: 260px; overflow: auto; padding: 10px; white-space: pre-wrap; word-break: break-word; }
.answer-text { white-space: pre-wrap; line-height: 1.6; }
</style>
