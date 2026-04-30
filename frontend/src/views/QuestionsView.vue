<template>
  <div class="page">
    <el-card>
      <template #header>题库浏览</template>
      <el-alert v-if="!session.currentBankId" title="请先选择题库" type="warning" :closable="false" />
      <el-form v-if="session.currentBankId" class="filters" inline @submit.prevent>
        <el-form-item label="题型">
          <el-select v-model="filters.type" clearable placeholder="全部题型" style="width: 180px" @change="search">
            <el-option v-for="type in questionTypes" :key="type.value" :label="type.label" :value="type.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="智能分类">
          <el-select v-model="filters.category" clearable placeholder="全部分类" style="width: 200px" @change="search">
            <el-option v-for="category in categoryOptions" :key="category" :label="category" :value="category" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目内容">
          <el-input v-model="filters.keyword" clearable placeholder="输入关键词" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table v-if="session.currentBankId" :data="questions" stripe>
        <el-table-column label="题型" width="120">
          <template #default="{ row }">
            {{ typeLabel(row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="knowledgeArea" label="范围" width="160" />
        <el-table-column label="题目" min-width="360">
          <template #default="{ row }">
            <div class="question-cell">
              <div>{{ row.content }}</div>
              <div v-if="row.options?.length" class="option-list">
                <div v-for="option in row.options" :key="option.optionKey">
                  {{ option.optionKey }}. {{ option.content }}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="答案" min-width="180">
          <template #default="{ row }">
            <el-tag v-if="row.answerSource === 'IMPORTED_FROM_MOCK'" class="source-tag" type="warning">
              来源：模拟卷
            </el-tag>
            <div v-if="row.hasAnswer" class="answer-text">{{ formatAnswer(row) }}</div>
            <el-tag v-else type="info">待补充</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="详解" min-width="260">
          <template #default="{ row }">
            <div class="explanation-text" v-html="formatExplanation(row)"></div>
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
import MarkdownIt from 'markdown-it'
import { api } from '../api'
import { useSessionStore } from '../stores'

const session = useSessionStore()
const questions = ref([])
const page = ref(0)
const size = ref(20)
const total = ref(0)
const filters = ref({ type: '', category: '', keyword: '' })
const questionTypes = [
  { label: '单选题', value: 'SINGLE_CHOICE' },
  { label: '多选题', value: 'MULTIPLE_CHOICE' },
  { label: '判断题', value: 'TRUE_FALSE' },
  { label: '简答题', value: 'SHORT_ANSWER' },
  { label: '实操题', value: 'PRACTICAL' },
]
const categoryOptions = [
  '区块链应用操作',
  '区块链测试',
  '区块链运维',
  '区块链应用设计',
  '综合',
]
const typeLabels = Object.fromEntries(questionTypes.map((item) => [item.value, item.label]))
const markdown = new MarkdownIt({ breaks: true, linkify: true })

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
    params: {
      page: page.value,
      size: size.value,
      type: filters.value.type || undefined,
      category: filters.value.category || undefined,
      keyword: filters.value.keyword || undefined,
    },
  })).data
  questions.value = response.items
  total.value = response.total
}

async function search() {
  page.value = 0
  await load()
}

async function resetFilters() {
  filters.value = { type: '', category: '', keyword: '' }
  await search()
}

async function changePage(currentPage) {
  page.value = currentPage - 1
  await load()
}

async function markHard(questionId) {
  await api.post(`/questions/${questionId}/hard`, { hard: true })
  ElMessage.success('已标为困难题')
}

function typeLabel(type) {
  return typeLabels[type] || type
}

function formatAnswer(row) {
  if (row.type === 'TRUE_FALSE') {
    return row.answer === 'TRUE' ? '对' : row.answer === 'FALSE' ? '错' : row.answer
  }
  return row.answer || ''
}

function formatExplanation(row) {
  return row.explanation ? markdown.render(String(row.explanation)) : '<span style="color:#909399;">暂无解析</span>'
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

.filters {
  margin-bottom: 12px;
}

.question-cell,
.answer-text {
  white-space: pre-wrap;
  line-height: 1.6;
}

.explanation-text {
  line-height: 1.6;
}

.option-list {
  margin-top: 8px;
  color: #606266;
}

.source-tag {
  margin-bottom: 6px;
}
</style>
