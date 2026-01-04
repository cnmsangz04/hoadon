<template>
  <div class="container-fluid py-3 register-invoices">
    <!-- Header and actions -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center">
        <h4 class="mb-0 font-weight-bold">Danh sách tờ khai hóa đơn điện tử</h4>
      </div>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
        <b-button size="sm" variant="success" @click="openCreate">
          <i class="fas fa-file-alt"></i>
          Tạo tờ khai
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
            <b-form-input v-model.trim="filters.keyword" placeholder="Tìm theo mã hồ sơ / số thông báo / tax code / tên DN" @keyup.enter="applyFilters" />
          </b-input-group>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-select v-model="filters.declarationType" :options="declarationTypeOptions">
            <template #first>
              <b-form-select-option :value="null">Tất cả hình thức tờ khai</b-form-select-option>
            </template>
          </b-form-select>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-select v-model="filters.status" :options="statusOptions">
            <template #first>
              <b-form-select-option :value="null">Tất cả trạng thái</b-form-select-option>
            </template>
          </b-form-select>
        </b-col>
        <b-col md="2" class="text-right">
          <b-button size="sm" variant="primary" @click="applyFilters">Tìm kiếm</b-button>
        </b-col>
      </b-row>
      <b-row class="mt-2">
        <b-col md="3" class="mb-2">
          <b-form-datepicker
            v-model="filters.dateFrom"
            :max="filters.dateTo || undefined"
            placeholder="Từ ngày"
            size="sm"
            locale="vi"
            :date-format-options="{ day: '2-digit', month: '2-digit', year: 'numeric' }"
          />
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-datepicker
            v-model="filters.dateTo"
            :min="filters.dateFrom || undefined"
            placeholder="Đến ngày"
            size="sm"
            locale="vi"
            :date-format-options="{ day: '2-digit', month: '2-digit', year: 'numeric' }"
          />
        </b-col>
        <b-col md="6" class="mb-2 d-flex align-items-center justify-content-end">
          <b-button size="sm" variant="outline-secondary" class="mr-2" @click="resetFilters">Xóa lọc</b-button>
          <b-button size="sm" variant="outline-primary" @click="applyFilters">
            <i class="fas fa-filter"></i>
            Áp dụng
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <!-- Register invoices table -->
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

        <template #cell(form_pattern)="{ item }">
          <code>{{ item.form_pattern || '—' }}</code>
        </template>

        <!-- New column: Ký hiệu (form_code + serial) -->
        <template #cell(form_serial)="{ item }">
          <code>{{ getFormSerial(item) }}</code>
        </template>

        <template #cell(declaration_date)="{ item }">
          {{ formatDate(item.declaration_date) }}
        </template>

        <template #cell(declaration_type)="{ item }">
          <b-badge :variant="item.declaration_type == 2 ? 'warning' : 'info'">
            {{ item.declaration_type == 2 ? 'Thay đổi' : 'ĐK mới' }}
          </b-badge>
        </template>

        <template #cell(invoice_forms)="{ item }">
          <span class="text-muted" v-if="!item.invoice_forms">—</span>
          <span v-else>{{ summarizeInvoiceForms(item.invoice_forms) }}</span>
        </template>

        <template #cell(status)="{ item }">
          <b-badge :variant="statusVariant(item.status)">{{ statusText(item.status) }}</b-badge>
        </template>

        <template #cell(option)="{ item }">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret>
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="openEdit(item)">Cập nhật</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="downloadXml(item)">Download XML</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) === 0" class="text-center" href="#" @click.prevent="onSignature(item)">Ký số</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) === 1" class="text-center" href="#" @click.prevent="sendToTaxAuthority(item)">Gửi CQT</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) > 1" class="text-center" href="#" @click.prevent="showHistory(item)">Lịch sử truyền nhận</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) === 0" class="text-center text-danger" href="#" @click.prevent="deleteItem(item)">Xóa tờ khai</b-dropdown-item>
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

    <!-- History modal -->
    <b-modal ref="historyModal" size="lg" title="Lịch sử truyền nhận" hide-header-close>
      <div>
        <b-table-simple bordered small responsive show-empty :busy="historyBusy" empty-text="Không có dữ liệu">
          <b-thead>
            <b-tr>
              <b-th class="text-center" style="width:60px">STT</b-th>
              <b-th>Tiêu đề</b-th>
              <b-th>Mô tả</b-th>
              <b-th style="width:160px">Người thao tác</b-th>
              <b-th style="width:160px">Ngày thực hiện</b-th>
            </b-tr>
          </b-thead>
          <b-tbody>
            <b-tr v-for="(row, idx) in historyRows" :key="row.id || idx">
              <b-td class="text-center">{{ idx + 1 }}</b-td>
              <b-td>{{ row.title || '—' }}</b-td>
              <b-td>{{ row.description || '—' }}</b-td>
              <b-td>{{ row.username || row.user_id || row.userId || '—' }}</b-td>
              <b-td>{{ formatDateTime(row.created_at || row.createdAt) }}</b-td>
            </b-tr>
            <b-tr v-if="!historyRows || historyRows.length === 0">
              <b-td colspan="5" class="text-center">Không có dữ liệu</b-td>
            </b-tr>
          </b-tbody>
        </b-table-simple>
      </div>
      <template #modal-footer>
        <b-button size="sm" variant="light" @click="$refs.historyModal.hide()">Đóng</b-button>
      </template>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'RegisterInvoiceList',
  data() {
    return {
      isBusy: false,
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
        declarationType: null,
        status: null,
        dateFrom: null,
        dateTo: null
      },
      fields: [
        { key: 'index', label: '#', thStyle: { width: '50px' } },
        { key: 'form_pattern', label: 'Mẫu số', thStyle: { width: '140px' } },
        { key: 'form_serial', label: 'Ký hiệu', thStyle: { width: '160px' } },
        { key: 'declaration_date', label: 'Ngày lập', thStyle: { width: '130px' } },
        { key: 'declaration_type', label: 'Hình thức tờ khai', thStyle: { width: '140px' } },
        { key: 'invoice_forms', label: 'Hình thức hóa đơn' },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '120px' } },
        { key: 'option', label: 'Chức năng', thStyle: { width: '140px' } }
      ],
      declarationTypeOptions: [
        { value: 1, text: 'Đăng ký mới' },
        { value: 2, text: 'Thay đổi' }
      ],
      statusOptions: [
        { value: 0, text: 'Khởi tạo' },
        { value: 1, text: 'Đã ký' },
        { value: 2, text: 'Đã gửi' },
        { value: 3, text: 'Không tiếp nhận' },
        { value: 4, text: 'Đã tiếp nhận' },
       	{ value: 5, text: 'Không chấp nhận' },
        { value: 6, text: 'Đã chấp nhận' },
        { value: 7, text: 'Từ chối / Lỗi' }
      ],
      historyBusy: false,
      historyRows: [],
      historyForId: null,
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    formatDate(d) {
      if (!d) return '—'
      try {
        const dt = new Date(d)
        const yyyy = dt.getFullYear()
        const mm = String(dt.getMonth() + 1).padStart(2, '0')
        const dd = String(dt.getDate()).padStart(2, '0')
        return `${dd}/${mm}/${yyyy}`
      } catch {
        return d
      }
    },
    formatDateTime(d) {
      if (!d) return '—'
      try {
        const dt = new Date(d)
        const yyyy = dt.getFullYear()
        const mm = String(dt.getMonth() + 1).padStart(2, '0')
        const dd = String(dt.getDate()).padStart(2, '0')
        const HH = String(dt.getHours()).padStart(2, '0')
        const MM = String(dt.getMinutes()).padStart(2, '0')
        const SS = String(dt.getSeconds()).padStart(2, '0')
        return `${dd}/${mm}/${yyyy} ${HH}:${MM}:${SS}`
      } catch {
        return String(d)
      }
    },
    summarizeInvoiceForms(json) {
      try {
        let data = typeof json === 'string' ? JSON.parse(json) : json
        if (typeof data === 'string') { try { data = JSON.parse(data) } catch {} }
        if (Array.isArray(data)) {
          return data.map(x => x.name || x.code || x).join(', ')
        }
        if (data && typeof data === 'object') {
          const keys = Object.keys(data)
          return keys.map(k => data[k]?.name || data[k]?.code || k).join(', ')
        }
        return String(data)
      } catch {
        return String(json)
      }
    },
    statusText(s) {
      const m = {
        0: 'Khởi tạo',
        1: 'Đã ký',
        2: 'Đã gửi',
		3: 'Không tiếp nhận',
        4: 'Đã tiếp nhận',
		5: 'Không chấp nhận',
        6: 'Đã chấp nhận',
        7: 'Từ chối / Lỗi'
      }
      return m[Number(s)] || '—'
    },
    statusVariant(s) {
      const n = Number(s)
      if (n === 6) return 'success' // Đã chấp nhận
      if (n === 5 || n === 7) return 'danger' // Không chấp nhận / Từ chối
      if (n === 4) return 'primary' // Đã tiếp nhận
      if (n === 3) return 'info' // Không tiếp nhận
      if (n === 2) return 'warning' // Đã gửi
      if (n === 1) return 'light' // Đã ký
      return 'secondary' // Khởi tạo hoặc khác
    },
    mapItem(raw) {
      // Convert camelCase keys from backend into snake_case expected by the table
      if (!raw || typeof raw !== 'object') return raw
      const m = {
        formPattern: 'form_pattern',
        declarationDate: 'declaration_date',
        declarationType: 'declaration_type',
        invoiceForms: 'invoice_forms',
        invoiceTypes: 'invoice_types',
        sendMethods: 'send_methods',
        transferMethods: 'transfer_methods',
        digitalCertificates: 'digital_certificates',
        responseReceiveFile: 'response_receive_file',
        responseAcceptFile: 'response_accept_file'
      }
      const out = { ...raw }
      Object.keys(m).forEach(k => {
        const target = m[k]
        if (typeof raw[k] !== 'undefined' && raw[k] !== null && typeof out[target] === 'undefined') {
          out[target] = raw[k]
        }
      })
      return out
    },
    normalizePageResponse(raw) {
      const out = { ...this.list }
      // Laravel style
      if (Array.isArray(raw.data)) {
        // Map items
        out.data = raw.data.map(this.mapItem)
        out.total = Number(raw.total || raw.data.length) || 0
        // Pagination links
        out.current_page = Number(raw.current_page) || 1
        out.last_page = Math.ceil(out.total / out.per_page) || 1
        out.from = (out.current_page - 1) * out.per_page + 1
        out.to = Math.min(out.from + out.per_page - 1, out.total) || 0
        return out
      }
      // Fallback: array
      if (Array.isArray(raw)) {
        out.data = raw.map(this.mapItem)
        out.total = raw.length
        out.current_page = 1
        out.last_page = 1
        out.from = 1
        out.to = raw.length
        return out
      }
      // Unknown format
      return out
    },
    async fetchList() {
      this.isBusy = true
      try {
        const params = this.buildQuery()
        const { data } = await axios.get('/register-invoices/list', { params })
        let normalized = this.normalizePageResponse(data)
        // Client-side fallback: enforce declaration type filter if backend did not
        if (this.filters.declarationType != null) {
          const want = Number(this.filters.declarationType)
          const filtered = (normalized.data || []).filter(item => Number((item.declaration_type ?? item.declarationType)) === want)
          // Recompute simple page metrics based on filtered data
          normalized = {
            ...normalized,
            data: filtered,
            total: filtered.length,
            last_page: 1,
            from: filtered.length ? 1 : 0,
            to: filtered.length
          }
        }
        this.list = { ...this.list, ...normalized }
      } catch (e) {
        // leave previous list
      } finally {
        this.isBusy = false
      }
    },
    buildQuery() {
      const q = {
        page: this.list.current_page,
        size: this.list.per_page
      }
      if (this.filters.keyword) q.keyword = this.filters.keyword.trim()
      if (this.filters.declarationType != null) {
        const dt = Number(this.filters.declarationType)
        q.declarationType = dt
        q.declaration_type = dt
      }
      if (this.filters.status != null) q.status = Number(this.filters.status)
      // Date filters: backend expects dateFrom/dateTo in YYYY-MM-DD
      const fmt = (v) => {
        if (!v) return null
        try {
          const d = typeof v === 'string' ? new Date(v) : v
          const yyyy = d.getFullYear()
          const mm = String(d.getMonth() + 1).padStart(2, '0')
          const dd = String(d.getDate()).padStart(2, '0')
          return `${yyyy}-${mm}-${dd}`
        } catch { return null }
      }
      const df = fmt(this.filters.dateFrom)
      const dt2 = fmt(this.filters.dateTo)
      if (df) q.dateFrom = df
      if (dt2) q.dateTo = dt2
      return q
    },
    onPageSizeChange() {
      this.list.current_page = 1
      this.fetchList()
    },
    onPageChange() {
      this.fetchList()
    },
    applyFilters() {
      this.list.current_page = 1
      this.fetchList()
    },
    resetFilters() {
      this.filters = {
        keyword: '',
        declarationType: null,
        status: null,
        dateFrom: null,
        dateTo: null
      }
      this.fetchList()
    },
    openCreate() {
      this.$router.push({ name: 'CustomerRegisterInvoiceCreate' })
    },
    openEdit(item) {
      this.$router.push({ name: 'CustomerRegisterInvoiceEdit', params: { id: item.id } })
    },
    openHistory(item) {
      this.$router.push({ name: 'register-invoice-history', params: { id: item.id } })
    },
    downloadXml(item) {
      try {
        const base = axios.defaults.baseURL || ''
        const url = `${base}/register-invoices/${item.id}/download-xml`
        // Use fetch to get XML and download with a filename
        fetch(url, {
          headers: {
            'Accept': 'application/xml',
            'Authorization': (axios.defaults.headers?.Authorization) || ''
          },
          credentials: 'include'
        }).then(async (res) => {
          if (!res.ok) throw new Error(`HTTP ${res.status}`)
          const text = await res.text()
          const blob = new Blob([text], { type: 'application/xml;charset=utf-8' })
          const link = document.createElement('a')
          const fnCompany = (item.company_name || item.companyName || 'to-khai').replace(/[^a-zA-Z0-9-_]+/g, '_')
          const fnDate = (item.declaration_date || item.declarationDate || '').toString().slice(0, 10)
          const filename = `to-khai-hddt_${fnCompany}_${fnDate || 'unknown'}.xml`
          link.href = URL.createObjectURL(blob)
          link.download = filename
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
          URL.revokeObjectURL(link.href)
        }).catch(() => {
          // Fallback: open in new tab
          window.open(url, '_blank')
        })
      } catch {
        const base = axios.defaults.baseURL || ''
        const url = `${base}/register-invoices/${item.id}/download-xml`
        window.open(url, '_blank')
      }
    },
    async updateStatus(id, status) {
      try {
        await axios.put(`/register-invoices/${id}/get`, { status })
      } catch (e) {
        // ignore; global error handler will show toast
      }
    },
    async deleteItem(item) {
      try {
        const id = item.id || item.ID || item.Id
        if (!id) return
        const ok = await this.$bvModal.msgBoxConfirm(
          `Bạn có chắc muốn xóa tờ khai #${id}?`,
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
        // Backend allows delete; we show delete only when status === 0
        await axios.delete(`/register-invoices/${id}`)
        this.$bvToast && this.$bvToast.toast('Đã xóa tờ khai', { title: 'Thành công', variant: 'success', solid: true, autoHideDelay: 3000 })
        // Refresh list after delete
        this.applyFilters()
      } catch (e) {
        const code = e?.response?.status
        const msg = code === 403 ? 'Không có quyền xóa tờ khai' : code === 404 ? 'Tờ khai không tồn tại' : 'Xóa tờ khai thất bại'
        this.$bvToast && this.$bvToast.toast(msg, { title: 'Lỗi', variant: 'danger', solid: true, autoHideDelay: 4000 })
      }
    },
    async onSignature(item) {
      try {
        const id = item.id || item.ID || item.Id
        if (!id) return
        let ok = true
        if (typeof window.$?.confirm === 'function') {
          ok = await new Promise(resolve => {
            window.$.confirm({
              title: 'Xác nhận ký số',
              content: `Bạn có chắc chắn muốn ký số tờ khai #${id}?`,
              theme: 'bootstrap',
              type: 'blue',
              icon: 'fas fa-signature',
              animation: 'zoom',
              closeAnimation: 'scale',
              boxWidth: '420px',
              useBootstrap: true,
              backgroundDismiss: true,
              escapeKey: 'cancel',
              buttons: {
                cancel: { text: 'Hủy', btnClass: 'btn-light', action: function(){ resolve(false) } },
                ok: { text: 'Đồng ý', btnClass: 'btn-primary', action: function(){ resolve(true) } }
              }
            })
          })
        } else {
          ok = window.confirm(`Xác nhận ký số tờ khai #${id}?`)
        }
        if (!ok) return
        const { data } = await axios.post(`/register-invoices/${id}/sign`, null, { successMessage: 'Đã ký số tờ khai thành công' })
        // Update the row item with returned fields
        const signatureName = data?.signatureInfo || data?.signature_info || null
        const signDate = data?.signDate || data?.sign_date || new Date().toISOString()
        const signedXml = data?.signedXml || data?.signed_xml || null
        const updated = {
          ...item,
          signatureInfo: signatureName,
          signature_info: signatureName,
          signDate: signDate,
          sign_date: signDate,
          signedXml: signedXml,
          signed_xml: signedXml,
          status: 1 // align with create.vue: after signing, set status = 1 to show "Gửi CQT"
        }
        const idx = this.list.data.findIndex(x => (x.id||x.ID||x.Id) === id)
        if (idx >= 0) this.$set(this.list.data, idx, updated)
        // Persist status change to 1
        await this.updateStatus(id, 1)
      } catch (e) {
        // ignore
      }
    },
    async sendToTaxAuthority(item) {
      try {
        const id = item.id || item.ID || item.Id
        if (!id) return
        let ok = true
        if (typeof window.$?.confirm === 'function') {
          ok = await new Promise(resolve => {
            window.$.confirm({
              title: 'Xác nhận gửi tờ khai',
              content: 'Xác nhận gửi tờ khai lên Cơ quan thuế',
              theme: 'bootstrap',
              type: 'blue',
              icon: 'fas fa-paper-plane',
              animation: 'zoom',
              closeAnimation: 'scale',
              boxWidth: '420px',
              useBootstrap: true,
              backgroundDismiss: true,
              escapeKey: 'cancel',
              buttons: {
                cancel: { text: 'Hủy', btnClass: 'btn-light', action: function(){ resolve(false) } },
                ok: { text: 'Gửi', btnClass: 'btn-primary', action: function(){ resolve(true) } }
              }
            })
          })
        } else {
          ok = window.confirm('Xác nhận gửi tờ khai lên Cơ quan thuế?')
        }
        if (!ok) return
        this.isBusy = true
        await axios.post(`/register-invoices/${id}/send`, null, { successMessage: 'Đã gửi tờ khai lên Cơ quan thuế' })
        // Align with create.vue: do not force a local status update; refresh row from backend
        await this.refreshRow(id)
        // Start polling for async tax responses with user notifications
        this.pollTaxStatusForRow(id)
      } catch (e) {
        // Error toast handled globally by axios plugin
      } finally {
        this.isBusy = false
      }
    },
    async refreshRow(id) {
      try {
        const { data } = await axios.get(`/register-invoices/${id}`)
        // Map camelCase to snake_case for the table
        const mapped = this.mapItem(data)
        const idx = this.list.data.findIndex(x => (x.id||x.ID||x.Id) === id)
        if (idx >= 0) this.$set(this.list.data, idx, { ...this.list.data[idx], ...mapped })
      } catch (e) {
        // ignore refresh errors
      }
    },
    pollTaxStatusForRow(id) {
      let tries = 0
      let notifiedReceive = false
      let notifiedAccept = false
      const timer = setInterval(async () => {
        try {
          tries++
          const { data } = await axios.get(`/register-invoices/${id}`)
          const status = Number(data?.status)
          // Notify receive stage (102)
          if (!notifiedReceive && (status === 3 || status === 4)) {
            notifiedReceive = true
            if (status === 4) {
              this.$bvToast && this.$bvToast.toast('Cơ quan thuế đã tiếp nhận tờ khai', { title: 'Thông báo', variant: 'success', solid: true, autoHideDelay: 4000 })
            } else {
              this.$bvToast && this.$bvToast.toast('Cơ quan thuế không tiếp nhận tờ khai', { title: 'Thông báo', variant: 'warning', solid: true, autoHideDelay: 4000 })
            }
          }
          // Notify accept stage (103)
          if (!notifiedAccept && (status === 5 || status === 6)) {
            notifiedAccept = true
            if (status === 6) {
              this.$bvToast && this.$bvToast.toast('Cơ quan thuế đã chấp nhận tờ khai', { title: 'Thông báo', variant: 'success', solid: true, autoHideDelay: 4000 })
            } else {
              this.$bvToast && this.$bvToast.toast('Cơ quan thuế không chấp nhận tờ khai', { title: 'Thông báo', variant: 'danger', solid: true, autoHideDelay: 4000 })
            }
          }
          // Update row in list with latest data
          this.refreshRow(id)
          // Stop when done or timeout
          if ((notifiedReceive && notifiedAccept) || tries >= 10) {
            clearInterval(timer)
          }
        } catch (err) {
          if (tries >= 10) clearInterval(timer)
        }
      }, 2000)
    },
    reload() {
      this.fetchList()
    },
    async showHistory(item) {
      try {
        const id = item.id || item.ID || item.Id
        if (!id) return
        this.historyBusy = true
        this.historyRows = []
        this.historyForId = id
        const { data } = await axios.get(`/register-invoices/${id}/history`)
        // Normalize keys to snake_case for view
        this.historyRows = Array.isArray(data) ? data.map(r => ({
          id: r.id,
          title: r.title,
          description: r.description,
          username: r.username || r.userName || r.user_name || null,
          user_id: r.userId || r.user_id || null,
          created_at: r.createdAt || r.created_at || null,
        })) : []
        this.$refs.historyModal.show()
      } catch (e) {
        this.historyRows = []
        this.$refs.historyModal && this.$refs.historyModal.show()
      } finally {
        this.historyBusy = false
      }
    },
    getFormSerial(item) {
      // Accept item.form_invoices or item.invoice_forms; can be array/object/stringified JSON
      const joinSerial = (arr) => {
        try {
          return arr
            .map(x => {
              const o = typeof x === 'string' ? JSON.parse(x) : x
              const code = o?.form_code || o?.formCode || o?.code || ''
              const serial = o?.serial || o?.form_serial || o?.symbol || ''
              const combined = `${(code || '').toString()}${(serial || '').toString()}`
              return combined || null
            })
            .filter(Boolean)
            .join(', ')
        } catch {
          return '—'
        }
      }
      try {
        let v = item?.form_invoices ?? item?.invoice_forms
        if (!v) return '—'
        if (typeof v === 'string') {
          try { v = JSON.parse(v) } catch { /* keep as string */ }
        }
        if (Array.isArray(v)) {
          return joinSerial(v) || '—'
        }
        if (typeof v === 'object') {
          const values = Object.values(v)
          return joinSerial(values) || '—'
        }
        return '—'
      } catch {
        return '—'
      }
    },
  }
}
</script>

<style scoped>
.register-invoices {
  max-width: 1200px;
  margin: 0 auto;
}
</style>