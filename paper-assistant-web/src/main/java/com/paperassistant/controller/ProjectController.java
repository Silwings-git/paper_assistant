package com.paperassistant.controller;

import com.paperassistant.dto.ApiResponse;
import com.paperassistant.dto.CreateProjectRequest;
import com.paperassistant.dto.ProjectDTO;
import com.paperassistant.dto.UpdateProjectRequest;
import com.paperassistant.service.project.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目管理 REST API
 */
@Tag(name = "项目管理", description = "研究项目的增删改查及基础论文设置")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "创建项目", description = "创建一个新的研究项目，包含名称、描述和研究方向")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数验证失败")
    })
    @PostMapping
    public ApiResponse<ProjectDTO> create(@Valid @RequestBody CreateProjectRequest request) {
        return ApiResponse.success(projectService.create(request));
    }

    @Operation(summary = "更新项目", description = "更新指定项目的名称、描述或研究方向")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @PutMapping("/{id}")
    public ApiResponse<ProjectDTO> update(
            @Parameter(description = "项目ID", example = "1") @PathVariable String id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return ApiResponse.success(projectService.update(id, request));
    }

    @Operation(summary = "查询项目详情", description = "根据ID获取项目的完整信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @GetMapping("/{id}")
    public ApiResponse<ProjectDTO> getById(
            @Parameter(description = "项目ID", example = "1") @PathVariable String id) {
        return ApiResponse.success(projectService.getById(id));
    }

    @Operation(summary = "列出所有项目", description = "获取当前用户的所有研究项目列表")
    @GetMapping
    public ApiResponse<List<ProjectDTO>> list() {
        return ApiResponse.success(projectService.list());
    }

    @Operation(summary = "删除项目", description = "根据ID删除指定项目")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "项目ID", example = "1") @PathVariable String id) {
        projectService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    @Operation(summary = "设置基础论文", description = "为项目指定一篇基础论文，作为后续分析的基准")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "设置成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目或论文不存在")
    })
    @PutMapping("/{id}/base-paper")
    public ApiResponse<ProjectDTO> setBasePaper(
            @Parameter(description = "项目ID", example = "1") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "基础论文ID请求体")
            @RequestBody SetBasePaperRequest request) {
        return ApiResponse.success(projectService.setBasePaper(id, request.paperId()));
    }

    public record SetBasePaperRequest(
            @io.swagger.v3.oas.annotations.media.Schema(description = "论文ID", example = "1")
            String paperId) {
    }
}
