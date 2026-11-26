<template>
  <div class="container-fluid py-3 knowledge-page">
    <div class="d-flex align-items-center justify-content-between mb-3 page-header">
      <h4 class="mb-0 font-weight-bold">Thư viện tài liệu AI</h4>
      <div class="d-flex align-items-center page-actions">
        <b-button size="sm" variant="outline-primary" class="mr-2" :disabled="busy" @click="rebuildIndex">
          <b-spinner v-if="busy" small class="mr-1" />
          <i v-else class="fas fa-sync-alt mr-1"></i>Đồng bộ chỉ mục
        </b-button>
        <b-button size="sm" variant="success" @click="openCreate">
          <i class="fas fa-plus mr-1"></i>Thêm tài liệu
        </b-button>
      </div>
    </div>

    <b-tabs class="knowledge-tabs" content-class="pt-3" nav-class="mb-0">
      <b-tab title="Tài liệu" active>
        <b-card class="mb-3 shadow-sm filter-card">
          <b-row>
            <b-col md="4" class="mb-2">
              <b-form-group label="Từ khóa" label-for="knowledge-search" class="mb-0">
                <b-input-group>
                  <b-input-group-prepend is-text><i class="fas fa-search text-muted"></i></b-input-group-prepend>
                  <b-form-input id="knowledge-search" v-model.trim="filters.q" placeholder="Tên tài liệu..." @keyup.enter="applyFilters" />
                </b-input-group>
              </b-form-group>
            </b-col>
            <b-col md="2" class="mb-2">
              <b-form-group label="Danh mục" label-for="knowledge-category" class="mb-0">
                <b-form-select id="knowledge-category" v-model="filters.categoryId" :options="categoryOptions" />
              </b-form-group>
            </b-col>
            <b-col md="2" class="mb-2">
              <b-form-group label="Định dạng" label-for="knowledge-type" class="mb-0">
                <b-form-select id="knowledge-type" v-model="filters.type" :options="typeFilterOptions" />
              </b-form-group>
            </b-col>
            <b-col md="2" class="mb-2">
              <b-form-group label="Trạng thái" label-for="knowledge-status" class="mb-0">
                <b-form-select id="knowledge-status" v-model="filters.status" :options="statusFilterOptions" />
              </b-form-group>
            </b-col>
            <b-col md="2" class="mb-2 d-flex align-items-end filter-actions">
              <b-button size="sm" variant="primary" class="mr-2" @click="applyFilters"><i class="fas fa-filter mr-1"></i>Lọc</b-button>
              <b-button size="sm" variant="outline-secondary" @click="resetFilters">Xóa</b-button>
            </b-col>
          </b-row>
        </b-card>

        <b-card class="shadow-sm table-card">
          <div class="d-flex align-items-center justify-content-between flex-wrap mb-2 table-toolbar">
            <span class="text-muted small"><i class="fas fa-folder-open mr-1"></i>{{ list.total }} tài liệu</span>
            <b-button size="sm" variant="link" class="header-link p-0" @click="openCategories">
              <i class="fas fa-tags mr-1"></i>Quản lý danh mục
            </b-button>
          </div>
          <b-table bordered hover responsive="lg" small show-empty class="mb-0 table-modern table-compact"
                   :busy="busy" :items="list.items" :fields="fields" empty-text="Chưa có tài liệu kiến thức">
            <template #table-busy>
              <div class="text-center py-4"><b-spinner small class="mr-2" />Đang tải tài liệu...</div>
            </template>
            <template #cell(index)="{ index }">
              {{ index + 1 + (list.current_page - 1) * list.per_page }}
            </template>
            <template #cell(title)="{ item }">
              <div class="document-title"><i class="fas fa-file-alt mr-2 text-primary"></i>{{ item.title }}</div>
              <div class="small text-muted document-meta">{{ item.originalFilename || '-' }} · v{{ item.currentVersion || '-' }}</div>
              <b-badge v-if="item.promptInjectionDetected" variant="warning" class="mt-1">Có chỉ dẫn đáng ngờ</b-badge>
            </template>
            <template #cell(documentType)="{ item }">
              <b-badge variant="info">{{ item.documentType || '-' }}</b-badge>
            </template>
            <template #cell(scope)="{ item }">
              <span>{{ item.scope === 'GLOBAL' ? 'Toàn hệ thống' : `Công ty #${item.companyId}` }}</span>
            </template>
            <template #cell(status)="{ item }">
              <b-badge :variant="statusVariant(item.status)">{{ statusLabel(item.status) }}</b-badge>
              <div class="small mt-1" :class="item.active ? 'text-success' : 'text-muted'">
                <i :class="item.active ? 'fas fa-check-circle mr-1' : 'fas fa-minus-circle mr-1'"></i>{{ item.active ? 'Đang áp dụng' : 'Chưa áp dụng' }}
              </div>
            </template>
            <template #cell(externalShareAllowed)="{ item }">
              <i :class="item.externalShareAllowed ? 'fas fa-check text-success' : 'fas fa-lock text-muted'"
                 :title="item.externalShareAllowed ? 'Được phép gửi đến Gemini' : 'Không được phép gửi đến Gemini'"></i>
            </template>
            <template #cell(option)="{ item }">
              <b-dropdown class="table-action-dropdown" size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
                <template #button-content><i class="fas fa-ellipsis-h"></i></template>
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
        <b-card no-body class="shadow-sm mb-3 tool-card">
          <b-card-header class="card-section-header">
            <div>
              <strong><i class="fas fa-flask mr-2 text-primary"></i>Kiểm thử truy xuất</strong>
              <small class="d-block text-muted">Nhập câu hỏi, chọn phạm vi rồi kiểm tra những đoạn tài liệu được sử dụng.</small>
            </div>
          </b-card-header>
          <b-card-body>
            <b-row class="test-form-row">
              <b-col lg="7">
                <b-form-group label="Câu hỏi hoặc từ khóa" label-for="knowledge-query" class="mb-lg-0">
                  <b-form-textarea id="knowledge-query" v-model.trim="testForm.query" rows="4"
                                   placeholder="Ví dụ: Cách xử lý hóa đơn điều chỉnh?" />
                </b-form-group>
              </b-col>
              <b-col lg="5" class="test-options">
                <b-row>
                  <b-col sm="6">
                    <b-form-group label="Công ty (tùy chọn)" label-for="knowledge-company">
                      <b-form-input id="knowledge-company" v-model.number="testForm.companyId" type="number" min="1" placeholder="ID công ty" />
                    </b-form-group>
                  </b-col>
                  <b-col sm="6">
                    <b-form-group label="Số đoạn tối đa" label-for="knowledge-limit">
                      <b-form-input id="knowledge-limit" v-model.number="testForm.limit" type="number" min="1" max="8" />
                    </b-form-group>
                  </b-col>
                </b-row>
                <b-form-checkbox v-model="testForm.externalOnly">
                  Chỉ dùng tài liệu được phép gửi đến Gemini
                </b-form-checkbox>
              </b-col>
            </b-row>
            <div class="test-form-footer">
              <span><i class="fas fa-shield-alt mr-1"></i>Chỉ các đoạn liên quan và được cấp quyền mới được gửi đi.</span>
              <div class="tool-actions">
                <b-button size="sm" variant="outline-primary" :disabled="testing || !testForm.query" @click="testRetrieval">
                  <b-spinner v-if="testing" small class="mr-1" /><i v-else class="fas fa-search mr-1"></i>Truy xuất
                </b-button>
                <b-button size="sm" variant="primary" :disabled="testing || !testForm.query" @click="askAi">
                  <i class="fas fa-robot mr-1"></i>Hỏi Gemini
                </b-button>
              </div>
            </div>
          </b-card-body>
        </b-card>
        <b-card v-if="answer" title="Câu trả lời AI" class="shadow-sm mb-3 answer-card">
          <div class="answer-text">{{ answer }}</div>
          <div v-if="requestId" class="small text-muted mt-2">Request ID: {{ requestId }}</div>
        </b-card>
        <b-card no-body class="shadow-sm source-panel">
          <b-card-header class="card-section-header">
            <div>
              <strong><i class="fas fa-list-alt mr-2 text-primary"></i>Nguồn được truy xuất</strong>
              <small class="d-block text-muted">Các đoạn kiến thức được xếp hạng theo mức độ liên quan.</small>
            </div>
            <b-badge variant="light">{{ sources.length }} nguồn</b-badge>
          </b-card-header>
          <b-card-body>
            <div v-if="!sources.length" class="empty-state">
              <i class="fas fa-search"></i>
              <strong>Chưa có kết quả truy xuất</strong>
              <span>Nhập câu hỏi ở trên để bắt đầu kiểm tra.</span>
            </div>
            <div v-else class="source-grid">
              <b-card v-for="(source, index) in sources" :key="source.chunkId || index" class="source-card">
                <div class="d-flex justify-content-between align-items-start">
                  <strong>[S{{ index + 1 }}] {{ source.title }}</strong>
                  <b-badge variant="light">{{ Number(source.score || 0).toFixed(3) }}</b-badge>
                </div>
                <div class="small text-muted mb-2">
                  {{ source.heading || 'Không có heading' }} · {{ source.sourceLocation }}
                </div>
                <pre class="source-content">{{ source.content }}</pre>
              </b-card>
            </div>
          </b-card-body>
        </b-card>
      </b-tab>
    </b-tabs>

    <b-modal ref="documentModal" class="knowledge-modal" :title="editingId ? 'Cập nhật tài liệu' : 'Thêm tài liệu kiến thức'"
             size="xl" scrollable dialog-class="knowledge-document-dialog"
             body-class="knowledge-document-body" @hidden="resetEditor">
      <b-form id="knowledge-document-form" class="knowledge-document-form" @submit.prevent="saveDocument">
        <div class="document-workspace" :class="{ 'metadata-only': editor.mode === 'metadata' }">
          <section class="document-form-section">
          <div class="document-section-heading">
            <div>
              <strong>Thông tin chung</strong>
              <span>Thông tin và phạm vi sử dụng</span>
            </div>
          </div>
          <b-form-group label="Tiêu đề *" label-for="knowledge-document-title">
            <b-form-input id="knowledge-document-title" v-model.trim="editor.title" maxlength="255"
                          placeholder="Nhập tiêu đề dễ nhận biết" required />
          </b-form-group>
          <b-form-group label="Danh mục" label-for="knowledge-document-category">
            <b-form-select id="knowledge-document-category" v-model="editor.categoryId" :options="categoryOptions" />
          </b-form-group>
          <div class="document-fields-pair">
            <div>
              <b-form-group label="Phạm vi" label-for="knowledge-document-scope">
                <b-form-select id="knowledge-document-scope" v-model="editor.scope" :options="scopeOptions" />
              </b-form-group>
            </div>
            <div>
              <b-form-group label="ID công ty" label-for="knowledge-document-company"
                            :description="editor.scope === 'GLOBAL' ? 'Không áp dụng' : ''">
                <b-form-input id="knowledge-document-company" v-model.number="editor.companyId" type="number" min="1"
                              placeholder="Nhập ID công ty" :disabled="editor.scope === 'GLOBAL'" />
              </b-form-group>
            </div>
          </div>
          <div class="document-dates">
            <div>
              <b-form-group label="Hiệu lực từ" label-for="knowledge-effective-from">
                <b-form-input id="knowledge-effective-from" v-model="editor.effectiveFrom" type="datetime-local" />
              </b-form-group>
            </div>
            <div>
              <b-form-group label="Hiệu lực đến" label-for="knowledge-effective-to">
                <b-form-input id="knowledge-effective-to" v-model="editor.effectiveTo" type="datetime-local" />
              </b-form-group>
            </div>
          </div>
          <div class="share-setting" :class="{ active: editor.externalShareAllowed }">
            <b-form-checkbox v-model="editor.externalShareAllowed">
              <strong>Cho phép gửi đoạn liên quan đến Gemini</strong>
              <span>Chỉ bật với tài liệu không chứa dữ liệu mật hoặc dữ liệu khách hàng.</span>
            </b-form-checkbox>
          </div>
          </section>

          <section v-if="editor.mode === 'markdown' || !editingId" class="document-form-section content-form-section">
          <div class="document-section-heading source-heading">
            <div>
              <strong>Nội dung tài liệu</strong>
              <span>{{ editor.mode === 'markdown' ? 'Soạn thảo và xem trước' : 'Tải tài liệu lên để xử lý' }}</span>
            </div>
          </div>
          <b-form-radio-group v-if="!editingId" v-model="editor.mode" buttons button-variant="outline-primary"
                              size="sm" class="source-mode-switch">
              <b-form-radio value="markdown"><i class="fas fa-edit mr-1"></i>Soạn Markdown</b-form-radio>
              <b-form-radio value="upload"><i class="fas fa-upload mr-1"></i>Tải tài liệu</b-form-radio>
          </b-form-radio-group>

          <template v-if="editor.mode === 'markdown'">
            <div class="editor-grid">
              <div>
                <div class="editor-pane-heading">
                  <label for="knowledge-markdown-editor">Nội dung Markdown *</label>
                  <b-button size="sm" variant="outline-secondary" type="button" :disabled="!editor.content" @click="previewMarkdown">
                    <i class="fas fa-eye mr-1"></i>Xem trước
                  </b-button>
                </div>
                <b-form-textarea id="knowledge-markdown-editor" v-model="editor.content" rows="10" class="markdown-editor"
                                 :placeholder="'# Tiêu đề\n\nNội dung kiến thức...'" />
              </div>
              <div>
                <div class="editor-pane-heading">
                  <label>Xem trước an toàn</label>
                  <span class="text-muted small"><i class="fas fa-shield-alt mr-1"></i>Đã lọc mã nguy hiểm</span>
                </div>
                <div v-if="previewHtml" class="markdown-preview" v-html="previewHtml"></div>
                <div v-else class="markdown-preview preview-empty">
                  <i class="far fa-eye"></i>
                  <span>Nhập nội dung rồi chọn “Xem trước”.</span>
                </div>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="upload-panel">
              <div class="upload-panel-icon"><i class="fas fa-cloud-upload-alt"></i></div>
              <div class="upload-panel-copy">
                <strong>Chọn tài liệu từ máy tính</strong>
                <span>Hỗ trợ MD, TXT, PDF, DOCX, XLSX, XML, XSD · Tối đa 10 MB</span>
              </div>
              <b-form-file v-model="editor.file" class="knowledge-file-picker"
                           accept=".md,.txt,.pdf,.docx,.xlsx,.xml,.xsd"
                           browse-text="Chọn file" placeholder="Chưa chọn tài liệu" />
            </div>
          </template>
          </section>
        </div>
      </b-form>
      <template #modal-footer>
        <div class="document-modal-actions">
          <b-button size="sm" variant="outline-secondary" @click="$refs.documentModal.hide()">Hủy</b-button>
          <b-button size="sm" variant="primary" type="submit" form="knowledge-document-form" :disabled="!canSaveDocument">
            <b-spinner v-if="saving" small class="mr-1" />
            <i v-else class="fas fa-save mr-1"></i>{{ editingId ? 'Lưu thay đổi' : 'Lưu tài liệu' }}
          </b-button>
        </div>
      </template>
    </b-modal>

    <b-modal ref="versionsModal" class="knowledge-modal" title="Lịch sử phiên bản" size="lg" ok-only ok-title="Đóng">
      <b-table small bordered responsive show-empty class="table-modern table-compact" :items="versions" :fields="versionFields" empty-text="Chưa có phiên bản">
        <template #cell(status)="{ item }"><b-badge :variant="statusVariant(item.status)">{{ statusLabel(item.status) }}</b-badge></template>
        <template #cell(fileSize)="{ item }">{{ formatBytes(item.fileSize) }}</template>
        <template #cell(ocr)="{ item }">
          <span v-if="item.pageCount">{{ item.pageCount }} trang</span>
          <b-badge v-if="item.ocrUsed" variant="info" class="ml-1">
            OCR {{ item.ocrAverageConfidence != null ? `${Math.round(item.ocrAverageConfidence * 100)}%` : '' }}
          </b-badge>
          <span v-if="!item.pageCount && !item.ocrUsed">-</span>
        </template>
        <template #cell(createdAt)="{ item }">{{ formatDateTime(item.createdAt) }}</template>
        <template #cell(promptInjectionDetected)="{ item }">
          <b-badge :variant="item.promptInjectionDetected ? 'warning' : 'success'">
            {{ item.promptInjectionDetected ? 'Cảnh báo' : 'An toàn' }}
          </b-badge>
        </template>
      </b-table>
    </b-modal>

    <b-modal ref="categoriesModal" class="knowledge-modal" title="Danh mục kiến thức" size="lg" hide-footer>
      <b-form class="mb-3" @submit.prevent="saveCategory">
        <b-row>
          <b-col md="4">
            <b-form-group label="Mã danh mục" class="mb-2">
              <b-form-input v-model.trim="categoryForm.code" placeholder="Ví dụ: POLICY" required />
            </b-form-group>
          </b-col>
          <b-col md="6">
            <b-form-group label="Tên danh mục" class="mb-2">
              <b-form-input v-model.trim="categoryForm.name" placeholder="Nhập tên danh mục" required />
            </b-form-group>
          </b-col>
          <b-col md="2" class="d-flex align-items-end mb-2">
            <b-button type="submit" variant="primary" block :disabled="!categoryForm.code || !categoryForm.name">Thêm</b-button>
          </b-col>
        </b-row>
      </b-form>
      <b-table small bordered responsive show-empty class="mb-0 table-modern table-compact" :items="categories" :fields="categoryFields" empty-text="Chưa có danh mục">
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
        { key: 'index', label: '#', thStyle: { width: '48px' }, tdClass: 'text-center' },
        { key: 'title', label: 'Tài liệu' },
        { key: 'documentType', label: 'Loại', thStyle: { width: '75px' }, tdClass: 'text-center' },
        { key: 'scope', label: 'Phạm vi', thStyle: { width: '135px' } },
        { key: 'chunkCount', label: 'Đoạn', thStyle: { width: '65px' }, tdClass: 'text-center' },
        { key: 'externalShareAllowed', label: 'Gemini', thStyle: { width: '70px' }, tdClass: 'text-center' },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '125px' } },
        { key: 'option', label: '', thStyle: { width: '48px' }, tdClass: 'text-center' },
      ],
      versionFields: [
        { key: 'versionNumber', label: 'Phiên bản' }, { key: 'originalFilename', label: 'File' },
        { key: 'fileSize', label: 'Dung lượng' }, { key: 'status', label: 'Trạng thái' },
        { key: 'ocr', label: 'Trang/OCR' },
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
    canSaveDocument() {
      if (this.saving || !this.editor.title) return false
      if (this.editor.scope === 'COMPANY' && !this.editor.companyId) return false
      if (this.editor.mode === 'markdown' && !this.editor.content) return false
      if (!this.editingId && this.editor.mode === 'upload' && !this.editor.file) return false
      return true
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
.knowledge-page { max-width: 100%; color: #1f2937; font-size: 13px; }
.page-header h4 { color: #1f2937; }
.page-actions { flex-wrap: wrap; }
.knowledge-page ::v-deep .btn { border-radius: 6px; font-weight: 600; }
.knowledge-page ::v-deep .form-control,
.knowledge-page ::v-deep .custom-select { border-color: #d8dee8; border-radius: 6px; min-height: 34px; }
.knowledge-page ::v-deep .form-control:focus,
.knowledge-page ::v-deep .custom-select:focus { border-color: #93b4ef; box-shadow: 0 0 0 3px rgba(37, 99, 235, .1); }
.knowledge-page ::v-deep .form-group { margin-bottom: 12px; }
.knowledge-page ::v-deep .form-group label { color: #475569; font-size: 12px; font-weight: 700; margin-bottom: 4px; }
.knowledge-page ::v-deep .card { border: 1px solid #e6ebf2; border-radius: 8px; }
.knowledge-page ::v-deep .card-header { background: #fff; border-bottom: 1px solid #edf1f6; padding: 13px 16px; }
.knowledge-page ::v-deep .card-body { padding: 16px; }
.card-section-header { align-items: center; display: flex; justify-content: space-between; gap: 16px; }
.card-section-header strong { color: #263445; font-size: 14px; }
.card-section-header small { color: #7b8797 !important; font-size: 12px; font-weight: 400; margin-top: 3px; }
.header-link { color: #2563eb; font-size: 12px; padding: 4px 0; }
.table-toolbar { min-height: 28px; }
.filter-actions { gap: 8px; }
.filter-actions .btn { margin: 0 !important; }
.knowledge-tabs ::v-deep .nav-tabs { border-bottom: 1px solid #d8dee8; }
.knowledge-tabs ::v-deep .nav-link { border: 0; border-bottom: 2px solid transparent; color: #64748b; font-weight: 600; padding: 10px 16px; }
.knowledge-tabs ::v-deep .nav-link:hover { color: #2563eb; }
.knowledge-tabs ::v-deep .nav-link.active { background: transparent; border-bottom-color: #2563eb; color: #2563eb; }
.knowledge-page ::v-deep .table { margin-bottom: 0; }
.knowledge-page ::v-deep .table thead th { background: #f8fafc; border-bottom: 2px solid #e5e7eb; color: #475569; font-size: 12px; font-weight: 700; vertical-align: middle; white-space: nowrap; }
.knowledge-page ::v-deep .table td { vertical-align: middle; }
.knowledge-page ::v-deep .table-modern.table-compact td,
.knowledge-page ::v-deep .table-modern.table-compact th { padding: 9px 12px; }
.knowledge-page ::v-deep .table-modern tbody tr:hover { background: #f8fbff; }
.document-title { color: #1f2937; font-weight: 700; }
.document-meta { padding-left: 22px; }
.test-form-row { align-items: stretch; }
.test-form-row ::v-deep textarea { min-height: 102px; resize: vertical; }
.test-options { border-left: 1px solid #e8edf4; padding-left: 22px; }
.test-options ::v-deep .custom-control-label { line-height: 1.5; }
.test-form-footer { align-items: center; border-top: 1px solid #edf1f6; color: #64748b; display: flex; flex-wrap: wrap; gap: 12px; justify-content: space-between; margin-top: 14px; padding-top: 12px; }
.test-form-footer > span { font-size: 12px; }
.tool-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.tool-actions .btn { margin: 0; min-width: 104px; }
.source-grid { display: grid; gap: 12px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.source-card { border-left: 3px solid #3b82f6 !important; border-radius: 6px !important; }
.source-card ::v-deep .card-body { padding: 12px; min-height: 0; }
.source-content { background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 5px; color: #334155; font-size: 12px; line-height: 1.5; margin: 0; max-height: 260px; overflow: auto; padding: 10px; white-space: pre-wrap; word-break: break-word; }
.empty-state { align-items: center; color: #64748b; display: flex; flex-direction: column; gap: 4px; justify-content: center; min-height: 126px; text-align: center; }
.empty-state i { color: #94a3b8; font-size: 22px; margin-bottom: 3px; }
.empty-state strong { color: #475569; }
.answer-card ::v-deep .card-body { background: #f8fafc; }
.answer-text { line-height: 1.65; white-space: pre-wrap; }
.document-workspace { align-items: stretch; display: grid; gap: 14px; grid-template-columns: minmax(320px, 350px) minmax(0, 1fr); }
.document-workspace.metadata-only { grid-template-columns: minmax(0, 1fr); }
.document-form-section { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; min-width: 0; padding: 14px 16px 12px; }
.document-form-section ::v-deep .form-group { margin-bottom: 11px; }
.document-form-section ::v-deep .form-group label { color: #475569; font-size: 12px; font-weight: 700; margin-bottom: 4px; }
.document-form-section ::v-deep .form-control,
.document-form-section ::v-deep .custom-select { border-color: #d8dee8; border-radius: 6px; min-height: 34px; }
.document-section-heading { align-items: flex-start; border-bottom: 1px solid #edf1f6; display: flex; justify-content: space-between; gap: 16px; margin-bottom: 12px; padding-bottom: 10px; }
.document-section-heading strong { color: #263445; display: block; font-size: 14px; }
.document-section-heading span { color: #7b8797; display: block; font-size: 12px; margin-top: 2px; }
.document-fields-pair { display: grid; gap: 10px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.document-dates > div { min-width: 0; }
.document-dates ::v-deep input { min-width: 0; width: 100%; }
.document-fields-pair ::v-deep .form-text { font-size: 11px; line-height: 1.3; margin-top: 2px; }
.source-mode-switch { display: flex; margin-bottom: 12px; width: 100%; }
.source-mode-switch ::v-deep .btn { flex: 1; min-width: 0; }
.share-setting { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; margin-top: 2px; padding: 9px 10px; transition: background .15s ease, border-color .15s ease; }
.share-setting.active { background: #fff8e6; border-color: #f4cf75; }
.share-setting ::v-deep .custom-control-label strong { color: #334155; display: block; font-size: 13px; }
.share-setting ::v-deep .custom-control-label span { color: #64748b; display: block; font-size: 12px; line-height: 1.4; margin-top: 1px; }
.editor-grid { display: grid; gap: 12px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.editor-grid > div { min-width: 0; }
.editor-pane-heading { align-items: center; display: flex; justify-content: space-between; gap: 10px; margin-bottom: 6px; min-height: 32px; }
.editor-pane-heading label { color: #475569; font-size: 12px; font-weight: 700; margin: 0; }
.markdown-editor { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; height: 280px; min-height: 220px; resize: vertical; }
.markdown-preview { background: #fff; border: 1px solid #d8dee8; border-radius: 6px; height: 280px; overflow: auto; padding: 14px; }
.preview-empty { align-items: center; background: #f8fafc; color: #94a3b8; display: flex; flex-direction: column; gap: 8px; justify-content: center; text-align: center; }
.preview-empty i { font-size: 26px; }
.markdown-preview ::v-deep table { width: 100%; border-collapse: collapse; margin-bottom: 1rem; }
.markdown-preview ::v-deep th, .markdown-preview ::v-deep td { border: 1px solid #dee2e6; padding: 6px 8px; }
.markdown-preview ::v-deep pre { background: #f4f6f8; border-radius: 4px; padding: 12px; overflow: auto; }
.upload-panel { align-items: center; background: #f8fafc; border: 1px dashed #aebbd0; border-radius: 8px; display: grid; gap: 12px; grid-template-columns: 44px minmax(0, 1fr); min-height: 190px; padding: 20px; }
.upload-panel-icon { align-items: center; background: #eaf2ff; border-radius: 8px; color: #2563eb; display: flex; font-size: 20px; height: 44px; justify-content: center; width: 44px; }
.upload-panel-copy strong { color: #334155; display: block; }
.upload-panel-copy span { color: #64748b; display: block; font-size: 12px; margin-top: 3px; }
.knowledge-file-picker { grid-column: 1 / -1; margin: 0; }
.document-modal-actions { align-items: center; display: flex; gap: 8px; justify-content: flex-end; width: 100%; }
.document-modal-actions .btn { margin: 0 !important; min-width: 100px; }
.knowledge-page ::v-deep .custom-file-label { border-color: #d8dee8; border-radius: 6px; color: #64748b; min-height: 36px; }
.knowledge-page ::v-deep .custom-file-label::after { background: #f8fafc; border-left-color: #d8dee8; color: #475569; }
.knowledge-page ::v-deep .badge { border-radius: 999px; font-size: 11px; font-weight: 700; padding: 4px 8px; }

@media (max-width: 768px) {
  .page-header { align-items: flex-start !important; flex-direction: column; }
  .page-actions { margin-top: 4px; width: 100%; }
  .page-actions .btn { flex: 1; }
  .card-section-header { align-items: flex-start; }
  .source-grid { grid-template-columns: 1fr; }
  .test-form-footer { align-items: flex-start; flex-direction: column; }
  .tool-actions, .tool-actions .btn { width: 100%; }
  .document-workspace { grid-template-columns: 1fr; }
  .editor-grid { grid-template-columns: 1fr; }
  .markdown-editor, .markdown-preview { height: 220px; }
}
@media (max-width: 991px) {
  .test-options { border-left: 0; border-top: 1px solid #e8edf4; margin-top: 12px; padding-left: 15px; padding-top: 12px; }
  .document-workspace { grid-template-columns: 1fr; }
}
</style>

<style>
.knowledge-document-dialog { max-width: min(1120px, calc(100vw - 32px)); }
.knowledge-document-dialog .modal-content { max-height: calc(100vh - 32px); }
.knowledge-document-dialog .modal-header { background: #fff; border-bottom: 1px solid #e2e8f0; padding: 13px 18px; }
.knowledge-document-dialog .modal-title { color: #263445; font-size: 16px; font-weight: 700; }
.knowledge-document-dialog .knowledge-document-body { background: #f5f7fb; padding: 14px 16px; }
.knowledge-document-dialog .modal-footer { background: #fff; border-top: 1px solid #e2e8f0; padding: 10px 16px; }
@media (max-width: 768px) {
  .knowledge-document-dialog { margin: 8px auto; max-width: calc(100vw - 16px); }
  .knowledge-document-dialog .modal-content { max-height: calc(100vh - 16px); }
}
</style>
