import axios from 'axios'

/**
 * Axios instance configured for backend API.
 * Constraint 3: credentials live ONLY in module memory (never localStorage/sessionStorage).
 * Provide credentials via setCredentials() (e.g. from a login form); clearCredentials()
 * on logout. After reload, the user must re-authenticate (no persisted Basic credentials).
 */
let authToken: string | null = null

export function setCredentials(username: string, password: string) {
  authToken = btoa(`${username}:${password}`)
  updateAuthHeader()
}

export function clearCredentials() {
  authToken = null
  updateAuthHeader()
}

function updateAuthHeader() {
  if (authToken) {
    apiClient.defaults.headers.common.Authorization = `Basic ${authToken}`
  } else {
    delete apiClient.defaults.headers.common.Authorization
  }
}

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json',
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
