import toastr from 'toastr'

toastr.options = {
  ...(toastr.options || {}),
  preventDuplicates: true
}

// Set toàn cục để theo dõi các khóa lỗi đang hiển thị
const shown = new Set()

export function toastError(message, key) {
  const k = key || String(message || '')
  if (shown.has(k)) return
  shown.add(k)
  toastr.error(message, 'Lỗi')
  setTimeout(() => shown.delete(k), 3500)
}

export function toastWarning(message, key) {
  const k = key || String(message || '')
  if (shown.has(k)) return
  shown.add(k)
  toastr.warning(message, 'Cảnh báo')
  setTimeout(() => shown.delete(k), 3500)
}

export function toastSuccess(message) {
  toastr.success(message, 'Thành công')
}

export function clearToastKeys() {
  shown.clear()
}
