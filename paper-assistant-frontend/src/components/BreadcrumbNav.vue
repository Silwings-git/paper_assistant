<template>
  <nav class="breadcrumb">
    <template v-for="(item, index) in items" :key="index">
      <span v-if="index > 0" class="separator">/</span>
      <router-link v-if="item.to && index < items.length - 1" :to="item.to" class="link">
        {{ item.label }}
      </router-link>
      <span v-else class="current">{{ item.label }}</span>
    </template>
  </nav>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{ label: string; to?: string }>
}>()
</script>

<style scoped>
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: 16px;
}
.separator { color: var(--color-border); }
.link { color: var(--color-primary); cursor: pointer; }
.current { color: var(--color-text); }
</style>
