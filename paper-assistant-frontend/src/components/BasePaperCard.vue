<template>
  <el-card class="base-paper-card" shadow="hover">
    <div class="card-header">
      <h4>Base 论文</h4>
      <el-button v-if="paper" type="primary" size="small" @click="$emit('change')">更换</el-button>
    </div>

    <div v-if="paper" class="paper-content">
      <h5 class="title">{{ paper.title }}</h5>
      <p class="abstract">{{ paper.abstract }}</p>
      <div class="meta">
        <span v-if="paper.authors?.length">{{ paper.authors.slice(0, 3).join(', ') }} 等</span>
        <span>引用: {{ paper.citationCount }}</span>
      </div>
    </div>

    <div v-else class="empty-content">
      <p>还未选择 Base 论文</p>
      <el-button type="primary" size="small" @click="$emit('select')">选择 Base 论文 →</el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { PaperDTO } from '@/types/paper'

defineProps<{
  paper: PaperDTO | null
}>()

defineEmits<{
  change: []
  select: []
}>()
</script>

<style scoped>
.base-paper-card { margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.card-header h4 { margin: 0; font-size: 15px; }
.title { margin-bottom: 8px; font-size: 14px; }
.abstract { font-size: 13px; color: var(--color-text-secondary); line-height: 1.6; margin-bottom: 8px; }
.meta { font-size: 12px; color: var(--color-text-secondary); display: flex; gap: 12px; }
.empty-content { text-align: center; padding: 20px; color: var(--color-text-secondary); }
</style>
