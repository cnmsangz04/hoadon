<template>
  <b-card class="document-recognition mb-3" border-variant="info">
    <div class="d-flex align-items-center justify-content-between flex-wrap">
      <div>
        <h6 class="mb-1 font-weight-bold"><i class="fas fa-file-image mr-2"></i>Nhận diện chứng từ</h6>
        <div class="small text-muted">Xử lý nội bộ bằng PDFBox và Tesseract. Dữ liệu không được gửi tới Gemini.</div>
      </div>
      <b-button size="sm" variant="outline-info" @click="expanded = !expanded">
        {{ expanded ? 'Thu gọn' : 'Mở nhận diện' }}
      </b-button>
    </div>

    <div v-if="expanded" class="mt-3">
      <b-form-file
        v-model="file"
        accept=".jpg,.jpeg,.png,.pdf,image/jpeg,image/png,application/pdf"
        browse-text="Chọn file"
        placeholder="Chọn ảnh hoặc PDF, tối đa 10 MB"
        drop-placeholder="Thả chứng từ vào đây"
        :disabled="busy"
        @input="fileChanged"
      />
      <div v-if="previewUrl" class="document-preview mt-3">
        <iframe v-if="isPdf" :src="previewUrl" title="Xem trước PDF" />
        <img v-else :src="previewUrl" alt="Xem trước chứng từ" />
      </div>
      <div class="d-flex mt-3">
        <b-button variant="info" :disabled="busy || !file" @click="recognize">
          <b-spinner v-if="busy" small class="mr-1" />{{ busy ? 'Đang nhận diện...' : 'Nhận diện nội bộ' }}
        </b-button>
        <b-button v-if="result || file" variant="outline-secondary" class="ml-2" :disabled="busy" @click="reset">
          Hủy bỏ
        </b-button>
      </div>

      <b-alert v-if="error" show variant="danger" class="mt-3 mb-0">{{ error }}</b-alert>
      <b-alert v-if="busy" show variant="info" class="mt-3 mb-0">
        Hệ thống đang trích xuất dữ liệu. Chứng từ nhiều trang hoặc ảnh mờ có thể cần thêm thời gian.
      </b-alert>

      <div v-if="result && form" class="recognition-result mt-3">
        <div class="d-flex justify-content-between flex-wrap mb-2">
          <strong>Kết quả nhận diện</strong>
          <span class="small text-muted">
            {{ sourceLabel }} · {{ result.pageCount }} trang · {{ confidenceLabel(result.averageConfidence) }}
          </span>
        </div>
        <b-alert v-if="result.warnings && result.warnings.length" show variant="warning">
          <div v-for="warning in result.warnings" :key="warning">• {{ warning }}</div>
        </b-alert>

        <b-row>
          <b-col md="6">
            <h6>Người bán</h6>
            <b-form-group label="Tên người bán" label-class="small mb-1"><b-form-input v-model.trim="form.seller.name" :class="lowClass('seller.name')" @input="touch('seller.name')" /></b-form-group>
            <b-form-group label="Mã số thuế" label-class="small mb-1"><b-form-input v-model.trim="form.seller.taxCode" :class="lowClass('seller.taxCode')" @input="touch('seller.taxCode')" /></b-form-group>
            <b-form-group label="Địa chỉ" label-class="small mb-1"><b-form-input v-model.trim="form.seller.address" :class="lowClass('seller.address')" @input="touch('seller.address')" /></b-form-group>
          </b-col>
          <b-col md="6">
            <h6>Người mua</h6>
            <b-form-group label="Tên người mua" label-class="small mb-1"><b-form-input v-model.trim="form.buyer.name" :class="lowClass('buyer.name')" @input="touch('buyer.name')" /></b-form-group>
            <b-form-group label="Mã số thuế" label-class="small mb-1"><b-form-input v-model.trim="form.buyer.taxCode" :class="lowClass('buyer.taxCode')" @input="touch('buyer.taxCode')" /></b-form-group>
            <b-form-group label="Địa chỉ" label-class="small mb-1"><b-form-input v-model.trim="form.buyer.address" :class="lowClass('buyer.address')" @input="touch('buyer.address')" /></b-form-group>
          </b-col>
        </b-row>
        <b-row>
          <b-col md="2">
            <b-form-group label="Ngày chứng từ" label-class="small mb-1"><b-form-input v-model="form.documentDate" type="date" :class="lowClass('documentDate')" @input="touch('documentDate')" /></b-form-group>
          </b-col>
          <b-col md="2">
            <b-form-group label="Tiền tệ" label-class="small mb-1"><b-form-input v-model.trim="form.currency" :class="lowClass('currency')" @input="touch('currency')" /></b-form-group>
          </b-col>
          <b-col md="2">
            <b-form-group label="Tiền hàng" label-class="small mb-1"><b-form-input v-model="form.subtotal" :class="lowClass('subtotal')" @input="touch('subtotal')" /></b-form-group>
          </b-col>
          <b-col md="2">
            <b-form-group label="Tiền thuế" label-class="small mb-1"><b-form-input v-model="form.vatAmount" :class="lowClass('vatAmount')" @input="touch('vatAmount')" /></b-form-group>
          </b-col>
          <b-col md="4">
            <b-form-group label="Tổng thanh toán" label-class="small mb-1"><b-form-input v-model="form.total" :class="lowClass('total')" @input="touch('total')" /></b-form-group>
          </b-col>
        </b-row>

        <div class="font-weight-bold mb-2">Hàng hóa, dịch vụ</div>
        <div v-if="!form.items.length" class="text-muted small mb-3">Chưa nhận diện chắc chắn được dòng hàng hóa.</div>
        <div v-for="(item, index) in form.items" :key="index" class="item-editor mb-2">
          <b-row>
            <b-col lg="3"><b-form-input v-model.trim="item.name" size="sm" placeholder="Tên hàng hóa" :class="lowClass(`items.${index}.name`)" @input="touch(`items.${index}.name`)" /></b-col>
            <b-col lg="1"><b-form-input v-model.trim="item.unit" size="sm" placeholder="ĐVT" :class="lowClass(`items.${index}.unit`)" @input="touch(`items.${index}.unit`)" /></b-col>
            <b-col lg="2"><b-form-input v-model="item.quantity" size="sm" placeholder="Số lượng" :class="lowClass(`items.${index}.quantity`)" @input="touch(`items.${index}.quantity`)" /></b-col>
            <b-col lg="2"><b-form-input v-model="item.unitPrice" size="sm" placeholder="Đơn giá" :class="lowClass(`items.${index}.unitPrice`)" @input="touch(`items.${index}.unitPrice`)" /></b-col>
            <b-col lg="1"><b-form-input v-model="item.vatRate" size="sm" placeholder="Thuế" :disabled="category === 2" :class="lowClass(`items.${index}.vatRate`)" @input="touch(`items.${index}.vatRate`)" /></b-col>
            <b-col lg="2"><b-form-input v-model="item.lineTotal" size="sm" placeholder="Thành tiền" :class="lowClass(`items.${index}.lineTotal`)" @input="touch(`items.${index}.lineTotal`)" /></b-col>
            <b-col lg="1"><b-button size="sm" variant="outline-danger" title="Xóa dòng" @click="form.items.splice(index, 1)"><i class="fas fa-trash"></i></b-button></b-col>
          </b-row>
          <div v-if="item.matchedProductCode" class="small text-success mt-1">Khớp danh mục: {{ item.matchedProductCode }}</div>
        </div>
        <b-button size="sm" variant="outline-primary" @click="addItem"><i class="fas fa-plus mr-1"></i>Thêm dòng</b-button>

        <div class="d-flex justify-content-end mt-3">
          <b-button variant="outline-secondary" class="mr-2" @click="recognize">Nhận diện lại</b-button>
          <b-button variant="success" @click="apply">Xác nhận áp dụng vào biểu mẫu</b-button>
        </div>
        <div class="small text-muted mt-2">Viền vàng là trường thiếu hoặc có độ tin cậy thấp. Hệ thống chưa lưu hóa đơn.</div>
      </div>
    </div>
  </b-card>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'DocumentRecognition',
  props: { category: { type: Number, required: true } },
  data () {
    return {
      expanded: false,
      busy: false,
      file: null,
      previewUrl: '',
      result: null,
      form: null,
      error: '',
      touched: new Set()
    }
  },
  computed: {
    isPdf () { return this.file && (this.file.type === 'application/pdf' || /\.pdf$/i.test(this.file.name || '')) },
    sourceLabel () {
      return ({ PDF_TEXT: 'PDF có văn bản', PDF_OCR: 'PDF scan', PDF_MIXED: 'PDF hỗn hợp', IMAGE_OCR: 'Hình ảnh' })[this.result?.sourceType] || 'Chứng từ'
    }
  },
  beforeDestroy () { this.revokePreview() },
  methods: {
    fileChanged () {
      this.result = null
      this.form = null
      this.error = ''
      this.touched = new Set()
      this.revokePreview()
      if (!this.file) return
      if (this.file.size > 10 * 1024 * 1024) {
        this.error = 'Chứng từ vượt quá giới hạn 10 MB'
        this.file = null
        return
      }
      this.previewUrl = URL.createObjectURL(this.file)
    },
    async recognize () {
      if (!this.file || this.busy) return
      this.busy = true
      this.error = ''
      try {
        const body = new FormData()
        body.append('file', this.file)
        const { data } = await axios.post('/ai/document-recognition/recognize', body, {
          params: { category: this.category },
          meta: { suppressGlobalErrorToast: true }
        })
        this.result = data
        this.form = this.editable(data)
        this.touched = new Set()
      } catch (e) {
        this.error = e?.response?.data?.message || 'Không thể nhận diện chứng từ lúc này'
      } finally {
        this.busy = false
      }
    },
    editable (data) {
      const value = field => field?.value || ''
      return {
        seller: { name: value(data.seller?.name), taxCode: value(data.seller?.taxCode), address: value(data.seller?.address) },
        buyer: { name: value(data.buyer?.name), taxCode: value(data.buyer?.taxCode), address: value(data.buyer?.address) },
        documentDate: value(data.documentDate),
        currency: value(data.currency),
        subtotal: value(data.subtotal),
        vatAmount: value(data.vatAmount),
        total: value(data.total),
        items: (data.items || []).map(item => ({
          name: value(item.name), unit: value(item.unit), quantity: value(item.quantity),
          unitPrice: value(item.unitPrice), vatRate: this.category === 2 ? '-1' : value(item.vatRate),
          lineTotal: value(item.lineTotal), matchedProductId: item.matchedProductId,
          matchedProductCode: item.matchedProductCode
        }))
      }
    },
    async apply () {
      if (!this.form) return
      const confirmed = await this.$bvModal.msgBoxConfirm('Áp dụng dữ liệu đã kiểm tra vào biểu mẫu hóa đơn nháp?', {
        title: 'Xác nhận kết quả nhận diện', okTitle: 'Áp dụng', cancelTitle: 'Hủy', okVariant: 'success'
      })
      if (!confirmed) return
      const number = value => {
        const parsed = Number(String(value || '').replace(/\s/g, '').replace(/\.(?=\d{3}(?:\D|$))/g, '').replace(',', '.'))
        return Number.isFinite(parsed) ? parsed : 0
      }
      const details = this.form.items.filter(item => item.name).map((item, index) => {
        const quantity = number(item.quantity)
        const price = number(item.unitPrice)
        const vatRate = this.category === 2 ? -1 : number(item.vatRate || -1)
        const recognizedTotal = String(item.lineTotal ?? '').trim()
        const total = recognizedTotal ? number(recognizedTotal) : quantity * price
        const vatAmount = vatRate > 0 ? total * vatRate / 100 : 0
        return {
          num: index + 1, isNum: true, hidden: false, disable: false,
          code: item.matchedProductCode || '', name: item.name, unit: item.unit,
          quantity, price, vatRate, vatRateOther: 0,
          total, vatAmount, amount: total + vatAmount, feature: 1
        }
      })
      this.$emit('applied', {
        dateExport: this.form.documentDate || null,
        currency: this.form.currency || null,
        customer: {
          code: this.result?.buyer?.matchedCustomerCode || null,
          name: this.form.buyer.name || null,
          buyer: null,
          taxcode: this.form.buyer.taxCode || null,
          address: this.form.buyer.address || null
        },
        detail: details
      })
      this.$toastr?.success('Đã áp dụng dữ liệu vào biểu mẫu; hóa đơn vẫn chưa được lưu', 'Thành công')
    },
    addItem () {
      this.form.items.push({ name: '', unit: '', quantity: '', unitPrice: '', vatRate: this.category === 2 ? '-1' : '', lineTotal: '', matchedProductCode: '' })
    },
    fieldObject (path) {
      const parts = path.split('.')
      let value = this.result
      for (const part of parts) value = value?.[part]
      return value
    },
    isLow (path) { return !this.touched.has(path) && !!this.fieldObject(path)?.lowConfidence },
    lowClass (path) { return { 'low-confidence': this.isLow(path) } },
    touch (path) { this.touched.add(path); this.touched = new Set(this.touched) },
    confidenceLabel (value) { return `tin cậy ${Math.round(Number(value || 0) * 100)}%` },
    reset () {
      this.file = null
      this.result = null
      this.form = null
      this.error = ''
      this.touched = new Set()
      this.revokePreview()
    },
    revokePreview () {
      if (this.previewUrl) URL.revokeObjectURL(this.previewUrl)
      this.previewUrl = ''
    }
  }
}
</script>

<style scoped>
.document-recognition { background: linear-gradient(135deg, #f2fcff 0%, #fff 72%); }
.document-preview { height: 300px; border: 1px solid #d7e7ec; border-radius: 8px; overflow: hidden; background: #f6f8fa; text-align: center; }
.document-preview iframe { width: 100%; height: 100%; border: 0; }
.document-preview img { max-width: 100%; max-height: 100%; object-fit: contain; }
.recognition-result { border: 1px solid #d7e7ec; border-radius: 8px; padding: 14px; background: #fff; }
.item-editor { border: 1px solid #e5e9ed; border-radius: 6px; padding: 9px; }
::v-deep .low-confidence { border-color: #f0ad4e !important; background: #fffaf0; }
</style>
