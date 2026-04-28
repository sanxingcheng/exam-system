<template>
  <div class="page">
    <el-card v-if="!attempt">
      <template #header>生成模拟试卷</template>
      <el-alert v-if="!session.currentBankId" title="请先选择题库" type="warning" :closable="false" />
      <el-form v-else inline>
        <el-form-item label="题目数量">
          <el-input-number v-model="questionCount" :min="1" :max="100" />
        </el-form-item>
        <el-button type="primary" @click="createAttempt">开始考试</el-button>
      </el-form>
    </el-card>
    <el-card v-else>
      <template #header>
        <div class="exam-header">
          <span>{{ attempt.title }}</span>
          <el-tag type="danger">倒计时 {{ remainingText }}</el-tag>
        </div>
      </template>
      <div v-for="question in attempt.questions" :key="question.id" class="question">
        <h3>{{ question.order }}. {{ question.content }}</h3>
        <pre v-if="question.optionsSnapshot">{{ question.optionsSnapshot }}</pre>
        <el-input v-model="answers[question.id]" placeholder="请输入答案，如 A 或 ABC" />
      </div>
      <el-button type="primary" @click="submit">提交试卷</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useSessionStore } from '../stores'

const session = useSessionStore()
const questionCount = ref(20)
const attempt = ref(null)
const answers = ref({})
const remainingSeconds = ref(0)
let timer = null

const remainingText = computed(() => {
  const minutes = Math.floor(remainingSeconds.value / 60)
  const seconds = remainingSeconds.value % 60
  return `${minutes}:${String(seconds).padStart(2, '0')}`
})

async function createAttempt() {
  const response = await api.post(`/question-banks/${session.currentBankId}/exam-attempts`, {
    questionCount: questionCount.value,
  })
  attempt.value = response.data
  remainingSeconds.value = attempt.value.durationMinutes * 60
  timer = setInterval(() => {
    remainingSeconds.value -= 1
    if (remainingSeconds.value <= 0) {
      submit()
    }
  }, 1000)
}

async function submit() {
  if (!attempt.value) return
  clearInterval(timer)
  const response = await api.post(`/exam-attempts/${attempt.value.id}/submit`, { answers: answers.value })
  attempt.value = null
  answers.value = {}
  ElMessage.success(`提交成功，得分 ${response.data.score}`)
}

onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.exam-header {
  display: flex;
  justify-content: space-between;
}

.question {
  padding: 16px 0;
  border-bottom: 1px solid #e5e7eb;
}

pre {
  white-space: pre-wrap;
}
</style>
