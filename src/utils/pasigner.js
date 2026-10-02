export function confirmUsbTokenSignature (content) {
  if (typeof window.$?.confirm !== 'function') {
    return Promise.resolve(window.confirm(content))
  }

  return new Promise(resolve => {
    window.$.confirm({
      title: 'Xác nhận ký số',
      content,
      theme: 'bootstrap',
      type: 'blue',
      icon: 'fas fa-signature',
      animation: 'zoom',
      closeAnimation: 'scale',
      boxWidth: '420px',
      useBootstrap: true,
      backgroundDismiss: true,
      escapeKey: 'cancel',
      buttons: {
        cancel: { text: 'Hủy', btnClass: 'btn-light', action: () => resolve(false) },
        ok: { text: 'Đồng ý', btnClass: 'btn-primary', action: () => resolve(true) }
      }
    })
  })
}

export function startSignaturePolling ({
  checkStatus,
  onSigned,
  onFailed,
  onTimeout,
  intervalMs = 1200,
  maxAttempts = 100
}) {
  let timer = null
  let attempts = 0
  let cancelled = false

  const stop = () => {
    cancelled = true
    if (timer) clearTimeout(timer)
    timer = null
  }

  const check = async () => {
    if (cancelled) return
    try {
      const data = await checkStatus()
      if (data?.state === 'signed') {
        stop()
        await onSigned?.(data)
        return
      }
      if (data?.state === 'failed') {
        stop()
        await onFailed?.(data)
        return
      }
    } catch (e) {
      // Keep the signing session alive across short network interruptions.
    }

    attempts += 1
    if (attempts >= maxAttempts) {
      stop()
      await onTimeout?.()
      return
    }
    timer = setTimeout(check, intervalMs)
  }

  timer = setTimeout(check, intervalMs)
  return stop
}
