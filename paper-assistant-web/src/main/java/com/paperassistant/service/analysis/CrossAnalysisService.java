package com.paperassistant.service.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperassistant.dto.CrossAnalysisDTO;
import com.paperassistant.dto.PaperAnalysisDTO;
import com.paperassistant.exception.LlmException;
import com.paperassistant.service.llm.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 交叉对比分析服务 (阶段 2)
 * 输入 N 篇 PaperAnalysis，输出主题聚类/方法对比/冲突检测/共识点/缺口
 */
@Service
public class CrossAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(CrossAnalysisService.class);

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public CrossAnalysisService(LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行交叉分析
     */
    public CrossAnalysisDTO analyze(List<PaperAnalysisDTO> analyses) {
        String systemPrompt = "你是一位资深学术研究顾问，擅长多论文交叉对比分析。请以严格的 JSON 格式返回结果。";
        String analysesSummary = buildAnalysesSummary(analyses);
        String userPrompt = PromptTemplates.STAGE2_CROSS_ANALYSIS
                .replace("{count}", String.valueOf(analyses.size()))
                .replace("[论文分析列表...]", analysesSummary);

        try {
            CrossAnalysisDTO result = llmService.chatStructured("analysis", systemPrompt, userPrompt, CrossAnalysisDTO.class);
            log.info("Cross analysis completed, found {} topic clusters", result.getTopicClusters() != null ? result.getTopicClusters().size() : 0);
            return result;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.error("Cross analysis failed: {}", e.getMessage(), e);
            throw new LlmException("dashscope", "OTHER", "交叉分析失败: " + e.getMessage(), e);
        }
    }

    private String buildAnalysesSummary(List<PaperAnalysisDTO> analyses) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < analyses.size(); i++) {
            PaperAnalysisDTO a = analyses.get(i);
            sb.append(String.format("[%d] coreQuestion: %s, methodology: %s, contributions: %s, limitations: %s%n",
                    i, a.getCoreQuestion(), a.getMethodologyCategory(),
                    a.getKeyContributions(), a.getLimitations()));
        }
        return sb.toString();
    }
}
