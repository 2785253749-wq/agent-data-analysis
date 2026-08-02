import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const mocks = vi.hoisted(() => ({
  getUsers: vi.fn(), createUser: vi.fn(), updateUser: vi.fn(),
  resetUserPassword: vi.fn(), deleteUser: vi.fn(),
}))
vi.mock('@/api/users', () => ({
  getUsers: mocks.getUsers, createUser: mocks.createUser, updateUser: mocks.updateUser,
  resetUserPassword: mocks.resetUserPassword, deleteUser: mocks.deleteUser,
}))

import { useUsersStore } from '@/stores/users'

describe('users store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads users with role filter', async () => {
    mocks.getUsers.mockResolvedValue({
      content: [{ id: 1, username: 'admin', role: 'ADMIN', isEnabled: true, orgId: 0, displayName: '', createdAt: '' }],
      totalElements: 1,
    })
    const store = useUsersStore()
    await store.setRole('ADMIN')

    expect(mocks.getUsers).toHaveBeenCalledWith('ADMIN', 0, 20)
    expect(store.users).toHaveLength(1)
  })

  it('creates then refreshes', async () => {
    mocks.getUsers.mockResolvedValue({ content: [], totalElements: 0 })
    const store = useUsersStore()
    await store.create({ username: 'a', password: 'p', role: 'ANALYST' })
    expect(mocks.createUser).toHaveBeenCalled()
  })

  it('resets password', async () => {
    const store = useUsersStore()
    await store.resetPassword(2, 'newpass')
    expect(mocks.resetUserPassword).toHaveBeenCalledWith(2, 'newpass')
  })
})
