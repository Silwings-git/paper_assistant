<template>
  <el-drawer :model-value="!!paper" @update:model-value="handleClose" title="论文详情" direction="rtl" size="540px">
    <div v-if="paper" class="paper-detail">
      <h3 class="title">{{ paper.title }}</h3>
      <div class="meta">
        <span v-if="paper.authors?.length">{{ paper.authors.join(', ') }}</span>
        <span v-if="paper.publishDate">{{ paper.publishDate }}</span>
        <span>引用: {{ paper.citationCount }}</span>
      </div>
      <div class="abstract">
        <h4>摘要</h4>
        <p>{{ paper.abstract || '暂无摘要' }}</p>
      </div>
      <div class="links">
        <el-button v-if="paper.pdfUrl" link type="primary" :href="paper.pdfUrl" target="_blank">PDF</el-button>
        <el-button v-if="paper.codeUrl" link type="primary" :href="paper.codeUrl" target="_blank">代码</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import type { PaperDTO } from '@/types/paper'

defineProps<{
  paper: PaperDTO | null
}>()

const emit = defineEmits<{
  close: []
}>()

function handleClose() {
  emit('close')
}
</script>

<style scoped>
.paper-detail { padding: 0 8px; }
.title { margin-bottom: 12px; line-height: 1.4; }
.meta { display: flex; flex-wrap: wrap; gap: 12px; font-size: 13px; color: var(--color-text-secondary); margin-bottom: 16px; }
.abstract h4 { margin-bottom: 8px; }
.abstract p { line-height: 1.8; color: var(--color-text-secondary); }
.links { margin-top: 16px; }
</style>
