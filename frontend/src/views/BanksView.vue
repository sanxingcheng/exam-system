<template>
  <div class="page">
    <el-card>
      <template #header>选择题库</template>
      <el-row :gutter="16">
        <el-col v-for="bank in banks" :key="bank.id" :xs="24" :sm="12" :md="8">
          <el-card class="card" shadow="hover">
            <h3>{{ bank.name }}</h3>
            <p>{{ bank.description }}</p>
            <el-tag>{{ bank.bankType }}</el-tag>
            <div class="actions">
              <el-button type="primary" @click="selectBank(bank.id)">使用该题库</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { useSessionStore } from '../stores'

const router = useRouter()
const session = useSessionStore()
const banks = ref([])

onMounted(async () => {
  banks.value = (await api.get('/question-banks')).data
})

function selectBank(id) {
  session.setBank(id)
  router.push('/questions')
}
</script>

<style scoped>
.actions {
  margin-top: 16px;
}
</style>
