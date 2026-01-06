<template>
  <div class="container-fluid py-3 vat-invoices">
    <!-- Header and actions -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center">
        <h4 class="mb-0 font-weight-bold">Danh sách hóa đơn giá trị gia tăng</h4>
      </div>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
        <b-button size="sm" variant="success" @click="goCreate">
          <i class="fas fa-file-invoice"></i>
          Lập hóa đơn
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
            <b-form-input v-model.trim="filters.keyword" placeholder="Tìm theo mã tra cứu / khách hàng / ký hiệu" @keyup.enter="applyFilters" />
          </b-input-group>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-select v-model="filters.status" :options="statusOptions">
            <template #first>
              <b-form-select-option :value="null">Tất cả trạng thái</b-form-select-option>
            </template>
          </b-form-select>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-datepicker
            v-model="filters.date"
            placeholder="Lọc theo ngày lập"
            :date-format-options="dateFormatOptions"
            locale="vi"
          />
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

    <!-- Invoices table -->
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
        :key="refreshKey"
        empty-text="Không có dữ liệu"
      >
        <template #cell(index)="{ index }">
          {{ index + 1 + (list.current_page - 1) * list.per_page }}
        </template>

        <template #cell(form_serial)="{ item }">
          <code>{{ (item.formCode || '') + (item.serial || '') }}</code>
        </template>

        <template #cell(no)="{ item }">
          {{ item.no || '—' }}
        </template>

        <template #cell(date_export)="{ item }">
          {{ formatDate(item.dateExport) }}
        </template>

        <template #cell(lookup_code)="{ item }">
          <code>{{ item.lookupCode || '—' }}</code>
        </template>

        <template #cell(customer_or_cqt)="{ item }">
          <div>
            <div>{{ item.customerName || '—' }}</div>
            <small class="text-muted">Mã CQT: {{ item.codeCqt ? 'Đã cấp mã' : 'Chưa cấp mã' }}</small>
          </div>
        </template>

        <template #cell(amount)="{ item }">
          {{ formatCurrency(item.amount) }}
        </template>

        <template #cell(username)="{ item }">
          {{ item.username || usernameOf(item.userId) }}
        </template>

        <template #cell(status)="{ item }">
          <b-badge :variant="statusVariant(item.status)">{{ statusText(item.status) }}</b-badge>
        </template>

        <template #cell(option)="{ item }">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret>
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="goEdit(item)">Cập nhật</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="cloneInvoice(item)">Sao chép</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="viewInvoice(item)">Xem</b-dropdown-item>

            <b-dropdown-item v-if="Number(item.status) === 0" class="text-center" href="#" @click.prevent="signInvoice(item)">Ký số</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) === 1" class="text-center" href="#" @click.prevent="sendToCqt(item)">Gửi CQT</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) === 3" class="text-center" href="#" @click.prevent="sendMail(item)">Gửi mail</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) > 1" class="text-center" href="#" @click.prevent="viewHistory(item)">Lịch sử truyền nhận</b-dropdown-item>

            <b-dropdown-item v-if="Number(item.status) === 0" class="text-center text-danger" href="#" @click.prevent="deleteItem(item)">Xóa hóa đơn</b-dropdown-item>
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

    <!-- Modal: View invoice (HTML via iframe) -->
    <b-modal
      id="modalVatInvoice"
      size="lg"
      :no-close-on-esc="false"
      :hide-header="true"
      body-class="p-0"
    >
      <iframe
        id="viewVatInv"
        :src="iframe.src"
        scrolling="no"
        frameborder="0"
        width="100%"
        onload="((obj) => {try{obj.style.height = obj.contentWindow.document.body.scrollHeight + 'px';}catch(e){obj.style.height = 0;}})(this)"
      ></iframe>
      <template #modal-footer>
        <div class="d-flex align-items-center w-100 justify-content-between">
          <div>
            <b-button variant="light" size="sm" @click="closeModal('modalVatInvoice')">Đóng</b-button>
          </div>
          <div class="d-flex align-items-center">
            <b-dropdown
              id="ddown-right-vat"
              text="Tải mẫu"
              extra-toggle-classes="nav-link-custom"
              variant="success"
              size="sm"
              class="mr-2"
            >
              <b-dropdown-item :href="'/v1/invoices/' + (iframe.lookup_code || '') + '/download-pdf'">
                <i class="fas fa-file-pdf"></i> Download PDF
              </b-dropdown-item>
              <b-dropdown-item :href="'/v1/invoices/' + (iframe.lookup_code || '') + '/download-xml'">
                <i class="fas fa-file-code"></i> Download XML
              </b-dropdown-item>
            </b-dropdown>
            
            <b-button variant="info" size="sm" @click="print(iframe.lookup_code)">
              <i class="fas fa-print"></i> In hóa đơn
            </b-button>
          </div>
        </div>
      </template>
    </b-modal>

    <!-- Modal: History -->
    <b-modal ref="historyModal" size="lg" title="Lịch sử truyền nhận" hide-header-close>
      <div>
        <b-table-simple bordered small responsive show-empty :busy="historyBusy" empty-text="Không có dữ liệu">
          <b-thead>
            <b-tr>
              <b-th class="text-center" style="width:60px">STT</b-th>
              <b-th style="width:150px">Tiêu đề</b-th>
              <b-th>Mô tả</b-th>
              <b-th style="width:100px">Người thao tác</b-th>
              <b-th style="width:110px">Ngày thực hiện</b-th>
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

    <!-- Modal: Send email notice -->
    <b-modal
      id="modalSendEmail"
      size="md"
      :no-close-on-esc="false"
      :hide-header="false"
      :busy="mail.loading"
      title="Gửi thông báo phát hành hóa đơn"
    >
      <div v-if="mail.invoice">
        <b-alert show variant="light" class="py-2">
          <div class="d-flex flex-column small">
            <div><b>Ký hiệu:</b> <code>{{ mail.invoice.formSerial || '—' }}</code></div>
            <div><b>Số hóa đơn:</b> {{ mail.invoice.no || '—' }}</div>
            <div><b>Ngày lập:</b> {{ formatDate(mail.invoice.dateExport) }}</div>
            <div><b>Tổng tiền:</b> {{ formatCurrency(mail.invoice.amount) }}</div>
          </div>
        </b-alert>
      </div>

      <b-form @submit.prevent="confirmSendEmail">
        <b-form-group label="Tên người nhận" label-for="mailName" :state="mail.errors.name === null">
          <b-form-input
            id="mailName"
            v-model.trim="mail.name"
            :state="mail.errors.name === null"
            placeholder="Nhập tên người nhận"
            autocomplete="name"
          />
          <b-form-invalid-feedback v-if="mail.errors.name">{{ mail.errors.name }}</b-form-invalid-feedback>
        </b-form-group>

        <b-form-group label="Email người nhận" label-for="mailEmail" :state="mail.errors.email === null">
          <b-form-input
            id="mailEmail"
            v-model.trim="mail.email"
            :state="mail.errors.email === null"
            placeholder="example@email.com"
            type="email"
            autocomplete="email"
          />
          <b-form-invalid-feedback v-if="mail.errors.email">{{ mail.errors.email }}</b-form-invalid-feedback>
        </b-form-group>
      </b-form>

      <template #modal-footer>
        <div class="d-flex align-items-center w-100 justify-content-between">
          <div>
            <b-button variant="light" size="sm" @click="closeModal('modalSendEmail')" :disabled="mail.loading">Đóng</b-button>
          </div>
          <div>
            <b-button variant="primary" size="sm" @click="confirmSendEmail" :disabled="mail.loading">
              <b-spinner small v-if="mail.loading" class="mr-2"/>
              Gửi
            </b-button>
          </div>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import { toastSuccess, toastWarning } from '@/utils/toast'

export default {
  name: 'VatInvoiceList',
  data () {
    return {
      isBusy: false,
      usersMap: {},
      refreshKey: 0,
      // iframe state for viewing invoice
      iframe: {
        src: null,
        lookup_code: null,
        status: null,
      },
      // send email modal state
      mail: {
        id: null,
        invoice: { formSerial: '', no: null, dateExport: null, amount: 0 },
        name: '',
        email: '',
        errors: { name: null, email: null },
        loading: false,
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
        status: null,
        date: null,
      },
      // Hiển thị dd/mm/YYYY trong datepicker
      dateFormatOptions: { year: 'numeric', month: '2-digit', day: '2-digit' },
      fields: [
        { key: 'index', label: 'STT', thStyle: { width: '60px' } },
        { key: 'form_serial', label: 'Ký hiệu', thStyle: { width: '140px' } },
        { key: 'no', label: 'Số', thStyle: { width: '100px' } },
        { key: 'date_export', label: 'Ngày lập', thStyle: { width: '140px' } },
        { key: 'lookup_code', label: 'Mã tra cứu', thStyle: { width: '100px' } },
        { key: 'customer_or_cqt', label: 'Khách hàng / Mã CQT', thStyle: { width: '200px' } },
        { key: 'amount', label: 'Tổng tiền', thStyle: { width: '140px' }, tdClass: 'text-right' },
        { key: 'username', label: 'Người phát hành', thStyle: { width: '160px' } },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '160px' } },
        { key: 'option', label: 'Chức năng', thStyle: { width: '180px' } },
      ],
      statusOptions: [
        { value: 0, text: 'Mới khởi tạo' },
        { value: 1, text: 'Đã ký' },
        { value: 2, text: 'Đã gửi thuế' },
        { value: 3, text: 'Đã phát hành' },
        { value: 4, text: 'Bị thay thế' },
        { value: 5, text: 'Bị điều chỉnh' },
        { value: 6, text: 'Đã hủy' },
        { value: 7, text: 'Không đủ điều kiện' },
      ],
      // Poll trackers to cancel on component destroy
      _pollTimers: {},
      _pollAttempts: {},
      // History modal state
      historyBusy: false,
      historyRows: [],
      historyForId: null,
    }
  },
  created () { this.fetchList() },
  beforeDestroy () {
    // Clear all pending poll timers
    try {
      Object.values(this._pollTimers || {}).forEach(t => clearTimeout(t))
      this._pollTimers = {}
      this._pollAttempts = {}
    } catch {}
  },
  methods: {
    closeModal(id) { this.$root.$emit('bv::hide::modal', id) },

    // Chuyển datepicker value về yyyy-MM-dd để backend parse LocalDate ổn định
    formatDateParam (d) {
      if (!d) return null
      try {
        // bootstrap-vue datepicker thường trả về 'YYYY-MM-DD' (string) hoặc Date
        if (typeof d === 'string') {
          // Nếu đã đúng format thì trả luôn
          if (/^\d{4}-\d{2}-\d{2}$/.test(d)) return d
          const dt = new Date(d)
          if (!isNaN(dt.getTime())) {
            const yyyy = dt.getFullYear()
            const mm = String(dt.getMonth() + 1).padStart(2, '0')
            const dd = String(dt.getDate()).padStart(2, '0')
            return `${yyyy}-${mm}-${dd}`
          }
          return null
        }
        if (d instanceof Date) {
          const yyyy = d.getFullYear()
          const mm = String(d.getMonth() + 1).padStart(2, '0')
          const dd = String(d.getDate()).padStart(2, '0')
          return `${yyyy}-${mm}-${dd}`
        }
        // fallback: attempt to new Date
        const dt = new Date(d)
        if (isNaN(dt.getTime())) return null
        const yyyy = dt.getFullYear()
        const mm = String(dt.getMonth() + 1).padStart(2, '0')
        const dd = String(dt.getDate()).padStart(2, '0')
        return `${yyyy}-${mm}-${dd}`
      } catch {
        return null
      }
    },

    async fetchList () {
      this.isBusy = true
      try {
        const params = {
          page: this.list.current_page,
          size: this.list.per_page,
          q: this.filters.keyword || undefined,
          status: this.filters.status != null ? Number(this.filters.status) : undefined,
          // gửi về YYYY-MM-DD để backend parse ổn định
          date: this.formatDateParam(this.filters.date) || undefined,
        }
        const { data } = await axios.get('/invoices', { params })
        const items = (data.items || data.data || []).map(it => ({
          id: it.id,
          formCode: it.formCode,
          serial: it.serial,
          no: it.no,
          dateExport: it.dateExport,
          lookupCode: it.lookupCode,
          customerName: it.customerName,
          codeCqt: it.codeCqt,
          amount: it.amount,
          userId: it.userId,
          username: it.username,
          status: it.status,
        }))
        this.list.data = items
        this.list.total = data.total || 0
        this.list.per_page = data.per_page || this.list.per_page
        this.list.current_page = data.current_page || this.list.current_page
        this.list.last_page = data.last_page || 1
        const from = (this.list.current_page - 1) * this.list.per_page + (items.length ? 1 : 0)
        const to = from + (items.length ? (items.length - 1) : 0)
        this.list.from = from
        this.list.to = to
        // bump key to force table refresh
        this.refreshKey++
      } catch (e) {
        // silent
      } finally {
        this.isBusy = false
      }
    },

    reload () { this.fetchList() },
    goCreate () { this.$router.push({ name: 'CustomerVatInvoiceCreate' }) },
    goEdit (item) { this.$router.push({ name: 'CustomerVatInvoiceEdit', params: { id: item.id } }) },

    onPageSizeChange () { this.list.current_page = 1; this.fetchList() },
    onPageChange () { this.fetchList() },
    applyFilters () { this.list.current_page = 1; this.fetchList() },
    resetFilters () { this.filters = { keyword: '', status: null, date: null }; this.applyFilters() },

    statusText (s) {
      const v = Number(s)
      switch (v) {
        case 0: return 'Mới khởi tạo'
        case 1: return 'Đã ký'
        case 2: return 'Đã gửi thuế'
        case 3: return 'Đã phát hành'
        case 4: return 'Bị thay thế'
        case 5: return 'Bị điều chỉnh'
        case 6: return 'Đã hủy'
        case 7: return 'Không đủ điều kiện'
        default: return '—'
      }
    },
    statusVariant (s) {
      const v = Number(s)
      if (v === 0) return 'secondary'
      if (v === 1) return 'info'
      if (v === 2) return 'warning'
      if (v === 3) return 'success'
      if (v === 4) return 'dark'
      if (v === 5) return 'primary'
      if (v === 6) return 'danger'
      return 'secondary'
    },
    usernameOf (uid) { return this.usersMap[uid] || '—' },
    formatDate (dt) { try { return (dt || '').toString().replace('T', ' ') } catch { return '—' } },
    formatCurrency (v) {
      try { const n = Number(v || 0); return n.toLocaleString('vi-VN') } catch { return '0' }
    },

    async cloneInvoice (item) {
      if (!item || !item.id) return
      this.isBusy = true
      try {
        const { data } = await axios.post(`/invoices/${item.id}/clone`)
        const newId = data && (data.id || data.data && data.data.id)
        await this.fetchList()
        if (newId) {
          this.$bvToast && this.$bvToast.toast('Đã sao chép hóa đơn', { title: 'Thành công', variant: 'success', solid: true, autoHideDelay: 2000 })
          this.$router.push({ name: 'CustomerVatInvoiceEdit', params: { id: newId } })
        } else {
          this.$bvToast && this.$bvToast.toast('Không lấy được ID hóa đơn mới', { title: 'Cảnh báo', variant: 'warning', solid: true, autoHideDelay: 3000 })
        }
      } catch (e) {
        const msg = (e && e.response && e.response.data && e.response.data.message) || 'Không thể sao chép hóa đơn'
        this.$bvToast && this.$bvToast.toast(msg, { title: 'Lỗi', variant: 'danger', solid: true, autoHideDelay: 4000 })
      } finally { this.isBusy = false }
    },
    viewInvoice (item) {
      const lookupCode = item.lookupCode || item.orderCode || item.lookup_code || null
      if (!lookupCode) return false
      this.iframe.lookup_code = lookupCode
      this.iframe.src = `/v1/invoices/${lookupCode}/view`
      this.$root.$emit('bv::show::modal', 'modalVatInvoice')
      return false
    },
    downloadXml (item) {
      const lookupCode = item?.lookupCode || item?.orderCode || item?.lookup_code
      if (!lookupCode) {
        this.$bvToast && this.$bvToast.toast('Không có mã tra cứu hóa đơn', { title: 'Lỗi', variant: 'danger', solid: true, autoHideDelay: 3000 })
        return
      }
      // axios plugin typically has baseURL = '/v1'
      const base = axios.defaults?.baseURL || ''
      window.open(`${base}/invoices/${lookupCode}/download-xml`, '_blank')
    },
    downloadPdf (item) {
      const lookupCode = item?.lookupCode || item?.orderCode || item?.lookup_code
      if (!lookupCode) {
        this.$bvToast && this.$bvToast.toast('Không có mã tra cứu hóa đơn', { title: 'Lỗi', variant: 'danger', solid: true, autoHideDelay: 3000 })
        return
      }
      const base = axios.defaults?.baseURL || ''
      window.open(`${base}/invoices/${lookupCode}/download-pdf`, '_blank')
    },

    async signInvoice (item) {
      try {
        const id = item && (item.id || item.ID || item.Id)
        if (!id) return
        let ok = true
        if (typeof window.$?.confirm === 'function') {
          ok = await new Promise(resolve => {
            window.$.confirm({
              title: 'Xác nhận ký số',
              content: `Bạn có chắc chắn muốn ký số hóa đơn #${id}?`,
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
          ok = window.confirm(`Xác nhận ký số hóa đơn #${id}?`)
        }
        if (!ok) return
        this.isBusy = true
        const { data } = await axios.post(`/invoices/${id}/sign`, null, { successMessage: 'Đã ký số hóa đơn thành công' })
        // Update local row fields: no and status
        const newNo = data?.no ?? data?.No ?? null
        const updated = { ...item, no: newNo, status: 1 }
        const idx = this.list.data.findIndex(x => (x.id||x.ID||x.Id) === id)
        if (idx >= 0) this.$set(this.list.data, idx, updated)
        // Refresh list to reflect latest data
        await this.fetchList()
      } catch (e) {
        // Global axios plugin should show error toast
      } finally {
        this.isBusy = false
      }
    },
    async sendToCqt (item) {
      try {
        const id = item && (item.id || item.ID || item.Id)
        if (!id) return
        let ok = true
        if (typeof window.$?.confirm === 'function') {
          ok = await new Promise(resolve => {
            window.$.confirm({
              title: 'Xác nhận gửi hóa đơn',
              content: 'Xác nhận gửi hóa đơn lên Cơ quan thuế',
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
          ok = window.confirm('Xác nhận gửi hóa đơn lên Cơ quan thuế?')
        }
        if (!ok) return
        this.isBusy = true
        await axios.post(`/invoices/${id}/send-to-cqt`, null, { successMessage: 'Đã gửi hóa đơn lên Cơ quan thuế' })
        await this.fetchList()
        // Poll history for async CQT response and show toast with history title
        this.pollHistoryAndNotify(id)
      } catch (e) {
        // handled globally by axios interceptor
      } finally {
        this.isBusy = false
      }
    },

    // Send email: open modal and prefill name/email from invoice customer
    async sendMail (item) {
      try {
        const id = item && (item.id || item.ID || item.Id)
        if (!id) return
        // Prefill invoice info from list item
        const formSerial = `${item.formCode || ''}${item.serial || ''}`
        this.mail = {
          id,
          invoice: { formSerial, no: item.no || null, dateExport: item.dateExport || null, amount: item.amount || 0 },
          name: '',
          email: '',
          errors: { name: null, email: null },
          loading: false,
        }
        // Load customer details from invoice detail API
        try {
          const { data } = await axios.get(`/invoices/${id}`, { meta: { suppressGlobalErrorToast: true } })
          const c = data && data.customer ? data.customer : null
          let recvName = ''
          let recvEmail = ''
          if (c && typeof c === 'object') {
            recvName = c.name || c.buyer || ''
            recvEmail = c.email || ''
          }
          this.mail.name = (recvName || '').toString()
          this.mail.email = (recvEmail || '').toString()
        } catch (e) {
          // ignore, keep defaults
        }
        this.$root.$emit('bv::show::modal', 'modalSendEmail')
      } catch (e) {
        // noop
      }
    },
    async confirmSendEmail () {
      // Validate
      this.mail.errors = { name: null, email: null }
      const name = (this.mail.name || '').trim()
      const email = (this.mail.email || '').trim()
      if (!name) this.mail.errors.name = 'Vui lòng nhập tên người nhận'
      if (!email) this.mail.errors.email = 'Vui lòng nhập email người nhận'
      else if (!this.isValidEmail(email)) this.mail.errors.email = 'Email không đúng định dạng'
      if (this.mail.errors.name || this.mail.errors.email) {
        toastWarning('Vui lòng nhập đầy đủ thông tin hợp lệ')
        return
      }
      // Submit
      try {
        this.mail.loading = true
        await axios.post(`/invoices/${this.mail.id}/send-email`, { name, email }, { successMessage: 'Đã gửi email thông báo phát hành hóa đơn' })
        this.mail.loading = false
        this.closeModal('modalSendEmail')
      } catch (e) {
        this.mail.loading = false
        // error toasts handled globally
      }
    },
    isValidEmail (email) {
      try { return /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(String(email || '').trim()) } catch { return false }
    },

    // Update local list item status by invoiceId
    _updateLocalStatus (invoiceId, status, extras = {}) {
      try {
        const id = Number(invoiceId)
        const idx = this.list.data.findIndex(x => Number(x.id) === id)
        if (idx >= 0) {
          const current = this.list.data[idx]
          // Preserve existing fields, update status and any extras (e.g., codeCqt)
          const updated = { ...current, status: Number(status), ...extras }
          this.$set(this.list.data, idx, updated)
          // bump key to force table refresh
          this.refreshKey++
        }
      } catch {}
    },

    // --- Polling helpers to notify after history saved by backend ---
    async pollHistoryAndNotify (invoiceId) {
      if (!invoiceId && invoiceId !== 0) return
      if (!this._pollTimers || typeof this._pollTimers !== 'object') this._pollTimers = {}
      if (!this._pollAttempts || typeof this._pollAttempts !== 'object') this._pollAttempts = {}
      const key = String(invoiceId)
      try { clearTimeout(this._pollTimers[key]) } catch {}
      this._pollAttempts[key] = 0
      const maxAttempts = 30
      const intervalMs = 1500
      const check = async () => {
        try {
          const { data: history = [] } = await axios.get(`/invoices/${invoiceId}/history`, { meta: { suppressGlobalErrorToast: true } })
          let outcome = null
          for (const h of Array.isArray(history) ? history : []) {
            if (h && (h.type === 202 || h.type === 204)) { outcome = h; break }
          }
          if (outcome) {
            const type = Number(outcome.type)
            const title = outcome.title || 'Kết quả từ CQT'
            const desc = outcome.description || ''
            if (type === 202) {
              // Accepted cấp mã => status 3 and codeCqt may be present
              toastSuccess(desc ? `${title}: ${desc}` : title)
              const code = desc || null
              this._updateLocalStatus(invoiceId, 3, code ? { codeCqt: code } : {})
            } else if (type === 204) {
              const xml = String(outcome.xmlData || '')
              const m = xml.match(/<LTBao>\s*(\d+)\s*<\/LTBao>/i)
              const lt = m ? m[1] : null
              if (lt === '2') {
                // Accepted for 203 => status 3
                toastSuccess(title)
                this._updateLocalStatus(invoiceId, 3)
              } else {
                // Rejected => status 7
                toastWarning(title)
                this._updateLocalStatus(invoiceId, 7)
              }
            }
            // Sync with backend to get any other fields updated
            await this.fetchList()
            try { clearTimeout(this._pollTimers[key]) } catch {}
            delete this._pollTimers[key]
            delete this._pollAttempts[key]
            return
          }
        } catch (e) {}
        const attempts = (this._pollAttempts[key] || 0) + 1
        this._pollAttempts[key] = attempts
        if (attempts >= maxAttempts) {
          delete this._pollTimers[key]
          delete this._pollAttempts[key]
          return
        }
        this._pollTimers[key] = setTimeout(check, intervalMs)
      }
      this._pollTimers[key] = setTimeout(check, 800)
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

    async deleteItem(item) {
      try {
        const id = item.id || item.ID || item.Id
        if (!id) return
        
        // Check status - only allow delete if status = 0
        if (Number(item.status) !== 0) {
          this.$bvToast && this.$bvToast.toast('Chỉ có thể xóa hóa đơn ở trạng thái "Mới khởi tạo"', {
            title: 'Thông báo',
            variant: 'warning',
            solid: true,
            autoHideDelay: 3000
          })
          return
        }

        const ok = await this.$bvModal.msgBoxConfirm(
          `Bạn có chắc muốn xóa hóa đơn #${id}?`,
          {
            title: 'Xác nhận xóa',
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

        await axios.delete(`/invoices/${id}`)
        this.$bvToast && this.$bvToast.toast('Đã xóa hóa đơn thành công', {
          title: 'Thành công',
          variant: 'success',
          solid: true,
          autoHideDelay: 3000
        })
        // Refresh list after delete
        this.fetchList()
      } catch (e) {
        const code = e?.response?.status
        const msg = code === 403
          ? 'Không có quyền xóa hóa đơn'
          : code === 404
          ? 'Hóa đơn không tồn tại'
          : code === 400
          ? 'Không thể xóa hóa đơn này'
          : 'Xóa hóa đơn thất bại'
        this.$bvToast && this.$bvToast.toast(msg, {
          title: 'Lỗi',
          variant: 'danger',
          solid: true,
          autoHideDelay: 4000
        })
      }
    },

    async viewHistory(item) {
      try {
        const id = item.id || item.ID || item.Id
        if (!id) return
        this.historyBusy = true
        this.historyRows = []
        this.historyForId = id
        const { data } = await axios.get(`/invoices/${id}/history`)
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
    }
  }
}
</script>

<style scoped>
/* ...existing code... */
</style>