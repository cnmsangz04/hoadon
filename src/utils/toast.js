import toastr from 'toastr'

// Set toàn cục để theo dõi các khóa lỗi đã hiển thị trong vòng đời app
const shown = new Set()

export function toastError(message, key) {
  const k = key || String(message || '')
  if (shown.has(k)) return
  shown.add(k)
  toastr.error(message, 'Lỗi')
}

export function toastWarning(message, key) {
  const k = key || String(message || '')
  if (shown.has(k)) return
  shown.add(k)
  toastr.warning(message, 'Cảnh báo')
}

export function toastSuccess(message) {
  toastr.success(message, 'Thành công')
}

export function clearToastKeys() {
  shown.clear()
}
