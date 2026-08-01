import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import type { MenuItem } from '@/utils/sidebarMenu'

import AppSidebar from '@/components/layout/AppSidebar.vue'

const NAV_ITEM: MenuItem = { key: '/datasets', path: '/datasets', title: '数据集管理' }
const DEV_ITEM: MenuItem = { key: '/dev/user', title: '用户管理', disabled: true, tip: '该功能开发中' }
const LOGOUT_ITEM: MenuItem = { key: 'logout', title: '退出系统', action: 'logout' }

function makeRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [{ path: '/', component: { template: '<div/>' } }],
  })
}

// Register stub components under the names Element Plus resolves to,
// so el-menu / el-menu-item / el-tooltip / el-icon render as plain elements.
function makeGlobal(router: ReturnType<typeof makeRouter>) {
  return {
    plugins: [router],
    stubs: {
      'el-menu': { template: '<div class="el-menu"><slot /></div>' },
      'el-menu-item': {
        props: ['index', 'disabled'],
        template: '<li class="el-menu-item" :data-index="index" :data-disabled="String(disabled)"><slot name="title" /><slot /></li>',
      },
      'el-tooltip': { template: '<div class="el-tooltip"><slot /></div>' },
      'el-icon': { template: '<i class="el-icon"><slot /></i>' },
    },
  }
}

describe('AppSidebar', () => {
  it('renders all menu titles', () => {
    const wrapper = mount(AppSidebar, {
      props: { menus: [NAV_ITEM, DEV_ITEM, LOGOUT_ITEM], width: 155, datasetId: null },
      global: makeGlobal(makeRouter()),
    })
    const text = wrapper.text()
    expect(text).toContain('数据集管理')
    expect(text).toContain('用户管理')
    expect(text).toContain('退出系统')
  })

  it('marks disabled items as disabled', () => {
    const wrapper = mount(AppSidebar, {
      props: { menus: [NAV_ITEM, DEV_ITEM], width: 155, datasetId: null },
      global: makeGlobal(makeRouter()),
    })
    const devItem = wrapper.findAll('.el-menu-item').find((w) => w.text().includes('用户管理'))
    expect(devItem?.attributes('data-disabled')).toBe('true')
  })

  it('emits logout when logout item is selected', () => {
    const wrapper = mount(AppSidebar, {
      props: { menus: [NAV_ITEM, LOGOUT_ITEM], width: 155, datasetId: null },
      global: makeGlobal(makeRouter()),
    })
    ;(wrapper.vm as any).onSelect('logout')
    expect(wrapper.emitted('logout')).toBeTruthy()
  })

  it('navigates to dataset fields when a dataset is selected', async () => {
    const router = makeRouter()
    await router.push('/')
    await router.isReady()
    const pushSpy = vi.spyOn(router, 'push')

    const wrapper = mount(AppSidebar, {
      props: {
        menus: [
          { key: '/datasets/fields', title: '字段语义管理', needsDataset: true, datasetSuffix: 'fields' } as MenuItem,
        ],
        width: 155,
        datasetId: 5,
      },
      global: makeGlobal(router),
    })

    ;(wrapper.vm as any).onSelect('/datasets/fields')
    expect(pushSpy).toHaveBeenCalledWith('/datasets/5/fields')
  })

  it('prompts to select dataset when none selected', async () => {
    const router = makeRouter()
    await router.push('/')
    await router.isReady()
    const pushSpy = vi.spyOn(router, 'push')

    const wrapper = mount(AppSidebar, {
      props: {
        menus: [
          { key: '/datasets/fields', title: '字段语义管理', needsDataset: true, datasetSuffix: 'fields' } as MenuItem,
        ],
        width: 155,
        datasetId: null,
      },
      global: makeGlobal(router),
    })

    ;(wrapper.vm as any).onSelect('/datasets/fields')
    expect(pushSpy).toHaveBeenCalledWith('/datasets')
  })
})
