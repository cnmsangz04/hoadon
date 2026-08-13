export function getAuthToken(key) {
  if (!key) return ''
  try {
    return localStorage.getItem(key) || sessionStorage.getItem(key) || ''
  } catch {
    return ''
  }
}

export function setAuthToken(key, token, remember) {
  if (!key) return
  try {
    localStorage.removeItem(key)
    sessionStorage.removeItem(key)
    if (!token) return
    const storage = remember ? localStorage : sessionStorage
    storage.setItem(key, token)
  } catch {}
}

export function removeAuthToken(key) {
  if (!key) return
  try {
    localStorage.removeItem(key)
    sessionStorage.removeItem(key)
  } catch {}
}
