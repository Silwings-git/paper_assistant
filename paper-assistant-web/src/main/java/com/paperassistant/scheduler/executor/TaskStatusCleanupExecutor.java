package com.paperassistant.scheduler.executor;

import com.paperassistant.service.retrieval.TaskStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 任务状态清理执行器
 */
@Component("taskStatusCleanup")
public class TaskStatusCleanupExecutor implements SchedulerJobExecutor {

    private static final Logger log = LoggerFactory.getLogger(TaskStatusCleanupExecutor.class);

    private final TaskStatusService taskStatusService;

    public TaskStatusCleanupExecutor(TaskStatusService taskStatusService) {
        this.taskStatusService = taskStatusService;
    }

    @Override
    public String execute() {
        long start = System.currentTimeMillis();
        int deleted = taskStatusService.cleanupOldTasks();
        long duration = System.currentTimeMillis() - start;
        String result = "清理完成，删除 " + deleted + " 条过期任务记录，耗时 " + duration + "ms";
        log.info(result);
        return result;
    }
}
