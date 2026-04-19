<template>
  <header class="app-header">
    <div class="header-content">
      <router-link to="/" class="logo">Paper Assistant</router-link>
    </div>
  </header>
</template>

<script setup lang="ts">
</script>

<style scoped>
.app-header {
  height: var(--header-height);
  background: var(--color-white);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 100%;
  display: flex;
  align-items: center;
}
.logo {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-text);
}
</style>
