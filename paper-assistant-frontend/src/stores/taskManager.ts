import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { TaskStatusDTO } from '@/types/analysis'
import { toTaskStatusDTO } from '@/types/analysis'
import { stompManager } from '@/utils/websocket'
import { getActiveSearchTaskApi, getActiveAnalysisTaskApi, cancelPaperTaskApi, cancelAnalysisTaskApi } from '@/api'

export interface ActiveTaskInfo {
  taskId: string
  taskType: 'SEARCH' | 'ANALYSIS'
  projectId: string
  status: TaskStatusDTO
}

export const useTaskManagerStore = defineStore('taskManager', () => {
  const activeTasks = ref<Map<string, ActiveTaskInfo>>(new Map()) // keyed by taskId

  const allActiveTasks = computed(() => Array.from(activeTasks.value.values()))

  /**
   * 注册项目：进入项目详情页时调用，查询 active 任务并订阅 WebSocket
   */
  async function register(projectId: string) {
    // 查询 active search task
    const searchRaw = await getActiveSearchTaskApi(projectId)
    if (searchRaw && !isTerminal(searchRaw.status ?? '')) {
      addTask(searchRaw)
    }

    // 查询 active analysis task
    const analysisRaw = await getActiveAnalysisTaskApi(projectId)
    if (analysisRaw && !isTerminal(analysisRaw.status ?? '')) {
      addTask(analysisRaw)
    }
  }

  /**
   * 注销项目：离开项目详情页时调用，取消 WebSocket 订阅
   */
  function unregister(projectId: string) {
    const tasksToRemove: string[] = []
    activeTasks.value.forEach((info, taskId) => {
      if (info.projectId === projectId) {
        tasksToRemove.push(taskId)
      }
    })
    tasksToRemove.forEach(taskId => {
      stompManager.unsubscribeTask(taskId)
      activeTasks.value.delete(taskId)
    })
  }

  /**
   * 取消任务
   */
  async function cancelTask(taskId: string, taskType: 'SEARCH' | 'ANALYSIS') {
    try {
      if (taskType === 'SEARCH') {
        await cancelPaperTaskApi(taskId)
      } else {
        await cancelAnalysisTaskApi(taskId)
      }
      stompManager.unsubscribeTask(taskId)
      activeTasks.value.delete(taskId)
    } catch (e) {
      console.error('Failed to cancel task:', taskId, e)
    }
  }

  function addTask(raw: any) {
    const taskId = raw.taskId
    if (!taskId) return
    const taskType = (raw.taskType === 'SEARCH' ? 'SEARCH' : 'ANALYSIS') as 'SEARCH' | 'ANALYSIS'
    const status = toTaskStatusDTO(raw)

    activeTasks.value.set(taskId, { taskId, taskType, projectId: raw.projectId, status })

    // 通过 WebSocket 订阅该任务的实时更新
    stompManager.subscribeTask(taskId, {
      onProgress: (msg) => {
        const existing = activeTasks.value.get(taskId)
        if (existing) {
          existing.status = toTaskStatusDTO(msg as any)
        }
      },
      onComplete: () => {
        activeTasks.value.delete(taskId)
      },
      onError: () => {
        activeTasks.value.delete(taskId)
      }
    })
  }

  function isTerminal(status: string): boolean {
    return ['SEARCHED', 'ANALYZED', 'FAILED', 'CANCELLED'].includes(status)
  }

  return {
    activeTasks,
    allActiveTasks,
    register,
    unregister,
    cancelTask
  }
})
