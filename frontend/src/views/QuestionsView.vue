<template>
  <div class="page">
    <el-card>
      <template #header>题库浏览</template>
      <el-alert v-if="!session.currentBankId" title="请先选择题库" type="warning" :closable="false" />
      <el-table v-else :data="questions" stripe>
        <el-table-column prop="type" label="题型" width="130" />
        <el-table-column prop="knowledgeArea" label="范围" width="160" />
        <el-table-column prop="content" label="题目" min-width="320" show-overflow-tooltip />
        <el-table-column label="答案" width="110">
          <template #default="{ row }">
            <el-tag :type="row.hasAnswer ? 'success' : 'info'">
              {{ row.hasAnswer ? '已有' : '待补充' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button text @click="markHard(row.id)">标为困难</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useSessionStore } from '../stores'

const session = useSessionStore()
const questions = ref([])

async function load() {
  if (!session.currentBankId) return
  questions.value = (await api.get(`/question-banks/${session.currentBankId}/questions`)).data
}

async function markHard(questionId) {
  await api.post(`/questions/${questionId}/hard`, { hard: true })
  ElMessage.success('已标为困难题')
}

onMounted(load)
</script>
