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
      <el-table :data="questions" stripe @row-click="selectQuestion">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="type" label="题型" width="130" />
        <el-table-column prop="content" label="题目" show-overflow-tooltip />
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.answerLocked" type="danger">答案锁定</el-tag>
            <el-tag v-else-if="row.hasAnswer" type="success">可维护</el-tag>
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
        v-if="current?.answerLocked"
        title="答案来源于模拟卷，不允许修改标准答案，只能补充解析。"
        type="warning"
        :closable="false"
      />
      <el-form label-position="top">
        <el-form-item label="标准答案">
          <el-input v-model="answerText" :disabled="current?.answerLocked" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="explanation" type="textarea" :rows="4" />
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
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useSessionStore } from '../stores'

const session = useSessionStore()
const banks = ref([])
const selectedBankId = ref(session.currentBankId)
const questions = ref([])
const page = ref(0)
const size = ref(20)
const total = ref(0)
const current = ref(null)
const dialogVisible = ref(false)
const answerText = ref('')
const explanation = ref('')
const importing = ref(false)

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
    params: { page: page.value, size: size.value },
  })).data
  questions.value = response.items
  total.value = response.total
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
  explanation.value = row.explanation || ''
  dialogVisible.value = true
}

async function saveAnswer() {
  if (current.value.answerLocked) {
    await api.put(`/admin/questions/${current.value.id}/explanation`, { explanation: explanation.value })
  } else {
    await api.put(`/admin/questions/${current.value.id}/answer`, {
      answerText: answerText.value,
      explanation: explanation.value,
    })
  }
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
</style>
