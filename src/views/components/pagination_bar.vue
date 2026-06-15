<template>
  <b-row class="pagination-bar mt-2">
    <b-col cols="6">
      <b-form inline>
        <b-form-select
          size="sm"
          class="d-inline-block mb-2 mr-2 pl-2 pr-4"
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
    </b-col>
    <b-col cols="6">
      <b-pagination
        v-if="totalPages > 1"
        align="right"
        :value="current"
        :per-page="size"
        :total-rows="total"
        :hide-goto-end-buttons="true"
        size="sm"
        pills
        @input="onPageInput"
      />
    </b-col>
  </b-row>
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
  padding-top: 10px;
  margin-top: 12px;
  border-top: 1px dashed #e5e7eb;
}

.pagination-bar .custom-select-sm {
  height: 30px;
  padding: 4px 10px;
  font-size: 13px;
  border-radius: 8px;
}

.pagination-bar .pagination {
  margin: 0;
  gap: 6px;
}

.pagination-bar ::v-deep .page-item .page-link {
  min-width: 32px;
  height: 30px;
  padding: 0 10px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  color: #374151;
  font-size: 13px;
  line-height: 28px;
  text-align: center;
  background-color: #fff;
  transition: all 0.15s ease;
}

.pagination-bar ::v-deep .page-item:not(.active):not(.disabled) .page-link:hover {
  background-color: #f3f4f6;
  border-color: #d1d5db;
  color: #111827;
}

.pagination-bar ::v-deep .page-item.active .page-link {
  background-color: #2563eb;
  border-color: #2563eb;
  color: #fff;
  font-weight: 600;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15);
}

.pagination-bar ::v-deep .page-item.disabled .page-link {
  background-color: #f9fafb;
  color: #9ca3af;
  border-color: #e5e7eb;
}

@media (max-width: 576px) {
  .pagination-bar {
    flex-direction: column;
    align-items: flex-start !important;
    gap: 8px;
  }

  .pagination-bar > div {
    flex: 0 0 100%;
    max-width: 100%;
  }
  .pagination-bar .pagination {
    justify-content: flex-start;
  }
}
</style>
