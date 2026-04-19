/**
 * API 层封装 - 基于自动生成的 SDK，统一解包 ApiResponse
 *
 * 设计原则：
 * - 对 stores 层保持与之前完全相同的函数签名和返回值
 * - 内部调用自动生成的 SDK 函数
 * - SDK 函数返回 AxiosResponse，拦截器已将 ApiResponse<T> 解包为 T
 * - 需要取 response.data 获取实际数据
 */
import { ElMessage } from 'element-plus'
import { client } from './generated/client.gen'
import { list as genListProjects, create as genCreateProject, update as genUpdateProject, getById as genGetProject, delete_ as genDeleteProject, setBasePaper } from './generated/sdk.gen'
import type { CreateProjectRequest, UpdateProjectRequest, ProjectDto, SetBasePaperRequest } from './generated/types.gen'
import { search as genSearchPapers, list1 as genListPapers, getById1 as genGetPaper, getTaskStatus as genGetPaperTaskStatus } from './generated/sdk.gen'
import type { PaperDto, TaskStatusDto, AnalysisResult } from './generated/types.gen'
import { getResult as genGetAnalysisResult, getTaskStatus1 as genGetAnalysisTaskStatus, getActiveTask as genGetActivePaperTask, getActiveTask1 as genGetActiveAnalysisTask } from './generated/sdk.gen'

// 初始化客户端配置
client.setConfig({
  baseURL: '',
  timeout: 30000,
})

// 添加响应拦截器 - 解包 ApiResponse
// 后端返回 { code: 200, data: T }，拦截后 response.data = T
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const axiosInstance = client.instance as any
axiosInstance.interceptors.response.use(
  (response: { data: { code?: number; message?: string; data?: unknown } }) => {
    const body = response.data
    if (body.code && body.code !== 200) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return { ...response, data: body.data }
  },
  (error: unknown) => {
    ElMessage.error('网络连接失败，请重试')
    return Promise.reject(error)
  }
)

// 工具函数：从 SDK 返回的 AxiosResponse 中提取 data
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function unwrap<T>(resp: any): T {
  const data = resp?.data ?? resp
  if (data === undefined || data === null) {
    throw new Error('API 返回空数据')
  }
  return data as T
}

// ========== 项目管理 ==========

export async function getProjects(): Promise<ProjectDto[]> {
  return unwrap<ProjectDto[]>(await genListProjects())
}

export async function createProject(req: CreateProjectRequest): Promise<ProjectDto> {
  return unwrap<ProjectDto>(await genCreateProject({ body: req }))
}

export async function updateProject(id: string, req: UpdateProjectRequest): Promise<ProjectDto> {
  return unwrap<ProjectDto>(await genUpdateProject({ path: { id }, body: req }))
}

export async function getProject(id: string): Promise<ProjectDto> {
  return unwrap<ProjectDto>(await genGetProject({ path: { id } }))
}

export async function deleteProject(id: string): Promise<void> {
  await genDeleteProject({ path: { id } })
}

export async function setProjectBasePaper(id: string, req: SetBasePaperRequest): Promise<ProjectDto> {
  return unwrap<ProjectDto>(await setBasePaper({ path: { id }, body: req }))
}

// ========== 论文检索 ==========

export async function searchPapersApi(projectId: string, keyword: string): Promise<string> {
  return unwrap<string>(await genSearchPapers({ query: { projectId, keyword } }))
}

export async function getPapersApi(projectId: string, page = 1, size = 20, sort = 'relevance'): Promise<{ records: PaperDto[]; total: number }> {
  return unwrap(await genListPapers({ query: { projectId, page, size, sort } }))
}

export async function getPaperApi(id: string): Promise<PaperDto> {
  return unwrap<PaperDto>(await genGetPaper({ path: { id } }))
}

export async function getPaperTaskStatusApi(taskId: string): Promise<TaskStatusDto> {
  return unwrap<TaskStatusDto>(await genGetPaperTaskStatus({ path: { taskId } }))
}

// ========== 分析 ==========

export async function triggerAnalysisApi(projectId: string, paperIds?: string[]): Promise<string> {
  const resp = await client.post<string>({
    url: `/api/v1/analysis/${projectId}`,
    body: paperIds ?? undefined,
  })
  return unwrap<string>(resp)
}

export async function getAnalysisResultApi(projectId: string): Promise<AnalysisResult | null> {
  return unwrap<AnalysisResult | null>(await genGetAnalysisResult({ path: { projectId } }))
}

export async function getAnalysisTaskStatusApi(taskId: string): Promise<TaskStatusDto> {
  return unwrap<TaskStatusDto>(await genGetAnalysisTaskStatus({ path: { taskId } }))
}

export async function getActiveSearchTaskApi(projectId: string): Promise<TaskStatusDto | null> {
  try {
    const resp = await genGetActivePaperTask({ query: { projectId } })
    return unwrap<TaskStatusDto | null>(resp)
  } catch {
    return null
  }
}

export async function getActiveAnalysisTaskApi(projectId: string): Promise<TaskStatusDto | null> {
  try {
    const resp = await genGetActiveAnalysisTask({ query: { projectId } })
    return unwrap<TaskStatusDto | null>(resp)
  } catch {
    return null
  }
}

// ========== 任务取消 ==========

export async function cancelPaperTaskApi(taskId: string): Promise<void> {
  await client.delete({ url: `/api/v1/papers/tasks/${taskId}` })
}

export async function cancelAnalysisTaskApi(taskId: string): Promise<void> {
  await client.delete({ url: `/api/v1/analysis/tasks/${taskId}` })
}
