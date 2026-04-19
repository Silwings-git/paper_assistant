import type { ProjectDto as GenProjectDto } from '@/api/generated/types.gen'

export enum ProjectStatus {
  CREATED = 'CREATED',
  SEARCHING = 'SEARCHING',
  SEARCHED = 'SEARCHED',
  ANALYZING = 'ANALYZING',
  ANALYZED = 'ANALYZED',
}

// 兼容旧接口：将 auto-generated 的 ProjectDto 映射为旧 ProjectDTO 类型
export type ProjectDTO = {
  id: string;
  name: string;
  description: string | null;
  topic: string;
  status: ProjectStatus;
  basePaperId: string | null;
  basePaperTitle: string | null;
  createTime: string;
  updateTime: string;
  paperCount: number;
}

// 类型转换函数
export function toProjectDTO(g: GenProjectDto): ProjectDTO {
  return {
    id: String(g.id ?? 0),
    name: g.name ?? '',
    description: g.description ?? null,
    topic: g.topic ?? '',
    status: g.status as ProjectStatus ?? ProjectStatus.CREATED,
    basePaperId: g.basePaperId != null ? String(g.basePaperId) : null,
    basePaperTitle: g.basePaperTitle ?? null,
    createTime: g.createTime ?? '',
    updateTime: g.updateTime ?? '',
    paperCount: g.paperCount ?? 0,
  }
}

export function toProjectDtoList(gArr: GenProjectDto[]): ProjectDTO[] {
  return gArr.map(toProjectDTO)
}
