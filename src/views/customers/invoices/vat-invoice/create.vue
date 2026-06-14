<template>
  <div class="invoice-container">
    <b-card class="invoice-card">
      <!-- Tiêu đề -->
      <div class="invoice-header">
        <h4 class="invoice-title">Lập hóa đơn GTGT</h4>
      </div>

      <!-- Đã bỏ cảnh báo inline; dùng toastr để hiển thị lỗi -->
      
      <!-- Khung tải -->
      <div v-if="isBusy" class="loading-skeleton">
        <b-skeleton width="100%" height="20px" animated class="mb-2" />
        <b-skeleton width="96%" height="20px" animated class="mb-2" />
        <b-skeleton width="92%" height="20px" animated class="mb-2" />
      </div>

      <div v-else>
        <!-- 1. Thông tin hóa đơn -->
        <div class="form-section">
          <h5 class="section-title">
            <i class="fas fa-file-invoice mr-2"></i>Thông tin hóa đơn
          </h5>
          <b-row>
            <b-col lg="4" md="6">
              <b-form-group label="Tên hóa đơn" label-class="form-label">
                <b-form-input :value="formInvoices.name || '—'" readonly />
              </b-form-group>
            </b-col>
            <b-col lg="4" md="6">
              <b-form-group label="Hình thức" label-class="form-label">
                <b-form-input :value="haveCodeLabel" readonly />
              </b-form-group>
            </b-col>
            <b-col lg="4" md="6">
              <b-form-group label="Mã đơn hàng" label-class="form-label">
                <b-form-input :value="frmData.order.code || ''" />
              </b-form-group>
            </b-col>
          </b-row>
          <b-row>
            <b-col lg="4" md="6">
              <b-form-group label="Ký hiệu" label-class="form-label">
                <b-input-group>
                  <b-input-group-prepend>
                    <span class="input-group-text input-code-text">
                      {{ formInvoices.form_code || prepare.formCode || '—' }}
                    </span>
                  </b-input-group-prepend>
                  <b-form-input class="serial-input" :value="formInvoices.serial || prepare.serial || '—'" readonly />
                </b-input-group>
              </b-form-group>
            </b-col>
            <b-col lg="4" md="6">
              <b-form-group label="Số hóa đơn" label-class="form-label">
                <b-form-input :value="frmData.no || '0'" readonly />
              </b-form-group>
            </b-col>
            <b-col lg="4" md="6">
              <b-form-group label="Ngày lập" label-class="form-label">
                <b-form-input type="date" v-model="frmData.date_export" />
              </b-form-group>
            </b-col>
          </b-row>
        </div>

        <!-- 2. Thông tin bên bán -->
        <div class="form-section">
          <h5 class="section-title">
            <i class="fas fa-building mr-2"></i>Thông tin bên bán
          </h5>
          <b-row>
            <b-col md="6">
              <b-form-group label="Đơn vị bán hàng" label-class="form-label">
                <b-form-input :value="companies.name || '—'" readonly />
              </b-form-group>
            </b-col>
            <b-col md="6">
              <b-form-group label="Mã số thuế" label-class="form-label">
                <b-form-input :value="companies.taxcode || '—'" readonly />
              </b-form-group>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <b-form-group label="Địa chỉ" label-class="form-label">
                <b-form-input :value="companies.address || '—'" readonly />
              </b-form-group>
            </b-col>
          </b-row>
        </div>

        <!-- 3. Thông tin bên mua -->
        <div class="form-section">
          <h5 class="section-title">
            <i class="fas fa-user-tie mr-2"></i>Thông tin bên mua
          </h5>
          <b-row>
            <b-col lg="4" md="6">
              <b-form-group label="Mã khách hàng" label-class="form-label">
                <b-form-input
                  v-model="frmData.customer.code"
                  list="autoCustomerCode"
                  placeholder="Nhập hoặc chọn mã khách hàng"
                  :disabled="loadingCustomers"
                  @change="onCustomerCodeChange"
                />
              </b-form-group>
            </b-col>
            <b-col lg="4" md="6">
              <b-form-group label="Đơn vị mua" label-class="form-label">
                <b-form-input v-model="frmData.customer.name" />
              </b-form-group>
            </b-col>
            <b-col lg="4" md="6">
              <b-form-group label="Người mua" label-class="form-label">
                <b-form-input v-model="frmData.customer.buyer" />
              </b-form-group>
            </b-col>
          </b-row>
          <b-row>
            <b-col lg="4" md="6">
              <b-form-group label="Mã số thuế" label-class="form-label">
                <b-form-input v-model="frmData.customer.taxcode" />
              </b-form-group>
            </b-col>
            <b-col lg="4" md="6">
              <b-form-group label="Email" label-class="form-label">
                <b-form-input type="email" v-model="frmData.customer.email" />
              </b-form-group>
            </b-col>
            <b-col lg="4" md="6">
              <b-form-group label="Điện thoại" label-class="form-label">
                <b-form-input v-model="frmData.customer.phone" />
              </b-form-group>
            </b-col>
          </b-row>
          <b-row>
            <b-col md="12">
              <b-form-group label="Địa chỉ" label-class="form-label">
                <b-form-input v-model="frmData.customer.address" />
              </b-form-group>
            </b-col>
          </b-row>
          <b-row>
            <b-col lg="4" md="6">
              <b-form-group label="Ngân hàng" label-class="form-label">
                <b-form-input v-model="frmData.customer.bank_name" />
              </b-form-group>
            </b-col>
            <b-col lg="4" md="6">
              <b-form-group label="Số tài khoản" label-class="form-label">
                <b-form-input v-model="frmData.customer.bank_no" />
              </b-form-group>
            </b-col>
            <b-col lg="4" md="6">
              <b-form-group label="Hình thức thanh toán" label-class="form-label">
                <b-form-select v-model="frmData.payment_type" :options="paymentTypeOptions" />
              </b-form-group>
            </b-col>
          </b-row>

          <datalist v-if="!loadingCustomers" id="autoCustomerCode">
            <option v-for="item in customersRaw" :key="item.code" :value="item.code">
              {{ (item.companyName || item.buyerName || '') + ' - MST: ' + (item.taxCode || item.taxcode || 'N/A') }}
            </option>
          </datalist>
        </div>

        <!-- 4. Chi tiết hàng hóa/dịch vụ -->
        <div class="form-section">
          <h5 class="section-title">
            <i class="fas fa-boxes mr-2"></i>Chi tiết hàng hóa/Dịch vụ
          </h5>
          <div class="table-responsive">
            <b-table
              id="tblinv"
              ref="tblinv"
              :busy.sync="isBusyDetail"
              :fields="loadColumnTable()"
              :items="frmData.detail"
              responsive
              hover
              outlined
              bordered
              class="invoice-table"
              tbody-tr-class="inv-record"
            >
              <template #table-busy>
                <b-skeleton-table :rows="1" :columns="10" hide-header />
              </template>

              <template #cell(num)="data">
                <div v-show="frmData.detail[data.index].name !== null" class="text-center">
                  <b-form-checkbox v-model="frmData.detail[data.index].isNum">
                    <span>{{ frmData.detail[data.index].num }}</span>
                  </b-form-checkbox>
                </div>
              </template>

              <template #cell(name)="data">
                <b-form-input
                  v-if="!frmData.detail[data.index].hidden"
                  v-model="frmData.detail[data.index].name"
                  class="table-input"
                  @focus="checkProductUpdated"
                  @change="onName(data.index)"
                  @keyup.alt.enter="onNewLineName(data.index)"
                  list="autoNameProduct"
                  :ref="'nameTextarea' + data.index"
                  :disabled="frmData.detail[data.index].disable || false"
                />
                <b-form-textarea
                  v-else
                  size="sm"
                  max-rows="20"
                  v-model="frmData.detail[data.index].name"
                  class="table-input"
                  @change="onName(data.index)"
                  @keyup.alt.enter="onNewLineName(data.index)"
                  :ref="'nameTextarea' + data.index"
                  :disabled="frmData.detail[data.index].disable || false"
                />
              </template>

              <template #cell(code)="data">
                <b-form-input 
                  v-model="frmData.detail[data.index].code" 
                  class="table-input" 
                  :disabled="frmData.detail[data.index].disable || false"
                />
              </template>

              <template #cell(unit)="data">
                <b-form-input 
                  v-model="frmData.detail[data.index].unit" 
                  class="table-input text-center" 
                  :disabled="frmData.detail[data.index].disable || false"
                />
              </template>

              <template #cell(quantity)="data">
                <b-form-input 
                  v-model="frmData.detail[data.index].quantity" 
                  class="table-input text-center" 
                  :formatter="splitNumberDecimal" 
                  @change="onQuantity(data.index)" 
                  :disabled="frmData.detail[data.index].disable || false"
                />
              </template>

              <template #cell(price)="data">
                <b-form-input 
                  v-model="frmData.detail[data.index].price" 
                  class="table-input text-right" 
                  :formatter="splitNumberDecimal" 
                  @change="onPrice(data.index)" 
                  :disabled="frmData.detail[data.index].disable || false"
                />
              </template>

              <template #cell(total)="data">
                <b-form-input 
                  v-model="frmData.detail[data.index].total" 
                  class="table-input text-right" 
                  :formatter="splitNumberDecimal" 
                  @change="onTotal(data.index)" 
                  :disabled="frmData.detail[data.index].disable || false || frmData.action === 'adjust' || frmData.type_create === 'adjust'"
                />
              </template>

              <template #cell(vatRate)="data">
                <b-form-select 
                  size="sm" 
                  v-model="frmData.detail[data.index].vatRate" 
                  class="table-select" 
                  @change="onVatRate($event, data.index)" 
                  :disabled="frmData.detail[data.index].disable || false"
                >
                  <option v-for="item in app.optionVatRates" :key="item.code" :value="item.code">
                    {{ item.label }}
                  </option>
                </b-form-select>
                <div v-show="frmData.detail[data.index].vatRate === -3" class="vat-other-input">
                  <b-form-input 
                    v-model="frmData.detail[data.index].vatRateOther" 
                    class="table-input text-right" 
                    @change="onVatRateOther($event, data.index)"
                  />
                  <span class="vat-percent">%</span>
                </div>
              </template>

              <template #cell(vatAmount)="data">
                <b-form-input 
                  v-model="frmData.detail[data.index].vatAmount" 
                  class="table-input text-right" 
                  :formatter="splitNumberDecimal" 
                  @change="onVatAmount(data.index)" 
                  :disabled="frmData.detail[data.index].disable || false"
                />
              </template>

              <template #cell(amount)="data">
                <b-form-input 
                  v-model="frmData.detail[data.index].amount" 
                  class="table-input text-right" 
                  :formatter="splitNumberDecimal" 
                  @change="onInputAmount(data.index)" 
                  :disabled="frmData.detail[data.index].disable || false"
                />
              </template>

              <template #cell(feature)="data">
                <b-form-select 
                  size="sm" 
                  v-model="frmData.detail[data.index].feature" 
                  class="table-select" 
                  @change="onFeature($event, data.index)" 
                  :disabled="frmData.detail[data.index].disable || false"
                >
                  <option v-for="item in feature" :key="item.value" :value="item.value">
                    {{ item.text }}
                  </option>
                </b-form-select>
              </template>

              <template #cell(delete)="data">
                <b-button variant="outline-danger" size="sm" @click="onDelete(data.index)">
                  <i class="far fa-trash-alt"></i>
                </b-button>
              </template>
            </b-table>
          </div>

          <datalist v-if="!loadingProduct" id="autoNameProduct">
            <option v-for="item in app.product" :key="item.code" :value="item.name">
              {{ 'Mã: ' + item.code + ' Giá: ' + splitNumber(item.price) + 'đ' }}
            </option>
          </datalist>
          
          <p v-if="!state('detail')" class="text-danger small mt-2">
            {{ invalidFeedback('detail') }}
          </p>
        </div>

        <!-- 5. Tổng hợp -->
        <div class="form-section summary-section">
          <h5 class="section-title">
            <i class="fas fa-calculator mr-2"></i>Tổng hợp
          </h5>
          <b-row>
            <b-col lg="4" md="6">
              <b-form-group label="Tổng tiền hàng" label-class="form-label summary-label">
                <b-form-input :value="splitNumber(frmData.total)" readonly class="summary-input" />
              </b-form-group>
            </b-col>
            <b-col lg="4" md="6">
              <b-form-group label="Tổng tiền thuế" label-class="form-label summary-label">
                <b-form-input :value="splitNumber(frmData.vat_amount)" readonly class="summary-input" />
              </b-form-group>
            </b-col>
            <b-col lg="4" md="6">
              <b-form-group label="Tổng tiền thanh toán" label-class="form-label summary-label">
                <b-form-input :value="splitNumber(frmData.amount)" readonly class="summary-input total-amount" />
              </b-form-group>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <b-form-group label="Số tiền bằng chữ" label-class="form-label summary-label">
                <b-form-input :value="frmData.amount_in_words" readonly class="summary-input" />
              </b-form-group>
            </b-col>
          </b-row>
        </div>

        <!-- Nút thao tác -->
        <div class="action-buttons">
		  <b-button size="sm" variant="outline-secondary" @click="goBack">
		    <i class="fas fa-arrow-left mr-1"></i> Quay lại
		  </b-button>
          <b-button
            v-if="canEdit"
            variant="success"
            @click="onSubmit"
          >
            <i class="fas fa-save mr-1"></i> Lưu và lập hóa đơn
          </b-button>
        </div>
      </div>
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'VatInvoiceCreate',
  data () {
    return {
      isBusy: false,
      isBusyDetail: false,
      error: null,
      // Theo dõi key lỗi đã hiển thị để tránh toast trùng
      shownErrorKeys: new Set(),
      prepare: { formId: null, formCode: null, serial: null, haveCode: null, registerId: null, registerEffectiveDate: null },
      formInvoices: { name: null, form_code: null, serial: null, have_code: null },
      companies: { name: null, taxcode: null, address: null },
      loadingCustomers: false,
      customerOptions: [],
      // keep raw customers for lookup by code
      customersRaw: [],
      loadingProduct: false,
      feature: [
        { value: 1, text: 'HH, DV' },
        { value: 2, text: 'KM' },
        { value: 3, text: 'CK' },
        { value: 4, text: 'Ghi chú' }
      ],
      app: {
        optionVatRates: [],
        product: []
      },
      frmData: {
        action: null,
        type_create: null,
        order: { code: null },
        no: null,
        date_export: null,
        customer: {
          code: null,
          name: null,
          buyer: null,
          taxcode: null,
          address: null,
          email: null,
          phone: null,
          bank_name: null,
          bank_no: null
        },
        payment_type: 1,
        detail: [
          {
            num: 1,
            isNum: true,
            hidden: false,
            disable: false,
            name: '',
            code: '',
            unit: '',
            quantity: 0,
            price: 0,
            total: 0,
            vatRate: -1,
            vatRateOther: 0,
            vatAmount: 0,
            amount: 0,
            feature: 1
          }
        ],
        total: 0,
        vat_amount: 0,
        amount: 0,
        amount_in_words: ''
      },
      paymentTypeOptions: [
        { value: 1, text: 'Tiền mặt' },
        { value: 2, text: 'Chuyển khoản' },
        { value: 3, text: 'Tiền mặt/Chuyển khoản' }
      ],
      company_id: null,
      editId: null,
      loadedInvoice: null,
      invoiceStatus: 0
    }
  },
  computed: {
    haveCodeLabel () {
      const v = this.formInvoices.have_code != null ? this.formInvoices.have_code : this.prepare && this.prepare.haveCode
      if (v === 1) return 'Có mã của cơ quan thuế'
      if (v === 2 || v === 0) return 'Không có mã của cơ quan thuế'
      return '—'
    },
    canEdit () {
      // Cho phép tạo mới (không có editId) hoặc chỉnh sửa khi status = 0
      if (!this.editId) return true
      return Number(this.invoiceStatus) === 0
    }
  },
  async created () { 
    // set default 'Ngày lập' to today's local date
    this.frmData.date_export = this.formatLocalDateYYYYMMDD(new Date())
    // detect edit mode from route: /invoice/:id/edit or params.id
    const rid = this.$route && (this.$route.params?.id || this.$route.query?.id)
    const id = rid ? Number(rid) : null
    if (Number.isFinite(id) && id > 0) {
      this.editId = id
    }
    const ready = await this.checkPrerequisites()
    if (!ready) return
    if (this.editId) this.loadInvoice()
  },
  methods: {
    // Unified error toast with deduplication by key
    toastError (message, key = null) {
      const k = key || message
      if (this.shownErrorKeys.has(k)) return
      this.shownErrorKeys.add(k)
      if (this.$toastr && typeof this.$toastr.error === 'function') {
        this.$toastr.error(message, 'Lỗi')
      }
    },
    // Định dạng ngày thành YYYY-MM-DD theo giờ local
    formatLocalDateYYYYMMDD (d = new Date()) {
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    // Chuẩn hóa thuế suất VAT thành số hoặc null
    normalizeVatRate (v) {
      if (v === null || v === undefined || v === '') return null
      const n = Number(v)
      return Number.isNaN(n) ? null : n
    },
    // Ánh xạ sản phẩm thô sang các trường chuẩn dùng trong dòng hóa đơn
    normalizeProduct (item) {
      if (!item) return null
      return {
        code: item.code || item.product_code || item.sku || '',
        name: item.name || item.product_name || '',
        unit: item.unit || item.unit_name || item.uom || '',
        price: Number(item.price ?? item.sale_price ?? item.unit_price ?? 0),
        vatRate: this.normalizeVatRate(item.vatRate ?? item.vat_rate ?? item.taxRate ?? item.tax_rate),
        feature: item.feature != null ? Number(item.feature) : (item.type != null ? Number(item.type) : 1)
      }
    },
    // Tìm sản phẩm theo tên chính xác trước, sau đó theo mã, rồi theo tên chứa chuỗi (không phân biệt hoa thường)
    findProductByNameOrCode (input) {
      const s = String(input || '').trim().toLowerCase()
      if (!s) return null
      const arr = Array.isArray(this.app.product) ? this.app.product : []
      let found = arr.find(p => String(p?.name || '').toLowerCase() === s)
      if (!found) found = arr.find(p => String(p?.code || '').toLowerCase() === s)
      if (!found) found = arr.find(p => String(p?.name || '').toLowerCase().includes(s))
      return this.normalizeProduct(found)
    },
    // Áp dữ liệu sản phẩm đã chuẩn hóa vào dòng bảng và tính lại
    applyProductToRow (p, idx) {
      if (!p || idx == null) return
      const r = this.frmData.detail[idx]
      if (!r) return
      r.code = p.code || r.code
      r.name = p.name || r.name
      r.unit = p.unit || r.unit
      if (!r.quantity || Number(r.quantity) <= 0) r.quantity = 1
      if (p.price != null) r.price = p.price
      if (p.vatRate != null) r.vatRate = p.vatRate
      if (p.feature != null) r.feature = p.feature
      // recalc based on quantity and price
      this.recalcRowFromQuantityPrice(idx)
    },
    async checkPrerequisites () {
      this.isBusy = true
      this.error = null
      try {
        const { data: prep } = await axios.get('/invoices/prepare')
        this.prepare = { ...this.prepare, ...prep }

        const { data: profile } = await axios.post('/setting/profile/get')
        this.companies = {
          name: profile.companyName || null,
          taxcode: profile.taxCode || null,
          address: profile.companyAddress || null
        }
        // detect company_id
        this.detectCompanyId(profile, prep)

        const { data: formList } = await axios.get('/form-invoices', { params: { category: 1, status: 1, size: 1, page: 1 } })
        const item = (formList && formList.items && formList.items[0]) || null
        if (!item) {
          throw new Error('Chưa có mẫu hóa đơn GTGT được kích hoạt')
        }
        this.formInvoices.name = item.name || null
        this.formInvoices.form_code = this.prepare.formCode || item.formCode || null
        this.formInvoices.serial = this.prepare.serial || item.serial || null
        this.formInvoices.have_code = this.prepare.haveCode != null ? this.prepare.haveCode : item.haveCode

        // customers by company_id
        await this.refreshCustomers()

        // products
        this.loadingProduct = true
        const { data: productsPage } = await axios.post('/categories/product/list', {}, { params: { page: 0, size: 200 } })
        this.app.product = (productsPage && productsPage.data) ? productsPage.data : []
        this.loadingProduct = false

        // VAT rates
        const { data: vatRates } = await axios.get('/categories/product/vat-rates')
        this.app.optionVatRates = Array.isArray(vatRates) ? vatRates : []
      } catch (e) {
        const msg = e && e.response && e.response.data && e.response.data.message
          ? e.response.data.message
          : 'Không đủ điều kiện để lập hóa đơn'
        this.error = msg
        this.toastError(msg, 'checkPrerequisites')
        this.$router.replace({ name: 'CustomerVatInvoiceList' })
        return false
      } finally {
        this.isBusy = false
      }
      return true
    },
    detectCompanyId (profile, prep) {
      // auto detect company_id based on user context
      // priority: profile.companyId -> prepare.companyId -> null
      const fromProfile = profile && (profile.companyId || profile.company_id)
      const fromPrep = prep && (prep.companyId || prep.company_id)
      this.company_id = fromProfile || fromPrep || null
    },
    async refreshCustomers () {
      this.loadingCustomers = true
      try {
        const payload = this.company_id ? { company_id: this.company_id } : {}
        const { data: customersPage } = await axios.post('/categories/customer/list', payload, { params: { page: 0, size: 100 } })
        const customers = customersPage && customersPage.data ? customersPage.data : []
        // store full customers for later lookup
        this.customersRaw = customers
        // Định dạng cho v-select: mảng đối tượng có giá trị và chữ hiển thị.
        const opts = customers.map(c => ({
          value: c.code,
          text: `${c.code} - ${(c.companyName || c.buyerName || '')}`.trim()
        }))
        this.customerOptions = opts
      } finally {
        this.loadingCustomers = false
      }
    },
    // populate customer fields when selecting a customer code
    onCustomerSelected (code) {
      const s = String(code || '').trim()
      if (!s) return
      const c = (this.customersRaw || []).find(x => String(x.code || '').trim() === s)
      if (!c) return
      // prefer companyName/buyerName, then fallback
      this.frmData.customer.code = c.code || this.frmData.customer.code
      this.frmData.customer.name = c.companyName || c.buyerName || this.frmData.customer.name
      this.frmData.customer.buyer = c.buyerName || this.frmData.customer.buyer
      this.frmData.customer.taxcode = c.taxCode || c.taxcode || this.frmData.customer.taxcode
      this.frmData.customer.address = c.companyAddress || c.address || this.frmData.customer.address
      this.frmData.customer.email = c.email || this.frmData.customer.email
      this.frmData.customer.phone = c.phone || this.frmData.customer.phone
      this.frmData.customer.bank_name = c.bankName || c.bank_name || this.frmData.customer.bank_name
      this.frmData.customer.bank_no = c.bankAccountNumber || c.bank_no || this.frmData.customer.bank_no
    },
    // Xử lý thay đổi mã khách hàng (cho datalist)
    onCustomerCodeChange () {
      const code = String(this.frmData.customer.code || '').trim()
      if (!code) return
      // Try to find customer by exact code match
      const c = (this.customersRaw || []).find(x => String(x.code || '').trim() === code)
      if (c) {
        // Auto-fill customer fields
        this.onCustomerSelected(code)
      }
    },
    // ensure there is always an empty row at the end
    ensureTrailingEmptyRow () {
      const rows = this.frmData.detail
      if (!Array.isArray(rows) || rows.length === 0) {
        this.frmData.detail = [this.createEmptyRow(1)]
        return
      }
      const last = rows[rows.length - 1]
      const isEmpty = !String(last.name || '').trim() && !String(last.code || '').trim()
      if (!isEmpty) {
        // append a new empty row
        this.frmData.detail.push(this.createEmptyRow(rows.length + 1))
      }
    },
    // helper to create a new empty row with next num
    createEmptyRow (num) {
      return {
        num: num,
        isNum: true,
        hidden: false,
        disable: false,
        name: '',
        code: '',
        unit: '',
        quantity: 0,
        price: 0,
        total: 0,
        vatRate: -1,
        vatRateOther: 0,
        vatAmount: 0,
        amount: 0,
        feature: 1
      }
    },
    // when name changes: try map product, recalc and ensure a trailing empty row exists
    onName (idx) {
      const name = this.frmData.detail[idx]?.name
      const p = this.findProductByNameOrCode(name)
      if (p) {
        this.applyProductToRow(p, idx)
      } else {
        // even if not matched, if quantity and price filled later, we still need totals
        this.recalcRowFromQuantityPrice(idx)
      }
      this.ensureTrailingEmptyRow()
    },
    // delete a row and renumber; keep at least one row
    onDelete (idx) {
      const rows = Array.isArray(this.frmData.detail) ? this.frmData.detail : []
      if (rows.length === 0) return
      if (idx < 0 || idx >= rows.length) return
      rows.splice(idx, 1)
      // if all removed, create one empty
      if (rows.length === 0) {
        rows.push(this.createEmptyRow(1))
      }
      // renumber
      rows.forEach((r, i) => { r.num = i + 1 })
      // ensure we keep a trailing empty row
      this.ensureTrailingEmptyRow()
      // recalc totals
      this.recalcTotals()
    },
    // Quay lại
    goBack () {
      if (this.$router && typeof this.$router.back === 'function') {
        this.$router.back()
      } else if (typeof window !== 'undefined' && window.history) {
        window.history.back()
      }
    },
    async loadInvoice () {
      try {
        this.isBusy = true
        const { data } = await axios.get(`/invoices/${this.editId}`)
        this.loadedInvoice = data
        this.invoiceStatus = data?.status ?? 0
        // Ánh xạ phản hồi về form
        this.frmData.no = data?.no ?? this.frmData.no
        this.frmData.date_export = data?.dateExport ?? this.frmData.date_export
        this.frmData.payment_type = data?.paymentType ?? this.frmData.payment_type
        this.frmData.order = { code: data?.orderCode ?? this.frmData.order?.code }
        // customer: object or null
        const cust = data?.customer || {}
        this.frmData.customer = {
          code: cust.code ?? this.frmData.customer.code,
          name: cust.name ?? this.frmData.customer.name,
          buyer: cust.buyer ?? this.frmData.customer.buyer,
          taxcode: cust.taxcode ?? cust.taxCode ?? this.frmData.customer.taxcode,
          address: cust.address ?? cust.companyAddress ?? this.frmData.customer.address,
          email: cust.email ?? this.frmData.customer.email,
          phone: cust.phone ?? this.frmData.customer.phone,
          bank_name: cust.bank_name ?? cust.bankName ?? this.frmData.customer.bank_name,
          bank_no: cust.bank_no ?? cust.bankAccountNumber ?? this.frmData.customer.bank_no
        }
        // detail rows: array
        const rows = Array.isArray(data?.detail) ? data.detail : []
        this.frmData.detail = rows.length > 0 ? rows.map((r, i) => ({
          num: r.num ?? (i + 1),
          isNum: r.isNum ?? true,
          hidden: r.hidden ?? false,
          disable: r.disable ?? false,
          name: r.name ?? '',
          code: r.code ?? '',
          unit: r.unit ?? '',
          quantity: Number(r.quantity ?? 0),
          price: Number(r.price ?? 0),
          total: Number(r.total ?? 0),
          vatRate: typeof r.vatRate === 'number' ? r.vatRate : -1,
          vatRateOther: Number(r.vatRateOther ?? 0),
          vatAmount: Number(r.vatAmount ?? 0),
          amount: Number(r.amount ?? 0),
          feature: Number(r.feature ?? 1)
        })) : this.frmData.detail
        // totals
        this.frmData.total = Number(data?.total ?? this.frmData.total)
        this.frmData.vat_amount = Number(data?.vatAmount ?? this.frmData.vat_amount)
        this.frmData.amount = Number(data?.amount ?? this.frmData.amount)
        this.frmData.amount_in_words = data?.amountInWords ?? this.frmData.amount_in_words
      } catch (e) {
        const msg = e?.response?.data?.message || 'Không tải được dữ liệu hóa đơn để chỉnh sửa'
        this.toastError(msg, 'load_invoice')
      } finally {
        this.isBusy = false
      }
    },
    async onSubmit () {
      try {
        const rows = Array.isArray(this.frmData.detail) ? this.frmData.detail.filter(r => String(r.name || '').trim()) : []
        if (rows.length === 0) {
          this.toastError('Vui lòng nhập ít nhất 1 dòng hàng hóa/dịch vụ', 'no_detail_rows')
          return
        }

        // Nếu đang ở chế độ chỉnh sửa nhưng hóa đơn không còn trạng thái 0 thì chặn
        if (this.editId && Number(this.invoiceStatus) !== 0) {
          this.toastError('Chỉ được phép cập nhật hóa đơn ở trạng thái nháp (status = 0)', 'invalid_status_update')
          return
        }

        this.recalcTotals()
        const formId = this.formInvoices?.form_id || this.prepare?.formId || null
        const basePayload = {
          formId: formId,
          no: this.frmData.no || null,
          dateExport: this.frmData.date_export || null,
          paymentType: this.frmData.payment_type || 1,
          status: 0,
          orderCode: this.frmData.order?.code || null,
          customer: { ...this.frmData.customer },
          detail: rows,
          total: this.frmData.total || 0,
          vatAmount: this.frmData.vat_amount || 0,
          amount: this.frmData.amount || 0,
          amountInWords: this.frmData.amount_in_words || '',
          currency: 'VND',
          exchangeRate: 0,
          vatRate: null,
          vatRateOther: 0
        }
        let res
        if (this.editId) {
          res = await axios.put(`/invoices/${this.editId}`, basePayload, { successMessage: 'Đã cập nhật hóa đơn' })
        } else {
          // Phía backend sẽ tự sinh id_attr (32 ký tự) và lookup_code (10 ký tự)
          res = await axios.post('/invoices', basePayload, { successMessage: 'Đã lưu hóa đơn' })
        }
        const data = res?.data || {}
        const id = data?.id
        if (id || this.editId) {
          if (this.$toastr && typeof this.$toastr.success === 'function') {
            this.$toastr.success(this.editId ? 'Hóa đơn đã được cập nhật' : 'Hóa đơn đã được lưu', 'Thành công')
          }
          if (this.$router) this.$router.replace({ name: 'CustomerVatInvoiceList' })
        } else {
          this.toastError('Không nhận được ID hóa đơn sau khi lưu/cập nhật', 'missing_id')
        }
      } catch (e) {
        const msg = e?.response?.data?.message || (this.editId ? 'Không thể cập nhật hóa đơn' : 'Không thể lưu hóa đơn')
        this.toastError(msg, 'submit_error')
      }
    },
    recalcRowFromQuantityPrice (idx) {
      const r = this.frmData.detail[idx]
      const quantity = Number(r.quantity || 0)
      const price = Number(r.price || 0)
      const total = quantity * price
      const rate = r.vatRate === -3 ? Number(r.vatRateOther || 0) : Number(r.vatRate || 0)
      const effectiveRate = Number.isFinite(rate) ? rate : 0
      const vatAmount = effectiveRate < 0 ? 0 : Math.round(total * effectiveRate / 100)
      const amount = total + vatAmount
      r.total = total
      r.vatAmount = vatAmount
      r.amount = amount
      this.recalcTotals()
    },
    recalcRowFromTotal (idx) {
      const r = this.frmData.detail[idx]
      const total = Number(r.total || 0)
      const rate = r.vatRate === -3 ? Number(r.vatRateOther || 0) : Number(r.vatRate || 0)
      const effectiveRate = Number.isFinite(rate) ? rate : 0
      const vatAmount = effectiveRate < 0 ? 0 : Math.round(total * effectiveRate / 100)
      const amount = total + vatAmount
      r.vatAmount = vatAmount
      r.amount = amount
      this.recalcTotals()
    },
    recalcRowFromVatRate (idx) {
      const r = this.frmData.detail[idx]
      const total = Number(r.total || 0)
      const rate = r.vatRate === -3 ? Number(r.vatRateOther || 0) : Number(r.vatRate || 0)
      const effectiveRate = Number.isFinite(rate) ? rate : 0
      const vatAmount = effectiveRate < 0 ? 0 : Math.round(total * effectiveRate / 100)
      const amount = total + vatAmount
      r.vatAmount = vatAmount
      r.amount = amount
      this.recalcTotals()
    },
    onQuantity (idx) { this.recalcRowFromQuantityPrice(idx) },
    onPrice (idx) { this.recalcRowFromQuantityPrice(idx) },
    onTotal (idx) { this.recalcRowFromTotal(idx) },
    onVatRate (evt, idx) { this.recalcRowFromVatRate(idx) },
    onVatRateOther (evt, idx) { this.recalcRowFromVatRate(idx) },
    onVatAmount (idx) { this.recalcRowFromVatAmount(idx) },
    onInputAmount (idx) { this.recalcTotals() },
    onFeature (evt, idx) { this.recalcTotals() },
    recalcTotals () {
      const rows = Array.isArray(this.frmData.detail) ? this.frmData.detail : []
      const sumIncluded = (getter) => rows.reduce((acc, r) => acc + (Number(r.feature) === 1 ? getter(r) : 0), 0)
      const total = sumIncluded(r => Number(r.total || 0))
      const vat_amount = sumIncluded(r => Number(r.vatAmount || 0))
      const amount = sumIncluded(r => Number(r.amount || 0))
      this.frmData.total = total
      this.frmData.vat_amount = vat_amount
      this.frmData.amount = amount
      this.frmData.amount_in_words = this.numberToWordsVn(amount)
    },
    numberToWordsVn (num) {
      // Robust Vietnamese number to words
      // Examples: 0 -> "Không đồng", 220000 -> "Hai trăm hai mươi nghìn đồng"
      const units = ['không', 'một', 'hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín']
      const scales = ['','nghìn','triệu','tỷ','nghìn tỷ','triệu tỷ','tỷ tỷ']

      const normalize = (n) => {
        const v = Number(n)
        if (!Number.isFinite(v)) return 0
        return Math.floor(Math.abs(v))
      }

      const readTriple = (n) => {
        const hundred = Math.floor(n / 100)
        const ten = Math.floor((n % 100) / 10)
        const one = n % 10
        const parts = []
        if (hundred > 0) {
          parts.push(units[hundred] + ' trăm')
          if (ten === 0 && one > 0) parts.push('lẻ')
        }
        if (ten > 1) {
          parts.push(units[ten] + ' mươi')
          if (one === 1) parts.push('mốt')
          else if (one === 4) parts.push('tư')
          else if (one === 5) parts.push('lăm')
          else if (one > 0) parts.push(units[one])
        } else if (ten === 1) {
          parts.push('mười')
          if (one === 1) parts.push('một')
          else if (one === 4) parts.push('bốn')
          else if (one === 5) parts.push('lăm')
          else if (one > 0) parts.push(units[one])
        } else if (ten === 0 && hundred === 0) {
          if (one > 0) parts.push(units[one])
        } else if (ten === 0) {
          if (one > 0) parts.push(units[one])
        }
        return parts.join(' ')
      }

      const readNumber = (n) => {
        if (n === 0) return 'không'
        const groups = []
        while (n > 0) {
          groups.push(n % 1000)
          n = Math.floor(n / 1000)
        }
        const words = []
        for (let i = groups.length - 1; i >= 0; i--) {
          const grp = groups[i]
          if (grp === 0) {
            // skip empty group but maintain scale progression
            continue
          }
          const tripleWords = readTriple(grp)
          if (tripleWords) {
            words.push(tripleWords)
            const scale = scales[i] || ''
            if (scale) words.push(scale)
          }
        }
        return words.join(' ').replace(/\s+/g, ' ').trim()
      }

      // Main
      const v = Number(num || 0)
      if (!Number.isFinite(v)) return 'Không đồng'
      if (v === 0) return 'Không đồng'
      const abs = normalize(v)
      const words = readNumber(abs)
      const head = v < 0 ? 'Âm ' : ''
      // Capitalize first letter
      const cap = words.charAt(0).toUpperCase() + words.slice(1)
      return `${head}${cap} đồng`
    },
    splitNumber (v) {
      const n = Number(v || 0)
      return n.toLocaleString('vi-VN')
    },
    splitNumberDecimal (v) {
      if (v === null || v === undefined) return ''
      const n = String(v).replace(/[^0-9.,-]/g, '')
      return n
    },
    state (field) { return true },
    invalidFeedback (field) { return '' },
    checkProductUpdated () {},
    onNewLineName (idx) {},
    loadColumnTable () {
      return [
        { key: 'num', label: '#', thClass: 'text-center', tdClass: 'text-center align-middle', stickyColumn: true },
        { key: 'name', label: 'Tên hàng hóa/Dịch vụ', thClass: 'text-left', tdClass: 'align-middle' },
        { key: 'code', label: 'Mã hàng', thClass: 'text-center', tdClass: 'text-center align-middle' },
        { key: 'unit', label: 'Đơn vị', thClass: 'text-center', tdClass: 'text-center align-middle' },
        { key: 'quantity', label: 'SL', thClass: 'text-center', tdClass: 'text-center align-middle' },
        { key: 'price', label: 'Đơn giá', thClass: 'text-right', tdClass: 'text-right align-middle' },
        { key: 'total', label: 'Thành tiền', thClass: 'text-right', tdClass: 'text-right align-middle' },
        { key: 'vatRate', label: 'Thuế suất', thClass: 'text-center', tdClass: 'text-center align-middle' },
        { key: 'vatAmount', label: 'Tiền thuế', thClass: 'text-right', tdClass: 'text-right align-middle' },
        { key: 'amount', label: 'Tổng', thClass: 'text-right', tdClass: 'text-right align-middle' },
        { key: 'feature', label: 'Tính chất', thClass: 'text-center', tdClass: 'text-center align-middle' },
        { key: 'delete', label: '', thClass: 'text-center', tdClass: 'text-center align-middle' }
      ]
    },
  }
}
</script>

<style scoped>
/* Container & Card */
.invoice-container {
  padding: 18px;
  background: linear-gradient(180deg, #f6f8fb 0%, #f3f6fa 100%);
  min-height: 100vh;
  font-size: 13px; /* base font slightly smaller */
}

.invoice-card {
  border-radius: 12px;
  box-shadow: 0 6px 18px rgba(31, 45, 61, 0.08);
  border: 1px solid #e6ebf2;
  overflow: hidden;
}

/* Tiêu đề */
.invoice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px 14px 14px;
  border-bottom: 1px solid #e6ebf2;
  background: #ffffff;
}

.invoice-title {
  margin: 0;
  font-weight: 700;
  color: #1f2d3d;
  letter-spacing: 0.2px;
  font-size: 18px; /* smaller title */
}

/* Alert spacing */
.alert-danger { margin-bottom: 12px; }

/* Sections */
.form-section {
  background: #fff;
  border: 1px solid #e6ebf2;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 14px;
  box-shadow: 0 1px 4px rgba(17, 24, 39, 0.04);
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 14px; /* smaller */
  font-weight: 700;
  color: #2b3850;
  margin: 0 0 8px;
}
.section-title i { color: #4f7dff; font-size: 14px; }

/* Form label & inputs */
.form-label {
  font-size: 11.5px;
  color: #6b7280;
  margin-bottom: 4px !important;
}

/* Compact inputs with nicer focus */
.form-section .form-control,
.form-section .custom-select {
  min-height: 30px; /* smaller height */
  padding: 4px 8px; /* tighter padding */
  font-size: 12.5px;
  border: 1px solid #d7dde5;
  border-radius: 7px;
  transition: box-shadow 0.15s ease, border-color 0.15s ease;
}
.form-section .form-control:focus,
.form-section .custom-select:focus {
  border-color: #4f7dff;
  box-shadow: 0 0 0 2px rgba(79, 125, 255, 0.15);
}

/* Input group text for code */
.input-group-text.input-code-text {
  background: #eef3ff;
  color: #2b4bb7;
  border: 1px solid #d7dde5;
  border-radius: 7px 0 0 7px;
  padding: 4px 8px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  height: 30px; /* match control height */
  border-right: 0; /* remove inner seam */
}

/* Specific style for the serial input next to code label */
.serial-input {
  border-radius: 0 7px 7px 0 !important;
  height: 30px; /* match group */
  border-left: 0; /* remove inner seam */
}

/* Keep legacy badge style if used elsewhere */
.input-badge {
  display: inline-flex;
  align-items: center;
  height: 28px;
  border-radius: 7px;
  padding: 0 8px;
  background: #4f7dff;
  font-size: 12px;
}

/* Table */
.table-responsive { border-radius: 8px; overflow: hidden; }
.invoice-table {
  font-size: 12.5px; /* smaller table font */
  margin-bottom: 0;
}
.invoice-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f7f9fc;
  color: #374151;
  padding: 8px;
  border-color: #e6ebf2;
  font-weight: 600;
}
.invoice-table tbody tr:nth-child(odd) td { background: #ffffff; }
.invoice-table tbody tr:nth-child(even) td { background: #fbfdff; }
.invoice-table tbody tr:hover td { background: #eef4ff; }
.invoice-table tbody td {
  padding: 6px 8px; /* tighter */
  vertical-align: middle;
  border-top: 1px solid #eef2f7;
}

/* Table inputs/selects */
.table-input.form-control {
  min-height: 28px;
  padding: 3px 6px;
  font-size: 12px;
  border: 1px solid #d7dde5;
  border-radius: 6px;
}
.table-input.form-control:focus { box-shadow: 0 0 0 2px rgba(79, 125, 255, 0.12); border-color: #4f7dff; }
.table-select.custom-select,
.table-select.form-control {
  min-height: 28px;
  padding: 2px 6px;
  font-size: 12px;
  border: 1px solid #d7dde5;
  border-radius: 6px;
}
.table-select:focus { box-shadow: 0 0 0 2px rgba(79, 125, 255, 0.12); border-color: #4f7dff; }

/* VAT other input inline */
.vat-other-input {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}
.vat-percent { color: #6c757d; font-size: 11.5px; }

/* Summary */
.summary-section { border: 1px dashed #e0e6ed; }
.summary-input.form-control {
  background: #f7f9fc;
  font-weight: 600;
  font-size: 12.5px;
}
.total-amount.form-control { color: #168a3a; }
.summary-label { color: #4b5563; font-size: 11.5px; }

/* Action buttons */
.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding-top: 8px;
}
.action-buttons .btn {
  border-radius: 7px;
  padding: 6px 10px; /* smaller */
  font-weight: 600;
  font-size: 12.5px;
}
.action-buttons .btn-success { box-shadow: 0 3px 10px rgba(22, 163, 74, 0.18); }
.action-buttons .btn-outline-secondary:hover { background: #f1f5f9; }

/* Khoảng cách container skeleton tải */
.loading-skeleton .mb-2 { margin-bottom: 8px !important; }

/* Utilities */
.small { font-size: 11.5px; }
.mt-2 { margin-top: 6px; }

/* Tinh chỉnh responsive */
@media (max-width: 992px) {
  .form-section { padding: 10px 12px; }
  .invoice-header { margin-bottom: 12px; }
}

@media (max-width: 576px) {
  .invoice-container { padding: 12px; }
  .section-title { font-size: 13px; }
  .form-section .form-control { font-size: 12px; }
  .invoice-title { font-size: 17px; }
}
</style>
