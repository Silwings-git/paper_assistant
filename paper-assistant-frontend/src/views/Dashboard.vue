<template>
  <div class="dashboard">
    <header class="header">
      <div class="header-logo">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"/>
        </svg>
        Paper Assistant
      </div>
      <nav class="header-nav">
        <button class="btn btn-ghost btn-sm">文档</button>
        <button class="btn btn-ghost btn-sm">设置</button>
      </nav>
    </header>

    <section class="hero">
      <h1>研究项目</h1>
      <p>管理你的研究项目，追踪最新论文动态，发现创新机会。</p>
    </section>

    <section class="section">
      <div class="section-header">
        <h2 class="section-title">全部项目</h2>
        <button class="btn btn-primary" @click="createModalVisible = true">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          新建项目
        </button>
      </div>

      <!-- Loading -->
      <div v-if="store.loading" class="loading-skeleton">
        <div class="project-card" v-for="i in 3" :key="i">
          <div class="skeleton-line" :style="{ width: '60%' }"></div>
          <div class="skeleton-line" :style="{ width: '100%' }"></div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else-if="store.projects.length === 0" class="empty-state">
        <p>暂无项目，点击右上角「新建项目」开始。</p>
      </div>

      <!-- Project grid -->
      <div v-else class="project-grid">
        <div
          v-for="project in store.projects"
          :key="project.id"
          class="project-card"
          @click="goToProjectDetail(project)"
        >
          <div class="project-card-header">
            <div>
              <div class="project-name">{{ project.name }}</div>
              <div class="project-topic">{{ project.topic }}</div>
            </div>
            <span :class="['badge', statusBadgeClass(project.status)]">
              <span class="badge-dot"></span>
              {{ statusLabel(project.status) }}
            </span>
          </div>
          <div class="project-meta">
            <span class="project-meta-item">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
              {{ project.paperCount }} 篇论文
            </span>
            <span class="project-meta-item">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
              {{ formatDate(project.createTime) }}
            </span>
          </div>
          <div class="project-actions">
            <button
              class="btn btn-outline btn-sm"
              @click.stop="goToProjectDetail(project)"
            >{{ project.status === 'ANALYZED' ? '查看报告' : '查看详情' }}</button>
            <button
              class="btn btn-danger btn-sm"
              @click.stop="openDeleteModal(project)"
            >删除</button>
          </div>
        </div>
      </div>
    </section>

    <!-- Create Modal -->
    <div :class="['modal-overlay', { hidden: !createModalVisible }]">
      <div class="modal">
        <h2>新建研究项目</h2>
        <form @submit.prevent="handleCreate">
          <div class="form-group">
            <label class="form-label">项目名称 <span class="required">*</span></label>
            <input type="text" class="form-input" v-model="createForm.name" placeholder="例如：大语言模型推理优化">
          </div>
          <div class="form-group">
            <label class="form-label">研究方向 / 关键词 <span class="required">*</span></label>
            <input type="text" class="form-input" v-model="createForm.topic" placeholder="例如：speculative decoding, KV cache">
          </div>
          <div class="form-group">
            <label class="form-label">项目描述</label>
            <textarea class="form-input" v-model="createForm.description" placeholder="可选，简要描述研究目标..."></textarea>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" @click="createModalVisible = false">取消</button>
            <button type="submit" class="btn btn-primary">创建项目</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Delete Confirm -->
    <div :class="['modal-overlay', { hidden: !deleteModalVisible }]">
      <div class="modal confirm-dialog">
        <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="var(--danger)" stroke-width="1.5" stroke-linecap="round" style="margin-bottom: 14px">
          <circle cx="12" cy="12" r="10"/>
          <line x1="15" y1="9" x2="9" y2="15"/>
          <line x1="9" y1="9" x2="15" y2="15"/>
        </svg>
        <h2>确认删除</h2>
        <p>删除后，该项目的论文数据和分析报告将一并被清除，且无法恢复。</p>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="deleteModalVisible = false">取消</button>
          <button class="btn btn-primary" style="background: var(--danger);" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import type { ProjectDTO } from '@/types/project'

const router = useRouter()
const store = useProjectStore()

// Modal states
const createModalVisible = ref(false)
const deleteModalVisible = ref(false)
const deletingProject = ref<ProjectDTO | null>(null)

// Form
const createForm = ref({ name: '', topic: '', description: '' })

onMounted(() => { store.fetchList() })

async function handleCreate() {
  if (!createForm.value.name || !createForm.value.topic) return
  await store.create({ name: createForm.value.name, topic: createForm.value.topic, description: createForm.value.description })
  createModalVisible.value = false
  createForm.value = { name: '', topic: '', description: '' }
}

function openDeleteModal(project: ProjectDTO) {
  deletingProject.value = project
  deleteModalVisible.value = true
}

async function confirmDelete() {
  if (deletingProject.value) {
    await store.remove(deletingProject.value.id)
    deleteModalVisible.value = false
    deletingProject.value = null
  }
}

function goToProjectDetail(project: ProjectDTO) {
  if (project.status === 'ANALYZED') {
    router.push(`/projects/${project.id}/analysis`)
  } else {
    router.push(`/projects/${project.id}`)
  }
}

function formatDate(dateStr: string): string {
  return dateStr.slice(0, 10)
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    CREATED: '已创建',
    SEARCHING: '检索中',
    SEARCHED: '检索完成',
    ANALYZING: '分析中',
    ANALYZED: '分析完成',
  }
  return map[status] || status
}

function statusBadgeClass(status: string): string {
  const map: Record<string, string> = {
    CREATED: 'badge-created',
    SEARCHING: 'badge-searching',
    SEARCHED: 'badge-searched',
    ANALYZING: 'badge-analyzing',
    ANALYZED: 'badge-analyzed',
  }
  return map[status] || 'badge-created'
}
</script>

<style scoped>
/* ===== Page layout ===== */
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  height: 56px;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
}
.header-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}
.header-logo svg { color: var(--accent); }
.header-nav { display: flex; align-items: center; gap: 8px; }

.hero { padding: 64px 48px 40px; max-width: 1200px; margin: 0 auto; }
.hero h1 { font-size: 28px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.hero p { font-size: 15px; color: var(--text-secondary); max-width: 480px; }

.section { padding: 0 48px 64px; max-width: 1200px; margin: 0 auto; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.section-title { font-size: 16px; font-weight: 600; color: var(--text-primary); }

/* ===== Project Grid ===== */
.project-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }

.project-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 16px;
  box-shadow: var(--shadow-card);
  transition: all 0.2s ease;
}
.project-card:hover {
  box-shadow: var(--shadow-card-hover);
  border-color: #d1d5db;
  transform: translateY(-1px);
}

.project-card-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.project-name { font-size: 16px; font-weight: 600; color: var(--text-primary); }
.project-topic {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: var(--text-muted);
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}
.project-meta-item { display: flex; align-items: center; gap: 5px; }
.project-actions { display: flex; gap: 8px; margin-top: auto; }

/* ===== Skeleton / Empty ===== */
.loading-skeleton { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.skeleton-line {
  height: 14px;
  background: var(--bg-hover);
  border-radius: var(--radius-sm);
  margin-bottom: 10px;
}
.empty-state { text-align: center; padding: 48px 0; color: var(--text-muted); font-size: 14px; }

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .header { padding: 0 20px; }
  .hero { padding: 48px 20px 32px; }
  .hero h1 { font-size: 24px; }
  .section { padding: 0 20px 48px; }
  .project-grid { grid-template-columns: 1fr; }
}
</style>
