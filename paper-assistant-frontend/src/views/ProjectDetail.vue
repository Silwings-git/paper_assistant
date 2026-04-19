<template>
  <div class="project-detail">
    <header class="header">
      <div class="header-logo" @click="router.push('/')">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"/>
        </svg>
        Paper Assistant
      </div>
    </header>

    <nav class="breadcrumb">
      <router-link to="/">项目列表</router-link>
      <span class="sep">/</span>
      <span class="current">{{ project?.name || '项目详情' }}</span>
    </nav>

    <div v-if="project" class="page">
      <!-- Project Info -->
      <div class="project-info">
        <h1>{{ project.name }}</h1>
        <span :class="['badge', statusBadgeClass(project.status)]">
          <span class="badge-dot"></span>
          {{ statusLabel(project.status) }}
        </span>
        <div class="info-meta">
          <span class="info-meta-item">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
            </svg>
            {{ project.paperCount }} 篇论文
          </span>
          <span class="info-meta-item">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
            创建于 {{ formatDate(project.createTime) }}
          </span>
        </div>
      </div>

      <!-- Base Paper -->
      <div v-if="basePaper" class="base-paper-card">
        <div>
          <div class="base-paper-label">Base 论文</div>
          <div class="base-paper-title">{{ basePaper.title }}</div>
          <div class="base-paper-authors">{{ formatAuthors(basePaper.authors) }}</div>
        </div>
        <div class="base-paper-actions">
          <button class="btn btn-outline btn-sm" @click="openBasePaperSelector">更换</button>
        </div>
      </div>
      <div v-else class="base-paper-card">
        <div class="base-paper-empty-text">尚未选择 Base 论文，请选择。</div>
        <button class="btn btn-outline btn-sm" @click="openBasePaperSelector">选择论文</button>
      </div>

      <!-- Search -->
      <div class="search-area">
        <input type="text" class="search-input" v-model="keyword" placeholder="输入关键词检索论文..."
               @keyup.enter="handleSearch"
               :disabled="isSearching || paperStore.searchLoading">
        <button class="btn btn-primary" @click="handleSearch"
                :disabled="disabledActions.includes('search') || !keyword.trim()">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          搜索
        </button>
      </div>

      <!-- Inline Search Progress -->
      <div v-if="paperStore.searchLoading || paperStore.currentTaskStatus?.status === 'SEARCHING'" class="inline-search-progress">
        <div class="inline-progress-content">
          <svg class="inline-spinner" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
          </svg>
          <span class="inline-progress-text">{{ paperStore.currentTaskStatus?.message || '正在检索论文...' }}</span>
          <span class="inline-progress-percent">{{ paperStore.currentTaskStatus?.progress ?? 0 }}%</span>
        </div>
        <div class="inline-progress-with-btn">
          <div class="inline-progress-bar">
            <div class="inline-progress-fill" :style="{ width: (paperStore.currentTaskStatus?.progress ?? 0) + '%' }"></div>
          </div>
          <button class="inline-cancel-btn" @click="handleCancelSearch">取消</button>
        </div>
      </div>

      <!-- Warning Banner -->
      <div v-if="partialFailure" class="warning-banner">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
          <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
        </svg>
        {{ warningMessage }}
      </div>

      <!-- Paper Table -->
      <div class="table-container" v-if="paperStore.papers.length > 0">
        <div class="table-toolbar">
          <div class="table-toolbar-left">
            <span class="selection-count">已选: {{ selectedIds.length }}/{{ paperStore.total }} 篇</span>
          </div>
          <div class="sort-tabs">
            <button :class="['sort-tab', { active: paperStore.sortOrder === 'relevance' }]"
                    @click="changeSort('relevance')">相关度</button>
            <button :class="['sort-tab', { active: paperStore.sortOrder === 'citation' }]"
                    @click="changeSort('citation')">引用数</button>
            <button :class="['sort-tab', { active: paperStore.sortOrder === 'date' }]"
                    @click="changeSort('date')">日期</button>
          </div>
        </div>

        <table>
          <thead>
            <tr>
              <th class="th-checkbox" @click="toggleSelectAll">
                <span class="custom-checkbox" :class="{ checked: isAllSelected }">
                  <svg v-if="isAllSelected" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                </span>
              </th>
              <th>论文标题</th>
              <th>作者</th>
              <th>日期</th>
              <th>引用</th>
              <th>影响力</th>
              <th>代码</th>
              <th>链接</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="paper in paperStore.papers" :key="paper.id"
                :class="{ selected: selectedIds.includes(paper.id) }">
              <td class="td-checkbox" @click.stop="togglePaper(paper.id)">
                <span class="custom-checkbox" :class="{ checked: selectedIds.includes(paper.id) }">
                  <svg v-if="selectedIds.includes(paper.id)" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                </span>
              </td>
              <td @click="openDrawer(paper)">
                <div class="paper-title">{{ paper.title }}</div>
                <div class="paper-abstract">{{ paper.abstract || '' }}</div>
              </td>
              <td @click="openDrawer(paper)"><div class="paper-authors">{{ formatAuthorsShort(paper.authors) }}</div></td>
              <td @click="openDrawer(paper)"><span class="paper-date">{{ paper.publishDate || '-' }}</span></td>
              <td @click="openDrawer(paper)"><span class="paper-cite">{{ formatNumber(paper.citationCount) }}</span></td>
              <td @click="openDrawer(paper)"><span class="paper-score">{{ Math.round(paper.influenceScore) }}</span></td>
              <td @click="openDrawer(paper)">
                <span v-if="paper.hasCode" class="tag tag-code">
                  <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/>
                  </svg>
                  有代码
                </span>
                <span v-else class="tag tag-no-code">无代码</span>
              </td>
              <td><a v-if="paper.pdfUrl" class="link-btn" :href="paper.pdfUrl" target="_blank" @click.stop>PDF</a></td>
            </tr>
          </tbody>
        </table>

        <!-- Pagination -->
        <div class="pagination" v-if="totalPages > 1">
          <span>共 {{ paperStore.total }} 篇论文，第 {{ paperStore.currentPage }} / {{ totalPages }} 页</span>
          <div class="pagination-pages">
            <button class="page-btn" :disabled="paperStore.currentPage <= 1"
                    @click="handlePageChange(paperStore.currentPage - 1)">&laquo;</button>
            <button v-for="page in displayPages" :key="page"
                    :class="['page-btn', { active: page === paperStore.currentPage }]"
                    @click="handlePageChange(page)">{{ page }}</button>
            <button class="page-btn" :disabled="paperStore.currentPage >= totalPages"
                    @click="handlePageChange(paperStore.currentPage + 1)">&raquo;</button>
          </div>
        </div>
      </div>

      <!-- Empty papers -->
      <div v-else-if="!paperStore.loading" class="empty-state">
        <p>暂无论文数据，请搜索或检索。</p>
      </div>

      <!-- Loading -->
      <div v-else class="loading-state">
        <div class="skeleton-table">
          <div class="skeleton-row" v-for="i in 5" :key="i">
            <div class="skeleton-cell" :style="{ width: '40px' }"></div>
            <div class="skeleton-cell" :style="{ width: '40%' }"></div>
            <div class="skeleton-cell" :style="{ width: '15%' }"></div>
            <div class="skeleton-cell" :style="{ width: '10%' }"></div>
            <div class="skeleton-cell" :style="{ width: '8%' }"></div>
          </div>
        </div>
      </div>

      <!-- Action Area -->
      <div class="action-area">
        <button class="btn btn-purple" @click="handleAnalyze"
                :disabled="analyzing || disabledActions.includes('triggerAnalysis') || selectedIds.length === 0"
                :title="selectedIds.length === 0 ? '请先选择论文' : ''">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M12 2a10 10 0 1 0 10 10H12V2z"/>
            <path d="M21.12 11.07A10 10 0 0 0 12 2v9.07L21.12 11.07z" opacity="0.5"/>
          </svg>
          触发分析 ({{ selectedIds.length }} 篇)
        </button>
        <button class="btn btn-primary" @click="router.push(`/projects/${projectId}/analysis`)"
                :disabled="project?.status !== 'ANALYZED'"
                :title="project?.status === 'ANALYZED' ? '' : '分析完成后可查看报告'">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
          </svg>
          查看报告
        </button>
        <button class="btn btn-outline" @click="handleSearch"
                :disabled="isSearching || paperStore.searchLoading || !keyword.trim()"
                :title="keyword.trim() ? '' : '请输入搜索词'">重新检索</button>
      </div>

      <!-- Task Progress: Search -->
      <div v-if="paperStore.currentTaskId && paperStore.currentTaskStatus && paperStore.currentTaskStatus.status !== 'CANCELLED'" class="task-progress" style="margin-top: 32px;">
        <div class="task-progress-header">
          <div class="task-progress-title">检索任务进度</div>
          <div class="task-progress-id">{{ paperStore.currentTaskId }}</div>
        </div>
        <div class="progress-bar-track">
          <div class="progress-bar-fill accent" :style="{ width: paperStore.currentTaskStatus.progress + '%' }"></div>
        </div>
        <div class="progress-info">
          <span class="progress-stage">{{ paperStore.currentTaskStatus.message || paperStore.currentTaskStatus.stage }}</span>
          <span class="progress-percent">{{ paperStore.currentTaskStatus.progress }}%</span>
          <button v-if="paperStore.currentTaskStatus.status === 'SEARCHING'" class="task-cancel-inline-btn" @click="handleCancelSearch">取消</button>
        </div>
      </div>

      <!-- Task Progress: Analysis -->
      <div v-if="analysisStore.currentTaskId && analysisStore.currentTaskStatus && analysisStore.currentTaskStatus.status !== 'CANCELLED'" class="task-progress" style="margin-top: 16px;">
        <div class="task-progress-header">
          <div class="task-progress-title">分析任务进度</div>
          <div class="task-progress-id">{{ analysisStore.currentTaskId }}</div>
        </div>
        <div class="progress-bar-track">
          <div class="progress-bar-fill purple" :style="{ width: analysisStore.currentTaskStatus.progress + '%' }"></div>
        </div>
        <div class="progress-info">
          <span class="progress-stage">{{ analysisStore.currentTaskStatus.message || analysisStore.currentTaskStatus.stage }}</span>
          <span class="progress-percent">{{ analysisStore.currentTaskStatus.progress }}%</span>
          <button v-if="analysisStore.currentTaskStatus.status === 'ANALYZING'" class="task-cancel-inline-btn" @click="handleCancelAnalysis">取消</button>
        </div>
      </div>
    </div>

    <!-- Paper Detail Drawer -->
    <div :class="['drawer-overlay', { hidden: !drawerVisible }]" @click.self="drawerVisible = false">
      <div class="drawer" v-if="selectedPaper">
        <div class="drawer-header">
          <h3>论文详情</h3>
          <button class="drawer-close" @click="drawerVisible = false">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="drawer-body">
          <div class="drawer-title">{{ selectedPaper.title }}</div>
          <div class="drawer-meta">
            <div class="drawer-meta-item"><span class="drawer-meta-label">作者</span>{{ formatAuthors(selectedPaper.authors) }}</div>
            <div class="drawer-meta-item" v-if="selectedPaper.publishDate"><span class="drawer-meta-label">日期</span>{{ selectedPaper.publishDate }}</div>
            <div class="drawer-meta-item" v-if="selectedPaper.category"><span class="drawer-meta-label">分类</span>{{ selectedPaper.category }}</div>
            <div class="drawer-meta-item"><span class="drawer-meta-label">引用数</span>{{ formatNumber(selectedPaper.citationCount) }}</div>
            <div class="drawer-meta-item" v-if="selectedPaper.arxivId"><span class="drawer-meta-label">arXiv ID</span>{{ selectedPaper.arxivId }}</div>
          </div>
          <div class="drawer-abstract">{{ selectedPaper.abstract || '暂无摘要' }}</div>
          <div class="drawer-links">
            <a v-if="selectedPaper.pdfUrl" class="btn btn-outline btn-sm" :href="selectedPaper.pdfUrl" target="_blank">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
                <polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/>
              </svg>
              PDF
            </a>
            <a v-if="selectedPaper.codeUrl" class="btn btn-outline btn-sm" :href="selectedPaper.codeUrl" target="_blank">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/>
              </svg>
              代码
            </a>
          </div>
        </div>
      </div>
    </div>

    <!-- Base Paper Selector -->
    <div :class="['modal-overlay', { hidden: !selectorVisible }]" @click.self="selectorVisible = false">
      <div class="selector-panel">
        <div class="selector-header">
          <h3>选择 Base 论文</h3>
          <button class="drawer-close" @click="selectorVisible = false">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="selector-body">
          <div v-if="selectorLoading" class="selector-loading">
            <div class="selector-spinner"></div>
            <span>加载中...</span>
          </div>
          <div v-else-if="selectorPapers.length === 0" class="selector-empty">
            <span>暂无论文，请先搜索。</span>
          </div>
          <template v-else>
            <input v-model="selectorSearchKeyword" class="selector-search-input" placeholder="按标题搜索论文..." />
            <div v-if="filteredSelectorPapers.length === 0" class="selector-empty">
              <span>未找到匹配的论文</span>
            </div>
            <div v-else v-for="paper in filteredSelectorPapers" :key="paper.id"
                 class="selector-item"
                 :class="{ selected: selectorSelectedId === paper.id }"
                 @click="selectorSelectedId = paper.id">
              <div :class="['selector-radio', { selected: selectorSelectedId === paper.id }]"></div>
              <div>
                <div class="selector-item-title">{{ paper.title }}</div>
                <div class="selector-item-authors">{{ formatAuthorsShort(paper.authors) }} &middot; {{ paper.publishDate || '-' }} &middot; 引用 {{ formatNumber(paper.citationCount) }}</div>
              </div>
            </div>
          </template>
        </div>
        <div class="selector-footer">
          <button class="btn btn-ghost" @click="selectorVisible = false">取消</button>
          <button class="btn btn-primary" :disabled="!selectorSelectedId" @click="confirmBasePaper">确认选择</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { usePaperStore } from '@/stores/paper'
import { useAnalysisStore } from '@/stores/analysis'
import { useTaskManagerStore } from '@/stores/taskManager'
import { getProjectStatusInfo } from '@/utils/status'
import type { PaperDTO } from '@/types/paper'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const paperStore = usePaperStore()
const analysisStore = useAnalysisStore()
const taskManager = useTaskManagerStore()

const keyword = ref('')
const analyzing = ref(false)
const selectedPaper = ref<PaperDTO | null>(null)
const drawerVisible = ref(false)
const selectorVisible = ref(false)
const selectorSelectedId = ref<string | null>(null)
const selectorPapers = ref<PaperDTO[]>([])
const selectorLoading = ref(false)
const selectorSearchKeyword = ref('')

// Selection
const selectedIds = ref<string[]>([])

// Warning/error state
const partialFailure = ref(false)
const warningMessage = ref('')

// Computed
const projectId = computed(() => String(route.params.id))
const project = computed(() => projectStore.projects.find(p => p.id === projectId.value) || null)
const basePaper = computed(() => {
  if (!project.value?.basePaperId) return null
  return paperStore.papers.find(p => p.id === project.value?.basePaperId) || null
})
const filteredSelectorPapers = computed(() => {
  const kw = selectorSearchKeyword.value.trim().toLowerCase()
  if (!kw) return selectorPapers.value
  return selectorPapers.value.filter(p => p.title.toLowerCase().includes(kw))
})
const disabledActions = computed(() => {
  if (!project.value) return []
  const base = getProjectStatusInfo(project.value.status).disabledActions
  // 根据全局任务状态动态禁用
  const tasks = Array.from(taskManager.activeTasks.values())
  const hasSearchTask = tasks.some(t => t.taskType === 'SEARCH' && !['SEARCHED', 'ANALYZED', 'FAILED', 'CANCELLED'].includes(t.status.status))
  const hasAnalysisTask = tasks.some(t => t.taskType === 'ANALYSIS' && !['SEARCHED', 'ANALYZED', 'FAILED', 'CANCELLED'].includes(t.status.status))
  if (hasSearchTask && !base.includes('search')) {
    base.push('search')
  }
  if (hasAnalysisTask && !base.includes('triggerAnalysis')) {
    base.push('triggerAnalysis')
  }
  return base
})
const isSearching = computed(() => project.value?.status === 'SEARCHING')
const totalPages = computed(() => Math.ceil(paperStore.total / paperStore.pageSize) || 1)
const isAllSelected = computed(() =>
  paperStore.papers.length > 0 && paperStore.papers.every(p => selectedIds.value.includes(p.id))
)

// Display pages for pagination
const displayPages = computed(() => {
  const total = totalPages.value
  const current = paperStore.currentPage
  const pages: number[] = []
  const start = Math.max(1, current - 2)
  const end = Math.min(total, current + 2)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

// Watch task status
watch(() => paperStore.currentTaskStatus, (status) => {
  if (!status) return
  if (status.status === 'FAILED') {
    partialFailure.value = true
    warningMessage.value = '检索失败: ' + (status.message || '未知错误')
  }
})

watch(() => analysisStore.currentTaskStatus, async (status) => {
  if (!status) return
  if (status.status === 'ANALYZING' || status.status === 'PENDING') {
    analyzing.value = true
  } else if (status.status === 'ANALYZED' || status.status === 'FAILED') {
    analyzing.value = false
    await projectStore.fetchList()
  }
})

// Lifecycle
onMounted(async () => {
  // 重置上一次的任务状态，避免导航时残留导致错误显示 loading
  paperStore.currentTaskId = null
  paperStore.currentTaskStatus = null
  paperStore.searchLoading = false

  if (projectStore.projects.length === 0) await projectStore.fetchList()
  await paperStore.fetchList(projectId.value)

  // 注册到全局任务管理
  await taskManager.register(projectId.value)

  // 根据全局任务状态同步 analysisStore/paperStore
  // 修复：即使项目状态是 ANALYZED/SEARCHED，也可能存在刚触发的活跃任务
  const analysisTask = Array.from(taskManager.activeTasks.values()).find(t => t.taskType === 'ANALYSIS')
  if (analysisTask) {
    analysisStore.currentTaskId = analysisTask.taskId
    analysisStore.currentTaskStatus = analysisTask.status
    if (analysisTask.status.status === 'ANALYZING' || analysisTask.status.status === 'PENDING') {
      analyzing.value = true
    }
  }

  const searchTask = Array.from(taskManager.activeTasks.values()).find(t => t.taskType === 'SEARCH')
  if (searchTask) {
    paperStore.currentTaskId = searchTask.taskId
    paperStore.currentTaskStatus = searchTask.status
    paperStore.searchLoading = true
  }

  // 根据项目状态恢复进行中的任务（兼容旧逻辑）
  const status = project.value?.status
  if (status === 'SEARCHING' && !paperStore.currentTaskId) {
    paperStore.restoreTask(projectId.value, () => {
      paperStore.fetchList(projectId.value)
      projectStore.fetchList()
    }, (message) => {
      partialFailure.value = true
      warningMessage.value = '检索失败: ' + message
    })
  }
  if (status === 'ANALYZING' && !analysisStore.currentTaskId) {
    analysisStore.restoreTask(projectId.value, () => {
      projectStore.fetchList()
    })
  }

  // Init selector selected id
  if (project.value?.basePaperId) {
    selectorSelectedId.value = project.value.basePaperId
  }
})

onUnmounted(() => {
  paperStore.stopSearchPolling()
  analysisStore.stopAnalyzePolling()
  taskManager.unregister(projectId.value)
})

// 取消任务
async function handleCancelSearch() {
  if (paperStore.currentTaskId) {
    await taskManager.cancelTask(paperStore.currentTaskId, 'SEARCH')
    paperStore.currentTaskId = null
    paperStore.currentTaskStatus = null
    paperStore.searchLoading = false
  }
}

async function handleCancelAnalysis() {
  if (analysisStore.currentTaskId) {
    await taskManager.cancelTask(analysisStore.currentTaskId, 'ANALYSIS')
    analysisStore.currentTaskId = null
    analysisStore.currentTaskStatus = null
    analyzing.value = false
  }
}

// Handlers
async function handleSearch() {
  partialFailure.value = false
  warningMessage.value = ''
  paperStore.currentPage = 1
  selectedIds.value = []
  await paperStore.search(projectId.value, keyword.value, () => {
    paperStore.fetchList(projectId.value)
    projectStore.fetchList() // 刷新项目状态
  }, (message) => {
    partialFailure.value = true
    warningMessage.value = '检索失败: ' + message
  })
}

async function handleAnalyze() {
  if (selectedIds.value.length === 0) return
  analyzing.value = true
  try {
    await analysisStore.trigger(projectId.value, selectedIds.value)
  } catch {
    analyzing.value = false
  }
}

async function handlePageChange(page: number) {
  if (page < 1 || page > totalPages.value) return
  paperStore.currentPage = page
  await paperStore.fetchList(projectId.value)
}

async function changeSort(order: 'relevance' | 'citation' | 'date') {
  paperStore.sortOrder = order
  paperStore.currentPage = 1
  await paperStore.fetchList(projectId.value)
}

function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = paperStore.papers.map(p => p.id)
  }
}

function togglePaper(id: string) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) {
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  } else {
    selectedIds.value = [...selectedIds.value, id]
  }
}

function openDrawer(paper: PaperDTO) {
  selectedPaper.value = paper
  drawerVisible.value = true
}

async function confirmBasePaper() {
  if (selectorSelectedId.value) {
    await projectStore.setBase(projectId.value, selectorSelectedId.value)
    selectorVisible.value = false
  }
}

async function openBasePaperSelector() {
  selectorVisible.value = true
  selectorSearchKeyword.value = ''
  selectorLoading.value = true
  selectorPapers.value = await paperStore.fetchAllPapers(projectId.value)
  selectorLoading.value = false
}

// Formatting helpers
function formatDate(dateStr: string): string { return dateStr.slice(0, 10) }
function formatAuthors(authors: string[]): string { return authors.join(', ') }
function formatAuthorsShort(authors: string[]): string {
  if (authors.length === 0) return '-'
  return authors.slice(0, 3).map(a => {
    const parts = a.split(' ')
    return parts.length > 1 ? parts[0][0] + '. ' + parts[parts.length - 1] : a
  }).join(', ')
}
function formatNumber(n: number): string { return n.toLocaleString() }
function statusLabel(status: string): string {
  const map: Record<string, string> = {
    CREATED: '已创建', SEARCHING: '检索中', SEARCHED: '检索完成',
    ANALYZING: '分析中', ANALYZED: '分析完成',
  }
  return map[status] || status
}
function statusBadgeClass(status: string): string {
  const map: Record<string, string> = {
    CREATED: 'badge-created', SEARCHING: 'badge-searching', SEARCHED: 'badge-searched',
    ANALYZING: 'badge-analyzing', ANALYZED: 'badge-analyzed',
  }
  return map[status] || 'badge-created'
}
</script>

<style scoped>
/* ===== Header ===== */
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
  cursor: pointer;
  color: var(--text-primary);
}
.header-logo svg { color: var(--accent); }

/* ===== Breadcrumb ===== */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 48px;
  font-size: 13px;
  color: var(--text-muted);
  max-width: 1400px;
  margin: 0 auto;
}
.breadcrumb a { color: var(--text-secondary); text-decoration: none; }
.breadcrumb a:hover { color: var(--accent); }
.breadcrumb .sep { color: var(--text-muted); }
.breadcrumb .current { color: var(--text-primary); font-weight: 500; }

/* ===== Page ===== */
.page { max-width: 1400px; margin: 0 auto; padding: 0 48px 64px; }

/* ===== Project Info ===== */
.project-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 0 16px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 24px;
  flex-wrap: wrap;
}
.project-info h1 { font-size: 20px; font-weight: 600; }
.info-meta { display: flex; align-items: center; gap: 16px; margin-left: auto; font-size: 12px; color: var(--text-muted); }
.info-meta-item { display: flex; align-items: center; gap: 5px; }

/* ===== Base Paper Card ===== */
.base-paper-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  box-shadow: var(--shadow-sm);
}
.base-paper-label { font-size: 12px; color: var(--text-muted); margin-bottom: 4px; text-transform: uppercase; letter-spacing: 0.04em; }
.base-paper-title { font-size: 15px; font-weight: 600; }
.base-paper-authors { font-size: 13px; color: var(--text-secondary); margin-top: 2px; }
.base-paper-actions { display: flex; gap: 8px; flex-shrink: 0; }
.base-paper-empty-text { font-size: 14px; color: var(--text-secondary); }

/* ===== Inline Search Progress ===== */
.inline-search-progress {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--bg-card);
  border: 1px solid var(--accent);
  border-radius: var(--radius-md);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
}
.inline-progress-content {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.inline-spinner {
  color: var(--accent);
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.inline-progress-text {
  flex: 1;
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
}
.inline-progress-percent {
  font-size: 13px;
  color: var(--accent);
  font-weight: 600;
  min-width: 36px;
  text-align: right;
}
.inline-progress-bar {
  height: 6px;
  background: var(--bg-secondary);
  border-radius: var(--radius-full);
  overflow: hidden;
}
.inline-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent), #60a5fa);
  border-radius: var(--radius-full);
  transition: width 0.4s ease;
}
.inline-progress-with-btn {
  display: flex;
  align-items: center;
  gap: 12px;
}
.inline-cancel-btn,
.task-cancel-inline-btn {
  font-size: 12px;
  color: #ef4444;
  background: transparent;
  border: 1px solid #ef4444;
  border-radius: 4px;
  padding: 2px 10px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.inline-cancel-btn:hover,
.task-cancel-inline-btn:hover {
  background: #fef2f2;
}

/* ===== Search ===== */
.search-area { display: flex; gap: 10px; margin-bottom: 20px; }
.search-input {
  flex: 1;
  padding: 10px 14px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.15s;
}
.search-input:focus { outline: none; border-color: var(--accent); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); }
.search-input::placeholder { color: var(--text-muted); }

/* ===== Table ===== */
.table-container {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid var(--border-color);
  font-size: 13px;
}
.table-toolbar-left { display: flex; align-items: center; gap: 12px; }
.selection-count { color: var(--accent); font-weight: 500; }
.sort-tabs { display: flex; gap: 2px; background: var(--bg-secondary); border-radius: var(--radius-sm); padding: 2px; }
.sort-tab {
  padding: 5px 14px;
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  border: none;
  background: transparent;
  font-family: inherit;
  transition: all 0.15s;
}
.sort-tab:hover { color: var(--text-primary); }
.sort-tab.active {
  background: var(--bg-primary);
  color: var(--text-primary);
  font-weight: 500;
  box-shadow: var(--shadow-sm);
}

/* ===== Custom Checkbox ===== */
.custom-checkbox {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #d1d5db;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;
  background: var(--bg-primary);
}
.custom-checkbox.checked {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}
.th-checkbox { cursor: pointer; }
.th-checkbox .custom-checkbox { vertical-align: middle; }
.td-checkbox { cursor: pointer; }
.td-checkbox .custom-checkbox { vertical-align: middle; }

table { width: 100%; border-collapse: collapse; }
thead { background: var(--bg-table-head); }
th {
  padding: 10px 16px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--border-color);
  white-space: nowrap;
}
td {
  padding: 14px 16px;
  font-size: 14px;
  border-bottom: 1px solid var(--border-light);
  vertical-align: top;
}
tr:last-child td { border-bottom: none; }
tbody tr { cursor: pointer; transition: background 0.1s; }
tbody tr:hover { background: var(--bg-hover); }
tbody tr.selected { background: var(--accent-light); }

.paper-title {
  font-weight: 500;
  max-width: 420px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.paper-abstract { font-size: 12px; color: var(--text-muted); margin-top: 4px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.paper-authors { font-size: 13px; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 180px; }
.paper-date { font-size: 13px; color: var(--text-secondary); white-space: nowrap; }
.paper-cite { font-size: 13px; font-weight: 500; }
.paper-score {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  background: var(--accent-light);
  color: var(--accent);
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 500;
}
.tag-code { background: var(--success-light); color: var(--success); }
.tag-no-code { background: #f3f4f6; color: var(--text-muted); }

.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--accent);
  font-size: 13px;
  text-decoration: none;
  cursor: pointer;
  background: none;
  border: none;
  font-family: inherit;
  padding: 2px 0;
}
.link-btn:hover { text-decoration: underline; }

/* ===== Pagination ===== */
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-top: 1px solid var(--border-color);
  font-size: 13px;
  color: var(--text-secondary);
}
.pagination-pages { display: flex; gap: 2px; }
.page-btn {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: transparent;
  border: none;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.page-btn:hover { background: var(--bg-hover); color: var(--text-primary); }
.page-btn.active { background: var(--accent); color: #fff; font-weight: 500; }
.page-btn:disabled { opacity: 0.3; cursor: not-allowed; }

/* ===== Action Area ===== */
.action-area { display: flex; gap: 10px; margin-top: 24px; }

/* ===== Selector Panel ===== */
.selector-panel {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  width: 620px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12);
}
.selector-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
}
.selector-header h3 { font-size: 16px; font-weight: 600; }
.selector-body { padding: 16px 24px; overflow-y: auto; flex: 1; }
.selector-search-input {
  width: 100%;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 13px;
  font-family: inherit;
}
.selector-search-input:focus { outline: none; border-color: var(--accent); }
.selector-search-input::placeholder { color: var(--text-muted); }
.selector-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--border-light);
  cursor: pointer;
}
.selector-item:last-child { border-bottom: none; }
.selector-radio {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid #d1d5db;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
  flex-shrink: 0;
}
.selector-radio.selected { border-color: var(--accent); }
.selector-radio.selected::after {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
}
.selector-item-title { font-size: 14px; font-weight: 500; margin-bottom: 2px; }
.selector-item-authors { font-size: 12px; color: var(--text-muted); }
.selector-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 24px; border-top: 1px solid var(--border-color); }
.selector-loading,
.selector-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: var(--text-muted);
  font-size: 14px;
  gap: 12px;
}
.selector-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid var(--border-color);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* ===== Drawer ===== */
.drawer-title { font-size: 17px; font-weight: 600; margin-bottom: 14px; line-height: 1.4; }
.drawer-meta { margin-bottom: 20px; }
.drawer-meta-item { font-size: 13px; color: var(--text-secondary); margin-bottom: 6px; }
.drawer-meta-label { color: var(--text-muted); margin-right: 8px; font-weight: 500; }
.drawer-abstract { font-size: 14px; line-height: 1.7; color: var(--text-secondary); margin-bottom: 24px; }
.drawer-links { display: flex; gap: 10px; }

/* ===== Empty / Loading ===== */
.empty-state { text-align: center; padding: 48px 0; color: var(--text-muted); font-size: 14px; }
.skeleton-table { padding: 20px; }
.skeleton-row { display: flex; gap: 16px; padding: 14px 0; border-bottom: 1px solid var(--border-light); }
.skeleton-cell { height: 14px; background: var(--bg-hover); border-radius: var(--radius-sm); }

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .page { padding: 0 20px 48px; }
  .breadcrumb { padding: 14px 20px; }
  .header { padding: 0 20px; }
  table { display: block; overflow-x: auto; }
}
</style>
