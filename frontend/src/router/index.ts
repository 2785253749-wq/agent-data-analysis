import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/LoginView.vue'), // placeholder — will become Dashboard
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      children: [
        {
          path: '',
          redirect: '/admin/datasets',
        },
        {
          path: 'datasets',
          name: 'adminDatasets',
          component: () => import('@/views/admin/DatasetList.vue'),
        },
        {
          path: 'datasets/new',
          name: 'adminDatasetNew',
          component: () => import('@/views/admin/DatasetForm.vue'),
        },
        {
          path: 'datasets/:id',
          name: 'adminDatasetEdit',
          component: () => import('@/views/admin/DatasetForm.vue'),
          props: true,
        },
        {
          path: 'datasets/:id/fields',
          name: 'adminDatasetFields',
          component: () => import('@/views/admin/FieldManager.vue'),
          props: true,
        },
        {
          path: 'datasets/:id/metrics',
          name: 'adminDatasetMetrics',
          component: () => import('@/views/admin/MetricManager.vue'),
          props: true,
        },
      ],
    },
  ],
})

export default router
