<template>
  <section class="document-recognition" :class="{ 'is-embedded': embedded }">
    <div v-if="!embedded" class="recognition-heading">
      <i class="far fa-file-image" aria-hidden="true"></i>
      <span>Nhận diện chứng từ</span>
    </div>
    <div class="recognition-workflow">
      <div v-if="!embedded" class="recognition-upload">
        <label class="sr-only" :for="`recognition-file-${_uid}`">Chọn ảnh hoặc PDF chứng từ</label>
        <b-form-file
          :id="`recognition-file-${_uid}`"
          v-model="file"
          accept=".jpg,.jpeg,.png,.pdf,image/jpeg,image/png,application/pdf"
          browse-text="Chọn tệp"
          placeholder="Chọn ảnh hoặc PDF chứng từ"
          drop-placeholder="Thả chứng từ vào đây"
          :disabled="busy"
          @input="fileChanged"
        />
      </div>
      <div v-if="!embedded" class="recognition-footer">
        <span class="recognition-hint"><i class="fas fa-lock" aria-hidden="true"></i> Xử lý nội bộ · Tối đa 10 MB</span>
        <div class="recognition-buttons">
          <b-button v-if="result || file" size="sm" variant="outline-secondary" :disabled="busy" @click="reset">Hủy</b-button>
          <b-button size="sm" variant="primary" :disabled="busy || !file" @click="recognize">
            <b-spinner v-if="busy" small class="mr-1" /><i v-else class="fas fa-search mr-1" aria-hidden="true"></i>
            {{ busy ? 'Đang nhận diện...' : 'Nhận diện' }}
          </b-button>
        </div>
      </div>
      <div v-if="previewUrl" class="document-preview">
        <iframe v-if="isPdf" :src="previewUrl" title="Xem trước PDF" />
        <img v-else :src="previewUrl" alt="Xem trước chứng từ" />
      </div>

      <b-alert v-if="error" show variant="danger" class="recognition-message mt-2 mb-0">{{ error }}</b-alert>
      <b-alert v-if="busy" show variant="info" class="recognition-message mt-2 mb-0">
        Hệ thống đang trích xuất dữ liệu. Chứng từ nhiều trang hoặc ảnh mờ có thể cần thêm thời gian.
      </b-alert>

      <div v-if="result && form" class="recognition-result mt-2">
        <div class="recognition-result-heading">
          <strong>Kết quả nhận diện</strong>
          <span class="small text-muted">
            {{ sourceLabel }} · {{ result.pageCount }} trang · {{ confidenceLabel(result.averageConfidence) }}
          </span>
        </div>
        <b-alert v-if="result.warnings && result.warnings.length" show variant="warning">
          <div v-for="warning in result.warnings" :key="warning">• {{ warning }}</div>
        </b-alert>

        <div class="recognition-parties">
          <div class="recognition-section">
            <h6>{{ category === 6 ? 'Đơn vị xuất' : 'Người bán' }}</h6>
            <b-form-group :label="category === 6 ? 'Đơn vị xuất' : 'Tên người bán'" label-class="small mb-1"><b-form-input v-model.trim="form.seller.name" :class="lowClass('seller.name')" @input="touch('seller.name')" /></b-form-group>
            <b-form-group label="Mã số thuế" label-class="small mb-1"><b-form-input v-model.trim="form.seller.taxCode" :class="lowClass('seller.taxCode')" @input="touch('seller.taxCode')" /></b-form-group>
            <b-form-group label="Địa chỉ" label-class="small mb-1"><b-form-input v-model.trim="form.seller.address" :class="lowClass('seller.address')" @input="touch('seller.address')" /></b-form-group>
          </div>
          <div class="recognition-section">
            <h6>{{ category === 6 ? 'Đơn vị nhận' : 'Người mua' }}</h6>
            <b-form-group :label="category === 6 ? 'Đơn vị nhận' : 'Tên người mua'" label-class="small mb-1"><b-form-input v-model.trim="form.buyer.name" :class="lowClass('buyer.name')" @input="touch('buyer.name')" /></b-form-group>
            <b-form-group label="Mã số thuế" label-class="small mb-1"><b-form-input v-model.trim="form.buyer.taxCode" :class="lowClass('buyer.taxCode')" @input="touch('buyer.taxCode')" /></b-form-group>
            <b-form-group label="Địa chỉ" label-class="small mb-1"><b-form-input v-model.trim="form.buyer.address" :class="lowClass('buyer.address')" @input="touch('buyer.address')" /></b-form-group>
          </div>
        </div>
        <div class="recognition-fields">
          <div>
            <b-form-group label="Ngày chứng từ" label-class="small mb-1"><b-form-input v-model="form.documentDate" type="date" :class="lowClass('documentDate')" @input="touch('documentDate')" /></b-form-group>
          </div>
          <div>
            <b-form-group label="Tiền tệ" label-class="small mb-1"><b-form-input v-model.trim="form.currency" :class="lowClass('currency')" @input="touch('currency')" /></b-form-group>
          </div>
          <div>
            <b-form-group label="Tiền hàng" label-class="small mb-1"><b-form-input v-model="form.subtotal" :class="lowClass('subtotal')" @input="touch('subtotal')" /></b-form-group>
          </div>
          <div v-if="category !== 6">
            <b-form-group label="Tiền thuế" label-class="small mb-1"><b-form-input v-model="form.vatAmount" :class="lowClass('vatAmount')" @input="touch('vatAmount')" /></b-form-group>
          </div>
          <div>
            <b-form-group label="Tổng thanh toán" label-class="small mb-1"><b-form-input v-model="form.total" :class="lowClass('total')" @input="touch('total')" /></b-form-group>
          </div>
        </div>

        <div class="font-weight-bold mb-2">Hàng hóa, dịch vụ</div>
        <div v-if="!form.items.length" class="text-muted small mb-3">Chưa nhận diện chắc chắn được dòng hàng hóa.</div>
        <div v-for="(item, index) in form.items" :key="index" class="item-editor mb-2">
          <div class="item-editor-heading">
            <span>Dòng {{ index + 1 }}</span>
            <b-button size="sm" variant="link" class="item-delete" title="Xóa dòng" :aria-label="`Xóa dòng ${index + 1}`" @click="form.items.splice(index, 1)"><i class="far fa-trash-alt" aria-hidden="true"></i></b-button>
          </div>
          <div class="recognition-fields">
            <b-form-group label="Tên hàng hóa, dịch vụ" label-class="small mb-1" class="field-full"><b-form-input v-model.trim="item.name" :class="lowClass(`items.${index}.name`)" @input="touch(`items.${index}.name`)" /></b-form-group>
            <b-form-group label="Đơn vị tính" label-class="small mb-1"><b-form-input v-model.trim="item.unit" :class="lowClass(`items.${index}.unit`)" @input="touch(`items.${index}.unit`)" /></b-form-group>
            <b-form-group :label="category === 6 ? 'Thực xuất' : 'Số lượng'" label-class="small mb-1"><b-form-input v-model="item.quantity" :class="lowClass(`items.${index}.quantity`)" @input="touch(`items.${index}.quantity`)" /></b-form-group>
            <b-form-group label="Đơn giá" label-class="small mb-1"><b-form-input v-model="item.unitPrice" :class="lowClass(`items.${index}.unitPrice`)" @input="touch(`items.${index}.unitPrice`)" /></b-form-group>
            <b-form-group v-if="category !== 6" label="Thuế suất (%)" label-class="small mb-1"><b-form-input v-model="item.vatRate" :disabled="category === 2" :class="lowClass(`items.${index}.vatRate`)" @input="touch(`items.${index}.vatRate`)" /></b-form-group>
            <b-form-group label="Thành tiền" label-class="small mb-1"><b-form-input v-model="item.lineTotal" :class="lowClass(`items.${index}.lineTotal`)" @input="touch(`items.${index}.lineTotal`)" /></b-form-group>
          </div>
          <div v-if="item.matchedProductCode" class="small text-success mt-1">Khớp danh mục: {{ item.matchedProductCode }}</div>
        </div>
        <b-button size="sm" variant="outline-primary" @click="addItem"><i class="fas fa-plus mr-1"></i>Thêm dòng</b-button>

        <div class="result-actions">
          <span class="small text-muted">Viền vàng: cần kiểm tra lại · Chưa lưu {{ category === 6 ? 'phiếu' : 'hóa đơn' }}</span>
          <div>
            <b-button size="sm" variant="outline-secondary" class="mr-2" :disabled="busy" @click="recognize">Nhận diện lại</b-button>
            <b-button size="sm" variant="primary" :disabled="busy" @click="apply">Áp dụng vào biểu mẫu</b-button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'DocumentRecognition',
  props: {
    category: { type: Number, required: true },
    embedded: { type: Boolean, default: false }
  },
  data () {
    return {
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
  watch: {
    busy: {
      immediate: true,
      handler () { this.$emit('control-state', { busy: this.busy }) }
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
          unitPrice: value(item.unitPrice), vatRate: this.category === 2 || this.category === 6 ? '-1' : value(item.vatRate),
          lineTotal: value(item.lineTotal), matchedProductId: item.matchedProductId,
          matchedProductCode: item.matchedProductCode
        }))
      }
    },
    async apply () {
      if (!this.form) return
      const documentLabel = this.category === 6 ? 'phiếu xuất kho' : 'hóa đơn nháp'
      const confirmed = await this.$bvModal.msgBoxConfirm(`Áp dụng dữ liệu đã kiểm tra vào biểu mẫu ${documentLabel}?`, {
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
        const vatRate = this.category === 2 || this.category === 6 ? -1 : number(item.vatRate || -1)
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
      this.$toastr?.success(`Đã áp dụng dữ liệu vào biểu mẫu; ${documentLabel} vẫn chưa được lưu`, 'Thành công')
    },
    addItem () {
      this.form.items.push({ name: '', unit: '', quantity: '', unitPrice: '', vatRate: this.category === 2 || this.category === 6 ? '-1' : '', lineTotal: '', matchedProductCode: '' })
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
.document-recognition { min-width: 0; padding: 12px; border: 1px solid #e6ebf2; border-radius: 8px; background: #fff; color: #334155; font-size: 13px; box-shadow: 0 2px 5px rgba(30, 41, 59, .025); }
.document-recognition.is-embedded { min-height: 0; padding: 0; border: 0; background: transparent; box-shadow: none; }
.recognition-heading { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; color: #334155; font-size: 13px; font-weight: 600; line-height: 20px; }
.recognition-heading i { width: 16px; color: #64748b; font-size: 16px; text-align: center; }
.recognition-workflow { min-width: 0; }
.recognition-upload ::v-deep .custom-file { height: 54px; }
.recognition-upload ::v-deep .custom-file-input { height: 54px; cursor: pointer; }
.recognition-upload ::v-deep .custom-file-label { height: 54px; margin: 0; padding: 16px 94px 16px 12px; overflow: hidden; border-color: #d8dee8; border-radius: 6px; color: #64748b; font-size: 13px; line-height: 20px; text-overflow: ellipsis; white-space: nowrap; }
.recognition-upload ::v-deep .custom-file-label::after { top: 10px; right: 10px; bottom: 10px; height: 32px; padding: 6px 10px; border: 1px solid #d8dee8; border-radius: 5px; background: #fff; color: #475569; font-size: 12px; line-height: 18px; }
.recognition-footer { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: 10px; }
.recognition-hint { color: #94a3b8; font-size: 11px; line-height: 16px; }
.recognition-hint i { margin-right: 3px; font-size: 10px; }
.recognition-buttons { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.document-recognition ::v-deep .btn-sm { min-height: 32px; padding: 5px 10px; border-radius: 5px; font-size: 12px; line-height: 20px; }
.document-preview { height: 210px; margin-top: 12px; border: 1px solid #e6ebf2; border-radius: 6px; overflow: hidden; background: #f8fafc; text-align: center; }
.document-preview iframe { width: 100%; height: 100%; border: 0; }
.document-preview img { max-width: 100%; max-height: 100%; object-fit: contain; }
.recognition-result { padding-top: 12px; border-top: 1px solid #e6ebf2; }
.recognition-result-heading { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 4px 10px; margin-bottom: 12px; }
.recognition-parties, .recognition-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 12px; }
.recognition-result ::v-deep .form-group { min-width: 0; margin-bottom: 10px; }
.recognition-result ::v-deep .form-control { height: 34px; border-color: #d8dee8; border-radius: 5px; font-size: 13px; }
.recognition-result ::v-deep label { color: #64748b; font-size: 12px; font-weight: 500; }
.recognition-result h6 { color: #334155; font-size: 13px; font-weight: 600; margin-bottom: 10px; }
.recognition-section { min-width: 0; }
.recognition-fields .field-full { grid-column: 1 / -1; }
.item-editor { background: #f8fafc; border: 1px solid #e6ebf2; border-radius: 6px; padding: 10px; }
.item-editor-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; color: #64748b; font-size: 12px; font-weight: 600; }
.item-editor .item-delete { min-height: 24px; padding: 2px 5px; color: #94a3b8; line-height: 20px; }
.item-editor .item-delete:hover { color: #dc2626; }
.result-actions { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 10px; margin-top: 12px; padding-top: 12px; border-top: 1px solid #e6ebf2; }
.result-actions > div { display: flex; flex-wrap: wrap; gap: 6px; }
.result-actions > div .mr-2 { margin-right: 0 !important; }
::v-deep .low-confidence { border-color: #f0ad4e !important; background: #fffaf0; }
@media (max-width: 575px) {
  .recognition-parties { grid-template-columns: minmax(0, 1fr); }
  .recognition-footer { flex-wrap: wrap; }
  .recognition-buttons { margin-left: auto; }
  .result-actions > div { width: 100%; }
  .result-actions .btn { flex: 1; }
}
</style>
