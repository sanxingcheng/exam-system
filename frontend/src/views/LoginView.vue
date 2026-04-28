<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2>考试系统登录</h2>
      <el-form label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password />
        </el-form-item>
        <el-alert
          v-if="error"
          class="card"
          :title="error"
          type="error"
          show-icon
          :closable="false"
        />
        <el-button type="primary" :loading="loading" @click="submitLogin">登录</el-button>
        <el-button :loading="loading" @click="submitRegister">注册普通用户</el-button>
      </el-form>
      <p class="hint">内置管理员：admin / Admin@123，首次登录需修改密码。</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSessionStore } from '../stores'

const router = useRouter()
const session = useSessionStore()
const username = ref('admin')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function afterAuth() {
  router.push(session.passwordChangeRequired ? '/change-password' : '/')
}

async function run(action) {
  loading.value = true
  error.value = ''
  try {
    await action()
    await afterAuth()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function submitLogin() {
  run(() => session.login(username.value, password.value))
}

function submitRegister() {
  run(() => session.register(username.value, password.value))
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: 80vh;
  place-items: center;
}

.login-card {
  width: min(420px, 92vw);
}

.hint {
  color: #6b7280;
  font-size: 13px;
}
</style>
