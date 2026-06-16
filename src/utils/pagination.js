function firstNumber(...values) {
  for (const value of values) {
    if (value === null || value === undefined || value === '') continue
    const number = Number(value)
    if (Number.isFinite(number)) return number
  }
  return 0
}

export function pageItems(data) {
  if (Array.isArray(data?.content)) return data.content
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data?.items)) return data.items
  if (Array.isArray(data)) return data
  return []
}

export function pageTotal(data) {
  return firstNumber(
    data?.totalElements,
    data?.total_elements,
    data?.total,
    data?.page?.totalElements,
    data?.page?.total_elements,
    data?.page?.total,
    data?.totalRows
  )
}

export function pageNumber(data, fallback = 1) {
  const zeroBased = data?.number ?? data?.page?.number
  if (zeroBased !== null && zeroBased !== undefined && zeroBased !== '') {
    return Math.max(1, firstNumber(zeroBased) + 1)
  }
  return Math.max(1, firstNumber(data?.current_page, data?.currentPage, fallback))
}

export function pageSize(data, fallback = 10) {
  return Math.max(1, firstNumber(data?.size, data?.page?.size, data?.per_page, data?.perPage, fallback))
}

export function pageLast(data, fallbackSize = 10) {
  return Math.max(1, firstNumber(
    data?.totalPages,
    data?.total_pages,
    data?.last_page,
    data?.page?.totalPages,
    data?.page?.total_pages,
    Math.ceil(pageTotal(data) / pageSize(data, fallbackSize))
  ))
}

export function pageFrom(data, currentPage = 1, perPage = 10) {
  const total = pageTotal(data)
  if (total <= 0) return 0
  return firstNumber(data?.from, (Math.max(1, Number(currentPage) || 1) - 1) * Math.max(1, Number(perPage) || 10) + 1)
}

export function pageTo(data, itemCount = 0, currentPage = 1, perPage = 10) {
  const total = pageTotal(data)
  if (total <= 0) return 0
  const from = pageFrom(data, currentPage, perPage)
  return firstNumber(data?.to, Math.min(total, from + Math.max(0, Number(itemCount) || 0) - 1))
}
