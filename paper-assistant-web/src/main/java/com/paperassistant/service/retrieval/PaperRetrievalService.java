package com.paperassistant.service.retrieval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paperassistant.config.WebSocketBroadcastService;
import com.paperassistant.dto.PaperDTO;
import com.paperassistant.dto.TaskStatusDTO;
import com.paperassistant.entity.Paper;
import com.paperassistant.entity.ProjectStatus;
import com.paperassistant.exception.BusinessException;
import com.paperassistant.exception.ErrorCode;
import com.paperassistant.mapper.PaperMapper;
import com.paperassistant.service.project.ProjectService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 论文检索服务：双源聚合、去重、排序、分页、缓存
 */
@Service
public class PaperRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(PaperRetrievalService.class);
    private static final int MAX_RESULTS = 100;

    private final ArxivFetcher arxivFetcher;
    private final SemanticScholarFetcher semanticScholarFetcher;
    private final PaperMapper paperMapper;
    private final ProjectService projectService;
    private final TaskStatusService taskStatusService;
    private final WebSocketBroadcastService broadcastService;
    private final ObjectMapper objectMapper;
    private final PaperRetrievalService self;

    public PaperRetrievalService(ArxivFetcher arxivFetcher,
                                 SemanticScholarFetcher semanticScholarFetcher,
                                 PaperMapper paperMapper,
                                 ProjectService projectService,
                                 TaskStatusService taskStatusService,
                                 WebSocketBroadcastService broadcastService,
                                 ObjectMapper objectMapper,
                                 @Lazy PaperRetrievalService self) {
        this.arxivFetcher = arxivFetcher;
        this.semanticScholarFetcher = semanticScholarFetcher;
        this.paperMapper = paperMapper;
        this.projectService = projectService;
        this.taskStatusService = taskStatusService;
        this.broadcastService = broadcastService;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    /**
     * 同步检索论文（带缓存）
     * 缓存 miss 时调用外部 API，结果写入 DB 并缓存
     */
    @Cacheable(value = "papers", key = "#projectId + ':' + #keyword")
    public List<Map<String, Object>> search(String projectId, String keyword) {
        log.info("Cache miss for projectId={}, keyword={}, calling external APIs", projectId, keyword);

        List<Map<String, Object>> allPapers = new ArrayList<>();

        // 检索 arXiv
        try {
            List<Map<String, Object>> arxivPapers = arxivFetcher.fetch(keyword);
            allPapers.addAll(arxivPapers);
            log.info("arXiv returned {} papers", arxivPapers.size());
        } catch (BusinessException e) {
            log.warn("arXiv fetch failed", e);
        }

        // 检索 Semantic Scholar
        try {
            List<Map<String, Object>> ssPapers = semanticScholarFetcher.fetch(keyword);
            allPapers.addAll(ssPapers);
            log.info("Semantic Scholar returned {} papers", ssPapers.size());
        } catch (BusinessException e) {
            log.warn("Semantic Scholar fetch failed", e);
        }

        // 去重、排序
        List<Map<String, Object>> deduplicated = deduplicate(allPapers);
        List<Map<String, Object>> sorted = sortPapers(deduplicated, "relevance");

        // 持久化到 DB
        persistPapers(projectId, sorted);

        return sorted;
    }

    /**
     * 创建搜索任务并异步执行（使用 self.search 走缓存）
     */
    public String searchAsync(String projectId, String keyword) {
        String taskId = UUID.randomUUID().toString();
        taskStatusService.createTask(taskId, "SEARCH", projectId);
        taskStatusService.updateTask(taskId, "SEARCHING", 0, "正在检索...", null);
        broadcastService.broadcastProgress(taskId, "SEARCHING", 0, "正在检索...");

        // 更新项目状态为检索中
        try {
            projectService.transitionStatus(projectId, ProjectStatus.SEARCHING);
        } catch (BusinessException e) {
            log.warn("Failed to transition project status to SEARCHING: {}", e.getMessage());
        }

        // 异步执行实际检索
        self.doSearchAsync(taskId, projectId, keyword);

        return taskId;
    }

    /**
     * 实际异步检索逻辑
     */
    @Async("retrievalExecutor")
    public CompletableFuture<Void> doSearchAsync(String taskId, String projectId, String keyword) {
        try {
            // 通过 self 调用，触发 @Cacheable 代理
            List<Map<String, Object>> papers = self.search(projectId, keyword);

            // 检查任务是否已被取消
            if (taskStatusService.isCancelled(taskId)) {
                log.info("Search task was cancelled: taskId={}", taskId);
                return CompletableFuture.completedFuture(null);
            }

            String status = "SEARCHED";
            String msg = "检索完成，共找到 " + papers.size() + " 篇论文";
            taskStatusService.updateTask(taskId, status, 100, msg, null);
            taskStatusService.updateTaskResult(taskId, objectMapper.writeValueAsString(Map.of("count", papers.size())));
            broadcastService.broadcastProgress(taskId, status, 100, "检索完成");

            // 更新项目状态
            projectService.transitionStatus(projectId, ProjectStatus.SEARCHED);

        } catch (Exception e) {
            log.error("Search failed for project {}: {}", projectId, e.getMessage(), e);
            taskStatusService.updateTask(taskId, "FAILED", 0, "检索失败: " + e.getMessage(), null);
            broadcastService.broadcastError(taskId, e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 查询检索结果 (分页)
     */
    public Page<PaperDTO> getPapers(String projectId, int page, int size, String sort) {
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Paper::getProjectId, projectId);
        wrapper.orderByDesc(Paper::getCreateTime);

        Page<Paper> paperPage = paperMapper.selectPage(new Page<>(page, size), wrapper);

        Page<PaperDTO> dtoPage = new Page<>(page, size, paperPage.getTotal());
        dtoPage.setRecords(paperPage.getRecords().stream().map(this::toDTO).toList());
        return dtoPage;
    }

    /**
     * 获取单篇论文详情
     */
    public PaperDTO getPaperById(String id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null || paper.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "论文不存在: " + id);
        }
        return toDTO(paper);
    }

    /**
     * 查询任务状态
     */
    public TaskStatusDTO getTaskStatus(String taskId) {
        return taskStatusService.getTaskStatus(taskId);
    }

    /**
     * 查询项目最新的进行中检索任务
     */
    public TaskStatusDTO getActiveTask(String projectId) {
        return taskStatusService.getLatestActiveTask(projectId, "SEARCH");
    }

    /**
     * 取消检索任务
     */
    public boolean cancelTask(String taskId) {
        boolean cancelled = taskStatusService.cancelTask(taskId);
        if (cancelled) {
            broadcastService.broadcastError(taskId, "任务已取消");
        }
        return cancelled;
    }

    // ---------- 内部方法 ----------


    private List<Map<String, Object>> deduplicate(List<Map<String, Object>> papers) {
        LevenshteinDistance ld = new LevenshteinDistance();
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seenTitles = new HashSet<>();

        for (Map<String, Object> paper : papers) {
            String title = (String) paper.get("title");
            if (title == null) continue;

            boolean duplicate = false;
            for (String seen : seenTitles) {
                String t1 = title.toLowerCase().replaceAll("[^a-z0-9]", "");
                String t2 = seen.toLowerCase().replaceAll("[^a-z0-9]", "");
                int maxLen = Math.max(t1.length(), t2.length());
                if (maxLen == 0) continue;
                double distance = (double) ld.apply(t1, t2) / maxLen;
                if (distance < 0.15) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate) {
                result.add(paper);
                seenTitles.add(title);
            }
        }

        return result;
    }

    private List<Map<String, Object>> sortPapers(List<Map<String, Object>> papers, String sortOrder) {
        return switch (sortOrder) {
            case "citation" -> papers.stream()
                    .sorted((a, b) -> Integer.compare(
                            ((Number) b.getOrDefault("citationCount", 0)).intValue(),
                            ((Number) a.getOrDefault("citationCount", 0)).intValue()))
                    .toList();
            case "date" -> papers.stream()
                    .sorted((a, b) -> {
                        LocalDate da = (LocalDate) a.getOrDefault("publishDate", LocalDate.MIN);
                        LocalDate db = (LocalDate) b.getOrDefault("publishDate", LocalDate.MIN);
                        if (da == null) return 1;
                        if (db == null) return -1;
                        return db.compareTo(da);
                    })
                    .toList();
            default -> papers; // relevance: 保持原始顺序
        };
    }

    private void persistPapers(String projectId, List<Map<String, Object>> papers) {
        for (Map<String, Object> paperData : papers) {
            try {
                String source = (String) paperData.get("source");
                String sourceId = (String) paperData.get("sourceId");
                if (source == null || source.isEmpty()) continue;
                if (sourceId == null || sourceId.isEmpty()) continue;

                String arxivId = (String) paperData.get("arxivId");

                String id = com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr();
                paperMapper.insertIgnore(
                        id, projectId, arxivId, source, sourceId,
                        (String) paperData.get("title"),
                        (String) paperData.get("abstract"),
                        toJson(paperData.get("authors")),
                        (LocalDate) paperData.get("publishDate"),
                        (Integer) paperData.get("citationCount"),
                        (Double) paperData.get("influenceScore"),
                        (Boolean) paperData.get("hasCode"),
                        (String) paperData.get("codeUrl"),
                        (String) paperData.get("pdfUrl"),
                        (String) paperData.get("category")
                );
            } catch (Exception e) {
                log.warn("Failed to persist paper: {}", paperData.get("title"), e);
            }
        }
    }

    private String toJson(Object value) {
        if (value == null) return "[]";
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private PaperDTO toDTO(Paper paper) {
        PaperDTO dto = new PaperDTO();
        dto.setId(paper.getId());
        dto.setProjectId(paper.getProjectId());
        dto.setArxivId(paper.getArxivId());
        dto.setSource(paper.getSource());
        dto.setSourceId(paper.getSourceId());
        dto.setTitle(paper.getTitle());
        dto.setAbstractText(paper.getAbstractText());
        dto.setPublishDate(paper.getPublishDate());
        dto.setCitationCount(paper.getCitationCount());
        dto.setInfluenceScore(paper.getInfluenceScore());
        dto.setHasCode(paper.getHasCode());
        dto.setCodeUrl(paper.getCodeUrl());
        dto.setPdfUrl(paper.getPdfUrl());
        dto.setCategory(paper.getCategory());

        // 解析 JSONB authors
        if (paper.getAuthors() != null) {
            try {
                dto.setAuthors(objectMapper.readValue(paper.getAuthors(), List.class));
            } catch (JsonProcessingException e) {
                dto.setAuthors(List.of());
            }
        }

        return dto;
    }
}
