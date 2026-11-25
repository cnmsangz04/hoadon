<template>
  <div class="container-fluid py-3">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h4 class="mb-0">Phiếu xuất kho hàng gửi bán đại lý</h4>
      <div>
        <b-button size="sm" variant="primary" class="mr-2" :disabled="!selectedIds.length || signing" @click="signSelected">Ký USB token</b-button>
        <b-button size="sm" variant="outline-success" class="mr-2" to="/imports/invoice?category=6&stockIssueType=2">Import</b-button>
        <b-button size="sm" variant="outline-info" class="mr-2" to="/reports/invoice/list?category=6&stockIssueType=2">Báo cáo Excel</b-button>
        <b-button size="sm" variant="success" to="/stock-issue/consignment/create">Lập phiếu</b-button>
      </div>
    </div>
    <b-card class="shadow-sm">
      <b-row class="mb-3">
        <b-col md="5"><b-form-input v-model.trim="keyword" placeholder="Tìm mã tra cứu, đại lý" @keyup.enter="load" /></b-col>
        <b-col md="3"><b-form-select v-model="status" :options="statuses" /></b-col>
        <b-col md="2"><b-button size="sm" variant="primary" @click="load">Tìm kiếm</b-button></b-col>
      </b-row>
      <b-table bordered hover responsive small show-empty :items="items" :fields="fields" :busy="loading" empty-text="Không có dữ liệu">
        <template #head(select)><b-form-checkbox :checked="allSelected" :indeterminate="selectedIds.length > 0 && !allSelected" @change="toggleAll" /></template>
        <template #cell(select)="{ item }"><b-form-checkbox :checked="selectedIds.includes(Number(item.id))" :disabled="Number(item.status) !== 0" @change="checked => toggleSelected(item, checked)" /></template>
        <template #cell(form)="{ item }"><code>{{ (item.formCode || '') + (item.serial || '') }}</code></template>
        <template #cell(dateExport)="{ item }">{{ item.dateExport || '—' }}</template>
        <template #cell(no)="{ item }">{{ item.no || '—' }}</template>
        <template #cell(amount)="{ item }">{{ money(item.amount) }}</template>
        <template #cell(status)="{ item }"><b-badge>{{ statusText(item.status) }}</b-badge></template>
        <template #cell(actions)="{ item }">
          <b-dropdown size="sm" right variant="link" no-caret>
            <template #button-content><i class="fas fa-ellipsis-h"></i></template>
            <b-dropdown-item v-if="Number(item.status) === 0" :to="'/stock-issue/consignment/' + item.id + '/edit'">Cập nhật</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) === 0" @click="sign([item.id])">Ký USB token</b-dropdown-item>
            <b-dropdown-item :href="'/v1/invoices/' + item.lookupCode + '/download-xml'" target="_blank">Tải XML</b-dropdown-item>
            <b-dropdown-item :href="'/v1/invoices/' + item.lookupCode + '/download-pdf'" target="_blank">Tải PDF</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) === 1 || Number(item.status) === 2" @click="send(item)">Gửi CQT</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) === 0" @click="remove(item)">Xóa phiếu</b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>
      <pagination-bar :current.sync="page" :size.sync="size" :total="total" :sizes="[10,20,50]" @page-change="load" @size-change="load" />
    </b-card>
  </div>
</template>
<script>
import axios from '@/plugins/axios'
import PaginationBar from '@/views/components/pagination_bar.vue'
import { toastSuccess, toastWarning } from '@/utils/toast'
import { confirmUsbTokenSignature } from '@/utils/pasigner'

export default {
  name: 'StockIssueConsignmentList',
  components: { PaginationBar },
  data () {
    return { items: [], total: 0, page: 1, size: 10, keyword: '', status: null, loading: false, signing: false, selectedIds: [], pollTimers: {}, statuses: [{ value: null, text: 'Tất cả trạng thái' }, { value: 0, text: 'Mới khởi tạo' }, { value: 1, text: 'Đã ký' }, { value: 2, text: 'Đã gửi thuế' }, { value: 3, text: 'Đã phát hành' }, { value: 4, text: 'Bị thay thế' }, { value: 5, text: 'Bị điều chỉnh' }, { value: 6, text: 'Đã hủy' }, { value: 7, text: 'Không đủ điều kiện' }], fields: [{ key: 'select', label: '' }, { key: 'form', label: 'Ký hiệu' }, { key: 'no', label: 'Số' }, { key: 'dateExport', label: 'Ngày lập' }, { key: 'customerName', label: 'Đại lý nhận' }, { key: 'amount', label: 'Giá trị' }, { key: 'status', label: 'Trạng thái' }, { key: 'actions', label: '' }] }
  },
  computed: { allSelected () { const ids = this.items.filter(item => Number(item.status) === 0).map(item => Number(item.id)); return ids.length > 0 && ids.every(id => this.selectedIds.includes(id)) } },
  mounted () { this.load() },
  beforeDestroy () { Object.values(this.pollTimers).forEach(timer => clearTimeout(timer)) },
  methods: {
    async load () { this.loading = true; try { const { data } = await axios.get('/invoices', { params: { category: 6, stockIssueType: 2, page: this.page, size: this.size, q: this.keyword || undefined, status: this.status } }); this.items = data.items || []; this.selectedIds = this.selectedIds.filter(id => this.items.some(item => Number(item.id) === id && Number(item.status) === 0)); this.total = data.total || 0 } finally { this.loading = false } },
    toggleSelected (item, checked) { const id = Number(item.id); this.selectedIds = checked ? Array.from(new Set([...this.selectedIds, id])) : this.selectedIds.filter(value => value !== id) },
    toggleAll (checked) { const ids = this.items.filter(item => Number(item.status) === 0).map(item => Number(item.id)); this.selectedIds = checked ? Array.from(new Set([...this.selectedIds, ...ids])) : this.selectedIds.filter(id => !ids.includes(id)) },
    async sign (ids) { try { const values = Array.from(new Set(ids.map(Number).filter(Boolean))); if (!values.length || !(await confirmUsbTokenSignature(values.length > 1 ? `Bạn có chắc chắn muốn ký số ${values.length} phiếu bằng USB token?` : 'Bạn có chắc chắn muốn ký số phiếu bằng USB token?'))) return; this.signing = true; const { data } = await axios.post('/invoices/sign-token/prepare', { ids: values }); if (!data || !data.hash) throw new Error('Không tạo được mã hash ký số'); location.href = 'pasigner://' + encodeURIComponent(JSON.stringify({ src: 'web', action: 'signature', pid: Math.random().toString(36).slice(2, 8), data: { hash: data.hash, domain: window.location.hostname } })); this.pollSignature(data.hash, values) } catch (e) { this.$bvToast && this.$bvToast.toast(e?.response?.data?.message || e.message || 'Không thể ký số bằng USB token', { title: 'Lỗi ký số', variant: 'danger', solid: true }) } finally { this.signing = false } },
    signSelected () { return this.sign(this.selectedIds) },
    pollSignature (hash, ids) { let attempts = 0; const check = async () => { try { const { data } = await axios.get('/invoices/sign-token/status', { params: { hash, ids: ids.join(',') }, meta: { suppressGlobalErrorToast: true } }); if (data?.state === 'signed') { toastSuccess(`Ký số thành công ${data.signed || ids.length} phiếu`); this.selectedIds = []; this.load(); return } if (data?.state === 'failed') { toastWarning('Ứng dụng ký số không trả về dữ liệu hợp lệ. Vui lòng ký lại.'); return } } catch (e) {} if (++attempts < 100) this.pollTimers[hash] = setTimeout(check, 1200); else toastWarning('Chưa nhận được dữ liệu ký số. Hãy kiểm tra P.A Signer hoặc thử ký lại.') }; this.pollTimers[hash] = setTimeout(check, 1200) },
    money (v) { return Number(v || 0).toLocaleString('vi-VN') + ' đ' },
    statusText (v) { return ({ 0: 'Mới khởi tạo', 1: 'Đã ký', 2: 'Đã gửi thuế', 3: 'Đã phát hành', 4: 'Bị thay thế', 5: 'Bị điều chỉnh', 6: 'Đã hủy', 7: 'Không đủ điều kiện' })[Number(v)] || '—' },
    async send (item) { await axios.post('/invoices/' + item.id + '/send-to-cqt'); this.load() },
    async remove (item) { if (!window.confirm('Xóa phiếu xuất kho này?')) return; await axios.delete('/invoices/' + item.id); this.load() }
  }
}
</script>
