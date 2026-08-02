import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { User, CreateUserRequest, UpdateUserRequest } from '@/api/users'
import { getUsers, createUser, updateUser, resetUserPassword, deleteUser } from '@/api/users'

export const useUsersStore = defineStore('users', () => {
  const users = ref<User[]>([])
  const loading = ref(false)
  const page = ref(0)
  const size = ref(20)
  const total = ref(0)
  const roleFilter = ref('')

  async function fetchUsers() {
    loading.value = true
    try {
      const r = await getUsers(roleFilter.value || undefined, page.value, size.value)
      users.value = r.content
      total.value = r.totalElements
    } finally {
      loading.value = false
    }
  }

  async function create(req: CreateUserRequest) {
    await createUser(req)
    await fetchUsers()
  }

  async function update(id: number, req: UpdateUserRequest) {
    await updateUser(id, req)
    await fetchUsers()
  }

  async function resetPassword(id: number, newPassword: string) {
    await resetUserPassword(id, newPassword)
  }

  async function remove(id: number) {
    await deleteUser(id)
    await fetchUsers()
  }

  function setRole(r: string) {
    roleFilter.value = r
    page.value = 0
    return fetchUsers()
  }

  return {
    users, loading, page, size, total, roleFilter,
    fetchUsers, create, update, resetPassword, remove, setRole,
  }
})
