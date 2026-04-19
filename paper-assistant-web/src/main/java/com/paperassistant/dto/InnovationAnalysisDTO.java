package com.paperassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 创新分析结果 DTO (阶段 3)
 */
@Schema(description = "创新分析结果 (阶段3)")
public class InnovationAnalysisDTO {

    @Schema(description = "研究空白列表")
    private List<ResearchGap> gaps;

    @Schema(description = "基础论文推荐列表")
    private List<BasePaperRecommendation> basePapers;

    @Schema(description = "创新点列表")
    private List<InnovationPoint> innovationPts;

    public List<ResearchGap> getGaps() {
        return gaps;
    }

    public void setGaps(List<ResearchGap> gaps) {
        this.gaps = gaps;
    }

    public List<BasePaperRecommendation> getBasePapers() {
        return basePapers;
    }

    public void setBasePapers(List<BasePaperRecommendation> basePapers) {
        this.basePapers = basePapers;
    }

    public List<InnovationPoint> getInnovationPts() {
        return innovationPts;
    }

    public void setInnovationPts(List<InnovationPoint> innovationPts) {
        this.innovationPts = innovationPts;
    }

    @Schema(description = "研究空白")
    public record ResearchGap(
            @Schema(description = "差距类别", example = "方法论")
            String category,
            @Schema(description = "差距描述", example = "现有RAG系统缺乏对多模态检索的支持")
            String description,
            @Schema(description = "证据描述")
            String evidence,
            @Schema(description = "支持该差距的论文ID列表")
            List<String> supportingPaperIds
    ) {}

    @Schema(description = "基础论文推荐")
    public record BasePaperRecommendation(
            @Schema(description = "论文ID", example = "paper-001")
            String paperId,
            @Schema(description = "arXiv论文ID", example = "2401.12345")
            String arxivId,
            @Schema(description = "论文标题", example = "Attention Is All You Need")
            String title,
            @Schema(description = "作者列表")
            List<String> authors,
            @Schema(description = "引用次数", example = "50000")
            Integer citationCount,
            @Schema(description = "推荐理由")
            String reason,
            @Schema(description = "创新方向")
            String innovationDirection
    ) {}

    @Schema(description = "创新点")
    public record InnovationPoint(
            @Schema(description = "创新点ID", example = "IP-001")
            String id,
            @Schema(description = "创新点描述")
            String description,
            @Schema(description = "实现难度: low/medium/high", example = "medium")
            String difficulty,
            @Schema(description = "贡献类型", example = "方法改进")
            String contributionType,
            @Schema(description = "基础论文ID", example = "paper-001")
            String basePaperId,
            @Schema(description = "对应的研究差距")
            String supportingGap
    ) {}
}
