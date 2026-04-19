package com.paperassistant.scheduler.controller;

import com.paperassistant.dto.ApiResponse;
import com.paperassistant.scheduler.executor.SchedulerJobExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 定时任务 HTTP API (供未来 XXL-JOB 等分布式调度远程调用)
 */
@Tag(name = "定时任务", description = "定时任务的远程执行接口（供分布式调度系统调用）")
@RestController
@RequestMapping("/api/v1/scheduler")
public class SchedulerJobController {

    private final Map<String, SchedulerJobExecutor> executors;

    public SchedulerJobController(Map<String, SchedulerJobExecutor> executors) {
        this.executors = executors;
    }

    @Operation(summary = "执行定时任务", description = "手动触发指定名称的定时任务执行，供远程调度系统调用")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "任务执行成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在")
    })
    @PostMapping("/{job-name}/execute")
    public ApiResponse<String> execute(
            @Parameter(description = "任务名称", example = "task-status-cleanup")
            @PathVariable("job-name") String jobName) {
        SchedulerJobExecutor executor = executors.get(jobName);
        if (executor == null) {
            return ApiResponse.error(4041, "任务不存在: " + jobName);
        }
        String result = executor.execute();
        return ApiResponse.success(result);
    }
}
