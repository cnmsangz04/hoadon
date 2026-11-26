export function stockIssueAiSnapshot (form, stockIssueType) {
  const info = form.stockInfo || {}
  return {
    formId: form.formId,
    dateExport: form.dateExport,
    paymentType: form.paymentType,
    currency: form.currency || 'VND',
    customer: {
      code: info.buyerCode || '',
      name: info.buyerName || '',
      taxcode: info.buyerTaxCode || '',
      address: info.buyerAddress || '',
      email: info.buyerEmail || '',
      phone: info.buyerPhone || '',
      bank_name: info.buyerBankName || '',
      bank_no: info.buyerBankAccount || '',
    },
    detail: (form.detail || []).map(row => ({
      code: row.code || '',
      name: row.name || '',
      unit: row.unit || '',
      quantity: Number(stockIssueType === 1 ? row.quantityExport || 0 : row.quantity || 0),
      price: Number(row.price || 0),
      vatRate: -1,
      feature: row.feature || 1,
    })),
  }
}

export function applyStockIssueAi (form, suggestion, stockIssueType) {
  if (!suggestion) return form
  const customer = suggestion.customer || {}
  const info = { ...(form.stockInfo || {}) }
  const fields = {
    buyerCode: customer.code,
    buyerName: customer.name || customer.buyer,
    buyerTaxCode: customer.taxcode,
    buyerAddress: customer.address,
    buyerEmail: customer.email,
    buyerPhone: customer.phone,
    buyerBankName: customer.bank_name,
    buyerBankAccount: customer.bank_no,
  }
  Object.entries(fields).forEach(([key, value]) => {
    if (value != null && String(value).trim()) info[key] = value
  })

  const previous = form.detail || []
  const detail = Array.isArray(suggestion.detail) && suggestion.detail.length
    ? suggestion.detail.map((row, index) => {
      const old = previous[index]
      const sameItem = old && (row.code && old.code
        ? row.code === old.code
        : row.name && row.name === old.name)
      const quantity = Number(row.quantity || 0)
      const price = Number(row.price || 0)
      const total = Number(row.total ?? quantity * price)
      return {
        ...(sameItem ? old : {}),
        num: index + 1,
        isNum: true,
        hidden: false,
        disable: false,
        feature: row.feature || 1,
        code: row.code || '',
        name: row.name || '',
        unit: row.unit || '',
        ...(stockIssueType === 1
          ? { quantityExport: quantity, quantityImport: sameItem ? old.quantityImport || 0 : 0 }
          : { quantity }),
        price,
        total,
        amount: Number(row.amount ?? total),
      }
    })
    : previous

  return {
    ...form,
    dateExport: suggestion.dateExport || form.dateExport,
    currency: suggestion.currency || form.currency,
    paymentType: suggestion.paymentType || form.paymentType,
    stockInfo: info,
    detail,
  }
}
