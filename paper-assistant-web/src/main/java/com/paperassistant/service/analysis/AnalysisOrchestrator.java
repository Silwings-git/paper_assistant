package com.paperassistant.service.analysis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paperassistant.config.WebSocketBroadcastService;
import com.paperassistant.dto.CrossAnalysisDTO;
import com.paperassistant.dto.InnovationAnalysisDTO;
import com.paperassistant.dto.PaperAnalysisDTO;
import com.paperassistant.dto.PaperDTO;
import com.paperassistant.entity.AnalysisResult;
import com.paperassistant.entity.Paper;
import com.paperassistant.entity.ProjectStatus;
import com.paperassistant.exception.BusinessException;
import com.paperassistant.exception.ErrorCode;
import com.paperassistant.mapper.AnalysisResultMapper;
import com.paperassistant.mapper.PaperMapper;
import com.paperassistant.service.project.ProjectService;
import com.paperassistant.service.retrieval.TaskStatusService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 三阶段分析编排服务
 */
@Service
public class AnalysisOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AnalysisOrchestrator.class);
    private static final int MAX_ANALYSIS_PAPERS = 20;

    private final ProjectService projectService;
    private final TaskStatusService taskStatusService;
    private final WebSocketBroadcastService broadcastService;
    private final AnalysisResultMapper analysisResultMapper;
    private final PaperMapper paperMapper;
    private final PaperDeepAnalyzer paperDeepAnalyzer;
    private final CrossAnalysisService crossAnalysisService;
    private final InnovationGenerator innovationGenerator;
    private final ObjectMapper objectMapper;
    private final AnalysisOrchestrator self;

    public AnalysisOrchestrator(ProjectService projectService,
                                TaskStatusService taskStatusService,
                                WebSocketBroadcastService broadcastService,
                                AnalysisResultMapper analysisResultMapper,
                                PaperMapper paperMapper,
                                PaperDeepAnalyzer paperDeepAnalyzer,
                                CrossAnalysisService crossAnalysisService,
                                InnovationGenerator innovationGenerator,
                                ObjectMapper objectMapper,
                                @org.springframework.context.annotation.Lazy AnalysisOrchestrator self) {
        this.projectService = projectService;
        this.taskStatusService = taskStatusService;
        this.broadcastService = broadcastService;
        this.analysisResultMapper = analysisResultMapper;
        this.paperMapper = paperMapper;
        this.paperDeepAnalyzer = paperDeepAnalyzer;
        this.crossAnalysisService = crossAnalysisService;
        this.innovationGenerator = innovationGenerator;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    /**
     * 创建分析任务并异步执行
     */
    public String analyzeAsync(String projectId, List<String> paperIds) {
        String taskId = UUID.randomUUID().toString();
        taskStatusService.createTask(taskId, "ANALYSIS", projectId);

        // 异步执行实际分析
        self.doAnalyzeAsync(taskId, projectId, paperIds);

        return taskId;
    }

    /**
     * 实际异步分析逻辑
     */
    @Async("analysisExecutor")
    public CompletableFuture<Void> doAnalyzeAsync(String taskId, String projectId, List<String> paperIds) {
        try {
            // 获取项目论文
            LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Paper::getProjectId, projectId);
            wrapper.eq(Paper::getIsDeleted, 0);
            wrapper.in(Paper::getId, paperIds);
            wrapper.orderByAsc(Paper::getId);
            List<Paper> papers = paperMapper.selectList(wrapper);

            if (papers.isEmpty()) {
                taskStatusService.updateTask(taskId, "FAILED", 0, "选中的论文不存在或已被删除", null);
                broadcastService.broadcastError(taskId, "选中的论文不存在或已被删除");
                return CompletableFuture.completedFuture(null);
            }

            // 分析数量上限检查 (2.5)
            List<Paper> papersToAnalyze = papers;
            if (papers.size() > MAX_ANALYSIS_PAPERS) {
                log.warn("Project {} has {} papers, limiting to {}", projectId, papers.size(), MAX_ANALYSIS_PAPERS);
                papersToAnalyze = papers.subList(0, MAX_ANALYSIS_PAPERS);
                taskStatusService.updateTask(taskId, "ANALYZING", 0,
                        "论文数量 " + papers.size() + " 超过上限 " + MAX_ANALYSIS_PAPERS + "，仅分析前 " + MAX_ANALYSIS_PAPERS + " 篇", null);
            }

            // 转换为 DTO
            List<PaperDTO> paperDTOs = papersToAnalyze.stream().map(this::toDTO).toList();

            // 更新项目状态为分析中
            try {
                projectService.transitionStatus(projectId, ProjectStatus.ANALYZING);
            } catch (Exception e) {
                log.warn("Failed to transition project status to ANALYZING: projectId={}", projectId, e);
            }

            // ===== 阶段 1: 逐篇深度理解 (0% -> 85%) =====
            taskStatusService.updateTask(taskId, "ANALYZING", 0, "阶段 1: 逐篇深度理解", null);
            broadcastService.broadcastProgress(taskId, "ANALYZING", 0, "阶段 1: 逐篇深度理解 (0/" + paperDTOs.size() + ")");

            List<PaperAnalysisDTO> analyses = new ArrayList<>();
            int total = paperDTOs.size();
            for (int i = 0; i < total; i++) {
                PaperDTO paper = paperDTOs.get(i);
                log.info("阶段 1: 分析论文 {}/{}", i + 1, total);
                PaperAnalysisDTO analysis = paperDeepAnalyzer.analyze(paper);
                if (analysis != null) {
                    analyses.add(analysis);
                }
                // 进度: 0% -> 85%
                int progress = (int) (85.0 * (i + 1) / total);
                String msg = String.format("阶段 1: 逐篇深度理解 (%d/%d)", i + 1, total);
                taskStatusService.updateTask(taskId, "ANALYZING", progress, msg, null);
                broadcastService.broadcastProgress(taskId, "ANALYZING", progress, msg);
            }

            log.info("阶段 1 完成，成功分析 {} 篇论文", analyses.size());

            // 检查任务是否已被取消
            if (taskStatusService.isCancelled(taskId)) {
                log.info("Analysis task was cancelled after stage 1: taskId={}", taskId);
                return CompletableFuture.completedFuture(null);
            }

            // ===== 阶段 2: 交叉对比分析 (85% -> 90%) =====
            taskStatusService.updateTask(taskId, "ANALYZING", 85, "阶段 2: 交叉对比分析", null);
            broadcastService.broadcastProgress(taskId, "ANALYZING", 85, "阶段 2: 交叉对比分析");

            CrossAnalysisDTO crossAnalysis;
            if (analyses.size() >= 2) {
                crossAnalysis = crossAnalysisService.analyze(analyses);
            } else {
                log.warn("Only {} paper(s) analyzed, skipping cross-analysis", analyses.size());
                crossAnalysis = new CrossAnalysisDTO();
            }

            // 检查任务是否已被取消
            if (taskStatusService.isCancelled(taskId)) {
                log.info("Analysis task was cancelled after stage 2: taskId={}", taskId);
                return CompletableFuture.completedFuture(null);
            }

            taskStatusService.updateTask(taskId, "ANALYZING", 90, "阶段 2 完成，开始创新生成", null);
            broadcastService.broadcastProgress(taskId, "ANALYZING", 90, "阶段 2 完成，开始创新生成");

            // ===== 阶段 3: 创新机会生成 (90% -> 100%) =====
            taskStatusService.updateTask(taskId, "ANALYZING", 90, "阶段 3: 创新机会生成", null);
            broadcastService.broadcastProgress(taskId, "ANALYZING", 90, "阶段 3: 创新机会生成");

            InnovationAnalysisDTO innovation = innovationGenerator.generate(crossAnalysis, paperDTOs);
            // 将 LLM 返回的索引引用转换为实际 paper ID
            innovationGenerator.resolvePaperReferences(innovation, paperDTOs);

            // 保存分析结果
            String resultId = com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr();
            analysisResultMapper.insertWithJsonB(
                    resultId, projectId,
                    toJson(innovation.getGaps()),
                    toJson(innovation.getBasePapers()),
                    toJson(innovation.getInnovationPts()));

            taskStatusService.updateTask(taskId, "ANALYZED", 100, "分析完成", null);
            broadcastService.broadcastProgress(taskId, "ANALYZED", 100, "分析完成");

            // 更新项目状态为分析完成
            try {
                projectService.transitionStatus(projectId, ProjectStatus.ANALYZED);
            } catch (Exception e) {
                log.warn("Failed to transition project status to ANALYZED: projectId={}", projectId, e);
            }

            log.info("三阶段分析完成: projectId={}, taskId={}", projectId, taskId);

        } catch (Exception e) {
            log.error("Analysis failed for project {}: {}", projectId, e.getMessage(), e);
            taskStatusService.updateTask(taskId, "FAILED", 0, "分析失败: " + e.getMessage(), null);
            broadcastService.broadcastError(taskId, e.getMessage());

            // 分析失败时恢复项目状态
            try {
                projectService.transitionStatus(projectId, ProjectStatus.SEARCHED);
            } catch (Exception e2) {
                log.warn("Failed to restore project status after analysis failure: projectId={}", projectId, e2);
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 获取分析结果
     */
    public AnalysisResult getAnalysisResult(String projectId) {
        AnalysisResult result = analysisResultMapper.selectLatestByProjectId(projectId);
        if (result == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "该项目暂无分析结果");
        }
        return result;
    }

    /**
     * 查询任务状态
     */
    public Object getTaskStatus(String taskId) {
        return taskStatusService.getTaskStatus(taskId);
    }

    /**
     * 查询项目最新的进行中分析任务
     */
    public Object getActiveTask(String projectId) {
        return taskStatusService.getLatestActiveTask(projectId, "ANALYSIS");
    }

    /**
     * 取消分析任务
     */
    public boolean cancelTask(String taskId) {
        boolean cancelled = taskStatusService.cancelTask(taskId);
        if (cancelled) {
            broadcastService.broadcastError(taskId, "任务已取消");
        }
        return cancelled;
    }

    private PaperDTO toDTO(Paper paper) {
        PaperDTO dto = new PaperDTO();
        dto.setId(paper.getId());
        dto.setProjectId(paper.getProjectId());
        dto.setArxivId(paper.getArxivId());
        dto.setTitle(paper.getTitle());
        dto.setAbstractText(paper.getAbstractText());
        dto.setPdfUrl(paper.getPdfUrl());
        dto.setPublishDate(paper.getPublishDate());
        dto.setCitationCount(paper.getCitationCount());
        dto.setInfluenceScore(paper.getInfluenceScore());
        dto.setHasCode(paper.getHasCode());
        dto.setCodeUrl(paper.getCodeUrl());
        dto.setCategory(paper.getCategory());
        return dto;
    }

    private String toJson(Object value) {
        if (value == null) return "[]";
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize analysis result", e);
            return "[]";
        }
    }
}
