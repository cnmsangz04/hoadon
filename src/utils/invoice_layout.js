export function findBlock(root, id, parent = null) {
  if (!root) return null
  if (root.id === id) return { block: root, parent }
  for (const child of root.children || []) {
    const found = findBlock(child, id, root)
    if (found) return found
  }
  return null
}

export function blockId() {
  return `b${
    window.crypto?.randomUUID?.().replace(/-/g, '') ||
    Date.now().toString(36) + Math.random().toString(36).slice(2)
  }`
}

export function copyBlock(block) {
  const copy = JSON.parse(JSON.stringify(block))
  const assignIds = (node) => {
    node.id = blockId()
    ;(node.children || []).forEach(assignIds)
  }
  assignIds(copy)
  return copy
}

export function placeBlock(root, block, targetId, placement, moving = false) {
  const target = findBlock(root, targetId)
  const source = moving ? findBlock(root, block.id) : null
  if (!target || (moving && (!source?.parent || findBlock(block, targetId)))) return false
  if (placement === 'inside' && !Array.isArray(target.block.children)) return false
  if (placement !== 'inside' && !target.parent) return false
  if (placement === 'inside' && target.block.type === 'columns') {
    const count = target.block.children.length - (source?.parent === target.block ? 1 : 0)
    if (count >= 6) return false
  }
  if (['before', 'after'].includes(placement) && target.parent?.type === 'columns') {
    const count = target.parent.children.length - (source?.parent === target.parent ? 1 : 0)
    if (count >= 6) return false
  }
  if (!['inside', 'before', 'after', 'left', 'right'].includes(placement)) return false
  if (source) source.parent.children.splice(source.parent.children.indexOf(block), 1)
  if (placement === 'inside') {
    target.block.children.push(block)
  } else {
    const index = target.parent.children.indexOf(target.block)
    if (placement === 'left' || placement === 'right') {
      target.parent.children.splice(index, 1, {
        id: blockId(),
        type: 'columns',
        style: {},
        gapMm: 3,
        children: placement === 'left' ? [block, target.block] : [target.block, block]
      })
    } else target.parent.children.splice(index + (placement === 'after' ? 1 : 0), 0, block)
  }
  return true
}
