import type { TaskStatusDto as GenTaskStatusDto, AnalysisResult as GenAnalysisResult } from '@/api/generated/types.gen'

// TaskStatusDTO - 字段完全兼容
export type TaskStatusDTO = {
  taskId: string;
  taskType: string;
  projectId: string;
  status: string;
  progress: number;
  stage: string;
  message: string;
  resultData?: string;
}

export function toTaskStatusDTO(g: GenTaskStatusDto): TaskStatusDTO {
  return {
    taskId: g.taskId ?? '',
    taskType: g.taskType ?? '',
    projectId: String(g.projectId ?? 0),
    status: g.status ?? '',
    progress: g.progress ?? 0,
    stage: g.stage ?? '',
    message: g.message ?? '',
    resultData: g.resultData,
  }
}

// AnalysisResultDTO - 旧类型为扁平结构，新结构为 JSON 字符串字段
// 由于后端存储的是 JSON 字符串，前端需要解析
export interface ResearchGap {
  category: string;
  description: string;
  evidence: string;
  supportingPapers: Array<{ arxivId: string; title: string }>;
}

export interface BasePaperRecommendation {
  paperId: string;
  arxivId: string;
  title: string;
  authors: string[];
  citationCount: number;
  reason: string;
  innovationDirection: string;
}

export interface InnovationPoint {
  id: string;
  description: string;
  difficulty: 'low' | 'medium' | 'high';
  contributionType: string;
  basePaperId: string;
  supportingGap: string;
}

export type AnalysisResultDTO = {
  id: string;
  projectId: string;
  gaps: ResearchGap[];
  basePapers: BasePaperRecommendation[];
  innovationPts: InnovationPoint[];
  createTime: string;
}

export function toAnalysisResultDTO(g: GenAnalysisResult | null): AnalysisResultDTO | null {
  if (!g) return null
  return {
    id: String(g.id ?? 0),
    projectId: String(g.projectId ?? 0),
    gaps: parseJsonArray<ResearchGap>(g.gaps).map((item) => ({
      ...item,
      supportingPapers: Array.isArray(item.supportingPapers) ? item.supportingPapers : [],
    })),
    basePapers: parseJsonArray<BasePaperRecommendation>(g.basePapers).map((item) => ({
      ...item,
      authors: Array.isArray(item.authors) ? item.authors : [],
      citationCount: item.citationCount ?? 0,
    })),
    innovationPts: parseJsonArray<InnovationPoint>(g.innovationPts).map((item) => ({
      ...item,
      difficulty: (item.difficulty ?? 'low') as 'low' | 'medium' | 'high',
    })),
    createTime: g.createTime ?? '',
  }
}

function parseJson<T>(str: string | undefined, fallback: T): T {
  if (!str) return fallback
  try {
    const parsed = JSON.parse(str) as T
    return parsed
  } catch {
    return fallback
  }
}

function parseJsonArray<T extends object>(str: string | undefined): T[] {
  const parsed = parseJson<T[] | null | undefined>(str, undefined)
  if (!parsed || !Array.isArray(parsed)) return []
  return parsed
}
