<template>
  <div class="page">
    <el-card>
      <h2>首次登录修改密码</h2>
      <el-form label-position="top">
        <el-form-item label="原密码">
          <el-input v-model="oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password />
        </el-form-item>
        <el-alert v-if="error" class="card" :title="error" type="error" :closable="false" />
        <el-button type="primary" @click="submit">保存并进入系统</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSessionStore } from '../stores'

const router = useRouter()
const session = useSessionStore()
const oldPassword = ref('')
const newPassword = ref('')
const error = ref('')

async function submit() {
  error.value = ''
  try {
    await session.changePassword(oldPassword.value, newPassword.value)
    router.push('/')
  } catch (err) {
    error.value = err.message
  }
}
</script>
