<template>
  <div class="task-progress-panel">
    <div v-if="taskStatus" class="progress-header">
      <el-tag :type="tagType" size="small" effect="plain">
        {{ stageLabel }}
      </el-tag>
      <span class="progress-text">{{ taskStatus.message }}</span>
    </div>
    <el-progress
      v-if="taskStatus"
      :percentage="taskStatus.progress"
      :status="statusType"
      :stroke-width="8"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TaskStatusDTO } from '@/types/analysis'

const props = defineProps<{
  taskStatus: TaskStatusDTO | null
}>()

const stageLabel = computed(() => {
  if (!props.taskStatus) return ''
  const msg = props.taskStatus.message || ''
  if (msg.includes('阶段 1')) return '逐篇分析'
  if (msg.includes('阶段 2')) return '交叉分析'
  if (msg.includes('阶段 3')) return '创新生成'
  if (msg.includes('检索')) return '论文检索'
  return props.taskStatus.stage || ''
})

const statusType = computed(() => {
  if (!props.taskStatus) return ''
  const s = props.taskStatus.status
  if (s === 'FAILED') return 'exception' as const
  if (s === 'ANALYZED' || s === 'SEARCHED') return 'success' as const
  if (s === 'SEARCHED_WITH_ERRORS') return 'warning' as const
  return '' as const
})

const tagType = computed(() => {
  if (!props.taskStatus) return 'info' as const
  const s = props.taskStatus.status
  if (s === 'FAILED') return 'danger' as const
  if (s === 'ANALYZED' || s === 'SEARCHED') return 'success' as const
  if (s === 'SEARCHED_WITH_ERRORS') return 'warning' as const
  return 'primary' as const
})
</script>

<style scoped>
.task-progress-panel {
  margin-bottom: 16px;
}
.progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.progress-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}
</style>
