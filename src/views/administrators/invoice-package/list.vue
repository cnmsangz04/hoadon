<template>
  <div class="container-fluid py-3 invoice-package-admin">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Quản lý gói hóa đơn</h4>
      <b-button size="sm" variant="outline-primary" @click="reloadAll">
        <i class="fas fa-sync-alt mr-1"></i>
        Làm mới
      </b-button>
    </div>

    <b-tabs content-class="mt-3" pills>
      <b-tab title="Danh mục gói" active>
        <b-card class="mb-3 shadow-sm">
          <b-row>
            <b-col md="5" class="mb-2">
              <b-input-group>
                <b-input-group-prepend is-text>
                  <i class="fas fa-search text-muted"></i>
                </b-input-group-prepend>
                <b-form-input v-model.trim="packageFilter.keyword" placeholder="Tìm theo tên gói" @keyup.enter="applyPackageFilter" />
              </b-input-group>
            </b-col>
            <b-col md="3" class="mb-2">
              <b-form-select v-model="packageFilter.status" :options="statusFilterOptions" />
            </b-col>
            <b-col md="4" class="mb-2 text-md-right">
              <b-button size="sm" variant="primary" class="mr-2" @click="applyPackageFilter">
                <i class="fas fa-filter mr-1"></i>
                Áp dụng
              </b-button>
              <b-button size="sm" variant="success" @click="openPackageModal()">
                <i class="fas fa-plus mr-1"></i>
                Thêm gói
              </b-button>
            </b-col>
          </b-row>
        </b-card>

        <b-card class="shadow-sm">
          <b-table
            bordered
            hover
            responsive
            small
            show-empty
            :busy="packageBusy"
            :items="packages"
            :fields="packageFields"
            empty-text="Không có dữ liệu"
          >
            <template #cell(index)="{ index }">
              {{ index + 1 + (packageList.current_page - 1) * packageList.per_page }}
            </template>

            <template #cell(name)="{ item }">
              <div class="font-weight-bold">{{ item.name }}</div>
              <small class="text-muted">{{ item.description || '—' }}</small>
            </template>

            <template #cell(invoiceQuantity)="{ item }">
              <strong>{{ formatNumber(item.invoiceQuantity) }}</strong>
              <div v-if="item.includeTrial" class="text-success small">Bao gồm gói trải nghiệm</div>
            </template>

            <template #cell(unitPrice)="{ item }">
              {{ formatCurrency(item.unitPrice) }}/hđ
            </template>

            <template #cell(totalPrice)="{ item }">
              <span class="font-weight-bold text-primary">{{ formatCurrency(item.totalPrice) }}</span>
            </template>

            <template #cell(status)="{ item }">
              <b-badge :variant="Number(item.status) === 1 ? 'success' : 'secondary'">
                {{ Number(item.status) === 1 ? 'Kích hoạt' : 'Ngưng hoạt động' }}
              </b-badge>
            </template>

            <template #cell(option)="{ item }">
              <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
                <template #button-content>
                  <i class="fas fa-ellipsis-h"></i>
                </template>
                <b-dropdown-item href="#" class="text-center" @click.prevent="openPackageModal(item)">Cập nhật</b-dropdown-item>
                <b-dropdown-item href="#" class="text-center text-danger" @click.prevent="deletePackage(item)">Xóa</b-dropdown-item>
              </b-dropdown>
            </template>
          </b-table>

          <pagination-bar
            :current.sync="packageList.current_page"
            :size.sync="packageList.per_page"
            :total="packageList.total"
            :sizes="pageSizes"
            @page-change="onPackagePageChange"
            @size-change="onPackagePageSizeChange"
          />
        </b-card>
      </b-tab>

      <b-tab title="Lịch sử mua">
        <b-card class="mb-3 shadow-sm">
          <b-row>
            <b-col md="3" class="mb-2">
              <v-select v-model="purchaseFilter.companyId" :options="companyOptions" label="label" :reduce="c => c.value" placeholder="Tất cả công ty" />
            </b-col>
            <b-col md="2" class="mb-2">
              <b-form-select v-model="purchaseFilter.paymentMethod" :options="paymentMethodOptions" />
            </b-col>
            <b-col md="2" class="mb-2">
              <b-form-input v-model="purchaseFilter.fromDate" type="date" />
            </b-col>
            <b-col md="2" class="mb-2">
              <b-form-input v-model="purchaseFilter.toDate" type="date" />
            </b-col>
            <b-col md="3" class="mb-2 text-md-right">
              <b-button size="sm" variant="primary" class="mr-2" @click="applyPurchaseFilter">
                <i class="fas fa-search mr-1"></i>
                Tìm kiếm
              </b-button>
              <b-button size="sm" variant="outline-secondary" @click="resetPurchaseFilter">Xóa lọc</b-button>
            </b-col>
          </b-row>
        </b-card>

        <b-card class="shadow-sm">
          <b-table
            bordered
            hover
            responsive
            small
            show-empty
            :busy="purchaseBusy"
            :items="purchases"
            :fields="purchaseFields"
            empty-text="Không có dữ liệu"
          >
            <template #cell(index)="{ index }">
              {{ index + 1 + (purchaseList.current_page - 1) * purchaseList.per_page }}
            </template>

            <template #cell(companyName)="{ item }">
              <div class="font-weight-bold">{{ item.companyName || '—' }}</div>
              <small class="text-muted">{{ item.companyTaxcode || '—' }}</small>
            </template>

            <template #cell(packageName)="{ item }">
              <div>{{ item.packageName || '—' }}</div>
              <small class="text-muted">{{ item.paymentCode || '—' }}</small>
            </template>

            <template #cell(invoiceQuantity)="{ item }">
              {{ formatNumber(item.invoiceQuantity) }}
            </template>

            <template #cell(totalPrice)="{ item }">
              <span class="font-weight-bold">{{ formatCurrency(item.totalPrice) }}</span>
            </template>

            <template #cell(paymentMethod)="{ item }">
              <b-badge variant="info">{{ paymentMethodText(item.paymentMethod) }}</b-badge>
            </template>

            <template #cell(paymentStatus)="{ item }">
              <b-badge :variant="item.paymentStatus === 'SUCCESS' ? 'success' : 'secondary'">
                {{ item.paymentStatus === 'SUCCESS' ? 'Thành công' : item.paymentStatus || '—' }}
              </b-badge>
            </template>

            <template #cell(paidAt)="{ item }">
              {{ formatDateTime(item.paidAt || item.createdAt) }}
            </template>
          </b-table>

          <pagination-bar
            :current.sync="purchaseList.current_page"
            :size.sync="purchaseList.per_page"
            :total="purchaseList.total"
            :sizes="pageSizes"
            @page-change="onPurchasePageChange"
            @size-change="onPurchasePageSizeChange"
          />
        </b-card>
      </b-tab>
    </b-tabs>

    <b-modal ref="packageModal" :title="packageForm.id ? 'Cập nhật gói hóa đơn' : 'Thêm gói hóa đơn'" hide-footer size="lg">
      <b-form novalidate @submit.prevent="savePackage">
        <b-row>
          <b-col md="8">
            <b-form-group label="Tên gói" label-class="font-weight-bold" :state="state('name')">
              <b-form-input v-model.trim="packageForm.name" required placeholder="VD: Bill #1" :state="state('name')" />
              <b-form-invalid-feedback :state="state('name')">
                {{ invalidFeedback('name') }}
              </b-form-invalid-feedback>
            </b-form-group>
          </b-col>
          <b-col md="4">
            <b-form-group label="Thứ tự" label-class="font-weight-bold" :state="state('displayOrder')">
              <b-form-input v-model.number="packageForm.displayOrder" type="number" min="0" :state="state('displayOrder')" />
              <b-form-invalid-feedback :state="state('displayOrder')">
                {{ invalidFeedback('displayOrder') }}
              </b-form-invalid-feedback>
            </b-form-group>
          </b-col>
          <b-col md="4">
            <b-form-group label="Số hóa đơn" label-class="font-weight-bold" :state="state('invoiceQuantity')">
              <b-form-input v-model.number="packageForm.invoiceQuantity" type="number" min="1" required @input="updateTotalPrice" :state="state('invoiceQuantity')" />
              <b-form-invalid-feedback :state="state('invoiceQuantity')">
                {{ invalidFeedback('invoiceQuantity') }}
              </b-form-invalid-feedback>
            </b-form-group>
          </b-col>
          <b-col md="4">
            <b-form-group label="Đơn giá" label-class="font-weight-bold" :state="state('unitPrice')">
              <b-form-input v-model.number="packageForm.unitPrice" type="number" min="0" required @input="updateTotalPrice" :state="state('unitPrice')" />
              <b-form-invalid-feedback :state="state('unitPrice')">
                {{ invalidFeedback('unitPrice') }}
              </b-form-invalid-feedback>
            </b-form-group>
          </b-col>
          <b-col md="4">
            <b-form-group label="Thành tiền" label-class="font-weight-bold" :state="state('totalPrice')">
              <b-form-input v-model.number="packageForm.totalPrice" type="number" min="0" required :state="state('totalPrice')" />
              <b-form-invalid-feedback :state="state('totalPrice')">
                {{ invalidFeedback('totalPrice') }}
              </b-form-invalid-feedback>
            </b-form-group>
          </b-col>
          <b-col md="6">
            <b-form-group label="Trạng thái" label-class="font-weight-bold">
              <b-form-select v-model="packageForm.status" :options="statusOptions" />
            </b-form-group>
          </b-col>
          <b-col md="6">
            <b-form-group label="Gói trải nghiệm" label-class="font-weight-bold">
              <b-form-checkbox v-model="packageForm.includeTrial" switch>
                Bao gồm gói trải nghiệm
              </b-form-checkbox>
            </b-form-group>
          </b-col>
          <b-col cols="12">
            <b-form-group label="Mô tả" label-class="font-weight-bold">
              <b-form-textarea v-model.trim="packageForm.description" rows="3" />
            </b-form-group>
          </b-col>
        </b-row>

        <div class="text-right">
          <b-button type="submit" variant="primary" :disabled="saving">
            <i class="fas fa-save mr-1"></i>
            Lưu
          </b-button>
          <b-button variant="secondary" class="ml-2" @click="$refs.packageModal.hide()">Hủy</b-button>
        </div>
      </b-form>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import { pageItems, pageTotal } from '@/utils/pagination'
import PaginationBar from '@/views/components/pagination_bar.vue'
import vSelect from 'vue-select'
import 'vue-select/dist/vue-select.css'

export default {
  name: 'AdminInvoicePackageList',
  components: { vSelect, PaginationBar },
  data() {
    return {
      packageBusy: false,
      purchaseBusy: false,
      saving: false,
      packages: [],
      purchases: [],
      companyOptions: [],
      packageFilter: {
        keyword: '',
        status: null,
      },
      purchaseFilter: {
        companyId: null,
        paymentMethod: null,
        fromDate: '',
        toDate: '',
      },
      packageForm: this.emptyPackageForm(),
      packageErrors: {},
      packageList: {
        current_page: 1,
        per_page: 10,
        total: 0,
      },
      purchaseList: {
        current_page: 1,
        per_page: 10,
        total: 0,
      },
      pageSizes: [10, 20, 50, 100],
      statusOptions: [
        { value: 1, text: 'Kích hoạt' },
        { value: 0, text: 'Ngưng hoạt động' },
      ],
      statusFilterOptions: [
        { value: null, text: 'Tất cả trạng thái' },
        { value: 1, text: 'Kích hoạt' },
        { value: 0, text: 'Ngưng hoạt động' },
      ],
      paymentMethodOptions: [
        { value: null, text: 'Tất cả thanh toán' },
        { value: 'MOMO', text: 'MoMo (tất cả)' },
        { value: 'MOMO_WALLET', text: 'MoMo ví điện tử' },
        { value: 'MOMO_ATM', text: 'MoMo ATM nội địa' },
        { value: 'MOMO_CREDIT', text: 'MoMo thẻ quốc tế' },
        { value: 'MOMO_PAY_LATER', text: 'MoMo trả sau' },
        { value: 'VNPAY', text: 'VNPAY' },
        { value: 'ZALOPAY', text: 'ZaloPay' },
      ],
      packageFields: [
        { key: 'index', label: '#', thStyle: { width: '60px' }, tdClass: 'text-center' },
        { key: 'name', label: 'Gói hóa đơn', thStyle: { minWidth: '220px' } },
        { key: 'invoiceQuantity', label: 'Số hóa đơn', thStyle: { width: '150px' } },
        { key: 'unitPrice', label: 'Đơn giá', thStyle: { width: '140px' } },
        { key: 'totalPrice', label: 'Thành tiền', thStyle: { width: '150px' } },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '130px' }, tdClass: 'text-center' },
        { key: 'option', label: '', thStyle: { width: '80px' }, tdClass: 'text-center' },
      ],
      purchaseFields: [
        { key: 'index', label: '#', thStyle: { width: '60px' }, tdClass: 'text-center' },
        { key: 'companyName', label: 'Công ty', thStyle: { minWidth: '240px' } },
        { key: 'packageName', label: 'Gói / mã giao dịch', thStyle: { minWidth: '220px' } },
        { key: 'invoiceQuantity', label: 'Số hóa đơn', thStyle: { width: '120px' }, tdClass: 'text-right' },
        { key: 'totalPrice', label: 'Thành tiền', thStyle: { width: '150px' }, tdClass: 'text-right' },
        { key: 'paymentMethod', label: 'Thanh toán', thStyle: { width: '110px' }, tdClass: 'text-center' },
        { key: 'paymentStatus', label: 'Trạng thái', thStyle: { width: '120px' }, tdClass: 'text-center' },
        { key: 'paidAt', label: 'Ngày mua', thStyle: { width: '160px' } },
      ],
    }
  },
  mounted() {
    this.loadCompanies()
    this.loadPackages()
    this.loadPurchases()
  },
  methods: {
    emptyPackageForm() {
      return {
        id: null,
        name: '',
        invoiceQuantity: 500,
        unitPrice: 600,
        totalPrice: 300000,
        includeTrial: true,
        description: 'Bao gồm gói trải nghiệm',
        status: 1,
        displayOrder: 0,
      }
    },
    async reloadAll() {
      await Promise.all([this.loadPackages(), this.loadPurchases(), this.loadCompanies()])
    },
    async loadPackages() {
      this.packageBusy = true
      try {
        const page = this.packageList.current_page - 1
        const { data } = await axios.post('/administrator/invoice-packages/list', this.packageFilter, {
          params: { page, size: this.packageList.per_page },
          meta: { suppressGlobalErrorToast: true },
        })
        this.packages = pageItems(data)
        this.packageList.total = pageTotal(data)
      } catch (err) {
        this.packages = []
        this.packageList.total = 0
        const message = err?.message === 'Network Error'
          ? 'Không kết nối được backend gói hóa đơn'
          : err?.response?.data?.message || 'Không thể tải danh sách gói hóa đơn'
        this.$toastr && this.$toastr.error(message)
      } finally {
        this.packageBusy = false
      }
    },
    applyPackageFilter() {
      this.packageList.current_page = 1
      this.loadPackages()
    },
    onPackagePageChange(page) {
      this.packageList.current_page = Number(page) || 1
      this.loadPackages()
    },
    onPackagePageSizeChange(size) {
      this.packageList.per_page = Number(size) || this.packageList.per_page
      this.packageList.current_page = 1
      this.loadPackages()
    },
    openPackageModal(item) {
      this.packageForm = item ? { ...item } : this.emptyPackageForm()
      this.packageErrors = {}
      this.$refs.packageModal.show()
    },
    updateTotalPrice() {
      const quantity = Number(this.packageForm.invoiceQuantity || 0)
      const unitPrice = Number(this.packageForm.unitPrice || 0)
      this.packageForm.totalPrice = quantity * unitPrice
    },
    async savePackage() {
      if (!this.validatePackageForm()) return
      this.saving = true
      try {
        await axios.post('/administrator/invoice-packages/save', this.packageForm)
        this.$toastr && this.$toastr.success('Đã lưu gói hóa đơn')
        this.$refs.packageModal.hide()
        this.loadPackages()
      } catch (err) {
        const message = err?.response?.data?.message || 'Không thể lưu gói hóa đơn'
        this.$toastr && this.$toastr.error(message)
      } finally {
        this.saving = false
      }
    },
    validatePackageForm() {
      const errors = {}
      if (!String(this.packageForm.name || '').trim()) {
        errors.name = ['Vui lòng nhập tên gói']
      }
      const invoiceQuantity = Number(this.packageForm.invoiceQuantity)
      if (!Number.isFinite(invoiceQuantity) || invoiceQuantity < 1) {
        errors.invoiceQuantity = ['Số hóa đơn phải lớn hơn 0']
      }
      const unitPrice = Number(this.packageForm.unitPrice)
      if (!Number.isFinite(unitPrice) || unitPrice < 0) {
        errors.unitPrice = ['Đơn giá không được âm']
      }
      const totalPrice = Number(this.packageForm.totalPrice)
      if (!Number.isFinite(totalPrice) || totalPrice < 0) {
        errors.totalPrice = ['Thành tiền không được âm']
      }
      const displayOrder = Number(this.packageForm.displayOrder || 0)
      if (!Number.isFinite(displayOrder) || displayOrder < 0) {
        errors.displayOrder = ['Thứ tự không được âm']
      }
      this.packageErrors = errors
      return Object.keys(errors).length === 0
    },
    state(field) {
      return Object.prototype.hasOwnProperty.call(this.packageErrors, field) ? false : null
    },
    invalidFeedback(field) {
      const value = this.packageErrors[field]
      return Array.isArray(value) ? value.join(' ') : (value || '')
    },
    async deletePackage(item) {
      const ok = await this.$bvModal.msgBoxConfirm(`Xóa gói ${item.name}?`, {
        title: 'Xóa gói hóa đơn',
        okTitle: 'Xóa',
        cancelTitle: 'Hủy',
        okVariant: 'danger',
        centered: true,
      })
      if (!ok) return
      try {
        await axios.post('/administrator/invoice-packages/delete', { id: item.id })
        this.$toastr && this.$toastr.success('Đã ngưng hoạt động gói hóa đơn')
        this.loadPackages()
      } catch (err) {
        const message = err?.response?.data?.message || 'Không thể xóa gói hóa đơn'
        this.$toastr && this.$toastr.error(message)
      }
    },
    async loadPurchases() {
      this.purchaseBusy = true
      try {
        const page = this.purchaseList.current_page - 1
        const { data } = await axios.post('/administrator/invoice-packages/purchases', this.normalizedPurchaseFilter(), {
          params: { page, size: this.purchaseList.per_page },
          meta: { suppressGlobalErrorToast: true },
        })
        this.purchases = pageItems(data)
        this.purchaseList.total = pageTotal(data)
      } catch (err) {
        this.purchases = []
        this.purchaseList.total = 0
        const message = err?.message === 'Network Error'
          ? 'Không kết nối được backend lịch sử mua gói'
          : err?.response?.data?.message || 'Không thể tải lịch sử mua gói'
        this.$toastr && this.$toastr.error(message)
      } finally {
        this.purchaseBusy = false
      }
    },
    applyPurchaseFilter() {
      this.purchaseList.current_page = 1
      this.loadPurchases()
    },
    resetPurchaseFilter() {
      this.purchaseFilter = {
        companyId: null,
        paymentMethod: null,
        fromDate: '',
        toDate: '',
      }
      this.applyPurchaseFilter()
    },
    onPurchasePageChange(page) {
      this.purchaseList.current_page = Number(page) || 1
      this.loadPurchases()
    },
    onPurchasePageSizeChange(size) {
      this.purchaseList.per_page = Number(size) || this.purchaseList.per_page
      this.purchaseList.current_page = 1
      this.loadPurchases()
    },
    normalizedPurchaseFilter() {
      return {
        companyId: this.purchaseFilter.companyId || null,
        paymentMethod: this.purchaseFilter.paymentMethod || null,
        fromDate: this.purchaseFilter.fromDate || null,
        toDate: this.purchaseFilter.toDate || null,
      }
    },
    async loadCompanies() {
      try {
        const { data } = await axios.post('/administrator/company/list', {}, {
          params: { page: 0, size: 5000 },
          meta: { suppressGlobalErrorToast: true },
        })
        this.companyOptions = (data.content || []).map(c => ({
          value: c.id,
          label: c.name || `#${c.id}`,
        }))
      } catch {
        this.companyOptions = []
      }
    },
    formatCurrency(value) {
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value || 0))
    },
    formatNumber(value) {
      return new Intl.NumberFormat('vi-VN').format(Number(value || 0))
    },
    paymentMethodText(method) {
      const normalized = String(method || '').toUpperCase()
      if (normalized === 'MOMO') return 'MoMo'
      if (normalized === 'MOMO_WALLET') return 'MoMo ví điện tử'
      if (normalized === 'MOMO_PAY_LATER') return 'MoMo trả sau'
      if (normalized === 'MOMO_ATM') return 'MoMo ATM nội địa'
      if (normalized === 'MOMO_CREDIT') return 'MoMo thẻ quốc tế'
      if (normalized === 'VNPAY') return 'VNPAY'
      if (normalized === 'ZALOPAY') return 'ZaloPay'
      return method || '—'
    },
    formatDateTime(value) {
      if (!value) return '—'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
      const dd = String(date.getDate()).padStart(2, '0')
      const mm = String(date.getMonth() + 1).padStart(2, '0')
      const yyyy = date.getFullYear()
      const hh = String(date.getHours()).padStart(2, '0')
      const mi = String(date.getMinutes()).padStart(2, '0')
      return `${dd}/${mm}/${yyyy} ${hh}:${mi}`
    },
  },
}
</script>

<style scoped>
.invoice-package-admin .card {
  border-radius: 8px;
}

.invoice-package-admin .table th {
  background: #f7f9fc;
  color: #4a5568;
  vertical-align: middle;
}

.invoice-package-admin .table td {
  vertical-align: middle;
}
</style>
