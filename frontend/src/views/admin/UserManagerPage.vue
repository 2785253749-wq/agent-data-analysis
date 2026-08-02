<template>
  <div class="user-manager-page">
    <FilterToolbar title="用户管理">
      <template #left-extra>
        <el-select v-model="store.roleFilter" placeholder="全部角色" clearable style="width: 140px" @change="onRoleChange">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="分析员" value="ANALYST" />
        </el-select>
      </template>
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
      </template>
    </FilterToolbar>

    <DataTableCard title="用户列表">
      <el-table :data="store.users" v-loading="store.loading" stripe>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="displayName" label="显示名" min-width="120">
          <template #default="{ row }">{{ row.displayName || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="90">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" effect="light" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '分析员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled ? 'success' : 'info'" effect="light" size="small">
              {{ row.isEnabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row as User)">编辑</el-button>
            <el-button link type="warning" @click="openReset(row as User)">重置密码</el-button>
            <el-button link type="danger" :disabled="row.username === 'admin'" @click="onDelete(row as User)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-pagination
          background layout="total, prev, pager, next"
          :total="store.total" :current-page="store.page + 1" :page-size="store.size"
          @current-change="(p: number) => { store.page = p - 1; store.fetchUsers() }"
        />
      </template>
    </DataTableCard>

    <!-- Create/Edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑用户' : '新增用户'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="!!editing" />
        </el-form-item>
        <el-form-item v-if="!editing" label="初始密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="显示名"><el-input v-model="form.displayName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="分析员" value="ANALYST" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.isEnabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- Reset password dialog -->
    <el-dialog v-model="resetVisible" :title="'重置密码：' + resetUsername" width="420px">
      <el-input v-model="newPassword" type="password" show-password placeholder="请输入新密码" />
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="warning" @click="onReset">重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import FilterToolbar from '@/components/common/FilterToolbar.vue'
import DataTableCard from '@/components/common/DataTableCard.vue'
import { useUsersStore } from '@/stores/users'
import type { User } from '@/api/users'

const store = useUsersStore()
const dialogVisible = ref(false)
const editing = ref<number | null>(null)
const resetVisible = ref(false)
const resetTarget = ref<number | null>(null)
const resetUsername = ref('')
const newPassword = ref('')

const form = reactive({
  username: '', password: '', displayName: '', role: 'ANALYST', isEnabled: true,
})

function openCreate() {
  editing.value = null
  Object.assign(form, { username: '', password: '', displayName: '', role: 'ANALYST', isEnabled: true })
  dialogVisible.value = true
}

function openEdit(u: User) {
  editing.value = u.id
  Object.assign(form, {
    username: u.username, password: '', displayName: u.displayName || '',
    role: u.role, isEnabled: u.isEnabled,
  })
  dialogVisible.value = true
}

async function onSave() {
  try {
    if (editing.value) {
      await store.update(editing.value, {
        displayName: form.displayName, role: form.role, isEnabled: form.isEnabled,
      })
    } else {
      await store.create({
        username: form.username, password: form.password,
        displayName: form.displayName, role: form.role, isEnabled: form.isEnabled,
      })
    }
    dialogVisible.value = false
    ElMessage.success('已保存')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.error || '保存失败')
  }
}

function openReset(u: User) {
  resetTarget.value = u.id
  resetUsername.value = u.username
  newPassword.value = ''
  resetVisible.value = true
}

async function onReset() {
  if (resetTarget.value && newPassword.value) {
    await store.resetPassword(resetTarget.value, newPassword.value)
    resetVisible.value = false
    ElMessage.success('密码已重置')
  }
}

async function onDelete(u: User) {
  await ElMessageBox.confirm(`确定删除用户"${u.username}"？`, '删除确认', { type: 'warning' })
  await store.remove(u.id)
  ElMessage.success('已删除')
}

function onRoleChange() {
  store.setRole(store.roleFilter)
}

function fmtTime(iso: string) {
  return new Date(iso).toLocaleString('zh-CN')
}

onMounted(() => store.fetchUsers())
</script>

<style scoped>
.user-manager-page { padding: 0; }
</style>
