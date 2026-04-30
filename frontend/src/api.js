import axios from 'axios'

export const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status
    const requestUrl = String(error.config?.url || '')
    const isAuthRequest = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/register')
    if ((status === 401 || status === 403) && !isAuthRequest) {
      localStorage.clear()
      window.location.replace('/login')
    }
    const message = error.response?.data?.message || '请求失败'
    return Promise.reject(new Error(message))
  },
)
