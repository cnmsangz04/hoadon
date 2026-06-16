export function required(value, message = 'Trường này là bắt buộc') {
  if (value === null || value === undefined || String(value).trim() === '') return message
  return null
}

export function email(value, message = 'Email không đúng định dạng') {
  const text = String(value || '').trim()
  if (!text) return null
  return /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(text) ? null : message
}

export function phone(value, message = 'Số điện thoại không đúng định dạng') {
  const text = String(value || '').trim()
  if (!text) return null
  return /^(\+?84|0)[0-9]{8,10}$/.test(text.replace(/[\s.-]/g, '')) ? null : message
}

export function taxCode(value, message = 'Mã số thuế không đúng định dạng') {
  const text = String(value || '').trim()
  if (!text) return null
  return /^\d{10}(-?\d{3})?$/.test(text) ? null : message
}

export function minLength(value, min, message) {
  const text = String(value || '')
  if (!text) return null
  return text.length >= min ? null : (message || `Vui lòng nhập ít nhất ${min} ký tự`)
}

export function numberRange(value, min, max, message) {
  if (value === null || value === undefined || value === '') return null
  const n = Number(value)
  if (!Number.isFinite(n)) return message || 'Giá trị phải là số'
  if (min !== null && min !== undefined && n < min) return message || `Giá trị phải từ ${min} trở lên`
  if (max !== null && max !== undefined && n > max) return message || `Giá trị không được lớn hơn ${max}`
  return null
}

export function url(value, message = 'Website không đúng định dạng') {
  const text = String(value || '').trim()
  if (!text) return null
  const normalized = /^https?:\/\//i.test(text) ? text : `https://${text}`
  try {
    const parsed = new URL(normalized)
    return parsed.hostname.includes('.') ? null : message
  } catch {
    return message
  }
}

export function firstError(rules) {
  for (const rule of rules) {
    if (rule) return rule
  }
  return null
}

export function hasErrors(errors) {
  return Object.values(errors || {}).some(Boolean)
}
