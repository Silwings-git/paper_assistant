import { ProjectStatus } from '@/types/project'

export function getProjectStatusInfo(status: ProjectStatus): {
  label: string;
  color: string;
  disabledActions: string[];
} {
  const disabledActionsByStatus: Record<ProjectStatus, string[]> = {
    [ProjectStatus.CREATED]: ['triggerAnalysis', 'setBasePaper'],
    [ProjectStatus.SEARCHING]: ['search', 'triggerAnalysis', 'setBasePaper'],
    [ProjectStatus.SEARCHED]: [],
    [ProjectStatus.ANALYZING]: ['search', 'triggerAnalysis', 'setBasePaper'],
    [ProjectStatus.ANALYZED]: [],
  }

  const statusMap: Record<ProjectStatus, { label: string; color: string }> = {
    [ProjectStatus.CREATED]: { label: '已创建', color: '#909399' },
    [ProjectStatus.SEARCHING]: { label: '检索中', color: '#409eff' },
    [ProjectStatus.SEARCHED]: { label: '检索完成', color: '#67c23a' },
    [ProjectStatus.ANALYZING]: { label: '分析中', color: '#e6a23c' },
    [ProjectStatus.ANALYZED]: { label: '分析完成', color: '#67c23a' },
  }

  const info = statusMap[status] || { label: status, color: '#909399' }
  return {
    ...info,
    disabledActions: disabledActionsByStatus[status] || [],
  }
}
