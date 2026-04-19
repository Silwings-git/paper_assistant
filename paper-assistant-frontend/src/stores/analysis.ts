import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AnalysisResultDTO, TaskStatusDTO } from '@/types/analysis'
import { toAnalysisResultDTO, toTaskStatusDTO } from '@/types/analysis'
import { triggerAnalysisApi, getAnalysisResultApi, getAnalysisTaskStatusApi, getActiveAnalysisTaskApi } from '@/api'
import { stompManager } from '@/utils/websocket'

export const useAnalysisStore = defineStore('analysis', () => {
  const result = ref<AnalysisResultDTO | null>(null)
  const currentTaskId = ref<string | null>(null)
  const currentTaskStatus = ref<TaskStatusDTO | null>(null)
  const loading = ref(false)

  let analyzePollTimer: ReturnType<typeof setInterval> | null = null

  function stopAnalyzePolling() {
    if (analyzePollTimer) {
      clearInterval(analyzePollTimer)
      analyzePollTimer = null
    }
  }

  async function trigger(projectId: string, paperIds?: string[], onComplete?: () => void) {
    stopAnalyzePolling()
    stompManager.unsubscribeTask(currentTaskId.value || '')
    const taskId = await triggerAnalysisApi(projectId, paperIds)
    currentTaskId.value = taskId

    // 通过 WebSocket 订阅任务状态
    stompManager.subscribeTask(taskId, {
      onProgress: (msg) => {
        currentTaskStatus.value = toTaskStatusDTO(msg as any)
      },
      onComplete: async () => {
        stopAnalyzePolling()
        await fetchResult(projectId)
        onComplete?.()
      },
      onError: () => {
        stopAnalyzePolling()
        currentTaskId.value = null
      }
    })
    return taskId
  }

  async function fetchResult(projectId: string) {
    loading.value = true
    try {
      const raw = await getAnalysisResultApi(projectId)
      result.value = toAnalysisResultDTO(raw)
    } catch {
      // 错误已由拦截器提示
    } finally {
      loading.value = false
    }
  }

  async function fetchTaskStatus(taskId: string) {
    const raw = await getAnalysisTaskStatusApi(taskId)
    const status = toTaskStatusDTO(raw)
    currentTaskStatus.value = status
    return status
  }

  /**
   * 恢复进行中的分析任务（页面刷新/导航后调用）
   */
  async function restoreTask(projectId: string, onComplete?: () => void) {
    const raw = await getActiveAnalysisTaskApi(projectId)
    if (!raw) return
    const status = toTaskStatusDTO(raw)
    if (status.status === 'ANALYZED' || status.status === 'FAILED' || status.status === 'CANCELLED') return

    currentTaskId.value = raw.taskId ?? null
    currentTaskStatus.value = status
    const taskId = raw.taskId!

    // 通过 WebSocket 订阅恢复的任务（修复之前不重启轮询的 bug）
    stompManager.subscribeTask(taskId, {
      onProgress: (msg) => {
        currentTaskStatus.value = toTaskStatusDTO(msg as any)
      },
      onComplete: async () => {
        stopAnalyzePolling()
        await fetchResult(projectId)
        onComplete?.()
      },
      onError: () => {
        stopAnalyzePolling()
        currentTaskId.value = null
        currentTaskStatus.value = null
      }
    })
  }

  return { result, currentTaskId, currentTaskStatus, loading, trigger, fetchResult, fetchTaskStatus, restoreTask, stopAnalyzePolling }
})
