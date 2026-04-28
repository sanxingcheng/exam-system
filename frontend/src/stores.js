import { defineStore } from 'pinia'
import { api } from './api'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    role: localStorage.getItem('role') || '',
    passwordChangeRequired: localStorage.getItem('passwordChangeRequired') === 'true',
    currentBankId: Number(localStorage.getItem('currentBankId')) || null,
  }),
  getters: {
    isAdmin: (state) => state.role === 'ADMIN',
    loggedIn: (state) => Boolean(state.token),
  },
  actions: {
    saveLogin(result) {
      this.token = result.token
      this.username = result.username
      this.role = result.role
      this.passwordChangeRequired = result.passwordChangeRequired
      localStorage.setItem('token', result.token)
      localStorage.setItem('username', result.username)
      localStorage.setItem('role', result.role)
      localStorage.setItem('passwordChangeRequired', String(result.passwordChangeRequired))
    },
    async login(username, password) {
      const response = await api.post('/auth/login', { username, password })
      this.saveLogin(response.data)
    },
    async register(username, password) {
      const response = await api.post('/auth/register', { username, password })
      this.saveLogin(response.data)
    },
    async changePassword(oldPassword, newPassword) {
      await api.post('/auth/change-password', { oldPassword, newPassword })
      this.passwordChangeRequired = false
      localStorage.setItem('passwordChangeRequired', 'false')
    },
    setBank(id) {
      this.currentBankId = id
      localStorage.setItem('currentBankId', String(id))
    },
    logout() {
      this.$reset()
      localStorage.clear()
    },
  },
})
