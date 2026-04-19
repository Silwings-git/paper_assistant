package com.paperassistant.controller;

import com.paperassistant.dto.ApiResponse;
import com.paperassistant.dto.TaskStatusDTO;
import com.paperassistant.entity.AnalysisResult;
import com.paperassistant.service.analysis.AnalysisOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分析 REST API
 */
@Tag(name = "论文分析", description = "LLM驱动的论文创新分析、交叉对比和研究方向推荐")
@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final AnalysisOrchestrator analysisOrchestrator;

    public AnalysisController(AnalysisOrchestrator analysisOrchestrator) {
        this.analysisOrchestrator = analysisOrchestrator;
    }

    /**
     * 触发异步分析
     */
    @Operation(summary = "触发论文分析", description = "异步分析项目中的论文，包含深度理解、交叉对比和创新方向生成三个阶段。若不传 paperIds 则分析项目下所有论文")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "分析任务已创建，返回任务ID"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @PostMapping("/{projectId}")
    public ApiResponse<String> triggerAnalysis(
            @Parameter(description = "项目ID", example = "1") @PathVariable String projectId,
            @Parameter(description = "论文ID列表，必填") @org.springframework.web.bind.annotation.RequestBody(required = false) java.util.List<String> paperIds) {
        if (paperIds == null || paperIds.isEmpty()) {
            return ApiResponse.error(400, "请至少选择一篇论文再触发分析");
        }
        String taskId = analysisOrchestrator.analyzeAsync(projectId, paperIds);
        return ApiResponse.success(taskId);
    }

    /**
     * 查询分析结果
     */
    @Operation(summary = "查询分析结果", description = "获取项目的LLM分析结果，包含研究空白、基础论文推荐和创新点")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目或分析结果不存在")
    })
    @GetMapping("/{projectId}/result")
    public ApiResponse<AnalysisResult> getResult(
            @Parameter(description = "项目ID", example = "1") @PathVariable String projectId) {
        return ApiResponse.success(analysisOrchestrator.getAnalysisResult(projectId));
    }

    /**
     * 查询任务状态
     */
    @Operation(summary = "查询分析任务状态", description = "根据任务ID查询异步分析任务的当前状态和进度")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在")
    })
    @GetMapping("/tasks/{taskId}")
    public ApiResponse<TaskStatusDTO> getTaskStatus(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        Object result = analysisOrchestrator.getTaskStatus(taskId);
        return ApiResponse.success((TaskStatusDTO) result);
    }

    /**
     * 查询项目最新的进行中分析任务
     */
    @Operation(summary = "查询活跃分析任务", description = "获取项目当前进行中的分析任务状态")
    @GetMapping("/tasks/active")
    public ApiResponse<TaskStatusDTO> getActiveTask(
            @Parameter(description = "项目ID") @RequestParam String projectId) {
        return ApiResponse.success((TaskStatusDTO) analysisOrchestrator.getActiveTask(projectId));
    }

    /**
     * 取消分析任务
     */
    @Operation(summary = "取消分析任务", description = "取消进行中的分析任务，任务状态将更新为 CANCELLED")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取消成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "任务已结束，无法取消"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在")
    })
    @DeleteMapping("/tasks/{taskId}")
    public ApiResponse<Void> cancelTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        boolean success = analysisOrchestrator.cancelTask(taskId);
        if (!success) {
            return ApiResponse.error(400, "任务已结束，无法取消");
        }
        return ApiResponse.success(null);
    }
}
