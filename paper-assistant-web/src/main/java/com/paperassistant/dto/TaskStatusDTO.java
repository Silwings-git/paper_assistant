package com.paperassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 任务状态响应 DTO
 */
@Schema(description = "异步任务状态信息")
public class TaskStatusDTO {

    @Schema(description = "任务ID", example = "task-abc123")
    private String taskId;

    @Schema(description = "任务类型: SEARCH(检索), ANALYSIS(分析)", example = "ANALYSIS")
    private String taskType;

    @Schema(description = "所属项目ID", example = "proj-001")
    private String projectId;

    @Schema(description = "任务状态: PENDING(等待中)/RUNNING(运行中)/COMPLETED(已完成)/FAILED(失败)", example = "RUNNING")
    private String status;

    @Schema(description = "任务进度 (0-100)", example = "50")
    private Integer progress;

    @Schema(description = "当前阶段描述", example = "正在执行交叉分析")
    private String stage;

    @Schema(description = "状态消息", example = "任务执行中")
    private String message;

    @Schema(description = "任务结果数据 (JSON字符串)")
    private String resultData;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResultData() {
        return resultData;
    }

    public void setResultData(String resultData) {
        this.resultData = resultData;
    }
}
