import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      children: [
        {
          path: '',
          redirect: '/home',
        },
        {
          path: 'home',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: '系统首页' },
        },
        {
          path: 'analysis',
          name: 'analysis',
          component: () => import('@/views/AnalysisView.vue'),
          meta: { title: 'AI 数据分析' },
        },
        {
          path: 'datasets',
          name: 'datasetList',
          component: () => import('@/views/admin/DatasetList.vue'),
          meta: { title: '数据集管理' },
        },
        {
          path: 'datasets/new',
          name: 'datasetNew',
          component: () => import('@/views/admin/DatasetForm.vue'),
          meta: { title: '新建数据集' },
        },
        {
          path: 'datasets/:id',
          name: 'datasetEdit',
          component: () => import('@/views/admin/DatasetForm.vue'),
          props: true,
          meta: { title: '编辑数据集' },
        },
        {
          path: 'datasets/:id/fields',
          name: 'datasetFields',
          component: () => import('@/views/admin/FieldManager.vue'),
          props: true,
          meta: { title: '字段语义管理' },
        },
        {
          path: 'datasets/:id/metrics',
          name: 'datasetMetrics',
          component: () => import('@/views/admin/MetricManager.vue'),
          props: true,
          meta: { title: '指标口径管理' },
        },
        {
          path: 'reports',
          name: 'reports',
          component: () => import('@/views/reports/AnalysisReport.vue'),
          meta: { title: '分析报告' },
        },
        {
          path: 'history',
          name: 'history',
          component: () => import('@/views/analysis/TaskListPage.vue'),
          props: { mode: 'history' },
          meta: { title: '分析历史' },
        },
        {
          path: 'trace',
          name: 'trace',
          component: () => import('@/views/analysis/TaskListPage.vue'),
          props: { mode: 'trace' },
          meta: { title: 'Agent 执行追踪' },
        },
        {
          path: 'conversations',
          name: 'conversations',
          component: () => import('@/views/analysis/ConversationView.vue'),
          meta: { title: '多轮分析会话' },
        },
        {
          path: 'models',
          name: 'aiModels',
          component: () => import('@/views/admin/AiModelPage.vue'),
          meta: { title: 'AI 模型配置' },
        },
        {
          path: 'prompts',
          name: 'promptTemplates',
          component: () => import('@/views/admin/PromptTemplatePage.vue'),
          meta: { title: 'Prompt 模板管理' },
        },
        {
          path: 'logs',
          name: 'auditLogs',
          component: () => import('@/views/admin/AuditLogPage.vue'),
          meta: { title: '操作日志' },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/admin/UserManagerPage.vue'),
          meta: { title: '用户管理' },
        },
      ],
    },
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue') },

    // ---- Legacy /admin/* URLs keep working (bookmarks, tests) ----
    { path: '/admin', redirect: '/datasets' },
    { path: '/admin/datasets', redirect: '/datasets' },
    { path: '/admin/datasets/new', redirect: '/datasets/new' },
    { path: '/admin/datasets/:id', redirect: (to) => `/datasets/${to.params.id}` },
    {
      path: '/admin/datasets/:id/fields',
      redirect: (to) => `/datasets/${to.params.id}/fields`,
    },
    {
      path: '/admin/datasets/:id/metrics',
      redirect: (to) => `/datasets/${to.params.id}/metrics`,
    },
  ],
})

export default router
