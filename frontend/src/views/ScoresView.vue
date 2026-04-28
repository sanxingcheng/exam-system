<template>
  <div class="page">
    <el-card>
      <template #header>模拟成绩</template>
      <el-alert v-if="!session.currentBankId" title="请先选择题库" type="warning" :closable="false" />
      <el-table v-else :data="attempts" stripe>
        <el-table-column prop="title" label="试卷" />
        <el-table-column prop="status" label="状态" width="130" />
        <el-table-column prop="score" label="分数" width="100" />
        <el-table-column prop="correctQuestions" label="答对" width="100" />
        <el-table-column prop="totalQuestions" label="总题数" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'
import { useSessionStore } from '../stores'

const session = useSessionStore()
const attempts = ref([])

onMounted(async () => {
  if (session.currentBankId) {
    attempts.value = (await api.get(`/question-banks/${session.currentBankId}/exam-attempts`)).data
  }
})
</script>
