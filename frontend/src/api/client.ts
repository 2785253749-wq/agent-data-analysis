import axios from 'axios'

/**
 * Axios instance configured for backend API.
 * Vite dev server proxies /api → localhost:8080.
 * Uses HTTP Basic auth (dev credentials, same as backend application.yml).
 */
const AUTH_USER = 'admin'
const AUTH_PASSWORD = 'test123'
const authToken = btoa(`${AUTH_USER}:${AUTH_PASSWORD}`)

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json',
    Authorization: `Basic ${authToken}`,
  },
})

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('[API Error]', error.response?.status, error.message)
    return Promise.reject(error)
  },
)

/**
 * Health check — GET /api/health
 * Returns app status, version, and component health.
 */
export interface HealthResponse {
  status: string
  application: string
  version: string
  timestamp: string
  database?: { status: string; message: string }
  deepseek?: { status: string; message: string }
}

export async function getHealth(): Promise<HealthResponse> {
  const { data } = await apiClient.get<HealthResponse>('/health')
  return data
}

export default apiClient
