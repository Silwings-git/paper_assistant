<template>
  <div class="analysis-report">
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
      <router-link :to="`/projects/${projectId}`">{{ project?.name || '' }}</router-link>
      <span class="sep">/</span>
      <span class="current">分析报告</span>
    </nav>

    <div class="page">
      <!-- Report Header -->
      <div class="report-header">
        <div>
          <h1>{{ project?.name || '分析报告' }}</h1>
          <div class="report-header-meta">
            <span v-if="project">{{ project.topic }}</span>
            <span :class="['badge', 'badge-analyzed']" v-if="analysisStore.result">
              <span class="badge-dot"></span>分析完成
            </span>
          </div>
        </div>
        <div style="display: flex; gap: 10px;">
          <button class="btn btn-primary" @click="handleAnalyze"
                  :disabled="analyzing"
                  title="请返回项目页面选择论文后触发分析">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M12 2a10 10 0 1 0 10 10H12V2z"/>
              <path d="M21.12 11.07A10 10 0 0 0 12 2v9.07L21.12 11.07z" opacity="0.5"/>
            </svg>
            触发分析
          </button>
          <button class="btn btn-outline" @click="router.push(`/projects/${projectId}`)">返回项目</button>
        </div>
      </div>

      <!-- Loading skeleton -->
      <div v-if="analysisStore.loading" class="loading-state">
        <div class="skeleton-section">
          <div class="skeleton-line" style="width: 120px;"></div>
          <div class="skeleton-grid">
            <div class="skeleton-card" v-for="i in 4" :key="i"></div>
          </div>
        </div>
        <div class="skeleton-section">
          <div class="skeleton-line" style="width: 160px;"></div>
          <div class="skeleton-card" v-for="i in 3" :key="i"></div>
        </div>
      </div>

      <!-- Analysis results -->
      <template v-else-if="analysisStore.result">
        <!-- Section 1: Research Gaps -->
        <section class="report-section">
          <div class="section-label">
            <span class="section-label-num">1</span>
            <h2>研究空白</h2>
          </div>
          <div v-if="analysisStore.result.gaps.length === 0" class="empty-state">暂无研究空白</div>
          <div v-else class="gap-grid">
            <div v-for="(gap, i) in analysisStore.result.gaps" :key="i"
                 class="gap-card" :class="gapCategoryClass(gap.category)">
              <span :class="['gap-category', gapCategoryBadge(gap.category)]">{{ gap.category }}</span>
              <div class="gap-desc">{{ gap.description }}</div>
              <div>
                <div class="gap-evidence-label">支撑证据</div>
                <div class="gap-evidence">{{ gap.evidence }}</div>
              </div>
              <div class="gap-papers">
                <a v-for="(sp, j) in gap.supportingPapers" :key="j"
                   class="gap-paper-link" href="#">{{ sp.title }}</a>
              </div>
            </div>
          </div>
        </section>

        <!-- Section 2: Base Paper Recommendations -->
        <section class="report-section">
          <div class="section-label">
            <span class="section-label-num">2</span>
            <h2>Base 论文推荐</h2>
          </div>
          <div v-if="analysisStore.result.basePapers.length === 0" class="empty-state">暂无推荐</div>
          <div v-else class="rec-list">
            <div v-for="(bp, i) in analysisStore.result.basePapers" :key="i" class="rec-card">
              <div class="rec-card-header">
                <span :class="['rec-rank', { gold: i === 0 }]">{{ i + 1 }}</span>
                <div>
                  <div class="rec-title">{{ bp.title }}</div>
                  <div class="rec-authors">{{ bp.authors.join(', ') }}</div>
                  <div class="rec-meta">
                    <span>引用: {{ bp.citationCount }}</span>
                    <span>arXiv: {{ bp.arxivId }}</span>
                  </div>
                </div>
              </div>
              <div>
                <div class="rec-reason-label">推荐理由</div>
                <div class="rec-reason">{{ bp.reason }}</div>
              </div>
              <div>
                <div class="rec-innovation-label">建议创新方向</div>
                <div class="rec-innovation">{{ bp.innovationDirection }}</div>
              </div>
              <div class="rec-card-actions">
                <button class="btn btn-primary btn-sm" @click="setAsBasePaper(bp.paperId)">设为 Base 论文</button>
                <button class="btn btn-outline btn-sm">查看原文</button>
              </div>
            </div>
          </div>
        </section>

        <!-- Section 3: Innovation Points -->
        <section class="report-section">
          <div class="section-label">
            <span class="section-label-num">3</span>
            <h2>创新点</h2>
          </div>
          <div v-if="analysisStore.result.innovationPts.length === 0" class="empty-state">暂无创新点</div>
          <div v-else class="innovation-grid">
            <div v-for="(pt, i) in analysisStore.result.innovationPts" :key="i"
                 class="innovation-card" :class="diffClass(pt.difficulty)">
              <div class="innovation-tags">
                <span :class="['innovation-tag', diffTagClass(pt.difficulty)]">{{ diffLabel(pt.difficulty) }}</span>
                <span class="innovation-tag tag-type">{{ pt.contributionType }}</span>
              </div>
              <div class="innovation-desc">{{ pt.description }}</div>
              <div>
                <div class="innovation-ref-label">关联 Base 论文</div>
                <div class="innovation-ref">{{ findBasePaperTitle(pt.basePaperId) }}</div>
              </div>
              <div>
                <div class="innovation-ref-label">对应研究空白</div>
                <div class="innovation-ref">{{ pt.supportingGap }}</div>
              </div>
            </div>
          </div>
        </section>
      </template>

      <!-- No results yet -->
      <div v-else class="empty-state" style="padding: 64px 0; text-align: center; color: var(--text-muted); font-size: 14px;">
        暂无分析结果，点击「触发分析」开始
      </div>

      <!-- Analysis Task Progress -->
      <div v-if="analysisStore.currentTaskId && analysisStore.currentTaskStatus"
           class="task-progress" style="margin-top: 32px;">
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
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { useAnalysisStore } from '@/stores/analysis'
import { ElMessage } from 'element-plus'
import { getActiveAnalysisTaskApi } from '@/api'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const analysisStore = useAnalysisStore()
const analyzing = ref(false)

const projectId = computed(() => String(route.params.id))
const project = computed(() => projectStore.projects.find(p => p.id === projectId.value) || null)

// Watch analysis task status
watch(() => analysisStore.currentTaskStatus, (status) => {
  if (!status) return
  if (status.status === 'FAILED') {
    analyzing.value = false
  } else if (status.status === 'ANALYZED') {
    analyzing.value = false
  } else if (status.status === 'ANALYZING' || status.status === 'PENDING') {
    analyzing.value = true
  }
})

onMounted(async () => {
  if (projectStore.projects.length === 0) await projectStore.fetchList()

  // Try loading existing results
  try {
    await analysisStore.fetchResult(projectId.value)
  } catch { /* no results */ }

  // Restore analysis task state from store or backend
  if (analysisStore.currentTaskId) {
    try {
      const status = await analysisStore.fetchTaskStatus(analysisStore.currentTaskId)
      if (status.status === 'ANALYZING') {
        analyzing.value = true
      }
    } catch { /* ignore */ }
  } else {
    // Try to restore from backend active task
    try {
      const raw = await getActiveAnalysisTaskApi(projectId.value)
      if (raw && raw.status !== 'ANALYZED' && raw.status !== 'FAILED' && raw.status !== 'CANCELLED') {
        analysisStore.currentTaskId = raw.taskId ?? null
        analysisStore.currentTaskStatus = raw as any
        if (raw.status === 'ANALYZING') analyzing.value = true
      }
    } catch { /* no active task */ }
  }
})

onUnmounted(() => {
  analysisStore.stopAnalyzePolling()
})

async function handleAnalyze() {
  ElMessage.warning('请返回项目页面选择论文后触发分析')
  router.push(`/projects/${projectId.value}`)
}

async function setAsBasePaper(paperId: string) {
  try {
    await projectStore.setBase(projectId.value, paperId)
    ElMessage.success('已设置为 Base 论文')
  } catch {
    ElMessage.error('设置 Base 论文失败，请重试')
  }
}

// Gap category helpers
function gapCategoryClass(category: string): string {
  if (category.includes('方法')) return ''
  if (category.includes('矛盾')) return ''
  if (category.includes('未探索')) return ''
  if (category.includes('数据集')) return ''
  return ''
}

function gapCategoryBadge(category: string): string {
  if (category.includes('方法')) return 'gap-cat-method'
  if (category.includes('矛盾')) return 'gap-cat-contradict'
  if (category.includes('未探索')) return 'gap-cat-explore'
  if (category.includes('数据集')) return 'gap-cat-data'
  return 'gap-cat-explore'
}

// Difficulty helpers
function diffClass(difficulty: string): string {
  if (difficulty === 'low') return 'diff-low'
  if (difficulty === 'medium') return 'diff-mid'
  if (difficulty === 'high') return 'diff-high'
  return 'diff-low'
}

function diffTagClass(difficulty: string): string {
  if (difficulty === 'low') return 'tag-diff-low'
  if (difficulty === 'medium') return 'tag-diff-mid'
  if (difficulty === 'high') return 'tag-diff-high'
  return 'tag-diff-low'
}

function diffLabel(difficulty: string): string {
  if (difficulty === 'low') return '低难度'
  if (difficulty === 'medium') return '中难度'
  if (difficulty === 'high') return '高难度'
  return '低难度'
}

function findBasePaperTitle(paperId: string): string {
  if (!analysisStore.result) return '-'
  const bp = analysisStore.result.basePapers.find(p => p.paperId === paperId)
  return bp?.title || `Paper #${paperId}`
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
.page { max-width: 1400px; margin: 0 auto; padding: 0 48px 80px; }

/* ===== Report Header ===== */
.report-header {
  padding: 28px 0 20px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 48px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.report-header h1 { font-size: 24px; font-weight: 700; margin-bottom: 6px; }
.report-header-meta { font-size: 14px; color: var(--text-secondary); display: flex; align-items: center; gap: 14px; }

/* ===== Section ===== */
.report-section { margin-bottom: 64px; }
.report-section:last-child { margin-bottom: 0; }
.section-label { display: flex; align-items: center; gap: 10px; margin-bottom: 24px; }
.section-label-num {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  background: var(--accent-light);
  color: var(--accent);
}
.section-label h2 { font-size: 18px; font-weight: 600; }

/* ===== Gap Cards ===== */
.gap-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.gap-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-shadow: var(--shadow-sm);
  transition: all 0.2s;
}
.gap-card:hover { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); border-color: #d1d5db; }

.gap-category {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
  align-self: flex-start;
}
.gap-cat-explore { background: var(--accent-light); color: var(--accent); }
.gap-cat-contradict { background: var(--warning-light); color: var(--warning); }
.gap-cat-method { background: var(--purple-light); color: var(--purple); }
.gap-cat-data { background: var(--pink-light); color: var(--pink); }

.gap-desc { font-size: 14px; line-height: 1.65; }
.gap-evidence {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  padding: 12px 14px;
  background: var(--bg-evidence);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--accent);
}
.gap-evidence-label { font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); margin-bottom: 4px; font-weight: 600; }
.gap-papers { display: flex; flex-wrap: wrap; gap: 6px; }
.gap-paper-link {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--accent);
  text-decoration: none;
  transition: all 0.15s;
}
.gap-paper-link:hover { background: var(--bg-hover); }

/* ===== Recommendations ===== */
.rec-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  margin-bottom: 20px;
  background: var(--purple-light);
  border: 1px solid rgba(124, 58, 237, 0.12);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--purple);
}
.rec-list { display: flex; flex-direction: column; gap: 16px; }
.rec-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-shadow: var(--shadow-sm);
}
.rec-card-header { display: flex; align-items: flex-start; gap: 14px; }
.rec-rank {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 700;
  background: var(--accent-light);
  color: var(--accent);
}
.rec-rank.gold { background: rgba(217, 119, 6, 0.1); color: var(--warning); }
.rec-title { font-size: 15px; font-weight: 600; line-height: 1.4; }
.rec-authors { font-size: 13px; color: var(--text-secondary); margin-top: 2px; }
.rec-meta { display: flex; gap: 14px; font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.rec-reason { font-size: 14px; line-height: 1.65; padding: 14px 16px; background: var(--bg-evidence); border-radius: var(--radius-sm); }
.rec-reason-label { font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); margin-bottom: 4px; font-weight: 600; }
.rec-innovation { font-size: 14px; color: var(--purple); line-height: 1.6; }
.rec-innovation-label { font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); margin-bottom: 4px; font-weight: 600; }
.rec-card-actions { display: flex; gap: 10px; margin-top: 4px; }

/* ===== Innovation Cards ===== */
.innovation-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px; }
.innovation-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: relative;
  overflow: hidden;
  transition: all 0.2s;
  box-shadow: var(--shadow-sm);
}
.innovation-card:hover { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); border-color: #d1d5db; }
.innovation-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}
.innovation-card.diff-low::before { background: var(--success); }
.innovation-card.diff-mid::before { background: var(--warning); }
.innovation-card.diff-high::before { background: var(--danger); }

.innovation-desc { font-size: 14px; line-height: 1.65; }
.innovation-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.innovation-tag { padding: 4px 10px; border-radius: var(--radius-full); font-size: 11px; font-weight: 500; }
.tag-diff-low { background: var(--success-light); color: var(--success); }
.tag-diff-mid { background: var(--warning-light); color: var(--warning); }
.tag-diff-high { background: var(--danger-light); color: var(--danger); }
.tag-type { background: var(--accent-light); color: var(--accent); }

.innovation-ref { font-size: 12px; color: var(--text-secondary); line-height: 1.5; padding: 10px 12px; background: var(--bg-evidence); border-radius: var(--radius-sm); }
.innovation-ref-label { font-size: 10px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); margin-bottom: 4px; font-weight: 600; }

/* ===== Loading Skeleton ===== */
.loading-state { padding: 24px 0; }
.skeleton-section { margin-bottom: 48px; }
.skeleton-line { height: 18px; background: var(--bg-hover); border-radius: var(--radius-sm); margin-bottom: 20px; }
.skeleton-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.skeleton-card { height: 120px; background: var(--bg-hover); border-radius: var(--radius-lg); }

/* ===== Empty State ===== */
.empty-state { text-align: center; padding: 24px 0; color: var(--text-muted); font-size: 14px; }

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .page { padding: 0 20px 48px; }
  .breadcrumb { padding: 14px 20px; }
  .header { padding: 0 20px; }
  .gap-grid { grid-template-columns: 1fr; }
  .innovation-grid { grid-template-columns: 1fr; }
}
</style>
