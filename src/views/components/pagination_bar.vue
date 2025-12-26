<template>
  <div class="pagination-bar d-flex align-items-center justify-content-between mt-2">
    <div class="d-flex align-items-center">
      <b-form inline>
        <b-form-select
          size="sm"
          class="d-inline-block mb-2 mr-2 pl-2 pr-4"
          :value="size"
          :options="sizes"
          @input="$emit('update:size', Number($event))"
          @change="onSizeChange"
        />
        <div class="pt-1 text-muted">
          <i class="fas fa-globe mr-1"></i>
          Hiển thị từ
          <b class="pl-1 pr-2">{{ from }}</b>
          đến
          <b class="pl-1 pr-2">{{ to }}</b>
          trong tổng số
          <b class="pl-1 pr-2">{{ total }}</b>
          bản ghi.
        </div>
      </b-form>
    </div>
    <div class="flex-grow-1"></div>
    <div class="text-right">
      <b-pagination
        v-if="total > 0 && totalPages > 1"
        align="right"
        :value="current"
        :per-page="size"
        :total-rows="total"
        :hide-goto-end-buttons="true"
        size="sm"
        pills
        @input="onPageInput"
        @change="onPageChange"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: 'PaginationBar',
  props: {
    current: { type: Number, required: true },
    size: { type: Number, required: true },
    total: { type: Number, required: true },
    sizes: { type: Array, default: () => [10, 20, 50, 100] }
  },
  computed: {
    totalPages() {
      const s = Number(this.size || 10)
      const t = Number(this.total || 0)
      if (!s || s <= 0) return 1
      return Math.max(1, Math.ceil(t / s))
    },
    from() {
      const t = Number(this.total || 0)
      if (t <= 0) return 0
      const s = Math.max(1, Number(this.size || 10))
      const c = Math.max(1, Number(this.current || 1))
      return (c - 1) * s + 1
    },
    to() {
      const t = Number(this.total || 0)
      if (t <= 0) return 0
      const s = Math.max(1, Number(this.size || 10))
      const c = Math.max(1, Number(this.current || 1))
      return Math.min(c * s, t)
    }
  },
  methods: {
    onPageInput(val) { this.$emit('update:current', Number(val)) },
    onPageChange(val) { this.$emit('page-change', Number(val)) },
    onSizeChange() { this.$emit('size-change', Number(this.size)) }
  }
}
</script>

<style scoped>
.pagination-bar { padding-top: 10px; margin-top: 12px; border-top: 1px dashed #e5e7eb; }
.pagination-bar .custom-select-sm, .pagination-bar .form-control-sm { height: 30px; padding: 4px 10px; font-size: 13px; border-radius: 8px; }
.pagination-bar .pagination { margin: 0; gap: 6px; }
.pagination-bar .page-item .page-link { min-width: 32px; height: 30px; padding: 0 10px; border-radius: 8px; border: 1px solid #e5e7eb; color: #374151; font-size: 13px; line-height: 28px; text-align: center; background-color: #fff; transition: all 0.15s ease; }
.pagination-bar .page-item:not(.active):not(.disabled) .page-link:hover { background-color: #f3f4f6; border-color: #d1d5db; color: #111827; }
.pagination-bar .page-item.active .page-link { background-color: #2563eb; border-color: #2563eb; color: #fff; font-weight: 600; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15); }
.pagination-bar .page-item.disabled .page-link { background-color: #f9fafb; color: #9ca3af; border-color: #e5e7eb; }
@media (max-width: 576px) { .pagination-bar { flex-direction: column; align-items: flex-start !important; gap: 8px; } .pagination-bar .pagination { justify-content: flex-start; } }
</style>