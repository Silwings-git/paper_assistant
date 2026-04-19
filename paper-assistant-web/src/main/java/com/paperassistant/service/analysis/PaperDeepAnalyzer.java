package com.paperassistant.service.analysis;

import com.paperassistant.dto.PaperAnalysisDTO;
import com.paperassistant.dto.PaperDTO;
import com.paperassistant.exception.BusinessException;
import com.paperassistant.exception.ErrorCode;
import com.paperassistant.exception.LlmException;
import com.paperassistant.service.fulltext.PaperFullText;
import com.paperassistant.service.fulltext.PaperFullTextExtractor;
import com.paperassistant.service.llm.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 论文深度分析服务 (阶段 1: 逐篇深度理解)
 * LLM 调用 + JSON Schema 验证 + 失败最多重试 2 次
 */
@Service
public class PaperDeepAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(PaperDeepAnalyzer.class);
    private static final int MAX_RETRIES = 2;
    private static final int MAX_CONTEXT_CHARS = 800_000;

    private final LlmService llmService;
    private final PaperFullTextExtractor fullTextExtractor;

    public PaperDeepAnalyzer(LlmService llmService, PaperFullTextExtractor fullTextExtractor) {
        this.llmService = llmService;
        this.fullTextExtractor = fullTextExtractor;
    }

    /**
     * 深度分析单篇论文
     */
    public PaperAnalysisDTO analyze(PaperDTO paper) {
        // Step 1: 提取全文
        PaperFullText fullText = fullTextExtractor.extractFullText(toEntity(paper));

        // Step 2: 安全截断全文（qwen3.6-plus 最大 983616 tokens，字符数需留余量）
        String context = fullText.fullText();
        if (context != null && context.length() > MAX_CONTEXT_CHARS) {
            log.warn("Paper {} full text too long ({} chars), truncating to {}", paper.getId(), context.length(), MAX_CONTEXT_CHARS);
            context = context.substring(0, MAX_CONTEXT_CHARS);
        }

        // Step 3: 构建 prompt
        String prompt = PromptTemplates.STAGE1_UNDERSTANDING
                .replace("{title}", paper.getTitle())
                .replace("{abstract}", paper.getAbstractText() != null ? paper.getAbstractText() : "")
                .replace("{citationCount}", String.valueOf(paper.getCitationCount()));

        // Step 4: 调用 LLM 结构化输出 (带重试)
        int retries = 0;
        while (retries <= MAX_RETRIES) {
            try {
                PaperAnalysisDTO result = llmService.chatStructured("analysis", prompt, context, PaperAnalysisDTO.class);
                log.debug("Paper {} analyzed successfully: coreQuestion={}", paper.getId(), result.getCoreQuestion());
                return result;
            } catch (LlmException e) {
                retries++;
                if (retries > MAX_RETRIES) {
                    log.error("Failed to analyze paper {} after {} retries", paper.getId(), MAX_RETRIES);
                    throw e;
                }
                log.warn("LLM analysis failed for paper {}, retry {}/{}", paper.getId(), retries, MAX_RETRIES);
            } catch (Exception e) {
                retries++;
                if (retries > MAX_RETRIES) {
                    log.error("Failed to analyze paper {} after {} retries", paper.getId(), MAX_RETRIES);
                    throw new BusinessException(ErrorCode.LLM_ERROR, "论文分析失败: " + e.getMessage(), e);
                }
                log.warn("LLM analysis error for paper {}, retry {}/{}", paper.getId(), retries, MAX_RETRIES);
            }
        }
        return null;
    }

    private com.paperassistant.entity.Paper toEntity(PaperDTO dto) {
        com.paperassistant.entity.Paper entity = new com.paperassistant.entity.Paper();
        entity.setId(dto.getId());
        entity.setArxivId(dto.getArxivId());
        entity.setTitle(dto.getTitle());
        entity.setAbstractText(dto.getAbstractText());
        entity.setPdfUrl(dto.getPdfUrl());
        return entity;
    }
}
