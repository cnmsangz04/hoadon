<template>
  <ul class="layout-block-tree">
    <li v-for="(block, index) in blocks" :key="block.id">
      <div :class="['block-row', { selected: selectedId === block.id }]">
        <button type="button" class="block-name" @click="$emit('select', block)">
          {{ label(block) }}
        </button>
        <button type="button" title="Lên" :disabled="index === 0" @click="move(index, -1)">
          ↑
        </button>
        <button
          type="button"
          title="Xuống"
          :disabled="index === blocks.length - 1"
          @click="move(index, 1)"
        >
          ↓
        </button>
        <button type="button" title="Xóa khối" @click="remove(index)">×</button>
      </div>
      <layout-block-tree
        v-if="block.children"
        :blocks="block.children"
        :selected-id="selectedId"
        @select="$emit('select', $event)"
        @change="$emit('change')"
      />
    </li>
  </ul>
</template>

<script>
export default {
  name: 'LayoutBlockTree',
  props: { blocks: { type: Array, required: true }, selectedId: { type: String, default: '' } },
  methods: {
    label(block) {
      return (
        block.label ||
        block.text ||
        block.fieldKey ||
        {
          stack: 'Nhóm dọc',
          columns: 'Nhóm cột',
          itemsTable: 'Bảng hàng hóa',
          taxSummaryTable: 'Bảng thuế',
          feesTable: 'Bảng phí',
          totals: 'Tổng cộng',
          divider: 'Đường kẻ',
          signatureSummary: 'Thông tin chữ ký',
          image: 'Ảnh',
          pageBreak: 'Ngắt trang'
        }[block.type] ||
        block.type
      )
    },
    move(index, direction) {
      const block = this.blocks.splice(index, 1)[0]
      this.blocks.splice(index + direction, 0, block)
      this.$emit('change')
    },
    remove(index) {
      this.blocks.splice(index, 1)
      this.$emit('select', null)
      this.$emit('change')
    }
  }
}
</script>

<style scoped>
.layout-block-tree {
  list-style: none;
  padding: 0 0 0 12px;
}
.block-row {
  display: flex;
  align-items: center;
  margin: 3px 0;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
}
.block-row.selected {
  border-color: #007bff;
  background: #eaf4ff;
}
.block-row button {
  border: 0;
  background: none;
  padding: 6px;
}
.block-name {
  flex: 1;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
