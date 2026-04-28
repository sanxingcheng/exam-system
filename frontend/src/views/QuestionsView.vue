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
      <el-pagination
        v-if="session.currentBankId"
        class="pager"
        layout="prev, pager, next, total"
        :current-page="page + 1"
        :page-size="size"
        :total="total"
        @current-change="changePage"
      />
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
const page = ref(0)
const size = ref(20)
const total = ref(0)

async function ensureBankSelected() {
  if (session.currentBankId) return
  const banks = (await api.get('/question-banks')).data
  if (banks.length > 0) {
    session.setBank(banks[0].id)
  }
}

async function load() {
  if (!session.currentBankId) return
  const response = (await api.get(`/question-banks/${session.currentBankId}/questions`, {
    params: { page: page.value, size: size.value },
  })).data
  questions.value = response.items
  total.value = response.total
}

async function changePage(currentPage) {
  page.value = currentPage - 1
  await load()
}

async function markHard(questionId) {
  await api.post(`/questions/${questionId}/hard`, { hard: true })
  ElMessage.success('已标为困难题')
}

onMounted(async () => {
  await ensureBankSelected()
  await load()
})
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
