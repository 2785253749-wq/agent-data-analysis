import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { setCredentials } from '@/api/client'
import './styles/main.css'

// Dev default credentials (in-memory only). Replace with a login form in production.
setCredentials('admin', 'test123')

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
