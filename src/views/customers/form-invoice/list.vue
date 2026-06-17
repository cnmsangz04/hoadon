<template>
  <div class="container-fluid py-3 form-invoices">
    <!-- Tiêu đề và thao tác -->
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

    <!-- Bộ lọc -->
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
        <b-col md="2" class="mb-2 text-right">
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

    <!-- Bảng mẫu hóa đơn -->
    <b-card class="shadow-sm">
      <b-table
        class="form-invoices-table"
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

        <template #cell(status)="{ item }">
          <b-badge :variant="statusVariant(item.status)">{{ statusText(item.status) }}</b-badge>
        </template>

        <template #cell(username)="{ item }">
          {{ item.username || usernameOf(item.userId || item.user_id) }}
        </template>

        <template #cell(updated_at)="{ item }">
          {{ formatDate(item.updatedAt || item.updated_at) }}
        </template>

        <template #cell(option)="{ item }">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="goEdit(item)">Cập nhật</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="btnView(item)">Xem</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) === 0" class="text-center text-danger" href="#" @click.prevent="onDelete(item)">Xóa</b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <!-- Khung tải khi chuyển trang -->
      <div v-if="isBusy" class="mt-2">
        <b-skeleton width="100%" height="20px" animated class="mb-2" />
        <b-skeleton width="96%" height="20px" animated class="mb-2" />
        <b-skeleton width="92%" height="20px" animated class="mb-2" />
      </div>

      <pagination-bar
        :current.sync="list.current_page"
        :size.sync="list.per_page"
        :total="list.total"
        :sizes="pageSizes"
        @page-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </b-card>

    <!-- Hộp thoại xem hóa đơn bằng iframe -->
    <b-modal
      id="modalFormInvoice"
      size="lg"
      :no-close-on-esc="false"
      title="Xem mẫu hóa đơn"
      body-class="invoice-preview-body"
    >
      <iframe
        id="viewInv"
        class="invoice-preview-frame"
        :src="iframe.src"
        :srcdoc="iframe.srcdoc"
        onload="((obj) => {try{obj.style.height = obj.contentWindow.document.body.scrollHeight + 'px';}catch(e){obj.style.height = 0;}})(this)"
      ></iframe>
      <template #modal-footer>
        <div class="modal-footer-spread">
          <b-button variant="light" size="sm" @click="closeModal('modalFormInvoice')">Đóng</b-button>
          <div class="modal-footer-actions">
            <b-dropdown
              id="ddown-right"
              text="Tải xuống"
              extra-toggle-classes="nav-link-custom"
              variant="success"
              size="sm"
            >
              <b-dropdown-item href="#" @click.prevent="downloadFormFile('pdf')">
                <i class="fas fa-file-pdf mr-1"></i> Tải PDF
              </b-dropdown-item>
              <b-dropdown-item href="#" @click.prevent="downloadFormFile('xml')">
                <i class="fas fa-file-code mr-1"></i> Tải XML
              </b-dropdown-item>
            </b-dropdown>
          </div>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import PaginationBar from '@/views/components/pagination_bar.vue'

export default {
  name: 'CustomerFormInvoiceList',
  components: { PaginationBar },
  data() {
    return {
      isBusy: false,
      usersMap: {},
      // Trạng thái iframe để xem hóa đơn
      iframe: {
        src: null,
        srcdoc: '',
        form_id: null,
        status: null,
      },
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
        type: null,
        status: null,
      },
      fields: [
        { key: 'index', label: '#', thStyle: { width: '4%' } },
        { key: 'name', label: 'Tên mẫu', thStyle: { width: '18%' } },
        { key: 'serial', label: 'Ký hiệu', thStyle: { width: '11%' } },
        { key: 'category', label: 'Loại hóa đơn', thStyle: { width: '14%' } },
        { key: 'type', label: 'Loại thuế suất', thStyle: { width: '13%' } },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '10%' } },
        { key: 'username', label: 'Người tạo', thStyle: { width: '13%' } },
        { key: 'updated_at', label: 'Ngày cập nhật', thStyle: { width: '12%' } },
        { key: 'option', label: 'Chức năng', thStyle: { width: '5%' } }
      ],
      categoryOptions: [
        { value: 1, text: 'Hóa đơn giá trị gia tăng' },
        { value: 2, text: 'Hóa đơn bán hàng' }
      ],
      typeOptions: [
        { value: 1, text: 'Một thuế suất' },
        { value: 2, text: 'Nhiều thuế suất' }
      ],
      statusOptions: [
        { value: 1, text: 'Kích hoạt' },
        { value: 0, text: 'Chưa kích hoạt' },
      ],
    }
  },
  created() { this.fetchList() },
  methods: {
    closeModal(id) {
      this.$root.$emit('bv::hide::modal', id)
    },
    print(formId) {
      const iframe = document.getElementById('viewInv')
      if (iframe && iframe.contentWindow) {
        try { iframe.contentWindow.focus(); iframe.contentWindow.print(); } catch (e) {}
      }
    },
    async btnView(item) {
      const formId = item.form_id != null ? item.form_id : (item.id != null ? item.id : null)
      if (!formId) return false
      try {
        const { data } = await axios.get(`/form-invoices/${formId}/view`, {
          responseType: 'text',
          headers: { Accept: 'text/html' }
        })
        this.iframe.form_id = formId
        this.iframe.src = 'about:blank'
        this.iframe.srcdoc = data || ''
        this.$root.$emit('bv::show::modal', 'modalFormInvoice')
      } catch (e) {
        this.$bvToast && this.$bvToast.toast('Không thể xem mẫu hóa đơn này', {
          title: 'Lỗi',
          variant: 'danger',
          solid: true,
          autoHideDelay: 3000
        })
      }
      return false
    },

    async downloadFormFile(type) {
      const id = this.iframe.form_id
      if (!id) return
      const ext = type === 'pdf' ? 'pdf' : 'xml'
      const mime = type === 'pdf' ? 'application/pdf' : 'application/xml;charset=utf-8'
      try {
        const { data } = await axios.get(`/form-invoices/${id}/download-${ext}`, {
          responseType: 'blob',
          headers: { Accept: mime }
        })
        const blob = data instanceof Blob ? data : new Blob([data], { type: mime })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `mau-hoa-don-${id}.${ext}`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
      } catch (e) {
        // Lỗi đã được interceptor axios xử lý
      }
    },

    async fetchList() {
      this.isBusy = true
      try {
        const params = {
          page: this.list.current_page,
          size: this.list.per_page,
          q: this.filters.keyword || undefined,
          category: this.filters.category || undefined,
          type: this.filters.type || undefined,
          status: this.filters.status || undefined,
        }
        const { data } = await axios.get('/form-invoices', { params })
        const items = (data.items || data.data || []).map(it => ({
          id: it.id,
          name: it.name,
          serial: it.serial,
          category: it.category,
          type: it.type,
          status: it.status,
          userId: it.userId || it.user_id,
          username: it.username,
          updatedAt: it.updatedAt || it.updated_at,
          file: it.file,
          photo: it.photo,
        }))
        this.list.data = items
        this.list.total = data.total || 0
        this.list.per_page = data.per_page || this.list.per_page
        this.list.current_page = data.current_page || this.list.current_page
        this.list.last_page = data.last_page || 1
        const from = (this.list.current_page - 1) * this.list.per_page + (items.length ? 1 : 0)
        const to = from + items.length - (items.length ? 0 : 0)
        this.list.from = from
        this.list.to = to
      } catch (e) {
        // Xử lý âm thầm
      } finally {
        this.isBusy = false
      }
    },
    reload() { this.fetchList() },
    goCreate() { this.$router.push({ name: 'CustomerFormInvoiceTemplate' }) },
    goEdit(item) { this.$router.push({ name: 'CustomerFormInvoiceEdit', params: { id: item.id } }) },
    async onDelete(item) {
      try {
        const id = item.id != null ? item.id : (item.form_id != null ? item.form_id : null)
        if (!id) return
        const ok = await this.$bvModal.msgBoxConfirm(
          `Bạn có chắc muốn xóa mẫu hóa đơn #${id}?`,
          {
            title: 'Xác nhận',
            size: 'sm',
            buttonSize: 'sm',
            okVariant: 'danger',
            okTitle: 'Xóa',
            cancelTitle: 'Hủy',
            footerClass: 'p-2',
            hideHeaderClose: false
          }
        )
        if (!ok) return
        await axios.delete(`/form-invoices/${id}`)
        this.$bvToast && this.$bvToast.toast('Đã xóa mẫu hóa đơn', { title: 'Thành công', variant: 'success', solid: true, autoHideDelay: 3000 })
        // Tải lại danh sách sau khi xóa
        this.applyFilters()
      } catch (e) {
        let msg = 'Xóa mẫu hóa đơn thất bại'
        if (e && e.response) {
          if (e.response.status === 400) {
            msg = 'Không thể xóa mẫu đã tạo hóa đơn'
          } else if (e.response.status === 403) {
            msg = 'Không có quyền xóa mẫu này'
          } else if (e.response.status === 404) {
            msg = 'Mẫu không tồn tại'
          }
        }
        this.$bvToast && this.$bvToast.toast(msg, { title: 'Lỗi', variant: 'danger', solid: true, autoHideDelay: 4000 })
      }
    },

    onPageSizeChange(size) {
      this.list.per_page = Number(size) || this.list.per_page
      this.list.current_page = 1
      this.fetchList()
    },
    onPageChange(page) {
      this.list.current_page = Number(page) || 1
      this.fetchList()
    },
    applyFilters() { this.list.current_page = 1; this.fetchList() },
    resetFilters() { this.filters = { keyword: '', category: null, type: null, status: null }; this.applyFilters() },

    categoryLabel(v) { return v === 1 ? 'Giá trị gia tăng' : v === 2 ? 'Bán hàng' : '—' },
    categoryVariant(v) { return v === 1 ? 'primary' : v === 2 ? 'info' : 'secondary' },
    typeLabel(v) { return v === 1 ? 'Một thuế suất' : v === 2 ? 'Nhiều thuế suất' : '—' },
    typeVariant(v) { return v === 1 ? 'success' : v === 2 ? 'warning' : 'secondary' },
    statusText(v) { return Number(v) === 1 ? 'Kích hoạt' : 'Chưa kích hoạt' },
    statusVariant(v) { return Number(v) === 1 ? 'success' : 'secondary' },
    usernameOf(uid) { return this.usersMap[uid] || '—' },
    formatDate(dt) { try { return (dt || '').toString().replace('T', ' ') } catch { return '—' } },
  }
}
</script>

<style scoped>
.font-weight-bold { font-weight: 700; }

/* Bỏ style grid tùy chỉnh để khớp layout registers/invoice */
.filters-grid, .filter-item, .span-4, .span-3, .span-2, .filter-actions { display: unset; grid-column: unset; }

/* Giữ style control gọn tương tự trang registers */
.shadow-sm { border-radius: 10px; }

#viewInv { min-height: 300px; }

.form-invoices::v-deep .table-responsive {
  overflow-x: hidden;
}

.form-invoices::v-deep .form-invoices-table {
  width: 100%;
  table-layout: fixed;
}

.form-invoices::v-deep .form-invoices-table th,
.form-invoices::v-deep .form-invoices-table td {
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
  vertical-align: middle;
}
</style>
