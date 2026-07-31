<template>
  <div class="dataset-form">
    <h2>{{ isEdit ? '编辑数据集' : '新建数据集' }}</h2>

    <form @submit.prevent="onSubmit" class="form">
      <div class="field">
        <label>名称 <span class="required">*</span></label>
        <input v-model="form.name" type="text" required maxlength="200" placeholder="例如：销售数据" />
      </div>

      <div class="field">
        <label>描述</label>
        <textarea v-model="form.description" rows="3" placeholder="数据集说明（可选）"></textarea>
      </div>

      <div class="field">
        <label>表名 <span class="required">*</span></label>
        <input v-model="form.tableName" type="text" required maxlength="200"
               placeholder="例如：sales_data" pattern="^[a-zA-Z_][a-zA-Z0-9_]*$" />
        <span class="hint">以字母或下划线开头，只能包含字母、数字和下划线</span>
      </div>

      <div class="field">
        <label>组织ID</label>
        <input v-model.number="form.orgId" type="number" />
      </div>

      <div class="field">
        <label>
          <input v-model="form.isEnabled" type="checkbox" />
          启用
        </label>
      </div>

      <div v-if="error" class="error-msg">{{ error }}</div>

      <div class="form-actions">
        <button type="submit" class="btn btn-primary" :disabled="saving">
          {{ saving ? '保存中...' : '保存' }}
        </button>
        <button type="button" class="btn btn-cancel" @click="$router.back()">取消</button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const store = useAdminStore()

const isEdit = computed(() => !!route.params.id)
const saving = ref(false)
const error = ref('')

const form = reactive({
  name: '',
  description: '',
  tableName: '',
  orgId: 0,
  isEnabled: true,
})

onMounted(async () => {
  if (isEdit.value) {
    const id = Number(route.params.id)
    try {
      const ds = await store.fetchDataset(id)
      form.name = ds.name
      form.description = ds.description || ''
      form.tableName = ds.tableName
      form.orgId = ds.orgId
      form.isEnabled = ds.isEnabled
    } catch {
      error.value = '数据集不存在'
    }
  }
})

async function onSubmit() {
  error.value = ''
  saving.value = true
  try {
    const req = { ...form }
    if (isEdit.value) {
      await store.updateDatasetAction(Number(route.params.id), req)
    } else {
      await store.createDataset(req)
    }
    router.push('/admin/datasets')
  } catch (e: any) {
    error.value = e.response?.data?.error || '保存失败'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.dataset-form { max-width: 600px; }
.dataset-form h2 { margin-bottom: 24px; }

.form { background: #fff; padding: 24px; border-radius: 8px; box-shadow: 0 1px 6px rgba(0,0,0,0.06); }

.field { margin-bottom: 16px; }
.field label { display: block; font-size: 14px; font-weight: 600; margin-bottom: 4px; color: #333; }

.required { color: #c62828; }

.field input[type="text"],
.field input[type="number"],
.field textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  font-size: 14px;
}

.field textarea { resize: vertical; }

.hint { font-size: 12px; color: #888; margin-top: 2px; }

.error-msg { background: #fdecea; color: #c62828; padding: 10px 16px; border-radius: 6px; font-size: 14px; margin-bottom: 16px; }

.form-actions { display: flex; gap: 12px; margin-top: 24px; }

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary { background: #1a1a2e; color: #fff; }
.btn-primary:hover { background: #2a2a4e; }
.btn-cancel { background: #f0f0f0; color: #333; }
.btn-cancel:hover { background: #e0e0e0; }
</style>
