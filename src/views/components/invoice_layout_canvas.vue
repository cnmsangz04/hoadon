<template>
  <div class="invoice-layout-canvas">
    <div class="canvas-status" role="status" aria-live="polite">
      <span v-if="loading">Đang cập nhật trang…</span>
      <span v-else-if="error" class="text-danger">{{ error }}</span>
      <span v-else>Trang thiết kế</span>
      <span class="canvas-page-size"
        >{{ layout.page.format }} ·
        {{ layout.page.orientation === "landscape" ? "Ngang" : "Dọc" }}</span
      >
      <span v-if="interactionNotice" class="canvas-interaction-notice">{{ interactionNotice }}</span>
      <button
        v-if="error"
        type="button"
        class="btn btn-sm btn-link"
        @click="schedule"
      >
        Thử lại
      </button>
    </div>
    <div ref="viewport" class="canvas-viewport">
      <div class="canvas-sheet-space" :style="spaceStyle">
        <div class="canvas-sheet" :style="sheetStyle">
          <div class="canvas-ruler" aria-hidden="true" />
          <iframe
            v-if="previewUrl"
            ref="frame"
            :src="previewUrl"
            :style="{ width: pageWidth + 'px', height: documentHeight + 'px' }"
            title="Trang thiết kế mẫu hóa đơn"
            @load="attachDocument"
          />
          <div
            v-show="!editingId"
            ref="interactionLayer"
            class="canvas-interaction-layer"
            :style="{ cursor: cursorMode }"
            tabindex="0"
            role="application"
            aria-label="Chỉnh sửa bố cục hóa đơn"
            @dblclick="editAt"
            @pointerdown="startPointer"
            @pointermove="handlePointerMove"
            @pointerleave="resetCursor"
            @pointerup="finishPointer"
            @pointercancel="cancelPointer"
            @lostpointercapture="lostPointerCapture"
            @keydown.esc.prevent.stop="cancelInteraction"
            @dragover.prevent="paletteOver"
            @dragleave="clearMarkers"
            @drop.prevent="paletteDrop"
          />
          <div v-if="hoverBox && !editingId" class="canvas-hover-outline" :style="hoverBox" />
          <div v-if="selectionBox && activeIds.length > 1" class="canvas-selection-outline" :style="selectionBox"><span>{{ activeIds.length }} phần tử</span></div>
          <div v-if="marqueeBox" class="canvas-marquee" :style="marqueeBox" />
          <div v-if="dropPreviewBox" class="canvas-drop-preview" :class="{'drop-invalid':dropInvalid}" :style="dropPreviewBox"><span>{{ dropInvalid ? 'Nhóm đã đủ 6 phần tử' : 'Thả phần tử tại đây' }}</span></div>
          <div v-for="(guide,index) in guides" :key="index" class="canvas-snap-guide" :style="guide" />
          <button
            v-if="resizeLeftBox && selectedId !== layout.body.id && !editingId"
            type="button"
            class="canvas-resize-edge canvas-resize-edge-left"
            :style="resizeLeftBox"
            :disabled="disabled"
            title="Kéo cạnh trái để đổi chiều rộng"
            aria-label="Kéo cạnh trái để đổi chiều rộng"
            @pointerdown.stop.prevent="startResize($event, 'width-left')"
          />
          <button
            v-if="resizeRightBox && selectedId !== layout.body.id && !editingId"
            type="button"
            class="canvas-resize-edge canvas-resize-edge-right"
            :style="resizeRightBox"
            :disabled="disabled"
            title="Kéo cạnh phải để đổi chiều rộng"
            aria-label="Kéo cạnh phải để đổi chiều rộng"
            @pointerdown.stop.prevent="startResize($event, 'width')"
          />
          <button
            v-if="resizeBottomBox && selectedId !== layout.body.id && !editingId"
            type="button"
            class="canvas-resize-edge canvas-resize-edge-bottom"
            :style="resizeBottomBox"
            :disabled="disabled"
            title="Kéo cạnh dưới để đổi chiều cao"
            aria-label="Kéo cạnh dưới để đổi chiều cao"
            @pointerdown.stop.prevent="startResize($event, 'height')"
          />
          <button
            v-if="resizeCornerBox && !editingId"
            type="button"
            class="canvas-resize-corner"
            :style="resizeCornerBox"
            :disabled="disabled"
            title="Kéo góc để đổi kích thước hình ảnh"
            aria-label="Kéo góc để đổi kích thước hình ảnh"
            @pointerdown.stop.prevent="startResize($event, 'image-corner')"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from "@/plugins/axios";
import { findBlock } from "@/utils/invoice_layout";

export default {
  name: "InvoiceLayoutCanvas",
  props: {
    layout: { type: Object, required: true },
    templateId: { type: Number, required: true },
    base: { type: String, required: true },
    selectedId: { type: String, default: "" },
    selectedIds: { type: Array, default: () => [] },
    paletteItem: { type: Object, default: null },
    zoom: { type: Number, default: 80 },
    rows: { type: Number, default: 3 },
    disabled: { type: Boolean, default: false },
  },
  data: () => ({
    documentHtml: "",
    previewUrl: "",
    fontCss: "",
    documentHeight: 1123,
    loading: false,
    error: "",
    timer: null,
    generation: 0,
    editingId: null,
    draggingId: null,
    pointerDrag: null,
    resizeRightBox: null,
    resizeLeftBox: null,
    resizeBottomBox: null,
    resizeCornerBox: null,
    cursorMode: "grab",
    resizeDrag: null,
    observer: null,
    measureFrame: null,
    destroyed: false,
    selectionBox: null,
    hoverBox: null,
    marqueeBox: null,
    dropPreviewBox: null,
    dropInvalid: false,
    guides: [],
    interactionNotice: "",
    pendingPlacement: null,
    autoScrollFrame: null,
    lastPointer: null,
  }),
  computed: {
    activeIds() {
      return this.selectedIds.length ? this.selectedIds : this.selectedId ? [this.selectedId] : [];
    },
    pageWidth() {
      const short = this.layout.page.format === "A5" ? 148 : 210;
      const long = this.layout.page.format === "A5" ? 210 : 297;
      return (
        ((this.layout.page.orientation === "landscape" ? long : short) * 96) /
        25.4
      );
    },
    pageHeight() {
      const short = this.layout.page.format === "A5" ? 148 : 210;
      const long = this.layout.page.format === "A5" ? 210 : 297;
      return (
        ((this.layout.page.orientation === "landscape" ? short : long) * 96) /
        25.4
      );
    },
    spaceStyle() {
      return {
        width: (this.pageWidth * this.zoom) / 100 + "px",
        height: ((this.documentHeight + 22) * this.zoom) / 100 + "px",
      };
    },
    sheetStyle() {
      return {
        transform: `scale(${this.zoom / 100})`,
        width: this.pageWidth + "px",
      };
    },
  },
  watch: {
    layout: {
      deep: true,
      handler() {
        this.schedule();
      },
    },
    templateId() {
      this.editingId = null;
      this.schedule();
    },
    rows() {
      this.schedule();
    },
    zoom() {
      this.$nextTick(this.markSelection);
    },
    selectedId() {
      this.markSelection();
    },
    selectedIds() {
      this.markSelection();
    },
    disabled() {
      this.markSelection();
    },
  },
  created() {
    this.schedule();
  },
  beforeDestroy() {
    this.destroyed = true;
    this.generation++;
    clearTimeout(this.timer);
    if (this.measureFrame !== null) cancelAnimationFrame(this.measureFrame);
    this.observer?.disconnect();
    this.removeResizeListeners();
    this.stopAutoScroll();
    window.removeEventListener("blur", this.cancelInteraction);
    window.removeEventListener("keydown",this.globalKeyDown,true);
    this.revokePreviewUrl();
  },
  mounted() {
    window.addEventListener("blur", this.cancelInteraction);
    window.addEventListener("keydown",this.globalKeyDown,true);
  },
  methods: {
    schedule() {
      clearTimeout(this.timer);
      const generation = ++this.generation;
      if (this.editingId || this.pointerDrag || this.resizeDrag) return;
      this.timer = setTimeout(() => { this.timer=null; this.refresh(generation); }, 250);
    },
    async refresh(generation) {
      this.loading = true;
      this.error = "";
      try {
        if (!this.fontCss) {
          const { data } = await axios.get(`${this.base}/designer-fonts`, {
            meta: { suppressGlobalErrorToast: true },
          });
          if (generation !== this.generation || this.destroyed) return;
          this.fontCss = data.css;
        }
        const { data } = await axios.post(
          `${this.base}/${this.templateId}/designer-preview`,
          {
            layout: this.layout,
            rows: this.rows,
          },
          { meta: { suppressGlobalErrorToast: true } }
        );
        if (generation !== this.generation || this.destroyed) return;
        this.setPreviewDocument(this.decorate(data.html));
      } catch (e) {
        if (generation !== this.generation || this.destroyed) return;
        this.error =
          e.response?.data?.message ||
          e.message ||
          "Không cập nhật được trang thiết kế";
      } finally {
        if (generation === this.generation && !this.destroyed)
          this.loading = false;
      }
    },
    decorate(html) {
      const margins = this.layout.page.marginMm;
      const css = `
        ${this.fontCss}
        html{background:#fff}body{width:${this.pageWidth}px;min-height:${this.pageHeight}px;padding:${margins.top}mm ${margins.right}mm ${margins.bottom}mm ${margins.left}mm!important}
        [data-block-id]{position:relative;cursor:grab;outline-offset:2px;user-select:none;touch-action:none}
        [data-editor-dragging]{cursor:grabbing}
        [data-block-id]:hover{outline:1px dashed #94a3b8}
        [data-editor-selected]{outline:2px solid #2563eb!important;z-index:1}
        .signatureSummary[data-editor-selected]{border-color:transparent!important}
        .signatureSummary[data-editor-selected]>.signature-valid-box{border-color:transparent!important}
        .columns>.block[data-width-fixed]{flex:0 0 auto}
        [data-editor-drop=before]{box-shadow:0 -3px #2563eb}
        [data-editor-drop=after]{box-shadow:0 3px #2563eb}
        [data-editor-drop=left]{box-shadow:-3px 0 #2563eb}
        [data-editor-drop=right]{box-shadow:3px 0 #2563eb}
        [data-editor-drop=inside]{background-color:#dbeafe!important;outline:2px dashed #2563eb}
        [data-block-id][contenteditable=true]{cursor:text;min-height:1.3em;user-select:text}
        .stack:empty,.columns:empty,.totals:empty,.relatedInvoice:empty{min-height:12mm;border:1px dashed #cbd5e1}
        .stack:empty:after,.columns:empty:after,.totals:empty:after,.relatedInvoice:empty:after{content:'Thả phần tử vào đây';color:#94a3b8;font:10pt sans-serif}
        .field:empty,.signatureSummary>.signature-valid-box:empty{min-height:7mm;border:1px dashed #cbd5e1}
        .field:empty:after{content:'Trường dữ liệu';color:#94a3b8;font:10pt sans-serif}
        .signatureSummary>.signature-valid-box:empty:after{content:'Thông tin chữ ký';color:#94a3b8;font:10pt sans-serif}
        .pageBreak{outline:1px dashed #94a3b8}.pageBreak:after{content:'Ngắt trang';position:absolute;top:0;left:0;font:8pt sans-serif;color:#64748b;pointer-events:none}
      `;
      const policy = `<meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; font-src data:; style-src 'unsafe-inline';">`;
      return this.sanitizePreviewHtml(html)
        .replace("<head>", "<head>" + policy)
        .replace("</head>", `<style>${css}</style></head>`);
    },
    sanitizePreviewHtml(html) {
      const parsed = new DOMParser().parseFromString(String(html || ""), "text/html");
      parsed
        .querySelectorAll(
          "script, iframe, frame, frameset, object, embed, base, form, input, button, meta[http-equiv='refresh']"
        )
        .forEach((element) => element.remove());
      parsed.querySelectorAll("*").forEach((element) => {
        [...element.attributes].forEach((attribute) => {
          const name = attribute.name.toLowerCase();
          const value = attribute.value.trim().toLowerCase();
          if (
            name.startsWith("on") ||
            (["href", "src", "xlink:href", "formaction"].includes(name) &&
              value.startsWith("javascript:"))
          )
            element.removeAttribute(attribute.name);
        });
      });
      return parsed.documentElement.outerHTML;
    },
    setPreviewDocument(html) {
      this.revokePreviewUrl();
      this.documentHtml = html;
      this.previewUrl = URL.createObjectURL(
        new Blob([html], { type: "text/html;charset=utf-8" })
      );
    },
    revokePreviewUrl() {
      if (!this.previewUrl) return;
      URL.revokeObjectURL(this.previewUrl);
      this.previewUrl = "";
    },
    attachDocument() {
      const doc = this.$refs.frame?.contentDocument;
      if (!doc?.body || !this.documentHtml) return;
      this.observer?.disconnect();
      doc.querySelectorAll("[data-block-id]").forEach((element) => {
        element.draggable = false;
      });
      doc.addEventListener("click", (event) => {
        if (!this.disabled)
          this.$emit(
            "select",
            event.target.closest("[data-block-id]")?.dataset.blockId || null
          );
      });
      doc.addEventListener("keydown", (event) => {
        if (this.disabled) return;
        if (event.target.isContentEditable) {
          if (
            !(event.ctrlKey || event.metaKey) ||
            event.key.toLowerCase() !== "s"
          )
            return;
          event.target.blur();
        }
        this.$emit("shortcut", event);
      });
      const measure = () => {
        if (this.destroyed || this.$refs.frame?.contentDocument !== doc) return;
        if (this.measureFrame !== null) return;
        this.measureFrame = requestAnimationFrame(() => {
          this.measureFrame = null;
          if (this.destroyed || this.$refs.frame?.contentDocument !== doc)
            return;
          const height = Math.max(
            this.pageHeight,
            Math.ceil(Math.max(doc.body.getBoundingClientRect().height,
              ...[...doc.querySelectorAll("[data-block-id]")].map(node=>node.getBoundingClientRect().bottom)))
          );
          if (height !== this.documentHeight) this.documentHeight = height;
        });
      };
      this.observer = new MutationObserver(measure);
      this.observer.observe(doc.body, {
        subtree: true,
        childList: true,
        characterData: true,
        attributes: true,
      });
      doc.fonts.ready.then(() => {
        measure();
        this.settlePlacement();
      });
      doc.addEventListener("load", measure, true);
      measure();
      this.markSelection();
    },
    point(event) {
      const frame = this.$refs.frame;
      const doc = frame?.contentDocument;
      if (!doc) return null;
      const rect = frame.getBoundingClientRect();
      const scale = this.zoom / 100;
      const clientX = (event.clientX - rect.left) / scale;
      const clientY = (event.clientY - rect.top) / scale;
      return {
        target: this.hitElement(doc, clientX, clientY),
        clientX,
        clientY,
      };
    },
    hitElement(doc, x, y) {
      const candidates = [...new Set(doc.elementsFromPoint(x, y)
        .map(node => node.closest("[data-block-id]"))
        .filter(Boolean))];
      const painted = candidates.find(node => {
        if (node.matches(".text,.field")) {
          const range = doc.createRange();
          range.selectNodeContents(node);
          return [...range.getClientRects()].some(rect =>
            x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom);
        }
        const block = findBlock(this.layout.body, node.dataset.blockId)?.block;
        return block && !block.children && !["spacer", "divider", "pageBreak"].includes(block.type);
      });
      return painted || candidates.find(node => {
        const type = findBlock(this.layout.body, node.dataset.blockId)?.block.type;
        return type !== "spacer" && type !== "pageBreak";
      }) || candidates[0] || null;
    },
    blockElement(id) {
      return this.$refs.frame?.contentDocument?.querySelector(`[data-block-id="${id}"]`);
    },
    blockPosition(block) {
      return block.position
        ? { x: Number(block.position.xMm || 0), y: Number(block.position.yMm || 0) }
        : { x: Number(block.style?.marginLeftMm || 0), y: Number(block.style?.offsetTopMm || 0) };
    },
    rootIds(ids) {
      const unique = [...new Set(ids)].filter(id => id !== this.layout.body.id && findBlock(this.layout.body, id));
      return unique.filter(id => !unique.some(other => other !== id && findBlock(findBlock(this.layout.body, other)?.block, id)));
    },
    emitSelection(ids, primaryId) {
      const roots = this.rootIds(ids);
      this.$emit("selection", { ids: roots, primaryId: roots.includes(primaryId) ? primaryId : roots[roots.length - 1] || null });
    },
    rectUnion(rects) {
      if (!rects.length) return null;
      const left = Math.min(...rects.map(r => r.left));
      const top = Math.min(...rects.map(r => r.top));
      const right = Math.max(...rects.map(r => r.right));
      const bottom = Math.max(...rects.map(r => r.bottom));
      return { left, top, right, bottom, width: right-left, height: bottom-top };
    },
    overlayBox(rect) {
      return rect ? { left: rect.left+"px", top: (22+rect.top)+"px", width: rect.width+"px", height: rect.height+"px" } : null;
    },
    inkRect(element) {
      if (element.matches(".text,.field")) {
        const range = element.ownerDocument.createRange();
        range.selectNodeContents(element);
        const r = range.getBoundingClientRect();
        if (r.width && r.height) return { left:r.left, top:r.top, right:r.right, bottom:r.bottom, width:r.width, height:r.height };
      }
      return element.getBoundingClientRect();
    },
    contentRects(block, node, excluded = new Set()) {
      if (!node || excluded.has(block.id)) return [];
      if (block.children) return block.children.flatMap(child =>
        this.contentRects(child, this.blockElement(child.id), excluded));
      if (["spacer", "divider", "pageBreak"].includes(block.type)) return [];
      return [block.type === "image" ? (node.querySelector("img") || node).getBoundingClientRect() : this.inkRect(node)];
    },
    movementRects(item) {
      return item.block.children
        ? [item.rect, ...item.content]
        : [item.ink];
    },
    applyPosition(node, x, y) {
      node.style.setProperty("--block-x", x+"mm");
      node.style.setProperty("--block-y", y+"mm");
    },
    editAt(event) {
      if (this.activeIds.length > 1) return;
      const point = this.point(event);
      if (point?.target) this.beginEditing(point);
    },
    startPointer(event) {
      if (this.disabled || event.button !== 0) return;
      const point = this.point(event);
      if (!point) return;
      this.pendingPlacement=null;
      const hit = point.target?.closest("[data-block-id]");
      const modifier = event.shiftKey || event.ctrlKey || event.metaKey;
      const resizeAxis = !modifier && this.resizeAxisAt(point);
      if (resizeAxis) { this.startResize(event, resizeAxis); return; }
      let element = hit;
      const primary = this.blockElement(this.selectedId);
      if (!modifier && !event.altKey && primary && primary !== hit && primary.contains(hit)) element = primary;
      let ids = this.rootIds(this.activeIds);
      const multiBox = this.rectUnion(ids.map(id => this.blockElement(id)?.getBoundingClientRect()).filter(Boolean));
      const inMulti = ids.length > 1 && multiBox && point.clientX >= multiBox.left && point.clientX <= multiBox.right && point.clientY >= multiBox.top && point.clientY <= multiBox.bottom;
      const candidate=findBlock(this.layout.body,element?.dataset.blockId)?.block;
      const er=element?.getBoundingClientRect();
      const nearBorder=er && Math.min(Math.abs(point.clientX-er.left),Math.abs(point.clientX-er.right),Math.abs(point.clientY-er.top),Math.abs(point.clientY-er.bottom))<6/(this.zoom/100);
      const blankGroup=candidate?.children && !ids.includes(candidate.id) && !nearBorder;
      const empty = !element || element.dataset.blockId === this.layout.body.id || blankGroup;
      if (modifier && !empty) {
        const id = element.dataset.blockId;
        ids = ids.includes(id) ? ids.filter(x => x !== id) : [...ids,id];
        this.emitSelection(ids,id);
        return;
      }
      if (empty && !inMulti) {
        this.pointerDrag = { kind:"marquee", pointerId:event.pointerId, x:event.clientX, y:event.clientY, point, initialIds:modifier ? ids : [],restoreIds:ids,clickId:blankGroup ? candidate.id : null,pendingRender:this.timer!==null || this.loading };
        if (!modifier) this.emitSelection([],null);
        this.$refs.interactionLayer.setPointerCapture(event.pointerId);
        clearTimeout(this.timer);
        this.timer=null;
        this.generation++;
        this.marqueeBox = this.overlayBox({left:point.clientX,top:point.clientY,width:0,height:0});
        return;
      }
      if (!inMulti && !ids.includes(element.dataset.blockId)) ids = [element.dataset.blockId];
      const roots = this.rootIds(ids);
      if (!roots.length) return;
      this.emitSelection(roots, roots.includes(element?.dataset.blockId) ? element.dataset.blockId : roots[0]);
      const snapshots = roots.map(id => {
        const found = findBlock(this.layout.body,id);
        const node = this.blockElement(id);
        return found?.block && node
          ? { id, block:found.block, parent:found.parent, node, position:this.blockPosition(found.block), rect:node.getBoundingClientRect(), ink:this.inkRect(node), content:this.contentRects(found.block,node) }
          : null;
      }).filter(Boolean);
      if (snapshots.length !== roots.length) return;
      const viewport = this.$refs.viewport;
      this.pointerDrag = {
        kind:"position", pointerId:event.pointerId, x:event.clientX, y:event.clientY,
        snapshots, scrollX:viewport.scrollLeft, scrollY:viewport.scrollTop,
        clickId: hit?.dataset.blockId, primaryId:roots[0],
      };
      clearTimeout(this.timer);
      this.generation++;
      this.loading = false;
      this.$refs.interactionLayer.setPointerCapture(event.pointerId);
      this.$refs.interactionLayer.focus({preventScroll:true});
      event.preventDefault();
    },
    handlePointerMove(event) {
      this.lastPointer = { clientX:event.clientX, clientY:event.clientY, pointerId:event.pointerId };
      this.updateCursor(event);
      this.movePointer(event);
    },
    updateCursor(event) {
      this.hoverBox=null;
      if (this.pointerDrag) { this.cursorMode = this.pointerDrag.kind === "marquee" ? "crosshair" : "grabbing"; return; }
      if (this.disabled) { this.cursorMode = "default"; return; }
      const point=this.point(event);
      const node=point?.target?.closest("[data-block-id]");
      if (node && node.dataset.blockId !== this.layout.body.id && !this.activeIds.includes(node.dataset.blockId))
        this.hoverBox=this.overlayBox(node.getBoundingClientRect());
      const axis = this.resizeAxisAt(point);
      this.cursorMode = axis?.startsWith("width") ? "ew-resize" : axis === "height" ? "ns-resize" : "grab";
    },
    resizeAxisAt(point) {
      if (!point || this.activeIds.length !== 1) return null;
      const found = findBlock(this.layout.body,this.selectedId);
      const rect = this.blockElement(this.selectedId)?.getBoundingClientRect();
      if (!found || !rect || found.block.id === this.layout.body.id || found.block.type === "pageBreak") return null;
      const scale = this.zoom/100;
      const tx = Math.min(4/scale,rect.width/8);
      const ty = Math.min(3/scale,rect.height/6);
      if (point.clientY >= rect.top && point.clientY <= rect.bottom) {
        if (Math.abs(point.clientX-rect.right)<=tx) return "width";
        if (Math.abs(point.clientX-rect.left)<=tx) return "width-left";
      }
      if (found.block.type === "image" && point.clientX >= rect.right - 12 / scale && point.clientY >= rect.bottom - 12 / scale) return "image-corner";
      if (Math.abs(point.clientY-rect.bottom)<=ty && point.clientX>=rect.left && point.clientX<=rect.right) return "height";
      return null;
    },
    resetCursor() {
      this.hoverBox=null;
      if (!this.pointerDrag && !this.resizeDrag) this.cursorMode="grab";
    },
    intersects(a,b,gap=1) {
      return a.left < b.right+gap && a.right > b.left-gap && a.top < b.bottom+gap && a.bottom > b.top-gap;
    },
    translated(rect,dx,dy) {
      return { ...rect,left:rect.left+dx,right:rect.right+dx,top:rect.top+dy,bottom:rect.bottom+dy };
    },
    positionDelta(drag, dx, dy) {
      const selected = new Set(drag.snapshots.map(s=>s.id));
      const bounds = this.rectUnion(drag.snapshots.flatMap(s=>this.movementRects(s)));
      const margins=this.layout.page.marginMm;
      const area = {left:margins.left*96/25.4,top:margins.top*96/25.4,right:this.pageWidth-margins.right*96/25.4,bottom:this.documentHeight-margins.bottom*96/25.4};
      let minX=area.left-bounds.left, maxX=area.right-bounds.right;
      let minY=area.top-bounds.top, maxY=area.bottom-bounds.bottom;
      for (const item of drag.snapshots) {
        if (item.parent?.type !== "signatureSummary") continue;
        const frame=item.node.parentElement.getBoundingClientRect();
        minX=Math.max(minX,frame.left-item.ink.left);
        maxX=Math.min(maxX,frame.right-item.ink.right);
        minY=Math.max(minY,frame.top-item.ink.top);
        maxY=Math.min(maxY,frame.bottom-item.ink.bottom);
      }
      const clamp = (x,y) => ({
        x:Math.max(minX,Math.min(maxX,x)),
        y:Math.max(minY,Math.min(maxY,y))
      });
      const obstacles = [];
      for (const item of drag.snapshots) {
        for (const sibling of item.parent?.children || []) {
          if (selected.has(sibling.id)) continue;
          const node=this.blockElement(sibling.id);
          obstacles.push(...this.contentRects(sibling,node,selected));
        }
      }
      let desired=clamp(dx,dy);
      const threshold=drag.disableSnap ? 0 : 5/(this.zoom/100);
      const xLines=[], yLines=[];
      for (const obstacle of obstacles) {
        for (const target of [obstacle.left,obstacle.right,(obstacle.left+obstacle.right)/2])
          for (const origin of [bounds.left,bounds.right,(bounds.left+bounds.right)/2])
            if (Math.abs(target-origin-desired.x)<threshold) xLines.push({delta:target-origin,line:target});
        for (const target of [obstacle.top,obstacle.bottom,(obstacle.top+obstacle.bottom)/2])
          for (const origin of [bounds.top,bounds.bottom,(bounds.top+bounds.bottom)/2])
            if (Math.abs(target-origin-desired.y)<threshold) yLines.push({delta:target-origin,line:target});
      }
      const xSnap=xLines.sort((a,b)=>Math.abs(a.delta-desired.x)-Math.abs(b.delta-desired.x))[0];
      const ySnap=yLines.sort((a,b)=>Math.abs(a.delta-desired.y)-Math.abs(b.delta-desired.y))[0];
      if (xSnap) desired.x=xSnap.delta;
      if (ySnap) desired.y=ySnap.delta;
      desired=clamp(desired.x,desired.y);
      const movingInk=drag.snapshots.flatMap(s=>s.content);
      const collides=(d)=>movingInk.some(rect=>obstacles.some(o=>this.intersects(this.translated(rect,d.x,d.y),o,1)));
      let resolved=desired;
      if (collides(desired)) {
        const candidates=[];
        for (const ink of movingInk) for (const obstacle of obstacles) {
          candidates.push(clamp(obstacle.right+3-ink.left,desired.y));
          candidates.push(clamp(obstacle.left-3-ink.right,desired.y));
          candidates.push(clamp(desired.x,obstacle.bottom+3-ink.top));
          candidates.push(clamp(desired.x,obstacle.top-3-ink.bottom));
        }
        const valid=candidates.filter(d=>!collides(d)).sort((a,b)=>
          Math.hypot(a.x-desired.x,a.y-desired.y)-Math.hypot(b.x-desired.x,b.y-desired.y)
        );
        const nearby=valid.find(d=>Math.hypot(d.x-desired.x,d.y-desired.y)<=8/(this.zoom/100));
        if (nearby) resolved=nearby;
        else {
          const last=drag.lastDelta || {x:0,y:0};
          const slides=[clamp(desired.x,last.y),clamp(last.x,desired.y)].filter(d=>!collides(d));
          resolved=slides.sort((a,b)=>Math.hypot(a.x-desired.x,a.y-desired.y)-Math.hypot(b.x-desired.x,b.y-desired.y))[0] || last;
        }
        this.interactionNotice="Đã canh để tránh chồng nội dung";
      } else this.interactionNotice="Thả chuột để đặt phần tử · Esc để hủy";
      drag.lastDelta=resolved;
      this.guides=[];
      if (xSnap && Math.abs(xSnap.delta-resolved.x)<.5) this.guides.push({left:xSnap.line+"px",top:"22px",height:this.documentHeight+"px",width:"1px"});
      if (ySnap && Math.abs(ySnap.delta-resolved.y)<.5) this.guides.push({left:"0px",top:(22+ySnap.line)+"px",width:this.pageWidth+"px",height:"1px"});
      return resolved;
    },
    startResize(event, axis) {
      if (this.disabled) return;
      this.pendingPlacement=null;
      const found = findBlock(this.layout.body, this.selectedId);
      if (!found || found.block.id === this.layout.body.id || found.block.type === "pageBreak") return;
      const element = this.$refs.frame?.contentDocument?.querySelector(
        `[data-block-id="${this.selectedId}"]`
      );
      const rect = element?.getBoundingClientRect();
      if (!element || !rect) return;
      const widthMm = Number(found.block.widthMm) > 0
        ? Number(found.block.widthMm)
        : Math.max(5, Math.min(270, ((rect?.width || 190 * 96 / 25.4) * 25.4) / 96));
      const measuredHeightMm = Math.max(3, Math.min(150, ((rect?.height || 10 * 96 / 25.4) * 25.4) / 96));
      const heightMm = axis === "height"
        ? measuredHeightMm
        : Number(found.block.heightMm) > 0 ? Number(found.block.heightMm) : measuredHeightMm;
      const doc = this.$refs.frame.contentDocument;
      const trailingBlock = axis === "height" && found.block.children?.length
        ? found.block.children[found.block.children.length - 1]
        : null;
      const trailingSpacerElement = trailingBlock?.type === "spacer"
        ? doc.querySelector(`[data-block-id="${trailingBlock.id}"]`)
        : null;
      const trailingSpacerRect = trailingSpacerElement?.getBoundingClientRect();
      const trailingSpacerHeightMm = trailingSpacerRect
        ? trailingSpacerRect.height * 25.4 / 96
        : 0;
      const resizeSnapLines = axis === "height"
        ? [...doc.querySelectorAll("[data-block-id]")]
            .filter((node) => node !== element && !element.contains(node) && !node.contains(element))
            .flatMap((node) => {
              const candidate = node.getBoundingClientRect();
              return [candidate.top, candidate.bottom];
            })
        : [];
      const columnCandidates = found.parent?.type === "columns" && axis !== "height"
        ? found.parent.children.map((block) => {
            const node = doc.querySelector(`[data-block-id="${block.id}"]`);
            return node
              ? { id: block.id, element: node, widthMm: node.getBoundingClientRect().width * 25.4 / 96 }
              : null;
          })
        : null;
      const columns = columnCandidates?.every(Boolean) ? columnCandidates : null;
      const columnIndex = columns?.findIndex((column) => column.id === found.block.id);
      const neighborIndex = axis === "width-left"
        ? columnIndex - 1
        : axis === "image-corner" && columnIndex === columns?.length - 1
          ? columnIndex - 1
          : columnIndex + 1;
      const neighbor = columns?.[neighborIndex];
      this.resizeDrag = {
        id: found.block.id,
        element,
        originalStyle:element.getAttribute("style"),
        originalHeightFixed:element.getAttribute("data-height-fixed"),
        trailingSpacer: trailingSpacerElement ? {
          id: trailingBlock.id,
          element: trailingSpacerElement,
          inner: trailingSpacerElement.firstElementChild,
          heightMm: trailingSpacerHeightMm,
          originalStyle: trailingSpacerElement.getAttribute("style"),
          originalInnerStyle: trailingSpacerElement.firstElementChild?.getAttribute("style"),
        } : null,
        resizeSnapLines,
        image:element.querySelector("img"),
        originalImageStyle:element.querySelector("img")?.getAttribute("style"),
        captureTarget: event.currentTarget,
        pointerId: event.pointerId,
        startX: event.clientX,
        startY: event.clientY,
        axis,
        widthMm: columns ? columns[columnIndex].widthMm : widthMm,
        heightMm,
        columns,
        columnStyles:columns?.map(column=>column.element.getAttribute("style")),
        neighbor,
        position: this.blockPosition(found.block),
        rect,
        area:found.parent?.type === "signatureSummary" ? element.parentElement.getBoundingClientRect() : {
          left:this.layout.page.marginMm.left*96/25.4,
          right:this.pageWidth-this.layout.page.marginMm.right*96/25.4,
          bottom:this.documentHeight-this.layout.page.marginMm.bottom*96/25.4,
        },
        minWidthMm: found.block.type === "signatureSummary" ? 20 : 5,
        minHeightMm: trailingSpacerElement
          ? Math.max(3, heightMm - trailingSpacerHeightMm + 1)
          : found.block.type === "signatureSummary" ? 10 : 3,
      };
      this.guides = [];
      event.currentTarget?.setPointerCapture(event.pointerId);
      event.preventDefault();
      clearTimeout(this.timer);
      this.generation++;
      this.loading = false;
      this.$emit("resize-start", { id: found.block.id });
      window.addEventListener("pointermove", this.resizePointerMove);
      window.addEventListener("pointerup", this.finishResize);
      window.addEventListener("pointercancel", this.finishResize);
    },
    resizePointerMove(event) {
      const drag = this.resizeDrag;
      if (!drag || drag.pointerId !== event.pointerId || this.disabled) return;
      const scale = Math.max(0.4, this.zoom / 100);
      const deltaX = ((event.clientX - drag.startX) / scale) * 25.4 / 96;
      const deltaY = ((event.clientY - drag.startY) / scale) * 25.4 / 96;
      const imageCorner = drag.axis === "image-corner";
      const horizontal = drag.axis !== "height" && !imageCorner;
      const requestedWidth = drag.widthMm + (drag.axis === "width-left" ? -deltaX : deltaX);
      const availableWidth=(drag.axis === "width-left" ? drag.rect.right-drag.area.left : drag.area.right-drag.rect.left)*25.4/96;
      const maxWidth = drag.neighbor ? drag.widthMm + drag.neighbor.widthMm - 5 : Math.min(270,availableWidth);
      const cornerDelta = Math.abs(deltaX) >= Math.abs(deltaY) ? deltaX : deltaY;
      const widthMm = imageCorner
        ? Math.max(drag.minWidthMm, Math.min(maxWidth, drag.widthMm + cornerDelta))
        : horizontal ? Math.max(drag.minWidthMm, Math.min(maxWidth, requestedWidth)) : drag.widthMm;
      const maxHeightMm = Math.min(150,(drag.area.bottom-drag.rect.top)*25.4/96);
      let heightMm = imageCorner
        ? Math.max(drag.minHeightMm, Math.min(maxHeightMm, widthMm * drag.heightMm / Math.max(1, drag.widthMm)))
        : horizontal ? drag.heightMm : Math.max(drag.minHeightMm, Math.min(maxHeightMm, drag.heightMm + deltaY));
      this.guides = [];
      if (!horizontal && !imageCorner && !event.altKey) {
        const proposedBottom = drag.rect.top + heightMm * 96 / 25.4;
        const threshold = 6 / scale;
        const snapLine = drag.resizeSnapLines
          .map((line) => ({ line, distance: Math.abs(line - proposedBottom) }))
          .filter((candidate) => candidate.distance <= threshold)
          .sort((a, b) => a.distance - b.distance)[0];
        if (snapLine) {
          const snappedHeightMm = (snapLine.line - drag.rect.top) * 25.4 / 96;
          if (snappedHeightMm >= drag.minHeightMm && snappedHeightMm <= maxHeightMm) {
            heightMm = snappedHeightMm;
            this.guides = [{
              left: "0px",
              top: `${22 + snapLine.line}px`,
              width: `${this.pageWidth}px`,
              height: "2px",
            }];
            this.interactionNotice = "Đã canh ngang với phần tử kế bên · Giữ Alt để bỏ canh";
          }
        }
      }
      let columnWidths;
      if (drag.columns && drag.neighbor) {
        columnWidths = drag.columns.map((column) => ({
          id: column.id,
          widthMm: column.id === drag.id ? widthMm
            : column.id === drag.neighbor.id ? column.widthMm + drag.widthMm - widthMm
              : column.widthMm,
        }));
        drag.columns.forEach((column, index) => {
          column.element.style.width = `${columnWidths[index].widthMm}mm`;
          column.element.style.flex = "0 0 auto";
        });
      } else if (horizontal || imageCorner) {
        drag.element.style.width = `${widthMm}mm`;
      }
      else {
        drag.element.style.minHeight = `${heightMm}mm`;
        drag.element.dataset.heightFixed = "true";
        if (drag.element.parentElement?.classList.contains("columns"))
          drag.element.style.alignSelf = "flex-start";
      }
      let linkedHeights;
      if (!horizontal && !imageCorner && drag.trailingSpacer) {
        const spacerHeightMm = Math.max(1, drag.trailingSpacer.heightMm + heightMm - drag.heightMm);
        drag.trailingSpacer.element.style.minHeight = `${spacerHeightMm}mm`;
        if (drag.trailingSpacer.inner)
          drag.trailingSpacer.inner.style.height = `${spacerHeightMm}mm`;
        linkedHeights = [{ id: drag.trailingSpacer.id, heightMm: spacerHeightMm }];
      }
      if (drag.image) {
        drag.image.style.width=`${widthMm}mm`;
        drag.image.style.height=`${heightMm}mm`;
      }
      let position;
      if (drag.axis === "width-left" && !drag.neighbor) {
        position={xMm:drag.position.x + drag.widthMm - widthMm,yMm:drag.position.y};
        this.applyPosition(drag.element,position.xMm,position.yMm);
      }
      this.positionResizeEdges(drag.element);
      drag.pending = { id: drag.id, widthMm, heightMm, axis: imageCorner ? "image" : horizontal ? "width" : "height", columnWidths, linkedHeights, position };
      event.preventDefault();
    },
    finishResize(event) {
      if (!this.resizeDrag || (event && event.pointerId !== this.resizeDrag.pointerId)) return;
      const drag = this.resizeDrag;
      this.removeResizeListeners();
      if (event?.type==="pointercancel") {
        drag.element.setAttribute("style",drag.originalStyle || "");
        if (drag.originalHeightFixed == null) drag.element.removeAttribute("data-height-fixed");
        else drag.element.setAttribute("data-height-fixed",drag.originalHeightFixed);
        if (drag.trailingSpacer) {
          drag.trailingSpacer.element.setAttribute("style",drag.trailingSpacer.originalStyle || "");
          drag.trailingSpacer.inner?.setAttribute("style",drag.trailingSpacer.originalInnerStyle || "");
        }
        drag.image?.setAttribute("style",drag.originalImageStyle || "");
        drag.columns?.forEach((column,index)=>column.element.setAttribute("style",drag.columnStyles[index] || ""));
      }
      if (drag.captureTarget?.hasPointerCapture(drag.pointerId))
        drag.captureTarget.releasePointerCapture(drag.pointerId);
      if (drag.pending && event?.type !== "pointercancel") this.$emit("resize", drag.pending);
      this.$emit("resize-end", { id: drag.id });
      this.resizeDrag = null;
      this.guides = [];
      this.cursorMode = "grab";
      this.markSelection();
      this.schedule();
    },
    removeResizeListeners() {
      window.removeEventListener("pointermove", this.resizePointerMove);
      window.removeEventListener("pointerup", this.finishResize);
      window.removeEventListener("pointercancel", this.finishResize);
    },
    movePointer(event) {
      const drag=this.pointerDrag;
      if (!drag || drag.pointerId!==event.pointerId || this.disabled) return;
      const scale=this.zoom/100;
      const viewport=this.$refs.viewport;
      if (drag.kind==="marquee") {
        const point=this.point(event);
        if (!point) return;
        const rect=this.rectUnion([
          {left:drag.point.clientX,right:drag.point.clientX,top:drag.point.clientY,bottom:drag.point.clientY},
          {left:point.clientX,right:point.clientX,top:point.clientY,bottom:point.clientY}
        ]);
        this.marqueeBox=this.overlayBox(rect);
        if (rect.width+rect.height>4/scale) {
          drag.moved=true;
          const doc=this.$refs.frame.contentDocument;
          const ids=[...doc.querySelectorAll("[data-block-id]")].filter(el=>{
            if (el.dataset.blockId===this.layout.body.id) return false;
            const r=el.getBoundingClientRect();
            return r.left>=rect.left && r.right<=rect.right && r.top>=rect.top && r.bottom<=rect.bottom;
          }).map(el=>el.dataset.blockId);
          this.emitSelection([...drag.initialIds,...ids],ids[ids.length-1]);
        }
        this.startAutoScroll();
        return;
      }
      const dx=(event.clientX-drag.x+viewport.scrollLeft-drag.scrollX)/scale;
      const dy=(event.clientY-drag.y+viewport.scrollTop-drag.scrollY)/scale;
      if (!this.draggingId && Math.hypot(dx,dy)*scale<3) return;
      if (!this.draggingId) {
        this.draggingId=drag.primaryId;
        this.$emit("position-start");
        drag.snapshots.forEach(s=>s.node.setAttribute("data-editor-dragging",""));
      }
      const delta=this.positionDelta(drag,dx,dy);
      drag.pending=drag.snapshots.map(s=>({
        id:s.id, offsetXMm:s.position.x+delta.x*25.4/96, offsetYMm:s.position.y+delta.y*25.4/96
      }));
      drag.pending.forEach((p,index)=>this.applyPosition(drag.snapshots[index].node,p.offsetXMm,p.offsetYMm));
      this.selectionBox=this.overlayBox(this.rectUnion(drag.snapshots.map(s=>s.node.getBoundingClientRect())));
      if (drag.snapshots.length===1) this.positionResizeEdges(drag.snapshots[0].node);
      event.preventDefault?.();
      this.startAutoScroll();
    },
    finishPointer(event) {
      const drag=this.pointerDrag;
      if (!drag || drag.pointerId!==event.pointerId) return;
      const cancelled=event.type==="pointercancel";
      const started=!!this.draggingId;
      this.pointerDrag=null;
      this.draggingId=null;
      this.stopAutoScroll();
      this.marqueeBox=null;
      this.guides=[];
      this.interactionNotice="";
      this.cursorMode="grab";
      if (drag.kind==="position") {
        drag.snapshots.forEach(s=>{
          s.node.removeAttribute("data-editor-dragging");
          if (cancelled) this.applyPosition(s.node,s.position.x,s.position.y);
        });
        if (started) {
          if (!cancelled && drag.pending) this.$emit("position",{changes:drag.pending});
          this.$emit("position-end");
        } else if (!cancelled && drag.clickId && !this.activeIds.includes(drag.clickId)) {
          this.emitSelection([drag.clickId],drag.clickId);
        }
      } else if (cancelled) {
        this.emitSelection(drag.restoreIds,drag.restoreIds[0]);
      } else if (!drag.moved && drag.clickId) {
        this.emitSelection([drag.clickId],drag.clickId);
      }
      const layer=this.$refs.interactionLayer;
      if (layer?.hasPointerCapture(event.pointerId)) layer.releasePointerCapture(event.pointerId);
      this.markSelection();
      if (started || drag.pendingRender) this.schedule();
    },
    cancelPointer() {
      if (!this.pointerDrag) return;
      this.finishPointer({pointerId:this.pointerDrag.pointerId,type:"pointercancel"});
    },
    cancelInteraction() {
      if (this.pointerDrag) this.cancelPointer();
      if (this.resizeDrag) this.finishResize({pointerId:this.resizeDrag.pointerId,type:"pointercancel"});
      this.clearMarkers();
    },
    globalKeyDown(event) {
      if (event.key==="Escape" && (this.pointerDrag || this.resizeDrag)) {
        event.preventDefault();
        event.stopImmediatePropagation();
        this.cancelInteraction();
      }
    },
    lostPointerCapture() {
      if (this.pointerDrag) this.cancelPointer();
    },
    startAutoScroll() {
      if (this.autoScrollFrame!==null || !this.pointerDrag) return;
      const tick=()=>{
        this.autoScrollFrame=-1;
        if (!this.pointerDrag || !this.lastPointer) { this.autoScrollFrame=null; return; }
        const viewport=this.$refs.viewport;
        const rect=viewport.getBoundingClientRect();
        const p=this.lastPointer;
        const top=Math.max(rect.top,100), bottom=Math.min(rect.bottom,window.innerHeight);
        const left=Math.max(rect.left,0), right=Math.min(rect.right,window.innerWidth);
        const speed=(value,min,max)=>value<min+28 ? -Math.min(14,(min+28-value)/2) : value>max-28 ? Math.min(14,(value-max+28)/2) : 0;
        const oldX=viewport.scrollLeft,oldY=viewport.scrollTop;
        viewport.scrollLeft+=speed(p.clientX,left,right);
        viewport.scrollTop+=speed(p.clientY,top,bottom);
        if (oldX!==viewport.scrollLeft || oldY!==viewport.scrollTop) this.movePointer(p);
        this.autoScrollFrame=requestAnimationFrame(tick);
      };
      this.autoScrollFrame=requestAnimationFrame(tick);
    },
    stopAutoScroll() {
      if (this.autoScrollFrame!==null) cancelAnimationFrame(this.autoScrollFrame);
      this.autoScrollFrame=null;
      this.lastPointer=null;
    },
    arrangeSelection() {
      this.pendingPlacement=null;
      let ids=this.rootIds(this.activeIds);
      if (ids.length===1) {
        const block=findBlock(this.layout.body,ids[0])?.block;
        if (block?.children?.length) ids=block.children.map(child=>child.id);
      }
      if (!ids.length) return;
      const entries=ids.map(id=>{
        const found=findBlock(this.layout.body,id);
        const node=this.blockElement(id);
        return node && found ? {id,node,block:found.block,position:this.blockPosition(found.block),rect:this.inkRect(node)} : null;
      }).filter(Boolean);
      if (!entries.length) return;
      const x=Math.min(...entries.map(e=>e.rect.left));
      let y=Math.min(...entries.map(e=>e.rect.top));
      const changes=entries.map(e=>{
        const change={id:e.id,offsetXMm:e.position.x+(x-e.rect.left)*25.4/96,offsetYMm:e.position.y+(y-e.rect.top)*25.4/96};
        y+=e.rect.height+4;
        return change;
      });
      this.$emit("position-start");
      if (this.activeIds.length===1) {
        const parent=this.blockElement(this.activeIds[0]);
        if (parent?.matches(".signatureSummary")) {
          const rect=parent.getBoundingClientRect();
          const overflow=y+4-rect.bottom;
          if (overflow>0) this.$emit("resize",{id:this.activeIds[0],axis:"height",heightMm:Math.min(150,(rect.height+overflow)*25.4/96)});
        }
      }
      this.$emit("position",{changes});
      this.$emit("position-end");
      this.interactionNotice="Đã xếp các phần tử thành hàng rõ ràng";
    },
    nudgeSelection(dxMm,dyMm) {
      this.pendingPlacement=null;
      const ids=this.rootIds(this.activeIds);
      const snapshots=ids.map(id=>{
        const found=findBlock(this.layout.body,id),node=this.blockElement(id);
        if (!found?.block || !node) return null;
        return {id,block:found.block,parent:found.parent,node,position:this.blockPosition(found.block),rect:node.getBoundingClientRect(),ink:this.inkRect(node),content:this.contentRects(found.block,node)};
      }).filter(Boolean);
      if (!snapshots.length || snapshots.length !== ids.length) return false;
      const delta=this.positionDelta({snapshots,disableSnap:true},dxMm*96/25.4,dyMm*96/25.4);
      const changes=snapshots.map(s=>({
        id:s.id,
        offsetXMm:s.position.x+delta.x*25.4/96,
        offsetYMm:s.position.y+delta.y*25.4/96,
      }));
      changes.forEach((change,index)=>this.applyPosition(
        snapshots[index].node,
        change.offsetXMm,
        change.offsetYMm
      ));
      this.$emit("position-start");
      this.$emit("position",{changes});
      this.$emit("position-end");
      this.markSelection();
      this.guides=[];
      return true;
    },
    clearMarkers() {
      this.hoverBox=null;
      const doc = this.$refs.frame?.contentDocument;
      if (doc) this.clearDrop(doc);
      this.dropPreviewBox=null;
      this.dropInvalid=false;
    },
    paletteOver(event) {
      if (this.disabled || !this.acceptsDrop(event)) return;
      this.clearMarkers();
      const point = this.point(event);
      let group=findBlock(this.layout.body,point?.target?.closest("[data-block-id]")?.dataset.blockId);
      while (group && !group.block.children) group=group.parent ? findBlock(this.layout.body,group.parent.id) : null;
      this.dropInvalid=group?.block.type==="columns" && group.block.children.length>=6;
      if (point) {
        const info=this.paletteItem;
        const isTable=info?.type?.endsWith("Table") || info?.type==="table" || info?.repeated;
        const container=this.blockElement(group?.block.id || this.layout.body.id);
        const style=container && container.ownerDocument.defaultView.getComputedStyle(container);
        const availableWidth=container ? container.clientWidth-parseFloat(style.paddingLeft)-parseFloat(style.paddingRight)
          : this.pageWidth-(this.layout.page.marginMm.left+this.layout.page.marginMm.right)*96/25.4;
        const width=isTable ? availableWidth : info?.type==="signatureSummary" ? 62*96/25.4 : 120;
        const height=info?.type==="signatureSummary" ? 24*96/25.4 : isTable ? 60 : 28;
        const left=this.layout.page.marginMm.left*96/25.4;
        const right=this.pageWidth-this.layout.page.marginMm.right*96/25.4;
        this.dropPreviewBox=this.overlayBox({left:Math.max(left,Math.min(right-width,point.clientX)),top:Math.max(this.layout.page.marginMm.top*96/25.4,point.clientY),width,height});
      }
      this.interactionNotice="Thả vào khung nét đứt để đặt phần tử";
      event.dataTransfer.dropEffect = "copy";
    },
    paletteDrop(event) {
      this.clearMarkers();
      if (this.disabled || !this.acceptsDrop(event)) return;
      const point = this.point(event);
      const type = event.dataTransfer.getData(
        "application/x-invoice-block-type"
      );
      const fieldKey = event.dataTransfer.getData(
        "application/x-invoice-field-key"
      );
      const dataSource = event.dataTransfer.getData(
        "application/x-invoice-data-source"
      );
      const node=point?.target?.closest("[data-block-id]");
      let found=findBlock(this.layout.body,node?.dataset.blockId);
      const tableColumn=type==="field" && found?.block.columns;
      while (found && !found.block.children && !tableColumn) {
        found=found.parent ? findBlock(this.layout.body,found.parent.id) : null;
      }
      const target={id:found?.block.id || this.layout.body.id,placement:"inside"};
      if (found?.block.type==="columns" && found.block.children.length>=6) {
        this.interactionNotice="Nhóm đã đủ 6 phần tử. Hãy thả vào vùng khác.";
        return;
      }
      if (!tableColumn && point) this.pendingPlacement={x:point.clientX,y:point.clientY,type,existing:[...this.$refs.frame.contentDocument.querySelectorAll("[data-block-id]")].map(el=>el.dataset.blockId),expires:Date.now()+10000};
      this.interactionNotice="";
      if (target && type)
        this.$emit("insert", {
          type,
          fieldKey,
          dataSource,
          targetId: target.id,
          placement: target.placement,
        });
    },
    settlePlacement() {
      const pending=this.pendingPlacement;
      if (!pending || pending.expires<Date.now()) { this.pendingPlacement=null; return; }
      const id=this.selectedId;
      const found=findBlock(this.layout.body,id);
      const node=this.blockElement(id);
      const matchesType=found && (found.block.type===pending.type || pending.type==="field" && found.block.type==="table");
      if (!node || !matchesType || pending.existing.includes(id)) return;
      this.pendingPlacement=null;
      const rect=this.inkRect(node);
      const old=this.blockPosition(found.block);
      const margins=this.layout.page.marginMm;
      const area={left:margins.left*96/25.4,top:margins.top*96/25.4,right:this.pageWidth-margins.right*96/25.4,bottom:this.documentHeight-margins.bottom*96/25.4};
      if (found.parent?.type==="signatureSummary") {
        const parent=node.parentElement.getBoundingClientRect();
        area.left=Math.max(area.left,parent.left);area.top=Math.max(area.top,parent.top);
        area.right=Math.min(area.right,parent.right);area.bottom=Math.min(area.bottom,parent.bottom);
      }
      const obstacles=this.contentRects(this.layout.body,this.blockElement(this.layout.body.id),new Set([id]));
      for (const other of this.$refs.frame.contentDocument.querySelectorAll("[data-block-id]")) {
        if (other.contains(node) || node.contains(other)) continue;
        const block=findBlock(this.layout.body,other.dataset.blockId)?.block;
        if (block?.style?.borderWidthMm>0 || block?.type==="signatureSummary") obstacles.push(other.getBoundingClientRect());
      }
      const placement=this.fitPlacement(rect,pending,area,obstacles);
      const change={id,offsetXMm:old.x+(placement.left-rect.left)*25.4/96,offsetYMm:old.y+(placement.top-rect.top)*25.4/96};
      this.applyPosition(node,change.offsetXMm,change.offsetYMm);
      this.$emit("position",{changes:[change],coalesce:true});
      this.markSelection();
    },
    fitPlacement(rect, point, area, obstacles) {
      const clamp=(left,top)=>({left:Math.max(area.left,Math.min(area.right-rect.width,left)),top:Math.max(area.top,Math.min(area.bottom-rect.height,top)),width:rect.width,height:rect.height});
      const target=clamp(point.x,point.y);
      const queue=[target],seen=new Set();
      const distance=candidate=>Math.hypot(candidate.left-target.left,candidate.top-target.top);
      for (let attempt=0;queue.length && attempt<120;attempt++) {
        queue.sort((a,b)=>distance(a)-distance(b));
        const candidate=queue.shift();
        const key=Math.round(candidate.left*10)+","+Math.round(candidate.top*10);
        if (seen.has(key)) continue;
        seen.add(key);
        const box={...candidate,right:candidate.left+rect.width,bottom:candidate.top+rect.height};
        const collisions=obstacles.filter(other=>this.intersects(box,other,4));
        if (!collisions.length) {
          if (distance(candidate)>1) this.interactionNotice="Đã đặt vào khoảng trống gần nhất để tránh chồng nội dung";
          return candidate;
        }
        for (const other of collisions) queue.push(
          clamp(other.left-rect.width-5,candidate.top),clamp(other.right+5,candidate.top),
          clamp(candidate.left,other.top-rect.height-5),clamp(candidate.left,other.bottom+5));
      }
      this.interactionNotice="Vị trí thả chưa đủ chỗ · Phần tử được thêm theo bố cục để bạn chỉnh tiếp";
      return rect;
    },
    markSelection() {
      const doc = this.$refs.frame?.contentDocument;
      if (!doc) return;
      doc.querySelectorAll("[data-block-id]").forEach((element) => {
        element.toggleAttribute(
          "data-editor-selected",
          this.activeIds.includes(element.dataset.blockId)
        );
        element.draggable = false;
      });
      const element = doc.querySelector(`[data-block-id="${this.selectedId}"]`);
      this.selectionBox=this.overlayBox(this.rectUnion(this.activeIds.map(id=>this.blockElement(id)?.getBoundingClientRect()).filter(Boolean)));
      this.positionResizeEdges(element);
    },
    positionResizeEdges(element) {
      const rect = element?.getBoundingClientRect();
      if (!rect) {
        this.resizeRightBox = null;
        this.resizeLeftBox = null;
        this.resizeBottomBox = null;
        this.resizeCornerBox = null;
        return;
      }
      const scale = Math.max(0.4, this.zoom / 100);
      const handleSize = Math.min(12 / scale,rect.width/4);
      const bottomHandleSize = Math.min(
        14 / scale,
        rect.height / 3
      );
      const left = rect.left;
      const top = 22 + rect.top;
      const width = rect.width;
      const height = rect.height;
      const found = findBlock(this.layout.body, this.selectedId);
      const resizable = this.activeIds.length===1 && found && found.block.id !== this.layout.body.id && found.block.type !== "pageBreak";
      this.resizeLeftBox = resizable
        ? { left: Math.max(0, left - handleSize / 2) + "px", top: Math.max(22, top) + "px", width: handleSize + "px", height: height + "px" }
        : null;
      this.resizeRightBox = resizable
        ? {
            left: Math.max(0, left + width - handleSize / 2) + "px",
            top: Math.max(22, top) + "px",
            width: handleSize + "px",
            height: height + "px",
          }
        : null;
      this.resizeBottomBox = resizable
        ? {
            left: Math.max(0, left) + "px",
            top: Math.max(22, top + height - bottomHandleSize / 2) + "px",
            width: width + "px",
            height: bottomHandleSize + "px",
          }
        : null;
      this.resizeCornerBox = resizable && found.block.type === "image"
        ? { left: `${Math.max(0, left + width - 7)}px`, top: `${Math.max(22, top + height - 7)}px`, width: "14px", height: "14px" }
        : null;
    },
    beginEditing(event) {
      if (this.disabled) return;
      const element = event.target.closest("[data-block-id]");
      const found = findBlock(this.layout.body, element?.dataset.blockId);
      if (!found || found.block.type !== "text") return;
      this.editingId = found.block.id;
      element.draggable = false;
      element.contentEditable = "true";
      element.textContent = found.block.text;
      element.focus();
      const onInput = () =>
        this.$emit("text", { id: found.block.id, text: element.innerText });
      element.addEventListener("input", onInput);
      element.addEventListener("paste", (paste) => {
        paste.preventDefault();
        element.ownerDocument.execCommand(
          "insertText",
          false,
          paste.clipboardData.getData("text/plain")
        );
      });
      element.addEventListener(
        "blur",
        () => {
          onInput();
          element.removeEventListener("input", onInput);
          element.contentEditable = "false";
          this.editingId = null;
          this.schedule();
        },
        { once: true }
      );
    },
    acceptsDrop(event) {
      return Array.from(event.dataTransfer?.types || []).some(
        (type) => type === "application/x-invoice-block-type"
      );
    },
    clearDrop(doc) {
      doc
        .querySelectorAll("[data-editor-drop]")
        .forEach((element) => element.removeAttribute("data-editor-drop"));
    },
  },
};
</script>

<style scoped>
.invoice-layout-canvas {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
.canvas-status {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
  min-height: 34px;
  padding: 7px 16px;
  color: #64748b;
  font-size: 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}
.canvas-page-size {
  margin-left: auto;
  font-size: 11px;
}
.canvas-viewport {
  overflow: auto;
  flex: 1;
  min-height: 0;
  background: #e8ecf2;
  padding: 24px;
  scrollbar-width: thin;
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
  z-index: 1;
  cursor: grab;
  touch-action: none;
  outline: none;
}
.canvas-resize-edge {
  position: absolute;
  z-index: 20;
  display: block;
  padding: 0;
  border: 0;
  background: transparent;
  pointer-events: auto;
  touch-action: none;
}
.canvas-resize-edge-right, .canvas-resize-edge-left {
  cursor: ew-resize;
}
.canvas-resize-edge-bottom {
  cursor: ns-resize;
}
.canvas-resize-corner {
  position: absolute;
  display: block;
  padding: 0;
  cursor: nwse-resize;
  border: 2px solid #2563eb;
  border-radius: 3px;
  background: #dbeafe;
  box-shadow: 0 0 0 1px #fff;
  z-index: 30;
  pointer-events: auto;
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
.canvas-hover-outline,.canvas-selection-outline,.canvas-marquee,.canvas-drop-preview,.canvas-snap-guide {
  position:absolute;
  pointer-events:none;
  z-index:8;
  box-sizing:border-box;
}
.canvas-hover-outline { border:1px dashed #64748b; }
.canvas-selection-outline { border:1px dashed #2563eb; }
.canvas-selection-outline span,.canvas-drop-preview span { position:absolute;bottom:100%;left:0;white-space:nowrap;background:#2563eb;color:white;font:12px Arial;padding:3px 6px;border-radius:3px; }
.canvas-marquee { border:1px solid #2563eb;background:#2563eb20; }
.canvas-drop-preview { border:2px dashed #2563eb;background:#dbeafe80; }
.canvas-drop-preview.drop-invalid { border-color:#dc2626;background:#fee2e280; }
.canvas-drop-preview.drop-invalid span { background:#dc2626; }
.canvas-snap-guide { background:#e11d48; }
.canvas-interaction-notice { color:#334155;max-width:55%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap; }
</style>
