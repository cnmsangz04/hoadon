<template>
  <div class="invoice-container stock-issue-create">
    <b-card class="invoice-card">
      <div class="invoice-header">
        <h4 class="invoice-title">
          {{ editId ? 'Cập nhật' : 'Lập' }} phiếu xuất kho kiêm vận chuyển nội
          bộ
        </h4>
      </div>

      <InvoiceAiTools
        v-if="prepare.formId && !form.referenceId && !form.invoiceType && (!form.status || Number(form.status) === 0)"
        :category="6"
        :form-id="Number(prepare.formId)"
        :snapshot="aiSnapshot"
        :show-assistant="true"
        @document-applied="applyAiSuggestion"
        @assistant-applied="applyAiSuggestion"
      />

      <div class="form-section">
        <h5 class="section-title">
          <i class="fas fa-file-invoice mr-2"></i>Thông tin phiếu xuất kho
        </h5>
        <b-row>
          <b-col lg="4" md="6"
            ><b-form-group label="Tên phiếu" label-class="form-label"
              ><b-form-input
                :value="
                  prepare.formName || 'Phiếu xuất kho kiêm vận chuyển nội bộ'
                "
                readonly /></b-form-group
          ></b-col>
          <b-col lg="4" md="6"
            ><b-form-group label="Hình thức" label-class="form-label"
              ><b-form-input :value="haveCodeLabel" readonly /></b-form-group
          ></b-col>
          <b-col lg="4" md="6"
            ><b-form-group label="Mã đơn hàng" label-class="form-label"
              ><b-form-input v-model="form.orderCode" /></b-form-group
          ></b-col>
        </b-row>
        <b-row>
          <b-col lg="4" md="6">
            <b-form-group label="Ký hiệu" label-class="form-label">
              <b-input-group>
                <b-input-group-prepend
                  ><span class="input-group-text input-code-text">{{
                    prepare.formCode || '—'
                  }}</span></b-input-group-prepend
                >
                <b-form-input
                  class="serial-input"
                  :value="prepare.serial || '—'"
                  readonly
                />
              </b-input-group>
            </b-form-group>
          </b-col>
          <b-col lg="4" md="6"
            ><b-form-group label="Số phiếu" label-class="form-label"
              ><b-form-input :value="form.no || '0'" readonly /></b-form-group
          ></b-col>
          <b-col lg="4" md="6"
            ><b-form-group label="Ngày lập" label-class="form-label"
              ><manual-date-picker
                v-model="form.dateExport"
                placeholder="dd/mm/yyyy" /></b-form-group
          ></b-col>
        </b-row>
      </div>

      <div class="form-section">
        <h5 class="section-title">
          <i class="fas fa-building mr-2"></i>Thông tin đơn vị xuất hàng
        </h5>
        <b-row>
          <b-col md="6"
            ><b-form-group label="Đơn vị xuất hàng" label-class="form-label"
              ><b-form-input
                :value="company.name || '—'"
                readonly /></b-form-group
          ></b-col>
          <b-col md="6"
            ><b-form-group label="Mã số thuế" label-class="form-label"
              ><b-form-input
                :value="company.taxcode || '—'"
                readonly /></b-form-group
          ></b-col>
        </b-row>
        <b-row
          ><b-col
            ><b-form-group label="Địa chỉ" label-class="form-label"
              ><b-form-input
                :value="company.address || '—'"
                readonly /></b-form-group></b-col
        ></b-row>
      </div>

      <div class="form-section">
        <h5 class="section-title">
          <i class="fas fa-user-tie mr-2"></i>Thông tin đơn vị nhận
        </h5>
        <b-row>
          <b-col lg="4" md="6"
            ><b-form-group label="Mã khách hàng" label-class="form-label"
              ><b-form-input
                v-model="customerPicker"
                list="stockInternalCustomers"
                placeholder="Nhập hoặc chọn mã khách hàng"
                :disabled="loadingCustomers"
                @change="onStockCustomerChange" /></b-form-group
          ></b-col>
          <b-col lg="4" md="6"
            ><b-form-group label="Đơn vị nhận" label-class="form-label"
              ><b-form-input
                v-model.trim="form.stockInfo.buyerName" /></b-form-group
          ></b-col>
          <b-col lg="4" md="6"
            ><b-form-group label="Người nhận hàng" label-class="form-label"
              ><b-form-input
                v-model.trim="form.stockInfo.receiverName" /></b-form-group
          ></b-col>
        </b-row>
        <b-row>
          <b-col lg="4" md="6">
            <b-form-group label="Mã số thuế" label-class="form-label">
              <b-input-group class="tax-lookup-group">
                <b-form-input
                  v-model.trim="form.stockInfo.buyerTaxCode"
                  placeholder="Nhập MST để tra cứu"
                  :disabled="lookingUpTaxCode"
                  @keyup.enter="lookupStockTaxCode"
                />
                <b-input-group-append>
                  <b-button
                    class="tax-lookup-button"
                    variant="outline-primary"
                    :disabled="
                      lookingUpTaxCode ||
                      !String(form.stockInfo.buyerTaxCode || '').trim()
                    "
                    title="Tra cứu thông tin người nộp thuế"
                    @click="lookupStockTaxCode"
                  >
                    <b-spinner v-if="lookingUpTaxCode" small />
                    <template v-else
                      ><i class="fas fa-search mr-1"></i>Tra cứu</template
                    >
                  </b-button>
                </b-input-group-append>
              </b-input-group>
            </b-form-group>
          </b-col>
          <b-col lg="4" md="6"
            ><b-form-group label="Email" label-class="form-label"
              ><b-form-input
                v-model.trim="form.stockInfo.buyerEmail"
                type="email" /></b-form-group
          ></b-col>
          <b-col lg="4" md="6"
            ><b-form-group label="Điện thoại" label-class="form-label"
              ><b-form-input
                v-model.trim="form.stockInfo.buyerPhone" /></b-form-group
          ></b-col>
        </b-row>
        <b-row
          ><b-col
            ><b-form-group label="Địa chỉ nhận" label-class="form-label"
              ><b-form-input
                v-model="form.stockInfo.buyerAddress" /></b-form-group></b-col
        ></b-row>
        <b-row>
          <b-col lg="4" md="6"
            ><b-form-group label="Ngân hàng" label-class="form-label"
              ><b-form-input
                v-model="form.stockInfo.buyerBankName" /></b-form-group
          ></b-col>
          <b-col lg="4" md="6"
            ><b-form-group label="Số tài khoản" label-class="form-label"
              ><b-form-input
                v-model="form.stockInfo.buyerBankAccount" /></b-form-group
          ></b-col>
          <b-col lg="4" md="6"
            ><b-form-group label="Hình thức thanh toán" label-class="form-label"
              ><b-form-select
                v-model="form.paymentType"
                :options="paymentTypeOptions" /></b-form-group
          ></b-col>
        </b-row>
        <datalist v-if="!loadingCustomers" id="stockInternalCustomers">
          <option
            v-for="item in customersRaw"
            :key="item.id || item.code"
            :value="customerOptionValue(item)"
          >
            {{
              (item.companyName || item.buyerName || '') +
              ' - MST: ' +
              (item.taxCode || item.taxcode || 'N/A')
            }}
          </option>
        </datalist>
      </div>

      <div class="form-section">
        <h5 class="section-title">
          <i class="fas fa-truck mr-2"></i>Thông tin vận chuyển
        </h5>
        <b-row>
          <b-col lg="3" md="6"
            ><b-form-group
              label="Lệnh điều động nội bộ"
              label-class="form-label"
              ><b-form-input
                v-model="form.stockInfo.dispatchOrderNo" /></b-form-group
          ></b-col>
          <b-col lg="3" md="6"
            ><b-form-group label="Người vận chuyển" label-class="form-label"
              ><b-form-input
                v-model="form.stockInfo.transportName" /></b-form-group
          ></b-col>
          <b-col lg="3" md="6"
            ><b-form-group
              label="Phương tiện vận chuyển"
              label-class="form-label"
              ><b-form-input v-model="form.stockInfo.vehicle" /></b-form-group
          ></b-col>
          <b-col lg="3" md="6"
            ><b-form-group label="Hợp đồng số" label-class="form-label"
              ><b-form-input
                v-model="form.stockInfo.transportContractNo" /></b-form-group
          ></b-col>
        </b-row>
      </div>

      <div class="form-section">
        <h5 class="section-title">
          <i class="fas fa-boxes mr-2"></i>Chi tiết hàng hóa
        </h5>
        <div class="table-responsive">
          <b-table
            :fields="detailFields"
            :items="form.detail"
            responsive
            hover
            outlined
            bordered
            class="invoice-table"
          >
            <template #cell(num)="{ index }"
              ><div class="text-center">{{ index + 1 }}</div></template
            >
            <template #cell(name)="{ item, index }"
              ><b-form-input
                v-model="item.name"
                list="stockInternalProducts"
                class="table-input"
                :disabled="loadingProducts"
                @change="onStockProductChange(item)"
                @keyup.alt.enter="onNewLineName(index)"
            /></template>
            <template #cell(code)="{ item }"
              ><b-form-input v-model="item.code" class="table-input"
            /></template>
            <template #cell(unit)="{ item }"
              ><b-form-input v-model="item.unit" class="table-input"
            /></template>
            <template #cell(quantityExport)="{ item }"
              ><b-form-input
                v-model.number="item.quantityExport"
                min="0"
                type="number"
                class="table-input"
                @change="onQuantityPriceChange(item)"
            /></template>
            <template #cell(quantityImport)="{ item }"
              ><b-form-input
                v-model.number="item.quantityImport"
                min="0"
                type="number"
                class="table-input"
            /></template>
            <template #cell(price)="{ item }"
              ><b-form-input
                v-model.number="item.price"
                min="0"
                type="number"
                class="table-input"
                @change="onQuantityPriceChange(item)"
            /></template>
            <template #cell(total)="{ item }"
              ><b-form-input
                v-model.number="item.total"
                min="0"
                type="number"
                class="table-input text-right total-cell"
                @change="onTotalChange(item)"
            /></template>
            <template #cell(amount)="{ item }"
              ><b-form-input
                v-model="item.amount"
                class="table-input text-right amount-cell"
                @change="onAmountChange(item)"
            /></template>
            <template #cell(actions)="{ index }"
              ><b-button
                size="sm"
                variant="outline-danger"
                @click="form.detail.splice(index, 1)"
                ><i class="far fa-trash-alt"></i></b-button
            ></template>
          </b-table>
        </div>
        <datalist v-if="!loadingProducts" id="stockInternalProducts">
          <option
            v-for="item in productsRaw"
            :key="item.id || item.code || item.productCode"
            :value="productOptionValue(item)"
          >
            {{ 'Giá: ' + money(item.price) }}
          </option>
        </datalist>
      </div>

      <div class="form-section summary-section">
        <h5 class="section-title">
          <i class="fas fa-calculator mr-2"></i>Tổng hợp
        </h5>
        <b-row>
          <b-col lg="6" md="6">
            <b-form-group
              label="Tổng tiền hàng"
              label-class="form-label summary-label"
            >
              <b-form-input
                :value="summaryNumber(total)"
                readonly
                class="summary-input"
              />
            </b-form-group>
          </b-col>
          <b-col lg="6" md="6">
            <b-form-group
              label="Tổng tiền thanh toán"
              label-class="form-label summary-label"
            >
              <b-form-input
                :value="summaryNumber(amountTotal)"
                readonly
                class="summary-input total-amount"
              />
            </b-form-group>
          </b-col>
        </b-row>
        <b-row>
          <b-col>
            <b-form-group
              label="Số tiền bằng chữ"
              label-class="form-label summary-label"
            >
              <b-form-input
                :value="amountTotal ? numberToWordsVn(amountTotal) : ''"
                readonly
                class="summary-input"
              />
            </b-form-group>
          </b-col>
        </b-row>
      </div>
      <div class="action-buttons">
        <b-button
          size="sm"
          to="/stock-issue/internal-transport/list"
          variant="outline-secondary"
          ><i class="fas fa-arrow-left mr-1"></i> Quay lại</b-button
        >
        <b-button :disabled="saving" variant="success" @click="save"
          ><b-spinner v-if="saving" small class="mr-1"></b-spinner
          ><i v-else class="fas fa-save mr-1"></i> Lưu và lập phiếu xuất
          kho</b-button
        >
      </div>
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import InvoiceAiTools from '@/views/components/invoice_ai_tools.vue'
import stockIssuePicker from '../stock-issue-picker'
import { applyStockIssueAi, stockIssueAiSnapshot } from '../stock-issue-ai'

const emptyDetail = (num) => ({
  num,
  feature: 1,
  name: '',
  code: '',
  unit: '',
  quantityExport: '',
  quantityImport: '',
  price: 0,
  total: 0,
  amount: 0,
})

export default {
  name: 'StockIssueInternalTransportCreate',
  components: { InvoiceAiTools },
  mixins: [stockIssuePicker],
  data() {
    return {
      editId: this.$route.params.id || null,
      saving: false,
      prepare: {},
      company: { name: null, taxcode: null, address: null },
      form: {
        dateExport: new Date().toISOString().slice(0, 10),
        orderCode: '',
        paymentType: 1,
        currency: 'VND',
        stockInfo: { transportContractNo: '' },
        detail: [emptyDetail(1)],
      },
      detailFields: [
        {
          key: 'num',
          label: '#',
          thClass: 'text-center',
          tdClass: 'text-center align-middle',
        },
        { key: 'name', label: 'Tên hàng hóa/Dịch vụ' },
        { key: 'code', label: 'Mã hàng' },
        { key: 'unit', label: 'Đơn vị' },
        { key: 'quantityExport', label: 'Thực xuất' },
        { key: 'quantityImport', label: 'Thực nhập' },
        { key: 'price', label: 'Đơn giá' },
        { key: 'total', label: 'Thành tiền' },
        { key: 'amount', label: 'Tổng' },
        { key: 'actions', label: '' },
      ],
      paymentTypeOptions: [
        { value: 1, text: 'Tiền mặt' },
        { value: 2, text: 'Chuyển khoản' },
        { value: 3, text: 'Tiền mặt/Chuyển khoản' },
      ],
    }
  },
  computed: {
    aiSnapshot() {
      return stockIssueAiSnapshot(this.form, 1)
    },
    total() {
      return this.form.detail.reduce((sum, row) => sum + this.lineTotal(row), 0)
    },
    amountTotal() {
      return this.form.detail.reduce(
        (sum, row) => sum + this.lineAmount(row),
        0
      )
    },
    haveCodeLabel() {
      if (Number(this.prepare.haveCode) === 1) return 'Có mã của cơ quan thuế'
      if ([0, 2].includes(Number(this.prepare.haveCode)))
        return 'Không có mã của cơ quan thuế'
      return '—'
    },
  },
  async mounted() {
    try {
      await Promise.all([this.loadPrepare(), this.loadCompany()])
      if (this.editId) await this.loadInvoice()
      else if (this.$route.query.referenceId) await this.loadReference()
    } catch (e) {
      this.redirectToList()
    }
  },
  methods: {
    applyAiSuggestion(result) {
      this.form = applyStockIssueAi(this.form, result, 1)
      this.customerPicker = this.form.stockInfo.buyerCode || this.form.stockInfo.buyerName || ''
      this.selectedCustomer = this.findStockCustomer(this.customerPicker)
    },
    redirectToList() {
      if (this.$router)
        this.$router.replace({
          name: 'CustomerStockIssueInternalTransportList',
        })
    },
    normalizeDetails(rows) {
      return (Array.isArray(rows) ? rows : []).map((row) => ({
        ...row,
        quantityExport: row.quantityExport ?? row.quantity ?? 0,
        quantityImport: row.quantityImport ?? 0,
        total:
          row.total ??
          Number(row.quantityExport ?? row.quantity ?? 0) *
            Number(row.price || 0),
        amount:
          row.amount ??
          row.total ??
          Number(row.quantityExport ?? row.quantity ?? 0) *
            Number(row.price || 0),
      }))
    },
    async loadPrepare() {
      const { data } = await axios.get('/invoices/prepare', {
        params: { category: 6, stockIssueType: 1 },
      })
      this.prepare = data
      this.form.formId = data.formId
      this.form.stockIssueType = 1
    },
    async loadCompany() {
      const { data } = await axios.get('/auth/info', {
        meta: { suppressGlobalErrorToast: true },
      })
      const company = data && data.company ? data.company : {}
      this.company = {
        name: company.name || null,
        taxcode: company.taxcode || company.taxCode || null,
        address: company.address || null,
      }
    },
    async loadInvoice() {
      const { data } = await axios.get('/invoices/' + this.editId)
      this.form = {
        ...this.form,
        ...data,
        stockInfo: data.others || {},
        detail: this.normalizeDetails(data.detail || this.form.detail),
        stockIssueType: 1,
      }
    },
    async loadReference() {
      const { data } = await axios.get(
        '/invoices/' + this.$route.query.referenceId
      )
      this.form = {
        ...this.form,
        dateExport: new Date().toISOString().slice(0, 10),
        formId: this.prepare.formId,
        stockInfo: data.others || {},
        detail: this.normalizeDetails(data.detail || this.form.detail),
        referenceId: data.id,
        invoiceType: Number(this.$route.query.invoiceType || 1),
        invoiceTypeAdjust: Number(this.$route.query.invoiceTypeAdjust || 0),
        relatedReason: this.$route.query.relatedReason || null,
        stockIssueType: 1,
      }
    },
    onNewLineName(index) {
      this.form.detail.splice(index + 1, 0, emptyDetail(index + 2))
    },
    onQuantityPriceChange(row) {
      row.total = Number(row.quantityExport || 0) * Number(row.price || 0)
      row.amount = row.total
    },
    onTotalChange(row) {
      row.total = Number(row.total || 0)
      row.amount = row.total
    },
    onAmountChange(row) {
      row.amount = Number(row.amount || 0)
    },
    lineTotal(row) {
      return Number(
        row.total ??
          Number(row.quantityExport ?? row.quantity ?? 0) *
            Number(row.price || 0)
      )
    },
    lineAmount(row) {
      return Number(row.amount ?? this.lineTotal(row))
    },
    money(value) {
      return Number(value || 0).toLocaleString('vi-VN') + ' đ'
    },
    summaryNumber(value) {
      return Number(value || 0).toLocaleString('vi-VN')
    },
    numberToWordsVn(num) {
      const units = [
        'không',
        'một',
        'hai',
        'ba',
        'bốn',
        'năm',
        'sáu',
        'bảy',
        'tám',
        'chín',
      ]
      const scales = [
        '',
        'nghìn',
        'triệu',
        'tỷ',
        'nghìn tỷ',
        'triệu tỷ',
        'tỷ tỷ',
      ]
      const readTriple = (value) => {
        const hundred = Math.floor(value / 100)
        const ten = Math.floor((value % 100) / 10)
        const one = value % 10
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
        } else if (ten === 0 && one > 0) {
          parts.push(units[one])
        }
        return parts.join(' ')
      }
      const value = Number(num || 0)
      if (!Number.isFinite(value) || value === 0) return 'Không đồng'
      let remaining = Math.floor(Math.abs(value))
      const groups = []
      while (remaining > 0) {
        groups.push(remaining % 1000)
        remaining = Math.floor(remaining / 1000)
      }
      const words = []
      for (let index = groups.length - 1; index >= 0; index--) {
        if (!groups[index]) continue
        const text = readTriple(groups[index])
        if (text) {
          words.push(text)
          if (scales[index]) words.push(scales[index])
        }
      }
      const result = words.join(' ')
      return (
        (value < 0 ? 'Âm ' : '') +
        result.charAt(0).toUpperCase() +
        result.slice(1) +
        ' đồng'
      )
    },
    async save() {
      this.saving = true
      try {
        const payload = {
          ...this.form,
          formId: this.form.formId || this.prepare.formId,
          stockIssueType: 1,
          stockInfo: this.form.stockInfo,
          total: this.total,
          amount: this.amountTotal,
          vatAmount: 0,
          vatRate: null,
          vatRateOther: 0,
          amountInWords: this.numberToWordsVn(this.amountTotal),
        }
        if (this.editId) await axios.put('/invoices/' + this.editId, payload)
        else await axios.post('/invoices', payload)
        this.redirectToList()
      } catch (e) {
        this.redirectToList()
      } finally {
        this.saving = false
      }
    },
  },
}
</script>

<style scoped>
.invoice-container {
  min-height: 100vh;
  padding: 18px;
  background: linear-gradient(180deg, #f6f8fb 0%, #f3f6fa 100%);
  font-size: 13px;
}
.invoice-card {
  border: 1px solid #e6ebf2;
  border-radius: 12px;
  box-shadow: 0 6px 18px rgba(31, 45, 61, 0.08);
  overflow: hidden;
}
.invoice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px 14px 14px;
  border-bottom: 1px solid #e6ebf2;
  background: #fff;
}
.invoice-title {
  margin: 0;
  color: #1f2d3d;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.2px;
}
.form-section {
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px solid #e6ebf2;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(17, 24, 39, 0.04);
}
.section-title {
  display: flex;
  align-items: center;
  margin: 0 0 8px;
  color: #2b3850;
  font-size: 14px;
  font-weight: 700;
}
.section-title i {
  color: #4f7dff;
  font-size: 14px;
}
.form-label {
  margin-bottom: var(--ui-label-gap) !important;
  color: #6b7280;
  font-size: 11.5px;
}
.form-section .form-control,
.form-section .custom-select {
  min-height: 30px;
  padding: 4px 8px;
  border: 1px solid #d7dde5;
  border-radius: 7px;
  font-size: 12.5px;
  transition: box-shadow 0.15s ease, border-color 0.15s ease;
}
.form-section .form-control:focus,
.form-section .custom-select:focus {
  border-color: #4f7dff;
  box-shadow: 0 0 0 2px rgba(79, 125, 255, 0.15);
}
.input-group-text.input-code-text {
  background: #eef3ff;
  color: #2b4bb7;
  border: 1px solid #d7dde5;
  border-radius: 7px 0 0 7px;
  padding: 4px 8px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  height: 30px;
  border-right: 0;
}
.serial-input {
  border-radius: 0 7px 7px 0 !important;
  height: 30px;
  border-left: 0;
}
.tax-lookup-group .form-control {
  border-radius: 7px 0 0 7px;
}
.tax-lookup-button {
  min-height: 30px;
  padding: 4px 9px;
  border-radius: 0 7px 7px 0;
  font-size: 12px;
  white-space: nowrap;
}
.table-responsive {
  overflow: hidden;
  border-radius: 8px;
}
.invoice-table {
  margin-bottom: 0;
  font-size: 12.5px;
}
.invoice-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 8px;
  background: #f7f9fc;
  color: #374151;
  border-color: #e6ebf2;
  font-weight: 600;
}
.invoice-table tbody tr:nth-child(odd) td {
  background: #fff;
}
.invoice-table tbody tr:nth-child(even) td {
  background: #fbfdff;
}
.invoice-table tbody tr:hover td {
  background: #eef4ff;
}
.invoice-table tbody td {
  padding: 6px 8px;
  vertical-align: middle;
  border-top: 1px solid #eef2f7;
}
.table-input.form-control {
  min-height: 28px;
  padding: 3px 6px;
  font-size: 12px;
  border: 1px solid #d7dde5;
  border-radius: 6px;
}
.total-cell.form-control {
  background: #fff;
  font-weight: 600;
}
.table-input.form-control:focus {
  box-shadow: 0 0 0 2px rgba(79, 125, 255, 0.12);
  border-color: #4f7dff;
}
.amount-cell {
  white-space: nowrap;
  font-weight: 600;
}
.summary-section {
  border: 1px dashed #e0e6ed;
}
.summary-input.form-control {
  background: #f7f9fc;
  font-weight: 600;
  font-size: 12.5px;
}
.total-amount.form-control {
  color: #168a3a;
}
.summary-label {
  color: #4b5563;
  font-size: 11.5px;
}
.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
}
.action-buttons .btn {
  border-radius: 7px;
  padding: 6px 10px;
  font-weight: 600;
  font-size: 12.5px;
}
.action-buttons .btn-success {
  box-shadow: 0 3px 10px rgba(22, 163, 74, 0.18);
}
.action-buttons .btn-outline-secondary:hover {
  background: #f1f5f9;
}
.mt-2 {
  margin-top: 6px;
}
@media (max-width: 992px) {
  .form-section {
    padding: 10px 12px;
  }
  .invoice-header {
    margin-bottom: 12px;
  }
}
@media (max-width: 576px) {
  .invoice-container {
    padding: 12px;
  }
  .section-title {
    font-size: 13px;
  }
  .form-section .form-control {
    font-size: 12px;
  }
  .invoice-title {
    font-size: 17px;
  }
}
</style>
