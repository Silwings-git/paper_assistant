package com.paperassistant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paperassistant.dto.ApiResponse;
import com.paperassistant.dto.PaperDTO;
import com.paperassistant.dto.TaskStatusDTO;
import com.paperassistant.service.retrieval.PaperRetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * 论文检索 REST API
 */
@Tag(name = "论文检索", description = "论文的搜索、查询和异步任务状态管理")
@Validated
@RestController
@RequestMapping("/api/v1/papers")
public class PaperController {

    private final PaperRetrievalService paperRetrievalService;

    public PaperController(PaperRetrievalService paperRetrievalService) {
        this.paperRetrievalService = paperRetrievalService;
    }

    /**
     * 触发异步检索
     */
    @Operation(summary = "触发论文搜索", description = "根据关键词异步搜索项目相关的学术论文，返回任务ID用于后续状态查询")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索任务已创建，返回任务ID"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误：projectId或keyword为空")
    })
    @PostMapping("/search")
    public ApiResponse<String> search(
            @Parameter(description = "项目ID", example = "1") @RequestParam @NotNull String projectId,
            @Parameter(description = "搜索关键词", example = "RAG retrieval augmented generation")
            @RequestParam @NotBlank String keyword) {
        String taskId = paperRetrievalService.searchAsync(projectId, keyword);
        return ApiResponse.success(taskId);
    }

    /**
     * 查询检索结果 (分页)
     */
    @Operation(summary = "分页查询论文列表", description = "获取项目下的所有论文列表，支持分页和排序")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping
    public ApiResponse<Page<PaperDTO>> list(
            @Parameter(description = "项目ID", example = "1") @RequestParam @NotNull String projectId,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数 (最大100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @Parameter(description = "排序方式: relevance(相关性), date(日期), citations(引用)",
                    example = "relevance")
            @RequestParam(defaultValue = "relevance") String sort) {
        return ApiResponse.success(paperRetrievalService.getPapers(projectId, page, Math.min(size, 100), sort));
    }

    /**
     * 获取单篇论文详情
     */
    @Operation(summary = "获取论文详情", description = "根据ID获取单篇论文的完整信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "论文不存在")
    })
    @GetMapping("/{id}")
    public ApiResponse<PaperDTO> getById(
            @Parameter(description = "论文ID", example = "1") @PathVariable String id) {
        return ApiResponse.success(paperRetrievalService.getPaperById(id));
    }

    /**
     * 查询任务状态
     */
    @Operation(summary = "查询任务状态", description = "根据任务ID查询异步搜索任务的当前状态和进度")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在")
    })
    @GetMapping("/tasks/{taskId}")
    public ApiResponse<TaskStatusDTO> getTaskStatus(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        return ApiResponse.success(paperRetrievalService.getTaskStatus(taskId));
    }

    /**
     * 查询项目最新的进行中检索任务
     */
    @Operation(summary = "查询活跃检索任务", description = "获取项目当前进行中的检索任务状态")
    @GetMapping("/tasks/active")
    public ApiResponse<TaskStatusDTO> getActiveTask(
            @Parameter(description = "项目ID") @RequestParam @NotNull String projectId) {
        return ApiResponse.success(paperRetrievalService.getActiveTask(projectId));
    }

    /**
     * 取消检索任务
     */
    @Operation(summary = "取消检索任务", description = "取消进行中的检索任务，任务状态将更新为 CANCELLED")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取消成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "任务已结束，无法取消"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在")
    })
    @DeleteMapping("/tasks/{taskId}")
    public ApiResponse<Void> cancelTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        boolean success = paperRetrievalService.cancelTask(taskId);
        if (!success) {
            return ApiResponse.error(400, "任务已结束，无法取消");
        }
        return ApiResponse.success(null);
    }
}
