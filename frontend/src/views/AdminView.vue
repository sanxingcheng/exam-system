<template>
  <div class="page">
    <el-card class="card">
      <template #header>管理员题库维护</template>
      <el-form inline>
        <el-form-item label="当前题库">
          <el-select v-model="selectedBankId" placeholder="请选择题库" style="width: 260px" @change="changeBank">
            <el-option v-for="bank in banks" :key="bank.id" :label="bank.name" :value="bank.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="importing" @click="importPdf">导入当前 PDF</el-button>
        </el-form-item>
      </el-form>
      <span class="tip">导入完成后再次导入会根据文件指纹跳过。</span>
    </el-card>
    <el-card>
      <el-form class="filters" inline @submit.prevent>
        <el-form-item label="题型">
          <el-select v-model="filters.type" clearable placeholder="全部题型" style="width: 180px" @change="search">
            <el-option v-for="type in questionTypes" :key="type.value" :label="type.label" :value="type.value" />
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
      <el-table :data="questions" stripe @row-click="selectQuestion">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="题型" width="120">
          <template #default="{ row }">
            {{ typeLabel(row.type) }}
          </template>
        </el-table-column>
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
        <el-table-column label="答案" min-width="220">
          <template #default="{ row }">
            <el-tag v-if="row.answerSource === 'IMPORTED_FROM_MOCK'" class="source-tag" type="warning">
              来源：模拟卷
            </el-tag>
            <div v-if="row.hasAnswer" class="answer-preview" v-html="renderMarkdown(formatAnswer(row))"></div>
            <el-tag v-else>待补答案</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        layout="prev, pager, next, total"
        :current-page="page + 1"
        :page-size="size"
        :total="total"
        @current-change="changePage"
      />
    </el-card>
    <el-dialog v-model="dialogVisible" title="维护答案" width="min(720px, 92vw)">
      <p>{{ current?.content }}</p>
      <el-alert
        v-if="current?.answerSource === 'IMPORTED_FROM_MOCK'"
        title="该答案来源于模拟卷，可根据导入错误进行修正。"
        type="warning"
        :closable="false"
      />
      <div v-if="current?.options?.length" class="dialog-options">
        <div v-for="option in current.options" :key="option.optionKey">
          {{ option.optionKey }}. {{ option.content }}
        </div>
      </div>
      <el-form label-position="top">
        <el-form-item label="标准答案">
          <el-radio-group v-if="current?.type === 'TRUE_FALSE'" v-model="answerText">
            <el-radio-button label="TRUE">对</el-radio-button>
            <el-radio-button label="FALSE">错</el-radio-button>
          </el-radio-group>
          <el-radio-group v-else-if="current?.type === 'SINGLE_CHOICE'" v-model="answerText">
            <el-radio v-for="option in current.options" :key="option.optionKey" :label="option.optionKey">
              {{ option.optionKey }}. {{ option.content }}
            </el-radio>
          </el-radio-group>
          <el-checkbox-group v-else-if="current?.type === 'MULTIPLE_CHOICE'" v-model="multipleAnswer">
            <el-checkbox v-for="option in current.options" :key="option.optionKey" :label="option.optionKey">
              {{ option.optionKey }}. {{ option.content }}
            </el-checkbox>
          </el-checkbox-group>
          <div v-else class="markdown-editor">
            <el-input v-model="answerText" type="textarea" :rows="8" placeholder="支持 Markdown 语法" />
            <div class="markdown-preview" v-html="renderMarkdown(answerText)"></div>
          </div>
        </el-form-item>
        <el-form-item label="解析">
          <div class="markdown-editor">
            <el-input v-model="explanation" type="textarea" :rows="6" placeholder="支持 Markdown 语法" />
            <div class="markdown-preview" v-html="renderMarkdown(explanation)"></div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAnswer">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownIt from 'markdown-it'
import { api } from '../api'
import { useSessionStore } from '../stores'

const session = useSessionStore()
const banks = ref([])
const selectedBankId = ref(session.currentBankId)
const questions = ref([])
const page = ref(0)
const size = ref(20)
const total = ref(0)
const filters = ref({ type: '', keyword: '' })
const current = ref(null)
const dialogVisible = ref(false)
const answerText = ref('')
const multipleAnswer = ref([])
const explanation = ref('')
const importing = ref(false)
const questionTypes = [
  { label: '单选题', value: 'SINGLE_CHOICE' },
  { label: '多选题', value: 'MULTIPLE_CHOICE' },
  { label: '判断题', value: 'TRUE_FALSE' },
  { label: '简答题', value: 'SHORT_ANSWER' },
  { label: '实操题', value: 'PRACTICAL' },
]
const markdown = new MarkdownIt({ breaks: true, linkify: true })
const typeLabels = Object.fromEntries(questionTypes.map((item) => [item.value, item.label]))

async function loadBanks() {
  banks.value = (await api.get('/question-banks')).data
  if (!selectedBankId.value && banks.value.length > 0) {
    changeBank(banks.value[0].id)
  }
}

async function load() {
  if (!selectedBankId.value) {
    questions.value = []
    total.value = 0
    return
  }
  const response = (await api.get(`/question-banks/${selectedBankId.value}/questions`, {
    params: {
      page: page.value,
      size: size.value,
      type: filters.value.type || undefined,
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
  filters.value = { type: '', keyword: '' }
  await search()
}

function changeBank(bankId) {
  selectedBankId.value = bankId
  page.value = 0
  session.setBank(bankId)
  load()
}

async function changePage(currentPage) {
  page.value = currentPage - 1
  await load()
}

async function importPdf() {
  importing.value = true
  try {
    const result = (await api.post('/admin/import/blockchain-pdf')).data
    ElMessage.success(result.report)
    if (result.detail) {
      await ElMessageBox.alert(
        [
          `导入成功：${result.importedCount} 题`,
          `单选题：${result.detail.singleChoiceCount}`,
          `多选题：${result.detail.multipleChoiceCount}`,
          `判断题：${result.detail.trueFalseCount}`,
          `实操题：${result.detail.practicalCount}`,
          `跳过（未命中题型小节）：${result.detail.skippedWithoutTypeHeading}`,
          `跳过（选择题缺少选项）：${result.detail.skippedChoiceWithoutOptions}`,
        ].join('<br/>'),
        '导入报告详情',
        { dangerouslyUseHTMLString: true, confirmButtonText: '知道了' },
      )
    }
    if (!selectedBankId.value && banks.value.length > 0) {
      changeBank(banks.value[0].id)
    }
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    importing.value = false
  }
}

function selectQuestion(row) {
  current.value = row
  answerText.value = row.answer || ''
  multipleAnswer.value = row.answer ? row.answer.split('') : []
  explanation.value = row.explanation || ''
  dialogVisible.value = true
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

function renderMarkdown(value) {
  return value ? markdown.render(String(value)) : ''
}

async function saveAnswer() {
  const normalizedAnswer = current.value.type === 'MULTIPLE_CHOICE'
    ? [...multipleAnswer.value].sort().join('')
    : answerText.value
  await api.put(`/admin/questions/${current.value.id}/answer`, {
    answerText: normalizedAnswer,
    explanation: explanation.value,
  })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

onMounted(async () => {
  await loadBanks()
  await load()
})
</script>

<style scoped>
.tip {
  margin-left: 12px;
  color: #6b7280;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.filters {
  margin-bottom: 12px;
}

.source-tag {
  margin-bottom: 6px;
}

.question-cell,
.answer-preview,
.dialog-options {
  white-space: pre-wrap;
  line-height: 1.6;
}

.option-list,
.dialog-options {
  margin-top: 8px;
  color: #606266;
}

.markdown-editor {
  display: grid;
  gap: 12px;
  width: 100%;
}

.markdown-preview {
  min-height: 80px;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fafafa;
  line-height: 1.7;
}
</style>
