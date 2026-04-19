import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { PaperDTO, SortOrder } from '@/types/paper'
import type { TaskStatusDTO } from '@/types/analysis'
import { toPaperDTOList } from '@/types/paper'
import { toTaskStatusDTO } from '@/types/analysis'
import { searchPapersApi, getPapersApi, getPaperTaskStatusApi, getActiveSearchTaskApi } from '@/api'
import { extractErrorMessage } from '@/utils/error'
import { stompManager } from '@/utils/websocket'

export const usePaperStore = defineStore('paper', () => {
  const papers = ref<PaperDTO[]>([])
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(10)
  const sortOrder = ref<SortOrder>('relevance')
  const loading = ref(false)
  const searchLoading = ref(false)
  const currentTaskId = ref<string | null>(null)
  const currentTaskStatus = ref<TaskStatusDTO | null>(null)

  let searchPollTimer: ReturnType<typeof setInterval> | null = null

  function stopSearchPolling() {
    if (searchPollTimer) {
      clearInterval(searchPollTimer)
      searchPollTimer = null
    }
  }

  async function search(projectId: string, keyword: string, onComplete?: () => void, onError?: (message: string) => void) {
    searchLoading.value = true
    stopSearchPolling()
    stompManager.unsubscribeTask(currentTaskId.value || '')
    let taskId: string
    try {
      taskId = await searchPapersApi(projectId, keyword)
    } catch (e) {
      searchLoading.value = false
      currentTaskId.value = null
      currentTaskStatus.value = null
      onError?.(extractErrorMessage(e))
      throw e
    }
    currentTaskId.value = taskId

    // 通过 WebSocket 订阅任务状态
    stompManager.subscribeTask(taskId, {
      onProgress: (msg) => {
        currentTaskStatus.value = toTaskStatusDTO(msg as any)
      },
      onComplete: () => {
        stopSearchPolling()
        searchLoading.value = false
        currentTaskId.value = null
        currentTaskStatus.value = null
        onComplete?.()
      },
      onError: (message) => {
        stopSearchPolling()
        searchLoading.value = false
        currentTaskId.value = null
        currentTaskStatus.value = null
        onError?.(message)
      }
    })
    return taskId
  }

  async function fetchList(projectId: string) {
    loading.value = true
    try {
      const res = await getPapersApi(projectId, currentPage.value, pageSize.value, sortOrder.value)
      if (res?.records) {
        papers.value = toPaperDTOList(res.records)
        total.value = res.total ?? 0
      }
    } catch {
      // 错误已由拦截器提示
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取项目中的所有论文（不分页，用于 Base 论文选择器）
   */
  async function fetchAllPapers(projectId: string): Promise<PaperDTO[]> {
    try {
      const res = await getPapersApi(projectId, 1, 9999, 'relevance')
      if (res?.records) {
        return toPaperDTOList(res.records)
      }
      return []
    } catch {
      return []
    }
  }

  async function fetchTaskStatus(taskId: string) {
    const raw = await getPaperTaskStatusApi(taskId)
    const status = toTaskStatusDTO(raw)
    currentTaskStatus.value = status
    return status
  }

  /**
   * 恢复进行中的检索任务（页面刷新/导航后调用）
   */
  async function restoreTask(projectId: string, onComplete?: () => void, onError?: (message: string) => void) {
    const raw = await getActiveSearchTaskApi(projectId)
    if (!raw) return
    const status = toTaskStatusDTO(raw)
    if (status.status === 'SEARCHED' || status.status === 'FAILED' || status.status === 'CANCELLED') return

    currentTaskId.value = raw.taskId ?? null
    currentTaskStatus.value = status
    searchLoading.value = true
    const taskId = raw.taskId!

    // 通过 WebSocket 订阅恢复的任务
    stompManager.subscribeTask(taskId, {
      onProgress: (msg) => {
        currentTaskStatus.value = toTaskStatusDTO(msg as any)
      },
      onComplete: () => {
        stopSearchPolling()
        searchLoading.value = false
        currentTaskId.value = null
        currentTaskStatus.value = null
        onComplete?.()
      },
      onError: (message) => {
        stopSearchPolling()
        searchLoading.value = false
        currentTaskId.value = null
        currentTaskStatus.value = null
        onError?.(message)
      }
    })
  }

  function selectPaper(_id: number) {
    // reserved
  }

  function deselectPaper(_id: number) {
    // reserved
  }

  function clearSelection() {
    // reserved
  }

  return {
    papers, total, currentPage, pageSize, sortOrder, loading,
    searchLoading, selectedIds: ref<Set<number>>(new Set()),
    currentTaskId, currentTaskStatus,
    search, fetchList, fetchAllPapers, fetchTaskStatus, restoreTask, stopSearchPolling,
    selectPaper, deselectPaper, clearSelection
  }
})
