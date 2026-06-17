<template>
  <div class="pagination-bar">
    <div class="pagination-summary">
      <b-form inline class="pagination-size-form">
        <b-form-select
          size="sm"
          class="pagination-size-select"
          :value="size"
          :options="sizes"
          @input="onSizeInput"
        />
        <div class="pt-1 text-muted">
          <i class="fas fa-globe mr-1"></i> Hiển thị từ
          <b class="pl-1 pr-2">{{ from }}</b>
          đến
          <b class="pl-1 pr-2">{{ to }}</b>
          trong tổng số
          <b class="pl-1 pr-2">{{ total }}</b>
          bản ghi.
        </div>
      </b-form>
    </div>
    <div class="pagination-pages">
      <b-pagination
        v-if="totalPages > 1"
        class="pagination-control"
        :value="current"
        :per-page="size"
        :total-rows="total"
        :hide-goto-end-buttons="true"
        prev-text="‹"
        next-text="›"
        size="sm"
        pills
        @input="onPageInput"
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
    onPageInput(val) {
      const page = Number(val)
      this.$emit('update:current', page)
      this.$emit('page-change', page)
    },
    onSizeInput(val) {
      const size = Number(val)
      this.$emit('update:size', size)
      this.$emit('size-change', size)
    }
  }
}
</script>

<style scoped>
.pagination-bar {
  align-items: center;
  padding-top: 10px;
  margin-top: 12px;
  border-top: 1px dashed #e5e7eb;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.pagination-summary,
.pagination-pages {
  min-width: 0;
}

.pagination-size-form {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination-size-select {
  height: 32px;
  margin: 0;
  min-width: 64px;
  padding: 4px 28px 4px 10px;
  font-size: 13px;
  border-radius: 8px;
}

.pagination-bar ::v-deep .pagination {
  align-items: center;
  display: flex;
  margin: 0;
  gap: 6px;
  justify-content: flex-end;
}

.pagination-bar ::v-deep .page-item .page-link {
  align-items: center;
  display: inline-flex;
  height: 32px;
  justify-content: center;
  line-height: 1;
  min-width: 32px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  color: #2563eb !important;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  background-color: #fff;
  transition: all 0.15s ease;
}

.pagination-bar ::v-deep .page-item:not(.active):not(.disabled) .page-link:hover {
  background-color: #f3f4f6;
  border-color: #d1d5db;
  color: #111827 !important;
}

.pagination-bar ::v-deep .page-item.active .page-link {
  background-color: #2563eb;
  border-color: #2563eb;
  color: #fff !important;
  font-weight: 800;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15);
}

.pagination-bar ::v-deep .page-item.disabled .page-link {
  background-color: #f9fafb;
  color: #9ca3af !important;
  border-color: #e5e7eb;
}

.pagination-bar ::v-deep .page-item:first-child .page-link,
.pagination-bar ::v-deep .page-item:last-child .page-link {
  font-size: 18px;
  padding-bottom: 2px;
}

@media (max-width: 576px) {
  .pagination-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-size-form {
    align-items: flex-start;
  }

  .pagination-pages,
  .pagination-bar ::v-deep .pagination {
    justify-content: flex-start;
  }
}
</style>
