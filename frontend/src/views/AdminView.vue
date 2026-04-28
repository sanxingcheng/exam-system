<template>
  <div class="page">
    <el-card class="card">
      <template #header>管理员题库维护</template>
      <el-button type="primary" @click="importPdf">导入当前 PDF</el-button>
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
const questions = ref([])
const current = ref(null)
const dialogVisible = ref(false)
const answerText = ref('')
const explanation = ref('')

async function load() {
  if (!session.currentBankId) return
  questions.value = (await api.get(`/question-banks/${session.currentBankId}/questions`)).data
}

async function importPdf() {
  const result = (await api.post('/admin/import/blockchain-pdf')).data
  ElMessage.success(result.report)
  await load()
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

onMounted(load)
</script>

<style scoped>
.tip {
  margin-left: 12px;
  color: #6b7280;
}
</style>
