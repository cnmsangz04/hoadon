import axios from '@/plugins/axios'

export default {
  data() {
    return {
      customersRaw: [],
      productsRaw: [],
      loadingCustomers: false,
      loadingProducts: false,
      lookingUpTaxCode: false,
      customerPicker: '',
      selectedCustomer: null,
    }
  },
  watch: {
    'form.stockInfo.buyerName': {
      immediate: true,
      handler(value) {
        if (!this.customerPicker && value) this.customerPicker = value
        if (!this.selectedCustomer && value)
          this.selectedCustomer = this.findStockCustomer(value)
      },
    },
    customersRaw(items) {
      const name =
        this.form && this.form.stockInfo && this.form.stockInfo.buyerName
      if (!this.selectedCustomer && name && Array.isArray(items))
        this.selectedCustomer = this.findStockCustomer(name)
    },
  },
  mounted() {
    this.refreshStockCustomers()
    this.refreshStockProducts()
  },
  methods: {
    async lookupStockTaxCode() {
      const info = this.form.stockInfo || (this.form.stockInfo = {})
      let taxcode = String(info.buyerTaxCode || '').replace(/\s+/g, '')
      if (/^\d{13}$/.test(taxcode))
        taxcode = `${taxcode.slice(0, 10)}-${taxcode.slice(10)}`
      if (!/^(?:\d{10}(?:-\d{3})?|\d{12})$/.test(taxcode)) {
        this.$toastr.error(
          'Mã số thuế phải gồm 10 số, 10 số-3 số chi nhánh hoặc 12 số',
          'Lỗi'
        )
        return
      }

      this.lookingUpTaxCode = true
      try {
        const { data } = await axios.get(
          `/tax-code-lookup/${encodeURIComponent(taxcode)}`,
          {
            meta: { suppressGlobalErrorToast: true },
          }
        )
        info.buyerTaxCode = data.taxCode || taxcode
        info.buyerName = data.name || info.buyerName
        info.buyerAddress = data.address || info.buyerAddress
        const status = data.status ? ` - ${data.status}` : ''
        this.$toastr.success(
          `Đã lấy thông tin: ${data.name}${status}`,
          'Tra cứu mã số thuế'
        )
      } catch (e) {
        const message =
          e?.response?.data?.message || 'Không tra cứu được mã số thuế lúc này'
        this.$toastr.error(message, 'Lỗi')
      } finally {
        this.lookingUpTaxCode = false
      }
    },
    customerOptionValue(item) {
      if (!item) return ''
      const code = String(item.code || '').trim()
      const name = String(item.companyName || item.buyerName || '').trim()
      const id = item.id != null ? ` (#${item.id})` : ''
      return `${[code, name].filter(Boolean).join(' - ')}${id}`.trim()
    },
    productOptionValue(item) {
      if (!item) return ''
      const code = String(item.code || item.productCode || '').trim()
      const name = String(item.name || item.productName || '').trim()
      const id = item.id != null ? ` (#${item.id})` : ''
      return `${[code, name].filter(Boolean).join(' - ')}${id}`.trim()
    },
    async refreshStockCustomers() {
      this.loadingCustomers = true
      try {
        const { data } = await axios.post(
          '/categories/customer/list',
          {},
          {
            params: { page: 0, size: 100 },
            meta: { suppressGlobalErrorToast: true },
          }
        )
        this.customersRaw = data && Array.isArray(data.data) ? data.data : []
      } catch (e) {
        this.customersRaw = []
      } finally {
        this.loadingCustomers = false
      }
    },
    async refreshStockProducts() {
      this.loadingProducts = true
      try {
        const { data } = await axios.post(
          '/categories/product/list',
          {},
          {
            params: { page: 0, size: 200 },
            meta: { suppressGlobalErrorToast: true },
          }
        )
        this.productsRaw = data && Array.isArray(data.data) ? data.data : []
      } catch (e) {
        this.productsRaw = []
      } finally {
        this.loadingProducts = false
      }
    },
    findStockCustomer(input) {
      const value = String(input || '')
        .trim()
        .toLowerCase()
      if (!value) return null
      return (
        this.customersRaw.find((item) => {
          const option = this.customerOptionValue(item).toLowerCase()
          const code = String(item.code || '')
            .trim()
            .toLowerCase()
          return option === value || code === value
        }) || null
      )
    },
    onStockCustomerChange() {
      const raw = String(this.customerPicker || '').trim()
      const customer = this.findStockCustomer(raw)
      const info = this.form.stockInfo || (this.form.stockInfo = {})
      if (!customer) {
        info.buyerName = raw
        return
      }
      this.applyStockCustomer(customer)
      this.customerPicker = info.buyerCode
    },
    onStockCustomerSelect(customer) {
      if (!customer) return
      this.customerPicker = this.customerOptionValue(customer)
      this.applyStockCustomer(customer)
    },
    applyStockCustomer(customer) {
      const info = this.form.stockInfo || (this.form.stockInfo = {})
      info.buyerCode = customer.code || customer.customerCode || ''
      info.buyerName = customer.companyName || customer.buyerName || ''
      info.buyerTaxCode = customer.taxCode || customer.taxcode || ''
      info.buyerAddress = customer.companyAddress || customer.address || ''
      info.buyerEmail = customer.email || ''
      info.buyerPhone = customer.phone || ''
      info.buyerBankName = customer.bankName || customer.bank_name || ''
      info.buyerBankAccount =
        customer.bankAccountNumber || customer.bank_no || ''
      this.selectedCustomer = customer
    },
    findStockProduct(input) {
      const value = String(input || '')
        .trim()
        .toLowerCase()
      if (!value) return null
      return (
        this.productsRaw.find((item) => {
          const option = this.productOptionValue(item).toLowerCase()
          const code = String(item.code || item.productCode || '')
            .trim()
            .toLowerCase()
          const name = String(item.name || item.productName || '')
            .trim()
            .toLowerCase()
          return option === value || code === value || name === value
        }) || null
      )
    },
    onStockProductChange(row) {
      const product = this.findStockProduct(row && (row.name || row.code))
      if (!product || !row) return
      this.applyStockProduct(product, row)
    },
    onStockProductSelect(product, row) {
      if (!product || !row) return
      this.applyStockProduct(product, row)
    },
    applyStockProduct(product, row) {
      row.code = product.code || product.productCode || row.code || ''
      row.name = product.name || product.productName || row.name || ''
      row.unit =
        product.unit ||
        product.unitName ||
        product.unit_name ||
        product.uom ||
        row.unit ||
        ''
      const price = product.price ?? product.sale_price ?? product.unit_price
      if (price != null && price !== '') {
        row.price = Number(price) || 0
        row.total = Number(row.quantityExport ?? row.quantity ?? 0) * row.price
        row.amount = row.total
      }
    },
  },
}
