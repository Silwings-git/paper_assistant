import type { PaperDto as GenPaperDto } from '@/api/generated/types.gen'

export type SortOrder = 'relevance' | 'citation' | 'date';

// 兼容旧接口：将 auto-generated 的 PaperDto 映射为旧 PaperDTO 类型
export type PaperDTO = {
  id: string;
  projectId: string;
  arxivId: string | null;
  title: string;
  abstract: string | null;
  authors: string[];
  publishDate: string | null;
  citationCount: number;
  influenceScore: number;
  hasCode: boolean;
  codeUrl: string | null;
  pdfUrl: string | null;
  category: string | null;
}

// 类型转换函数
export function toPaperDTO(g: GenPaperDto): PaperDTO {
  return {
    id: String(g.id ?? 0),
    projectId: String(g.projectId ?? 0),
    arxivId: g.arxivId ?? null,
    title: g.title ?? '',
    abstract: g.abstractText ?? null, // 字段名映射: abstractText → abstract
    authors: g.authors ?? [],
    publishDate: g.publishDate ?? null,
    citationCount: g.citationCount ?? 0,
    influenceScore: g.influenceScore ?? 0,
    hasCode: g.hasCode ?? false,
    codeUrl: g.codeUrl ?? null,
    pdfUrl: g.pdfUrl ?? null,
    category: g.category ?? null,
  }
}

export function toPaperDTOList(gArr: GenPaperDto[]): PaperDTO[] {
  return gArr.map(toPaperDTO)
}
