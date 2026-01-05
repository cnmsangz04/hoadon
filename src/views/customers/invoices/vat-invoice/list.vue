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
          <b-form-datepicker v-model="filters.date" placeholder="Lọc theo ngày lập" />
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
            <b-dropdown-item class="text-center" href="#" @click.prevent="signInvoice(item)">Ký số</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="sendToCqt(item)">Gửi CQT</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="sendMail(item)">Gửi mail</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="viewHistory(item)">Lịch sử truyền nhận</b-dropdown-item>
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
  name: 'VatInvoiceList',
  data () {
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
        status: null,
        date: null,
      },
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
        { value: 7, text: 'Không đủ điều kiện cấp mã' },
      ],
    }
  },
  created () { this.fetchList() },
  methods: {
    async fetchList () {
      this.isBusy = true
      try {
        const params = {
          page: this.list.current_page,
          size: this.list.per_page,
          q: this.filters.keyword || undefined,
          status: this.filters.status != null ? Number(this.filters.status) : undefined,
          date: this.filters.date || undefined,
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
        case 7: return 'Không đủ điều kiện cấp mã'
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
      try {
        const n = Number(v || 0)
        return n.toLocaleString('vi-VN')
      } catch { return '0' }
    },

    // Action placeholders
    cloneInvoice (item) { /* TODO: implement clone API */ },
    viewInvoice (item) { /* TODO: open modal or route to view */ },
    signInvoice (item) { /* TODO: implement signing */ },
    sendToCqt (item) { /* TODO: integrate with tax authority API */ },
    sendMail (item) { /* TODO: call backend mail sender */ },
    viewHistory (item) { /* TODO: navigate to transmission history */ },
  }
}
</script>

<style scoped>
.font-weight-bold { font-weight: 700; }
.shadow-sm { border-radius: 10px; }
</style>