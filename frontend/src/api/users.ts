import apiClient from './client'
import type { Paged } from './adminModels'

export interface User {
  id: number
  username: string
  displayName: string | null
  role: string
  orgId: number
  isEnabled: boolean
  createdAt: string
}

export interface CreateUserRequest {
  username: string
  password: string
  displayName?: string
  role: string
  isEnabled?: boolean
}

export interface UpdateUserRequest {
  displayName?: string
  role?: string
  isEnabled?: boolean
}

export async function getUsers(role?: string, page = 0, size = 20): Promise<Paged<User>> {
  const params: Record<string, string | number> = { page, size }
  if (role) params.role = role
  const { data } = await apiClient.get('/admin/users', { params })
  return data
}

export async function createUser(req: CreateUserRequest): Promise<User> {
  const { data } = await apiClient.post('/admin/users', req)
  return data
}

export async function updateUser(id: number, req: UpdateUserRequest): Promise<User> {
  const { data } = await apiClient.put(`/admin/users/${id}`, req)
  return data
}

export async function resetUserPassword(id: number, newPassword: string): Promise<void> {
  await apiClient.post(`/admin/users/${id}/reset-password`, null, { params: { newPassword } })
}

export async function deleteUser(id: number): Promise<void> {
  await apiClient.delete(`/admin/users/${id}`)
}
