<template>
  <div class="container-fluid py-3 permissions knowledge-page">
    <div class="d-flex align-items-center justify-content-between mb-3 page-header">
      <h4 class="mb-0 font-weight-bold">{{ $t('aiKnowledge.documents.title') }}</h4>
      <div class="d-flex align-items-center page-actions">
        <b-button size="sm" variant="outline-primary" class="mr-2" :disabled="busy" @click="rebuildIndex">
          <b-spinner v-if="busy" small class="mr-1" />
          <i v-else class="fas fa-sync-alt mr-1"></i>{{ $t('aiKnowledge.documents.syncIndex') }}
        </b-button>
        <b-button size="sm" variant="success" @click="openCreate">
          <i class="fas fa-plus mr-1"></i>{{ $t('aiKnowledge.documents.addDocument') }}
        </b-button>
      </div>
    </div>

    <b-card class="mb-3 shadow-sm filter-card">
          <b-row>
            <b-col md="6" lg="4" class="mb-2 mb-lg-0">
              <b-input-group>
                <b-input-group-prepend is-text><i class="fas fa-search text-muted"></i></b-input-group-prepend>
                <b-form-input id="knowledge-search" v-model.trim="filters.q" :placeholder="$t('aiKnowledge.documents.searchPlaceholder')" @keyup.enter="applyFilters" />
              </b-input-group>
            </b-col>
            <b-col md="6" lg="2" class="mb-2 mb-lg-0">
              <b-form-select id="knowledge-category" v-model="filters.categoryId" :options="categoryFilterOptions" />
            </b-col>
            <b-col md="6" lg="2" class="mb-2 mb-lg-0">
              <b-form-select id="knowledge-type" v-model="filters.type" :options="typeFilterOptions" />
            </b-col>
            <b-col md="6" lg="2" class="mb-2 mb-lg-0">
              <b-form-select id="knowledge-status" v-model="filters.status" :options="statusFilterOptions" />
            </b-col>
            <b-col cols="12" lg="2" class="filter-actions">
              <b-button size="sm" variant="primary" @click="applyFilters"><i class="fas fa-filter mr-1"></i>{{ $t('aiKnowledge.documents.filter') }}</b-button>
              <b-button size="sm" variant="outline-secondary" @click="resetFilters">{{ $t('aiKnowledge.documents.clear') }}</b-button>
            </b-col>
          </b-row>
    </b-card>

    <b-card class="shadow-sm table-card">
          <div class="d-flex align-items-center flex-wrap mb-2 table-toolbar">
            <span class="text-muted small"><i class="fas fa-folder-open mr-1"></i>{{ $t('aiKnowledge.documents.documentCount', { count: list.total }) }}</span>
          </div>
          <b-table bordered hover responsive="lg" small show-empty class="mb-0 table-modern table-compact"
                   :busy="busy" :items="list.items" :fields="fields" :empty-text="$t('aiKnowledge.documents.empty')">
            <template #table-busy>
              <div class="text-center py-4"><b-spinner small class="mr-2" />{{ $t('aiKnowledge.documents.loading') }}</div>
            </template>
            <template #cell(index)="{ index }">
              {{ index + 1 + (list.current_page - 1) * list.per_page }}
            </template>
            <template #cell(title)="{ item }">
              <div class="document-title"><i class="fas fa-file-alt mr-2 text-primary"></i>{{ item.title }}</div>
              <div class="small text-muted document-meta">{{ item.originalFilename || '-' }} · v{{ item.currentVersion || '-' }}</div>
              <b-badge v-if="item.promptInjectionDetected" variant="warning" class="mt-1">{{ $t('aiKnowledge.documents.suspicious') }}</b-badge>
            </template>
            <template #cell(documentType)="{ item }">
              <b-badge variant="info">{{ item.documentType || '-' }}</b-badge>
            </template>
            <template #cell(scope)>
              <span>{{ $t('aiKnowledge.documents.global') }}</span>
            </template>
            <template #cell(status)="{ item }">
              <b-badge :variant="statusVariant(item.status)">{{ statusLabel(item.status) }}</b-badge>
              <div class="small mt-1" :class="item.active ? 'text-success' : 'text-muted'">
                <i :class="item.active ? 'fas fa-check-circle mr-1' : 'fas fa-minus-circle mr-1'"></i>{{ item.active ? $t('aiKnowledge.documents.applying') : $t('aiKnowledge.documents.notApplying') }}
              </div>
            </template>
            <template #cell(externalShareAllowed)="{ item }">
              <i :class="item.externalShareAllowed ? 'fas fa-check text-success' : 'fas fa-lock text-muted'"
                 :title="item.externalShareAllowed ? $t('aiKnowledge.documents.externalAllowed') : $t('aiKnowledge.documents.externalDenied')"></i>
            </template>
            <template #cell(option)="{ item }">
              <b-dropdown class="table-action-dropdown" size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
                <template #button-content><i class="fas fa-ellipsis-h"></i></template>
                <b-dropdown-item @click="openEdit(item)"><i class="fas fa-edit mr-2"></i>{{ $t('aiKnowledge.documents.edit') }}</b-dropdown-item>
                <b-dropdown-item @click="showVersions(item)"><i class="fas fa-history mr-2"></i>{{ $t('aiKnowledge.documents.versions') }}</b-dropdown-item>
                <b-dropdown-item v-if="!item.active && item.status === 'READY'" @click="toggleActive(item, true)">
                  <i class="fas fa-toggle-on mr-2"></i>{{ $t('aiKnowledge.documents.activate') }}
                </b-dropdown-item>
                <b-dropdown-item v-if="item.active" @click="toggleActive(item, false)">
                  <i class="fas fa-toggle-off mr-2"></i>{{ $t('aiKnowledge.documents.deactivate') }}
                </b-dropdown-item>
                <b-dropdown-item @click="reprocess(item)"><i class="fas fa-redo mr-2"></i>{{ $t('aiKnowledge.documents.reprocess') }}</b-dropdown-item>
                <b-dropdown-divider />
                <b-dropdown-item class="text-danger" @click="remove(item)"><i class="fas fa-trash mr-2"></i>{{ $t('aiKnowledge.documents.delete') }}</b-dropdown-item>
              </b-dropdown>
            </template>
          </b-table>
          <pagination-bar :current.sync="list.current_page" :size.sync="list.per_page" :total="list.total"
                          :sizes="[10, 20, 50, 100]" @page-change="pageChanged" @size-change="sizeChanged" />
    </b-card>

    <b-modal ref="documentModal" class="knowledge-modal" size="xl" scrollable
             dialog-class="knowledge-document-dialog" body-class="knowledge-document-body"
             :title="editingId ? $t('aiKnowledge.documents.editTitle') : $t('aiKnowledge.documents.addTitle')"
             @shown="documentModalShown" @hidden="resetEditor">
      <b-form id="knowledge-document-form" class="knowledge-document-form" @submit.prevent="saveDocument">
        <div class="metadata-form-grid">
          <b-form-group class="metadata-title-field" :label="$t('aiKnowledge.documents.fieldTitle')" label-for="knowledge-document-title">
            <b-form-input id="knowledge-document-title" v-model.trim="editor.title" maxlength="255"
                          :placeholder="$t('aiKnowledge.documents.titlePlaceholder')" required />
          </b-form-group>
          <b-form-group :label="$t('aiKnowledge.documents.category')" label-for="knowledge-document-category">
            <b-form-select id="knowledge-document-category" v-model="editor.categoryId" :options="categoryOptions" />
          </b-form-group>
          <b-form-group :label="$t('aiKnowledge.documents.effectiveFrom')" label-for="knowledge-effective-from">
            <b-form-input id="knowledge-effective-from" v-model="editor.effectiveFrom" type="datetime-local" />
          </b-form-group>
          <b-form-group :label="$t('aiKnowledge.documents.effectiveTo')" label-for="knowledge-effective-to">
            <b-form-input id="knowledge-effective-to" v-model="editor.effectiveTo" type="datetime-local" />
          </b-form-group>
        </div>

        <div class="document-options-row">
          <b-form-checkbox v-model="editor.externalShareAllowed">{{ $t('aiKnowledge.documents.useGemini') }}</b-form-checkbox>
          <span><i class="fas fa-globe-asia mr-1"></i>{{ $t('aiKnowledge.documents.global') }}</span>
        </div>

        <template v-if="editor.mode === 'markdown' || !editingId">
          <div v-if="!editingId" class="content-toolbar">
            <b-form-radio-group v-model="editor.mode" buttons button-variant="outline-primary"
                                size="sm" class="source-mode-switch">
              <b-form-radio value="markdown"><i class="fas fa-pen mr-1"></i>{{ $t('aiKnowledge.documents.compose') }}</b-form-radio>
              <b-form-radio value="upload"><i class="fas fa-upload mr-1"></i>{{ $t('aiKnowledge.documents.upload') }}</b-form-radio>
            </b-form-radio-group>
          </div>

          <div v-if="editor.mode === 'markdown'" class="editor-grid">
            <div class="editor-pane">
              <div class="editor-pane-heading">
                <label for="knowledge-markdown-editor">{{ $t('aiKnowledge.documents.content') }}</label>
                <b-button size="sm" variant="outline-primary" type="button" :disabled="!editor.content" @click="previewMarkdown">
                  <i class="fas fa-eye mr-1"></i>{{ $t('aiKnowledge.documents.preview') }}
                </b-button>
              </div>
              <b-form-textarea id="knowledge-markdown-editor" v-model="editor.content" rows="10" class="markdown-editor"
                               :placeholder="$t('aiKnowledge.documents.contentPlaceholder')" />
            </div>
            <div class="editor-pane preview-pane">
              <div class="editor-pane-heading"><label>{{ $t('aiKnowledge.documents.previewTitle') }}</label></div>
              <div v-if="previewHtml" class="markdown-preview" v-html="previewHtml"></div>
              <div v-else class="markdown-preview preview-empty">
                <i class="far fa-eye"></i>
                <span>{{ $t('aiKnowledge.documents.noPreview') }}</span>
              </div>
            </div>
          </div>
          <div v-else class="upload-panel">
            <b-form-file v-model="editor.file" class="knowledge-file-picker"
                         accept=".md,.txt,.pdf,.docx,.xlsx,.xml,.xsd"
                         :browse-text="$t('aiKnowledge.documents.chooseDocument')" :placeholder="$t('aiKnowledge.documents.filePlaceholder')"
                         :drop-placeholder="$t('aiKnowledge.documents.dropPlaceholder')" />
          </div>
        </template>
      </b-form>
      <template #modal-footer>
        <div class="document-modal-footer">
          <b-button size="sm" variant="outline-secondary" @click="$refs.documentModal.hide()">{{ $t('common.cancel') }}</b-button>
          <b-button size="sm" variant="primary" type="submit" form="knowledge-document-form" :disabled="!canSaveDocument">
            <b-spinner v-if="saving" small class="mr-1" />
            <i v-else class="fas fa-save mr-1"></i>{{ editingId ? $t('aiKnowledge.documents.saveChanges') : $t('aiKnowledge.documents.saveDocument') }}
          </b-button>
        </div>
      </template>
    </b-modal>

    <b-modal ref="versionsModal" class="knowledge-modal" :title="$t('aiKnowledge.documents.versionHistory')" size="lg" ok-only :ok-title="$t('common.close')">
      <b-table small bordered responsive show-empty class="table-modern table-compact" :items="versions" :fields="versionFields" :empty-text="$t('aiKnowledge.documents.noVersions')">
        <template #cell(status)="{ item }"><b-badge :variant="statusVariant(item.status)">{{ statusLabel(item.status) }}</b-badge></template>
        <template #cell(fileSize)="{ item }">{{ formatBytes(item.fileSize) }}</template>
        <template #cell(ocr)="{ item }">
          <span v-if="item.pageCount">{{ $t('aiKnowledge.documents.pages', { count: item.pageCount }) }}</span>
          <b-badge v-if="item.ocrUsed" variant="info" class="ml-1">
            OCR {{ item.ocrAverageConfidence != null ? `${Math.round(item.ocrAverageConfidence * 100)}%` : '' }}
          </b-badge>
          <span v-if="!item.pageCount && !item.ocrUsed">-</span>
        </template>
        <template #cell(createdAt)="{ item }">{{ formatDateTime(item.createdAt) }}</template>
        <template #cell(promptInjectionDetected)="{ item }">
          <b-badge :variant="item.promptInjectionDetected ? 'warning' : 'success'">
            {{ item.promptInjectionDetected ? $t('aiKnowledge.documents.warning') : $t('aiKnowledge.documents.safe') }}
          </b-badge>
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
  title: '', categoryId: null,
  externalShareAllowed: false, effectiveFrom: '', effectiveTo: '',
  mode: 'markdown', content: '', file: null,
})

export default {
  name: 'AdminAiKnowledge',
  components: { PaginationBar },
  data() {
    return {
      busy: false, saving: false, editingId: null,
      categories: [], versions: [], previewHtml: '', originalContent: '',
      filters: { q: '', categoryId: null, type: '', status: '' },
      list: { items: [], current_page: 1, per_page: 20, total: 0 },
      editor: emptyEditor(),
    }
  },
  computed: {
    fields() {
      return [
        { key: 'index', label: '#', thStyle: { width: '48px' }, tdClass: 'text-center' },
        { key: 'title', label: this.$t('aiKnowledge.documents.columns.document') },
        { key: 'documentType', label: this.$t('aiKnowledge.documents.columns.type'), thStyle: { width: '75px' }, tdClass: 'text-center' },
        { key: 'scope', label: this.$t('aiKnowledge.documents.columns.scope'), thStyle: { width: '135px' } },
        { key: 'chunkCount', label: this.$t('aiKnowledge.documents.columns.chunks'), thStyle: { width: '65px' }, tdClass: 'text-center' },
        { key: 'externalShareAllowed', label: 'Gemini', thStyle: { width: '70px' }, tdClass: 'text-center' },
        { key: 'status', label: this.$t('aiKnowledge.documents.columns.status'), thStyle: { width: '125px' } },
        { key: 'option', label: '', thStyle: { width: '48px' }, tdClass: 'text-center' },
      ]
    },
    versionFields() {
      return [
        { key: 'versionNumber', label: this.$t('aiKnowledge.documents.columns.version') },
        { key: 'originalFilename', label: this.$t('aiKnowledge.documents.columns.file') },
        { key: 'fileSize', label: this.$t('aiKnowledge.documents.columns.size') },
        { key: 'status', label: this.$t('aiKnowledge.documents.columns.status') },
        { key: 'ocr', label: this.$t('aiKnowledge.documents.columns.pagesOcr') },
        { key: 'promptInjectionDetected', label: this.$t('aiKnowledge.documents.columns.content') },
        { key: 'createdAt', label: this.$t('aiKnowledge.documents.columns.createdAt') },
      ]
    },
    typeFilterOptions() {
      return [{ value: '', text: this.$t('aiKnowledge.documents.allFormats') }, ...['MD', 'TXT', 'PDF', 'DOCX', 'XLSX', 'XML', 'XSD'].map(value => ({ value, text: value }))]
    },
    statusFilterOptions() {
      return [
        { value: '', text: this.$t('aiKnowledge.documents.allStatuses') },
        { value: 'READY', text: this.$t('aiKnowledge.documents.ready') },
        { value: 'PROCESSING', text: this.$t('aiKnowledge.documents.processing') },
        { value: 'OCR_REQUIRED', text: this.$t('aiKnowledge.documents.ocrRequired') },
        { value: 'FAILED', text: this.$t('aiKnowledge.documents.failed') },
      ]
    },
    categoryOptions() {
      return [{ value: null, text: this.$t('aiKnowledge.documents.uncategorized') }, ...this.categories.map(item => ({ value: item.id, text: item.name }))]
    },
    categoryFilterOptions() {
      return [{ value: null, text: this.$t('aiKnowledge.documents.allCategories') }, ...this.categories.map(item => ({ value: item.id, text: item.name }))]
    },
    canSaveDocument() {
      if (this.saving || !this.editor.title) return false
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
    documentModalShown() {
      this.$nextTick(() => {
        const body = document.querySelector('.knowledge-document-dialog .knowledge-document-body')
        if (body) body.scrollTop = 0
        const title = document.getElementById('knowledge-document-title')
        if (!this.editingId && title) title.focus({ preventScroll: true })
      })
    },
    async openEdit(item) {
      const { data } = await axios.get(`/administrator/ai/knowledge/documents/${item.id}`)
      this.editingId = item.id
      this.editor = {
        title: data.title, categoryId: data.categoryId,
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
        title: this.editor.title, categoryId: this.editor.categoryId || null, scope: 'GLOBAL', companyId: null,
        externalShareAllowed: !!this.editor.externalShareAllowed,
        effectiveFrom: this.editor.effectiveFrom || null, effectiveTo: this.editor.effectiveTo || null,
      }
    },
    markdownPayload() {
      return { ...this.metadataPayload(), content: this.editor.content,
        effectiveFrom: this.editor.effectiveFrom || null, effectiveTo: this.editor.effectiveTo || null }
    },
    async saveDocument() {
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
        toastSuccess(this.$t('aiKnowledge.documents.saved'))
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
      toastSuccess(this.$t(active ? 'aiKnowledge.documents.activated' : 'aiKnowledge.documents.deactivated'))
      this.fetchDocuments()
    },
    async reprocess(item) {
      await axios.post(`/administrator/ai/knowledge/documents/${item.id}/reprocess`)
      toastSuccess(this.$t('aiKnowledge.documents.reprocessed'))
      this.fetchDocuments()
    },
    async remove(item) {
      const ok = await this.$bvModal.msgBoxConfirm(this.$t('aiKnowledge.documents.deleteConfirm', { name: item.title }), { title: this.$t('aiKnowledge.documents.confirmDelete'), okVariant: 'danger', okTitle: this.$t('aiKnowledge.documents.delete'), cancelTitle: this.$t('common.cancel') })
      if (!ok) return
      await axios.delete(`/administrator/ai/knowledge/documents/${item.id}`)
      toastSuccess(this.$t('aiKnowledge.documents.deleted'))
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
        toastSuccess(this.$t('aiKnowledge.documents.indexResult', { message: data.message, count: data.chunks }))
      } finally { this.busy = false }
    },
    statusVariant(status) { return ({ READY: 'success', PROCESSING: 'info', OCR_REQUIRED: 'warning', FAILED: 'danger' })[status] || 'secondary' },
    statusLabel(status) { return ({ READY: this.$t('aiKnowledge.documents.ready'), PROCESSING: this.$t('aiKnowledge.documents.processing'), OCR_REQUIRED: this.$t('aiKnowledge.documents.ocrRequired'), FAILED: this.$t('aiKnowledge.documents.failed') })[status] || status },
    formatBytes(value) { if (!value) return '0 B'; if (value < 1024) return `${value} B`; return `${(value / 1024).toFixed(1)} KB` },
    formatDateTime(value) { return value ? new Date(value).toLocaleString(this.$i18n.locale === 'en' ? 'en-US' : 'vi-VN') : '-' },
    toLocalInput(value) { return value ? String(value).slice(0, 16) : '' },
  },
}
</script>

<style scoped>
.knowledge-page { max-width: 100%; }
.page-actions { flex-wrap: wrap; }
.table-toolbar { min-height: 28px; }
.filter-actions { display: flex; justify-content: flex-end; gap: 8px; }
.filter-actions .btn { margin: 0 !important; white-space: nowrap; }
.document-title { color: #1f2937; font-weight: 700; }
.document-meta { padding-left: 22px; }
.metadata-form-grid { display: grid; gap: 12px; grid-template-columns: minmax(280px, 1.8fr) repeat(3, minmax(170px, 1fr)); }
.metadata-form-grid ::v-deep .form-group { min-width: 0; margin-bottom: 0; }
.metadata-form-grid ::v-deep input { min-width: 0; width: 100%; }
.document-options-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 12px; padding: 9px 12px; border: 1px solid #e2e8f0; border-radius: 6px; background: #f8fafc; }
.document-options-row ::v-deep .custom-control { margin: 0; }
.document-options-row > span { color: #64748b; font-size: 12px; white-space: nowrap; }
.content-toolbar { display: flex; align-items: center; justify-content: flex-end; gap: 12px; margin-top: 14px; padding-top: 12px; border-top: 1px solid #e2e8f0; }
.source-mode-switch { display: flex; flex: 0 0 auto; min-width: 310px; margin: 0; }
.source-mode-switch ::v-deep .btn { min-width: 0; flex: 1; padding-right: 12px; padding-left: 12px; }
.source-mode-switch ::v-deep .btn:not(.active) { border-color: #d8dee8; background: #fff; color: #52637a; }
.editor-grid { display: grid; gap: 12px; grid-template-columns: repeat(2, minmax(0, 1fr)); margin-top: 10px; }
.editor-pane { min-width: 0; overflow: hidden; border: 1px solid #dfe6ef; border-radius: 7px; background: #fff; }
.editor-pane-heading { display: flex; min-height: 42px; align-items: center; justify-content: space-between; gap: 10px; padding: 5px 10px; border-bottom: 1px solid #edf1f6; background: #f8fafc; }
.editor-pane-heading label { color: #475569; font-size: 12px; font-weight: 700; margin: 0; }
.editor-pane-heading .btn { flex: 0 0 auto; }
.markdown-editor { height: 250px !important; min-height: 200px !important; padding: 12px !important; border: 0 !important; border-radius: 0 !important; font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; line-height: 1.55; resize: vertical; box-shadow: none !important; }
.markdown-preview { height: 250px; overflow: auto; padding: 14px; background: #fff; }
.preview-empty { align-items: center; background: #fff; color: #94a3b8; display: flex; flex-direction: column; gap: 7px; justify-content: center; text-align: center; }
.preview-empty i { font-size: 20px; }
.preview-empty span { font-size: 12px; }
.markdown-preview ::v-deep table { width: 100%; border-collapse: collapse; margin-bottom: 1rem; }
.markdown-preview ::v-deep th, .markdown-preview ::v-deep td { border: 1px solid #dee2e6; padding: 6px 8px; }
.markdown-preview ::v-deep pre { background: #f4f6f8; border-radius: 4px; padding: 12px; overflow: auto; }
.upload-panel { margin-top: 10px; padding: 18px; border: 1px dashed #aebbd0; border-radius: 7px; background: #f8fafc; }
.knowledge-file-picker { margin: 0; }
.document-modal-footer { display: flex; width: 100%; align-items: center; justify-content: flex-end; gap: 8px; }
.document-modal-footer .btn { min-width: 96px; margin: 0 !important; }
.knowledge-page ::v-deep .custom-file-label { border-color: #d8dee8; border-radius: 6px; color: #64748b; min-height: 36px; }
.knowledge-page ::v-deep .custom-file-label::after { background: #f8fafc; border-left-color: #d8dee8; color: #475569; }
@media (max-width: 768px) {
  .page-header { align-items: flex-start !important; flex-direction: column; }
  .page-actions { margin-top: 4px; width: 100%; }
  .page-actions .btn { flex: 1; }
  .filter-actions { margin-top: 8px; }
  .filter-actions .btn { flex: 1; }
  .editor-grid { grid-template-columns: 1fr; }
  .markdown-editor, .markdown-preview { height: 220px !important; }
  .metadata-form-grid { grid-template-columns: 1fr; }
  .source-mode-switch { width: 100%; min-width: 0; }
  .content-toolbar { align-items: stretch; flex-direction: column; }
  .document-options-row { align-items: flex-start; flex-direction: column; }
  .document-modal-footer .btn { flex: 1; }
}
@media (max-width: 991px) {
  .metadata-form-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .metadata-title-field { grid-column: 1 / -1; }
}
@media (max-width: 768px) {
  .metadata-form-grid { grid-template-columns: 1fr; }
  .metadata-title-field { grid-column: auto; }
}
</style>

<style>
.knowledge-document-dialog { max-width: min(1060px, calc(100vw - 32px)); }
.knowledge-document-dialog .modal-content { max-height: calc(100vh - 32px); }
.knowledge-document-dialog .modal-header { background: #fff; border-bottom: 1px solid #e2e8f0; padding: 13px 18px; }
.knowledge-document-dialog .modal-title { color: #263445; font-size: 16px; font-weight: 700; }
.knowledge-document-dialog .knowledge-document-body { background: #fff; padding: 16px 18px; }
.knowledge-document-dialog .modal-footer { background: #fff; border-top: 1px solid #e2e8f0; padding: 10px 16px; }
@media (max-width: 768px) {
  .knowledge-document-dialog { margin: 8px auto; max-width: calc(100vw - 32px); }
  .knowledge-document-dialog .modal-content { max-height: calc(100vh - 16px); }
}
</style>
