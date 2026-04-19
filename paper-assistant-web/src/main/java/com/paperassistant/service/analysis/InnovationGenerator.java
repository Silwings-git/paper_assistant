package com.paperassistant.service.analysis;

import com.paperassistant.dto.CrossAnalysisDTO;
import com.paperassistant.dto.InnovationAnalysisDTO;
import com.paperassistant.dto.PaperDTO;
import com.paperassistant.exception.LlmException;
import com.paperassistant.service.llm.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 创新机会生成服务 (阶段 3)
 * 基于交叉分析结果生成研究空白/base 论文/创新点
 */
@Service
public class InnovationGenerator {

    private static final Logger log = LoggerFactory.getLogger(InnovationGenerator.class);

    private final LlmService llmService;

    public InnovationGenerator(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * 生成创新机会
     */
    public InnovationAnalysisDTO generate(CrossAnalysisDTO crossAnalysis,
                                          List<com.paperassistant.dto.PaperDTO> papers) {
        String systemPrompt = "你是一位资深学术研究顾问，擅长发现研究空白和创新机会。请以严格的 JSON 格式返回结果。";

        // 附加交叉分析摘要
        String crossSummary = buildCrossAnalysisSummary(crossAnalysis);
        String paperInfo = buildPaperInfo(papers);
        String userPrompt = PromptTemplates.STAGE3_INNOVATION
                .replace("[交叉分析结果...]", crossSummary)
                .replace("[原始论文信息...]", paperInfo);

        try {
            InnovationAnalysisDTO result = llmService.chatStructured("analysis", systemPrompt, userPrompt, InnovationAnalysisDTO.class);
            log.info("Innovation generation completed, found {} gaps, {} base papers, {} innovation points",
                    result.getGaps() != null ? result.getGaps().size() : 0,
                    result.getBasePapers() != null ? result.getBasePapers().size() : 0,
                    result.getInnovationPts() != null ? result.getInnovationPts().size() : 0);
            return result;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.error("Innovation generation failed: {}", e.getMessage(), e);
            throw new LlmException("dashscope", "OTHER", "创新生成失败: " + e.getMessage(), e);
        }
    }

    private String buildCrossAnalysisSummary(CrossAnalysisDTO crossAnalysis) {
        StringBuilder sb = new StringBuilder();
        if (crossAnalysis.getTopicClusters() != null) {
            sb.append("Topic Clusters: ").append(crossAnalysis.getTopicClusters()).append("\n");
        }
        if (crossAnalysis.getMethodologyGaps() != null) {
            sb.append("Methodology Gaps: ").append(crossAnalysis.getMethodologyGaps()).append("\n");
        }
        if (crossAnalysis.getDatasetGaps() != null) {
            sb.append("Dataset Gaps: ").append(crossAnalysis.getDatasetGaps()).append("\n");
        }
        if (crossAnalysis.getConsensusPoints() != null) {
            sb.append("Consensus Points: ").append(crossAnalysis.getConsensusPoints()).append("\n");
        }
        return sb.toString();
    }

    private String buildPaperInfo(List<PaperDTO> papers) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < papers.size(); i++) {
            PaperDTO p = papers.get(i);
            sb.append(String.format("[%d] %s (citations: %d, arxivId: %s)%n",
                    i, p.getTitle(), p.getCitationCount(), p.getArxivId()));
        }
        return sb.toString();
    }

    /**
     * 将 LLM 返回结果中的索引引用转换为实际 paper ID
     */
    public InnovationAnalysisDTO resolvePaperReferences(InnovationAnalysisDTO result,
                                                         List<PaperDTO> papers) {
        if (result == null || papers == null || papers.isEmpty()) return result;

        // 处理 research gaps 中的 supportingPaperIds
        if (result.getGaps() != null) {
            List<InnovationAnalysisDTO.ResearchGap> resolvedGaps = result.getGaps().stream()
                    .map(gap -> {
                        if (gap.supportingPaperIds() == null) return gap;
                        List<String> resolvedIds = gap.supportingPaperIds().stream()
                                .map(id -> resolveId(id, papers))
                                .collect(Collectors.toList());
                        return new InnovationAnalysisDTO.ResearchGap(
                                gap.category(), gap.description(), gap.evidence(), resolvedIds);
                    })
                    .collect(Collectors.toList());
            result.setGaps(resolvedGaps);
        }

        // 处理 base papers 中的 paperId
        if (result.getBasePapers() != null) {
            List<InnovationAnalysisDTO.BasePaperRecommendation> resolvedBps = result.getBasePapers().stream()
                    .map(bp -> new InnovationAnalysisDTO.BasePaperRecommendation(
                            resolveId(bp.paperId(), papers), bp.arxivId(), bp.title(), bp.authors(),
                            bp.citationCount(), bp.reason(), bp.innovationDirection()))
                    .collect(Collectors.toList());
            result.setBasePapers(resolvedBps);
        }

        // 处理 innovation points 中的 basePaperId
        if (result.getInnovationPts() != null) {
            List<InnovationAnalysisDTO.InnovationPoint> resolvedPts = result.getInnovationPts().stream()
                    .map(pt -> new InnovationAnalysisDTO.InnovationPoint(
                            pt.id(), pt.description(), pt.difficulty(), pt.contributionType(),
                            resolveId(pt.basePaperId(), papers), pt.supportingGap()))
                    .collect(Collectors.toList());
            result.setInnovationPts(resolvedPts);
        }

        return result;
    }

    private String resolveId(String idStr, List<PaperDTO> papers) {
        if (idStr == null || idStr.isBlank()) return idStr;
        try {
            int index = Integer.parseInt(idStr.trim());
            if (index >= 0 && index < papers.size()) {
                return papers.get(index).getId();
            }
        } catch (NumberFormatException e) {
            // 如果不是数字，假设已经是实际 paper ID
            return idStr;
        }
        return idStr;
    }
}
