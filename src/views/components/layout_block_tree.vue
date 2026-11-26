<template>
  <ul class="layout-block-tree">
    <li v-for="(block, index) in blocks" :key="block.id" class="tree-item">
      <div
        class="tree-entry"
        :class="{
          selected: block.id === selectedId,
          dragging: block.id === draggingId,
          'drop-before':
            dragOverId === block.id && dragOverPlacement === 'before',
          'drop-inside':
            dragOverId === block.id && dragOverPlacement === 'inside',
          'drop-after':
            dragOverId === block.id && dragOverPlacement === 'after',
        }"
      >
        <div
          class="block-row"
          draggable="true"
          :aria-label="`Kéo ${label(block)}`"
          @dragstart="startDrag($event, block)"
          @dragend="clearDrag"
          @dragover.prevent="dragOver($event, block)"
          @dragleave="dragLeave($event, block)"
          @drop.prevent="drop($event, block)"
        >
          <button
            v-if="block.children && block.children.length"
            type="button"
            class="tree-toggle"
            :aria-label="
              (expanded[block.id] ? 'Thu gọn ' : 'Mở nhóm ') + label(block)
            "
            :aria-expanded="!!expanded[block.id]"
            @click="$set(expanded, block.id, !expanded[block.id])"
          >
            <span :class="{ open: expanded[block.id] }" aria-hidden="true"
              >›</span
            >
          </button>
          <span v-else class="tree-toggle-placeholder" aria-hidden="true" />
          <button
            type="button"
            class="block-name"
            :title="label(block)"
            :aria-pressed="block.id === selectedId"
            @click="$emit('select', block)"
          >
            <span class="tree-block-icon" aria-hidden="true">{{
              icon(block)
            }}</span>
            <span class="tree-block-label">{{ label(block) }}</span>
          </button>
        </div>
        <div v-if="block.id === selectedId" class="tree-actions">
          <button
            type="button"
            title="Di chuyển lên"
            :disabled="index === 0"
            @click="move(index, -1)"
          >
            ↑ Lên
          </button>
          <button
            type="button"
            title="Di chuyển xuống"
            :disabled="index === blocks.length - 1"
            @click="move(index, 1)"
          >
            ↓ Xuống
          </button>
          <button
            type="button"
            class="tree-remove"
            title="Xóa khối"
            @click="remove(index)"
          >
            Xóa
          </button>
        </div>
      </div>
      <layout-block-tree
        v-if="block.children && expanded[block.id]"
        :blocks="block.children"
        :selected-id="selectedId"
        :resolve-label="resolveLabel"
        @select="$emit('select', $event)"
        @change="$emit('change')"
        @move="$emit('move', $event)"
        @remove="$emit('remove', $event)"
      />
    </li>
  </ul>
</template>

<script>
export default {
  name: "LayoutBlockTree",
  props: {
    blocks: { type: Array, required: true },
    selectedId: { type: String, default: "" },
    resolveLabel: { type: Function, default: null },
  },
  data: () => ({
    expanded: {},
    draggingId: "",
    dragOverId: "",
    dragOverPlacement: "",
  }),
  watch: {
    selectedId: {
      immediate: true,
      handler(id) {
        if (!id) return;
        const contains = (block) =>
          block.id === id || (block.children || []).some(contains);
        this.blocks.forEach((block) => {
          if (block.children?.some(contains))
            this.$set(this.expanded, block.id, true);
        });
      },
    },
  },
  methods: {
    startDrag(event, block) {
      this.draggingId = block.id;
      this.dragOverId = "";
      this.dragOverPlacement = "";
      event.dataTransfer.setData("application/x-invoice-block-id", block.id);
      event.dataTransfer.effectAllowed = "move";
    },
    dragOver(event, block) {
      const id =
        event.dataTransfer?.getData("application/x-invoice-block-id") ||
        this.draggingId;
      if (!id || id === block.id) return;
      this.dragOverId = block.id;
      this.dragOverPlacement = this.dropPlacement(event, block);
      event.dataTransfer.dropEffect = "move";
    },
    dragLeave(event, block) {
      if (event.currentTarget.contains(event.relatedTarget)) return;
      if (this.dragOverId === block.id) {
        this.dragOverId = "";
        this.dragOverPlacement = "";
      }
    },
    drop(event, block) {
      const id =
        event.dataTransfer?.getData("application/x-invoice-block-id") ||
        this.draggingId;
      const placement = this.dropPlacement(event, block);
      this.clearDrag();
      if (!id || id === block.id) return;
      this.$emit("move", { id, targetId: block.id, placement });
    },
    dropPlacement(event, block) {
      const box = event.currentTarget.getBoundingClientRect();
      const ratio = (event.clientY - box.top) / Math.max(box.height, 1);
      if (Array.isArray(block.children) && ratio > 0.25 && ratio < 0.75)
        return "inside";
      return ratio <= 0.5 ? "before" : "after";
    },
    clearDrag() {
      this.draggingId = "";
      this.dragOverId = "";
      this.dragOverPlacement = "";
    },
    label(block) {
      if (this.resolveLabel) return this.resolveLabel(block);
      return (
        block.label ||
        block.text ||
        block.fieldKey ||
        {
          stack: "Nhóm dọc",
          columns: "Nhóm cột",
          itemsTable: "Bảng hàng hóa",
          taxSummaryTable: "Bảng thuế",
          feesTable: "Bảng phí",
          totals: "Tổng cộng",
          divider: "Đường kẻ",
          signatureSummary: "Thông tin chữ ký",
          image: "Ảnh",
          pageBreak: "Ngắt trang",
        }[block.type] ||
        block.type
      );
    },
    icon(block) {
      if (block.children) return block.type === "columns" ? "▥" : "☰";
      if (block.columns) return "▦";
      return (
        {
          field: "{ }",
          text: "T",
          image: "▧",
          divider: "―",
          spacer: "↕",
          pageBreak: "↵",
          signatureSummary: "✎",
        }[block.type] || "·"
      );
    },
    move(index, direction) {
      const block = this.blocks[index];
      const target = this.blocks[index + direction];
      if (!block || !target) return;
      this.$emit("move", {
        id: block.id,
        targetId: target.id,
        placement: direction < 0 ? "before" : "after",
      });
    },
    remove(index) {
      const block = this.blocks[index];
      if (block) this.$emit("remove", block.id);
    },
  },
};
</script>

<style scoped>
.layout-block-tree {
  list-style: none;
  padding: 0;
  margin: 0;
}
.layout-block-tree .layout-block-tree {
  margin-left: 12px;
  padding-left: 5px;
  border-left: 1px solid #e2e8f0;
}
.tree-item {
  margin: 2px 0;
}
.tree-entry {
  border-radius: 5px;
}
.tree-entry:hover {
  background: #f4f7fb;
}
.tree-entry.selected {
  background: #edf4ff;
  box-shadow: inset 2px 0 #2563eb;
}
.block-row {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 34px;
  cursor: grab;
}
.block-row:active {
  cursor: grabbing;
}
.tree-entry.dragging {
  opacity: 0.45;
}
.tree-entry.drop-before .block-row {
  box-shadow: inset 0 2px #2563eb;
}
.tree-entry.drop-after .block-row {
  box-shadow: inset 0 -2px #2563eb;
}
.tree-entry.drop-inside {
  background: #eaf2ff;
}
.tree-entry.drop-inside .block-row {
  outline: 1px dashed #2563eb;
  outline-offset: -2px;
}
.tree-entry button {
  border: 0;
  background: transparent;
  color: #475569;
  font-size: 12px;
}
.tree-toggle,
.tree-toggle-placeholder {
  flex: 0 0 18px;
  width: 18px;
}
.tree-toggle {
  height: 30px;
  padding: 0;
}
.tree-toggle span {
  display: inline-block;
  font-size: 20px;
  line-height: 1;
}
.tree-toggle span.open {
  transform: rotate(90deg);
}
.tree-entry .block-name {
  display: flex;
  align-items: center;
  gap: 7px;
  flex: 1;
  min-width: 0;
  min-height: 34px;
  padding: 6px 7px 6px 0;
  text-align: left;
}
.tree-block-icon {
  flex: 0 0 16px;
  text-align: center;
  color: #8a9bb1;
  font-size: 11px;
}
.tree-block-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.selected .block-name,
.selected .tree-block-icon {
  color: #1d4ed8;
}
.tree-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 0 5px 5px 22px;
}
.tree-actions button {
  padding: 4px 5px;
  border-radius: 3px;
  font-size: 11px;
}
.tree-actions button:hover:enabled {
  background: #dceafe;
  color: #1d4ed8;
}
.tree-actions button:disabled {
  opacity: 0.4;
}
.tree-actions .tree-remove {
  margin-left: auto;
  color: #b34343;
}
.tree-actions .tree-remove:hover {
  background: #fcecec;
  color: #a12d2d;
}
.tree-entry button:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: -2px;
}
</style>
