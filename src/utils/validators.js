import i18n from '@/i18n'

function translatedMessage(key, values) {
  return i18n.t(`validation.${key}`, values)
}

export function required(value, customMessage) {
  const errorMessage = customMessage || translatedMessage('required')
  if (value === null || value === undefined || String(value).trim() === '') return errorMessage
  return null
}

export function email(value, customMessage) {
  const errorMessage = customMessage || translatedMessage('email')
  const text = String(value || '').trim()
  if (!text) return null
  return /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(text) ? null : errorMessage
}

export function phone(value, customMessage) {
  const errorMessage = customMessage || translatedMessage('phone')
  const text = String(value || '').trim()
  if (!text) return null
  return /^(\+?84|0)[0-9]{8,10}$/.test(text.replace(/[\s.-]/g, '')) ? null : errorMessage
}

export function taxCode(value, customMessage) {
  const errorMessage = customMessage || translatedMessage('taxCode')
  const text = String(value || '').trim()
  if (!text) return null
  return /^(?:\d{10}(?:-?\d{3})?|\d{12})$/.test(text) ? null : errorMessage
}

export function minLength(value, min, customMessage) {
  const text = String(value || '')
  if (!text) return null
  return text.length >= min ? null : (customMessage || translatedMessage('minLength', { min }))
}

export function numberRange(value, min, max, customMessage) {
  if (value === null || value === undefined || value === '') return null
  const n = Number(value)
  if (!Number.isFinite(n)) return customMessage || translatedMessage('number')
  if (min !== null && min !== undefined && n < min) return customMessage || translatedMessage('minimum', { min })
  if (max !== null && max !== undefined && n > max) return customMessage || translatedMessage('maximum', { max })
  return null
}

export function url(value, customMessage) {
  const errorMessage = customMessage || translatedMessage('url')
  const text = String(value || '').trim()
  if (!text) return null
  const normalized = /^https?:\/\//i.test(text) ? text : `https://${text}`
  try {
    const parsed = new URL(normalized)
    return parsed.hostname.includes('.') ? null : errorMessage
  } catch {
    return errorMessage
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
