<template>
  <div class="container-fluid py-3 register-invoices">
    <!-- Tiêu đề và thao tác -->
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

    <!-- Bộ lọc -->
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

    <!-- Bảng tờ khai hóa đơn -->
    <b-card class="shadow-sm">
      <b-table
        class="register-invoices-table"
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
          <code class="text-wrap-anywhere">{{ item.form_pattern || '—' }}</code>
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
          <span v-else class="text-wrap-anywhere">{{ summarizeInvoiceForms(item.invoice_forms) }}</span>
        </template>

        <template #cell(status)="{ item }">
          <b-badge :variant="statusVariant(item.status)">{{ statusText(item.status) }}</b-badge>
        </template>

        <template #cell(option)="{ item }">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
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

    <!-- Hộp thoại lịch sử -->
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
import PaginationBar from '@/views/components/pagination_bar.vue'

export default {
  name: 'RegisterInvoiceList',
  components: { PaginationBar },
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
        { key: 'index', label: '#', thStyle: { width: '4%' } },
        { key: 'form_pattern', label: 'Mẫu số', thStyle: { width: '13%' } },
        { key: 'declaration_date', label: 'Ngày lập', thStyle: { width: '11%' } },
        { key: 'declaration_type', label: 'Hình thức tờ khai', thStyle: { width: '13%' } },
        { key: 'invoice_forms', label: 'Hình thức hóa đơn', thStyle: { width: '37%' } },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '10%' } },
        { key: 'option', label: 'Chức năng', thStyle: { width: '12%' } }
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
      // Chuyển key camelCase từ backend sang snake_case theo bảng
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
      // Định dạng phân trang tương tự hệ cũ
      if (Array.isArray(raw.data)) {
        // Ánh xạ danh sách item
        out.data = raw.data.map(this.mapItem)
        out.total = Number(raw.total || raw.data.length) || 0
        // Pagination links
        out.current_page = Number(raw.current_page) || 1
        out.last_page = Math.ceil(out.total / out.per_page) || 1
        out.from = (out.current_page - 1) * out.per_page + 1
        out.to = Math.min(out.from + out.per_page - 1, out.total) || 0
        return out
      }
      // Dự phòng: mảng dữ liệu
      if (Array.isArray(raw)) {
        out.data = raw.map(this.mapItem)
        out.total = raw.length
        out.current_page = 1
        out.last_page = 1
        out.from = 1
        out.to = raw.length
        return out
      }
      // Định dạng không xác định
      return out
    },
    async fetchList() {
      this.isBusy = true
      try {
        const params = this.buildQuery()
        const { data } = await axios.get('/register-invoices/list', { params })
        let normalized = this.normalizePageResponse(data)
        // Dự phòng phía client: lọc loại tờ khai nếu backend chưa lọc
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
    onPageSizeChange(size) {
      this.list.per_page = Number(size) || this.list.per_page
      this.list.current_page = 1
      this.fetchList()
    },
    onPageChange(page) {
      this.list.current_page = Number(page) || 1
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
        // Dùng fetch để lấy XML và tải xuống với tên file
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
          // Dự phòng: mở trong tab mới
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
        // b? qua; global error handler will show toast
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
        // Phía backend cho phép xóa; giao diện chỉ hiện xóa khi status === 0
        await axios.delete(`/register-invoices/${id}`)
        this.$bvToast && this.$bvToast.toast('Đã xóa tờ khai', { title: 'Thành công', variant: 'success', solid: true, autoHideDelay: 3000 })
        // Tải lại danh sách sau khi xóa
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
        // Cập nhật the row item with returned fields
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
        // b? qua
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
        // Đồng bộ với create.vue: không ép cập nhật status cục bộ, nạp lại dòng từ backend
        await this.refreshRow(id)
        // Bắt đầu polling phản hồi thuế bất đồng bộ kèm thông báo người dùng
        this.pollTaxStatusForRow(id)
      } catch (e) {
        // Toast lỗi đã được axios plugin xử lý toàn cục
      } finally {
        this.isBusy = false
      }
    },
    async refreshRow(id) {
      try {
        const { data } = await axios.get(`/register-invoices/${id}`)
        // Chuyển camelCase sang snake_case cho bảng
        const mapped = this.mapItem(data)
        const idx = this.list.data.findIndex(x => (x.id||x.ID||x.Id) === id)
        if (idx >= 0) this.$set(this.list.data, idx, { ...this.list.data[idx], ...mapped })
      } catch (e) {
        // b? qua refresh errors
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
          // Thông báo giai đoạn tiếp nhận (102)
          if (!notifiedReceive && (status === 3 || status === 4)) {
            notifiedReceive = true
            if (status === 4) {
              this.$bvToast && this.$bvToast.toast('Cơ quan thuế đã tiếp nhận tờ khai', { title: 'Thông báo', variant: 'success', solid: true, autoHideDelay: 4000 })
            } else {
              this.$bvToast && this.$bvToast.toast('Cơ quan thuế không tiếp nhận tờ khai', { title: 'Thông báo', variant: 'warning', solid: true, autoHideDelay: 4000 })
            }
          }
          // Thông báo giai đoạn chấp nhận (103)
          if (!notifiedAccept && (status === 5 || status === 6)) {
            notifiedAccept = true
            if (status === 6) {
              this.$bvToast && this.$bvToast.toast('Cơ quan thuế đã chấp nhận tờ khai', { title: 'Thông báo', variant: 'success', solid: true, autoHideDelay: 4000 })
            } else {
              this.$bvToast && this.$bvToast.toast('Cơ quan thuế không chấp nhận tờ khai', { title: 'Thông báo', variant: 'danger', solid: true, autoHideDelay: 4000 })
            }
          }
          // Cập nhật row in list with latest data
          this.refreshRow(id)
          // Dừng khi hoàn tất hoặc hết thời gian chờ
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
        // Chuẩn hóa key sang snake_case cho phần hiển thị
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
    }
  }
}
</script>

<style scoped>
.register-invoices {
  width: 100%;
  max-width: none;
}

.register-invoices::v-deep .table-responsive {
  overflow-x: hidden;
}

.register-invoices::v-deep .register-invoices-table {
  width: 100%;
  table-layout: fixed;
}

.register-invoices::v-deep .register-invoices-table th,
.register-invoices::v-deep .register-invoices-table td {
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
  vertical-align: middle;
}

.text-wrap-anywhere {
  overflow-wrap: anywhere;
  word-break: break-word;
}
</style>
