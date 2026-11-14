<template>
  <div class="container-fluid py-3 template-designer">
    <div class="designer-heading">
      <div>
        <h3 class="mb-1">Thiết kế mẫu hóa đơn</h3>
        <span class="text-muted"
          >Soạn thảo, sắp xếp bố cục và theo dõi thay đổi ngay trên trang.</span
        >
      </div>
      <div class="d-flex action-bar">
        <b-button variant="outline-secondary" @click="showLibrary = !showLibrary"
          >Quản lý mẫu</b-button
        >
        <b-button variant="secondary" @click="$router.back()">Trở lại</b-button>
      </div>
    </div>
    <b-alert v-if="error" show dismissible variant="danger" @dismissed="error = ''">{{
      error
    }}</b-alert>
    <b-alert v-if="notice" show dismissible variant="success" @dismissed="notice = ''">{{
      notice
    }}</b-alert>
    <b-collapse :visible="showLibrary || !details">
      <b-card class="mb-3">
        <div class="row align-items-end">
          <div class="col-md-4">
            <b-form-group label="Mẫu đang quản lý">
              <b-form-select
                :disabled="busy"
                v-model="selectedTemplateId"
                :options="templateOptions"
                @change="load"
              />
            </b-form-group>
          </div>
          <div class="col-md-4">
            <b-form-group label="Tên mẫu mới"
              ><b-form-input v-model.trim="newName" maxlength="255"
            /></b-form-group>
          </div>
          <div class="col-md-2">
            <b-form-group label="Loại hóa đơn">
              <b-form-select
                v-model.number="newCategory"
                :options="[
                  { value: 1, text: 'Giá trị gia tăng' },
                  { value: 2, text: 'Bán hàng' }
                ]"
              />
            </b-form-group>
          </div>
          <div class="col-md-2">
            <b-button class="mb-3" variant="primary" :disabled="busy || !newName" @click="create"
              >Tạo mẫu</b-button
            >
          </div>
        </div>
        <b-pagination
          v-if="total > 50"
          v-model="page"
          :total-rows="total"
          :per-page="50"
          @change="fetchTemplates"
        />
      </b-card>
    </b-collapse>
    <template v-if="details && draft">
      <div class="document-header">
        <div class="d-flex align-items-center action-bar">
          <strong>{{ details.template.name }}</strong>
          <b-badge :variant="dirty ? 'warning' : 'success'">{{
            dirty ? 'Chưa lưu' : 'Đã lưu'
          }}</b-badge>
        </div>
        <div class="d-flex flex-wrap action-bar">
          <b-button size="sm" variant="primary" :disabled="busy" @click="save">Lưu nháp</b-button>
          <b-button size="sm" variant="outline-secondary" :disabled="busy" @click="upgradeCatalog"
            >Cập nhật danh mục</b-button
          >
          <b-button size="sm" variant="outline-primary" :disabled="busy" @click="preview"
            >Bản in PDF</b-button
          >
          <b-button size="sm" variant="success" :disabled="busy" @click="publish">
            {{ busy ? 'Đang xử lý…' : 'Kiểm tra và công bố' }}
          </b-button>
        </div>
      </div>
      <div class="designer-ribbon">
        <div class="ribbon-tabs" role="tablist" aria-label="Công cụ thiết kế">
          <button
            v-for="tab in editorTabs"
            :key="tab.value"
            type="button"
            role="tab"
            :aria-selected="activeTab === tab.value"
            :class="{ active: activeTab === tab.value }"
            @click="activeTab = tab.value"
          >
            {{ tab.text }}
          </button>
          <div class="zoom-control">
            <label for="invoice-designer-zoom" class="mb-0">Thu phóng</label>
            <input
              id="invoice-designer-zoom"
              v-model.number="zoom"
              type="range"
              min="40"
              max="150"
              step="5"
            />
            <span>{{ zoom }}%</span>
          </div>
        </div>
        <fieldset v-if="layout" :disabled="busy" class="ribbon-content">
          <div v-if="activeTab === 'home'" class="format-toolbar">
            <div class="toolbar-group">
              <button type="button" title="Hoàn tác (Ctrl+Z)" :disabled="!canUndo" @click="undo">
                ↶
              </button>
              <button type="button" title="Làm lại (Ctrl+Y)" :disabled="!canRedo" @click="redo">
                ↷
              </button>
            </div>
            <div class="toolbar-group">
              <select v-model="layout.theme.fontAssetFamily" aria-label="Phông chữ">
                <option value="body">Times New Roman</option>
                <option value="sans">Arial</option>
              </select>
              <input
                :value="selectedStyle('fontSizePt', layout.theme.fontSizePt)"
                type="number"
                min="7"
                max="36"
                class="font-size-input"
                aria-label="Cỡ chữ"
                @change="setStyle('fontSizePt', Number($event.target.value))"
              />
            </div>
            <div class="toolbar-group">
              <button
                type="button"
                title="In đậm"
                :class="{ active: selectedStyle('fontWeight', 400) === 700 }"
                @click="
                  setStyle('fontWeight', selectedStyle('fontWeight', 400) === 700 ? 400 : 700)
                "
              >
                <strong>B</strong>
              </button>
              <button
                type="button"
                title="In nghiêng"
                :class="{
                  active: selectedStyle('fontStyle', 'normal') === 'italic'
                }"
                @click="
                  setStyle(
                    'fontStyle',
                    selectedStyle('fontStyle', 'normal') === 'italic' ? 'normal' : 'italic'
                  )
                "
              >
                <em>I</em>
              </button>
              <label class="color-tool" title="Màu chữ"
                >A<input
                  type="color"
                  :value="selectedStyle('color', layout.theme.color)"
                  aria-label="Màu chữ"
                  @input="setStyle('color', $event.target.value)"
              /></label>
              <label class="color-tool" title="Màu nền"
                >Nền<input
                  type="color"
                  :value="selectedStyle('backgroundColor', '#ffffff')"
                  aria-label="Màu nền"
                  @input="setStyle('backgroundColor', $event.target.value)"
              /></label>
            </div>
            <div class="toolbar-group">
              <button
                v-for="align in alignments"
                :key="align.value"
                type="button"
                :title="align.text"
                :class="{
                  active: selectedStyle('textAlign', 'left') === align.value
                }"
                @click="setStyle('textAlign', align.value)"
              >
                <span :class="['alignment-icon', align.value]" aria-hidden="true"
                  ><i /><i /><i
                /></span>
              </button>
            </div>
            <div class="toolbar-group">
              <button
                type="button"
                :disabled="!selected || selected.id === layout.body.id"
                @click="duplicateSelected"
              >
                Nhân đôi
              </button>
              <button
                type="button"
                :disabled="!selected || selected.id === layout.body.id"
                @click="deleteSelected"
              >
                Xóa
              </button>
            </div>
            <small class="text-muted">{{
              selected
                ? 'Đang chọn: ' + blockLabel(selected)
                : 'Chọn một phần tử trên trang để định dạng'
            }}</small>
          </div>
          <div v-else-if="activeTab === 'insert'" class="insert-toolbar">
            <button
              v-for="type in blockTypes"
              :key="type.value"
              type="button"
              draggable="true"
              @dragstart="dragPalette($event, type.value)"
              @click="addBlock(type.value)"
            >
              {{ type.text }}
            </button>
            <label class="btn btn-sm btn-outline-secondary mb-0"
              >Ảnh / logo
              <input
                type="file"
                class="d-none"
                accept="image/png,image/jpeg"
                @change="uploadImage"
              />
            </label>
          </div>
          <div v-else-if="activeTab === 'page'" class="page-toolbar">
            <b-button size="sm" variant="outline-primary" @click="applyDefaultLayout"
              >Dùng mẫu thiết kế mới</b-button
            >
            <label
              >Khổ giấy
              <b-form-select size="sm" v-model="layout.page.format" :options="['A4', 'A5']"
            /></label>
            <label
              >Hướng trang
              <b-form-select
                size="sm"
                v-model="layout.page.orientation"
                :options="[
                  { value: 'portrait', text: 'Dọc' },
                  { value: 'landscape', text: 'Ngang' }
                ]"
            /></label>
            <label v-for="(value, side) in layout.page.marginMm" :key="side"
              >{{ marginLabels[side] }} (mm)
              <b-form-input
                size="sm"
                v-model.number="layout.page.marginMm[side]"
                type="number"
                min="3"
                max="40"
              />
            </label>
            <label
              >Cỡ chữ mặc định
              <b-form-input
                size="sm"
                v-model.number="layout.theme.fontSizePt"
                type="number"
                min="7"
                max="32"
            /></label>
            <label
              >Giãn dòng
              <b-form-input
                size="sm"
                v-model.number="layout.theme.lineHeight"
                type="number"
                min="1"
                max="2"
                step="0.1"
            /></label>
          </div>
        </fieldset>
        <div v-if="activeTab === 'versions'" class="versions-panel">
          <div class="d-flex flex-wrap action-bar mb-3">
            <b-button size="sm" variant="outline-secondary" :disabled="busy" @click="clone"
              >Nhân bản mẫu</b-button
            >
            <b-button size="sm" variant="outline-danger" :disabled="busy" @click="archive"
              >Ngừng sử dụng</b-button
            >
          </div>
          <div v-for="version in details.versions" :key="version.id" class="version-row">
            <span>Phiên bản {{ version.versionNo }} · {{ version.publishedAt }}</span>
            <div>
              <b-button
                size="sm"
                variant="outline-primary"
                :disabled="busy"
                @click="restore(version)"
                >Tạo nháp từ bản này</b-button
              >
              <b-button
                v-if="formId"
                size="sm"
                variant="success"
                class="ml-2"
                :disabled="busy"
                @click="bind(version)"
                >Dùng cho hóa đơn mới</b-button
              >
            </div>
          </div>
          <span v-if="!details.versions.length" class="text-muted">Chưa có bản đã công bố</span>
        </div>
      </div>
      <div v-if="layout" class="designer-workspace">
        <aside class="designer-sidebar">
          <h6>Thêm phần tử</h6>
          <p class="sidebar-help">Kéo vào trang hoặc bấm để chèn.</p>
          <div class="block-palette">
            <button
              v-for="type in blockTypes"
              :key="type.value"
              type="button"
              draggable="true"
              :disabled="busy"
              @dragstart="dragPalette($event, type.value)"
              @click="addBlock(type.value)"
            >
              <span aria-hidden="true">＋</span>{{ type.text }}
            </button>
          </div>
          <label class="sample-rows"
            >Dữ liệu minh họa
            <b-form-select
              size="sm"
              v-model.number="sampleRows"
              :options="[
                { value: 0, text: 'Không có dòng hàng' },
                { value: 1, text: '1 dòng hàng' },
                { value: 3, text: '3 dòng hàng' },
                { value: 10, text: '10 dòng hàng' },
                { value: 60, text: '60 dòng hàng' }
              ]"
            />
          </label>
          <details class="layout-outline">
            <summary>Cấu trúc bố cục</summary>
            <fieldset :disabled="busy">
              <layout-block-tree
                :blocks="layout.body.children"
                :selected-id="selected ? selected.id : ''"
                @select="selected = $event"
              />
            </fieldset>
          </details>
          <p class="sidebar-help mt-3">
            Thả sát cạnh trái/phải để xếp cạnh nhau. Thả ở giữa nhóm để đưa vào nhóm.
          </p>
        </aside>
        <main class="designer-document">
          <invoice-layout-canvas
            :key="selectedTemplateId"
            :layout="layout"
            :template-id="selectedTemplateId"
            :base="base"
            :selected-id="selected ? selected.id : ''"
            :zoom="zoom"
            :rows="sampleRows"
            :disabled="busy"
            @select="selectBlock"
            @move="moveBlock"
            @insert="insertBlock"
            @text="changeText"
            @shortcut="handleShortcut"
          />
        </main>
        <aside class="designer-inspector">
          <fieldset :disabled="busy">
            <b-card v-if="selected" title="Thuộc tính khối" class="mb-3">
              <b-form-group v-if="selected.type === 'text'" label="Nội dung"
                ><b-form-textarea v-model="selected.text" rows="3"
              /></b-form-group>
              <template v-if="selected.type === 'field'">
                <b-form-group label="Nhãn"><b-form-input v-model="selected.label" /></b-form-group>
                <b-form-group label="Trường dữ liệu"
                  ><b-form-select v-model="selected.fieldKey" :options="scalarFields"
                /></b-form-group>
                <b-form-group label="Định dạng"
                  ><b-form-select v-model="selected.formatter" :options="formatters"
                /></b-form-group>
              </template>
              <b-form-group v-if="['table'].includes(selected.type)" label="Danh sách dữ liệu">
                <b-form-select
                  v-model="selected.dataSource"
                  :options="repeatingDatasets"
                  @change="changeDataset"
                />
              </b-form-group>
              <template v-if="selected.columns">
                <div
                  v-for="(column, index) in selected.columns"
                  :key="column.id"
                  class="column-settings"
                >
                  <b-form-input v-model="column.label" placeholder="Tên cột" />
                  <b-form-select
                    v-model="column.fieldKey"
                    :options="fieldsFor(selected.dataSource, column.fieldKey)"
                  />
                  <b-form-input
                    v-model.number="column.widthPercent"
                    type="number"
                    min="1"
                    max="100"
                    title="Độ rộng %"
                  />
                  <b-form-select v-model="column.formatter" :options="formatters" />
                  <b-form-select
                    v-model="column.style.textAlign"
                    :options="alignments"
                    title="Căn dữ liệu trong cột"
                  />
                  <b-button
                    size="sm"
                    variant="outline-danger"
                    :disabled="selected.columns.length === 1"
                    @click="removeColumn(index)"
                    >Xóa</b-button
                  >
                </div>
                <b-button
                  size="sm"
                  variant="outline-primary"
                  :disabled="!fieldsFor(selected.dataSource).length"
                  @click="addColumn"
                  >Thêm cột</b-button
                ><small class="d-block text-muted my-2">Tổng độ rộng các cột phải bằng 100%.</small>
              </template>
              <div class="row">
                <div class="col-4">
                  <b-form-group label="Cỡ chữ"
                    ><b-form-input
                      v-model.number="selected.style.fontSizePt"
                      type="number"
                      min="7"
                      max="36"
                  /></b-form-group>
                </div>
                <div class="col-4">
                  <b-form-group label="Căn chữ"
                    ><b-form-select
                      v-model="selected.style.textAlign"
                      :options="['left', 'center', 'right']"
                  /></b-form-group>
                </div>
                <div class="col-4">
                  <b-form-group label="Nét chữ"
                    ><b-form-select
                      v-model.number="selected.style.fontWeight"
                      :options="[
                        { value: 400, text: 'Thường' },
                        { value: 700, text: 'Đậm' }
                      ]"
                  /></b-form-group>
                </div>
              </div>
            </b-card>
            <div v-else class="inspector-empty">
              <span aria-hidden="true">↖</span>
              <strong>Chọn phần tử trên trang</strong>
              <p>Chỉnh nội dung, dữ liệu, màu sắc và khoảng cách tại đây.</p>
            </div>
            <b-card v-if="selected" title="Kích thước và khoảng cách" class="mb-3">
              <b-form-group label="Khoảng đệm (mm)">
                <b-form-input
                  v-model.number="selected.style.paddingMm"
                  type="number"
                  min="0"
                  max="10"
                />
              </b-form-group>
              <b-form-group v-if="selected.children" label="Khoảng cách phần tử (mm)">
                <b-form-input v-model.number="selected.gapMm" type="number" min="0" max="20" />
              </b-form-group>
              <template v-if="selected.type === 'image'">
                <b-form-group label="Rộng (mm)"
                  ><b-form-input v-model.number="selected.widthMm" type="number" min="1" max="270"
                /></b-form-group>
                <b-form-group label="Cao (mm)"
                  ><b-form-input v-model.number="selected.heightMm" type="number" min="1" max="150"
                /></b-form-group>
              </template>
              <b-form-group v-if="selected.type === 'spacer'" label="Chiều cao (mm)">
                <b-form-input v-model.number="selected.heightMm" type="number" min="1" max="150" />
              </b-form-group>
              <b-form-group label="Viền (mm)"
                ><b-form-input
                  v-model.number="selected.style.borderWidthMm"
                  type="number"
                  min="0"
                  max="2"
                  step="0.1"
              /></b-form-group>
              <label
                >Màu viền
                <input
                  type="color"
                  :value="selectedStyle('borderColor', '#666666')"
                  @input="setStyle('borderColor', $event.target.value)"
              /></label>
            </b-card>
            <b-card v-if="selected && selected.columns" title="Định dạng bảng" class="mb-3">
              <div v-for="part in ['headerStyle', 'cellStyle']" :key="part" class="mb-3">
                <strong class="d-block mb-2">{{
                  part === 'headerStyle' ? 'Hàng tiêu đề' : 'Dòng dữ liệu'
                }}</strong>
                <div class="d-flex justify-content-between mb-2">
                  <label
                    >Màu nền
                    <input
                      type="color"
                      :value="selected[part].backgroundColor || '#ffffff'"
                      @input="$set(selected[part], 'backgroundColor', $event.target.value)"
                  /></label>
                  <label
                    >Màu chữ
                    <input
                      type="color"
                      :value="selected[part].color || layout.theme.color"
                      @input="$set(selected[part], 'color', $event.target.value)"
                  /></label>
                </div>
                <b-form-group label="Cỡ chữ"
                  ><b-form-input
                    v-model.number="selected[part].fontSizePt"
                    type="number"
                    min="7"
                    max="36"
                /></b-form-group>
                <b-form-group label="Khoảng đệm ô (mm)"
                  ><b-form-input
                    v-model.number="selected[part].paddingMm"
                    type="number"
                    min="0"
                    max="10"
                    step="0.5"
                /></b-form-group>
              </div>
            </b-card>
          </fieldset>
        </aside>
      </div>
      <b-modal v-model="showPdf" title="Bản in PDF" size="xl" hide-footer>
        <b-alert v-if="dirty && jobId" show variant="warning"
          >Bản PDF thuộc bản nháp đã lưu trước đó. Đóng cửa sổ và chọn Bản in PDF để cập
          nhật.</b-alert
        >
        <pdf-artifact-viewer :job-id="jobId" />
      </b-modal>
    </template>
  </div>
</template>
<script>
import axios from '@/plugins/axios'
import LayoutBlockTree from '@/views/components/layout_block_tree.vue'
import PdfArtifactViewer from '@/views/components/pdf_artifact_viewer.vue'
import InvoiceLayoutCanvas from '@/views/components/invoice_layout_canvas.vue'
import { findBlock, blockId, copyBlock, placeBlock } from '@/utils/invoice_layout'

const requestKey = () =>
  window.crypto?.randomUUID
    ? window.crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`
export default {
  name: 'InvoiceTemplateDesigner',
  components: { LayoutBlockTree, PdfArtifactViewer, InvoiceLayoutCanvas },
  data: () => ({
    templates: [],
    selectedTemplateId: null,
    details: null,
    draft: null,
    layout: null,
    selected: null,
    fields: [],
    datasets: [],
    newName: '',
    newCategory: 1,
    newBlockType: 'field',
    busy: false,
    dirty: false,
    savedLayout: '',
    error: '',
    notice: '',
    jobId: null,
    page: 1,
    total: 0,
    generation: 0,
    showLibrary: false,
    showPdf: false,
    activeTab: 'home',
    zoom: 80,
    sampleRows: 3,
    history: [],
    historyIndex: -1,
    historyTimer: null,
    historyApplying: false,
    editorTabs: [
      { value: 'home', text: 'Trang đầu' },
      { value: 'insert', text: 'Chèn' },
      { value: 'page', text: 'Bố cục trang' },
      { value: 'versions', text: 'Phiên bản' }
    ],
    alignments: [
      { value: 'left', text: 'Căn trái' },
      { value: 'center', text: 'Căn giữa' },
      { value: 'right', text: 'Căn phải' }
    ],
    marginLabels: { top: 'Trên', right: 'Phải', bottom: 'Dưới', left: 'Trái' },
    formatters: [
      { value: 'plainText', text: 'Văn bản' },
      { value: 'moneyByCurrency', text: 'Tiền tệ' },
      { value: 'decimal', text: 'Số thập phân' },
      { value: 'date', text: 'Ngày tháng' },
      { value: 'invoiceNumber', text: 'Số hóa đơn' }
    ],
    blockTypes: [
      { value: 'field', text: 'Trường dữ liệu' },
      { value: 'text', text: 'Văn bản' },
      { value: 'stack', text: 'Nhóm dọc' },
      { value: 'columns', text: 'Nhóm cột' },
      { value: 'itemsTable', text: 'Bảng hàng hóa' },
      { value: 'table', text: 'Bảng dữ liệu' },
      { value: 'taxSummaryTable', text: 'Bảng thuế' },
      { value: 'feesTable', text: 'Bảng phí' },
      { value: 'totals', text: 'Nhóm tổng tiền' },
      { value: 'signatureSummary', text: 'Thông tin chữ ký' },
      { value: 'divider', text: 'Đường kẻ' },
      { value: 'spacer', text: 'Khoảng trống' },
      { value: 'pageBreak', text: 'Ngắt trang' }
    ]
  }),
  computed: {
    canUndo() {
      return this.historyIndex > 0 || this.pendingHistory
    },
    canRedo() {
      return this.historyIndex < this.history.length - 1 && !this.pendingHistory
    },
    pendingHistory() {
      return !!this.layout && JSON.stringify(this.layout) !== this.history[this.historyIndex]
    },
    isAdmin() {
      return this.$route.path.startsWith('/administrator/')
    },
    base() {
      return this.isAdmin ? '/administrator/invoice-templates' : '/invoice-templates'
    },
    formId() {
      return this.$route.query.formId ? Number(this.$route.query.formId) : null
    },
    templateOptions() {
      return [
        { value: null, text: 'Chọn mẫu' },
        ...this.templates.map((t) => ({ value: t.id, text: t.name }))
      ]
    },
    scalarFields() {
      return this.fields
        .filter((f) => !f.repeated)
        .filter((f) => f.active !== false || this.selected?.fieldKey === f.key)
        .map((f) => ({ value: f.key, text: f.label }))
    },
    repeatingDatasets() {
      return this.datasets
        .filter((d) => d.cardinality === 'MANY' && d.parent === 'document' && d.active)
        .map((d) => ({ value: d.code, text: d.label }))
    }
  },
  watch: {
    layout: {
      deep: true,
      handler() {
        if (this.layout && this.draft) {
          this.dirty = JSON.stringify(this.layout) !== this.savedLayout
          if (!this.historyApplying) {
            clearTimeout(this.historyTimer)
            this.historyTimer = setTimeout(this.recordHistory, 350)
          }
        }
      }
    },
    selected(value) {
      if (value && !value.style) this.$set(value, 'style', {})
    }
  },
  async created() {
    window.addEventListener('keydown', this.handleShortcut)
    try {
      const { data } = await axios.get(`${this.base}/catalog`)
      this.fields = data.fields
      this.datasets = data.datasets || []
      await this.fetchTemplates(1)
      if (this.formId) {
        const response = await axios.post(
          `${this.base}/from-form/${this.formId}`,
          {},
          { headers: { 'Idempotency-Key': requestKey() } }
        )
        this.selectedTemplateId = response.data.template.id
        await this.install(response.data)
      } else if (this.$route.query.templateId) {
        this.selectedTemplateId = Number(this.$route.query.templateId)
        await this.load()
      } else {
        this.showLibrary = true
        const category = Number(this.$route.query.category)
        if ([1, 2].includes(category)) this.newCategory = category
      }
    } catch (e) {
      this.fail(e)
    }
  },
  beforeDestroy() {
    this.generation++
    clearTimeout(this.historyTimer)
    window.removeEventListener('keydown', this.handleShortcut)
  },
  beforeRouteLeave(to, from, next) {
    next(!this.dirty || window.confirm('Bỏ các thay đổi chưa lưu?'))
  },
  methods: {
    blockLabel(block) {
      return (
        block.label ||
        block.text ||
        this.fields.find((field) => field.key === block.fieldKey)?.label ||
        this.blockTypes.find((type) => type.value === block.type)?.text ||
        block.type
      )
    },
    selectBlock(id) {
      this.selected = id ? findBlock(this.layout.body, id)?.block || null : null
    },
    selectedStyle(key, fallback) {
      return this.selected?.style?.[key] ?? fallback
    },
    setStyle(key, value) {
      if (!this.layout || this.busy) return
      const block = this.selected || this.layout.body
      this.selected = block
      if (!block.style) this.$set(block, 'style', {})
      this.$set(block.style, key, value)
    },
    resetHistory() {
      clearTimeout(this.historyTimer)
      this.history = this.layout ? [JSON.stringify(this.layout)] : []
      this.historyIndex = this.history.length - 1
    },
    recordHistory() {
      if (!this.layout || this.historyApplying) return
      const snapshot = JSON.stringify(this.layout)
      if (snapshot === this.history[this.historyIndex]) return
      this.history.splice(this.historyIndex + 1)
      this.history.push(snapshot)
      if (this.history.length > 80) this.history.shift()
      this.historyIndex = this.history.length - 1
    },
    applyHistory(index) {
      clearTimeout(this.historyTimer)
      const selectedId = this.selected?.id
      this.historyApplying = true
      this.historyIndex = index
      this.layout = JSON.parse(this.history[index])
      this.selectBlock(selectedId)
      this.$nextTick(() => {
        this.historyApplying = false
      })
    },
    undo() {
      if (this.busy) return
      this.recordHistory()
      if (this.historyIndex > 0) this.applyHistory(this.historyIndex - 1)
    },
    redo() {
      if (!this.busy && this.canRedo) this.applyHistory(this.historyIndex + 1)
    },
    handleShortcut(event) {
      if (this.busy || this.showPdf || !this.layout) return
      const input = event.target?.closest?.('input, textarea, select, [contenteditable=true]')
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
        event.preventDefault()
        this.save()
      } else if (!input && (event.ctrlKey || event.metaKey)) {
        const key = event.key.toLowerCase()
        if (key === 'z' || key === 'y') {
          event.preventDefault()
          if (key === 'y' || event.shiftKey) this.redo()
          else this.undo()
        } else if (key === 'd') {
          event.preventDefault()
          this.duplicateSelected()
        }
      } else if (!input && (event.key === 'Delete' || event.key === 'Backspace')) {
        event.preventDefault()
        this.deleteSelected()
      }
    },
    changeText({ id, text }) {
      if (this.busy || text.length > 5000) return
      const found = findBlock(this.layout.body, id)
      if (found?.block.type === 'text') found.block.text = text
    },
    moveBlock({ id, targetId, placement }) {
      if (this.busy) return
      const found = findBlock(this.layout.body, id)
      if (!found) return
      this.recordHistory()
      if (placeBlock(this.layout.body, found.block, targetId, placement, true)) {
        this.selected = found.block
        this.recordHistory()
      } else this.error = 'Không thể đặt phần tử ở vị trí này. Một nhóm cột chứa tối đa 6 phần tử.'
    },
    dragPalette(event, type) {
      if (this.busy) {
        event.preventDefault()
        return
      }
      event.dataTransfer.setData('application/x-invoice-block-type', type)
      event.dataTransfer.effectAllowed = 'copy'
    },
    insertBlock({ type, targetId, placement }) {
      this.addBlock(type, { targetId, placement })
    },
    duplicateSelected() {
      if (!this.selected || this.busy) return
      const found = findBlock(this.layout.body, this.selected.id)
      if (!found?.parent) return
      if (found.parent.type === 'columns' && found.parent.children.length >= 6) {
        this.error = 'Một nhóm cột chứa tối đa 6 phần tử'
        return
      }
      this.recordHistory()
      const copy = copyBlock(found.block)
      found.parent.children.splice(found.parent.children.indexOf(found.block) + 1, 0, copy)
      this.selected = copy
      this.recordHistory()
    },
    deleteSelected() {
      if (!this.selected || this.busy) return
      const found = findBlock(this.layout.body, this.selected.id)
      if (!found?.parent) return
      this.recordHistory()
      found.parent.children.splice(found.parent.children.indexOf(found.block), 1)
      this.selected = null
      this.recordHistory()
    },
    fail(e) {
      this.error = e.response?.data?.message || e.message || 'Không thực hiện được thao tác'
    },
    fieldsFor(group, currentKey = null) {
      return this.fields
        .filter((f) => (f.rowScope || f.dataset || f.group) === group)
        .filter((f) => f.active !== false || f.key === currentKey)
        .map((f) => ({ value: f.key, text: f.label }))
    },
    async upgradeCatalog() {
      this.busy = true
      this.error = ''
      try {
        if (this.dirty) await this.saveDraft()
        await axios.post(`${this.base}/${this.selectedTemplateId}/upgrade-catalog`, {
          expectedLockVersion: this.draft.lockVersion
        })
        const { data } = await axios.get(`${this.base}/${this.selectedTemplateId}`)
        await this.install(data)
        this.notice =
          'Đã cập nhật danh mục cho bản nháp. Kiểm tra và công bố lại mẫu để áp dụng cho hóa đơn mới.'
      } catch (e) {
        this.fail(e)
      } finally {
        this.busy = false
      }
    },
    changeDataset() {
      if (!this.selected?.columns) return
      const first = this.fieldsFor(this.selected.dataSource)[0]
      this.selected.columns = first
        ? [
            {
              id: blockId(),
              label: first.text,
              fieldKey: first.value,
              widthPercent: 100,
              formatter: 'plainText',
              style: {}
            }
          ]
        : []
    },
    async fetchTemplates(page = 1) {
      this.page = page
      const { data } = await axios.get(this.base, {
        params: { page, size: 50 }
      })
      this.templates = data.content
      this.total = data.totalElements
    },
    async create() {
      if (this.dirty && !window.confirm('Bỏ các thay đổi chưa lưu để tạo mẫu mới?')) return
      this.busy = true
      this.error = ''
      try {
        const { data } = await axios.post(
          this.base,
          { name: this.newName, category: this.newCategory },
          { headers: { 'Idempotency-Key': requestKey() } }
        )
        await this.fetchTemplates()
        this.selectedTemplateId = data.template.id
        await this.install(data)
        this.newName = ''
      } catch (e) {
        this.fail(e)
      } finally {
        this.busy = false
      }
    },
    async install(data) {
      this.details = data
      this.draft = data.draft
      if (data.draft) {
        try {
          this.layout = JSON.parse(data.draft.sourceContent)
        } catch (e) {
          this.layout = null
          throw new Error(
            'Dữ liệu bố cục của mẫu không hợp lệ. Vui lòng tạo lại mẫu trong trình thiết kế.'
          )
        }
      } else {
        this.layout = null
      }
      const normalize = (block) => {
        if (!block.style) this.$set(block, 'style', {})
        if (block.columns) {
          if (!block.headerStyle) this.$set(block, 'headerStyle', {})
          if (!block.cellStyle) this.$set(block, 'cellStyle', {})
          block.columns.forEach((column) => {
            if (!column.style) this.$set(column, 'style', {})
          })
        }
        ;(block.children || []).forEach(normalize)
      }
      if (this.layout) {
        const response = await axios.get(`${this.base}/catalog`, {
          params: { version: this.layout.fieldCatalogVersion }
        })
        this.fields = response.data.fields
        this.datasets = response.data.datasets || []
        normalize(this.layout.body)
      }
      this.savedLayout = this.layout ? JSON.stringify(this.layout) : ''
      this.selected = null
      this.dirty = false
      this.jobId = null
      this.resetHistory()
      this.showLibrary = false
    },
    async load() {
      if (!this.selectedTemplateId) return
      if (this.dirty && !window.confirm('Bỏ thay đổi chưa lưu để mở mẫu khác?')) {
        this.selectedTemplateId = this.details.template.id
        return
      }
      this.error = ''
      this.notice = ''
      this.busy = true
      try {
        const { data } = await axios.get(`${this.base}/${this.selectedTemplateId}`)
        await this.install(data)
      } catch (e) {
        this.fail(e)
      } finally {
        this.busy = false
      }
    },
    async saveDraft() {
      if (!this.layout) return
      const snapshot = JSON.parse(JSON.stringify(this.layout))
      const { data } = await axios.put(`${this.base}/${this.selectedTemplateId}/draft`, {
        expectedLockVersion: this.draft.lockVersion,
        layout: snapshot
      })
      this.draft = data
      this.savedLayout = JSON.stringify(JSON.parse(data.sourceContent))
      this.dirty = JSON.stringify(this.layout) !== this.savedLayout
    },
    async applyDefaultLayout() {
      if (
        this.busy ||
        !window.confirm(
          'Thay bố cục bản nháp bằng mẫu thiết kế mới? Bạn có thể hoàn tác; các bản đã công bố vẫn được giữ nguyên.'
        )
      )
        return
      this.busy = true
      try {
        const { data } = await axios.get(`${this.base}/default-layout`)
        data.fieldCatalogVersion = this.draft.fieldCatalogVersion
        this.recordHistory()
        this.layout = data
        this.selected = null
        this.recordHistory()
      } catch (e) {
        this.fail(e)
      } finally {
        this.busy = false
      }
    },
    async save() {
      this.busy = true
      this.error = ''
      try {
        await this.saveDraft()
        this.notice = 'Đã lưu bản nháp'
      } catch (e) {
        this.fail(e)
      } finally {
        this.busy = false
      }
    },
    async preview() {
      this.busy = true
      this.error = ''
      try {
        if (this.dirty) await this.saveDraft()
        const { data } = await axios.post(`${this.base}/${this.selectedTemplateId}/preview`, {
          expectedLockVersion: this.draft.lockVersion
        })
        this.jobId = data.jobId || data.id
        this.showPdf = true
      } catch (e) {
        this.fail(e)
      } finally {
        this.busy = false
      }
    },
    async publish() {
      this.busy = true
      this.error = ''
      this.notice = 'Đang kiểm tra bản in với 0, 1 và 60 dòng hàng hóa…'
      const generation = ++this.generation
      try {
        if (this.dirty) await this.saveDraft()
        const revision = this.draft.lockVersion
        const { data } = await axios.post(`${this.base}/${this.selectedTemplateId}/validate`, {
          expectedLockVersion: revision
        })
        const deadline = Date.now() + 180000
        let run = data
        while (run.state === 'RUNNING' && Date.now() < deadline) {
          await new Promise((resolve) => setTimeout(resolve, 1000))
          if (generation !== this.generation) return
          run = (await axios.get(`${this.base}/validation-runs/${data.id}`)).data
        }
        if (run.state !== 'PASSED') {
          const failed = JSON.parse(run.reportJson || '{}').fixtures?.find((f) => f.errorCode)
          if (failed) {
            this.jobId = failed.jobId
            this.showPdf = true
          }
          throw new Error(
            'Bộ kiểm tra bản in chưa đạt. Xem lỗi ở phần bản in để chỉnh sửa hoặc thử lại.'
          )
        }
        await axios.post(
          `${this.base}/${this.selectedTemplateId}/publish`,
          { expectedLockVersion: revision, validationReportId: data.id },
          { headers: { 'Idempotency-Key': requestKey() } }
        )
        this.dirty = false
        await this.load()
        this.notice = 'Đã công bố phiên bản mới. Hóa đơn cũ giữ nguyên mẫu đã chốt.'
      } catch (e) {
        this.fail(e)
        this.notice = ''
      } finally {
        this.busy = false
      }
    },
    async clone() {
      const source = this.details.versions[0]
      if (!source) {
        this.error = 'Cần công bố một phiên bản trước khi nhân bản'
        return
      }
      this.busy = true
      try {
        const { data } = await axios.post(
          this.base,
          {
            name: `${this.details.template.name} - Bản sao`,
            category: this.details.template.category,
            sourceVersionId: source.id
          },
          { headers: { 'Idempotency-Key': requestKey() } }
        )
        await this.fetchTemplates()
        this.selectedTemplateId = data.template.id
        await this.install(data)
      } catch (e) {
        this.fail(e)
      } finally {
        this.busy = false
      }
    },
    async restore(version) {
      if (this.dirty && !window.confirm('Bỏ thay đổi chưa lưu?')) return
      this.busy = true
      try {
        await axios.post(`${this.base}/${this.selectedTemplateId}/draft-from-version`, {
          versionId: version.id,
          expectedLockVersion: this.draft.lockVersion
        })
        this.dirty = false
        await this.load()
      } catch (e) {
        this.fail(e)
      } finally {
        this.busy = false
      }
    },
    async archive() {
      if (!window.confirm('Ngừng chọn mẫu này cho hóa đơn mới? Hóa đơn cũ vẫn xem được.')) return
      this.busy = true
      try {
        await axios.post(`${this.base}/${this.selectedTemplateId}/archive`, {
          expectedLockVersion: this.details.template.lockVersion
        })
        this.details = null
        this.draft = null
        this.layout = null
        this.selectedTemplateId = null
        await this.fetchTemplates()
      } catch (e) {
        this.fail(e)
      } finally {
        this.busy = false
      }
    },
    async bind(version) {
      this.busy = true
      try {
        await axios.post(`${this.base}/versions/${version.id}/bind/${this.formId}`, {})
        this.notice = 'Đã chọn phiên bản cho hóa đơn mới'
      } catch (e) {
        this.fail(e)
      } finally {
        this.busy = false
      }
    },
    async addBlock(type = this.newBlockType, position = null) {
      if (this.busy || !this.layout || !this.blockTypes.some((item) => item.value === type)) return
      const templateId = this.selectedTemplateId
      const block = {
        id: blockId(),
        type,
        style: { fontSizePt: 10, fontWeight: 400, textAlign: 'left' }
      }
      if (block.type === 'field')
        Object.assign(block, {
          fieldKey: 'seller.Ten',
          label: '',
          formatter: 'plainText'
        })
      if (block.type === 'text') block.text = 'Nội dung'
      if (['stack', 'columns', 'totals'].includes(block.type)) {
        block.children = []
        block.gapMm = 2
      }
      if (block.type === 'spacer') block.heightMm = 5
      if (['table'].includes(block.type)) {
        const source = this.repeatingDatasets[0]
        if (!source) {
          this.error = 'Danh mục chưa có danh sách lặp'
          return
        }
        block.dataSource = source.value
        if (block.type === 'table') {
          const field = this.fieldsFor(source.value)[0]
          block.columns = field
            ? [
                {
                  id: blockId(),
                  label: field.text,
                  fieldKey: field.value,
                  widthPercent: 100,
                  formatter: 'plainText',
                  style: {}
                }
              ]
            : []
          block.repeatHeader = true
          block.rowSplitPolicy = 'splitIfOversized'
        }
      }
      if (block.type.endsWith('Table')) {
        block.dataSource = {
          itemsTable: 'items',
          taxSummaryTable: 'taxGroups',
          feesTable: 'fees'
        }[block.type]
        block.repeatHeader = true
        block.rowSplitPolicy = 'splitIfOversized'
        if (block.type === 'feesTable') {
          const fields = this.fieldsFor('fees')
          block.columns = fields.map((f, i) => ({
            id: `fee${i}`,
            fieldKey: f.value,
            label: f.text,
            widthPercent:
              i === fields.length - 1
                ? 100 - Math.floor(100 / fields.length) * i
                : Math.floor(100 / fields.length),
            formatter: 'decimal'
          }))
        } else {
          try {
            const { data } = await axios.get(`${this.base}/default-layout`)
            const table = data.body.children.find((b) => b.type === block.type)
            block.columns = table.columns
            block.headerStyle = table.headerStyle || {}
            block.cellStyle = table.cellStyle || {}
          } catch (e) {
            this.fail(e)
            return
          }
        }
      }
      if (block.columns) {
        block.headerStyle = block.headerStyle || {}
        block.cellStyle = block.cellStyle || {}
        block.columns.forEach((column) => {
          if (!column.style) column.style = {}
        })
      }
      if (templateId !== this.selectedTemplateId || this.busy || !this.layout) return
      this.recordHistory()
      let placed
      if (position)
        placed = placeBlock(this.layout.body, block, position.targetId, position.placement)
      else if (this.selected?.children)
        placed = placeBlock(this.layout.body, block, this.selected.id, 'inside')
      else if (this.selected)
        placed = placeBlock(this.layout.body, block, this.selected.id, 'after')
      else placed = placeBlock(this.layout.body, block, this.layout.body.id, 'inside')
      if (!placed) {
        this.error = 'Không thể chèn vào vị trí này. Kiểm tra số phần tử trong nhóm.'
        return
      }
      this.selected = block
      this.recordHistory()
    },
    addColumn() {
      const field = this.fieldsFor(this.selected.dataSource)[0]
      if (!field || this.selected.columns.length >= 15) return
      this.selected.columns.push({
        id: `c${Date.now()}`,
        label: field.text,
        fieldKey: field.value,
        widthPercent: 10,
        formatter: 'plainText',
        style: {}
      })
      this.balanceColumns()
    },
    removeColumn(index) {
      this.selected.columns.splice(index, 1)
      this.balanceColumns()
    },
    balanceColumns() {
      const columns = this.selected.columns
      const width = Math.floor(100 / columns.length)
      columns.forEach((column, index) => {
        column.widthPercent = index === columns.length - 1 ? 100 - width * index : width
      })
    },
    async uploadImage(event) {
      const file = event.target.files[0]
      if (!file) return
      const templateId = this.selectedTemplateId
      try {
        const body = new FormData()
        body.append('file', file)
        body.append('kind', 'IMAGE')
        const { data } = await axios.post(`${this.base}/assets`, body)
        if (templateId !== this.selectedTemplateId || !this.layout) return
        const name = `image${data.id}`
        this.$set(this.layout.assets, name, { assetId: data.id })
        this.layout.body.children.unshift({
          id: name,
          type: 'image',
          asset: name,
          widthMm: 35,
          heightMm: 20
        })
        this.selected = this.layout.body.children[0]
      } catch (e) {
        this.fail(e)
      } finally {
        event.target.value = ''
      }
    }
  }
}
</script>

<style scoped>
.template-designer {
  color: #1e293b;
}
.designer-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.designer-heading .text-muted {
  font-size: 13px;
}
.document-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: #fff;
  border: 1px solid #dce2eb;
  border-radius: 8px 8px 0 0;
}
.designer-ribbon {
  background: #fff;
  border: 1px solid #dce2eb;
  border-top: 0;
}
.ribbon-tabs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  border-bottom: 1px solid #e2e8f0;
  padding: 0 12px;
}
.ribbon-tabs > button {
  padding: 11px 14px;
  color: #475569;
  background: none;
  border: 0;
  border-bottom: 3px solid transparent;
}
.ribbon-tabs > button.active {
  color: #1d4ed8;
  border-bottom-color: #2563eb;
  font-weight: 600;
}
.zoom-control {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  font-size: 12px;
}
.zoom-control input {
  width: 90px;
}
.zoom-control span {
  min-width: 34px;
}
.ribbon-content {
  border: 0;
  margin: 0;
  padding: 0;
}
.format-toolbar,
.insert-toolbar,
.page-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 67px;
  padding: 12px;
}
.format-toolbar button,
.insert-toolbar button {
  background: #fff;
  border: 1px solid #dce2eb;
  padding: 6px 10px;
  border-radius: 4px;
  color: #334155;
  min-height: 33px;
}
.format-toolbar button:hover,
.insert-toolbar button:hover {
  background: #f1f5f9;
}
.format-toolbar button.active {
  color: #1d4ed8;
  background: #dbeafe;
  border-color: #93c5fd;
}
.format-toolbar button:disabled {
  opacity: 0.4;
  cursor: default;
}
.toolbar-group {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-right: 10px;
  border-right: 1px solid #e2e8f0;
}
.alignment-icon {
  display: block;
  width: 16px;
}
.alignment-icon i {
  display: block;
  height: 2px;
  margin: 3px 0;
  background: currentColor;
}
.alignment-icon i:nth-child(2) {
  width: 10px;
}
.alignment-icon.center i:nth-child(2) {
  margin-left: auto;
  margin-right: auto;
}
.alignment-icon.right i:nth-child(2) {
  margin-left: auto;
}
.toolbar-group select,
.font-size-input {
  border: 1px solid #dce2eb;
  border-radius: 4px;
  padding: 6px;
  height: 33px;
}
.font-size-input {
  width: 58px;
}
.color-tool {
  position: relative;
  margin: 0;
  padding: 4px 7px;
  font-size: 13px;
  cursor: pointer;
}
.color-tool input {
  display: block;
  height: 8px;
  border: 0;
  padding: 0;
  width: 25px;
  cursor: pointer;
}
.page-toolbar label {
  margin: 0;
  width: 100px;
  font-size: 12px;
  color: #64748b;
}
.page-toolbar label:first-child,
.page-toolbar label:nth-child(2) {
  width: 115px;
}
.versions-panel {
  padding: 16px;
}
.version-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
}
.designer-workspace {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr) 285px;
  border: 1px solid #dce2eb;
  border-top: 0;
  border-radius: 0 0 8px 8px;
  overflow: hidden;
}
.designer-sidebar,
.designer-inspector {
  background: #fff;
  padding: 16px 12px;
  min-width: 0;
  overflow: auto;
  max-height: calc(100vh - 270px);
}
.designer-sidebar {
  border-right: 1px solid #e2e8f0;
}
.designer-inspector {
  border-left: 1px solid #e2e8f0;
}
.designer-document {
  min-width: 0;
}
.sidebar-help {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}
.block-palette {
  display: grid;
  grid-template-columns: 1fr;
  gap: 5px;
}
.block-palette button {
  text-align: left;
  padding: 8px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  color: #475569;
  font-size: 13px;
  cursor: grab;
}
.block-palette button:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}
.block-palette button span {
  color: #2563eb;
  margin-right: 8px;
}
.sample-rows {
  font-size: 12px;
  color: #64748b;
  margin-top: 18px;
  display: block;
}
.layout-outline {
  margin-top: 18px;
  font-size: 13px;
}
.layout-outline summary {
  cursor: pointer;
  padding: 8px 0;
  font-weight: 600;
}
.designer-inspector .card {
  border: 0;
}
.designer-inspector ::v-deep .card-body {
  padding: 4px;
}
.designer-inspector ::v-deep .card-title {
  font-size: 15px;
  margin-bottom: 18px;
}
.designer-inspector ::v-deep label {
  font-size: 12px;
  color: #64748b;
}
.designer-inspector ::v-deep .form-control,
.designer-inspector ::v-deep .custom-select {
  font-size: 13px;
}
.designer-inspector .row {
  margin: 0 -4px;
}
.designer-inspector .row > div {
  padding: 0 4px;
}
.inspector-empty {
  text-align: center;
  padding: 60px 8px;
  color: #64748b;
  font-size: 13px;
}
.inspector-empty span {
  display: block;
  font-size: 32px;
  margin-bottom: 16px;
  color: #94a3b8;
}
.inspector-empty strong {
  display: block;
  margin-bottom: 8px;
  color: #334155;
}
@media (max-width: 1280px) {
  .designer-workspace {
    grid-template-columns: 175px minmax(0, 1fr) 245px;
  }
}
@media (max-width: 960px) {
  .designer-workspace {
    grid-template-columns: minmax(0, 1fr) 245px;
  }
  .designer-sidebar {
    grid-column: 1 / -1;
    max-height: 190px;
    border-bottom: 1px solid #e2e8f0;
  }
  .block-palette {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  .designer-sidebar .sample-rows,
  .designer-sidebar .layout-outline,
  .designer-sidebar > p {
    display: none;
  }
  .designer-heading,
  .document-header {
    flex-wrap: wrap;
  }
}
@media (max-width: 640px) {
  .designer-workspace {
    grid-template-columns: minmax(0, 1fr);
  }
  .designer-inspector {
    max-height: none;
    border-top: 1px solid #e2e8f0;
  }
  .block-palette {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .zoom-control {
    margin: 8px 0;
  }
}
.action-bar {
  gap: 8px;
}
.column-settings {
  display: grid;
  grid-template-columns: 1fr 70px;
  gap: 5px;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e8f0;
}
.column-settings > select {
  grid-column: 1 / -1;
}
.template-designer label input[type='color'] {
  width: 45px;
  vertical-align: middle;
}
</style>
