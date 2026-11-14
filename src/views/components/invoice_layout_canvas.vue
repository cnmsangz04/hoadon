<template>
  <div class="invoice-layout-canvas">
    <div class="canvas-status" role="status" aria-live="polite">
      <span v-if="loading">Đang cập nhật trang…</span>
      <span v-else-if="error" class="text-danger">{{ error }}</span>
      <span v-else>Chọn để định dạng · Nhấp đúp để sửa văn bản · Kéo để đổi vị trí</span>
      <button v-if="error" type="button" class="btn btn-sm btn-link" @click="schedule">
        Thử lại
      </button>
    </div>
    <div ref="viewport" class="canvas-viewport">
      <div class="canvas-sheet-space" :style="spaceStyle">
        <div class="canvas-sheet" :style="sheetStyle">
          <div class="canvas-ruler" aria-hidden="true" />
          <iframe
            ref="frame"
            :srcdoc="documentHtml"
            :style="{ width: pageWidth + 'px', height: documentHeight + 'px' }"
            title="Trang thiết kế mẫu hóa đơn"
            sandbox="allow-same-origin"
            @load="attachDocument"
          />
          <div
            v-show="!editingId"
            ref="interactionLayer"
            class="canvas-interaction-layer"
            tabindex="0"
            role="application"
            aria-label="Chỉnh sửa bố cục hóa đơn"
            @click="selectAt"
            @dblclick="editAt"
            @pointerdown="startPointer"
            @pointermove="movePointer"
            @pointerup="finishPointer"
            @pointercancel="cancelPointer"
            @dragover.prevent="paletteOver"
            @dragleave="clearMarkers"
            @drop.prevent="paletteDrop"
          />
          <button
            v-if="selectionBox && selectedId !== layout.body.id && !editingId"
            type="button"
            class="canvas-selection-handle"
            :style="selectionBox"
            :disabled="disabled"
            title="Kéo phần tử đang chọn"
            aria-label="Kéo phần tử đang chọn"
            @pointerdown.stop.prevent="startSelectedPointer"
            @click.stop
          >
            ⋮⋮
          </button>
        </div>
      </div>
    </div>
    <div class="canvas-footnote">
      Trang soạn thảo dùng dữ liệu minh họa. Xem Bản in PDF để kiểm tra ngắt trang.
    </div>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import { findBlock } from '@/utils/invoice_layout'

export default {
  name: 'InvoiceLayoutCanvas',
  props: {
    layout: { type: Object, required: true },
    templateId: { type: Number, required: true },
    base: { type: String, required: true },
    selectedId: { type: String, default: '' },
    zoom: { type: Number, default: 80 },
    rows: { type: Number, default: 3 },
    disabled: { type: Boolean, default: false }
  },
  data: () => ({
    documentHtml: '',
    fontCss: '',
    documentHeight: 1123,
    loading: false,
    error: '',
    timer: null,
    generation: 0,
    editingId: null,
    draggingId: null,
    pointerDrag: null,
    suppressClick: false,
    selectionBox: null,
    observer: null,
    destroyed: false
  }),
  computed: {
    pageWidth() {
      const short = this.layout.page.format === 'A5' ? 148 : 210
      const long = this.layout.page.format === 'A5' ? 210 : 297
      return ((this.layout.page.orientation === 'landscape' ? long : short) * 96) / 25.4
    },
    pageHeight() {
      const short = this.layout.page.format === 'A5' ? 148 : 210
      const long = this.layout.page.format === 'A5' ? 210 : 297
      return ((this.layout.page.orientation === 'landscape' ? short : long) * 96) / 25.4
    },
    spaceStyle() {
      return {
        width: (this.pageWidth * this.zoom) / 100 + 'px',
        height: ((this.documentHeight + 22) * this.zoom) / 100 + 'px'
      }
    },
    sheetStyle() {
      return { transform: `scale(${this.zoom / 100})`, width: this.pageWidth + 'px' }
    }
  },
  watch: {
    layout: {
      deep: true,
      handler() {
        this.schedule()
      }
    },
    templateId() {
      this.editingId = null
      this.schedule()
    },
    rows() {
      this.schedule()
    },
    selectedId() {
      this.markSelection()
    },
    disabled() {
      this.markSelection()
    }
  },
  created() {
    this.schedule()
  },
  beforeDestroy() {
    this.destroyed = true
    this.generation++
    clearTimeout(this.timer)
    this.observer?.disconnect()
  },
  methods: {
    schedule() {
      clearTimeout(this.timer)
      const generation = ++this.generation
      if (this.editingId) return
      this.timer = setTimeout(() => this.refresh(generation), 250)
    },
    async refresh(generation) {
      this.loading = true
      this.error = ''
      try {
        if (!this.fontCss) {
          const { data } = await axios.get(`${this.base}/designer-fonts`, {
            meta: { suppressGlobalErrorToast: true }
          })
          if (generation !== this.generation || this.destroyed) return
          this.fontCss = data.css
        }
        const { data } = await axios.post(
          `${this.base}/${this.templateId}/designer-preview`,
          {
            layout: this.layout,
            rows: this.rows
          },
          { meta: { suppressGlobalErrorToast: true } }
        )
        if (generation !== this.generation || this.destroyed) return
        this.documentHtml = this.decorate(data.html)
      } catch (e) {
        if (generation !== this.generation || this.destroyed) return
        this.error = e.response?.data?.message || e.message || 'Không cập nhật được trang thiết kế'
      } finally {
        if (generation === this.generation && !this.destroyed) this.loading = false
      }
    },
    decorate(html) {
      const margins = this.layout.page.marginMm
      const css = `
        ${this.fontCss}
        html{background:#fff}body{width:${this.pageWidth}px;min-height:${this.pageHeight}px;padding:${margins.top}mm ${margins.right}mm ${margins.bottom}mm ${margins.left}mm!important}
        [data-block-id]{position:relative;cursor:grab;outline-offset:2px;user-select:none;touch-action:none}
        [data-editor-dragging]{opacity:.45;cursor:grabbing}
        [data-block-id]:hover{outline:1px dashed #94a3b8}
        [data-editor-selected]{outline:2px solid #2563eb!important}
        [data-editor-drop=before]{box-shadow:0 -3px #2563eb}
        [data-editor-drop=after]{box-shadow:0 3px #2563eb}
        [data-editor-drop=left]{box-shadow:-3px 0 #2563eb}
        [data-editor-drop=right]{box-shadow:3px 0 #2563eb}
        [data-editor-drop=inside]{background-color:#dbeafe!important;outline:2px dashed #2563eb}
        [data-block-id][contenteditable=true]{cursor:text;min-height:1.3em;user-select:text}
        .stack:empty,.columns:empty,.totals:empty,.relatedInvoice:empty{min-height:12mm;border:1px dashed #cbd5e1}
        .stack:empty:after,.columns:empty:after,.totals:empty:after,.relatedInvoice:empty:after{content:'Thả phần tử vào đây';color:#94a3b8;font:10pt sans-serif}
        .pageBreak{outline:1px dashed #94a3b8}.pageBreak:after{content:'Ngắt trang';position:absolute;top:0;left:0;font:8pt sans-serif;color:#64748b;pointer-events:none}
      `
      const policy = `<meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; font-src data:; style-src 'unsafe-inline';">`
      return html
        .replace('<head>', '<head>' + policy)
        .replace('</head>', `<style>${css}</style></head>`)
    },
    attachDocument() {
      const doc = this.$refs.frame?.contentDocument
      if (!doc?.body || !this.documentHtml) return
      this.observer?.disconnect()
      doc.querySelectorAll('[data-block-id]').forEach((element) => {
        element.draggable = false
      })
      doc.addEventListener('click', (event) => {
        if (!this.disabled)
          this.$emit('select', event.target.closest('[data-block-id]')?.dataset.blockId || null)
      })
      doc.addEventListener('keydown', (event) => {
        if (this.disabled) return
        if (event.target.isContentEditable) {
          if (!(event.ctrlKey || event.metaKey) || event.key.toLowerCase() !== 's') return
          event.target.blur()
        }
        this.$emit('shortcut', event)
      })
      const measure = () => {
        if (!this.destroyed && this.$refs.frame?.contentDocument === doc)
          this.documentHeight = Math.max(
            this.pageHeight,
            Math.ceil(doc.body.getBoundingClientRect().height)
          )
      }
      this.observer = new ResizeObserver(measure)
      this.observer.observe(doc.body)
      doc.fonts.ready.then(measure)
      measure()
      this.markSelection()
    },
    point(event) {
      const frame = this.$refs.frame
      const doc = frame?.contentDocument
      if (!doc) return null
      const rect = frame.getBoundingClientRect()
      const scale = this.zoom / 100
      const clientX = (event.clientX - rect.left) / scale
      const clientY = (event.clientY - rect.top) / scale
      return { target: doc.elementFromPoint(clientX, clientY), clientX, clientY }
    },
    selectAt(event) {
      if (this.disabled || this.suppressClick) return
      const point = this.point(event)
      const element = point?.target?.closest('[data-block-id]')
      this.$emit('select', element?.dataset.blockId || null)
      this.$refs.interactionLayer.focus({ preventScroll: true })
    },
    editAt(event) {
      const point = this.point(event)
      if (point?.target) this.beginEditing(point)
    },
    startPointer(event) {
      if (this.disabled || event.button !== 0) return
      const element = this.point(event)?.target?.closest('[data-block-id]')
      this.beginPointer(event, element)
    },
    startSelectedPointer(event) {
      if (this.disabled || event.button !== 0) return
      const element = this.$refs.frame?.contentDocument?.querySelector(
        `[data-block-id="${this.selectedId}"]`
      )
      this.beginPointer(event, element)
    },
    beginPointer(event, element) {
      if (!element || element.dataset.blockId === this.layout.body.id) return
      this.pointerDrag = {
        id: element.dataset.blockId,
        element,
        pointerId: event.pointerId,
        x: event.clientX,
        y: event.clientY
      }
    },
    movePointer(event) {
      const drag = this.pointerDrag
      if (!drag || drag.pointerId !== event.pointerId || this.disabled) return
      if (!this.draggingId && Math.hypot(event.clientX - drag.x, event.clientY - drag.y) < 5) return
      if (!this.draggingId) {
        this.draggingId = drag.id
        clearTimeout(this.timer)
        this.generation++
        this.loading = false
        this.$refs.interactionLayer.setPointerCapture(event.pointerId)
        drag.element.setAttribute('data-editor-dragging', '')
      }
      event.preventDefault()
      this.clearMarkers()
      const point = this.point(event)
      const target = point && this.dropTarget(point)
      if (target) target.element.dataset.editorDrop = target.placement
    },
    finishPointer(event) {
      const drag = this.pointerDrag
      if (!drag || drag.pointerId !== event.pointerId) return
      const id = this.draggingId
      const point = this.point(event)
      const target =
        id && point && !this.disabled && event.type !== 'pointercancel'
          ? this.dropTarget(point)
          : null
      this.cancelPointer(event)
      if (id) {
        this.suppressClick = true
        setTimeout(() => {
          this.suppressClick = false
        }, 0)
        if (target) this.$emit('move', { id, targetId: target.id, placement: target.placement })
        this.schedule()
      }
    },
    cancelPointer(event) {
      this.pointerDrag?.element.removeAttribute('data-editor-dragging')
      if (this.$refs.interactionLayer.hasPointerCapture(event.pointerId))
        this.$refs.interactionLayer.releasePointerCapture(event.pointerId)
      this.pointerDrag = null
      this.draggingId = null
      this.clearMarkers()
    },
    clearMarkers() {
      const doc = this.$refs.frame?.contentDocument
      if (doc) this.clearDrop(doc)
    },
    paletteOver(event) {
      if (this.disabled || !this.acceptsDrop(event)) return
      this.clearMarkers()
      const point = this.point(event)
      const target = point && this.dropTarget(point)
      if (target) target.element.dataset.editorDrop = target.placement
      event.dataTransfer.dropEffect = 'copy'
    },
    paletteDrop(event) {
      this.clearMarkers()
      if (this.disabled || !this.acceptsDrop(event)) return
      const point = this.point(event)
      const target = point && this.dropTarget(point)
      const type = event.dataTransfer.getData('application/x-invoice-block-type')
      if (target && type)
        this.$emit('insert', { type, targetId: target.id, placement: target.placement })
    },
    markSelection() {
      const doc = this.$refs.frame?.contentDocument
      if (!doc) return
      doc.querySelectorAll('[data-block-id]').forEach((element) => {
        element.toggleAttribute('data-editor-selected', element.dataset.blockId === this.selectedId)
        element.draggable = false
      })
      const element = doc.querySelector(`[data-block-id="${this.selectedId}"]`)
      const rect = element?.getBoundingClientRect()
      this.selectionBox = rect
        ? { left: Math.max(0, rect.left - 19) + 'px', top: Math.max(22, rect.top + 22) + 'px' }
        : null
    },
    beginEditing(event) {
      if (this.disabled) return
      const element = event.target.closest('[data-block-id]')
      const found = findBlock(this.layout.body, element?.dataset.blockId)
      if (!found || found.block.type !== 'text') return
      this.editingId = found.block.id
      element.draggable = false
      element.contentEditable = 'true'
      element.textContent = found.block.text
      element.focus()
      const onInput = () => this.$emit('text', { id: found.block.id, text: element.innerText })
      element.addEventListener('input', onInput)
      element.addEventListener('paste', (paste) => {
        paste.preventDefault()
        element.ownerDocument.execCommand(
          'insertText',
          false,
          paste.clipboardData.getData('text/plain')
        )
      })
      element.addEventListener(
        'blur',
        () => {
          onInput()
          element.removeEventListener('input', onInput)
          element.contentEditable = 'false'
          this.editingId = null
          this.schedule()
        },
        { once: true }
      )
    },
    acceptsDrop(event) {
      return Array.from(event.dataTransfer?.types || []).some(
        (type) => type === 'application/x-invoice-block-type'
      )
    },
    dropTarget(event) {
      const element = event.target?.closest('[data-block-id]')
      const found = findBlock(this.layout.body, element?.dataset.blockId)
      if (!found) return null
      const id = found.block.id
      if (this.draggingId && findBlock(findBlock(this.layout.body, this.draggingId)?.block, id))
        return null
      const rect = element.getBoundingClientRect()
      const x = (event.clientX - rect.left) / Math.max(1, rect.width)
      const y = (event.clientY - rect.top) / Math.max(1, rect.height)
      let placement
      if (!found.parent || (found.block.children && y > 0.25 && y < 0.75 && x > 0.2 && x < 0.8))
        placement = 'inside'
      else if (found.parent.type === 'columns') placement = x < 0.5 ? 'before' : 'after'
      else if (x < 0.12 || x > 0.88) placement = x < 0.5 ? 'left' : 'right'
      else placement = y < 0.5 ? 'before' : 'after'
      return { element, id, placement }
    },
    clearDrop(doc) {
      doc
        .querySelectorAll('[data-editor-drop]')
        .forEach((element) => element.removeAttribute('data-editor-drop'))
    }
  }
}
</script>

<style scoped>
.canvas-status {
  min-height: 37px;
  padding: 9px 16px;
  color: #64748b;
  font-size: 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}
.canvas-viewport {
  overflow: auto;
  height: calc(100vh - 350px);
  min-height: 480px;
  background: #e9edf2;
  padding: 24px;
}
.canvas-sheet-space {
  margin: 0 auto;
}
.canvas-sheet {
  position: relative;
  transform-origin: top left;
  box-shadow: 0 3px 16px #25355026;
  background: white;
}
.canvas-sheet iframe {
  display: block;
  border: 0;
  background: #fff;
}
.canvas-interaction-layer {
  position: absolute;
  top: 22px;
  right: 0;
  bottom: 0;
  left: 0;
  cursor: grab;
  touch-action: none;
  outline: none;
}
.canvas-selection-handle {
  position: absolute;
  z-index: 2;
  width: 18px;
  height: 23px;
  padding: 0;
  border: 1px solid #2563eb;
  border-radius: 3px;
  background: #eff6ff;
  color: #2563eb;
  cursor: grab;
  touch-action: none;
}
.canvas-ruler {
  height: 22px;
  border-bottom: 1px solid #cbd5e1;
  background: repeating-linear-gradient(
      90deg,
      #94a3b8 0,
      #94a3b8 1px,
      transparent 1px,
      transparent 37.795px
    )
    bottom / 100% 8px no-repeat #fafafa;
}
.canvas-footnote {
  padding: 8px 16px;
  font-size: 12px;
  color: #64748b;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}
</style>
