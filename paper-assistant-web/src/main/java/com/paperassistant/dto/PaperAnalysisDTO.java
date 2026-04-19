package com.paperassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 论文深度分析结果 DTO (阶段 1 逐篇理解)
 */
@Schema(description = "论文深度分析结果 (阶段1)")
public class PaperAnalysisDTO {

    @Schema(description = "核心研究问题", example = "如何提高 RAG 系统在开放域问答中的准确性")
    private String coreQuestion;

    /**
     * @see MethodologyCategory
     */
    @Schema(description = "方法论分类", example = "EXPERIMENTAL")
    private String methodologyCategory;

    @Schema(description = "关键贡献列表")
    private List<String> keyContributions;

    @Schema(description = "研究局限性列表")
    private List<String> limitations;

    @Schema(description = "关键词列表", example = "[\"RAG\", \"retrieval\", \"vector database\"]")
    private List<String> keywords;

    @Schema(description = "使用的方法和数据集")
    private List<String> methodsAndDatasets;

    public String getCoreQuestion() {
        return coreQuestion;
    }

    public void setCoreQuestion(String coreQuestion) {
        this.coreQuestion = coreQuestion;
    }

    public String getMethodologyCategory() {
        return methodologyCategory;
    }

    public void setMethodologyCategory(String methodologyCategory) {
        this.methodologyCategory = methodologyCategory;
    }

    public List<String> getKeyContributions() {
        return keyContributions;
    }

    public void setKeyContributions(List<String> keyContributions) {
        this.keyContributions = keyContributions;
    }

    public List<String> getLimitations() {
        return limitations;
    }

    public void setLimitations(List<String> limitations) {
        this.limitations = limitations;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getMethodsAndDatasets() {
        return methodsAndDatasets;
    }

    public void setMethodsAndDatasets(List<String> methodsAndDatasets) {
        this.methodsAndDatasets = methodsAndDatasets;
    }

    @Schema(description = "方法论分类")
    public enum MethodologyCategory {
        THEORETICAL, EXPERIMENTAL, SURVEY, COMPARATIVE
    }
}
