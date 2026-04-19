<template>
  <div class="empty-state">
    <p class="title">{{ title }}</p>
    <p class="description">{{ description }}</p>
    <el-button v-if="actionText" type="primary" @click="onAction">{{ actionText }}</el-button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
  actionText?: string;
  onAction?: () => void;
}>()
</script>

<style scoped>
.empty-state {
  text-align: center;
  padding: 48px 24px;
  color: var(--color-text-secondary);
}
.title { font-size: var(--font-size-lg); color: var(--color-text); margin-bottom: 8px; }
.description { margin-bottom: 16px; }
</style>
