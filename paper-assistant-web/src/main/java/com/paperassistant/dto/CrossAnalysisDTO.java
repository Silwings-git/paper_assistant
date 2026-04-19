package com.paperassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 交叉分析结果 DTO (阶段 2)
 */
@Schema(description = "交叉分析结果 (阶段2)")
public class CrossAnalysisDTO {

    @Schema(description = "主题簇列表")
    private List<TopicCluster> topicClusters;

    @Schema(description = "方法对比列表")
    private List<MethodComparison> methodComparison;

    @Schema(description = "观点冲突列表")
    private List<Conflict> conflicts;

    @Schema(description = "共识观点列表")
    private List<String> consensusPoints;

    @Schema(description = "方法论差距列表")
    private List<String> methodologyGaps;

    @Schema(description = "数据集差距列表")
    private List<String> datasetGaps;

    public List<TopicCluster> getTopicClusters() {
        return topicClusters;
    }

    public void setTopicClusters(List<TopicCluster> topicClusters) {
        this.topicClusters = topicClusters;
    }

    public List<MethodComparison> getMethodComparison() {
        return methodComparison;
    }

    public void setMethodComparison(List<MethodComparison> methodComparison) {
        this.methodComparison = methodComparison;
    }

    public List<Conflict> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<Conflict> conflicts) {
        this.conflicts = conflicts;
    }

    public List<String> getConsensusPoints() {
        return consensusPoints;
    }

    public void setConsensusPoints(List<String> consensusPoints) {
        this.consensusPoints = consensusPoints;
    }

    public List<String> getMethodologyGaps() {
        return methodologyGaps;
    }

    public void setMethodologyGaps(List<String> methodologyGaps) {
        this.methodologyGaps = methodologyGaps;
    }

    public List<String> getDatasetGaps() {
        return datasetGaps;
    }

    public void setDatasetGaps(List<String> datasetGaps) {
        this.datasetGaps = datasetGaps;
    }

    @Schema(description = "主题簇")
    public record TopicCluster(
            @Schema(description = "主题名称", example = "检索优化")
            String name,
            @Schema(description = "论文索引列表")
            List<Integer> paperIndices) {}

    @Schema(description = "方法对比")
    public record MethodComparison(
            @Schema(description = "方法名称", example = "BM25")
            String method,
            @Schema(description = "使用该方法论文索引列表")
            List<Integer> papers) {}

    @Schema(description = "观点冲突")
    public record Conflict(
            @Schema(description = "冲突描述", example = "论文A认为方法X优于Y，论文B得出相反结论")
            String description,
            @Schema(description = "论文A的索引", example = "0")
            int paperA,
            @Schema(description = "论文B的索引", example = "1")
            int paperB) {}
}
