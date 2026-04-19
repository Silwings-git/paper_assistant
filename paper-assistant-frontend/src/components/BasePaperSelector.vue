<template>
  <el-dialog v-model="visible" title="选择 Base 论文" width="620px">
    <div class="paper-list">
      <div v-for="paper in papers" :key="paper.id"
           class="paper-item"
           :class="{ selected: selectedId === paper.id }"
           @click="selectedId = paper.id">
        <div class="radio" :class="{ checked: selectedId === paper.id }"></div>
        <div class="info">
          <h5 class="title">{{ paper.title }}</h5>
          <div class="meta">
            <span v-if="paper.authors?.length">{{ paper.authors.slice(0, 3).join(', ') }}</span>
            <span v-if="paper.publishDate">{{ paper.publishDate }}</span>
            <span>引用: {{ paper.citationCount }}</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!selectedId" @click="confirm">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { PaperDTO } from '@/types/paper'

const props = defineProps<{
  papers: PaperDTO[]
  currentBasePaperId: string | null
}>()

const emit = defineEmits<{
  confirm: [paperId: string]
}>()

const visible = ref(false)
const selectedId = ref<string | null>(props.currentBasePaperId)

watch(() => props.currentBasePaperId, (val) => { selectedId.value = val })

function open() { visible.value = true }
function confirm() {
  if (selectedId.value) {
    emit('confirm', selectedId.value)
    visible.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.paper-list { max-height: 400px; overflow-y: auto; }
.paper-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.paper-item:hover { background: var(--color-bg); }
.paper-item.selected { background: #ecf5ff; }
.radio {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid var(--color-border);
  flex-shrink: 0;
  margin-top: 2px;
}
.radio.checked {
  border-color: var(--color-primary);
  background: var(--color-primary);
  box-shadow: inset 0 0 0 3px white;
}
.info { flex: 1; min-width: 0; }
.title { margin-bottom: 4px; font-size: 14px; }
.meta { font-size: 12px; color: var(--color-text-secondary); display: flex; gap: 12px; flex-wrap: wrap; }
</style>
