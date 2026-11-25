<template>
  <div class="container-fluid py-3 stock-issue-list">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">{{ pageTitle }}</h4>
      <div>
        <b-dropdown
          size="sm"
          variant="primary"
          class="mr-2"
          :disabled="!selectedIds.length || loading"
          right
          boundary="window"
        >
          <template #button-content>
            <i class="fas fa-signature"></i>
            Ký số
            <span v-if="selectedIds.length">({{ selectedIds.length }})</span>
          </template>
          <b-dropdown-item href="#" @click.prevent="signSelected">
            <i class="fas fa-plug mr-1"></i> Ký USB token
          </b-dropdown-item>
        </b-dropdown>
        <b-button
          size="sm"
          variant="outline-primary"
          class="mr-2"
          :disabled="loading"
          @click="fetchList"
        >
          <i class="fas fa-sync-alt"></i> Làm mới
        </b-button>
        <b-button
          size="sm"
          variant="outline-success"
          class="mr-2"
          to="/imports/stock-issue"
        >
          <i class="fas fa-file-invoice"></i> Import phiếu xuất kho
        </b-button>
        <b-button
          size="sm"
          variant="outline-info"
          class="mr-2"
          :to="reportRoute"
        >
          <i class="fas fa-file-excel"></i> Báo cáo Excel
        </b-button>
        <b-button size="sm" variant="success" :to="createRoute">
          <i class="fas fa-file-invoice"></i> Lập phiếu
        </b-button>
      </div>
    </div>

    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="4" class="mb-2">
          <b-input-group>
            <b-input-group-prepend is-text
              ><i class="fas fa-search text-muted"></i
            ></b-input-group-prepend>
            <b-form-input
              v-model.trim="filters.keyword"
              placeholder="Tìm mã tra cứu / người nhận"
              @keyup.enter="applyFilters"
            />
          </b-input-group>
        </b-col>
        <b-col md="3" class="mb-2"
          ><b-form-select v-model="filters.status" :options="statusOptions"
        /></b-col>
        <b-col md="3" class="mb-2"
          ><manual-date-picker
            v-model="filters.date"
            placeholder="Lọc theo ngày lập"
        /></b-col>
        <b-col md="2" class="mb-2 text-right"
          ><b-button size="sm" variant="primary" @click="applyFilters"
            >Tìm kiếm</b-button
          ></b-col
        >
      </b-row>
      <div class="d-flex justify-content-end mt-2">
        <b-button
          size="sm"
          variant="outline-secondary"
          class="mr-2"
          @click="resetFilters"
          >Xóa lọc</b-button
        >
        <b-button size="sm" variant="outline-primary" @click="applyFilters"
          ><i class="fas fa-filter"></i> Áp dụng</b-button
        >
      </div>
    </b-card>

    <b-card class="shadow-sm">
      <b-table
        bordered
        hover
        responsive
        small
        show-empty
        :items="items"
        :fields="fields"
        :busy="loading"
        empty-text="Không có dữ liệu"
      >
        <template #head(select)>
          <b-form-checkbox
            :checked="allSelected"
            :indeterminate="selectedIds.length > 0 && !allSelected"
            aria-label="Chọn tất cả phiếu có thể ký"
            @change="toggleAll"
          />
        </template>
        <template #cell(select)="{ item }"
          ><b-form-checkbox
            :checked="isSelected(item.id)"
            :disabled="Number(item.status) !== 0"
            aria-label="Chọn phiếu"
            @change="(checked) => toggleSelected(item.id, checked)"
        /></template>
        <template #cell(index)="{ index }">{{
          index + 1 + (page - 1) * size
        }}</template>
        <template #cell(form_serial)="{ item }"
          ><code>{{
            (item.formCode || '') + (item.serial || '')
          }}</code></template
        >
        <template #cell(date_export)="{ item }">{{
          formatDate(item.dateExport)
        }}</template>
        <template #cell(no)="{ item }">{{ item.no || '—' }}</template>
        <template #cell(lookup_code)="{ item }"
          ><code>{{ item.lookupCode || '—' }}</code></template
        >
        <template #cell(receiver)="{ item }">
          <div>{{ item.customerName || '—' }}</div>
          <small class="text-muted"
            >Mã CQT: {{ item.codeCqt ? 'Đã cấp mã' : 'Chưa cấp mã' }}</small
          >
        </template>
        <template #cell(amount)="{ item }">{{
          formatCurrency(item.amount)
        }}</template>
        <template #cell(username)="{ item }">{{
          item.username || '—'
        }}</template>
        <template #cell(status)="{ item }"
          ><b-badge :variant="statusVariant(item.status)">{{
            statusText(item.status)
          }}</b-badge></template
        >
        <template #cell(relation)="{ item }"
          ><b-badge
            :variant="relationVariant(item)"
            :title="relationTitle(item)"
            >{{ relationText(item) }}</b-badge
          ></template
        >
        <template #cell(actions)="{ item }">
          <b-dropdown
            size="sm"
            right
            variant="link"
            toggle-class="text-decoration-none"
            no-caret
            boundary="window"
          >
            <template #button-content
              ><i class="fas fa-ellipsis-h"></i
            ></template>
            <b-dropdown-item href="#" @click.prevent="goEdit(item)"
              >Cập nhật</b-dropdown-item
            >
            <b-dropdown-item href="#" @click.prevent="cloneItem(item)"
              >Sao chép</b-dropdown-item
            >
            <b-dropdown-item href="#" @click.prevent="viewItem(item)"
              >Xem</b-dropdown-item
            >
            <b-dropdown-item
              v-if="Number(item.status) === 0"
              href="#"
              @click.prevent="signOne(item)"
              >Ký USB token</b-dropdown-item
            >
            <b-dropdown-item
              v-if="canSend(item)"
              href="#"
              @click.prevent="sendToCqt(item)"
              >{{
                Number(item.status) === 2 ? 'Gửi lại CQT' : 'Gửi CQT'
              }}</b-dropdown-item
            >
            <b-dropdown-item
              v-if="Number(item.status) === 3"
              href="#"
              @click.prevent="sendMail(item)"
              >Gửi mail</b-dropdown-item
            >
            <b-dropdown-item
              v-if="Number(item.status) > 1"
              href="#"
              @click.prevent="viewHistory(item)"
              >Lịch sử truyền nhận</b-dropdown-item
            >
            <b-dropdown-item
              v-if="Number(item.status) > 2"
              href="#"
              @click.prevent="createErrorMessage(item)"
              >Lập TB sai sót</b-dropdown-item
            >
            <b-dropdown-item
              v-if="Number(item.status) === 0"
              href="#"
              class="text-danger"
              @click.prevent="deleteItem(item)"
              >Xóa phiếu</b-dropdown-item
            >
          </b-dropdown>
        </template>
      </b-table>
      <pagination-bar
        :current.sync="page"
        :size.sync="size"
        :total="total"
        :sizes="[10, 20, 50, 100]"
        @page-change="fetchList"
        @size-change="onPageSizeChange"
      />
    </b-card>

    <b-modal
      id="stockIssuePreview"
      size="lg"
      title="Xem phiếu xuất kho"
      body-class="invoice-preview-body"
    >
      <iframe
        id="stockIssuePreviewFrame"
        class="invoice-preview-frame"
        :src="preview.src"
        onload="((obj) => {try{obj.style.height = obj.contentWindow.document.body.scrollHeight + 'px';}catch(e){obj.style.height = 0;}})(this)"
      ></iframe>
      <template #modal-footer>
        <div class="modal-footer-spread">
          <b-button variant="light" size="sm" @click="closeModal"
            >Đóng</b-button
          >
          <div class="modal-footer-actions">
            <b-button
              variant="success"
              size="sm"
              :href="invoicePublicUrl(preview.lookupCode, '/download-pdf')"
              ><i class="fas fa-file-pdf mr-1"></i> Tải PDF</b-button
            >
            <b-button
              variant="info"
              size="sm"
              :href="invoicePublicUrl(preview.lookupCode, '/download-xml')"
              ><i class="fas fa-file-code mr-1"></i> Tải XML</b-button
            >
            <b-button variant="secondary" size="sm" @click="printPreview"
              ><i class="fas fa-print mr-1"></i> In phiếu</b-button
            >
          </div>
        </div>
      </template>
    </b-modal>

    <b-modal
      ref="historyModal"
      size="lg"
      title="Lịch sử truyền nhận"
      hide-header-close
    >
      <b-table-simple
        bordered
        small
        responsive
        show-empty
        :busy="historyLoading"
        empty-text="Không có dữ liệu"
      >
        <b-thead
          ><b-tr
            ><b-th class="text-center">STT</b-th><b-th>Tiêu đề</b-th
            ><b-th>Mô tả</b-th><b-th>Người thao tác</b-th
            ><b-th>Ngày thực hiện</b-th></b-tr
          ></b-thead
        >
        <b-tbody>
          <b-tr v-for="(row, index) in historyRows" :key="row.id || index"
            ><b-td class="text-center">{{ index + 1 }}</b-td
            ><b-td>{{ row.title || '—' }}</b-td
            ><b-td>{{ row.description || '—' }}</b-td
            ><b-td>{{ row.username || row.user_id || '—' }}</b-td
            ><b-td>{{
              formatDateTime(row.createdAt || row.created_at)
            }}</b-td></b-tr
          >
          <b-tr v-if="!historyRows.length"
            ><b-td colspan="5" class="text-center">Không có dữ liệu</b-td></b-tr
          >
        </b-tbody>
      </b-table-simple>
      <template #modal-footer
        ><b-button size="sm" variant="light" @click="$refs.historyModal.hide()"
          >Đóng</b-button
        ></template
      >
    </b-modal>

    <b-modal
      ref="mailModal"
      size="md"
      title="Gửi thông báo phát hành phiếu"
      :busy="mail.loading"
    >
      <b-alert show variant="light" class="py-2"
        ><div class="small">
          <div>
            <b>Ký hiệu:</b> <code>{{ mail.formSerial || '—' }}</code>
          </div>
          <div><b>Số phiếu:</b> {{ mail.no || '—' }}</div>
          <div><b>Ngày lập:</b> {{ formatDate(mail.dateExport) }}</div>
        </div></b-alert
      >
      <b-form-group label="Tên người nhận"
        ><b-form-input v-model.trim="mail.name"
      /></b-form-group>
      <b-form-group label="Email người nhận"
        ><b-form-input v-model.trim="mail.email" type="email"
      /></b-form-group>
      <template #modal-footer
        ><b-button size="sm" variant="light" @click="$refs.mailModal.hide()"
          >Đóng</b-button
        ><b-button
          size="sm"
          variant="primary"
          :disabled="mail.loading"
          @click="confirmSendMail"
          >Gửi</b-button
        ></template
      >
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import PaginationBar from '@/views/components/pagination_bar.vue'
import { toastSuccess, toastWarning } from '@/utils/toast'
import { confirmUsbTokenSignature } from '@/utils/pasigner'

export default {
  name: 'StockIssueList',
  components: { PaginationBar },
  props: {
    stockIssueType: { type: Number, required: true },
    pageTitle: { type: String, required: true },
    receiverLabel: { type: String, required: true },
    createRoute: { type: String, required: true },
    editRoutePrefix: { type: String, required: true },
    reportRoute: { type: String, required: true },
  },
  data() {
    return {
      items: [],
      total: 0,
      page: 1,
      size: 10,
      loading: false,
      selectedIds: [],
      pollTimers: {},
      pollAttempts: {},
      sellerTaxcode: '',
      filters: { keyword: '', status: null, date: null },
      preview: { src: null, lookupCode: null },
      historyRows: [],
      historyLoading: false,
      mail: {
        id: null,
        formSerial: '',
        no: null,
        dateExport: null,
        name: '',
        email: '',
        loading: false,
      },
      fields: [
        { key: 'select', label: '', thStyle: { width: '3%' } },
        { key: 'index', label: 'STT', thStyle: { width: '4%' } },
        { key: 'form_serial', label: 'Ký hiệu', thStyle: { width: '9%' } },
        { key: 'no', label: 'Số', thStyle: { width: '6%' } },
        { key: 'date_export', label: 'Ngày lập', thStyle: { width: '8%' } },
        { key: 'lookup_code', label: 'Mã tra cứu', thStyle: { width: '9%' } },
        {
          key: 'receiver',
          label: this.receiverLabel,
          thStyle: { width: '17%' },
        },
        { key: 'amount', label: 'Giá trị', thStyle: { width: '9%' } },
        { key: 'username', label: 'Người lập', thStyle: { width: '10%' } },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '10%' } },
        { key: 'relation', label: 'Loại phiếu', thStyle: { width: '8%' } },
        { key: 'actions', label: '', thStyle: { width: '7%' } },
      ],
      statusOptions: [
        { value: null, text: 'Tất cả trạng thái' },
        { value: 0, text: 'Mới khởi tạo' },
        { value: 1, text: 'Đã ký' },
        { value: 2, text: 'Đã gửi thuế' },
        { value: 3, text: 'Đã phát hành' },
        { value: 4, text: 'Bị thay thế' },
        { value: 5, text: 'Bị điều chỉnh' },
        { value: 6, text: 'Đã hủy' },
        { value: 7, text: 'Không đủ điều kiện' },
      ],
    }
  },
  computed: {
    allSelected() {
      const ids = this.items
        .filter((item) => Number(item.status) === 0)
        .map((item) => Number(item.id))
      return ids.length > 0 && ids.every((id) => this.selectedIds.includes(id))
    },
  },
  created() {
    this.ensureSellerTaxcode()
    this.fetchList()
  },
  beforeDestroy() {
    Object.values(this.pollTimers).forEach((timer) => clearTimeout(timer))
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        const params = {
          page: this.page,
          size: this.size,
          q: this.filters.keyword || undefined,
          status:
            this.filters.status == null
              ? undefined
              : Number(this.filters.status),
          date: this.formatDateParam(this.filters.date) || undefined,
          category: 6,
          stockIssueType: this.stockIssueType,
        }
        const { data } = await axios.get('/invoices', { params })
        this.items = (data.items || data.data || []).map((item) => ({
          ...item,
          id: item.id,
          customerName: item.customerName || item.customer_name,
        }))
        this.total = data.total || 0
        this.selectedIds = this.selectedIds.filter((id) =>
          this.items.some(
            (item) => Number(item.id) === id && Number(item.status) === 0
          )
        )
      } catch (error) {
        this.showError(error, 'Không tải được danh sách phiếu xuất kho')
      } finally {
        this.loading = false
      }
    },
    formatDateParam(value) {
      if (!value) return null
      const date = value instanceof Date ? value : new Date(value)
      if (Number.isNaN(date.getTime())) return null
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(
        2,
        '0'
      )}-${String(date.getDate()).padStart(2, '0')}`
    },
    applyFilters() {
      this.page = 1
      this.fetchList()
    },
    resetFilters() {
      this.filters = { keyword: '', status: null, date: null }
      this.applyFilters()
    },
    onPageSizeChange(value) {
      this.size = Number(value) || 10
      this.page = 1
      this.fetchList()
    },
    isSelected(id) {
      return this.selectedIds.includes(Number(id))
    },
    toggleSelected(id, checked) {
      const value = Number(id)
      this.selectedIds = checked
        ? Array.from(new Set([...this.selectedIds, value]))
        : this.selectedIds.filter((item) => item !== value)
    },
    toggleAll(checked) {
      const ids = this.items
        .filter((item) => Number(item.status) === 0)
        .map((item) => Number(item.id))
      this.selectedIds = checked
        ? Array.from(new Set([...this.selectedIds, ...ids]))
        : this.selectedIds.filter((id) => !ids.includes(id))
    },
    goEdit(item) {
      this.$router.push({ path: `${this.editRoutePrefix}/${item.id}/edit` })
    },
    async cloneItem(item) {
      try {
        const { data } = await axios.post(`/invoices/${item.id}/clone`)
        const id = data?.id || data?.data?.id
        if (id) {
          toastSuccess('Đã sao chép phiếu xuất kho')
          this.$router.push({ path: `${this.editRoutePrefix}/${id}/edit` })
        }
      } catch (e) {
        this.showError(e, 'Không thể sao chép phiếu xuất kho')
      }
    },
    async viewItem(item) {
      const code = item.lookupCode || item.lookup_code
      if (!code)
        return this.showError(null, 'Không có mã tra cứu phiếu xuất kho')
      await this.ensureSellerTaxcode()
      const url = this.invoicePublicUrl(code, '/view')
      this.preview.lookupCode = code
      this.preview.src = `${url}${
        url.includes('?') ? '&' : '?'
      }viewRevision=${Date.now()}`
      this.$root.$emit('bv::show::modal', 'stockIssuePreview')
    },
    closeModal() {
      this.$root.$emit('bv::hide::modal', 'stockIssuePreview')
    },
    printPreview() {
      const frame = document.getElementById('stockIssuePreviewFrame')
      if (frame?.contentWindow)
        frame.contentWindow.printInvoice
          ? frame.contentWindow.printInvoice()
          : frame.contentWindow.print()
    },
    async signOne(item) {
      this.selectedIds = [Number(item.id)]
      await this.signSelected()
    },
    async signSelected() {
      const ids = Array.from(
        new Set(this.selectedIds.map(Number).filter(Boolean))
      )
      if (!ids.length) return
      try {
        if (
          !(await confirmUsbTokenSignature(
            ids.length > 1
              ? `Bạn có chắc chắn muốn ký số ${ids.length} phiếu bằng USB token?`
              : 'Bạn có chắc chắn muốn ký số phiếu bằng USB token?'
          ))
        )
          return
        const { data } = await axios.post('/invoices/sign-token/prepare', {
          ids,
        })
        if (!data?.hash) throw new Error('Không tạo được mã hash ký số')
        location.href =
          'pasigner://' +
          encodeURIComponent(
            JSON.stringify({
              src: 'web',
              action: 'signature',
              pid: Math.random().toString(36).slice(2, 8),
              data: { hash: data.hash, domain: window.location.hostname },
            })
          )
        this.pollSignature(data.hash, ids)
      } catch (e) {
        this.showError(e, 'Không thể ký số phiếu xuất kho')
      }
    },
    pollSignature(hash, ids) {
      let attempts = 0
      const check = async () => {
        try {
          const { data } = await axios.get('/invoices/sign-token/status', {
            params: { hash, ids: ids.join(',') },
            meta: { suppressGlobalErrorToast: true },
          })
          if (data?.state === 'signed') {
            toastSuccess(`Ký số thành công ${data.signed || ids.length} phiếu`)
            this.selectedIds = []
            this.fetchList()
            return
          }
          if (data?.state === 'failed')
            return toastWarning(
              'Ứng dụng ký số không trả về dữ liệu hợp lệ. Vui lòng ký lại.'
            )
        } catch (error) {
          const status = Number(error?.response?.status || 0)
          if (status === 401 || status === 403) {
            this.showError(error, 'Không thể kiểm tra trạng thái ký số')
            return
          }
        }
        if (++attempts < 100) this.pollTimers[hash] = setTimeout(check, 1200)
        else
          toastWarning(
            'Chưa nhận được dữ liệu ký số. Hãy kiểm tra P.A Signer hoặc thử ký lại.'
          )
      }
      this.pollTimers[hash] = setTimeout(check, 1200)
    },
    canSend(item) {
      return Number(item.status) === 1 || Number(item.status) === 2
    },
    async sendToCqt(item) {
      const resend = Number(item.status) === 2
      if (
        !window.confirm(
          resend
            ? 'Xác nhận gửi lại phiếu xuất kho lên Cơ quan thuế?'
            : 'Xác nhận gửi phiếu xuất kho lên Cơ quan thuế?'
        )
      )
        return
      try {
        await axios.post(`/invoices/${item.id}/send-to-cqt`, null, {
          successMessage: resend
            ? 'Đã gửi lại phiếu xuất kho lên Cơ quan thuế'
            : 'Đã gửi phiếu xuất kho lên Cơ quan thuế',
        })
        await this.fetchList()
      } catch (error) {
        this.showError(error, 'Không thể gửi phiếu xuất kho lên Cơ quan thuế')
      }
    },
    async sendMail(item) {
      try {
        const { data } = await axios.get(`/invoices/${item.id}`, {
          meta: { suppressGlobalErrorToast: true },
        })
        const receiver = data?.customer || {}
        this.mail = {
          id: item.id,
          formSerial: `${item.formCode || ''}${item.serial || ''}`,
          no: item.no,
          dateExport: item.dateExport,
          name: receiver.name || receiver.buyer || '',
          email: receiver.email || '',
          loading: false,
        }
        this.$refs.mailModal.show()
      } catch (e) {
        this.showError(e, 'Không thể tải thông tin người nhận')
      }
    },
    async confirmSendMail() {
      if (!this.mail.name || !this.mail.email)
        return this.showError(null, 'Vui lòng nhập tên và email người nhận')
      try {
        this.mail.loading = true
        await axios.post(
          `/invoices/${this.mail.id}/send-email`,
          { name: this.mail.name, email: this.mail.email },
          { successMessage: 'Đã gửi email thông báo phát hành phiếu' }
        )
        this.$refs.mailModal.hide()
      } catch (error) {
        this.showError(error, 'Không thể gửi email')
      } finally {
        this.mail.loading = false
      }
    },
    async viewHistory(item) {
      this.historyLoading = true
      this.historyRows = []
      this.$refs.historyModal.show()
      try {
        const { data } = await axios.get(`/invoices/${item.id}/history`)
        this.historyRows = Array.isArray(data) ? data : []
      } catch (error) {
        this.showError(error, 'Không thể tải lịch sử truyền nhận')
      } finally {
        this.historyLoading = false
      }
    },
    createErrorMessage(item) {
      this.$router.push({
        name: 'CustomerStockErrorMessageCreate',
        query: { ids: String(item.id) },
      })
    },
    async deleteItem(item) {
      if (
        Number(item.status) !== 0 ||
        !window.confirm('Xóa phiếu xuất kho này?')
      )
        return
      try {
        await axios.delete(`/invoices/${item.id}`)
        toastSuccess('Đã xóa phiếu xuất kho')
        await this.fetchList()
      } catch (e) {
        this.showError(e, 'Xóa phiếu xuất kho thất bại')
      }
    },
    relationText(item) {
      const type = Number(item.invoiceType || 0)
      if (Number(item.status) === 4) return 'Bị thay'
      if (Number(item.status) === 5) return 'Bị ĐC'
      if (type === 1) return 'Thay thế'
      if (type === 2) return 'Điều chỉnh'
      return 'Gốc'
    },
    relationTitle(item) {
      const type = Number(item.invoiceType || 0)
      if (type === 1) return 'Phiếu xuất kho thay thế'
      if (type === 2) return 'Phiếu xuất kho điều chỉnh'
      return 'Phiếu xuất kho gốc'
    },
    relationVariant(item) {
      const type = Number(item.invoiceType || 0)
      return type === 1 ? 'primary' : type === 2 ? 'dark' : 'secondary'
    },
    statusText(value) {
      return (
        {
          0: 'Mới khởi tạo',
          1: 'Đã ký',
          2: 'Đã gửi thuế',
          3: 'Đã phát hành',
          4: 'Bị thay thế',
          5: 'Bị điều chỉnh',
          6: 'Đã hủy',
          7: 'Không đủ điều kiện',
        }[Number(value)] || '—'
      )
    },
    statusVariant(value) {
      return (
        {
          0: 'secondary',
          1: 'info',
          2: 'warning',
          3: 'success',
          4: 'primary',
          5: 'dark',
          6: 'danger',
          7: 'light',
        }[Number(value)] || 'secondary'
      )
    },
    formatDate(value) {
      return value ? String(value).replace('T', ' ') : '—'
    },
    formatDateTime(value) {
      if (!value) return '—'
      const date = new Date(value)
      return Number.isNaN(date.getTime())
        ? String(value)
        : date.toLocaleString('vi-VN')
    },
    formatCurrency(value) {
      return Number(value || 0).toLocaleString('vi-VN')
    },
    appSellerTaxcode() {
      const company = this.$app?.info?.company || {}
      return String(company.taxcode || company.taxCode || '').trim()
    },
    async ensureSellerTaxcode() {
      if (this.appSellerTaxcode()) {
        this.sellerTaxcode = this.appSellerTaxcode()
        return
      }
      try {
        const { data } = await axios.get('/auth/info', {
          meta: { suppressGlobalErrorToast: true },
        })
        this.sellerTaxcode = String(
          data?.company?.taxcode || data?.company?.taxCode || ''
        ).trim()
      } catch (error) {
        this.showError(error, 'Không xác định được mã số thuế đơn vị xuất hàng')
      }
    },
    invoicePublicUrl(code, action) {
      const base = (axios.defaults?.baseURL || '/v1').replace(/\/$/, '')
      const taxcode = this.sellerTaxcode || this.appSellerTaxcode()
      return `${base}/invoices/${encodeURIComponent(code || '')}${action}${
        taxcode ? `?taxcode=${encodeURIComponent(taxcode)}` : ''
      }`
    },
    showError(error, fallback) {
      const message =
        error?.response?.data?.message || error?.message || fallback
      this.$bvToast &&
        this.$bvToast.toast(message, {
          title: 'Lỗi',
          variant: 'danger',
          solid: true,
          autoHideDelay: 4000,
        })
    },
  },
}
</script>
