import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { MenuItem } from '@/utils/sidebarMenu'
import {
  resolveMenuClick, clearSessionStorage, activeMenuKey,
} from '@/utils/sidebarMenu'

// ---- Fixtures ----

const NAV_ITEM: MenuItem = { key: '/datasets', path: '/datasets', title: '数据集管理' }
const FIELD_ITEM: MenuItem = {
  key: '/datasets/fields', title: '字段语义管理', needsDataset: true, datasetSuffix: 'fields',
}
const METRIC_ITEM: MenuItem = {
  key: '/datasets/metrics', title: '指标口径管理', needsDataset: true, datasetSuffix: 'metrics',
}
const DEV_ITEM: MenuItem = { key: '/dev/user', title: '用户管理', disabled: true, tip: '该功能开发中' }
const LOGOUT_ITEM: MenuItem = { key: 'logout', title: '退出系统', action: 'logout' }

describe('resolveMenuClick — implemented navigation', () => {
  it('navigates to a plain path item', () => {
    const action = resolveMenuClick(NAV_ITEM, { datasetId: null })
    expect(action).toEqual({ type: 'navigate', path: '/datasets' })
  })

  it('fields item navigates to /datasets/:id/fields when a dataset is selected', () => {
    const action = resolveMenuClick(FIELD_ITEM, { datasetId: 3 })
    expect(action).toEqual({ type: 'navigate', path: '/datasets/3/fields' })
  })

  it('metrics item navigates to /datasets/:id/metrics when a dataset is selected', () => {
    const action = resolveMenuClick(METRIC_ITEM, { datasetId: 7 })
    expect(action).toEqual({ type: 'navigate', path: '/datasets/7/metrics' })
  })

  it('fields item returns needs-dataset when no dataset is selected', () => {
    const action = resolveMenuClick(FIELD_ITEM, { datasetId: null })
    expect(action).toEqual({ type: 'needs-dataset' })
  })

  it('metrics item returns needs-dataset when no dataset is selected', () => {
    const action = resolveMenuClick(METRIC_ITEM, {})
    expect(action).toEqual({ type: 'needs-dataset' })
  })
})

describe('resolveMenuClick — disabled (dev-in-progress)', () => {
  it('ignores click on disabled items', () => {
    const action = resolveMenuClick(DEV_ITEM, { datasetId: null })
    expect(action).toEqual({ type: 'ignore' })
  })

  it('ignores click on disabled items even with a dataset selected', () => {
    const action = resolveMenuClick(DEV_ITEM, { datasetId: 1 })
    expect(action).toEqual({ type: 'ignore' })
  })
})

describe('resolveMenuClick — logout', () => {
  it('returns logout action', () => {
    const action = resolveMenuClick(LOGOUT_ITEM, {})
    expect(action).toEqual({ type: 'logout' })
  })
})

describe('clearSessionStorage — logout cleanup', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('removes app-owned keys on logout', () => {
    localStorage.setItem('analysis-latest-v1', '{}')
    localStorage.setItem('auth-token', 'abc')
    localStorage.setItem('user-info', 'admin')
    localStorage.setItem('keep-me', 'x') // unrelated key

    const removed = clearSessionStorage()

    expect(removed).toContain('analysis-latest-v1')
    expect(removed).toContain('auth-token')
    expect(removed).toContain('user-info')
    expect(localStorage.getItem('analysis-latest-v1')).toBeNull()
    expect(localStorage.getItem('auth-token')).toBeNull()
    expect(localStorage.getItem('user-info')).toBeNull()
    expect(localStorage.getItem('keep-me')).toBe('x') // untouched
  })

  it('returns empty array when no app keys present', () => {
    const removed = clearSessionStorage()
    expect(removed).toEqual([])
  })
})

describe('activeMenuKey — sidebar highlight', () => {
  it('highlights fields menu on /datasets/:id/fields', () => {
    expect(activeMenuKey('/datasets/3/fields')).toBe('/datasets/fields')
  })

  it('highlights metrics menu on /datasets/:id/metrics', () => {
    expect(activeMenuKey('/datasets/3/metrics')).toBe('/datasets/metrics')
  })

  it('highlights datasets menu on any other /datasets path', () => {
    expect(activeMenuKey('/datasets')).toBe('/datasets')
    expect(activeMenuKey('/datasets/new')).toBe('/datasets')
  })

  it('highlights reports menu on /reports', () => {
    expect(activeMenuKey('/reports')).toBe('/reports')
  })

  it('highlights history menu on /history', () => {
    expect(activeMenuKey('/history')).toBe('/history')
  })

  it('highlights trace menu on /trace', () => {
    expect(activeMenuKey('/trace')).toBe('/trace')
  })

  it('falls back to exact path for home', () => {
    expect(activeMenuKey('/')).toBe('/')
  })
})
