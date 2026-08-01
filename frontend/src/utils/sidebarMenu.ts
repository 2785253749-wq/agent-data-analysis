import type { Component } from 'vue'

export interface MenuItem {
  /** el-menu index — stable key used for highlight matching */
  key: string
  /** title shown in the menu */
  title: string
  icon?: Component
  /** real navigation path (undefined for special actions) */
  path?: string
  /** disabled (dev-in-progress): greyed, hover tooltip, click does nothing */
  disabled?: boolean
  /** hover tooltip text for disabled items */
  tip?: string
  /** requires a selected dataset before navigating (fields/metrics) */
  needsDataset?: boolean
  /** dataset-scoped sub-path appended to /datasets/:id/ (fields | metrics) */
  datasetSuffix?: 'fields' | 'metrics'
  /** special action (logout) */
  action?: 'logout'
}

export interface MenuClickContext {
  datasetId?: number | null
}

export type MenuAction =
  | { type: 'navigate'; path: string }
  | { type: 'needs-dataset' }
  | { type: 'logout' }
  | { type: 'ignore' }

/**
 * Resolve what a menu click should do, given the item and current context.
 * Pure function — no Vue/router/store dependencies, easy to unit test.
 */
export function resolveMenuClick(item: MenuItem, ctx: MenuClickContext = {}): MenuAction {
  // Disabled / special-action items never navigate
  if (item.disabled) return { type: 'ignore' }
  if (item.action === 'logout') return { type: 'logout' }

  // Dataset-scoped items (fields/metrics): require a selected dataset
  if (item.needsDataset) {
    if (item.datasetSuffix) {
      if (!ctx.datasetId) return { type: 'needs-dataset' }
      return { type: 'navigate', path: `/datasets/${ctx.datasetId}/${item.datasetSuffix}` }
    }
    // needsDataset but no suffix → fall back to datasets list
    if (!ctx.datasetId) return { type: 'needs-dataset' }
    return { type: 'navigate', path: '/datasets' }
  }

  // Normal navigation
  if (item.path) return { type: 'navigate', path: item.path }
  return { type: 'ignore' }
}

/** App keys owned by this app (reports/history etc.). */
const APP_STORAGE_KEYS = [
  'analysis-latest-v1',
  'analysis-reports-v1',
  'auth-token',
  'user-info',
]

/**
 * Clear token / user info / related localStorage on logout.
 * Returns the list of keys that were removed.
 */
export function clearSessionStorage(): string[] {
  const removed: string[] = []
  for (const key of APP_STORAGE_KEYS) {
    if (localStorage.getItem(key) !== null) {
      localStorage.removeItem(key)
      removed.push(key)
    }
  }
  return removed
}

/**
 * Menu highlight key for the current route path.
 * Fields/metrics sub-pages highlight their own sidebar entry, not "数据集管理".
 */
export function activeMenuKey(path: string): string {
  if (/^\/datasets\/\d+\/fields$/.test(path)) return '/datasets/fields'
  if (/^\/datasets\/\d+\/metrics$/.test(path)) return '/datasets/metrics'
  if (/^\/datasets/.test(path)) return '/datasets'
  if (/^\/reports/.test(path)) return '/reports'
  return path
}
