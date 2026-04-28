<template>
  <el-container class="app-shell">
    <el-header v-if="session.loggedIn" class="header">
      <div class="brand">区块链考试系统</div>
      <el-menu mode="horizontal" router class="nav">
        <el-menu-item index="/">题库</el-menu-item>
        <el-menu-item index="/questions">题目</el-menu-item>
        <el-menu-item index="/exam">模拟考试</el-menu-item>
        <el-menu-item index="/scores">成绩</el-menu-item>
        <el-menu-item v-if="session.isAdmin" index="/admin">管理</el-menu-item>
      </el-menu>
      <el-button text @click="logout">退出</el-button>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useSessionStore } from './stores'

const router = useRouter()
const session = useSessionStore()

function logout() {
  session.logout()
  router.push('/login')
}
</script>
