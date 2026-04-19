<template>
  <el-tag :style="{ borderRadius: '999px' }">
    <span class="dot" :style="{ backgroundColor: color }"></span>
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { getProjectStatusInfo } from '@/utils/status'
import { ProjectStatus } from '@/types/project'

const props = defineProps<{ status: ProjectStatus }>()
const info = getProjectStatusInfo(props.status)
const label = info.label
const color = info.color
</script>

<style scoped>
.dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
}
</style>
