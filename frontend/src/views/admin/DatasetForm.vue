<template>
  <div class="dataset-form">
    <DataTableCard :title="isEdit ? '编辑数据集' : '新建数据集'">
      <el-form
        :model="form"
        :rules="rules"
        label-width="100px"
        class="dataset-form-body"
        @submit.prevent="onSubmit"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="200" placeholder="例如：销售数据" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="数据集说明（可选）"
          />
        </el-form-item>

        <el-form-item label="表名" prop="tableName">
          <el-input v-model="form.tableName" maxlength="200" placeholder="例如：sales_data" />
          <div class="hint">以字母或下划线开头，只能包含字母、数字和下划线</div>
        </el-form-item>

        <el-form-item label="组织ID" prop="orgId">
          <el-input-number v-model="form.orgId" :min="0" />
        </el-form-item>

        <el-form-item label="启用" prop="isEnabled">
          <el-switch v-model="form.isEnabled" />
        </el-form-item>

        <el-alert v-if="error" type="error" :title="error" :closable="false" show-icon class="form-error" />

        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="saving">
            {{ saving ? '保存中...' : '保存' }}
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </DataTableCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormRules } from 'element-plus'
import DataTableCard from '@/components/common/DataTableCard.vue'
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

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  tableName: [
    { required: true, message: '请输入表名', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/,
      message: '以字母或下划线开头，只能包含字母、数字和下划线',
      trigger: 'blur',
    },
  ],
}

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
    router.push('/datasets')
  } catch (e: any) {
    error.value = e.response?.data?.error || '保存失败'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.dataset-form { max-width: 640px; }

.dataset-form-body {
  padding: 24px 24px 8px;
}

.hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  margin-top: 2px;
}

.form-error {
  margin-bottom: 16px;
}
</style>
