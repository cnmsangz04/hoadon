export function appResourceUrl(value) {
  if (typeof value !== 'string' || !value.trim()) return value || ''

  const source = value.trim()
  if (typeof window === 'undefined' || !window.location) return source

  try {
    const url = new URL(source, window.location.origin)
    if (url.pathname === '/uploads' || url.pathname.startsWith('/uploads/') ||
        url.pathname === '/v1/uploads' || url.pathname.startsWith('/v1/uploads/')) {
      return `${url.pathname}${url.search}${url.hash}`
    }
  } catch (_) {}
  return source
}
