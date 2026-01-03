<template>
  <div class="container-fluid py-3 form-invoices">
    <!-- Header and actions -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center">
        <h4 class="mb-0 font-weight-bold">Danh sách mẫu hóa đơn</h4>
      </div>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
        <b-button size="sm" variant="success" @click="goCreate">
          <i class="fas fa-file-alt"></i>
          Thêm mẫu
        </b-button>
      </div>
    </div>

    <!-- Filters -->
    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="4" class="mb-2">
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input v-model.trim="filters.keyword" placeholder="Tìm theo tên mẫu / ký hiệu" @keyup.enter="applyFilters" />
          </b-input-group>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-select v-model="filters.category" :options="categoryOptions">
            <template #first>
              <b-form-select-option :value="null">Tất cả loại hóa đơn</b-form-select-option>
            </template>
          </b-form-select>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-select v-model="filters.type" :options="typeOptions">
            <template #first>
              <b-form-select-option :value="null">Tất cả loại thuế suất</b-form-select-option>
            </template>
          </b-form-select>
        </b-col>
        <b-col md="2" class="text-right">
          <b-button size="sm" variant="primary" @click="applyFilters">Tìm kiếm</b-button>
        </b-col>
      </b-row>
      <b-row class="mt-2">
        <b-col md="12" class="mb-2 d-flex align-items-center justify-content-end">
          <b-button size="sm" variant="outline-secondary" class="mr-2" @click="resetFilters">Xóa lọc</b-button>
          <b-button size="sm" variant="outline-primary" @click="applyFilters">
            <i class="fas fa-filter"></i>
            Áp dụng
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <!-- Form invoices table -->
    <b-card class="shadow-sm">
      <b-table
        bordered
        hover
        responsive
        small
        show-empty
        :items="list.data"
        :fields="fields"
        :busy="isBusy"
        empty-text="Không có dữ liệu"
      >
        <template #cell(index)="{ index }">
          {{ index + 1 + (list.current_page - 1) * list.per_page }}
        </template>

        <template #cell(serial)="{ item }">
          <code>{{ item.serial || '—' }}</code>
        </template>

        <template #cell(category)="{ item }">
          <b-badge :variant="categoryVariant(item.category)">{{ item.categoryLabel || categoryLabel(item.category) }}</b-badge>
        </template>

        <template #cell(type)="{ item }">
          <b-badge :variant="typeVariant(item.type)">{{ item.typeLabel || typeLabel(item.type) }}</b-badge>
        </template>

        <template #cell(username)="{ item }">
          {{ item.username || usernameOf(item.userId || item.user_id) }}
        </template>

        <template #cell(updated_at)="{ item }">
          {{ formatDate(item.updatedAt || item.updated_at) }}
        </template>

        <template #cell(option)="{ item }">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret>
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="goEdit(item)">Cập nhật</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="goView(item)">Xem</b-dropdown-item>
            <b-dropdown-item class="text-center text-danger" href="#" @click.prevent="onDelete(item)">Xóa</b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <!-- Loading skeleton when changing page -->
      <div v-if="isBusy" class="mt-2">
        <b-skeleton width="100%" height="20px" animated class="mb-2" />
        <b-skeleton width="96%" height="20px" animated class="mb-2" />
        <b-skeleton width="92%" height="20px" animated class="mb-2" />
      </div>

      <b-row class="mt-2">
        <b-col cols="6">
          <b-form inline>
            <b-form-select
              size="sm"
              class="d-inline-block mb-2 mr-2 pl-2 pr-4"
              v-model.number="list.per_page"
              :options="pageSizes"
              @input="onPageSizeChange"
            />
            <div class="pt-1 text-muted">
              <i class="fas fa-globe mr-1"></i> Hiển thị từ
              <b class="pl-1 pr-2">{{ list.from || 0 }}</b>
              đến
              <b class="pl-1 pr-2">{{ list.to || 0 }}</b>
              trong tổng số
              <b class="pl-1 pr-2">{{ list.total || 0 }}</b>
              bản ghi.
            </div>
          </b-form>
        </b-col>
        <b-col cols="6">
          <b-pagination
            align="right"
            v-model.number="list.current_page"
            :per-page="list.per_page"
            :total-rows="list.total"
            :hide-goto-end-buttons="true"
            v-if="list.last_page > 1"
            size="sm"
            pills
            @input="onPageChange"
          />
        </b-col>
      </b-row>
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'CustomerFormInvoiceList',
  data() {
    return {
      isBusy: false,
      usersMap: {},
      list: {
        current_page: 1,
        data: [],
        last_page: 1,
        prev_page_url: null,
        next_page_url: null,
        per_page: 10,
        total: 0,
        from: 0,
        to: 0
      },
      pageSizes: [10, 20, 50, 100],
      filters: {
        keyword: '',
        category: null,
        type: null
      },
      fields: [
        { key: 'index', label: '#', thStyle: { width: '50px' } },
        { key: 'name', label: 'Tên mẫu' },
        { key: 'serial', label: 'Ký hiệu', thStyle: { width: '140px' } },
        { key: 'category', label: 'Loại hóa đơn', thStyle: { width: '180px' } },
        { key: 'type', label: 'Loại thuế suất', thStyle: { width: '160px' } },
        { key: 'username', label: 'Người tạo', thStyle: { width: '160px' } },
        { key: 'updated_at', label: 'Ngày cập nhật', thStyle: { width: '140px' } },
        { key: 'option', label: 'Chức năng', thStyle: { width: '140px' } }
      ],
      categoryOptions: [
        { value: 1, text: 'Hóa đơn giá trị gia tăng' },
        { value: 2, text: 'Hóa đơn bán hàng' }
      ],
      typeOptions: [
        { value: 1, text: 'Một thuế suất' },
        { value: 2, text: 'Nhiều thuế suất' }
      ]
    }
  },
  created() { this.fetchList() },
  methods: {
    formatDate(d) {
      if (!d) return '—'
      try {
        const dt = new Date(d)
        const yyyy = dt.getFullYear()
        const mm = String(dt.getMonth() + 1).padStart(2, '0')
        const dd = String(dt.getDate()).padStart(2, '0')
        return `${dd}/${mm}/${yyyy}`
      } catch { return String(d) }
    },
    categoryLabel(v) { return Number(v) === 1 ? 'Hóa đơn giá trị gia tăng' : Number(v) === 2 ? 'Hóa đơn bán hàng' : '—' },
    categoryVariant(v) { return Number(v) === 1 ? 'info' : Number(v) === 2 ? 'secondary' : 'light' },
    typeLabel(v) { return Number(v) === 1 ? 'Một thuế suất' : Number(v) === 2 ? 'Nhiều thuế suất' : '—' },
    typeVariant(v) { return Number(v) === 1 ? 'success' : Number(v) === 2 ? 'warning' : 'light' },
    usernameOf(uid) { return uid ? (this.usersMap[uid] || `#${uid}`) : '—' },

    buildQuery() {
      const params = {
        page: this.list.current_page,
        size: this.list.per_page,
      }
      if (this.filters.keyword) params.q = this.filters.keyword
      if (this.filters.category != null) params.category = this.filters.category
      if (this.filters.type != null) params.type = this.filters.type
      return params
    },
    normalizePageResponse(raw) {
      const out = { ...this.list }
      if (raw && Array.isArray(raw.items)) {
        out.data = raw.items
        out.total = Number(raw.total || raw.items.length) || 0
        out.current_page = Number(raw.current_page || out.current_page) || 1
        out.last_page = Number(raw.last_page || Math.ceil(out.total / out.per_page) || 1)
        out.from = (out.current_page - 1) * out.per_page + 1
        out.to = Math.min(out.from + out.per_page - 1, out.total) || 0
        return out
      }
      if (Array.isArray(raw)) {
        out.data = raw
        out.total = raw.length
        out.current_page = 1
        out.last_page = 1
        out.from = 1
        out.to = raw.length
        return out
      }
      return out
    },
    async fetchList() {
      this.isBusy = true
      try {
        const params = this.buildQuery()
        const { data } = await axios.get('/form-invoices', { params })
        let normalized = this.normalizePageResponse(data)
        // Fallback: username resolution
        const ids = [...new Set((normalized.data || []).map(x => x.userId || x.user_id).filter(Boolean))]
        if (ids.length) {
          try {
            const ur = await axios.get('/form-invoices/users/by-ids', { params: { ids: ids.join(',') } })
            const list = Array.isArray(ur?.data) ? ur.data : (ur?.data?.items || [])
            const map = {}
            list.forEach(u => { if (u && u.id != null) map[u.id] = u.username || u.name })
            this.usersMap = map
            normalized.data = (normalized.data || []).map(it => ({ ...it, username: it.username || map[it.userId || it.user_id] }))
          } catch {}
        }
        this.list = normalized
      } catch (e) {
        // handled by axios interceptor
      } finally {
        this.isBusy = false
      }
    },
    onPageSizeChange() { this.list.current_page = 1; this.fetchList() },
    onPageChange() { this.fetchList() },
    applyFilters() { this.list.current_page = 1; this.fetchList() },
    resetFilters() { this.filters = { keyword: '', category: null, type: null }; this.list.current_page = 1; this.fetchList() },
    reload() { this.fetchList() },

    goCreate() { this.$router.push({ name: 'CustomerFormInvoiceTemplate' }) },
    goEdit(it) { this.$router.push({ name: 'CustomerFormInvoiceEdit', params: { id: it.id } }) },
    goView(it) { this.$router.push({ name: 'CustomerFormInvoiceView', params: { id: it.id } }) },
    async onDelete(it) {
      const ok = window.confirm(`Xóa mẫu "${it.name}"?`)
      if (!ok) return
      try {
        await axios.delete(`/form-invoices/${it.id}`, { successMessage: 'Đã xóa mẫu' })
        this.fetchList()
      } catch {}
    }
  }
}
</script>

<style scoped>
.font-weight-bold { font-weight: 700; }
</style>